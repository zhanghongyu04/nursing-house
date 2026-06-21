package com.zhiling.system.application.service.impl;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.common.result.PageResult;
import com.zhiling.common.utils.AESUtil;
import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.model.dto.SanatoriumDetailPageQueryDto;
import com.zhiling.model.dto.SanatoriumImportExcelDto;
import com.zhiling.model.dto.SanatoriumPageQueryDto;
import com.zhiling.model.entity.Elder;
import com.zhiling.model.entity.Sanatorium;
import com.zhiling.system.application.service.SanatoriumService;
import com.zhiling.system.application.repository.ElderRepository;
import com.zhiling.system.application.repository.SanatoriumRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * 养老院服务实现
 *
 * @author zhanghongyu
 */
@Service
public class SanatoriumServiceImpl implements SanatoriumService {
    private static final int MAX_UNPAGED_SIZE = Integer.MAX_VALUE;
    private static final String SANA_PAGE_PATH = "/web/sanatorium/page";
    private static final String SANA_ADD_PATH = "/web/sanatorium/add";
    private static final String SANA_UPDATE_PATH = "/web/sanatorium/update";
    private static final String SANA_DELETE_PATH = "/web/sanatorium/delete";
    private static final String SANA_EXPORT_PATH = "/web/sanatorium/export";
    private static final String SANA_IMPORT_PATH = "/web/sanatorium/import";
    private static final String SANA_ELDER_DISTRIBUTION_PATH = "/web/sanatorium/elderDistribution";
    private static final String SANA_ELDER_PAGE_PATH = "/web/sanatorium/pageSanaElderList";

    private final SanatoriumRepository sanatoriumRepository;
    private final ElderRepository elderRepository;
    private final SecurityHelper securityHelper;

    public SanatoriumServiceImpl(SanatoriumRepository sanatoriumRepository,
                                ElderRepository elderRepository,
                                SecurityHelper securityHelper) {
        this.sanatoriumRepository = sanatoriumRepository;
        this.elderRepository = elderRepository;
        this.securityHelper = securityHelper;
    }

    /**
     * 查询所有养老院信息
     */
    public List<Sanatorium> list() {
        if (!securityHelper.hasGovAdminRoleForSensitiveOperation()) {
            Set<Long> sanaScopeIds = securityHelper.requireCurrentSanaScopeIdsForSensitiveOperation();
            if (sanaScopeIds.isEmpty()) {
                return List.of();
            }
            return sanatoriumRepository.listByIds(sanaScopeIds);
        }
        return sanatoriumRepository.listAll();
    }

    @Override
    public List<Sanatorium> listForExport(SanatoriumPageQueryDto queryDto) {
        requireResource(SANA_EXPORT_PATH);
        SanatoriumPageQueryDto exportQuery = queryDto == null ? new SanatoriumPageQueryDto() : queryDto;
        exportQuery.setPage(1);
        exportQuery.setPageSize(MAX_UNPAGED_SIZE);
        if (!securityHelper.hasGovAdminRoleForSensitiveOperation()) {
            exportQuery.setSanaScopeIds(securityHelper.requireCurrentSanaScopeIdsForSensitiveOperation());
            exportQuery.setSanaId(null);
        }
        Page<Sanatorium> page = new Page<>(exportQuery.getPage(), exportQuery.getPageSize());
        IPage<Sanatorium> result = sanatoriumRepository.selectPage(page, exportQuery);
        return result.getRecords() == null ? List.of() : result.getRecords();
    }

    /**
     * 批量保存养老院信息（使用 MyBatis-Plus）
     */
    public boolean saveBatch(List<Sanatorium> sanatoriumList) {
        requireResource(SANA_ADD_PATH);
        if (sanatoriumList == null || sanatoriumList.isEmpty()) {
            return true;
        }

        // 非政府管理员仅允许维护本机构
        if (!securityHelper.hasGovAdminRoleForSensitiveOperation()) {
            Set<Long> sanaScopeIds = securityHelper.requireCurrentSanaScopeIdsForSensitiveOperation();
            if (sanatoriumList.size() > 1) {
                throw new AccessDeniedException("机构管理员仅可导入本机构单条数据");
            }
            Sanatorium current = sanatoriumList.get(0);
            if (current.getId() != null && !sanaScopeIds.contains(current.getId())) {
                throw new AccessDeniedException("无权导入其他机构数据");
            }
            if (current.getId() == null) {
                if (sanaScopeIds.size() != 1) {
                    throw new AccessDeniedException("当前账号已授权多个机构，请明确指定机构ID");
                }
                current.setId(sanaScopeIds.iterator().next());
            }
            Long targetSanaId = current.getId();
            if (sanatoriumRepository.selectById(targetSanaId) != null) {
                return sanatoriumRepository.updateById(current);
            }
            return sanatoriumRepository.insert(current);
        }

        for (Sanatorium sanatorium : sanatoriumList) {
            sanatoriumRepository.insert(sanatorium);
        }
        return true;
    }

    @Override
    public ImportResult importSanatoriums(List<SanatoriumImportExcelDto> rows) {
        requireResource(SANA_IMPORT_PATH);
        List<SanatoriumImportExcelDto> importRows = rows == null ? List.of() : rows;
        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;
        List<String> failDetails = new java.util.ArrayList<>();
        Set<String> batchUsccSet = new java.util.HashSet<>();

        for (int i = 0; i < importRows.size(); i++) {
            int excelRowNum = i + 2;
            SanatoriumImportExcelDto row = importRows.get(i);
            if (!hasImportContent(row)) {
                skipCount++;
                continue;
            }

            List<String> rowErrors = new java.util.ArrayList<>();
            Sanatorium sanatorium = toSanatoriumEntity(row, rowErrors);
            String sanaName = sanatorium.getSanaName() != null ? sanatorium.getSanaName() : "未知机构";

            String uscc = sanatorium.getUscc();
            if (uscc != null) {
                if (!batchUsccSet.add(uscc)) {
                    rowErrors.add("与本次导入中的其他行统一社会信用代码重复");
                } else if (sanatoriumRepository.existsByUscc(uscc)) {
                    rowErrors.add("统一社会信用代码已存在（" + uscc + "）");
                }
            }

            if (!rowErrors.isEmpty()) {
                failCount++;
                failDetails.add(String.format("第%s行（%s）：%s", excelRowNum, sanaName, String.join("；", rowErrors)));
                continue;
            }

            try {
                boolean inserted = sanatoriumRepository.insert(sanatorium);
                if (inserted) {
                    successCount++;
                } else {
                    failCount++;
                    failDetails.add(String.format("第%s行（%s）：数据库执行失败", excelRowNum, sanaName));
                }
            } catch (Exception e) {
                failCount++;
                failDetails.add(String.format("第%s行（%s）：导入失败 - %s", excelRowNum, sanaName, e.getMessage()));
            }
        }

        return new ImportResult(importRows.size(), successCount, failCount, skipCount, failDetails);
    }

    /**
     * 分页查询养老院信息（使用 MyBatis-Plus 分页）
     */
    public PageResult page(SanatoriumPageQueryDto sanatoriumPageQueryDto) {
        requireResource(SANA_PAGE_PATH);
        if (!securityHelper.hasGovAdminRoleForSensitiveOperation()) {
            sanatoriumPageQueryDto.setSanaScopeIds(securityHelper.requireCurrentSanaScopeIdsForSensitiveOperation());
            sanatoriumPageQueryDto.setSanaId(null);
        }
        Page<Sanatorium> page = new Page<>(sanatoriumPageQueryDto.getPage(), sanatoriumPageQueryDto.getPageSize());
        IPage<Sanatorium> result = sanatoriumRepository.selectPage(page, sanatoriumPageQueryDto);
        return new PageResult(result.getTotal(), result.getRecords());
    }

    /**
     * 删除养老院信息
     */
    public Boolean removeById(Long id) {
        requireResource(SANA_DELETE_PATH);
        assertCanOperateSana(id);
        return sanatoriumRepository.deleteById(id);
    }

    /**
     * 修改养老院信息
     */
    public boolean update(Sanatorium sanatorium) {
        requireResource(SANA_UPDATE_PATH);
        if (sanatorium == null || sanatorium.getId() == null) {
            throw new IllegalArgumentException("机构ID不能为空");
        }
        assertCanOperateSana(sanatorium.getId());
        return sanatoriumRepository.updateById(sanatorium);
    }

    /**
     * 根据养老院名称查询不同自理能力老人数量分布
     */
    public Map<String, Integer> elderDistribution(String sanaName) {
        requireResource(SANA_ELDER_DISTRIBUTION_PATH);
        if (!securityHelper.hasGovAdminRoleForSensitiveOperation()) {
            Set<Long> sanaScopeIds = securityHelper.requireCurrentSanaScopeIdsForSensitiveOperation();
            if (sanaName == null || sanaName.trim().isEmpty()) {
                if (sanaScopeIds.size() != 1) {
                    throw new AccessDeniedException("当前账号已授权多个机构，请选择具体机构");
                }
                sanaName = sanatoriumRepository.getNameById(sanaScopeIds.iterator().next());
            } else {
                Long targetSanaId = sanatoriumRepository.selectIdByName(sanaName);
                if (targetSanaId == null || !sanaScopeIds.contains(targetSanaId)) {
                    throw new AccessDeniedException("无权查看该机构数据");
                }
            }
        }
        if (sanaName == null || sanaName.trim().isEmpty()) {
            return Map.of();
        }
        List<Map<String, Object>> list = elderRepository.queryElderDistribution(sanaName);

        Map<String, Integer> result = new HashMap<>();
        for (Map<String, Object> map : list) {
            Object selfCare = map.get("self_care");
            Object countObj = map.get("count");

            int count = (countObj instanceof Number) ? ((Number) countObj).intValue() : 0;
            String label = formatSelfCare(selfCare);

            result.put(label, count);
        }

        return result;
    }

    /**
     * 方法：formatSelfCare
     *
     * @author zhanghongyu
     */
    private String formatSelfCare(Object val) {
        int type;
        // 判断类型
        if (val instanceof Number) {
            type = ((Number) val).intValue();
        } else if (val instanceof String) {
            try {
                type = Integer.parseInt((String) val);
            } catch (NumberFormatException e) {
                return "未知";
            }
        } else if (val instanceof Boolean) {
            type = (Boolean) val ? 1 : 0;
        } else {
            return "未知";
        }

        return switch (type) {
            case 0 -> "能力完好";
            case 1 -> "轻度失能";
            case 2 -> "中度失能";
            case 3 -> "重度失能";
            case 4 -> "完全失能";
            default -> "未知";
        };
    }

    /**
     * 分页查询养老院详情老人信息（使用 MyBatis-Plus 分页）
     */
    public PageResult pageSanaElderList(SanatoriumDetailPageQueryDto sanatoriumDetailPageQueryDto) {
        requireResource(SANA_ELDER_PAGE_PATH);
        if (!securityHelper.hasGovAdminRoleForSensitiveOperation()) {
            sanatoriumDetailPageQueryDto.setSanaScopeIds(securityHelper.requireCurrentSanaScopeIdsForSensitiveOperation());
            sanatoriumDetailPageQueryDto.setSanaId(null);
        }
        Page<Elder> page = new Page<>(sanatoriumDetailPageQueryDto.getPage(), sanatoriumDetailPageQueryDto.getPageSize());
        IPage<Elder> result = elderRepository.selectPageSanaElderList(page, sanatoriumDetailPageQueryDto);
        LocalDate today = LocalDate.now();
        result.getRecords().forEach(elder -> {
            maskElderIdNumberForDisplay(elder);
            Long inpatientDays = calculateInpatientDays(today, elder.getInpatientsTime(), elder.getOutpatientsTime());
            elder.setInpatientDays(inpatientDays);
            elder.setInpatientDaysLabel(isDischarged(today, elder.getOutpatientsTime()) ? "已出院" : String.valueOf(inpatientDays));
        });
        return new PageResult(result.getTotal(), result.getRecords());
    }

    /**
     * 入住时长计算规则：
     * 1. 无入院时间：0
     * 2. 有出院时间：按 入院 -> 出院 计算（若出院晚于今天，按今天截断）
     * 3. 无出院时间：按 入院 -> 今天 计算
     * 4. 结束时间早于入院时间：0
     */
    private Long calculateInpatientDays(LocalDate today, LocalDate inpatientsTime, LocalDate outpatientsTime) {
        if (inpatientsTime == null) {
            return 0L;
        }

        LocalDate endDate = today;
        if (outpatientsTime != null) {
            endDate = outpatientsTime.isAfter(today) ? today : outpatientsTime;
        }
        if (endDate.isBefore(inpatientsTime)) {
            return 0L;
        }

        long days = ChronoUnit.DAYS.between(inpatientsTime, endDate);
        return Math.max(days, 0L);
    }

    /**
     * 方法：isDischarged
     *
     * @author zhanghongyu
     */
    private boolean isDischarged(LocalDate today, LocalDate outpatientsTime) {
        return outpatientsTime != null && !outpatientsTime.isAfter(today);
    }

    private void maskElderIdNumberForDisplay(Elder elder) {
        if (elder == null || elder.getIdNumber() == null || elder.getIdNumber().isBlank()) {
            return;
        }
        String idNumber = AESUtil.decrypt(elder.getIdNumber());
        if (idNumber == null || idNumber.isBlank() || idNumber.length() <= 7) {
            elder.setIdNumber(idNumber);
            return;
        }
        int maskLength = Math.max(1, idNumber.length() - 10);
        elder.setIdNumber(idNumber.substring(0, 6) + "*".repeat(maskLength) + idNumber.substring(idNumber.length() - 4));
    }

    private Sanatorium toSanatoriumEntity(SanatoriumImportExcelDto row, List<String> rowErrors) {
        Sanatorium sanatorium = new Sanatorium();
        String sanaName = trimToNull(row.getSanaName());
        sanatorium.setSanaName(sanaName);
        if (sanaName == null) {
            rowErrors.add("机构名称不能为空");
        } else if (sanaName.length() > 255) {
            rowErrors.add("机构名称长度不能超过255个字");
        }

        String sanaAffiliation = trimToNull(row.getSanaAffiliation());
        sanatorium.setSanaAffiliation(sanaAffiliation);
        if (sanaAffiliation != null && sanaAffiliation.length() > 255) {
            rowErrors.add("所属区划长度不能超过255个字");
        }

        String sanaAddress = trimToNull(row.getSanaAddress());
        sanatorium.setSanaAddress(sanaAddress);
        if (sanaAddress == null) {
            rowErrors.add("机构地址不能为空");
        } else if (sanaAddress.length() > 255) {
            rowErrors.add("机构地址长度不能超过255个字");
        }

        sanatorium.setStatus(parseStatus(row.getStatus(), rowErrors));

        String uscc = trimToNull(row.getUscc());
        sanatorium.setUscc(uscc);
        if (uscc == null) {
            rowErrors.add("统一社会信用代码不能为空");
        } else if (uscc.length() > 20) {
            rowErrors.add("统一社会信用代码长度不能超过20位");
        }

        String legalPersons = trimToNull(row.getLegalPersons());
        sanatorium.setLegalPersons(legalPersons);
        if (legalPersons != null && legalPersons.length() > 50) {
            rowErrors.add("法人姓名长度不能超过50个字");
        }

        String legalPhone = trimToNull(row.getLegalPhone());
        sanatorium.setLegalPhone(legalPhone);
        if (legalPhone != null && legalPhone.length() > 11) {
            rowErrors.add("法人联系方式长度不能超过11位");
        }

        sanatorium.setBedCount(parseInteger(row.getBedCount(), "床位总数", rowErrors));
        sanatorium.setBedInUse(parseInteger(row.getBedInUse(), "已用床位数", rowErrors));
        sanatorium.setElderCount(parseInteger(row.getElderCount(), "养老人员数量", rowErrors));
        sanatorium.setNursingCount(parseInteger(row.getNursingCount(), "护理人员数量", rowErrors));
        sanatorium.setMedicalCount(parseInteger(row.getMedicalCount(), "医护人员数量", rowErrors));
        if (sanatorium.getBedCount() != null && sanatorium.getBedInUse() != null
                && sanatorium.getBedInUse() > sanatorium.getBedCount()) {
            rowErrors.add("已用床位数不能大于床位总数");
        }
        sanatorium.setOrgLevel(1);
        return sanatorium;
    }

    private boolean hasImportContent(SanatoriumImportExcelDto row) {
        if (row == null) {
            return false;
        }
        return trimToNull(row.getSanaName()) != null
                || trimToNull(row.getSanaAffiliation()) != null
                || trimToNull(row.getSanaAddress()) != null
                || trimToNull(row.getStatus()) != null
                || trimToNull(row.getUscc()) != null
                || trimToNull(row.getLegalPersons()) != null
                || trimToNull(row.getLegalPhone()) != null
                || trimToNull(row.getBedCount()) != null
                || trimToNull(row.getBedInUse()) != null
                || trimToNull(row.getElderCount()) != null
                || trimToNull(row.getNursingCount()) != null
                || trimToNull(row.getMedicalCount()) != null;
    }

    private Integer parseStatus(String value, List<String> rowErrors) {
        String normalized = normalizeLabel(value);
        if (normalized == null) {
            return null;
        }
        return switch (normalized) {
            case "0", "正常运营" -> 0;
            case "1", "停业整顿" -> 1;
            case "2", "注销取缔" -> 2;
            default -> {
                rowErrors.add("运营状态仅支持正常运营、停业整顿、注销取缔");
                yield null;
            }
        };
    }

    private Integer parseInteger(String value, String fieldName, List<String> rowErrors) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(normalized.replaceAll("\\.0$", ""));
            if (parsed < 0) {
                rowErrors.add(fieldName + "不能小于0");
                return null;
            }
            return parsed;
        } catch (NumberFormatException e) {
            rowErrors.add(fieldName + "必须为整数");
            return null;
        }
    }

    private String normalizeLabel(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.replaceAll("\\s+", "");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 方法：assertCanOperateSana
     *
     * @author zhanghongyu
     */
    private void assertCanOperateSana(Long targetSanaId) {
        if (!securityHelper.canOperateSanaForSensitiveOperation(targetSanaId)) {
            throw new AccessDeniedException("无权操作其他机构数据");
        }
    }

    private void requireResource(String resourcePath) {
        if (!securityHelper.hasResourcePathForSensitiveOperation(resourcePath)) {
            throw new AccessDeniedException("无权访问康养机构功能");
        }
    }
}

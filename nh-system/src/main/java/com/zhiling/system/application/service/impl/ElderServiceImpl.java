package com.zhiling.system.application.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.common.result.PageResult;
import com.zhiling.common.utils.AESUtil;
import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.model.dto.ElderDto;
import com.zhiling.model.dto.ElderExcelDto;
import com.zhiling.model.dto.ElderImportExcelDto;
import com.zhiling.model.dto.ElderPageQueryDto;
import com.zhiling.model.entity.Elder;
import com.zhiling.model.entity.Sanatorium;
import com.zhiling.common.enums.BedTypeEnum;
import com.zhiling.system.application.service.ElderService;
import com.zhiling.system.application.repository.ElderRepository;
import com.zhiling.system.application.repository.SanatoriumRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 老人信息服务实现
 *
 * @author zhanghongyu
 */
@Service
public class ElderServiceImpl implements ElderService {

    /** 用于查询全部记录的分页大小 */
    private static final int MAX_UNPAGED_SIZE = Integer.MAX_VALUE;
    private static final String ELDER_PAGE_PATH = "/web/elder/page";
    private static final String ELDER_ADD_PATH = "/web/elder/add";
    private static final String ELDER_UPDATE_PATH = "/web/elder/update";
    private static final String ELDER_DELETE_PATH = "/web/elder/delete";
    private static final String ELDER_EXPORT_PATH = "/web/elder/export";
    private static final String ELDER_IMPORT_PATH = "/web/elder/import";
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy.M.d"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd")
    );

    private final ElderRepository elderRepository;
    private final SanatoriumRepository sanatoriumRepository;
    private final SecurityHelper securityHelper;

    public ElderServiceImpl(ElderRepository elderRepository,
                           SanatoriumRepository sanatoriumRepository,
                           SecurityHelper securityHelper) {
        this.elderRepository = elderRepository;
        this.sanatoriumRepository = sanatoriumRepository;
        this.securityHelper = securityHelper;
    }

    /**
     * 添加老人信息
     */
    public Boolean add(Elder elder) {
        requireResource(ELDER_ADD_PATH);
        elder.setOccupiedBedType(normalizeBedType(elder.getOccupiedBedType()));
        String sanaName = elder.getSanaName();
        Long sanatoriumId = sanatoriumRepository.selectIdByName(sanaName);
        assertCanOperateSana(sanatoriumId);
        elder.setSanaId(sanatoriumId);

        encryptSensitiveFields(elder);

        return elderRepository.insert(elder);
    }

    /**
     * 删除老人信息
     */
    public Boolean delete(Long id) {
        requireResource(ELDER_DELETE_PATH);
        Elder target = elderRepository.selectById(id);
        if (target == null) {
            return false;
        }
        assertCanOperateSana(target.getSanaId());
        return elderRepository.removeById(id);
    }

    /**
     * 修改老人信息
     */
    public Boolean update(ElderDto elder) {
        requireResource(ELDER_UPDATE_PATH);
        if (elder == null || elder.getId() == null) {
            throw new IllegalArgumentException("老人ID不能为空");
        }
        elder.setOccupiedBedType(normalizeBedType(elder.getOccupiedBedType()));
        Elder target = elderRepository.selectById(elder.getId());
        if (target == null) {
            return false;
        }
        assertCanOperateSana(target.getSanaId());
        if (elder.getSanaId() != null) {
            assertCanOperateSana(elder.getSanaId());
        } else if (elder.getSanaName() != null) {
            String sanaName = elder.getSanaName();
            Long sanatoriumId = sanatoriumRepository.selectIdByName(sanaName);
            assertCanOperateSana(sanatoriumId);
            elder.setSanaId(sanatoriumId);
        }

        if (elder.getIdNumber() != null && !elder.getIdNumber().trim().isEmpty()) {
            elder.setIdNumber(AESUtil.encrypt(elder.getIdNumber()));
        } else {
            elder.setIdNumber(null);
        }
        if (elder.getPhoneNumber() != null) {
            elder.setPhoneNumber(AESUtil.encrypt(elder.getPhoneNumber()));
        }
        if (elder.getHomeAddress() != null) {
            elder.setHomeAddress(AESUtil.encrypt(elder.getHomeAddress()));
        }
        if (elder.getGuardianPhone() != null) {
            elder.setGuardianPhone(AESUtil.encrypt(elder.getGuardianPhone()));
        }

        return elderRepository.update(elder);
    }

    /**
     * 分页查询老人信息（使用 MyBatis-Plus 分页）
     */
    public PageResult page(ElderPageQueryDto elderPageQueryDto) {
        requireResource(ELDER_PAGE_PATH);
        elderPageQueryDto.setOccupiedBedType(normalizeBedType(elderPageQueryDto.getOccupiedBedType()));
        if (!securityHelper.hasGovAdminRoleForSensitiveOperation()) {
            Set<Long> scopeIds = securityHelper.requireCurrentSanaScopeIdsForSensitiveOperation();
            elderPageQueryDto.setSanaScopeIds(scopeIds);
            Long requestedSanaId = elderPageQueryDto.getSanaId();
            if (requestedSanaId != null && !scopeIds.contains(requestedSanaId)) {
                throw new AccessDeniedException("无权查询其他机构数据");
            }
        } else if (elderPageQueryDto.getSanaName() != null && !elderPageQueryDto.getSanaName().trim().isEmpty()) {
            elderPageQueryDto.setSanaId(sanatoriumRepository.selectIdByName(elderPageQueryDto.getSanaName()));
        }

        Page<Elder> page = new Page<>(elderPageQueryDto.getPage(), elderPageQueryDto.getPageSize());
        IPage<Elder> result = elderRepository.selectPage(page, elderPageQueryDto);

        // 解密敏感信息
        for (Elder elder : result.getRecords()) {
            decryptElderInfo(elder);
            maskSensitiveFieldsForDisplay(elder);
        }

        return new PageResult(result.getTotal(), result.getRecords());
    }

    /**
     * 查询所有老人信息
     */
    public List<Elder> list() {
        List<Elder> elders;
        if (securityHelper.hasGovAdminRoleForSensitiveOperation()) {
            elders = elderRepository.listAll();
        } else {
            ElderPageQueryDto dto = ElderPageQueryDto.builder()
                    .page(1)
                    .pageSize(MAX_UNPAGED_SIZE)
                    .sanaScopeIds(securityHelper.requireCurrentSanaScopeIdsForSensitiveOperation())
                    .build();
            Page<Elder> page = new Page<>(dto.getPage(), dto.getPageSize());
            elders = elderRepository.selectPage(page, dto).getRecords();
        }

        // 解密敏感信息
        for (Elder elder : elders) {
            String sanaNameName = sanatoriumRepository.getNameById(elder.getSanaId());
            elder.setSanaName(sanaNameName);
            decryptElderInfo(elder);
        }

        return elders;
    }

    @Override
    public List<ElderExcelDto> listForExport(ElderPageQueryDto queryDto) {
        requireResource(ELDER_EXPORT_PATH);
        ElderPageQueryDto exportQuery = queryDto == null ? new ElderPageQueryDto() : queryDto;
        exportQuery.setPage(1);
        exportQuery.setPageSize(MAX_UNPAGED_SIZE);
        PageResult result = pageForExport(exportQuery);
        List<?> records = result.getRecords() == null ? List.of() : result.getRecords();
        return records.stream()
                .filter(Elder.class::isInstance)
                .map(Elder.class::cast)
                .map(this::toExcelDto)
                .toList();
    }

    private PageResult pageForExport(ElderPageQueryDto elderPageQueryDto) {
        elderPageQueryDto.setOccupiedBedType(normalizeBedType(elderPageQueryDto.getOccupiedBedType()));
        if (!securityHelper.hasGovAdminRoleForSensitiveOperation()) {
            Set<Long> scopeIds = securityHelper.requireCurrentSanaScopeIdsForSensitiveOperation();
            elderPageQueryDto.setSanaScopeIds(scopeIds);
            Long requestedSanaId = elderPageQueryDto.getSanaId();
            if (requestedSanaId != null && !scopeIds.contains(requestedSanaId)) {
                throw new AccessDeniedException("无权查询其他机构数据");
            }
        } else if (elderPageQueryDto.getSanaName() != null && !elderPageQueryDto.getSanaName().trim().isEmpty()) {
            elderPageQueryDto.setSanaId(sanatoriumRepository.selectIdByName(elderPageQueryDto.getSanaName()));
        }

        Page<Elder> page = new Page<>(elderPageQueryDto.getPage(), elderPageQueryDto.getPageSize());
        IPage<Elder> result = elderRepository.selectPage(page, elderPageQueryDto);
        for (Elder elder : result.getRecords()) {
            String sanaNameName = sanatoriumRepository.getNameById(elder.getSanaId());
            elder.setSanaName(sanaNameName);
            decryptElderInfo(elder);
        }
        return new PageResult(result.getTotal(), result.getRecords());
    }

    /**
     * 对老人敏感字段做入库加密，避免明文落库。
     *
     * @author zhanghongyu
     */
    private void encryptSensitiveFields(Elder elder) {
        if (elder.getIdNumber() != null && !elder.getIdNumber().trim().isEmpty()) {
            elder.setIdNumber(AESUtil.encrypt(elder.getIdNumber()));
        }
        if (elder.getPhoneNumber() != null && !elder.getPhoneNumber().trim().isEmpty()) {
            elder.setPhoneNumber(AESUtil.encrypt(elder.getPhoneNumber()));
        }
        if (elder.getHomeAddress() != null && !elder.getHomeAddress().trim().isEmpty()) {
            elder.setHomeAddress(AESUtil.encrypt(elder.getHomeAddress()));
        }
        if (elder.getGuardianPhone() != null && !elder.getGuardianPhone().trim().isEmpty()) {
            elder.setGuardianPhone(AESUtil.encrypt(elder.getGuardianPhone()));
        }
    }

    /**
     * 将已加密的老人敏感字段解密为展示态。
     *
     * @author zhanghongyu
     */
    private void decryptElderInfo(Elder elder) {
        String idNumber = elder.getIdNumber();
        if (idNumber != null) {
            elder.setIdNumber(AESUtil.decrypt(idNumber));
        }

        String phoneNumber = elder.getPhoneNumber();
        if (phoneNumber != null) {
            elder.setPhoneNumber(AESUtil.decrypt(phoneNumber));
        }

        String homeAddress = elder.getHomeAddress();
        if (homeAddress != null) {
            elder.setHomeAddress(AESUtil.decrypt(homeAddress));
        }

        String guardianPhone = elder.getGuardianPhone();
        if (guardianPhone != null) {
            elder.setGuardianPhone(AESUtil.decrypt(guardianPhone));
        }
    }

    /**
     * 页面展示态只返回脱敏身份证号，避免完整证件号进入浏览器内存。
     *
     * @author zhanghongyu
     */
    public void maskSensitiveFieldsForDisplay(Elder elder) {
        if (elder == null) {
            return;
        }
        elder.setIdNumber(maskIdNumber(elder.getIdNumber()));
    }

    private String maskIdNumber(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String normalized = value.trim();
        if (normalized.length() <= 7) {
            return normalized;
        }
        int maskLength = Math.max(1, normalized.length() - 10);
        return normalized.substring(0, 6) + "*".repeat(maskLength) + normalized.substring(normalized.length() - 4);
    }

    /**
     * 断言当前用户具备目标机构的数据操作权限。
     *
     * @author zhanghongyu
     */
    private void assertCanOperateSana(Long sanaId) {
        if (!securityHelper.canOperateSanaForSensitiveOperation(sanaId)) {
            throw new AccessDeniedException("无权操作其他机构数据");
        }
    }

    /**
     * 批量导入老人数据并返回成功/失败明细。
     *
     * @author zhanghongyu
     */
    @Override
    public ImportResult importElders(List<ElderImportExcelDto> rows) {
        requireResource(ELDER_IMPORT_PATH);
        List<ElderImportExcelDto> importRows = rows == null ? List.of() : rows;
        Set<Long> scopeIds = securityHelper.hasGovAdminRoleForSensitiveOperation()
                ? null
                : securityHelper.requireCurrentSanaScopeIdsForSensitiveOperation();
        int successCount = 0;
        int failCount = 0;
        List<String> failDetails = new ArrayList<>();

        for (int i = 0; i < importRows.size(); i++) {
            int excelRowNum = i + 2;
            ElderImportExcelDto row = importRows.get(i);
            if (!hasImportContent(row)) {
                continue;
            }

            List<String> rowErrors = new ArrayList<>();
            Elder elder = toElderEntity(row, scopeIds, rowErrors);
            String elderName = elder.getElderName() != null ? elder.getElderName() : "未知姓名";
            if (!rowErrors.isEmpty()) {
                failCount++;
                failDetails.add(String.format("第%s行（%s）：%s", excelRowNum, elderName, String.join("；", rowErrors)));
                continue;
            }

            try {
                encryptSensitiveFields(elder);
                Boolean insertResult = elderRepository.insert(elder);
                if (insertResult != null && insertResult) {
                    successCount++;
                } else {
                    failCount++;
                    failDetails.add(String.format("第%s行（%s）：数据库执行失败", excelRowNum, elderName));
                }
            } catch (Exception e) {
                failCount++;
                failDetails.add(String.format("第%s行（%s）：导入失败 - %s", excelRowNum, elderName, e.getMessage()));
            }
        }

        return new ImportResult(successCount, failCount, failDetails);
    }

    private ElderExcelDto toExcelDto(Elder elder) {
        ElderExcelDto row = new ElderExcelDto();
        row.setSanaName(elder.getSanaName());
        row.setElderName(elder.getElderName());
        row.setSex(formatSex(elder.getSex()));
        row.setAge(elder.getAge() == null ? null : String.valueOf(elder.getAge()));
        row.setIdNumber(elder.getIdNumber());
        row.setPhoneNumber(elder.getPhoneNumber());
        row.setHomeAddress(elder.getHomeAddress());
        row.setFamilySituation(formatFamilySituation(elder.getFamilySituation()));
        row.setOccupiedBedType(formatOccupiedBedType(elder.getOccupiedBedType()));
        row.setRoomNumber(elder.getRoomNumber());
        row.setGuardianName(elder.getGuardianName());
        row.setGuardianPhone(elder.getGuardianPhone());
        row.setSelfCare(formatSelfCare(elder.getSelfCare()));
        row.setInpatientsTime(formatDate(elder.getInpatientsTime()));
        row.setOutpatientsTime(formatDate(elder.getOutpatientsTime()));
        row.setFees(elder.getFees() == null ? null : String.format("%.2f", elder.getFees()));
        return row;
    }

    private Elder toElderEntity(ElderImportExcelDto row, Set<Long> scopeIds, List<String> rowErrors) {
        Elder elder = new Elder();
        String sanaName = trimToNull(row.getSanaName());
        if (sanaName == null) {
            rowErrors.add("所属机构名称不能为空");
        } else {
            Sanatorium sanatorium = sanatoriumRepository.selectByExactName(sanaName);
            if (sanatorium == null) {
                rowErrors.add("所属机构名称不存在，请填写系统中已有机构的完整名称");
            } else {
                Long sanaId = sanatorium.getId();
                elder.setSanaId(sanaId);
                elder.setSanaName(sanatorium.getSanaName());
                if (scopeIds != null && !scopeIds.contains(sanaId)) {
                    rowErrors.add("无权导入该机构数据");
                }
            }
        }

        String elderName = trimToNull(row.getElderName());
        elder.setElderName(elderName);
        if (elderName == null) {
            rowErrors.add("老人姓名不能为空");
        } else if (elderName.length() > 20) {
            rowErrors.add("老人姓名长度不能超过20个字");
        }

        elder.setSex(parseSex(row.getSex(), rowErrors));
        elder.setAge(parseInteger(row.getAge(), "年龄", rowErrors));
        elder.setIdNumber(trimToNull(row.getIdNumber()));
        elder.setPhoneNumber(trimToNull(row.getPhoneNumber()));
        elder.setHomeAddress(trimToNull(row.getHomeAddress()));
        elder.setFamilySituation(parseFamilySituation(row.getFamilySituation(), rowErrors));
        elder.setOccupiedBedType(parseOccupiedBedType(row.getOccupiedBedType(), rowErrors));
        elder.setRoomNumber(trimToNull(row.getRoomNumber()));

        String guardianName = trimToNull(row.getGuardianName());
        elder.setGuardianName(guardianName);
        if (guardianName != null && guardianName.length() > 50) {
            rowErrors.add("监护人姓名长度不能超过50个字");
        }
        elder.setGuardianPhone(trimToNull(row.getGuardianPhone()));
        elder.setSelfCare(parseSelfCare(row.getSelfCare(), rowErrors));
        elder.setInpatientsTime(parseDate(row.getInpatientsTime(), "入院时间", rowErrors));
        elder.setOutpatientsTime(parseDate(row.getOutpatientsTime(), "出院时间", rowErrors));
        elder.setFees(parseFloat(row.getFees(), "收费标准", rowErrors));
        elder.setStatus(parseStatus(row.getStatus(), rowErrors));
        return elder;
    }

    private boolean hasImportContent(ElderImportExcelDto row) {
        if (row == null) {
            return false;
        }
        return trimToNull(row.getSanaName()) != null
                || trimToNull(row.getElderName()) != null
                || trimToNull(row.getSex()) != null
                || trimToNull(row.getAge()) != null
                || trimToNull(row.getIdNumber()) != null
                || trimToNull(row.getPhoneNumber()) != null
                || trimToNull(row.getHomeAddress()) != null
                || trimToNull(row.getFamilySituation()) != null
                || trimToNull(row.getOccupiedBedType()) != null
                || trimToNull(row.getRoomNumber()) != null
                || trimToNull(row.getGuardianName()) != null
                || trimToNull(row.getGuardianPhone()) != null
                || trimToNull(row.getSelfCare()) != null
                || trimToNull(row.getInpatientsTime()) != null
                || trimToNull(row.getOutpatientsTime()) != null
                || trimToNull(row.getFees()) != null
                || trimToNull(row.getStatus()) != null;
    }

    private String formatSex(Integer value) {
        return switch (value == null ? 2 : value) {
            case 0 -> "男";
            case 1 -> "女";
            default -> "未知";
        };
    }

    private Integer parseSex(String value, List<String> rowErrors) {
        String normalized = normalizeLabel(value);
        if (normalized == null) {
            return null;
        }
        return switch (normalized) {
            case "男", "男性", "0" -> 0;
            case "女", "女性", "1" -> 1;
            case "未知", "2" -> 2;
            default -> {
                rowErrors.add("性别仅支持男、女、未知");
                yield null;
            }
        };
    }

    private String formatFamilySituation(Integer value) {
        return switch (value == null ? 0 : value) {
            case 1 -> "低保";
            case 2 -> "特困老年人";
            case 3 -> "建档立卡";
            default -> "社会老年人";
        };
    }

    private Integer parseFamilySituation(String value, List<String> rowErrors) {
        String normalized = normalizeLabel(value);
        if (normalized == null) {
            return null;
        }
        return switch (normalized) {
            case "0", "社会老年人", "社会老人" -> 0;
            case "1", "低保", "低保老年人" -> 1;
            case "2", "特困", "特困老年人" -> 2;
            case "3", "建档立卡" -> 3;
            default -> {
                rowErrors.add("入住类型仅支持社会老年人、低保、特困、建档立卡");
                yield null;
            }
        };
    }

    private String formatOccupiedBedType(Integer value) {
        return BedTypeEnum.NURSING.getCode().equals(value) ? "护理型床位" : "普通床位";
    }

    private Integer parseOccupiedBedType(String value, List<String> rowErrors) {
        String normalized = normalizeLabel(value);
        if (normalized == null) {
            return null;
        }
        return switch (normalized) {
            case "0", "普通床位" -> 0;
            case "1", "护理型床位", "护理床位" -> 1;
            default -> {
                rowErrors.add("床位类型仅支持普通床位、护理型床位");
                yield null;
            }
        };
    }

    private String formatSelfCare(Integer value) {
        return switch (value == null ? 0 : value) {
            case 1 -> "轻度失能";
            case 2 -> "中度失能";
            case 3 -> "重度失能";
            case 4 -> "完全失能";
            default -> "能力完好";
        };
    }

    private Integer parseSelfCare(String value, List<String> rowErrors) {
        String normalized = normalizeLabel(value);
        if (normalized == null) {
            return null;
        }
        return switch (normalized) {
            case "0", "能力完好" -> 0;
            case "1", "轻度", "轻度失能" -> 1;
            case "2", "中度", "中度失能" -> 2;
            case "3", "重度", "重度失能" -> 3;
            case "4", "完全失能" -> 4;
            default -> {
                rowErrors.add("自理能力仅支持能力完好、轻度失能、中度失能、重度失能、完全失能");
                yield null;
            }
        };
    }

    private String formatDate(LocalDate value) {
        return value == null ? null : value.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private LocalDate parseDate(String value, String fieldName, List<String> rowErrors) {
        String normalized = trimToNull(value);
        if (normalized == null || "0".equals(normalized)) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // 尝试下一种格式
            }
        }
        rowErrors.add(fieldName + "格式不支持，请使用 yyyy-MM-dd");
        return null;
    }

    private Integer parseInteger(String value, String fieldName, List<String> rowErrors) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Integer.valueOf(normalized.replaceAll("\\.0$", ""));
        } catch (NumberFormatException e) {
            rowErrors.add(fieldName + "必须为整数");
            return null;
        }
    }

    private Float parseFloat(String value, String fieldName, List<String> rowErrors) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Float.valueOf(normalized.replaceAll("[^0-9.\\-]", ""));
        } catch (NumberFormatException e) {
            rowErrors.add(fieldName + "必须为数字");
            return null;
        }
    }

    private Integer parseStatus(String value, List<String> rowErrors) {
        String normalized = normalizeLabel(value);
        if (normalized == null) {
            return null;
        }
        return switch (normalized) {
            case "0", "正常", "启用", "正常/启用" -> 0;
            case "1", "禁用", "停用", "停用/禁用" -> 1;
            default -> {
                rowErrors.add("状态仅支持正常、禁用");
                yield null;
            }
        };
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
     * 标准化床位类型，仅允许普通床位和护理型床位。
     *
     * @author zhanghongyu
     */
    private Integer normalizeBedType(Integer bedType) {
        if (bedType == null) {
            return null;
        }
        if (bedType.equals(BedTypeEnum.NORMAL.getCode()) || bedType.equals(BedTypeEnum.NURSING.getCode())) {
            return bedType;
        }
        throw new IllegalArgumentException("床位类型仅支持：0(普通床位)、1(护理型床位)");
    }

    private void requireResource(String resourcePath) {
        if (!securityHelper.hasResourcePathForSensitiveOperation(resourcePath)) {
            throw new AccessDeniedException("无权访问老人信息功能");
        }
    }
}


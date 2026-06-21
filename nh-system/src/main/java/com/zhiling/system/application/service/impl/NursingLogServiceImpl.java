package com.zhiling.system.application.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.common.constant.RoleConstant;
import com.zhiling.common.exception.ProjectException;
import com.zhiling.common.result.PageResult;
import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.model.dto.NursingLogAddDto;
import com.zhiling.model.dto.NursingLogExportDto;
import com.zhiling.model.dto.NursingLogMyPageQueryDto;
import com.zhiling.model.dto.NursingLogPageQueryDto;
import com.zhiling.model.dto.NursingLogUpdateDto;
import com.zhiling.model.entity.Elder;
import com.zhiling.model.entity.NursingLog;
import com.zhiling.model.entity.NursingTask;
import com.zhiling.system.application.repository.ElderRepository;
import com.zhiling.system.application.repository.NursingLogRepository;
import com.zhiling.system.application.repository.NursingTaskRepository;
import com.zhiling.system.application.service.CommonFileService;
import com.zhiling.system.application.service.NursingLogService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 护理日志服务实现
 *
 * @author zhanghongyu
 */
@Service
public class NursingLogServiceImpl implements NursingLogService {
    private static final Logger log = LoggerFactory.getLogger(NursingLogServiceImpl.class);

    private static final int ABNORMAL_NO = 0;
    private static final int ABNORMAL_YES = 1;
    private static final String LOG_PAGE_PATH = "/web/nursing-log/page";
    private static final String LOG_MY_PAGE_PATH = "/web/nursing-log/my/page";
    private static final String LOG_ADD_PATH = "/web/nursing-log/add";
    private static final String LOG_UPDATE_PATH = "/web/nursing-log/update/**";
    private static final String LOG_EXPORT_PATH = "/web/nursing-log/export";

    private final NursingLogRepository nursingLogRepository;
    private final NursingTaskRepository nursingTaskRepository;
    private final ElderRepository elderRepository;
    private final SecurityHelper securityHelper;
    private final CommonFileService commonFileService;

    /**
     * 护理日志服务依赖注入构造器。
     *
     * @author zhanghongyu
     */
    public NursingLogServiceImpl(NursingLogRepository nursingLogRepository,
                                 NursingTaskRepository nursingTaskRepository,
                                 ElderRepository elderRepository,
                                 SecurityHelper securityHelper,
                                 CommonFileService commonFileService) {
        this.nursingLogRepository = nursingLogRepository;
        this.nursingTaskRepository = nursingTaskRepository;
        this.elderRepository = elderRepository;
        this.securityHelper = securityHelper;
        this.commonFileService = commonFileService;
    }

    /**
     * 机构侧分页查询护理日志。
     *
     * @author zhanghongyu
     */
    @Override
    public PageResult page(NursingLogPageQueryDto queryDto) {
        try {
            requireResource("log.page", LOG_PAGE_PATH);
            ensureOrgManagerRole("log.page");
            Set<Long> scopeIds = requireScopeIds("log.page");
            NursingLogPageQueryDto safeDto = queryDto == null ? new NursingLogPageQueryDto() : queryDto;
            if (safeDto.getSanaId() != null && !scopeIds.contains(safeDto.getSanaId())) {
                log.warn("log.page forbidden: userId={}, requestSanaId={}, scopeIds={}",
                        securityHelper.getCurrentUserId(), safeDto.getSanaId(), scopeIds);
                throw new ProjectException(403, "无权查询其他机构护理日志");
            }
            safeDto.setSanaScopeIds(scopeIds);

            Page<NursingLog> page = new Page<>(resolvePageNo(safeDto.getPage()), resolvePageSize(safeDto.getPageSize()));
            IPage<NursingLog> result = nursingLogRepository.page(page, safeDto);
            return new PageResult(result.getTotal(), result.getRecords());
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            log.error("log.page failed, query={}", queryDto, e);
            throw new ProjectException(500, "护理日志分页查询失败");
        }
    }

    /**
     * 护理端查询“我的日志”分页。
     *
     * @author zhanghongyu
     */
    @Override
    public PageResult myPage(NursingLogMyPageQueryDto queryDto) {
        try {
            requireResource("log.myPage", LOG_MY_PAGE_PATH);
            ensureNurseRole("log.myPage");
            Set<Long> scopeIds = requireScopeIds("log.myPage");
            NursingLogMyPageQueryDto safeDto = queryDto == null ? new NursingLogMyPageQueryDto() : queryDto;
            if (safeDto.getSanaId() != null && !scopeIds.contains(safeDto.getSanaId())) {
                log.warn("log.myPage forbidden: userId={}, requestSanaId={}, scopeIds={}",
                        securityHelper.getCurrentUserId(), safeDto.getSanaId(), scopeIds);
                throw new ProjectException(403, "无权查询其他机构护理日志");
            }
            safeDto.setNurseUserId(securityHelper.requireCurrentUserId());
            safeDto.setSanaScopeIds(scopeIds);

            Page<NursingLog> page = new Page<>(resolvePageNo(safeDto.getPage()), resolvePageSize(safeDto.getPageSize()));
            IPage<NursingLog> result = nursingLogRepository.myPage(page, safeDto);
            return new PageResult(result.getTotal(), result.getRecords());
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            log.error("log.myPage failed, query={}", queryDto, e);
            throw new ProjectException(500, "我的护理日志分页查询失败");
        }
    }

    /**
     * 新增护理日志（护理员侧）。
     *
     * @author zhanghongyu
     */
    @Override
    @Transactional
    public Boolean add(NursingLogAddDto addDto) {
        try {
            requireResource("log.add", LOG_ADD_PATH);
            ensureNurseRole("log.add");
            if (addDto == null) {
                throw new ProjectException(400, "请求参数不能为空");
            }
            if (!StringUtils.hasText(addDto.getContent())) {
                throw new ProjectException(400, "日志内容不能为空");
            }

            Long currentUserId = securityHelper.requireCurrentUserId();
            Set<Long> scopeIds = requireScopeIds("log.add");
            Long targetSanaId = resolveTargetSanaId(addDto.getSanaId(), scopeIds, "log.add");

            validateTaskForNurse(addDto.getTaskId(), targetSanaId, currentUserId, "log.add");
            validateElderInSana(addDto.getElderId(), targetSanaId, "log.add");
            validateTaskLogUniqueness(addDto.getTaskId(), currentUserId, null, "log.add");
            Integer abnormalFlag = normalizeAbnormalFlag(addDto.getAbnormalFlag(), "log.add");

            NursingLog nursingLog = NursingLog.builder()
                    .sanaId(targetSanaId)
                    .taskId(addDto.getTaskId())
                    .elderId(addDto.getElderId())
                    .nurseUserId(currentUserId)
                    .logTime(addDto.getLogTime() == null ? LocalDateTime.now() : addDto.getLogTime())
                    .content(addDto.getContent())
                    .abnormalFlag(abnormalFlag)
                    .attachmentUrls(addDto.getAttachmentUrls())
                    .build();
            boolean ok = nursingLogRepository.insert(nursingLog);
            if (!ok) {
                throw new ProjectException(500, "护理日志新增失败");
            }
            return true;
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            log.error("log.add failed, dto={}", addDto, e);
            throw new ProjectException(500, "护理日志新增失败");
        }
    }

    /**
     * 修改护理日志（仅本人）。
     *
     * @author zhanghongyu
     */
    @Override
    @Transactional
    public Boolean update(Long id, NursingLogUpdateDto updateDto) {
        try {
            requireResource("log.update", LOG_UPDATE_PATH);
            ensureNurseRole("log.update");
            if (id == null) {
                throw new ProjectException(400, "日志ID不能为空");
            }
            if (updateDto == null) {
                throw new ProjectException(400, "请求参数不能为空");
            }

            Long currentUserId = securityHelper.requireCurrentUserId();
            Set<Long> scopeIds = requireScopeIds("log.update");
            NursingLog existing = nursingLogRepository.selectById(id);
            if (existing == null) {
                throw new ProjectException(404, "护理日志不存在");
            }
            if (!Objects.equals(currentUserId, existing.getNurseUserId())) {
                log.warn("log.update forbidden: not owner, userId={}, logId={}, nurseUserId={}",
                        currentUserId, id, existing.getNurseUserId());
                throw new ProjectException(403, "仅可修改本人护理日志");
            }
            if (!scopeIds.contains(existing.getSanaId())) {
                log.warn("log.update forbidden: userId={}, logId={}, logSanaId={}, scopeIds={}",
                        currentUserId, id, existing.getSanaId(), scopeIds);
                throw new ProjectException(403, "无权修改其他机构护理日志");
            }
            if (updateDto.getSanaId() != null && !Objects.equals(updateDto.getSanaId(), existing.getSanaId())) {
                throw new ProjectException(400, "不可修改日志所属机构");
            }
            if (updateDto.getContent() != null && !StringUtils.hasText(updateDto.getContent())) {
                throw new ProjectException(400, "日志内容不能为空");
            }

            Long targetSanaId = existing.getSanaId();
            Long targetTaskId = updateDto.getTaskId() != null ? updateDto.getTaskId() : existing.getTaskId();
            validateTaskForNurse(targetTaskId, targetSanaId, currentUserId, "log.update");
            validateTaskLogUniqueness(targetTaskId, currentUserId, existing.getId(), "log.update");
            if (updateDto.getElderId() != null) {
                validateElderInSana(updateDto.getElderId(), targetSanaId, "log.update");
            }
            Integer abnormalFlag = updateDto.getAbnormalFlag() == null
                    ? null : normalizeAbnormalFlag(updateDto.getAbnormalFlag(), "log.update");

            NursingLog patch = NursingLog.builder()
                    .id(id)
                    .taskId(updateDto.getTaskId())
                    .elderId(updateDto.getElderId())
                    .logTime(updateDto.getLogTime())
                    .content(updateDto.getContent())
                    .abnormalFlag(abnormalFlag)
                    .attachmentUrls(updateDto.getAttachmentUrls())
                    .build();
            boolean ok = nursingLogRepository.updateById(patch);
            if (!ok) {
                throw new ProjectException(500, "护理日志更新失败");
            }
            return true;
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            log.error("log.update failed, id={}, dto={}", id, updateDto, e);
            throw new ProjectException(500, "护理日志更新失败");
        }
    }

    /**
     * 按筛选条件导出护理日志，支持 PDF/DOCX 报告与附件 ZIP 打包。
     *
     * @author zhanghongyu
     */
    @Override
    public byte[] export(NursingLogExportDto exportDto) {
        try {
            // 导出接口仅允许机构侧角色调用；护士写日志但不负责机构级批量导出。
            requireResource("log.export", LOG_EXPORT_PATH);
            ensureOrgManagerRole("log.export");
            Set<Long> scopeIds = requireScopeIds("log.export");
            NursingLogExportDto safeDto = exportDto == null ? new NursingLogExportDto() : exportDto;
            if (safeDto.getSanaId() != null && !scopeIds.contains(safeDto.getSanaId())) {
                log.warn("log.export forbidden: userId={}, requestSanaId={}, scopeIds={}",
                        securityHelper.getCurrentUserId(), safeDto.getSanaId(), scopeIds);
                throw new ProjectException(403, "无权导出其他机构护理日志");
            }
            // logIds 允许前端“单选/多选导出”，会在这里去重、过滤非法值并做数量上限保护。
            List<Long> safeLogIds = normalizeLogIds(safeDto.getLogIds(), "log.export");
            safeDto.setLogIds(safeLogIds);
            safeDto.setSanaScopeIds(scopeIds);

            List<NursingLog> logs = listLogsForExport(safeDto);
            if (!safeLogIds.isEmpty()) {
                // 强校验：前端选中的每个 ID 都必须命中机构权限范围，避免“部分导出成功”造成误解。
                Set<Long> matchedIds = new HashSet<>();
                for (NursingLog logItem : logs) {
                    if (logItem.getId() != null) {
                        matchedIds.add(logItem.getId());
                    }
                }
                if (matchedIds.size() != safeLogIds.size()) {
                    log.warn("log.export bad request: selected logIds not fully matched, userId={}, selectedSize={}, matchedSize={}",
                            securityHelper.getCurrentUserId(), safeLogIds.size(), matchedIds.size());
                    throw new ProjectException(400, "部分选中日志不存在或无权限导出，请刷新后重试");
                }
            }
            boolean includeAttachments = safeDto.getIncludeAttachments() == null || safeDto.getIncludeAttachments();
            String reportFormat = normalizeReportFormat(safeDto.getReportFormat());

            // 报告与附件最终统一打包为 ZIP，便于离线归档与审计留痕。
            byte[] reportBytes = "pdf".equals(reportFormat)
                    ? buildPdfReport(logs)
                    : buildDocxReport(logs);
            String reportEntryName = "report/nursing-log-report." + reportFormat;

            byte[] zipBytes = buildExportZip(logs, reportBytes, reportEntryName, includeAttachments);
            log.info("log.export success: operatorUserId={}, reportFormat={}, includeAttachments={}, recordCount={}, zipBytes={}",
                    securityHelper.getCurrentUserId(), reportFormat, includeAttachments, logs.size(), zipBytes.length);
            return zipBytes;
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            log.error("log.export failed, dto={}", exportDto, e);
            throw new ProjectException(500, "护理日志导出失败");
        }
    }

    /**
     * 采用分页拉全量，避免一次性查询过大导致内存峰值和 SQL 压力。
     * pageSize 固定为 500，兼顾导出速度和单页稳定性。
     *
     * @author zhanghongyu
     */
    private List<NursingLog> listLogsForExport(NursingLogExportDto exportDto) {
        List<NursingLog> allLogs = new ArrayList<>();
        int pageNo = 1;
        int pageSize = 500;
        while (true) {
            NursingLogPageQueryDto pageQueryDto = NursingLogPageQueryDto.builder()
                    .page(pageNo)
                    .pageSize(pageSize)
                    .sanaId(exportDto.getSanaId())
                    .taskId(exportDto.getTaskId())
                    .elderId(exportDto.getElderId())
                    .nurseUserId(exportDto.getNurseUserId())
                    .abnormalFlag(exportDto.getAbnormalFlag())
                    .content(exportDto.getContent())
                    .logTimeBegin(exportDto.getLogTimeBegin())
                    .logTimeEnd(exportDto.getLogTimeEnd())
                    .sanaScopeIds(exportDto.getSanaScopeIds())
                    .logIds(exportDto.getLogIds())
                    .build();
            Page<NursingLog> page = new Page<>(pageNo, pageSize);
            IPage<NursingLog> result = nursingLogRepository.page(page, pageQueryDto);
            List<NursingLog> records = result.getRecords() == null ? List.of() : result.getRecords();
            allLogs.addAll(records);
            if (records.size() < pageSize || allLogs.size() >= result.getTotal()) {
                break;
            }
            pageNo++;
        }
        return allLogs;
    }

    /**
     * 规范化前端传入的 logIds：
     * 1) 过滤 null/非正数；2) 去重且保持顺序；3) 保护上限防止超大导出请求。
     *
     * @author zhanghongyu
     */
    private List<Long> normalizeLogIds(List<Long> rawLogIds, String scene) {
        if (rawLogIds == null || rawLogIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (Long logId : rawLogIds) {
            if (logId == null || logId <= 0) {
                continue;
            }
            uniqueIds.add(logId);
        }
        if (uniqueIds.isEmpty()) {
            return List.of();
        }
        if (uniqueIds.size() > 1000) {
            log.warn("{} bad request: too many logIds, userId={}, size={}",
                    scene, securityHelper.getCurrentUserId(), uniqueIds.size());
            throw new ProjectException(400, "一次最多导出 1000 条日志");
        }
        return new ArrayList<>(uniqueIds);
    }

    /**
     * 规范化导出报告格式，仅允许 pdf/docx。
     *
     * @author zhanghongyu
     */
    private String normalizeReportFormat(String reportFormat) {
        String format = (reportFormat == null ? "" : reportFormat.trim().toLowerCase());
        // 仅开放白名单格式，防止无效参数导致分支歧义。
        if ("pdf".equals(format) || "docx".equals(format)) {
            return format;
        }
        return "docx";
    }

    /**
     * 构建 DOCX 报告正文。
     *
     * @author zhanghongyu
     */
    private byte[] buildDocxReport(List<NursingLog> logs) throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFParagraph titleParagraph = document.createParagraph();
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText("护理日志导出报告");
            titleRun.setBold(true);
            titleRun.setFontSize(16);

            XWPFParagraph metaParagraph = document.createParagraph();
            XWPFRun metaRun = metaParagraph.createRun();
            metaRun.setText("导出时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            metaRun.addBreak();
            metaRun.setText("日志条数: " + logs.size());

            for (int i = 0; i < logs.size(); i++) {
                NursingLog logItem = logs.get(i);
                XWPFParagraph rowParagraph = document.createParagraph();
                XWPFRun rowRun = rowParagraph.createRun();
                rowRun.setBold(true);
                rowRun.setText("[" + (i + 1) + "] 日志ID=" + safeLong(logItem.getId()) + " 任务=" + safeText(logItem.getTaskTitle()));
                rowRun.addBreak();
                rowRun.setBold(false);
                rowRun.setText("机构: " + safeText(logItem.getSanaName()) + " 护理员: " + safeText(logItem.getNurseUsername()) + " 老人: " + safeText(logItem.getElderName()));
                rowRun.addBreak();
                rowRun.setText("日志时间: " + formatDateTime(logItem.getLogTime()) + " 异常标记: " + (Objects.equals(logItem.getAbnormalFlag(), ABNORMAL_YES) ? "异常" : "正常"));
                rowRun.addBreak();
                rowRun.setText("内容: " + safeText(logItem.getContent()));
                List<String> attachments = splitAttachmentUrls(logItem.getAttachmentUrls());
                if (!attachments.isEmpty()) {
                    rowRun.addBreak();
                    rowRun.setText("附件:");
                    for (int attachmentIndex = 0; attachmentIndex < attachments.size(); attachmentIndex++) {
                        rowRun.addBreak();
                        rowRun.setText(" - " + resolveDisplayFileName(resolveFileNameFromUrl(attachments.get(attachmentIndex), attachmentIndex + 1)));
                    }
                }
            }
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * 构建 PDF 报告正文（中文模板）。
     *
     * @author zhanghongyu
     */
    private byte[] buildPdfReport(List<NursingLog> logs) throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // 先拼“语义行”，再统一进入排版阶段（换页、换行、字体处理），避免模板逻辑与版式耦合。
            List<String> lines = new ArrayList<>();
            lines.add("护理日志导出报告");
            lines.add("导出时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            lines.add("日志条数: " + logs.size());
            lines.add(" ");
            for (int i = 0; i < logs.size(); i++) {
                NursingLog logItem = logs.get(i);
                lines.add("[" + (i + 1) + "] 日志 ID=" + safeLong(logItem.getId()) + " 任务=" + safeText(logItem.getTaskTitle()));
                lines.add("机构: " + safeText(logItem.getSanaName()) + " 护理员: " + safeText(logItem.getNurseUsername())
                        + " 老人: " + safeText(logItem.getElderName()));
                lines.add("日志时间: " + formatDateTime(logItem.getLogTime()) + " 异常标记: " + (Objects.equals(logItem.getAbnormalFlag(), ABNORMAL_YES) ? "异常" : "正常"));
                lines.add("内容: " + safeText(logItem.getContent()));
                List<String> attachments = splitAttachmentUrls(logItem.getAttachmentUrls());
                if (!attachments.isEmpty()) {
                    StringBuilder builder = new StringBuilder("附件: ");
                    for (int attachmentIndex = 0; attachmentIndex < attachments.size(); attachmentIndex++) {
                        if (attachmentIndex > 0) {
                            builder.append("；");
                        }
                        builder.append(resolveDisplayFileName(resolveFileNameFromUrl(attachments.get(attachmentIndex), attachmentIndex + 1)));
                    }
                    lines.add(builder.toString());
                }
                lines.add(" ");
            }
            writePdfLines(document, lines);
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * 将文本行写入 PDF，并负责分页与逐行落笔。
     *
     * @author zhanghongyu
     */
    private void writePdfLines(PDDocument document, List<String> lines) throws Exception {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PDPageContentStream stream = new PDPageContentStream(document, page);
        PDFont pdfFont = resolvePdfFont(document);
        stream.setFont(pdfFont, 10);
        boolean hasUnicodeFont = pdfFont instanceof PDType0Font;
        float x = 40f;
        float y = 800f;
        float lineHeight = 14f;
        float maxWidth = PDRectangle.A4.getWidth() - 80f;
        for (String line : lines) {
            // PDFBox 不会自动换行；先按字体实际宽度切行，再逐行写入。
            List<String> wrappedLines = wrapPdfLine(line, pdfFont, 10f, maxWidth);
            for (String wrappedLine : wrappedLines) {
                if (y < 50f) {
                    stream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    stream = new PDPageContentStream(document, page);
                    stream.setFont(pdfFont, 10);
                    y = 800f;
                }
                stream.beginText();
                stream.newLineAtOffset(x, y);
                stream.showText(hasUnicodeFont ? safePdfUnicodeText(wrappedLine) : safePdfAsciiText(wrappedLine));
                stream.endText();
                y -= lineHeight;
            }
        }
        stream.close();
    }

    /**
     * 基于字体宽度对单行文本进行自动换行。
     *
     * @author zhanghongyu
     */
    private List<String> wrapPdfLine(String line, PDFont font, float fontSize, float maxWidth) {
        String safe = line == null ? "" : line;
        if (safe.isEmpty()) {
            return List.of("");
        }
        List<String> wrapped = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < safe.length(); i++) {
            char ch = safe.charAt(i);
            current.append(ch);
            if (ch == '\n') {
                wrapped.add(current.toString().replace("\n", ""));
                current = new StringBuilder();
                continue;
            }
            if (pdfTextWidth(current.toString(), font, fontSize) > maxWidth) {
                current.deleteCharAt(current.length() - 1);
                if (current.isEmpty()) {
                    wrapped.add(String.valueOf(ch));
                } else {
                    wrapped.add(current.toString());
                    current = new StringBuilder().append(ch);
                }
            }
        }
        if (!current.isEmpty()) {
            wrapped.add(current.toString());
        }
        return wrapped;
    }

    /**
     * 计算字符串在当前字体大小下的渲染宽度。
     *
     * @author zhanghongyu
     */
    private float pdfTextWidth(String text, PDFont font, float fontSize) {
        try {
            return font.getStringWidth(text) / 1000f * fontSize;
        } catch (Exception e) {
            // 字体宽度计算失败时兜底估算，保证导出流程不中断。
            return text.length() * fontSize;
        }
    }

    /**
     * PDF 字体加载策略：
     * - 优先加载常见 CJK 字体（Windows/Linux/macOS），确保中文可读；
     * - 全部失败才回退 Helvetica（会丢失 CJK，日志里会告警）。
     *
     * @author zhanghongyu
     */
    private PDFont resolvePdfFont(PDDocument document) {
        List<Path> candidatePaths = List.of(
                Path.of("C:/Windows/Fonts/msyh.ttf"),
                Path.of("C:/Windows/Fonts/simhei.ttf"),
                Path.of("C:/Windows/Fonts/simsun.ttc"),
                Path.of("/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc"),
                Path.of("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"),
                Path.of("/System/Library/Fonts/PingFang.ttc")
        );
        for (Path path : candidatePaths) {
            try {
                if (!Files.exists(path)) {
                    continue;
                }
                try (InputStream inputStream = Files.newInputStream(path)) {
                    PDFont font = PDType0Font.load(document, inputStream);
                    log.info("log.export pdf font loaded: {}", path);
                    return font;
                }
            } catch (Exception e) {
                log.warn("log.export pdf font load failed, path={}, err={}", path, e.getMessage());
            }
        }
        log.warn("log.export pdf font fallback to Helvetica, CJK may be garbled");
        return PDType1Font.HELVETICA;
    }

    /**
     * ZIP 结构固定为 report/ + attachments/ + manifest.txt：
     * - report：主报告（pdf/docx）
     * - attachments：原始附件归档
     * - manifest：索引与缺失记录，便于后续审计排查
     *
     * @author zhanghongyu
     */
    private byte[] buildExportZip(List<NursingLog> logs, byte[] reportBytes, String reportEntryName, boolean includeAttachments) throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            zipOutputStream.putNextEntry(new ZipEntry(reportEntryName));
            zipOutputStream.write(reportBytes);
            zipOutputStream.closeEntry();

            List<String> missingAttachments = new ArrayList<>();
            AtomicInteger attachmentCounter = new AtomicInteger(1);
            Set<String> seenUrls = new HashSet<>();
            LinkedHashMap<Long, List<String>> logAttachmentMap = new LinkedHashMap<>();

            if (includeAttachments) {
                for (NursingLog logItem : logs) {
                    List<String> downloadedNames = new ArrayList<>();
                    List<String> attachmentUrls = splitAttachmentUrls(logItem.getAttachmentUrls());
                    for (int index = 0; index < attachmentUrls.size(); index++) {
                        String url = attachmentUrls.get(index);
                        // 同一 URL 在多个日志里重复出现时仅打包一次，减少 ZIP 体积。
                        if (!StringUtils.hasText(url) || !seenUrls.add(url)) {
                            continue;
                        }
                        byte[] bytes = commonFileService.downloadAsBytesByUrl(url);
                        if (bytes == null || bytes.length == 0) {
                            missingAttachments.add("logId=" + safeLong(logItem.getId()) + ", url=" + url);
                            continue;
                        }
                        String baseName = sanitizeFileName(resolveFileNameFromUrl(url, index + 1));
                        String finalName = attachmentCounter.getAndIncrement() + "-" + baseName;
                        String entryName = "attachments/" + finalName;
                        zipOutputStream.putNextEntry(new ZipEntry(entryName));
                        zipOutputStream.write(bytes);
                        zipOutputStream.closeEntry();
                        downloadedNames.add(entryName);
                    }
                    if (!downloadedNames.isEmpty()) {
                        logAttachmentMap.put(logItem.getId(), downloadedNames);
                    }
                }
            }

            byte[] manifestBytes = buildManifest(logs, includeAttachments, logAttachmentMap, missingAttachments);
            log.info("log.export manifest built: recordCount={}, includeAttachments={}, attachmentLogCount={}, missingAttachmentCount={}, manifestBytes={}",
                    logs.size(), includeAttachments, logAttachmentMap.size(), missingAttachments.size(), manifestBytes.length);
            zipOutputStream.putNextEntry(new ZipEntry("manifest.txt"));
            zipOutputStream.write(manifestBytes);
            zipOutputStream.closeEntry();
            zipOutputStream.finish();
            return outputStream.toByteArray();
        }
    }

    /**
     * 生成导出清单（manifest.txt）。
     *
     * @author zhanghongyu
     */
    private byte[] buildManifest(List<NursingLog> logs,
                                 boolean includeAttachments,
                                 LinkedHashMap<Long, List<String>> logAttachmentMap,
                                 List<String> missingAttachments) {
        final String line = "\r\n";
        StringBuilder builder = new StringBuilder();
        builder.append("nursing-log export manifest").append(line);
        builder.append("exportTime=").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append(line);
        builder.append("recordCount=").append(logs.size()).append(line);
        builder.append("includeAttachments=").append(includeAttachments).append(line);
        builder.append(line);
        builder.append("[log attachments]").append(line);
        if (logs.isEmpty()) {
            builder.append("- no log records matched current filters").append(line);
        } else {
            for (NursingLog logItem : logs) {
                List<String> entries = logAttachmentMap.getOrDefault(logItem.getId(), List.of());
                builder.append("logId=").append(safeLong(logItem.getId())).append(", files=");
                if (entries.isEmpty()) {
                    builder.append("-");
                } else {
                    builder.append(String.join(",", entries));
                }
                builder.append(line);
            }
        }
        builder.append(line);
        builder.append("[missing attachments]").append(line);
        if (missingAttachments.isEmpty()) {
            builder.append("-").append(line);
        } else {
            for (String missing : missingAttachments) {
                builder.append(missing).append(line);
            }
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 历史数据兼容：附件字段允许“逗号分隔字符串”。
     * 目前导出按该格式解析；若未来切换 JSON 存储，建议在此处做统一解码适配。
     *
     * @author zhanghongyu
     */
    private List<String> splitAttachmentUrls(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        String[] split = raw.split(",");
        for (String part : split) {
            if (StringUtils.hasText(part)) {
                result.add(part.trim());
            }
        }
        return result;
    }

    /**
     * 从附件 URL 解析文件名，失败时返回兜底名。
     *
     * @author zhanghongyu
     */
    private String resolveFileNameFromUrl(String url, int fallbackIndex) {
        try {
            if (!StringUtils.hasText(url)) {
                return "attachment-" + fallbackIndex;
            }
            String pureUrl = url.split("\\?")[0];
            int slashIndex = pureUrl.lastIndexOf('/');
            if (slashIndex < 0 || slashIndex >= pureUrl.length() - 1) {
                return "attachment-" + fallbackIndex;
            }
            String rawName = pureUrl.substring(slashIndex + 1);
            return java.net.URLDecoder.decode(rawName, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 文件名解析失败不影响导出主体，使用兜底名称保证流程可继续。
            return "attachment-" + fallbackIndex;
        }
    }

    /**
     * 展示名清洗规则：
     * - 去除路径前缀（如 20260417/）
     * - 去除对象存储常见 UUID 前缀
     * 目标是让导出的 PDF/DOCX 文本可读性接近原始文件名。
     *
     * @author zhanghongyu
     */
    private String resolveDisplayFileName(String decodedName) {
        if (!StringUtils.hasText(decodedName)) {
            return "attachment";
        }
        String name = decodedName.trim();
        String normalized = name.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0 && slash < normalized.length() - 1) {
            name = normalized.substring(slash + 1);
        }
        String withoutDatePrefix = name.replaceFirst("^\\d{8}/", "");
        return withoutDatePrefix.replaceFirst(
                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}-(.+)$",
                "$1"
        );
    }

    /**
     * 将文件名转换为可安全写入 ZIP 条目的名称。
     *
     * @author zhanghongyu
     */
    private String sanitizeFileName(String name) {
        String source = StringUtils.hasText(name) ? name : "attachment";
        String sanitized = source.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_").trim();
        return sanitized.isEmpty() ? "attachment" : sanitized;
    }

    /**
     * 空值友好文本。
     *
     * @author zhanghongyu
     */
    private String safeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "-";
    }

    /**
     * 空值友好数字（Long）。
     *
     * @author zhanghongyu
     */
    private String safeLong(Long value) {
        return value == null ? "-" : String.valueOf(value);
    }

    /**
     * 统一日期时间格式化。
     *
     * @author zhanghongyu
     */
    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * PDF Unicode 字体场景下的文本清洗。
     *
     * @author zhanghongyu
     */
    private String safePdfUnicodeText(String source) {
        if (source == null) {
            return "";
        }
        // PDF 文本流不接受原始 CR/LF，先规整为空格后再写入。
        String normalized = source
                .replace('\r', ' ')
                .replace('\n', ' ');
        return normalized.length() > 180 ? normalized.substring(0, 180) : normalized;
    }

    /**
     * PDF ASCII 回退字体场景下的文本清洗。
     *
     * @author zhanghongyu
     */
    private String safePdfAsciiText(String source) {
        if (source == null) {
            return "";
        }
        String ascii = source.replaceAll("[^\\x20-\\x7E]", "?");
        return ascii.length() > 180 ? ascii.substring(0, 180) : ascii;
    }

    /**
     * 校验当前用户是否拥有指定护理日志资源。
     *
     * @author zhanghongyu
     */
    private void requireResource(String scene, String resourcePath) {
        if (!securityHelper.hasResourcePathForSensitiveOperation(resourcePath)) {
            log.warn("{} forbidden: missing resource permission, userId={}, resourcePath={}",
                    scene, securityHelper.getCurrentUserId(), resourcePath);
            throw new ProjectException(403, "无权限执行该操作");
        }
    }

    /**
     * 校验机构侧角色权限。
     *
     * @author zhanghongyu
     */
    private void ensureOrgManagerRole(String scene) {
        // 护理日志模块按业务隔离：政府管理员不可直接访问机构日志明细。
        if (securityHelper.hasGovAdminRoleForSensitiveOperation()) {
            log.warn("{} forbidden: GOV_ADMIN, userId={}", scene, securityHelper.getCurrentUserId());
            throw new ProjectException(403, "政府管理员不可访问护理日志模块");
        }
        if (!securityHelper.hasAnyRoleForSensitiveOperation(RoleConstant.ORG_ADMIN, RoleConstant.PARENT_ORG_ADMIN)) {
            log.warn("{} forbidden: role not allowed, userId={}", scene, securityHelper.getCurrentUserId());
            throw new ProjectException(403, "无权限执行该操作");
        }
    }

    /**
     * 校验护理员角色权限。
     *
     * @author zhanghongyu
     */
    private void ensureNurseRole(String scene) {
        if (securityHelper.hasGovAdminRoleForSensitiveOperation()) {
            log.warn("{} forbidden: GOV_ADMIN, userId={}", scene, securityHelper.getCurrentUserId());
            throw new ProjectException(403, "政府管理员不可访问护理日志模块");
        }
        if (!securityHelper.hasAnyRoleForSensitiveOperation(RoleConstant.NURSE)) {
            log.warn("{} forbidden: nurse role required, userId={}", scene, securityHelper.getCurrentUserId());
            throw new ProjectException(403, "仅护理人员可执行该操作");
        }
    }

    /**
     * 获取当前用户机构范围，若缺失则抛出权限异常。
     *
     * @author zhanghongyu
     */
    private Set<Long> requireScopeIds(String scene) {
        try {
            return securityHelper.requireCurrentSanaScopeIdsForSensitiveOperation();
        } catch (Exception e) {
            // 无机构范围属于权限边界错误，不应继续执行默认全量查询。
            log.warn("{} forbidden: sana scope missing, userId={}", scene, securityHelper.getCurrentUserId());
            throw new ProjectException(403, "当前用户未绑定机构或无机构范围");
        }
    }

    /**
     * 根据请求参数与用户机构范围解析目标机构 ID。
     *
     * @author zhanghongyu
     */
    private Long resolveTargetSanaId(Long requestedSanaId, Set<Long> scopeIds, String scene) {
        if (requestedSanaId != null) {
            if (!scopeIds.contains(requestedSanaId)) {
                log.warn("{} forbidden: request sana out of scope, userId={}, requestSanaId={}, scopeIds={}",
                        scene, securityHelper.getCurrentUserId(), requestedSanaId, scopeIds);
                throw new ProjectException(403, "无权操作其他机构数据");
            }
            return requestedSanaId;
        }
        Long currentSanaId = securityHelper.getCurrentSanaId();
        if (currentSanaId != null && scopeIds.contains(currentSanaId)) {
            return currentSanaId;
        }
        if (scopeIds.size() == 1) {
            return scopeIds.iterator().next();
        }
        log.warn("{} bad request: multi-scope user without sanaId, userId={}, scopeIds={}",
                scene, securityHelper.getCurrentUserId(), scopeIds);
        throw new ProjectException(400, "当前账号已授权多个机构，请指定 sanaId");
    }

    /**
     * 校验任务归属机构与执行人权限是否匹配当前护理员。
     *
     * @author zhanghongyu
     */
    private void validateTaskForNurse(Long taskId, Long targetSanaId, Long currentUserId, String scene) {
        if (taskId == null) {
            return;
        }
        NursingTask task = nursingTaskRepository.selectById(taskId);
        if (task == null) {
            log.warn("{} not found: taskId={}", scene, taskId);
            throw new ProjectException(404, "关联任务不存在");
        }
        if (task.getSanaId() == null || !task.getSanaId().equals(targetSanaId)) {
            log.warn("{} forbidden: task sana mismatch, taskId={}, targetSanaId={}, taskSanaId={}",
                    scene, taskId, targetSanaId, task.getSanaId());
            throw new ProjectException(403, "关联任务不属于当前机构");
        }
        if (!Objects.equals(currentUserId, task.getAssigneeUserId())) {
            log.warn("{} forbidden: task assignee mismatch, userId={}, taskId={}, taskAssigneeUserId={}",
                    scene, currentUserId, taskId, task.getAssigneeUserId());
            throw new ProjectException(403, "仅可关联本人护理任务");
        }
    }

    /**
     * 校验“任务-护理员”日志唯一性，避免重复记录。
     *
     * @author zhanghongyu
     */
    private void validateTaskLogUniqueness(Long taskId, Long currentUserId, Long excludeLogId, String scene) {
        if (taskId == null || currentUserId == null) {
            return;
        }
        NursingLog existingLog = nursingLogRepository.selectByTaskIdAndNurseUserId(taskId, currentUserId);
        if (existingLog == null) {
            return;
        }
        if (excludeLogId != null && Objects.equals(existingLog.getId(), excludeLogId)) {
            return;
        }
        log.warn("{} conflict: duplicated task log, userId={}, taskId={}, existingLogId={}, excludeLogId={}",
                scene, currentUserId, taskId, existingLog.getId(), excludeLogId);
        throw new ProjectException(409, "该任务已存在护理日志，请直接编辑原日志");
    }

    /**
     * 校验老人是否属于目标机构。
     *
     * @author zhanghongyu
     */
    private void validateElderInSana(Long elderId, Long targetSanaId, String scene) {
        if (elderId == null) {
            return;
        }
        Elder elder = elderRepository.selectById(elderId);
        if (elder == null) {
            log.warn("{} not found: elderId={}", scene, elderId);
            throw new ProjectException(404, "关联老人不存在");
        }
        if (elder.getSanaId() == null || !elder.getSanaId().equals(targetSanaId)) {
            log.warn("{} forbidden: elder sana mismatch, elderId={}, targetSanaId={}, elderSanaId={}",
                    scene, elderId, targetSanaId, elder.getSanaId());
            throw new ProjectException(403, "关联老人不属于当前机构");
        }
    }

    /**
     * 规范化异常标记值（仅允许 0/1）。
     *
     * @author zhanghongyu
     */
    private Integer normalizeAbnormalFlag(Integer abnormalFlag, String scene) {
        if (abnormalFlag == null) {
            return ABNORMAL_NO;
        }
        if (abnormalFlag == ABNORMAL_NO || abnormalFlag == ABNORMAL_YES) {
            return abnormalFlag;
        }
        log.warn("{} bad request: invalid abnormalFlag={}", scene, abnormalFlag);
        throw new ProjectException(400, "abnormalFlag 仅支持 0/1");
    }

    /**
     * 页码容错归一化。
     *
     * @author zhanghongyu
     */
    private long resolvePageNo(Integer pageNo) {
        return pageNo == null || pageNo <= 0 ? 1L : pageNo.longValue();
    }

    /**
     * 分页大小容错归一化。
     *
     * @author zhanghongyu
     */
    private long resolvePageSize(Integer pageSize) {
        return pageSize == null || pageSize <= 0 ? 10L : pageSize.longValue();
    }
}

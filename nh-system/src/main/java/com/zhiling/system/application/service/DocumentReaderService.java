package com.zhiling.system.application.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 统一文档读取服务
 * 支持 PDF、Word (.doc/.docx) 文档解析
 * 基于 Spring AI Document 格式，适配 Qwen 模型
 *
 * @author zhanghongyu
 */
@Service
@Slf4j
public class DocumentReaderService {

    private static final int MAX_CONTENT_LENGTH = 3600; // 单块内容硬上限，避免检索上下文过肥
    private static final int TARGET_CONTENT_LENGTH = 2600; // 优先聚合到这个区间，降低块大小波动
    private static final int MAX_MERGED_CONTENT_LENGTH = 4200; // 合并相邻小块时允许的轻微缓冲
    private static final int MIN_CONTENT_LENGTH = 100; // 最小有效内容长度
    private static final int MIN_MERGE_CHUNK_LENGTH = 800;
    private static final int MAX_HEADING_LENGTH = 80;
    private static final int MAX_STANDALONE_HEADING_LENGTH = 18;
    private static final int MIN_FOLLOWING_PARAGRAPH_LENGTH = 40;
    private static final int MAX_HEADING_CONTEXT_LENGTH = 200;
    private static final Pattern MARKDOWN_HEADING_PATTERN = Pattern.compile("^#{1,6}\\s+.+$");
    private static final Pattern CHAPTER_HEADING_PATTERN = Pattern.compile("^第[一二三四五六七八九十百千万零〇\\d]+[编章节部分篇卷].*");
    private static final Pattern SECTION_HEADING_PATTERN = Pattern.compile("^第[一二三四五六七八九十百千万零〇\\d]+[节条款目].*");
    private static final Pattern NUMERIC_HEADING_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+){0,5})(?:[、.）)]|\\s)+.+$");
    private static final Pattern CHINESE_OUTLINE_HEADING_PATTERN = Pattern.compile("^[一二三四五六七八九十]+[、.]\\s*.+$");
    private static final Pattern PAREN_HEADING_PATTERN = Pattern.compile("^[（(][一二三四五六七八九十百千万零〇\\d]+[）)]\\s*.+$");
    private static final Pattern BULLET_HEADING_PATTERN = Pattern.compile("^[•·●■◆◦-]\\s*.+$");

    /**
     * 根据文件类型读取文档内容
     *
     * @param fileName 文件名
     * @param content 文件内容
     * @return 解析后的文档列表（Spring AI Document 格式）
     */
    public List<Document> readDocument(String fileName, byte[] content) {
        String extension = getFileExtension(fileName).toLowerCase();

        try (InputStream inputStream = new ByteArrayInputStream(content)) {
            // 使用传统 if-else 替代 switch 表达式以确保兼容性
            if ("pdf".equals(extension)) {
                return readPdf(fileName, inputStream);
            } else if ("docx".equals(extension)) {
                return readDocx(fileName, inputStream);
            } else if ("doc".equals(extension)) {
                return readDoc(fileName, inputStream);
            } else if ("txt".equals(extension)) {
                return readTxt(fileName, inputStream);
            } else {
                log.warn("不支持的文件类型: {}, 尝试按文本处理", extension);
                return readTxt("unknown.txt", inputStream);
            }
        } catch (Exception | LinkageError e) {
            log.error("文档解析失败: {}, 错误: {}", fileName, e.getMessage(), e);
            throw new RuntimeException("文档解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 读取 PDF 文档（基于 Spring AI 格式）
     */
    private List<Document> readPdf(String fileName, InputStream inputStream) throws Exception {
        List<Document> documents = new ArrayList<>();

        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setLineSeparator("\n");

            String text = stripper.getText(document);
            text = cleanText(text);

            // 检查是否为空或过短
            if (text.isBlank() || text.length() < MIN_CONTENT_LENGTH) {
                log.warn("PDF 文档内容过短或无法提取文本: {}", fileName);
                return List.of(createWarningDocument("此 PDF 文档可能包含扫描图片，无法提取文本内容。建议使用 OCR 工具处理。"));
            }

            // 智能分块（按段落和 token 限制）
            List<String> chunks = splitContentIntelligently(text, fileName);

            // 将每个分块转换为 Spring AI Document
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);

                Document doc = new Document(chunk);
                doc.getMetadata().put("file_name", fileName);
                doc.getMetadata().put("file_type", "pdf");
                doc.getMetadata().put("chunk_index", String.valueOf(i + 1));
                doc.getMetadata().put("total_chunks", String.valueOf(chunks.size()));

                documents.add(doc);
            }

            log.info("PDF 文档解析完成: {}, 页数: {}, 分块数: {}, 总字符数: {}",
                    fileName, document.getNumberOfPages(), chunks.size(), text.length());
        }

        return documents;
    }

    /**
     * 读取 DOCX 文档 (Word 2007+)
     */
    private List<Document> readDocx(String fileName, InputStream inputStream) throws Exception {
        List<Document> documents = new ArrayList<>();

        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            StringBuilder text = new StringBuilder();

            for (XWPFParagraph paragraph : paragraphs) {
                String paragraphText = paragraph.getText();
                if (paragraphText != null && !paragraphText.isBlank()) {
                    text.append(paragraphText).append("\n\n");
                }
            }

            String content = cleanText(text.toString());

            if (content.isBlank() || content.length() < MIN_CONTENT_LENGTH) {
                return List.of(createWarningDocument("此 Word 文档内容为空。"));
            }

            // 智能分块
            List<String> chunks = splitContentIntelligently(content, fileName);

            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);

                Document doc = new Document(chunk);
                doc.getMetadata().put("file_name", fileName);
                doc.getMetadata().put("file_type", "docx");
                doc.getMetadata().put("chunk_index", String.valueOf(i + 1));
                doc.getMetadata().put("total_chunks", String.valueOf(chunks.size()));

                documents.add(doc);
            }

            log.info("DOCX 文档解析完成: {}, 段落数: {}, 分块数: {}, 总字符数: {}",
                    fileName, paragraphs.size(), chunks.size(), content.length());
        }

        return documents;
    }

    /**
     * 读取 DOC 文档 (Word 97-2003)
     */
    private List<Document> readDoc(String fileName, InputStream inputStream) throws Exception {
        List<Document> documents = new ArrayList<>();

        try (HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {
            String text = extractor.getText();

            // 处理旧版 Word 文档的特殊格式
            text = text.replace("\f", ""); // 移除分页符
            text = text.replaceAll("\\s{3,}", " "); // 清理多余空白

            String content = cleanText(text);

            if (content.isBlank() || content.length() < MIN_CONTENT_LENGTH) {
                return List.of(createWarningDocument("此 Word 文档内容为空。"));
            }

            // 智能分块
            List<String> chunks = splitContentIntelligently(content, fileName);

            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);

                Document doc = new Document(chunk);
                doc.getMetadata().put("file_name", fileName);
                doc.getMetadata().put("file_type", "doc");
                doc.getMetadata().put("chunk_index", String.valueOf(i + 1));
                doc.getMetadata().put("total_chunks", String.valueOf(chunks.size()));

                documents.add(doc);
            }

            log.info("DOC 文档解析完成: {}, 分块数: {}, 总字符数: {}",
                    fileName, chunks.size(), content.length());
        }

        return documents;
    }

    /**
     * 读取纯文本文档
     */
    private List<Document> readTxt(String fileName, InputStream inputStream) throws Exception {
        String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        text = cleanText(text);

        if (text.isBlank()) {
            return List.of(createWarningDocument("文本文档内容为空。"));
        }

        // 智能分块
        List<String> chunks = splitContentIntelligently(text, fileName);

        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);

            Document doc = new Document(chunk);
            doc.getMetadata().put("file_name", fileName);
            doc.getMetadata().put("file_type", "txt");
            doc.getMetadata().put("chunk_index", String.valueOf(i + 1));
            doc.getMetadata().put("total_chunks", String.valueOf(chunks.size()));

            documents.add(doc);
        }

        log.info("TXT 文档解析完成: {}, 分块数: {}, 总字符数: {}",
                fileName, chunks.size(), text.length());

        return documents;
    }

    /**
     * 智能分块 - 按标题/段落语义优先分割
     * 先识别标题层级，再按章节聚合段落；超长章节再按句子边界兜底切分。
     */
    private List<String> splitContentIntelligently(String text, String fileName) {
        List<SemanticBlock> blocks = extractSemanticBlocks(text);
        List<SemanticSection> sections = buildSemanticSections(blocks);
        List<String> chunks = new ArrayList<>();
        for (SemanticSection section : sections) {
            chunks.addAll(splitSectionBySemanticBoundary(section));
        }
        if (chunks.isEmpty() && !text.isBlank()) {
            chunks.add(text.trim());
        }
        chunks = mergeAdjacentSmallChunks(chunks);
        log.info("文档分块完成: {} -> {} 块", fileName, chunks.size());
        return chunks;
    }

    /**
     * 查找句子边界（在指定范围内找句号、问号、感叹号等）
     */
    private int findSentenceBoundary(String text, int start, int end) {
        // 优先在窗口末尾附近断句，避免把超长段落切成大量小块
        for (int i = end - 1; i > start; i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '；'
                    || c == '.' || c == '!' || c == '?' || c == ';' || c == '，') {
                return i + 1;
            }
        }
        return end;
    }

    /**
     * 先把原始文本拆成语义块。
     * 标题与正文会被分开保留，供后续章节重组使用。
     */
    private List<SemanticBlock> extractSemanticBlocks(String text) {
        List<SemanticBlock> blocks = new ArrayList<>();
        String[] rawParagraphs = text.split("\\n{2,}");
        for (int i = 0; i < rawParagraphs.length; i++) {
            String rawParagraph = rawParagraphs[i];
            String paragraph = rawParagraph == null ? "" : rawParagraph.trim();
            if (paragraph.isEmpty()) {
                continue;
            }
            String nextParagraph = findNextNonBlankParagraph(rawParagraphs, i + 1);
            if (looksLikeStandaloneHeadingParagraph(paragraph, nextParagraph)) {
                blocks.add(new SemanticBlock(stripMarkdownHeadingMarker(paragraph), true, detectHeadingLevel(paragraph)));
                continue;
            }
            appendParagraphBlocks(paragraph, blocks);
        }
        return blocks;
    }

    /**
     * 方法：findNextNonBlankParagraph
     *
     * @author zhanghongyu
     */
    private String findNextNonBlankParagraph(String[] rawParagraphs, int startIndex) {
        for (int i = startIndex; i < rawParagraphs.length; i++) {
            String paragraph = rawParagraphs[i] == null ? "" : rawParagraphs[i].trim();
            if (!paragraph.isEmpty()) {
                return paragraph;
            }
        }
        return null;
    }

    /**
     * 方法：appendParagraphBlocks
     *
     * @author zhanghongyu
     */
    private void appendParagraphBlocks(String paragraph, List<SemanticBlock> blocks) {
        String[] rawLines = paragraph.split("\\n+");
        StringBuilder paragraphBuffer = new StringBuilder();

        for (String rawLine : rawLines) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (looksLikeHeading(line)) {
                flushParagraphBuffer(paragraphBuffer, blocks);
                blocks.add(new SemanticBlock(stripMarkdownHeadingMarker(line), true, detectHeadingLevel(line)));
                continue;
            }
            appendSemanticLine(paragraphBuffer, line);
        }

        flushParagraphBuffer(paragraphBuffer, blocks);
    }

    /**
     * 方法：flushParagraphBuffer
     *
     * @author zhanghongyu
     */
    private void flushParagraphBuffer(StringBuilder paragraphBuffer, List<SemanticBlock> blocks) {
        if (paragraphBuffer.length() == 0) {
            return;
        }
        blocks.add(new SemanticBlock(paragraphBuffer.toString().trim(), false, 0));
        paragraphBuffer.setLength(0);
    }

    /**
     * 方法：appendSemanticLine
     *
     * @author zhanghongyu
     */
    private void appendSemanticLine(StringBuilder paragraphBuffer, String line) {
        if (paragraphBuffer.length() == 0) {
            paragraphBuffer.append(line);
            return;
        }

        char lastChar = paragraphBuffer.charAt(paragraphBuffer.length() - 1);
        if (isSentenceEnding(lastChar) || looksLikeListItem(line)) {
            paragraphBuffer.append('\n');
        } else {
            paragraphBuffer.append(' ');
        }
        paragraphBuffer.append(line);
    }

    /**
     * 根据标题层级把正文归并到对应章节。
     * 这样切块时可以把标题上下文稳定附带到每个 chunk 上。
     */
    private List<SemanticSection> buildSemanticSections(List<SemanticBlock> blocks) {
        List<SemanticSection> sections = new ArrayList<>();
        List<SemanticHeading> headingStack = new ArrayList<>();
        List<String> currentParagraphs = new ArrayList<>();

        for (SemanticBlock block : blocks) {
            if (block.heading) {
                if (!currentParagraphs.isEmpty()) {
                    sections.add(new SemanticSection(buildHeadingContext(headingStack), new ArrayList<>(currentParagraphs)));
                    currentParagraphs.clear();
                }
                updateHeadingStack(headingStack, block);
                continue;
            }
            currentParagraphs.add(block.text);
        }

        if (!currentParagraphs.isEmpty()) {
            sections.add(new SemanticSection(buildHeadingContext(headingStack), currentParagraphs));
        }
        return sections;
    }

    /**
     * 方法：updateHeadingStack
     *
     * @author zhanghongyu
     */
    private void updateHeadingStack(List<SemanticHeading> headingStack, SemanticBlock block) {
        int level = block.level <= 0 ? 1 : block.level;
        while (headingStack.size() >= level) {
            headingStack.remove(headingStack.size() - 1);
        }
        headingStack.add(new SemanticHeading(block.text));
    }

    /**
     * 方法：buildHeadingContext
     *
     * @author zhanghongyu
     */
    private String buildHeadingContext(List<SemanticHeading> headingStack) {
        if (headingStack.isEmpty()) {
            return "";
        }

        List<String> headings = new ArrayList<>();
        int currentLength = 0;
        for (int i = headingStack.size() - 1; i >= 0; i--) {
            String text = headingStack.get(i).text;
            int extra = text.length() + (headings.isEmpty() ? 0 : 1);
            if (currentLength + extra > MAX_HEADING_CONTEXT_LENGTH && !headings.isEmpty()) {
                break;
            }
            headings.add(0, text);
            currentLength += extra;
        }
        return String.join("\n", headings).trim();
    }

    /**
     * 以章节为单位切块。
     * 超长章节保留标题上下文，再对正文按段落和句边界做兜底切分。
     */
    private List<String> splitSectionBySemanticBoundary(SemanticSection section) {
        List<String> chunks = new ArrayList<>();
        if (section.paragraphs == null || section.paragraphs.isEmpty()) {
            return chunks;
        }

        String headingContext = section.headingContext == null ? "" : section.headingContext.trim();
        String headingPrefix = headingContext.isEmpty() ? "" : headingContext + "\n\n";
        int maxBodyLength = Math.max(MAX_CONTENT_LENGTH - headingPrefix.length(), MIN_CONTENT_LENGTH);
        int targetBodyLength = Math.min(maxBodyLength,
                Math.max(TARGET_CONTENT_LENGTH - headingPrefix.length(), MIN_CONTENT_LENGTH));
        StringBuilder bodyBuilder = new StringBuilder();

        for (String paragraph : section.paragraphs) {
            if (paragraph == null || paragraph.isBlank()) {
                continue;
            }
            List<String> paragraphPieces = splitOversizedParagraph(paragraph.trim(), maxBodyLength);
            for (String piece : paragraphPieces) {
                if (piece.isBlank()) {
                    continue;
                }
                int candidateLength = bodyBuilder.length() + piece.length() + (bodyBuilder.length() > 0 ? 2 : 0);
                boolean exceedsHardLimit = candidateLength > maxBodyLength;
                boolean exceedsTarget = bodyBuilder.length() >= targetBodyLength && candidateLength > targetBodyLength;
                if ((exceedsHardLimit || exceedsTarget) && bodyBuilder.length() > 0) {
                    chunks.add(composeChunk(headingPrefix, bodyBuilder));
                    bodyBuilder.setLength(0);
                }

                if (bodyBuilder.length() > 0) {
                    bodyBuilder.append("\n\n");
                }
                bodyBuilder.append(piece);
            }
        }

        if (bodyBuilder.length() > 0) {
            chunks.add(composeChunk(headingPrefix, bodyBuilder));
        }

        return chunks;
    }

    /**
     * 方法：composeChunk
     *
     * @author zhanghongyu
     */
    private String composeChunk(String headingPrefix, StringBuilder bodyBuilder) {
        return (headingPrefix + bodyBuilder.toString().trim()).trim();
    }

    /**
     * 方法：splitOversizedParagraph
     *
     * @author zhanghongyu
     */
    private List<String> splitOversizedParagraph(String paragraph, int maxLength) {
        List<String> pieces = new ArrayList<>();
        if (paragraph.length() <= maxLength) {
            pieces.add(paragraph);
            return pieces;
        }

        int start = 0;
        while (start < paragraph.length()) {
            int end = Math.min(start + maxLength, paragraph.length());
            int splitPoint = findSentenceBoundary(paragraph, start, end);
            if (splitPoint <= start) {
                splitPoint = end;
            }
            String piece = paragraph.substring(start, splitPoint).trim();
            if (!piece.isEmpty()) {
                pieces.add(piece);
            }
            start = splitPoint;
        }

        return pieces;
    }

    /**
     * 将过小的相邻块做一次轻量合并，避免标题误切或章节过薄导致检索上下文过散。
     */
    private List<String> mergeAdjacentSmallChunks(List<String> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        List<String> normalizedChunks = chunks.stream()
                .map(chunk -> chunk == null ? "" : chunk.trim())
                .filter(chunk -> !chunk.isEmpty())
                .toList();
        if (normalizedChunks.size() <= 1) {
            return normalizedChunks;
        }

        List<String> merged = new ArrayList<>();
        int index = 0;
        while (index < normalizedChunks.size()) {
            String current = normalizedChunks.get(index);

            if (!merged.isEmpty()) {
                String previous = merged.get(merged.size() - 1);
                if (previous.length() < MIN_MERGE_CHUNK_LENGTH
                        && canMergeChunks(previous, current)) {
                    merged.set(merged.size() - 1, combineChunks(previous, current));
                    index++;
                    continue;
                }
            }

            if (current.length() < MIN_MERGE_CHUNK_LENGTH && index + 1 < normalizedChunks.size()) {
                String next = normalizedChunks.get(index + 1);
                if (canMergeChunks(current, next)) {
                    merged.add(combineChunks(current, next));
                    index += 2;
                    continue;
                }
            }

            merged.add(current);
            index++;
        }

        return merged;
    }

    /**
     * 方法：canMergeChunks
     *
     * @author zhanghongyu
     */
    private boolean canMergeChunks(String left, String right) {
        return left != null
                && right != null
                && left.length() + right.length() + 2 <= MAX_MERGED_CONTENT_LENGTH;
    }

    /**
     * 方法：combineChunks
     *
     * @author zhanghongyu
     */
    private String combineChunks(String left, String right) {
        return (left.trim() + "\n\n" + right.trim()).trim();
    }

    /**
     * 识别常见标题形态。
     * 包括 Markdown 标题、中文章节标题和数字大纲标题等显式结构化标题。
     */
    private boolean looksLikeHeading(String line) {
        String normalized = line == null ? "" : line.trim();
        if (normalized.isEmpty() || normalized.length() > MAX_HEADING_LENGTH) {
            return false;
        }

        if (MARKDOWN_HEADING_PATTERN.matcher(normalized).matches()) {
            return true;
        }
        if (CHAPTER_HEADING_PATTERN.matcher(normalized).matches()
                || SECTION_HEADING_PATTERN.matcher(normalized).matches()
                || NUMERIC_HEADING_PATTERN.matcher(normalized).matches()
                || CHINESE_OUTLINE_HEADING_PATTERN.matcher(normalized).matches()
                || PAREN_HEADING_PATTERN.matcher(normalized).matches()) {
            return !endsWithSentencePunctuation(normalized);
        }

        return false;
    }

    /**
     * 识别“单独占一段”的短标题。
     * 这类标题没有显式编号时，仅在后续段落明显更像正文时才认定为标题，避免把短正文误切碎。
     */
    private boolean looksLikeStandaloneHeadingParagraph(String paragraph, String nextParagraph) {
        String normalized = paragraph == null ? "" : paragraph.trim();
        if (normalized.isEmpty() || normalized.contains("\n")) {
            return false;
        }
        if (looksLikeHeading(normalized)) {
            return true;
        }
        if (normalized.length() > MAX_STANDALONE_HEADING_LENGTH
                || endsWithSentencePunctuation(normalized)
                || countSentenceSeparators(normalized) > 0
                || looksLikeListItem(normalized)) {
            return false;
        }
        if (nextParagraph == null || nextParagraph.isBlank()) {
            return false;
        }

        String nextNormalized = nextParagraph.trim();
        if (nextNormalized.length() < MIN_FOLLOWING_PARAGRAPH_LENGTH) {
            return false;
        }

        return countSentenceSeparators(nextNormalized) > 0
                || nextNormalized.contains("：")
                || nextNormalized.contains(":")
                || nextNormalized.length() >= normalized.length() + 12;
    }

    /**
     * 方法：detectHeadingLevel
     *
     * @author zhanghongyu
     */
    private int detectHeadingLevel(String line) {
        String normalized = line == null ? "" : line.trim();
        if (normalized.isEmpty()) {
            return 1;
        }

        if (normalized.startsWith("#")) {
            int level = 0;
            while (level < normalized.length() && normalized.charAt(level) == '#') {
                level++;
            }
            return Math.max(level, 1);
        }

        if (CHAPTER_HEADING_PATTERN.matcher(normalized).matches()) {
            return normalized.contains("章") || normalized.contains("编") || normalized.contains("篇")
                    || normalized.contains("卷") || normalized.contains("部分") ? 1 : 2;
        }

        if (SECTION_HEADING_PATTERN.matcher(normalized).matches()) {
            return normalized.contains("节") ? 2 : 3;
        }

        java.util.regex.Matcher numericMatcher = NUMERIC_HEADING_PATTERN.matcher(normalized);
        if (numericMatcher.matches()) {
            String prefix = numericMatcher.group(1);
            return prefix.split("\\.").length;
        }

        if (CHINESE_OUTLINE_HEADING_PATTERN.matcher(normalized).matches()) {
            return 2;
        }

        if (PAREN_HEADING_PATTERN.matcher(normalized).matches()) {
            return 3;
        }

        return 1;
    }

    /**
     * 方法：stripMarkdownHeadingMarker
     *
     * @author zhanghongyu
     */
    private String stripMarkdownHeadingMarker(String line) {
        String normalized = line == null ? "" : line.trim();
        if (MARKDOWN_HEADING_PATTERN.matcher(normalized).matches()) {
            return normalized.replaceFirst("^#{1,6}\\s*", "").trim();
        }
        return normalized;
    }

    /**
     * 方法：endsWithSentencePunctuation
     *
     * @author zhanghongyu
     */
    private boolean endsWithSentencePunctuation(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        char lastChar = text.charAt(text.length() - 1);
        return isSentenceEnding(lastChar);
    }

    /**
     * 方法：countSentenceSeparators
     *
     * @author zhanghongyu
     */
    private int countSentenceSeparators(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if (isSentenceEnding(c) || c == '，' || c == ',') {
                count++;
            }
        }
        return count;
    }

    /**
     * 方法：looksLikeListItem
     *
     * @author zhanghongyu
     */
    private boolean looksLikeListItem(String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.isEmpty()) {
            return false;
        }
        return BULLET_HEADING_PATTERN.matcher(normalized).matches()
                || PAREN_HEADING_PATTERN.matcher(normalized).matches()
                || normalized.matches("^\\d+[、.)）].*")
                || normalized.matches("^[一二三四五六七八九十]+、.*");
    }

    /**
     * 方法：isSentenceEnding
     *
     * @author zhanghongyu
     */
    private boolean isSentenceEnding(char c) {
        return c == '。' || c == '！' || c == '？' || c == '；'
                || c == '.' || c == '!' || c == '?' || c == ';';
    }

    /**
     * 清理提取的文本
     */
    private String cleanText(String text) {
        if (text == null) {
            return "";
        }

        String normalized = text
                // 统一换行符
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                // 将常见不可见空白统一为普通空格
                .replace('\u00A0', ' ')
                .replace('\u2002', ' ')
                .replace('\u2003', ' ')
                .replace('\u2009', ' ')
                .replace('\u3000', ' ')
                // 移除特殊控制字符（保留换行和制表）
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "")
                // 清理页眉页脚噪音
                .replaceAll("第\\s*\\d+\\s*页\\s*[\\/／]\\s*共?\\s*\\d+\\s*页", "")
                .replaceAll("第\\s*\\d+\\s*页", "")
                .replaceAll("-\\s*\\d+\\s*-\\s*", "");

        String[] lines = normalized.split("\\n", -1);
        StringBuilder builder = new StringBuilder();
        int blankLineCount = 0;

        for (String rawLine : lines) {
            String line = rawLine
                    // 行内多余空白压缩
                    .replaceAll("[\\t\\f\\x0B ]{2,}", " ")
                    .trim();

            if (line.isEmpty()) {
                blankLineCount++;
                // 最多保留一个空行，避免段落间距过大
                if (blankLineCount > 1) {
                    continue;
                }
            } else {
                blankLineCount = 0;
            }

            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(line);
        }

        return builder.toString().trim();
    }

    /**
     * 创建警告文档
     */
    private Document createWarningDocument(String message) {
        Document doc = new Document(message);
        doc.getMetadata().put("warning", "true");
        return doc;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }

    /**
     * 检查文件类型是否支持
     */
    public boolean isSupportedFileType(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        return List.of("pdf", "docx", "doc", "txt").contains(extension);
    }

    /**
     * 获取支持的文件类型列表
     */
    public List<String> getSupportedFileTypes() {
        return List.of("pdf", "docx", "doc", "txt");
    }

    /**
     * 获取支持的 MIME 类型列表
     */
    public List<String> getSupportedMimeTypes() {
        return List.of(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "text/plain"
        );
    }

    /**
     * 估算文本的 Token 数量
     * 粗略估算：中文约 1.5 字符 = 1 token，英文约 4 字符 = 1 token
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int chineseChars = 0;
        int otherChars = 0;

        for (char c : text.toCharArray()) {
            if (isChinese(c)) {
                chineseChars++;
            } else {
                otherChars++;
            }
        }

        // 中文约 1.5 字符 = 1 token，英文约 4 字符 = 1 token
        return (int) (chineseChars / 1.5 + otherChars / 4.0);
    }

    /**
     * 判断是否为中文字符
     */
    private boolean isChinese(char c) {
        return (c >= 0x4E00 && c <= 0x9FFF) || // CJK 统一表意文字符
               (c >= 0x3400 && c <= 0x4DBF);   // CJK 扩展A区
    }

    private static final class SemanticBlock {
        private final String text;
        private final boolean heading;
        private final int level;

        /**
         * 方法：SemanticBlock
         *
         * @author zhanghongyu
         */
        private SemanticBlock(String text, boolean heading, int level) {
            this.text = text;
            this.heading = heading;
            this.level = level;
        }
    }

    private static final class SemanticHeading {
        private final String text;

        /**
         * 方法：SemanticHeading
         *
         * @author zhanghongyu
         */
        private SemanticHeading(String text) {
            this.text = text;
        }
    }

    private static final class SemanticSection {
        private final String headingContext;
        private final List<String> paragraphs;

        /**
         * 方法：SemanticSection
         *
         * @author zhanghongyu
         */
        private SemanticSection(String headingContext, List<String> paragraphs) {
            this.headingContext = headingContext;
            this.paragraphs = paragraphs;
        }
    }
}

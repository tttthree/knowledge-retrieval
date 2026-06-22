package com.knowledgeretrieval.service.document;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档解析器：将上传的 TXT / PDF 文件解析为纯文本。
 * 根据文件扩展名选择解析策略。
 */
@Slf4j
@Service
public class DocumentParser {

    /**
     * 解析文档输入流为纯文本。
     *
     * @param inputStream 文档输入流
     * @param fileName    原始文件名（用于判断文件类型）
     * @return 提取的纯文本
     */
    public String parse(InputStream inputStream, String fileName) throws IOException {
        if (fileName == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith(".txt")) {
            return parseTxt(inputStream);
        } else if (lowerName.endsWith(".csv")) {
            return parseCsv(inputStream);
        } else if (lowerName.endsWith(".html") || lowerName.endsWith(".htm")) {
            return parseHtml(inputStream);
        } else if (lowerName.endsWith(".pdf")) {
            return parsePdf(inputStream);
        } else if (lowerName.endsWith(".md")) {
            return parseTxt(inputStream);
        } else {
            // 无法识别类型时，尝试按纯文本处理
            log.warn("未知文件扩展名 '{}'，尝试按纯文本解析", fileName);
            return parseTxt(inputStream);
        }
    }

    /**
     * 解析纯文本文件。
     */
    private String parseTxt(InputStream inputStream) throws IOException {
        String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        log.info("TXT 文件解析完成: {} 字符", text.length());
        return text;
    }

    /**
     * 解析 CSV 文件，保留表头和前 50 行数据作为 LLM 抽取样本。
     */
    private String parseCsv(InputStream inputStream) throws IOException {
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader().setSkipHeaderRecord(false).build().parse(reader)) {

            StringBuilder sb = new StringBuilder();
            // 表头
            sb.append(String.join(",", parser.getHeaderNames())).append("\n");

            // 数据行（最多 50 行）
            int rowCount = 0;
            for (CSVRecord record : parser) {
                List<String> values = new ArrayList<>();
                for (String header : parser.getHeaderNames()) {
                    values.add(record.get(header));
                }
                sb.append(String.join(",", values)).append("\n");
                rowCount++;
                if (rowCount >= 50) break;
            }
            log.info("CSV 解析完成: {} 列, {} 行", parser.getHeaderNames().size(), rowCount);
            return sb.toString();
        }
    }

    /**
     * 解析 HTML 文件，去除标签保留纯文本。
     */
    private String parseHtml(InputStream inputStream) throws IOException {
        String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        // 简单去标签，保留文本结构
        String text = html.replaceAll("(?s)<script[^>]*>.*?</script>", "")
                .replaceAll("(?s)<style[^>]*>.*?</style>", "")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&[a-z]+;", " ")
                .replaceAll("\\s{2,}", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        log.info("HTML 解析完成: {} 字符", text.length());
        return text;
    }

    /**
     * 使用 Apache PDFBox 解析 PDF 文件。
     */
    private String parsePdf(InputStream inputStream) throws IOException {
        // PDFBox 3.x 使用 Loader + RandomAccessReadBuffer
        byte[] bytes = inputStream.readAllBytes();
        try (var document = Loader.loadPDF(new RandomAccessReadBuffer(bytes))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            log.info("PDF 文件解析完成: {} 字符, {} 页", text.length(), document.getNumberOfPages());
            return text;
        }
    }
}

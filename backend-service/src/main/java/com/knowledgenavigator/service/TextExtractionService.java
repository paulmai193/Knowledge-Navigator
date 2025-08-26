package com.knowledgenavigator.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;

@Service
public class TextExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(TextExtractionService.class);

    public String extractText(String filePath, String contentType) throws IOException {
        logger.debug("Extracting text from file: {} ({})", filePath, contentType);
        
        try {
            switch (contentType) {
                case "text/plain":
                    return Files.readString(Paths.get(filePath));
                case "application/pdf":
                    return extractPdfText(filePath);
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                    return extractDocxText(filePath);
                case "application/msword":
                    return extractDocText(filePath);
                default:
                    logger.warn("Unsupported content type: {}", contentType);
                    return "Content extraction not supported for: " + contentType;
            }
        } catch (Exception e) {
            logger.error("Error extracting text from file {}: {}", filePath, e.getMessage());
            throw new IOException("Error extracting text from file: " + e.getMessage(), e);
        }
    }

    private String extractPdfText(String filePath) throws IOException {
        try (PDDocument document = PDDocument.load(new FileInputStream(filePath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractDocxText(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractDocText(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             HWPFDocument document = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }
}
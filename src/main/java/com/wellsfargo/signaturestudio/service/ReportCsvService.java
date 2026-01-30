package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.enums.FieldType;
import com.wellsfargo.signaturestudio.model.Document;
import com.wellsfargo.signaturestudio.model.Transaction;
import com.wellsfargo.signaturestudio.model.User;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating CSV reports using Apache Commons CSV.
 * Uses FieldType enum for field selection and extraction.
 */
@Service
public class ReportCsvService {

    private static final Logger logger = LoggerFactory.getLogger(ReportCsvService.class);
    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final FieldConfigurationService fieldConfigurationService;

    public ReportCsvService(FieldConfigurationService fieldConfigurationService) {
        this.fieldConfigurationService = fieldConfigurationService;
    }

    /**
     * Generate CSV report for transactions.
     *
     * @param transactions List of transactions to include in report
     * @param selectedFieldTypes List of FieldType enum names to include
     * @return CSV data as byte array
     * @throws IOException if CSV generation fails
     */
    public byte[] generateTransactionReportCsv(List<Transaction> transactions,
                                               List<String> selectedFieldTypes) throws IOException {
        logger.debug("Generating CSV report for {} transactions with {} fields",
                    transactions.size(), selectedFieldTypes.size());

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (CSVPrinter printer = new CSVPrinter(
                new OutputStreamWriter(out, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder()
                    .setHeader(buildCsvHeaders(selectedFieldTypes))
                    .build())) {

            for (Transaction transaction : transactions) {
                List<String> row = new ArrayList<>();
                for (String fieldTypeName : selectedFieldTypes) {
                    String value = extractFieldValue(transaction, fieldTypeName);
                    row.add(value != null ? value : "");
                }
                printer.printRecord(row);
            }

            printer.flush();
        }

        logger.debug("Generated CSV report: {} bytes", out.size());
        return out.toByteArray();
    }

    /**
     * Build CSV headers from FieldType enum names.
     */
    private String[] buildCsvHeaders(List<String> selectedFieldTypes) {
        return selectedFieldTypes.stream()
            .map(fieldConfigurationService::getFieldDisplayName)
            .toArray(String[]::new);
    }

    /**
     * Extract field value from transaction based on FieldType.
     */
    private String extractFieldValue(Transaction transaction, String fieldTypeName) {
        try {
            FieldType fieldType = FieldType.valueOf(fieldTypeName);
            String fieldPath = fieldType.getFieldPath();

            // Handle nested fields (e.g., "users.externalId")
            if (fieldPath.contains(".")) {
                return extractNestedFieldValue(transaction, fieldPath);
            }

            // Handle direct transaction fields
            return switch (fieldTypeName) {
                case "ACCT_NAME" -> transaction.getAccountCode();
                case "ACCT_ID" -> transaction.getAccountId();
                case "TXN_NAME" -> transaction.getTitle();
                case "TXN_GUID" -> transaction.getId();
                case "TXN_STATUS" -> transaction.getStatus();
                case "TXN_STATUS_TMSTP" -> formatInstant(transaction.getUpdatedAt());
                case "TXN_SENDER_ID" -> transaction.getCreatorEmail();
                default -> "";
            };
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid field type: {}", fieldTypeName);
            return "";
        }
    }

    /**
     * Extract nested field value (e.g., users.externalId, documents.title).
     */
    private String extractNestedFieldValue(Transaction transaction, String fieldPath) {
        String[] parts = fieldPath.split("\\.", 2);
        String entity = parts[0];  // e.g., "users" or "documents"
        String field = parts[1];   // e.g., "externalId" or "title"

        if ("users".equals(entity)) {
            List<User> users = transaction.getUsers();
            if (users == null || users.isEmpty()) {
                return "";
            }

            return users.stream()
                .map(user -> {
                    String value = switch (field) {
                        case "externalId" -> user.getExternalId();
                        case "userCategory" -> user.getUserCategory();
                        default -> "";
                    };
                    return value;
                })
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("; "));
        }

        if ("documents".equals(entity)) {
            List<Document> documents = transaction.getDocuments();
            if (documents == null || documents.isEmpty()) {
                return "";
            }

            return documents.stream()
                .map(doc -> {
                    return switch (field) {
                        case "title" -> doc.getTitle();
                        case "id" -> doc.getId();
                        case "status" -> ""; // Document status not in model yet
                        default -> "";
                    };
                })
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("; "));
        }

        return "";
    }

    /**
     * Format Instant to readable string.
     */
    private String formatInstant(Instant instant) {
        if (instant == null) {
            return "";
        }
        return DATE_FORMATTER.format(instant);
    }
}

package com.wellsfargo.signaturestudio.controller;

import com.wellsfargo.signaturestudio.constants.SessionConstants;
import com.wellsfargo.signaturestudio.domain.*;
import com.wellsfargo.signaturestudio.enums.FieldType;
import com.wellsfargo.signaturestudio.enums.ReportType;
import com.wellsfargo.signaturestudio.service.FieldConfigurationService;
import com.wellsfargo.signaturestudio.service.ReportCsvService;
import com.wellsfargo.signaturestudio.service.ReportQueryService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for report generation and management.
 * Provides endpoints for:
 * - Getting available fields
 * - Generating reports on-the-fly (synchronous)
 * - Downloading CSV reports
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");

    private final FieldConfigurationService fieldConfigurationService;
    private final ReportQueryService reportQueryService;
    private final ReportCsvService reportCsvService;

    public ReportController(FieldConfigurationService fieldConfigurationService,
                           ReportQueryService reportQueryService,
                           ReportCsvService reportCsvService) {
        this.fieldConfigurationService = fieldConfigurationService;
        this.reportQueryService = reportQueryService;
        this.reportCsvService = reportCsvService;
    }

    /**
     * Get available field types for report generation.
     * GET /api/reports/field-options?reportType=TRANSACTION_REPORT
     *
     * Returns all available FieldType enums that can be selected for reports.
     */
    @GetMapping("/field-options")
    public ResponseEntity<AvailableFieldsResponse> getFieldOptions(
            @RequestParam(defaultValue = "TRANSACTION_REPORT") ReportType reportType) {

        logger.debug("Getting field options for report type: {}", reportType);

        AvailableFieldsResponse response = fieldConfigurationService.getAvailableFields(reportType);

        return ResponseEntity.ok(response);
    }

    /**
     * Get list of all available FieldType enums.
     * GET /api/reports/field-types
     *
     * Returns simple list of field type enum names and display names.
     */
    @GetMapping("/field-types")
    public ResponseEntity<List<FieldTypeInfo>> getFieldTypes() {
        logger.debug("Getting all field types");

        List<FieldTypeInfo> fieldTypes = Arrays.stream(FieldType.values())
            .map(ft -> new FieldTypeInfo(ft.name(), ft.getDisplayName(), ft.getCategory()))
            .collect(Collectors.toList());

        return ResponseEntity.ok(fieldTypes);
    }

    /**
     * Generate a transaction report on-the-fly (synchronous).
     * POST /api/reports/generate
     *
     * Request body:
     * {
     *   "selectedFields": ["TXN_GUID", "TXN_NAME", "TXN_STATUS", "ACCT_NAME"],
     *   "accountId": "optional-account-id",
     *   "includeAllAccounts": false,
     *   "filterCriteria": {
     *     "createdAfter": "2024-01-01T00:00:00Z",
     *     "createdBefore": "2024-12-31T23:59:59Z",
     *     "statuses": ["completed", "in-progress"],
     *     "searchText": "contract"
     *   }
     * }
     *
     * Returns CSV file as download.
     */
    @PostMapping("/generate")
    public ResponseEntity<Resource> generateReport(
            @Valid @RequestBody GenerateReportOnTheFlyRequest request,
            HttpSession session) {

        // Validate session
        String userId = (String) session.getAttribute(SessionConstants.USERNAME);
        if (userId == null || userId.isEmpty()) {
            auditLogger.warn("UNAUTHORIZED_REPORT_ACCESS | No session");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Generating report for user: {}, fields: {}, filters: {}",
                   userId, request.getSelectedFields().size(),
                   request.getFilterCriteria() != null ? "yes" : "no");

        try {
            // Validate field selection
            fieldConfigurationService.validateFields(ReportType.TRANSACTION_REPORT, request.getSelectedFields());

            // Query transactions with filters
            var transactions = reportQueryService.queryTransactionsForReport(
                request.getAccountId(),
                request.isIncludeAllAccounts(),
                userId,
                session,
                request.getFilterCriteria()
            );

            logger.info("Found {} transactions for report", transactions.size());

            // Generate CSV
            byte[] csvData = reportCsvService.generateTransactionReportCsv(
                transactions,
                request.getSelectedFields()
            );

            // Create filename with timestamp
            String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                .replace(":", "-")
                .replace(".", "-");
            String filename = "transaction-report-" + timestamp + ".csv";

            // Audit log
            auditLogger.info("REPORT_GENERATED | User: {} | Rows: {} | Size: {} bytes",
                           userId, transactions.size(), csvData.length);

            // Return as downloadable file
            ByteArrayResource resource = new ByteArrayResource(csvData);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(csvData.length)
                .body(resource);

        } catch (IOException e) {
            logger.error("Error generating CSV report", e);
            auditLogger.error("REPORT_GENERATION_FAILED | User: {} | Error: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Error generating report", e);
            auditLogger.error("REPORT_GENERATION_FAILED | User: {} | Error: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Preview report data (without downloading).
     * POST /api/reports/preview
     *
     * Returns summary of what would be included in the report.
     */
    @PostMapping("/preview")
    public ResponseEntity<ReportPreviewResponse> previewReport(
            @Valid @RequestBody GenerateReportOnTheFlyRequest request,
            HttpSession session) {

        // Validate session
        String userId = (String) session.getAttribute(SessionConstants.USERNAME);
        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.debug("Previewing report for user: {}", userId);

        try {
            // Validate field selection
            fieldConfigurationService.validateFields(ReportType.TRANSACTION_REPORT, request.getSelectedFields());

            // Query transactions with filters
            var transactions = reportQueryService.queryTransactionsForReport(
                request.getAccountId(),
                request.isIncludeAllAccounts(),
                userId,
                session,
                request.getFilterCriteria()
            );

            // Build preview response
            ReportPreviewResponse preview = new ReportPreviewResponse();
            preview.setTransactionCount(transactions.size());
            preview.setSelectedFieldCount(request.getSelectedFields().size());
            preview.setHasFilters(request.getFilterCriteria() != null && request.getFilterCriteria().hasFilters());

            return ResponseEntity.ok(preview);

        } catch (Exception e) {
            logger.error("Error previewing report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Helper DTOs

    /**
     * Field type information DTO.
     */
    public static class FieldTypeInfo {
        private String name;
        private String displayName;
        private String category;

        public FieldTypeInfo(String name, String displayName, String category) {
            this.name = name;
            this.displayName = displayName;
            this.category = category;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }
    }

    /**
     * Request for generating report on-the-fly.
     */
    public static class GenerateReportOnTheFlyRequest {
        private List<String> selectedFields;
        private String accountId;
        private boolean includeAllAccounts;
        private ReportFilterCriteria filterCriteria;

        public List<String> getSelectedFields() {
            return selectedFields;
        }

        public void setSelectedFields(List<String> selectedFields) {
            this.selectedFields = selectedFields;
        }

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public boolean isIncludeAllAccounts() {
            return includeAllAccounts;
        }

        public void setIncludeAllAccounts(boolean includeAllAccounts) {
            this.includeAllAccounts = includeAllAccounts;
        }

        public ReportFilterCriteria getFilterCriteria() {
            return filterCriteria;
        }

        public void setFilterCriteria(ReportFilterCriteria filterCriteria) {
            this.filterCriteria = filterCriteria;
        }
    }

    /**
     * Report preview response.
     */
    public static class ReportPreviewResponse {
        private int transactionCount;
        private int selectedFieldCount;
        private boolean hasFilters;

        public int getTransactionCount() {
            return transactionCount;
        }

        public void setTransactionCount(int transactionCount) {
            this.transactionCount = transactionCount;
        }

        public int getSelectedFieldCount() {
            return selectedFieldCount;
        }

        public void setSelectedFieldCount(int selectedFieldCount) {
            this.selectedFieldCount = selectedFieldCount;
        }

        public boolean isHasFilters() {
            return hasFilters;
        }

        public void setHasFilters(boolean hasFilters) {
            this.hasFilters = hasFilters;
        }
    }
}

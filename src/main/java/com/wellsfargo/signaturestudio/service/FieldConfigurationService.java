package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.domain.AvailableFieldsResponse;
import com.wellsfargo.signaturestudio.domain.FieldOption;
import com.wellsfargo.signaturestudio.enums.FieldType;
import com.wellsfargo.signaturestudio.enums.ReportType;
import com.wellsfargo.signaturestudio.exception.ErrorCode;
import com.wellsfargo.signaturestudio.exception.ServiceException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing field configuration for different report types.
 * Uses FieldType enum for field selection.
 */
@Service
public class FieldConfigurationService {

    /**
     * Get available fields for a report type.
     * Returns all FieldType enums as available options.
     */
    public AvailableFieldsResponse getAvailableFields(ReportType reportType) {
        if (reportType == null) {
            throw new ServiceException(ErrorCode.VALIDATION_ERROR, "Report type is required");
        }

        if (reportType != ReportType.TRANSACTION_REPORT) {
            throw new ServiceException(ErrorCode.VALIDATION_ERROR, "Unsupported report type: " + reportType);
        }

        // Convert all FieldType enums to FieldOption objects
        List<FieldOption> fields = Arrays.stream(FieldType.values())
            .map(fieldType -> new FieldOption(
                fieldType.name(),                    // fieldName is the enum name
                fieldType.getDisplayName(),          // displayName
                getFieldDataType(fieldType),         // fieldType
                fieldType.getCategory(),             // category
                isDefaultField(fieldType)            // isDefault
            ))
            .collect(Collectors.toList());

        List<String> defaultFields = getDefaultFields(reportType);

        return new AvailableFieldsResponse(reportType, fields, defaultFields);
    }

    /**
     * Get default field selection for a report type.
     * Returns enum names of default fields.
     */
    public List<String> getDefaultFields(ReportType reportType) {
        if (reportType == null) {
            throw new ServiceException(ErrorCode.VALIDATION_ERROR, "Report type is required");
        }

        if (reportType != ReportType.TRANSACTION_REPORT) {
            throw new ServiceException(ErrorCode.VALIDATION_ERROR, "Unsupported report type: " + reportType);
        }

        // Default fields: most commonly used fields
        return List.of(
            FieldType.TXN_GUID.name(),
            FieldType.TXN_NAME.name(),
            FieldType.TXN_STATUS.name(),
            FieldType.ACCT_NAME.name(),
            FieldType.TXN_SENDER_ID.name(),
            FieldType.TXN_STATUS_TMSTP.name()
        );
    }

    /**
     * Validate field selection for a report type.
     * Validates that all selected fields are valid FieldType enum names.
     */
    public void validateFields(ReportType reportType, List<String> selectedFields) {
        if (reportType == null) {
            throw new ServiceException(ErrorCode.VALIDATION_ERROR, "Report type is required");
        }

        if (selectedFields == null || selectedFields.isEmpty()) {
            throw new ServiceException(ErrorCode.INVALID_FIELD_SELECTION,
                "At least one field must be selected");
        }

        // Get all valid field type names
        Set<String> validFieldNames = Arrays.stream(FieldType.values())
            .map(Enum::name)
            .collect(Collectors.toSet());

        // Find invalid fields
        List<String> invalidFields = selectedFields.stream()
            .filter(field -> !validFieldNames.contains(field))
            .collect(Collectors.toList());

        if (!invalidFields.isEmpty()) {
            throw new ServiceException(ErrorCode.INVALID_FIELD_SELECTION,
                "Invalid fields: " + String.join(", ", invalidFields));
        }
    }

    /**
     * Get display name for a field type enum name.
     */
    public String getFieldDisplayName(String fieldTypeName) {
        try {
            FieldType fieldType = FieldType.valueOf(fieldTypeName);
            return fieldType.getDisplayName();
        } catch (IllegalArgumentException e) {
            return fieldTypeName;
        }
    }

    /**
     * Check if a field is valid for a report type.
     */
    public boolean isValidField(ReportType reportType, String fieldTypeName) {
        if (reportType == null || fieldTypeName == null) {
            return false;
        }

        try {
            FieldType.valueOf(fieldTypeName);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Convert FieldType enum name to field path for data extraction.
     */
    public String getFieldPath(String fieldTypeName) {
        try {
            FieldType fieldType = FieldType.valueOf(fieldTypeName);
            return fieldType.getFieldPath();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Determine data type for a field.
     */
    private String getFieldDataType(FieldType fieldType) {
        return switch (fieldType) {
            case TXN_STATUS_TMSTP -> "instant";
            case DOC_TITLE, DOC_ID, DOC_STATUS, SIGNER_EXTERNAL_ID, SIGNER_EXTERNAL_ID_TYPE -> "list";
            default -> "string";
        };
    }

    /**
     * Determine if a field should be included in default selection.
     */
    private boolean isDefaultField(FieldType fieldType) {
        return switch (fieldType) {
            case TXN_GUID, TXN_NAME, TXN_STATUS, ACCT_NAME, TXN_SENDER_ID, TXN_STATUS_TMSTP -> true;
            default -> false;
        };
    }
}

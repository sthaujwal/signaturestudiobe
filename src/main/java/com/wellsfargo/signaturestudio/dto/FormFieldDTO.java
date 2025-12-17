package com.wellsfargo.signaturestudio.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class FormFieldDTO {
    private String id;
    private String documentId;
    
    @NotBlank(message = "Field name is required")
    private String fieldName;
    
    private String fieldLabel;
    
    @NotBlank(message = "Field type is required")
    private String fieldType; // text, signature, date, checkbox, radio, dropdown, etc.
    
    private String fieldValue;
    private String defaultValue;
    private Boolean isRequired;
    private Boolean isReadonly;
    
    // Single position fields (kept for backward compatibility and simple fields)
    private Integer pageNumber;
    private Double xPosition;
    private Double yPosition;
    private Double width;
    private Double height;
    
    // Multiple positions (for radio buttons, checkboxes with multiple options, etc.)
    private List<PositionDTO> positions;
    
    private Integer signingOrder;
    private String assignedToUserId;
    private Map<String, Object> validationRules;
    private Map<String, Object> options;
    private Instant createdAt;
    private Instant updatedAt;
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
    
    public String getFieldLabel() {
        return fieldLabel;
    }
    
    public void setFieldLabel(String fieldLabel) {
        this.fieldLabel = fieldLabel;
    }
    
    public String getFieldType() {
        return fieldType;
    }
    
    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }
    
    public String getFieldValue() {
        return fieldValue;
    }
    
    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }
    
    public String getDefaultValue() {
        return defaultValue;
    }
    
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    public Boolean getIsRequired() {
        return isRequired;
    }
    
    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }
    
    public Boolean getIsReadonly() {
        return isReadonly;
    }
    
    public void setIsReadonly(Boolean isReadonly) {
        this.isReadonly = isReadonly;
    }
    
    public Integer getPageNumber() {
        return pageNumber;
    }
    
    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }
    
    public Double getXPosition() {
        return xPosition;
    }
    
    public void setXPosition(Double xPosition) {
        this.xPosition = xPosition;
    }
    
    public Double getYPosition() {
        return yPosition;
    }
    
    public void setYPosition(Double yPosition) {
        this.yPosition = yPosition;
    }
    
    public Double getWidth() {
        return width;
    }
    
    public void setWidth(Double width) {
        this.width = width;
    }
    
    public Double getHeight() {
        return height;
    }
    
    public void setHeight(Double height) {
        this.height = height;
    }
    
    public List<PositionDTO> getPositions() {
        return positions;
    }
    
    public void setPositions(List<PositionDTO> positions) {
        this.positions = positions;
    }
    
    public Integer getSigningOrder() {
        return signingOrder;
    }
    
    public void setSigningOrder(Integer signingOrder) {
        this.signingOrder = signingOrder;
    }
    
    public String getAssignedToUserId() {
        return assignedToUserId;
    }
    
    public void setAssignedToUserId(String assignedToUserId) {
        this.assignedToUserId = assignedToUserId;
    }
    
    public Map<String, Object> getValidationRules() {
        return validationRules;
    }
    
    public void setValidationRules(Map<String, Object> validationRules) {
        this.validationRules = validationRules;
    }
    
    public Map<String, Object> getOptions() {
        return options;
    }
    
    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}


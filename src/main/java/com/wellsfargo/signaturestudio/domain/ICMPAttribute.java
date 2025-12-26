package com.wellsfargo.signaturestudio.domain;

public class ICMPAttribute {
    private String id;
    private String icmpId;
    private String attributeKey;
    private String attributeValue;
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getIcmpId() {
        return icmpId;
    }
    
    public void setIcmpId(String icmpId) {
        this.icmpId = icmpId;
    }
    
    public String getAttributeKey() {
        return attributeKey;
    }
    
    public void setAttributeKey(String attributeKey) {
        this.attributeKey = attributeKey;
    }
    
    public String getAttributeValue() {
        return attributeValue;
    }
    
    public void setAttributeValue(String attributeValue) {
        this.attributeValue = attributeValue;
    }
}


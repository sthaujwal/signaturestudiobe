package com.wellsfargo.signaturestudio.domain;

public class Position {
    private Integer pageNumber;
    private Double xPosition;
    private Double yPosition;
    private Double width;
    private Double height;
    private String optionValue; // For radio buttons, the value this position represents
    
    // Getters and Setters
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
    
    public String getOptionValue() {
        return optionValue;
    }
    
    public void setOptionValue(String optionValue) {
        this.optionValue = optionValue;
    }
}


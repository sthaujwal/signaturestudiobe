package com.wellsfargo.signaturestudio.domain;

import java.util.Map;

/**
 * Domain object for dashboard statistics
 */
public class DashboardStats {
    private long totalTransactions;
    private long pendingTransactions;
    private long completedTransactions;
    private long rejectedTransactions;
    private long inProgressTransactions;
    private Map<String, Long> transactionsByStatus;
    private Map<String, Long> transactionsByPriority;
    private long transactionsThisWeek;
    private double completionRate;
    private double averageProcessingTimeDays;
    
    // Getters and Setters
    public long getTotalTransactions() {
        return totalTransactions;
    }
    
    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }
    
    public long getPendingTransactions() {
        return pendingTransactions;
    }
    
    public void setPendingTransactions(long pendingTransactions) {
        this.pendingTransactions = pendingTransactions;
    }
    
    public long getCompletedTransactions() {
        return completedTransactions;
    }
    
    public void setCompletedTransactions(long completedTransactions) {
        this.completedTransactions = completedTransactions;
    }
    
    public long getRejectedTransactions() {
        return rejectedTransactions;
    }
    
    public void setRejectedTransactions(long rejectedTransactions) {
        this.rejectedTransactions = rejectedTransactions;
    }
    
    public long getInProgressTransactions() {
        return inProgressTransactions;
    }
    
    public void setInProgressTransactions(long inProgressTransactions) {
        this.inProgressTransactions = inProgressTransactions;
    }
    
    public Map<String, Long> getTransactionsByStatus() {
        return transactionsByStatus;
    }
    
    public void setTransactionsByStatus(Map<String, Long> transactionsByStatus) {
        this.transactionsByStatus = transactionsByStatus;
    }
    
    public Map<String, Long> getTransactionsByPriority() {
        return transactionsByPriority;
    }
    
    public void setTransactionsByPriority(Map<String, Long> transactionsByPriority) {
        this.transactionsByPriority = transactionsByPriority;
    }
    
    public long getTransactionsThisWeek() {
        return transactionsThisWeek;
    }
    
    public void setTransactionsThisWeek(long transactionsThisWeek) {
        this.transactionsThisWeek = transactionsThisWeek;
    }
    
    public double getCompletionRate() {
        return completionRate;
    }
    
    public void setCompletionRate(double completionRate) {
        this.completionRate = completionRate;
    }
    
    public double getAverageProcessingTimeDays() {
        return averageProcessingTimeDays;
    }
    
    public void setAverageProcessingTimeDays(double averageProcessingTimeDays) {
        this.averageProcessingTimeDays = averageProcessingTimeDays;
    }
}


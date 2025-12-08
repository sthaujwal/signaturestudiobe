package com.wellsfargo.signaturestudio.config;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * W3C Trace Context support
 * Handles traceparent and tracestate headers according to W3C Trace Context specification
 */
public class TraceContext {
    
    public static final String TRACE_PARENT_HEADER = "traceparent";
    public static final String TRACE_STATE_HEADER = "tracestate";
    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String SPAN_ID_MDC_KEY = "spanId";
    
    private String traceId;
    private String parentId;
    private String spanId;
    private byte flags;
    private String traceState;
    
    public TraceContext() {
        // Generate new trace context
        this.traceId = generateTraceId();
        this.spanId = generateSpanId();
        this.parentId = null;
        this.flags = 0x01; // sampled flag
        this.traceState = null;
    }
    
    public TraceContext(String traceparent) {
        parseTraceParent(traceparent);
    }
    
    /**
     * Parse W3C traceparent header
     * Format: version-trace-id-parent-id-trace-flags
     * Example: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
     */
    private void parseTraceParent(String traceparent) {
        if (traceparent == null || traceparent.isEmpty()) {
            // Generate new trace context
            this.traceId = generateTraceId();
            this.spanId = generateSpanId();
            this.parentId = null;
            this.flags = 0x01;
            return;
        }
        
        String[] parts = traceparent.split("-");
        if (parts.length != 4) {
            // Invalid format, generate new
            this.traceId = generateTraceId();
            this.spanId = generateSpanId();
            this.parentId = null;
            this.flags = 0x01;
            return;
        }
        
        try {
            // parts[0] is version (currently always "00")
            this.traceId = parts[1];
            this.parentId = parts[2];
            this.spanId = generateSpanId(); // Generate new span for this service
            this.flags = (byte) Integer.parseInt(parts[3], 16);
        } catch (Exception e) {
            // Invalid format, generate new
            this.traceId = generateTraceId();
            this.spanId = generateSpanId();
            this.parentId = null;
            this.flags = 0x01;
        }
    }
    
    /**
     * Generate trace ID (32 hex characters)
     */
    private String generateTraceId() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString().replace("-", "").substring(0, 32);
    }
    
    /**
     * Generate span ID (16 hex characters)
     */
    private String generateSpanId() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString().replace("-", "").substring(0, 16);
    }
    
    /**
     * Get traceparent header value
     */
    public String getTraceParent() {
        return String.format("00-%s-%s-%02x", traceId, spanId, flags);
    }
    
    /**
     * Get tracestate header value
     */
    public String getTraceState() {
        return traceState;
    }
    
    public void setTraceState(String traceState) {
        this.traceState = traceState;
    }
    
    public String getTraceId() {
        return traceId;
    }
    
    public String getSpanId() {
        return spanId;
    }
    
    public String getParentId() {
        return parentId;
    }
    
    /**
     * Set trace context in MDC for logging
     */
    public void setInMDC() {
        MDC.put(TRACE_ID_MDC_KEY, traceId);
        MDC.put(SPAN_ID_MDC_KEY, spanId);
    }
    
    /**
     * Clear trace context from MDC
     */
    public static void clearMDC() {
        MDC.remove(TRACE_ID_MDC_KEY);
        MDC.remove(SPAN_ID_MDC_KEY);
    }
    
    /**
     * Get trace ID from MDC
     */
    public static String getTraceIdFromMDC() {
        return MDC.get(TRACE_ID_MDC_KEY);
    }
}


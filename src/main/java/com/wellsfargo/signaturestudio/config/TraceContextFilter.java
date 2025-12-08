package com.wellsfargo.signaturestudio.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to handle W3C Trace Context headers (traceparent and tracestate)
 * Extracts trace context from incoming requests and sets it in MDC for logging
 */
@Component
public class TraceContextFilter extends OncePerRequestFilter {
    
    private static final String TRACE_CONTEXT_ATTRIBUTE = "traceContext";
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, 
                                   @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            // Extract traceparent header
            String traceparent = request.getHeader(TraceContext.TRACE_PARENT_HEADER);
            String tracestate = request.getHeader(TraceContext.TRACE_STATE_HEADER);
            
            // Create or parse trace context
            TraceContext traceContext;
            if (traceparent != null && !traceparent.isEmpty()) {
                traceContext = new TraceContext(traceparent);
            } else {
                traceContext = new TraceContext();
            }
            
            if (tracestate != null && !tracestate.isEmpty()) {
                traceContext.setTraceState(tracestate);
            }
            
            // Set trace context in request attribute for use in controllers/services
            request.setAttribute(TRACE_CONTEXT_ATTRIBUTE, traceContext);
            
            // Set trace context in MDC for logging
            traceContext.setInMDC();
            
            // Add traceparent to response headers
            response.setHeader(TraceContext.TRACE_PARENT_HEADER, traceContext.getTraceParent());
            if (traceContext.getTraceState() != null) {
                response.setHeader(TraceContext.TRACE_STATE_HEADER, traceContext.getTraceState());
            }
            
            filterChain.doFilter(request, response);
        } finally {
            // Clear MDC after request
            TraceContext.clearMDC();
        }
    }
    
    /**
     * Get trace context from request
     */
    public static TraceContext getTraceContext(HttpServletRequest request) {
        return (TraceContext) request.getAttribute(TRACE_CONTEXT_ATTRIBUTE);
    }
}


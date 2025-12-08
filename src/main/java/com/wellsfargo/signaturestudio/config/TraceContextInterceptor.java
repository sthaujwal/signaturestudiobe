package com.wellsfargo.signaturestudio.config;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;

import java.io.IOException;

/**
 * RestTemplate interceptor to propagate W3C Trace Context headers to downstream services
 */
public class TraceContextInterceptor implements ClientHttpRequestInterceptor {
    
    @Override
    @NonNull
    public ClientHttpResponse intercept(
            @NonNull HttpRequest request,
            @NonNull byte[] body,
            @NonNull ClientHttpRequestExecution execution) throws IOException {
        
        // Get trace context from MDC or create new one
        String traceId = MDC.get(TraceContext.TRACE_ID_MDC_KEY);
        
        if (traceId != null && traceId.length() == 32) {
            // Preserve trace ID but create new span for downstream service
            String newSpanId = generateSpanId();
            String traceparent = String.format("00-%s-%s-01", traceId, newSpanId);
            request.getHeaders().set(TraceContext.TRACE_PARENT_HEADER, traceparent);
        } else {
            // Create new trace context
            TraceContext traceContext = new TraceContext();
            request.getHeaders().set(TraceContext.TRACE_PARENT_HEADER, traceContext.getTraceParent());
            if (traceContext.getTraceState() != null) {
                request.getHeaders().set(TraceContext.TRACE_STATE_HEADER, traceContext.getTraceState());
            }
        }
        
        return execution.execute(request, body);
    }
    
    private String generateSpanId() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}


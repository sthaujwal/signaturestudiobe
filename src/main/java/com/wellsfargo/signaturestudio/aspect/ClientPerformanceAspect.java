package com.wellsfargo.signaturestudio.aspect;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * AspectJ aspect for performance logging of client integration calls.
 * Logs class name, method name, execution time, and session ID for all client methods.
 */
@Aspect
@Component
public class ClientPerformanceAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(ClientPerformanceAspect.class);
    private static final String PERFORMANCE_LOG_PREFIX = "PERF";
    
    /**
     * Intercepts all public methods in client package classes.
     * Logs performance metrics including execution time, class name, method name, and session ID.
     */
    @Around("execution(public * com.wellsfargo.signaturestudio.client.*.*(..))")
    public Object logClientPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String sessionId = getSessionId();
        String traceId = getTraceId();
        
        Object result = null;
        Throwable exception = null;
        
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            exception = t;
            throw t;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            logPerformanceMetrics(className, methodName, executionTime, sessionId, traceId, exception);
        }
    }
    
    /**
     * Retrieves session ID from HttpServletRequest if available.
     * Falls back to trace ID from MDC if session is not available.
     */
    private String getSessionId() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                if (request != null) {
                    HttpSession session = request.getSession(false);
                    if (session != null) {
                        return session.getId();
                    }
                }
            }
        } catch (Exception e) {
            // Ignore - session may not be available in all contexts
        }
        
        // Fallback to trace ID from MDC
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : "N/A";
    }
    
    /**
     * Retrieves trace ID from MDC if available.
     */
    private String getTraceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : "N/A";
    }
    
    /**
     * Logs performance metrics in a structured format.
     */
    private void logPerformanceMetrics(
            String className,
            String methodName,
            long executionTimeMs,
            String sessionId,
            String traceId,
            Throwable exception) {
        
        String status = exception != null ? "FAILED" : "SUCCESS";
        String exceptionName = exception != null ? exception.getClass().getSimpleName() : null;
        
        // Structured log format for easy parsing
        logger.info("{} | Class: {} | Method: {} | ExecutionTime: {}ms | SessionId: {} | TraceId: {} | Status: {} {}",
            PERFORMANCE_LOG_PREFIX,
            className,
            methodName,
            executionTimeMs,
            sessionId,
            traceId,
            status,
            exceptionName != null ? "| Exception: " + exceptionName : ""
        );
        
        // Also log slow requests (threshold: 1000ms)
        if (executionTimeMs > 10000) {
            logger.info("SLOW_REQUEST | Class: {} | Method: {} | ExecutionTime: {}ms | SessionId: {} | TraceId: {}",
                className, methodName, executionTimeMs, sessionId, traceId);
        }
        
        // Log errors with stack trace
        if (exception != null) {
            logger.error("CLIENT_ERROR | Class: {} | Method: {} | ExecutionTime: {}ms | SessionId: {} | TraceId: {}",
                className, methodName, executionTimeMs, sessionId, traceId, exception);
        }
    }
}


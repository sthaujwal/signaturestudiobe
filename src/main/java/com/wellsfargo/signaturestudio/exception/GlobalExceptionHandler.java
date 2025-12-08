package com.wellsfargo.signaturestudio.exception;

import com.wellsfargo.signaturestudio.config.TraceContext;
import com.wellsfargo.signaturestudio.config.TraceContextFilter;
import com.wellsfargo.signaturestudio.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorResponseDTO> handleServiceException(ServiceException ex, WebRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        logger.error("Service exception: {} - {}", errorCode.getCode(), ex.getMessage(), ex);
        
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
            errorCode.getCode(),
            errorCode.getMessage(),
            ex.getDetailMessage()
        );
        errorResponse.setPath(getRequestPath(request));
        errorResponse.setTraceId(getTraceId(request));
        
        return ResponseEntity.status(errorCode.getHttpStatus()).body(errorResponse);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });
        
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
            ErrorCode.VALIDATION_ERROR.getCode(),
            ErrorCode.VALIDATION_ERROR.getMessage()
        );
        errorResponse.setValidationErrors(validationErrors);
        errorResponse.setPath(getRequestPath(request));
        errorResponse.setTraceId(getTraceId(request));
        
        logger.warn("Validation error: {} - Path: {}", validationErrors, getRequestPath(request), ex);
        
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getHttpStatus()).body(errorResponse);
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDTO> handleRuntimeException(RuntimeException ex, WebRequest request) {
        logger.error("Runtime exception occurred", ex);
        
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
            ErrorCode.INTERNAL_ERROR.getCode(),
            ErrorCode.INTERNAL_ERROR.getMessage(),
            ex.getMessage()
        );
        errorResponse.setPath(getRequestPath(request));
        errorResponse.setTraceId(getTraceId(request));
        
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus()).body(errorResponse);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex, WebRequest request) {
        logger.error("Unexpected exception occurred", ex);
        
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
            ErrorCode.UNEXPECTED_ERROR.getCode(),
            ErrorCode.UNEXPECTED_ERROR.getMessage()
        );
        errorResponse.setPath(getRequestPath(request));
        errorResponse.setTraceId(getTraceId(request));
        
        return ResponseEntity.status(ErrorCode.UNEXPECTED_ERROR.getHttpStatus()).body(errorResponse);
    }
    
    private String getRequestPath(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            return ((ServletWebRequest) request).getRequest().getRequestURI();
        }
        return null;
    }
    
    private String getTraceId(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            HttpServletRequest httpRequest = ((ServletWebRequest) request).getRequest();
            TraceContext traceContext = TraceContextFilter.getTraceContext(httpRequest);
            if (traceContext != null) {
                return traceContext.getTraceId();
            }
        }
        return TraceContext.getTraceIdFromMDC();
    }
}



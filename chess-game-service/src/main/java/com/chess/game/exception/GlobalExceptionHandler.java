package com.chess.game.exception;

import com.chess.common.dto.ErrorResponse;
import com.chess.common.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        String traceId = request.getHeader("X-Request-Id");
        log.warn("Not found: {} [traceId={}]", ex.getMessage(), traceId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.builder()
                .error(ex.getCode())
                .message(ex.getMessage())
                .traceId(traceId)
                .build());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex, HttpServletRequest request) {
        String traceId = request.getHeader("X-Request-Id");
        log.warn("Validation error: {} [traceId={}]", ex.getMessage(), traceId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.builder()
                .error(ex.getCode())
                .message(ex.getMessage())
                .traceId(traceId)
                .details(ex.getDetails())
                .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String traceId = request.getHeader("X-Request-Id");
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String key = error instanceof FieldError fe ? fe.getField() : error.getObjectName();
            fieldErrors.put(key, error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid");
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.builder()
                .error("VALIDATION_ERROR")
                .message("Invalid request parameters")
                .traceId(traceId)
                .details(fieldErrors)
                .build());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        String traceId = request.getHeader("X-Request-Id");
        log.warn("Unauthorized: {} [traceId={}]", ex.getMessage(), traceId);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse.builder()
                .error(ex.getCode())
                .message(ex.getMessage())
                .traceId(traceId)
                .build());
    }

    @ExceptionHandler({AccessDeniedException.class, ForbiddenException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(Exception ex, HttpServletRequest request) {
        String traceId = request.getHeader("X-Request-Id");
        log.warn("Forbidden: {} [traceId={}]", ex.getMessage(), traceId);
        String code = ex instanceof ForbiddenException fe ? fe.getCode() : "FORBIDDEN";
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.builder()
                .error(code)
                .message(ex.getMessage() != null ? ex.getMessage() : "Access denied")
                .traceId(traceId)
                .build());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        String traceId = request.getHeader("X-Request-Id");
        log.warn("Conflict: {} [traceId={}]", ex.getMessage(), traceId);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.builder()
                .error(ex.getCode())
                .message(ex.getMessage())
                .traceId(traceId)
                .build());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
        String traceId = request.getHeader("X-Request-Id");
        log.warn("Business error: {} [traceId={}]", ex.getMessage(), traceId);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ErrorResponse.builder()
                .error(ex.getCode())
                .message(ex.getMessage())
                .traceId(traceId)
                .details(ex.getDetails())
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        String traceId = request.getHeader("X-Request-Id");
        log.error("Unexpected error [traceId={}]", traceId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.builder()
                .error("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred")
                .traceId(traceId)
                .build());
    }
}


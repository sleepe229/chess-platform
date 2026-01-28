package com.chess.user.exception;

import com.chess.common.dto.ErrorResponse;
import com.chess.common.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(
            NotFoundException ex,
            HttpServletRequest request) {

        String traceId = request.getHeader("X-Request-Id");
        log.warn("Not found: {} [traceId={}]", ex.getMessage(), traceId);

        ErrorResponse error = ErrorResponse.builder()
                .error(ex.getCode())
                .message(ex.getMessage())
                .traceId(traceId)
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex,
            HttpServletRequest request) {

        String traceId = request.getHeader("X-Request-Id");
        log.warn("Validation error: {} [traceId={}]", ex.getMessage(), traceId);

        ErrorResponse error = ErrorResponse.builder()
                .error(ex.getCode())
                .message(ex.getMessage())
                .traceId(traceId)
                .details(ex.getDetails())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String traceId = request.getHeader("X-Request-Id");

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String key;
            if (error instanceof FieldError) {
                key = ((FieldError) error).getField();
            } else {
                key = error.getObjectName();
            }

            String message = error.getDefaultMessage();
            fieldErrors.put(key, message != null ? message : "Invalid value");
        });

        ErrorResponse error = ErrorResponse.builder()
                .error("VALIDATION_ERROR")
                .message("Invalid request parameters")
                .traceId(traceId)
                .details(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String traceId = request.getHeader("X-Request-Id");
        String message = ex.getName() != null
                ? String.format("Parameter '%s' has invalid value", ex.getName())
                : "Invalid parameter value";
        if (ex.getRequiredType() != null && ex.getRequiredType().getSimpleName().equals("UUID")) {
            message = "Invalid UUID format";
        }
        log.warn("Type mismatch: {} [traceId={}]", message, traceId);

        ErrorResponse error = ErrorResponse.builder()
                .error("INVALID_PARAMETER")
                .message(message)
                .traceId(traceId)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(
            UnauthorizedException ex,
            HttpServletRequest request) {

        String traceId = request.getHeader("X-Request-Id");
        log.warn("Unauthorized: {} [traceId={}]", ex.getMessage(), traceId);

        ErrorResponse error = ErrorResponse.builder()
                .error(ex.getCode())
                .message(ex.getMessage())
                .traceId(traceId)
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {

        String traceId = request.getHeader("X-Request-Id");
        log.warn("Access denied: {} [traceId={}]", ex.getMessage(), traceId);

        ErrorResponse error = ErrorResponse.builder()
                .error("FORBIDDEN")
                .message(ex.getMessage() != null ? ex.getMessage() : "Access denied")
                .traceId(traceId)
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(
            ForbiddenException ex,
            HttpServletRequest request) {

        String traceId = request.getHeader("X-Request-Id");
        log.warn("Forbidden: {} [traceId={}]", ex.getMessage(), traceId);

        ErrorResponse error = ErrorResponse.builder()
                .error(ex.getCode())
                .message(ex.getMessage())
                .traceId(traceId)
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(
            ConflictException ex,
            HttpServletRequest request) {

        String traceId = request.getHeader("X-Request-Id");
        log.warn("Conflict: {} [traceId={}]", ex.getMessage(), traceId);

        ErrorResponse error = ErrorResponse.builder()
                .error(ex.getCode())
                .message(ex.getMessage())
                .traceId(traceId)
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request) {

        String traceId = request.getHeader("X-Request-Id");
        log.warn("Business error: {} [traceId={}]", ex.getMessage(), traceId);

        ErrorResponse error = ErrorResponse.builder()
                .error(ex.getCode())
                .message(ex.getMessage())
                .traceId(traceId)
                .details(ex.getDetails())
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        String traceId = request.getHeader("X-Request-Id");
        log.error("Unexpected error [traceId={}]", traceId, ex);

        ErrorResponse error = ErrorResponse.builder()
                .error("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred")
                .traceId(traceId)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

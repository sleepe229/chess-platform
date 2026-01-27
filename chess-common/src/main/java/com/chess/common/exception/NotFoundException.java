package com.chess.common.exception;

public class NotFoundException extends BusinessException {
    public NotFoundException(String entityName, Object id) {
        super("NOT_FOUND", String.format("%s with id %s not found", entityName, id));
    }

    public NotFoundException(String message) {
        super("NOT_FOUND", message);
    }
}

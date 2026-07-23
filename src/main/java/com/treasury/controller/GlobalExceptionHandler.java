package com.treasury.controller;

import com.treasury.domain.ExceptionCategory;
import com.treasury.domain.ExceptionCaseType;
import com.treasury.domain.ExceptionSeverity;
import com.treasury.service.ExceptionCaseService;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ExceptionCaseService exceptionCaseService;

    public GlobalExceptionHandler(ExceptionCaseService exceptionCaseService) {
        this.exceptionCaseService = exceptionCaseService;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse badRequest(IllegalArgumentException exception) {
        return new ErrorResponse(exception.getMessage(), Map.of());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse conflict(IllegalStateException exception) {
        return new ErrorResponse(exception.getMessage(), Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse forbidden() {
        return new ErrorResponse("当前岗位没有执行此操作的权限", Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse unauthorized() {
        return new ErrorResponse("账号或密码错误", Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse validation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return new ErrorResponse("请检查表单内容", fields);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse systemException(Exception exception) {
        LOGGER.error("请求处理发生未预期系统异常", exception);
        try {
            String exceptionName = exception.getClass().getSimpleName();
            exceptionCaseService.register(
                    ExceptionCategory.SYSTEM,
                    ExceptionCaseType.SYSTEM_RUNTIME, ExceptionSeverity.HIGH,
                    "系统运行异常",
                    "请求处理发生未预期异常，请结合服务日志进一步排查",
                    "SYSTEM_RUNTIME", exceptionName, exceptionName
            );
        } catch (Exception registrationException) {
            LOGGER.warn("系统异常工单登记失败", registrationException);
        }
        return new ErrorResponse("系统暂时无法处理请求，技术异常已登记，请稍后重试", Map.of());
    }

    public record ErrorResponse(String message, Map<String, String> fields) {
    }
}

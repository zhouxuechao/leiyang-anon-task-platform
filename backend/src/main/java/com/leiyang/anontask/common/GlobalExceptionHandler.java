package com.leiyang.anontask.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BizException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResult<Void> handleBiz(BizException e) {
    log.info("biz_exception message={}", e.getMessage());
    return ApiResult.fail(e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResult<Void> handleValidation(MethodArgumentNotValidException e) {
    String msg = e.getBindingResult().getAllErrors().isEmpty()
        ? "Validation failed"
        : e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
    log.info("validation_exception message={}", msg);
    return ApiResult.fail(msg);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResult<Void> handleConstraint(ConstraintViolationException e) {
    log.info("constraint_exception message={}", e.getMessage());
    return ApiResult.fail(e.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResult<Void> handleIllegal(IllegalArgumentException e) {
    log.info("illegal_argument_exception message={}", e.getMessage());
    return ApiResult.fail(e.getMessage());
  }

  @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResult<Void> handleMissingParam(org.springframework.web.bind.MissingServletRequestParameterException e) {
    log.info("missing_param_exception message={}", e.getMessage());
    return ApiResult.fail(e.getMessage());
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiResult<Void> handleUnexpected(Exception e) {
    log.error("unexpected_exception", e);
    return ApiResult.fail("系统异常，请稍后再试");
  }
}

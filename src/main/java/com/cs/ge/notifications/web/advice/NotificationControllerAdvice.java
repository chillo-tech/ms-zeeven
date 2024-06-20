package com.cs.ge.notifications.web.advice;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class NotificationControllerAdvice
        extends ResponseEntityExceptionHandler {
    @ExceptionHandler(value
            = {IllegalArgumentException.class, IllegalStateException.class})
    protected static ResponseEntity<Object> handleConflict(
            final RuntimeException ex, final WebRequest request) {
        return new ResponseEntity<Object>(
                Map.of(
                        "status", HttpStatus.BAD_REQUEST.value(),
                        "message", ex.getMessage()
                ), new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }
}

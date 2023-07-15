package com.cs.ge.controllers.advice;

import com.cs.ge.dto.ExceptionData;
import com.cs.ge.exception.ApplicationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@ControllerAdvice
public class ApplicationExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ExceptionData handleApplicationException(final ApplicationException applicationException) {
        log.error("Erreur", applicationException);
        final ExceptionData exceptionData = new ExceptionData();
        exceptionData.setTimestamp(LocalDateTime.now());
        exceptionData.setMessage(applicationException.getMessage());
        return exceptionData;

    }

    @ExceptionHandler({StripeException.class, SignatureVerificationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ExceptionData handleStripeException(final StripeException stripeException) {
        log.error("Erreur", stripeException);
        final ExceptionData exceptionData = new ExceptionData();
        exceptionData.setTimestamp(LocalDateTime.now());
        exceptionData.setMessage("Une erreur est survenue lors de votre paiment veuilez nous contacter");
        return exceptionData;

    }


    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ExceptionData handleIllegalArgumentException(final IllegalArgumentException exception) {
        log.error("Erreur", exception);
        final ExceptionData exceptionData = new ExceptionData();
        exceptionData.setTimestamp(LocalDateTime.now());
        exceptionData.setMessage(exception.getMessage());
        return exceptionData;
    }

    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ExceptionData handleIOException(final IOException exception) {
        log.error("Erreur", exception);
        final ExceptionData exceptionData = new ExceptionData();
        exceptionData.setTimestamp(LocalDateTime.now());
        exceptionData.setMessage(exception.getMessage());
        return exceptionData;
    }

}

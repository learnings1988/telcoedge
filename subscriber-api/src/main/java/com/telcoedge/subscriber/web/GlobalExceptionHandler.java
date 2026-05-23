package com.telcoedge.subscriber.web;


import com.telcoedge.subscriber.exception.SubscriberAlreadyExistException;
import com.telcoedge.subscriber.exception.SubscriberNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;


@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(
            GlobalExceptionHandler.class);

    @ExceptionHandler(SubscriberNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(SubscriberNotFoundException ex,
                                        HttpServletRequest req){
        return ErrorResponse.of( 404, "Not Found", ex.getMessage(),
                req.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex,
                                          HttpServletRequest req){
        List<ErrorResponse.FieldError> fieldErrors =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(fe -> new ErrorResponse.FieldError(
                                fe.getField(), fe.getDefaultMessage()
                        )).toList();
        return ErrorResponse.withFieldErrors(400, "Bad Request", req.getRequestURI(),
                fieldErrors);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(Exception ex,
                         HttpServletRequest req){
        log.error("Unexpected error at {}", req.getRequestURI(), ex);
        return ErrorResponse.of(500, "Internal Server Error",
                "An Unexpected error occured", req.getRequestURI());
    }

    @ExceptionHandler(SubscriberAlreadyExistException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(SubscriberAlreadyExistException ex,
                                        HttpServletRequest req){
        return ErrorResponse.of(409, "Conflict", ex.getMessage(),
                req.getRequestURI());
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnreadable(HttpMessageNotReadableException ex,
                                          HttpServletRequest req){
        return ErrorResponse.of(400, "Bad Request", "Request body is missing or malformed",
                req.getRequestURI());
    }

}

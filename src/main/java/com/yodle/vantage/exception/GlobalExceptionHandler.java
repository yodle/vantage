/*
 * Copyright 2016 Yodle, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *     http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yodle.vantage.exception;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Adapted from spring ResponseEntityExceptionHandler
    private final Map<Class, HttpStatus> exceptionStatusCodeMap = ImmutableMap.<Class, HttpStatus>builder()
            .put(NoSuchRequestHandlingMethodException.class, HttpStatus.NOT_FOUND)
            .put(HttpRequestMethodNotSupportedException.class, HttpStatus.METHOD_NOT_ALLOWED)
            .put(HttpMediaTypeNotSupportedException.class, HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .put(HttpMediaTypeNotAcceptableException.class, HttpStatus.NOT_ACCEPTABLE)
            .put(MissingServletRequestParameterException.class, HttpStatus.BAD_REQUEST)
            .put(ServletRequestBindingException.class, HttpStatus.BAD_REQUEST)
            .put(ConversionNotSupportedException.class, HttpStatus.INTERNAL_SERVER_ERROR)
            .put(TypeMismatchException.class, HttpStatus.BAD_REQUEST)
            .put(HttpMessageNotReadableException.class, HttpStatus.BAD_REQUEST)
            .put(HttpMessageNotWritableException.class, HttpStatus.INTERNAL_SERVER_ERROR)
            .put(MethodArgumentNotValidException.class, HttpStatus.BAD_REQUEST)
            .put(MissingServletRequestPartException.class, HttpStatus.BAD_REQUEST)
            .put(BindException.class, HttpStatus.BAD_REQUEST)
            .put(NoHandlerFoundException.class, HttpStatus.NOT_FOUND)
            .put(NoComponentFoundException.class, HttpStatus.NOT_FOUND)
            .put(NoVersionFoundException.class, HttpStatus.NOT_FOUND)
            .put(NoIssueFoundException.class, HttpStatus.NOT_FOUND)
            .build();

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(HttpServletRequest request, Exception exception) {
        final HttpStatus httpStatus = exceptionStatusCodeMap.getOrDefault(exception.getClass(), HttpStatus.INTERNAL_SERVER_ERROR);
        final Map<String, Object> errorResponse = buildErrorResponse(httpStatus, request, exception);

        if (httpStatus.is5xxServerError()) {
            logger.error("Server error: {}", errorResponse.toString(), exception);
        } else {
            logger.debug("Client error: {}", errorResponse.toString());
        }

        return new ResponseEntity<>(errorResponse, httpStatus);
    }

    private Map<String, Object> buildErrorResponse(HttpStatus httpStatus, HttpServletRequest request, Exception e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", DateTime.now().toString());
        errorResponse.put("status", httpStatus.value());
        errorResponse.put("error", httpStatus.getReasonPhrase());
        errorResponse.put("exception", e.getClass().getCanonicalName());
        errorResponse.put("message", e.getMessage());
        errorResponse.put("path", request.getRequestURI());
        return errorResponse;
    }
}

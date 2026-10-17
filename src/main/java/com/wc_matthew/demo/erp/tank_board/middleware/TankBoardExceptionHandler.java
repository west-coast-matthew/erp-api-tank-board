package com.wc_matthew.demo.erp.tank_board.middleware;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.wc_matthew.demo.erp.core.constants.ErrorConstants;
import com.wc_matthew.demo.erp.core.exception.BaseException;
import com.wc_matthew.demo.erp.core.exception.InvalidRequestException;
import com.wc_matthew.demo.erp.core.middleware.GlobalExceptionHandler;
import com.wc_matthew.demo.erp.core.service.ExceptionHandlerService;

/**
 * Runs ahead of erp-core-java's {@link GlobalExceptionHandler}, which builds the error headers
 * but never attaches them to the response, and fails on exceptions that carry no HTTP status
 * (none of the core exceptions set one). Same headers, attached, with a status derived from
 * the error code. Anything not handled here still falls through to the core handler.
 */
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TankBoardExceptionHandler {

	@Autowired
	ExceptionHandlerService exceptionService;

	@ExceptionHandler(BaseException.class)
	public ResponseEntity<String> handleBaseException(BaseException ex) {
		exceptionService.logException(ex);

		HttpHeaders headers = new HttpHeaders();
		headers.set(GlobalExceptionHandler.HEADER_ERROR_CODE, ex.getErrCode());
		headers.set(GlobalExceptionHandler.HEADER_DEFAULT_MESSAGE, ex.getMessage());
		headers.set(GlobalExceptionHandler.HEADER_MESSAGE, ex.getCustomMessage());
		return new ResponseEntity<>(ex.getMessage(), headers, statusFor(ex));
	}

	@ExceptionHandler({ MissingRequestHeaderException.class, MethodArgumentTypeMismatchException.class,
			HttpMessageNotReadableException.class })
	public ResponseEntity<String> handleBadRequest(Exception ex) {
		return handleBaseException(new InvalidRequestException(ex.getMessage()));
	}

	static HttpStatusCode statusFor(BaseException ex) {
		if (ex.getHttpStatusCode() != null) {
			return ex.getHttpStatusCode();
		}
		String errCode = ex.getErrCode() == null ? "" : ex.getErrCode();
		return switch (errCode) {
			case ErrorConstants.ENTITY_NOT_FOUND -> HttpStatus.NOT_FOUND;
			case ErrorConstants.ENTITY_EXISTS -> HttpStatus.CONFLICT;
			case ErrorConstants.INVALID_REQUEST, ErrorConstants.BAD_REQUEST, ErrorConstants.ENTITY_VALIDATION_ERR -> HttpStatus.BAD_REQUEST;
			case ErrorConstants.NOT_LOGGED_IN, ErrorConstants.INVALID_SECURITY_TOKEN -> HttpStatus.UNAUTHORIZED;
			default -> HttpStatus.INTERNAL_SERVER_ERROR;
		};
	}
}

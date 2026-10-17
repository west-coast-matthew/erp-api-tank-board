package com.wc_matthew.demo.erp.tank_board.controller;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Same helpers as erp-api-entity-mgmnt-java's BaseController. */
public abstract class BaseController {

	String getCurrentTransactionId() {
		return "1234";
	}

	URI getLocationRef(Long id) {
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(id)
				.toUri();
		return location;
	}

	HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("transaction-id", getCurrentTransactionId());
		return headers;
	}
}

package com.wc_matthew.demo.erp.tank_board.vo;

import java.util.Locale;

import com.wc_matthew.demo.erp.core.exception.InvalidRequestException;

import lombok.Value;

/**
 * The caller of a tank board request: who they are, their role and their assigned facility.
 * Mirrors the tank-board UI's CurrentUser (username, role, facility.id).
 */
@Value
public class TankBoardUserVO {
	String username;
	/** Trimmed and lower-cased, the form tank board roles are stored in. */
	String role;
	Long facilityId;

	public static TankBoardUserVO of(String username, String role, Long facilityId) {
		if (username == null || username.isBlank()) {
			throw new InvalidRequestException("A username is required");
		}
		if (role == null || role.isBlank()) {
			throw new InvalidRequestException("A user role is required");
		}
		if (facilityId == null) {
			throw new InvalidRequestException("A facility id is required");
		}
		return new TankBoardUserVO(username.trim(), role.trim().toLowerCase(Locale.ROOT), facilityId);
	}
}

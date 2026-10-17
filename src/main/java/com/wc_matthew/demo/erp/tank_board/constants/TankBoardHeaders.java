package com.wc_matthew.demo.erp.tank_board.constants;

/**
 * Request headers identifying the caller. A stand-in until erp-core-java's SecurityService
 * validates tokens and the user can be read from the session instead.
 */
public final class TankBoardHeaders {

	public static final String USERNAME = "erp-username";
	public static final String USER_ROLE = "erp-user-role";
	public static final String FACILITY_ID = "erp-facility-id";

	private TankBoardHeaders() {
	}
}

package com.wc_matthew.demo.erp.tank_board.constants;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * SHARED boards are the templates common to a facility, visible by role. CLONE boards are a
 * user's own copy of a shared board. Serialized as "shared" / "clone", the values the
 * tank-board UI already uses for a facility map's kind.
 */
public enum TankBoardKind {

	SHARED("shared"),
	CLONE("clone");

	private final String value;

	TankBoardKind(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}
}

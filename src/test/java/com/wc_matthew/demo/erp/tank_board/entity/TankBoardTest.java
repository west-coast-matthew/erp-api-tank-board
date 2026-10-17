package com.wc_matthew.demo.erp.tank_board.entity;

import org.junit.Test;

import com.wc_matthew.demo.erp.core.exception.EntityValidationFailureException;
import com.wc_matthew.demo.erp.tank_board.constants.TankBoardKind;

public class TankBoardTest {

	@Test
	public void validateKind_sharedWithoutOwner_passes() {
		board(TankBoardKind.SHARED, null, null).validateKind();
	}

	@Test
	public void validateKind_cloneWithOwnerAndSource_passes() {
		board(TankBoardKind.CLONE, "teammember", new TankBoard()).validateKind();
	}

	@Test(expected = EntityValidationFailureException.class)
	public void validateKind_sharedWithOwner_fails() {
		board(TankBoardKind.SHARED, "teammember", null).validateKind();
	}

	@Test(expected = EntityValidationFailureException.class)
	public void validateKind_sharedWithSource_fails() {
		board(TankBoardKind.SHARED, null, new TankBoard()).validateKind();
	}

	@Test(expected = EntityValidationFailureException.class)
	public void validateKind_cloneWithoutOwner_fails() {
		board(TankBoardKind.CLONE, null, new TankBoard()).validateKind();
	}

	@Test(expected = EntityValidationFailureException.class)
	public void validateKind_cloneWithoutSource_fails() {
		board(TankBoardKind.CLONE, "teammember", null).validateKind();
	}

	private static TankBoard board(TankBoardKind kind, String owner, TankBoard source) {
		TankBoard board = new TankBoard();
		board.setKind(kind);
		board.setOwnerUsername(owner);
		board.setSource(source);
		return board;
	}
}

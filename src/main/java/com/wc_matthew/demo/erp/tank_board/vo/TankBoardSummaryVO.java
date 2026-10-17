package com.wc_matthew.demo.erp.tank_board.vo;

import com.wc_matthew.demo.erp.tank_board.constants.TankBoardKind;

import lombok.Builder;
import lombok.Value;

/**
 * A tank board as listed on the tank-board screen. Same shape as the UI's FacilityMapSummary,
 * except that ids are the numeric database ids.
 */
@Value
@Builder
public class TankBoardSummaryVO {
	Long id;
	String title;
	String description;
	Long facilityId;
	TankBoardKind kind;
	/** For clones: the shared board this was cloned from. */
	Long sourceId;
	String sourceTitle;
}

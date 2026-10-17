package com.wc_matthew.demo.erp.tank_board.vo;

import java.time.Instant;
import java.util.Map;

import com.wc_matthew.demo.erp.tank_board.constants.TankBoardKind;

import lombok.Builder;
import lombok.Value;

/** One tank board opened on its own: the summary fields plus its settings. */
@Value
@Builder
public class TankBoardDetailVO {
	Long id;
	String title;
	String description;
	Long facilityId;
	TankBoardKind kind;
	Long sourceId;
	String sourceTitle;
	Map<String, Object> settings;
	Instant createdAt;
	Instant updatedAt;
}

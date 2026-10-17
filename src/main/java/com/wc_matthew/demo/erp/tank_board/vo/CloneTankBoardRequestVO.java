package com.wc_matthew.demo.erp.tank_board.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Body of a clone request. Same shape as the UI's CloneFacilityMapInput. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CloneTankBoardRequestVO {
	/** The shared board to copy. */
	private Long sourceId;
	private String title;
	private String description;
}

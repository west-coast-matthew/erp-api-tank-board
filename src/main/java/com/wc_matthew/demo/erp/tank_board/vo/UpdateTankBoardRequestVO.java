package com.wc_matthew.demo.erp.tank_board.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Body of a request renaming a cloned board or changing its description. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTankBoardRequestVO {
	private String title;
	private String description;
}

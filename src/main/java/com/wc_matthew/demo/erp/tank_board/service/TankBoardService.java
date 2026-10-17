package com.wc_matthew.demo.erp.tank_board.service;

import java.util.List;
import java.util.Map;

import com.wc_matthew.demo.erp.tank_board.vo.CloneTankBoardRequestVO;
import com.wc_matthew.demo.erp.tank_board.vo.TankBoardDetailVO;
import com.wc_matthew.demo.erp.tank_board.vo.TankBoardSummaryVO;
import com.wc_matthew.demo.erp.tank_board.vo.TankBoardUserVO;
import com.wc_matthew.demo.erp.tank_board.vo.UpdateTankBoardRequestVO;

/**
 * Tank boards for a user. Only the user's own clones can be changed or deleted; shared boards
 * are read-only templates. A board the user cannot see is reported as not found.
 */
public interface TankBoardService {

	/** Shared boards for the user's facility that their role may see, ordered by title. */
	public List<TankBoardSummaryVO> listTemplates(TankBoardUserVO user);

	/** The user's own cloned boards, oldest first. */
	public List<TankBoardSummaryVO> listBoards(TankBoardUserVO user);

	/** A shared board the user may see, or one of their own clones. */
	public TankBoardDetailVO getBoard(TankBoardUserVO user, Long id);

	/** Copies a shared board, settings included, as a new clone owned by the user. */
	public TankBoardSummaryVO cloneBoard(TankBoardUserVO user, CloneTankBoardRequestVO request);

	/** Changes a clone's title and description. */
	public TankBoardDetailVO updateBoard(TankBoardUserVO user, Long id, UpdateTankBoardRequestVO request);

	/** Replaces a clone's settings. */
	public TankBoardDetailVO saveSettings(TankBoardUserVO user, Long id, Map<String, Object> settings);

	/** Archives a clone (soft delete, like the entity management services). */
	public void deleteBoard(TankBoardUserVO user, Long id);

}

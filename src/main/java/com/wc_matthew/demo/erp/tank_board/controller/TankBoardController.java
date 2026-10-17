package com.wc_matthew.demo.erp.tank_board.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wc_matthew.demo.erp.tank_board.constants.TankBoardHeaders;
import com.wc_matthew.demo.erp.tank_board.service.TankBoardService;
import com.wc_matthew.demo.erp.tank_board.vo.CloneTankBoardRequestVO;
import com.wc_matthew.demo.erp.tank_board.vo.TankBoardDetailVO;
import com.wc_matthew.demo.erp.tank_board.vo.TankBoardSummaryVO;
import com.wc_matthew.demo.erp.tank_board.vo.TankBoardUserVO;
import com.wc_matthew.demo.erp.tank_board.vo.UpdateTankBoardRequestVO;

/**
 * Tank boards for the calling user, identified by the {@link TankBoardHeaders} request headers.
 * The tank-board screen's list is GET /templates followed by GET /boards.
 */
@RestController
@RequestMapping("/tank-board")
public class TankBoardController extends BaseController {

	@Autowired
	TankBoardService tankBoardService;

	@GetMapping("/templates")
	public ResponseEntity<List<TankBoardSummaryVO>> listTemplates(
			@RequestHeader(TankBoardHeaders.USERNAME) String username,
			@RequestHeader(TankBoardHeaders.USER_ROLE) String role,
			@RequestHeader(TankBoardHeaders.FACILITY_ID) Long facilityId) {
		TankBoardUserVO user = TankBoardUserVO.of(username, role, facilityId);
		return ResponseEntity.ok().headers(getHeaders()).body(tankBoardService.listTemplates(user));
	}

	@GetMapping("/boards")
	public ResponseEntity<List<TankBoardSummaryVO>> listBoards(
			@RequestHeader(TankBoardHeaders.USERNAME) String username,
			@RequestHeader(TankBoardHeaders.USER_ROLE) String role,
			@RequestHeader(TankBoardHeaders.FACILITY_ID) Long facilityId) {
		TankBoardUserVO user = TankBoardUserVO.of(username, role, facilityId);
		return ResponseEntity.ok().headers(getHeaders()).body(tankBoardService.listBoards(user));
	}

	@GetMapping("/boards/{id}")
	public ResponseEntity<TankBoardDetailVO> getBoard(
			@RequestHeader(TankBoardHeaders.USERNAME) String username,
			@RequestHeader(TankBoardHeaders.USER_ROLE) String role,
			@RequestHeader(TankBoardHeaders.FACILITY_ID) Long facilityId,
			@PathVariable("id") Long id) {
		TankBoardUserVO user = TankBoardUserVO.of(username, role, facilityId);
		return ResponseEntity.ok().headers(getHeaders()).body(tankBoardService.getBoard(user, id));
	}

	/** Clones a shared board. 201 with the new clone and its Location. */
	@PostMapping("/boards")
	public ResponseEntity<TankBoardSummaryVO> cloneBoard(
			@RequestHeader(TankBoardHeaders.USERNAME) String username,
			@RequestHeader(TankBoardHeaders.USER_ROLE) String role,
			@RequestHeader(TankBoardHeaders.FACILITY_ID) Long facilityId,
			@RequestBody CloneTankBoardRequestVO request) {
		TankBoardUserVO user = TankBoardUserVO.of(username, role, facilityId);
		TankBoardSummaryVO clone = tankBoardService.cloneBoard(user, request);
		return ResponseEntity.created(getLocationRef(clone.getId())).headers(getHeaders()).body(clone);
	}

	@PutMapping("/boards/{id}")
	public ResponseEntity<TankBoardDetailVO> updateBoard(
			@RequestHeader(TankBoardHeaders.USERNAME) String username,
			@RequestHeader(TankBoardHeaders.USER_ROLE) String role,
			@RequestHeader(TankBoardHeaders.FACILITY_ID) Long facilityId,
			@PathVariable("id") Long id,
			@RequestBody UpdateTankBoardRequestVO request) {
		TankBoardUserVO user = TankBoardUserVO.of(username, role, facilityId);
		return ResponseEntity.ok().headers(getHeaders()).body(tankBoardService.updateBoard(user, id, request));
	}

	/** Replaces the clone's settings with the request body (any JSON object). */
	@PutMapping("/boards/{id}/settings")
	public ResponseEntity<TankBoardDetailVO> saveSettings(
			@RequestHeader(TankBoardHeaders.USERNAME) String username,
			@RequestHeader(TankBoardHeaders.USER_ROLE) String role,
			@RequestHeader(TankBoardHeaders.FACILITY_ID) Long facilityId,
			@PathVariable("id") Long id,
			@RequestBody Map<String, Object> settings) {
		TankBoardUserVO user = TankBoardUserVO.of(username, role, facilityId);
		return ResponseEntity.ok().headers(getHeaders()).body(tankBoardService.saveSettings(user, id, settings));
	}

	/** Archives a clone. 204. */
	@DeleteMapping("/boards/{id}")
	public ResponseEntity<Void> deleteBoard(
			@RequestHeader(TankBoardHeaders.USERNAME) String username,
			@RequestHeader(TankBoardHeaders.USER_ROLE) String role,
			@RequestHeader(TankBoardHeaders.FACILITY_ID) Long facilityId,
			@PathVariable("id") Long id) {
		TankBoardUserVO user = TankBoardUserVO.of(username, role, facilityId);
		tankBoardService.deleteBoard(user, id);
		return ResponseEntity.noContent().headers(getHeaders()).build();
	}
}

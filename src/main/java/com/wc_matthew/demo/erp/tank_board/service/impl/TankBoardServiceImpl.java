package com.wc_matthew.demo.erp.tank_board.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wc_matthew.demo.erp.core.exception.EntityNotFoundException;
import com.wc_matthew.demo.erp.core.exception.InvalidRequestException;
import com.wc_matthew.demo.erp.tank_board.constants.TankBoardKind;
import com.wc_matthew.demo.erp.tank_board.entity.TankBoard;
import com.wc_matthew.demo.erp.tank_board.repo.TankBoardRepo;
import com.wc_matthew.demo.erp.tank_board.service.TankBoardService;
import com.wc_matthew.demo.erp.tank_board.vo.CloneTankBoardRequestVO;
import com.wc_matthew.demo.erp.tank_board.vo.TankBoardDetailVO;
import com.wc_matthew.demo.erp.tank_board.vo.TankBoardSummaryVO;
import com.wc_matthew.demo.erp.tank_board.vo.TankBoardUserVO;
import com.wc_matthew.demo.erp.tank_board.vo.UpdateTankBoardRequestVO;

@Service("TankBoardService")
@Transactional(readOnly = true)
public class TankBoardServiceImpl implements TankBoardService {

	@Autowired
	TankBoardRepo repo;

	@Override
	public List<TankBoardSummaryVO> listTemplates(TankBoardUserVO user) {
		return repo.findActiveByFacilityAndRole(TankBoardKind.SHARED, user.getFacilityId(), user.getRole())
				.stream().map(this::toSummary).toList();
	}

	@Override
	public List<TankBoardSummaryVO> listBoards(TankBoardUserVO user) {
		return repo.findActiveByOwner(TankBoardKind.CLONE, user.getUsername())
				.stream().map(this::toSummary).toList();
	}

	@Override
	public TankBoardDetailVO getBoard(TankBoardUserVO user, Long id) {
		return toDetail(getVisibleBoard(user, id));
	}

	@Override
	@Transactional
	public TankBoardSummaryVO cloneBoard(TankBoardUserVO user, CloneTankBoardRequestVO request) {
		if (request == null || request.getSourceId() == null) {
			throw new InvalidRequestException("A source board id is required");
		}
		TankBoard source = getVisibleBoard(user, request.getSourceId());
		if (source.getKind() != TankBoardKind.SHARED) {
			throw new InvalidRequestException("Only shared tank boards can be cloned");
		}

		TankBoard clone = new TankBoard();
		clone.setKind(TankBoardKind.CLONE);
		clone.setName(validTitle(request.getTitle()));
		clone.setDescription(trimToNull(request.getDescription()));
		clone.setFacility(source.getFacility());
		clone.setOwnerUsername(user.getUsername());
		clone.setSource(source);
		clone.setSettings(copyOf(source.getSettings()));
		return toSummary(repo.save(clone));
	}

	@Override
	@Transactional
	public TankBoardDetailVO updateBoard(TankBoardUserVO user, Long id, UpdateTankBoardRequestVO request) {
		if (request == null) {
			throw new InvalidRequestException("A title is required");
		}
		TankBoard board = getOwnedClone(user, id);
		board.setName(validTitle(request.getTitle()));
		board.setDescription(trimToNull(request.getDescription()));
		return toDetail(repo.save(board));
	}

	@Override
	@Transactional
	public TankBoardDetailVO saveSettings(TankBoardUserVO user, Long id, Map<String, Object> settings) {
		TankBoard board = getOwnedClone(user, id);
		board.setSettings(copyOf(settings));
		return toDetail(repo.save(board));
	}

	@Override
	@Transactional
	public void deleteBoard(TankBoardUserVO user, Long id) {
		TankBoard board = getOwnedClone(user, id);
		board.setActive(false);
		repo.save(board);
	}

	/****************************************************************************************
	 * Lookups
	 ****************************************************************************************/

	/** The board, if it is active and either shared with the user or their own clone. */
	TankBoard getVisibleBoard(TankBoardUserVO user, Long id) {
		TankBoard board = repo.findById(id).orElse(null);
		if (board == null || !board.isActive() || !isVisibleTo(board, user)) {
			throw new EntityNotFoundException("Unable to locate tank board " + id);
		}
		return board;
	}

	TankBoard getOwnedClone(TankBoardUserVO user, Long id) {
		TankBoard board = getVisibleBoard(user, id);
		if (board.getKind() != TankBoardKind.CLONE) {
			throw new InvalidRequestException("Shared tank board " + id + " cannot be changed; clone it first");
		}
		return board;
	}

	static boolean isVisibleTo(TankBoard board, TankBoardUserVO user) {
		if (board.getKind() == TankBoardKind.CLONE) {
			return user.getUsername().equals(board.getOwnerUsername());
		}
		return board.getFacility() != null
				&& user.getFacilityId().equals(board.getFacility().getId())
				&& board.getRoles().contains(user.getRole());
	}

	/****************************************************************************************
	 * Validation and conversion
	 ****************************************************************************************/

	static String validTitle(String title) {
		String trimmed = trimToNull(title);
		if (trimmed == null) {
			throw new InvalidRequestException("A title is required");
		}
		if (trimmed.length() > TankBoard.MAX_NAME_LENGTH) {
			throw new InvalidRequestException("A title can be at most " + TankBoard.MAX_NAME_LENGTH + " characters");
		}
		return trimmed;
	}

	static String trimToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	static Map<String, Object> copyOf(Map<String, Object> settings) {
		return settings == null ? new HashMap<>() : new HashMap<>(settings);
	}

	TankBoardSummaryVO toSummary(TankBoard board) {
		TankBoard source = board.getSource();
		return TankBoardSummaryVO.builder()
				.id(board.getId())
				.title(board.getName())
				.description(board.getDescription())
				.facilityId(board.getFacility() == null ? null : board.getFacility().getId())
				.kind(board.getKind())
				.sourceId(source == null ? null : source.getId())
				.sourceTitle(source == null ? null : source.getName())
				.build();
	}

	TankBoardDetailVO toDetail(TankBoard board) {
		TankBoard source = board.getSource();
		return TankBoardDetailVO.builder()
				.id(board.getId())
				.title(board.getName())
				.description(board.getDescription())
				.facilityId(board.getFacility() == null ? null : board.getFacility().getId())
				.kind(board.getKind())
				.sourceId(source == null ? null : source.getId())
				.sourceTitle(source == null ? null : source.getName())
				.settings(copyOf(board.getSettings()))
				.createdAt(board.getCreatedAt())
				.updatedAt(board.getUpdatedAt())
				.build();
	}
}

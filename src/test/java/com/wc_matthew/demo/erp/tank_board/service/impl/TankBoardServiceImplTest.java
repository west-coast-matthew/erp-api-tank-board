package com.wc_matthew.demo.erp.tank_board.service.impl;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.wc_matthew.demo.erp.core.entity.base.Facility;
import com.wc_matthew.demo.erp.core.exception.EntityNotFoundException;
import com.wc_matthew.demo.erp.core.exception.InvalidRequestException;
import com.wc_matthew.demo.erp.tank_board.constants.TankBoardKind;
import com.wc_matthew.demo.erp.tank_board.entity.TankBoard;
import com.wc_matthew.demo.erp.tank_board.repo.TankBoardRepo;
import com.wc_matthew.demo.erp.tank_board.vo.CloneTankBoardRequestVO;
import com.wc_matthew.demo.erp.tank_board.vo.TankBoardDetailVO;
import com.wc_matthew.demo.erp.tank_board.vo.TankBoardSummaryVO;
import com.wc_matthew.demo.erp.tank_board.vo.TankBoardUserVO;
import com.wc_matthew.demo.erp.tank_board.vo.UpdateTankBoardRequestVO;

public class TankBoardServiceImplTest {

	private static final TankBoardUserVO USER = TankBoardUserVO.of("teammember", "Team Member", 100L);

	@Mock
	private TankBoardRepo repo;

	@InjectMocks
	private TankBoardServiceImpl service;

	private Facility facility100;
	private TankBoard westTankFarm;
	private TankBoard myClone;

	@Before
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		facility100 = facility(100L);
		westTankFarm = shared(1L, "West Tank Farm", facility100, "team member", "admin");
		westTankFarm.getSettings().put("zoom", 2);
		myClone = clone(10L, "My West", westTankFarm, "teammember");

		when(repo.findById(any())).thenReturn(Optional.empty());
		when(repo.findById(1L)).thenReturn(Optional.of(westTankFarm));
		when(repo.findById(10L)).thenReturn(Optional.of(myClone));
		when(repo.save(any(TankBoard.class))).thenAnswer(inv -> {
			TankBoard saved = inv.getArgument(0);
			if (saved.getId() == null) saved.setId(99L);
			return saved;
		});
	}

	// ── user ──────────────────────────────────────────────────────────────────

	@Test
	public void user_normalizesRole() {
		assertEquals("team member", TankBoardUserVO.of(" teammember ", "  Team Member ", 100L).getRole());
		assertEquals("teammember", TankBoardUserVO.of(" teammember ", "Team Member", 100L).getUsername());
	}

	@Test(expected = InvalidRequestException.class)
	public void user_blankUsername_isRejected() {
		TankBoardUserVO.of(" ", "admin", 100L);
	}

	@Test(expected = InvalidRequestException.class)
	public void user_missingFacility_isRejected() {
		TankBoardUserVO.of("admin", "admin", null);
	}

	// ── lists ─────────────────────────────────────────────────────────────────

	@Test
	public void listTemplates_queriesSharedBoardsForFacilityAndRole() {
		when(repo.findActiveByFacilityAndRole(TankBoardKind.SHARED, 100L, "team member")).thenReturn(List.of(westTankFarm));

		List<TankBoardSummaryVO> templates = service.listTemplates(USER);

		assertEquals(1, templates.size());
		TankBoardSummaryVO vo = templates.get(0);
		assertEquals(Long.valueOf(1L), vo.getId());
		assertEquals("West Tank Farm", vo.getTitle());
		assertEquals(Long.valueOf(100L), vo.getFacilityId());
		assertEquals(TankBoardKind.SHARED, vo.getKind());
		assertNull(vo.getSourceId());
		assertNull(vo.getSourceTitle());
	}

	@Test
	public void listBoards_returnsUsersClonesWithSource() {
		when(repo.findActiveByOwner(TankBoardKind.CLONE, "teammember")).thenReturn(List.of(myClone));

		List<TankBoardSummaryVO> boards = service.listBoards(USER);

		assertEquals(1, boards.size());
		assertEquals(TankBoardKind.CLONE, boards.get(0).getKind());
		assertEquals(Long.valueOf(1L), boards.get(0).getSourceId());
		assertEquals("West Tank Farm", boards.get(0).getSourceTitle());
	}

	// ── get ───────────────────────────────────────────────────────────────────

	@Test
	public void getBoard_sharedBoardVisibleToRole_returnsDetailWithSettings() {
		TankBoardDetailVO detail = service.getBoard(USER, 1L);

		assertEquals("West Tank Farm", detail.getTitle());
		assertEquals(2, detail.getSettings().get("zoom"));
	}

	@Test
	public void getBoard_settingsAreACopy() {
		service.getBoard(USER, 1L).getSettings().put("zoom", 5);

		assertEquals(2, westTankFarm.getSettings().get("zoom"));
	}

	@Test(expected = EntityNotFoundException.class)
	public void getBoard_unknownId_isNotFound() {
		service.getBoard(USER, 404L);
	}

	@Test(expected = EntityNotFoundException.class)
	public void getBoard_roleNotListed_isNotFound() {
		service.getBoard(TankBoardUserVO.of("auditor", "Compliance", 100L), 1L);
	}

	@Test(expected = EntityNotFoundException.class)
	public void getBoard_otherFacility_isNotFound() {
		service.getBoard(TankBoardUserVO.of("teammember", "Team Member", 200L), 1L);
	}

	@Test(expected = EntityNotFoundException.class)
	public void getBoard_someoneElsesClone_isNotFound() {
		service.getBoard(TankBoardUserVO.of("planner", "Planner", 100L), 10L);
	}

	@Test(expected = EntityNotFoundException.class)
	public void getBoard_archivedClone_isNotFound() {
		myClone.setActive(false);
		service.getBoard(USER, 10L);
	}

	// ── clone ─────────────────────────────────────────────────────────────────

	@Test
	public void cloneBoard_savesOwnedCopyWithSettings() {
		TankBoardSummaryVO result = service.cloneBoard(USER, new CloneTankBoardRequestVO(1L, "  My copy ", "  For nights "));

		ArgumentCaptor<TankBoard> saved = ArgumentCaptor.forClass(TankBoard.class);
		verify(repo).save(saved.capture());
		TankBoard clone = saved.getValue();
		assertEquals(TankBoardKind.CLONE, clone.getKind());
		assertEquals("My copy", clone.getName());
		assertEquals("For nights", clone.getDescription());
		assertEquals("teammember", clone.getOwnerUsername());
		assertSame(westTankFarm, clone.getSource());
		assertSame(facility100, clone.getFacility());
		assertEquals(Map.of("zoom", 2), clone.getSettings());
		assertNotSame(westTankFarm.getSettings(), clone.getSettings());

		assertEquals(Long.valueOf(99L), result.getId());
		assertEquals(TankBoardKind.CLONE, result.getKind());
		assertEquals(Long.valueOf(1L), result.getSourceId());
		assertEquals("West Tank Farm", result.getSourceTitle());
	}

	@Test
	public void cloneBoard_blankDescription_isStoredAsNull() {
		service.cloneBoard(USER, new CloneTankBoardRequestVO(1L, "My copy", "   "));

		ArgumentCaptor<TankBoard> saved = ArgumentCaptor.forClass(TankBoard.class);
		verify(repo).save(saved.capture());
		assertNull(saved.getValue().getDescription());
	}

	@Test
	public void cloneBoard_blankTitle_isRejected() {
		assertThrows(InvalidRequestException.class,
				() -> service.cloneBoard(USER, new CloneTankBoardRequestVO(1L, "  ", null)));
		verify(repo, never()).save(any());
	}

	@Test
	public void cloneBoard_titleTooLong_isRejected() {
		String title = "x".repeat(TankBoard.MAX_NAME_LENGTH + 1);
		assertThrows(InvalidRequestException.class,
				() -> service.cloneBoard(USER, new CloneTankBoardRequestVO(1L, title, null)));
	}

	@Test(expected = InvalidRequestException.class)
	public void cloneBoard_missingSource_isRejected() {
		service.cloneBoard(USER, new CloneTankBoardRequestVO(null, "My copy", null));
	}

	@Test(expected = InvalidRequestException.class)
	public void cloneBoard_ofAClone_isRejected() {
		service.cloneBoard(USER, new CloneTankBoardRequestVO(10L, "Copy of copy", null));
	}

	@Test(expected = EntityNotFoundException.class)
	public void cloneBoard_sourceNotVisible_isNotFound() {
		service.cloneBoard(TankBoardUserVO.of("auditor", "Compliance", 100L), new CloneTankBoardRequestVO(1L, "Mine", null));
	}

	// ── update / settings / delete ────────────────────────────────────────────

	@Test
	public void updateBoard_changesTitleAndDescription() {
		TankBoardDetailVO detail = service.updateBoard(USER, 10L, new UpdateTankBoardRequestVO(" Renamed ", null));

		assertEquals("Renamed", myClone.getName());
		assertNull(myClone.getDescription());
		assertEquals("Renamed", detail.getTitle());
		verify(repo).save(myClone);
	}

	@Test(expected = InvalidRequestException.class)
	public void updateBoard_sharedBoard_isRejected() {
		service.updateBoard(USER, 1L, new UpdateTankBoardRequestVO("Renamed", null));
	}

	@Test
	public void saveSettings_replacesSettings() {
		Map<String, Object> settings = new HashMap<>();
		settings.put("hiddenTanks", List.of("T-101"));

		TankBoardDetailVO detail = service.saveSettings(USER, 10L, settings);

		assertEquals(settings, myClone.getSettings());
		assertFalse(myClone.getSettings().containsKey("zoom"));
		assertEquals(settings, detail.getSettings());
	}

	@Test
	public void saveSettings_null_clearsSettings() {
		service.saveSettings(USER, 10L, null);

		assertTrue(myClone.getSettings().isEmpty());
	}

	@Test(expected = InvalidRequestException.class)
	public void saveSettings_sharedBoard_isRejected() {
		service.saveSettings(USER, 1L, Map.of());
	}

	@Test
	public void deleteBoard_archivesClone() {
		service.deleteBoard(USER, 10L);

		assertFalse(myClone.isActive());
		verify(repo).save(myClone);
		verify(repo, never()).delete(any());
	}

	@Test
	public void deleteBoard_sharedBoard_isRejected() {
		assertThrows(InvalidRequestException.class, () -> service.deleteBoard(USER, 1L));
		assertTrue(westTankFarm.isActive());
	}

	@Test(expected = EntityNotFoundException.class)
	public void deleteBoard_someoneElsesClone_isNotFound() {
		service.deleteBoard(TankBoardUserVO.of("planner", "Planner", 100L), 10L);
	}

	// ── fixtures ──────────────────────────────────────────────────────────────

	static Facility facility(Long id) {
		Facility facility = new Facility();
		facility.setId(id);
		return facility;
	}

	static TankBoard shared(Long id, String name, Facility facility, String... roles) {
		TankBoard board = new TankBoard();
		board.setId(id);
		board.setKind(TankBoardKind.SHARED);
		board.setName(name);
		board.setFacility(facility);
		board.getRoles().addAll(Set.of(roles));
		return board;
	}

	static TankBoard clone(Long id, String name, TankBoard source, String owner) {
		TankBoard board = new TankBoard();
		board.setId(id);
		board.setKind(TankBoardKind.CLONE);
		board.setName(name);
		board.setFacility(source.getFacility());
		board.setSource(source);
		board.setOwnerUsername(owner);
		return board;
	}
}

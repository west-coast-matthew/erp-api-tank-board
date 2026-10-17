package com.wc_matthew.demo.erp.tank_board.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.wc_matthew.demo.erp.core.exception.EntityNotFoundException;
import com.wc_matthew.demo.erp.core.exception.InvalidRequestException;
import com.wc_matthew.demo.erp.core.service.ExceptionHandlerService;
import com.wc_matthew.demo.erp.tank_board.constants.TankBoardKind;
import com.wc_matthew.demo.erp.tank_board.middleware.TankBoardExceptionHandler;
import com.wc_matthew.demo.erp.tank_board.service.TankBoardService;
import com.wc_matthew.demo.erp.tank_board.vo.CloneTankBoardRequestVO;
import com.wc_matthew.demo.erp.tank_board.vo.TankBoardDetailVO;
import com.wc_matthew.demo.erp.tank_board.vo.TankBoardSummaryVO;
import com.wc_matthew.demo.erp.tank_board.vo.TankBoardUserVO;
import com.wc_matthew.demo.erp.tank_board.vo.UpdateTankBoardRequestVO;

public class TankBoardControllerTest {

	private static final TankBoardUserVO USER = TankBoardUserVO.of("teammember", "Team Member", 100L);

	private static final TankBoardSummaryVO SHARED = TankBoardSummaryVO.builder()
			.id(1L).title("West Tank Farm").facilityId(100L).kind(TankBoardKind.SHARED).build();

	private static final TankBoardSummaryVO CLONE = TankBoardSummaryVO.builder()
			.id(10L).title("My West").facilityId(100L).kind(TankBoardKind.CLONE).sourceId(1L).sourceTitle("West Tank Farm").build();

	private static final TankBoardDetailVO CLONE_DETAIL = TankBoardDetailVO.builder()
			.id(10L).title("My West").facilityId(100L).kind(TankBoardKind.CLONE).sourceId(1L)
			.settings(Map.of("zoom", 2)).build();

	@Mock
	private TankBoardService tankBoardService;

	@Mock
	private ExceptionHandlerService exceptionService;

	@InjectMocks
	private TankBoardController controller;

	@InjectMocks
	private TankBoardExceptionHandler exceptionHandler;

	private MockMvc mvc;

	@Before
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(exceptionHandler).build();
	}

	private static MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request) {
		return request
				.header("erp-username", "teammember")
				.header("erp-user-role", "Team Member")
				.header("erp-facility-id", "100");
	}

	@Test
	public void listTemplates_returnsSummariesWithUiKind() throws Exception {
		when(tankBoardService.listTemplates(USER)).thenReturn(List.of(SHARED));

		mvc.perform(asUser(get("/tank-board/templates")))
				.andExpect(status().isOk())
				.andExpect(header().string("transaction-id", "1234"))
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].title").value("West Tank Farm"))
				.andExpect(jsonPath("$[0].facilityId").value(100))
				.andExpect(jsonPath("$[0].kind").value("shared"));
	}

	@Test
	public void listBoards_returnsUsersClones() throws Exception {
		when(tankBoardService.listBoards(USER)).thenReturn(List.of(CLONE));

		mvc.perform(asUser(get("/tank-board/boards")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].kind").value("clone"))
				.andExpect(jsonPath("$[0].sourceTitle").value("West Tank Farm"));
	}

	@Test
	public void getBoard_returnsDetailWithSettings() throws Exception {
		when(tankBoardService.getBoard(USER, 10L)).thenReturn(CLONE_DETAIL);

		mvc.perform(asUser(get("/tank-board/boards/10")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.settings.zoom").value(2));
	}

	@Test
	public void getBoard_notFound_returns404WithErrorHeaders() throws Exception {
		when(tankBoardService.getBoard(USER, 404L)).thenThrow(new EntityNotFoundException("Unable to locate tank board 404"));

		mvc.perform(asUser(get("/tank-board/boards/404")))
				.andExpect(status().isNotFound())
				.andExpect(header().string("app-err-code", "ERP-1003"))
				.andExpect(header().string("default-message", "Entity not found"))
				.andExpect(header().string("message", "Unable to locate tank board 404"));
	}

	@Test
	public void missingUserHeaders_returns400() throws Exception {
		mvc.perform(get("/tank-board/templates"))
				.andExpect(status().isBadRequest())
				.andExpect(header().string("app-err-code", "ERP-1008"));
		verifyNoInteractions(tankBoardService);
	}

	@Test
	public void nonNumericFacility_returns400() throws Exception {
		mvc.perform(get("/tank-board/templates")
				.header("erp-username", "teammember")
				.header("erp-user-role", "Team Member")
				.header("erp-facility-id", "abc"))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void cloneBoard_returns201WithLocation() throws Exception {
		when(tankBoardService.cloneBoard(eq(USER), any())).thenReturn(CLONE);

		mvc.perform(asUser(post("/tank-board/boards"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"sourceId\":1,\"title\":\"My West\",\"description\":\"Nights\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", endsWith("/tank-board/boards/10")))
				.andExpect(jsonPath("$.id").value(10));

		ArgumentCaptor<CloneTankBoardRequestVO> request = ArgumentCaptor.forClass(CloneTankBoardRequestVO.class);
		verify(tankBoardService).cloneBoard(eq(USER), request.capture());
		assertEquals(new CloneTankBoardRequestVO(1L, "My West", "Nights"), request.getValue());
	}

	@Test
	public void cloneBoard_invalid_returns400() throws Exception {
		when(tankBoardService.cloneBoard(eq(USER), any())).thenThrow(new InvalidRequestException("A title is required"));

		mvc.perform(asUser(post("/tank-board/boards"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"sourceId\":1,\"title\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(header().string("message", "A title is required"));
	}

	@Test
	public void cloneBoard_malformedBody_returns400() throws Exception {
		mvc.perform(asUser(post("/tank-board/boards"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{not json"))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void updateBoard_passesTitleAndDescription() throws Exception {
		when(tankBoardService.updateBoard(eq(USER), eq(10L), any())).thenReturn(CLONE_DETAIL);

		mvc.perform(asUser(put("/tank-board/boards/10"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Renamed\",\"description\":\"x\"}"))
				.andExpect(status().isOk());

		verify(tankBoardService).updateBoard(USER, 10L, new UpdateTankBoardRequestVO("Renamed", "x"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void saveSettings_passesBodyAsSettings() throws Exception {
		when(tankBoardService.saveSettings(eq(USER), eq(10L), any())).thenReturn(CLONE_DETAIL);

		mvc.perform(asUser(put("/tank-board/boards/10/settings"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"zoom\":2,\"hiddenTanks\":[\"T-101\"]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(10));

		ArgumentCaptor<Map<String, Object>> settings = ArgumentCaptor.forClass(Map.class);
		verify(tankBoardService).saveSettings(eq(USER), eq(10L), settings.capture());
		assertEquals(2, settings.getValue().get("zoom"));
		assertEquals(List.of("T-101"), settings.getValue().get("hiddenTanks"));
	}

	@Test
	public void deleteBoard_returns204() throws Exception {
		mvc.perform(asUser(delete("/tank-board/boards/10")))
				.andExpect(status().isNoContent())
				.andExpect(header().string("transaction-id", "1234"));

		verify(tankBoardService).deleteBoard(USER, 10L);
	}

	@Test
	public void deleteBoard_sharedBoard_returns400() throws Exception {
		doThrow(new InvalidRequestException("Shared tank board 1 cannot be changed; clone it first"))
				.when(tankBoardService).deleteBoard(eq(USER), anyLong());

		mvc.perform(asUser(delete("/tank-board/boards/1")))
				.andExpect(status().isBadRequest());
	}

	private static void assertEquals(Object expected, Object actual) {
		org.junit.Assert.assertEquals(expected, actual);
	}
}

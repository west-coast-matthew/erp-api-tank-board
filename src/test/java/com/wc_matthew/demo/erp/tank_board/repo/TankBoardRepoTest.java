package com.wc_matthew.demo.erp.tank_board.repo;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.wc_matthew.demo.erp.core.constants.FacilityName;
import com.wc_matthew.demo.erp.core.entity.base.Facility;
import com.wc_matthew.demo.erp.core.repository.FacilityRepository;
import com.wc_matthew.demo.erp.tank_board.config.TankBoardTemplateSeeder;
import com.wc_matthew.demo.erp.tank_board.constants.TankBoardKind;
import com.wc_matthew.demo.erp.tank_board.entity.TankBoard;

/**
 * Runs the repository queries, the role collection and the JSON settings column against an
 * in-memory H2 database, so the persistence mapping is checked without MariaDB.
 *
 * Only the entities a tank board needs are scanned: the full erp-core-java entity set does not
 * currently build a persistence unit (its *CompEntry relations have no JPA annotation).
 */
@RunWith(SpringRunner.class)
@DataJpaTest(properties = { "spring.jpa.hibernate.ddl-auto=create-drop", "spring.datasource.url=jdbc:h2:mem:tankboard;MODE=MySQL" })
@ContextConfiguration(classes = TankBoardRepoTest.JpaConfig.class)
public class TankBoardRepoTest {

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EntityScan(basePackageClasses = { Facility.class, TankBoard.class })
	@EnableJpaRepositories(basePackageClasses = { FacilityRepository.class, TankBoardRepo.class },
			includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = { FacilityRepository.class, TankBoardRepo.class }))
	static class JpaConfig {
	}

	@Autowired
	private TestEntityManager em;

	@Autowired
	private TankBoardRepo repo;

	@Autowired
	private FacilityRepository facilityRepo;

	private Facility facilityA;
	private Facility facilityB;

	@Before
	public void setUp() {
		facilityA = em.persist(facility(FacilityName.FACILITY_A));
		facilityB = em.persist(facility(FacilityName.FACILITY_B));
	}

	@Test
	public void findActiveByFacilityAndRole_filtersByKindFacilityRoleAndActive() {
		TankBoard west = em.persist(shared("West Tank Farm", facilityA, "team member", "admin"));
		em.persist(shared("Packaging Location", facilityA, "admin"));
		em.persist(shared("Other Facility", facilityB, "team member"));
		TankBoard archived = shared("Archived", facilityA, "team member");
		archived.setActive(false);
		em.persist(archived);
		TankBoard east = em.persist(shared("East Tank Farm", facilityA, "team member"));
		em.persist(clone("My West", west, "teammember"));
		em.flush();
		em.clear();

		List<TankBoard> templates = repo.findActiveByFacilityAndRole(TankBoardKind.SHARED, facilityA.getId(), "team member");

		assertEquals(List.of(east.getId(), west.getId()), templates.stream().map(TankBoard::getId).toList());
	}

	@Test
	public void findActiveByOwner_returnsActiveClonesOfOwner() {
		TankBoard west = em.persist(shared("West Tank Farm", facilityA, "team member"));
		TankBoard mine = em.persist(clone("Mine", west, "teammember"));
		em.persist(clone("Theirs", west, "planner"));
		TankBoard deleted = clone("Deleted", west, "teammember");
		deleted.setActive(false);
		em.persist(deleted);
		em.flush();
		em.clear();

		List<TankBoard> boards = repo.findActiveByOwner(TankBoardKind.CLONE, "teammember");

		assertEquals(1, boards.size());
		assertEquals(mine.getId(), boards.get(0).getId());
		assertEquals("West Tank Farm", boards.get(0).getSource().getName());
	}

	@Test
	public void settingsAndRoles_roundTrip() {
		TankBoard west = shared("West Tank Farm", facilityA, "team member", "planner");
		west.getSettings().put("zoom", 2);
		west.getSettings().put("hiddenTanks", List.of("T-101", "T-102"));
		Long id = em.persistAndFlush(west).getId();
		em.clear();

		TankBoard loaded = repo.findById(id).orElseThrow();

		assertEquals(Map.of("zoom", 2, "hiddenTanks", List.of("T-101", "T-102")), loaded.getSettings());
		assertEquals(2, loaded.getRoles().size());
		assertTrue(loaded.getRoles().contains("planner"));
		assertNotNull(loaded.getCreatedAt());
	}

	@Test
	public void seeder_addsTemplatesOnlyToFacilitiesWithout() {
		em.persist(shared("Existing", facilityB, "admin"));
		em.flush();

		TankBoardTemplateSeeder seeder = new TankBoardTemplateSeeder(facilityRepo, repo);
		seeder.run(null);
		seeder.run(null);
		em.flush();
		em.clear();

		assertEquals(4, repo.countByKindAndFacility(TankBoardKind.SHARED, facilityA));
		assertEquals(1, repo.countByKindAndFacility(TankBoardKind.SHARED, facilityB));
		assertEquals(2, repo.findActiveByFacilityAndRole(TankBoardKind.SHARED, facilityA.getId(), "compliance").size());
	}

	private static Facility facility(FacilityName name) {
		Facility facility = new Facility();
		facility.setName(name);
		return facility;
	}

	private static TankBoard shared(String name, Facility facility, String... roles) {
		TankBoard board = new TankBoard();
		board.setKind(TankBoardKind.SHARED);
		board.setName(name);
		board.setFacility(facility);
		board.getRoles().addAll(List.of(roles));
		return board;
	}

	private static TankBoard clone(String name, TankBoard source, String owner) {
		TankBoard board = new TankBoard();
		board.setKind(TankBoardKind.CLONE);
		board.setName(name);
		board.setFacility(source.getFacility());
		board.setSource(source);
		board.setOwnerUsername(owner);
		return board;
	}
}

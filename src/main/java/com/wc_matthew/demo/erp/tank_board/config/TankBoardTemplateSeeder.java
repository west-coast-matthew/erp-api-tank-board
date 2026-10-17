package com.wc_matthew.demo.erp.tank_board.config;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import com.wc_matthew.demo.erp.core.entity.base.Facility;
import com.wc_matthew.demo.erp.core.repository.FacilityRepository;
import com.wc_matthew.demo.erp.tank_board.constants.TankBoardKind;
import com.wc_matthew.demo.erp.tank_board.entity.TankBoard;
import com.wc_matthew.demo.erp.tank_board.repo.TankBoardRepo;

/**
 * Gives every facility that has no shared tank boards the four demo templates the tank-board
 * UI's mock service returns (spec 003). Off with tank-board.seed-demo-templates=false.
 */
@Component
@ConditionalOnProperty(name = "tank-board.seed-demo-templates", havingValue = "true")
public class TankBoardTemplateSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(TankBoardTemplateSeeder.class);

	static final Set<String> ALL_ROLES = Set.of("admin", "team member", "planner", "compliance");
	static final Set<String> OPERATIONS_ROLES = Set.of("admin", "team member", "planner");

	record Template(String name, String description, Set<String> roles) {
	}

	static final List<Template> TEMPLATES = List.of(
			new Template("Bulk Material Intake Location", "Receiving docks and intake lines for bulk material.", OPERATIONS_ROLES),
			new Template("Packaging Location", "Packaging lines and finished goods staging.", OPERATIONS_ROLES),
			new Template("West Tank Farm", "Tanks and transfer lines on the west side of the facility.", ALL_ROLES),
			new Template("East Tank Farm", "Tanks and transfer lines on the east side of the facility.", ALL_ROLES));

	private final FacilityRepository facilityRepo;
	private final TankBoardRepo tankBoardRepo;

	public TankBoardTemplateSeeder(FacilityRepository facilityRepo, TankBoardRepo tankBoardRepo) {
		this.facilityRepo = facilityRepo;
		this.tankBoardRepo = tankBoardRepo;
	}

	@Override
	public void run(ApplicationArguments args) {
		try {
			for (Facility facility : facilityRepo.findAll()) {
				if (tankBoardRepo.countByKindAndFacility(TankBoardKind.SHARED, facility) == 0) {
					TEMPLATES.forEach(t -> tankBoardRepo.save(toBoard(t, facility)));
					log.info("Seeded {} demo tank board templates for facility {}", TEMPLATES.size(), facility.getId());
				}
			}
		} catch (DataAccessException ex) {
			// e.g. the tables do not exist yet (DDL_AUTO=none on a fresh database).
			log.warn("Skipped seeding demo tank board templates: {}", ex.getMessage());
		}
	}

	static TankBoard toBoard(Template template, Facility facility) {
		TankBoard board = new TankBoard();
		board.setKind(TankBoardKind.SHARED);
		board.setName(template.name());
		board.setDescription(template.description());
		board.setFacility(facility);
		board.getRoles().addAll(template.roles());
		return board;
	}
}

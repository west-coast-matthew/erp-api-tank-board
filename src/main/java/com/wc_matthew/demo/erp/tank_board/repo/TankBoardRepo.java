package com.wc_matthew.demo.erp.tank_board.repo;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.wc_matthew.demo.erp.core.entity.base.Facility;
import com.wc_matthew.demo.erp.tank_board.constants.TankBoardKind;
import com.wc_matthew.demo.erp.tank_board.entity.TankBoard;

public interface TankBoardRepo extends CrudRepository<TankBoard, Long> {

	/** Active boards of a kind at a facility that list the role (already lower-cased). */
	@Query("select distinct b from TankBoard b join b.roles r"
			+ " where b.kind = :kind and b.facility.id = :facilityId and r = :role and b.isActive = true"
			+ " order by b.name")
	List<TankBoard> findActiveByFacilityAndRole(@Param("kind") TankBoardKind kind,
			@Param("facilityId") Long facilityId, @Param("role") String role);

	@Query("select b from TankBoard b"
			+ " where b.kind = :kind and b.ownerUsername = :owner and b.isActive = true"
			+ " order by b.createdAt, b.id")
	List<TankBoard> findActiveByOwner(@Param("kind") TankBoardKind kind, @Param("owner") String owner);

	long countByKindAndFacility(TankBoardKind kind, Facility facility);

}

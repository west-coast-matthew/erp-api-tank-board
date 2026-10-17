package com.wc_matthew.demo.erp.tank_board.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.wc_matthew.demo.erp.core.entity.BaseDictionaryEntity;
import com.wc_matthew.demo.erp.core.entity.base.Facility;
import com.wc_matthew.demo.erp.core.exception.EntityValidationFailureException;
import com.wc_matthew.demo.erp.tank_board.constants.TankBoardKind;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * A tank board (facility map) for one facility: either a SHARED template, visible to the roles
 * in {@link #roles}, or a user's CLONE of one, owned by {@link #ownerUsername}.
 */
@Getter
@Setter
@Entity
@Table(name = "tank_board")
public class TankBoard extends BaseDictionaryEntity {

	public static final int MAX_NAME_LENGTH = 100;

	/****************************************************************************************
	 * Core attributes
	 ****************************************************************************************/
	@NotBlank(message = "Value must be present for attribute 'name'")
	@Size(max = MAX_NAME_LENGTH)
	@Column(name = "name", nullable = false, length = MAX_NAME_LENGTH)
	private String name;

	@NotNull(message = "Value must be present for attribute 'kind'")
	@Enumerated(EnumType.STRING)
	@Column(name = "kind", nullable = false, length = 20)
	private TankBoardKind kind;

	@NotNull(message = "Value must be present for attribute 'facility'")
	@ManyToOne
	@JoinColumn(name = "facility_id", nullable = false)
	private Facility facility;

	/** CLONE only: the user the copy belongs to. */
	@Column(name = "owner_username", length = 100)
	private String ownerUsername;

	/** CLONE only: the shared board this was copied from. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "source_id")
	private TankBoard source;

	/** SHARED only: lower-cased role names allowed to see the board. */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "tank_board_role", joinColumns = @JoinColumn(name = "tank_board_id"))
	@Column(name = "role", length = 50)
	private Set<String> roles = new HashSet<>();

	/** The user's customizations. Free-form until the customizations are defined. */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "settings")
	private Map<String, Object> settings = new HashMap<>();

	/****************************************************************************************
	 * Integrity checks
	 ****************************************************************************************/

	/**
	 * Runs on every insert/update so a shared board can never carry an owner and a clone can
	 * never lose its owner or source, whichever code path writes the row.
	 */
	@PrePersist
	@PreUpdate
	public void validateKind() {
		if (kind == TankBoardKind.SHARED && (ownerUsername != null || source != null)) {
			throw new EntityValidationFailureException("A shared tank board cannot have an owner or a source board");
		}
		if (kind == TankBoardKind.CLONE && (ownerUsername == null || source == null)) {
			throw new EntityValidationFailureException("A cloned tank board needs an owner and a source board");
		}
	}

	/****************************************************************************************
	 * Convenience methods
	 ****************************************************************************************/
	public String toString() {
		return "TankBoard: " + this.id;
	}
}

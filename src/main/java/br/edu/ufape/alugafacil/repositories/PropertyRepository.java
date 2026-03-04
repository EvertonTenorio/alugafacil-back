package br.edu.ufape.alugafacil.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.edu.ufape.alugafacil.enums.PropertyStatus;
import br.edu.ufape.alugafacil.models.Property;

@Repository
public interface PropertyRepository extends
		JpaRepository<Property, UUID>,
		QuerydslPredicateExecutor<Property> {
	List<Property> findByUserUserId(UUID userId);

	@Query("SELECT COUNT(p) FROM Property p WHERE p.user.userId = :userId AND p.status = :status")
	long countPropertiesByUser(@Param("userId") UUID userId, @Param("status") PropertyStatus status);

	@Modifying
	@Query("UPDATE Property p SET p.status = 'PAUSED' WHERE p.user.id = :userId AND p.status = 'ACTIVE'")
	void pauseActivePropertiesByUserId(@Param("userId") UUID userId);

	@Query("SELECT p FROM Property p WHERE p.user.agency.user.id = :adminId")
    List<Property> findByAgencyAdminId(@Param("adminId") UUID adminId);

	
	@Query("SELECT p FROM Property p WHERE p.status = :status ORDER BY p.createdAt DESC")
	List<Property> findRecentProperties(@Param("status") PropertyStatus status, Pageable pageable);
}

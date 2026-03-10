package br.edu.ufape.alugafacil.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
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


    long countByOwner_UserIdAndStatus(UUID ownerId, PropertyStatus status);	

    @Modifying
    @Query("UPDATE Property p SET p.status = 'PAUSED' WHERE p.owner.userId = :userId AND p.status = 'ACTIVE'")
    void pauseActivePropertiesByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE Property p SET p.assignedRealtor = null WHERE p.assignedRealtor.userId = :realtorId")
    void unassignRealtorProperties(@Param("realtorId") UUID realtorId);

    @Query("SELECT p FROM Property p WHERE p.status = :status ORDER BY p.createdAt DESC")
    List<Property> findRecentProperties(@Param("status") PropertyStatus status, Pageable pageable);
    
    List<Property> findByAgency_AgencyId(UUID agencyId);
    
    List<Property> findByAssignedRealtor_UserId(UUID realtorId);
    
    List<Property> findByOwner_UserId(UUID ownerId); 

    Page<Property> findByAssignedRealtor_UserId(UUID realtorId, Pageable pageable);
    
    Page<Property> findByAgency_AgencyId(UUID agencyId, Pageable pageable);
    
    Page<Property> findByOwner_UserId(UUID ownerId, Pageable pageable);
    
    Page<Property> findByAssignedRealtor_UserIdAndStatus(UUID userId, PropertyStatus status, Pageable pageable);
    
    Page<Property> findByAgency_AgencyIdAndStatus(UUID agencyId, PropertyStatus status, Pageable pageable);
    
    Page<Property> findByOwner_UserIdAndStatus(UUID ownerId, PropertyStatus status, Pageable pageable);

    @Query("SELECT p FROM Property p WHERE p.owner.agency.user.userId = :adminId")
    List<Property> findByAgencyAdminId(@Param("adminId") UUID adminId);

	@Query("SELECT p FROM Property p WHERE " +
       "p.agency.agencyId = :agencyId OR " +
       "p.owner.agency.agencyId = :agencyId OR " +
       "p.assignedRealtor.agency.agencyId = :agencyId")
	List<Property> findByAgencyIdOrOwnerAgencyId(@Param("agencyId") UUID agencyId);
}
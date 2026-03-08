package br.edu.ufape.alugafacil.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.edu.ufape.alugafacil.enums.PropertyStatus;
import br.edu.ufape.alugafacil.models.PropertyView;

@Repository
public interface PropertyViewRepository extends JpaRepository<PropertyView, UUID> {

    boolean existsByUserUserIdAndPropertyPropertyId(UUID userId, UUID propertyId);

    long countByPropertyPropertyId(UUID propertyId);

    @Query("SELECT pv.property.propertyId, COUNT(pv) FROM PropertyView pv " +
           "WHERE pv.property.status = :status GROUP BY pv.property.propertyId " +
           "ORDER BY COUNT(pv) DESC")
    List<Object[]> findTopPropertyIdsByViewCount(@Param("status") PropertyStatus status, Pageable pageable);

    @Query("SELECT pv.property.propertyId, COUNT(pv) FROM PropertyView pv " +
           "WHERE pv.property.propertyId IN :propertyIds GROUP BY pv.property.propertyId")
    List<Object[]> countByPropertyIdIn(@Param("propertyIds") List<UUID> propertyIds);
}

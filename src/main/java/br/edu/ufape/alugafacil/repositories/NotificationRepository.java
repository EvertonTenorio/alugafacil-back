package br.edu.ufape.alugafacil.repositories;

import br.edu.ufape.alugafacil.models.Notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("SELECT n FROM Notification n WHERE n.user.userId = :userId")
    Page<Notification> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
    
}
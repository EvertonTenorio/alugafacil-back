package br.edu.ufape.alugafacil.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.ufape.alugafacil.models.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u WHERE u.cpf =:cpf")
    Optional<User> findUserByCpf(@Param("cpf")String cpf);

    @Query("SELECT u FROM User u WHERE u.email =:email")
    Optional<User> findByEmail(@Param("email")String email);

    @Query("SELECT u FROM User u WHERE u.agency.agencyId =:agencyId")
    List<User> findByAgencyId(UUID agencyId);
    
    Page<User> findByAgency_AgencyId(UUID agencyId, Pageable pageable);
}

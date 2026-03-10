package br.edu.ufape.alugafacil.dtos.user;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import br.edu.ufape.alugafacil.dtos.realStateAgency.RealStateAgencyResponse;
import br.edu.ufape.alugafacil.enums.UserStatus;
import br.edu.ufape.alugafacil.enums.UserType;


public record UserResponse  (
        UUID userId,
        String name,
        String email,
        String photoUrl,
        String cpf,
        String creciNumber,
        String phoneNumber,
        UserType userType,
        @JsonIgnoreProperties({"user", "members"})
        RealStateAgencyResponse agency,
        UserStatus status,
        Integer propertiesCount
){}

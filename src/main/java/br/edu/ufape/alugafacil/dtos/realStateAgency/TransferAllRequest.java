package br.edu.ufape.alugafacil.dtos.realStateAgency;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record TransferAllRequest(
		@NotNull(message = "O ID do corretor de origem não pode ser nulo")
        UUID fromRealtorId,
        
        @NotNull(message = "O ID do corretor de destino não pode ser nulo")
        UUID toRealtorId) {

}

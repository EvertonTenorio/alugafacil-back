package br.edu.ufape.alugafacil.dtos.property;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import br.edu.ufape.alugafacil.dtos.address.AddressRequest;
import br.edu.ufape.alugafacil.dtos.geolocation.GeolocationRequest;
import br.edu.ufape.alugafacil.enums.PropertyStatus;
import br.edu.ufape.alugafacil.enums.PropertyType;

public record PropertyRequest(
    @NotBlank(message = "O título é obrigatório")
    String title,

    @NotBlank(message = "A descrição é obrigatória")
    String description,

    @NotNull(message = "O endereço é obrigatório")
    @Valid
    AddressRequest address,

    @NotNull(message = "A geolocalização é obrigatória")
    @Valid
    GeolocationRequest geolocation,

    @NotNull(message = "O preço é obrigatório")
    @Positive(message = "O preço deve ser positivo")
    Integer priceInCents,

    @Min(value = 0, message = "O aluguel semanal não pode ser negativo")
    Integer weeklyRentInCents,

    @Min(value = 0, message = "O valor da caução não pode ser negativo")
    Integer securityDepositInCents,

    @Min(value = 1, message = "O tempo mínimo deve ser de pelo menos 1 mês")
    Integer minimumLeaseMonths,

    @Min(value = 1, message = "O número máximo de ocupantes deve ser pelo menos 1")
    Integer maxOccupants,

    LocalDate availableFrom,

    @Min(value = 0, message = "Número de cômodos não pode ser negativo")
    Integer numberOfRooms,

    @Min(value = 0)
    Integer numberOfBedrooms,

    @Min(value = 0)
    Integer numberOfBathrooms,

    @NotNull
    Boolean furnished,

    @NotNull
    Boolean petFriendly,

    @NotNull
    Boolean garage,
    
    @NotNull
    Boolean isOwner,

    String videoUrl,

    @NotBlank(message = "Telefone para contato é obrigatório")
    String phoneNumber,

    List<String> photoUrls,

    List<String> amenities,

    List<String> houseRules,

    PropertyStatus status,

    @NotNull(message = "O tipo do imóvel é obrigatório")
    PropertyType type,

    @NotNull(message = "O ID do proprietário é obrigatório")
    UUID userId,

    UUID agencyId 
) {}
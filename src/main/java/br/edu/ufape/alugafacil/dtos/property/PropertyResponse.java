package br.edu.ufape.alugafacil.dtos.property;

import java.time.LocalDate; 
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import br.edu.ufape.alugafacil.dtos.address.AddressResponse;
import br.edu.ufape.alugafacil.dtos.geolocalionResponse.GeolocationResponse;
import br.edu.ufape.alugafacil.enums.PropertyStatus;
import br.edu.ufape.alugafacil.enums.PropertyType;

public record PropertyResponse(
    UUID propertyId,
    String title,
    String description,
    AddressResponse address,
    GeolocationResponse geolocation,
    Integer priceInCents,
    Integer weeklyRentInCents,
    Integer securityDepositInCents,
    Integer minimumLeaseMonths,
    Integer maxOccupants,
    LocalDate availableFrom,
    Integer numberOfRooms,
    Integer numberOfBedrooms,
    Integer numberOfBathrooms,
    Boolean furnished,
    Boolean petFriendly,
    Boolean garage,
    Boolean isOwner,
    String videoUrl,
    String phoneNumber,
    List<String> photoUrls,
    List<String> amenities,
    List<String> houseRules,
    PropertyStatus status,
    PropertyType type,
    UUID ownerId,
    LocalDateTime createdAt,
    Long viewCount,
    UUID agencyId,
    UUID assignedRealtorId
) {}
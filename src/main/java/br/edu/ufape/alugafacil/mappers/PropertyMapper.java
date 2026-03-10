package br.edu.ufape.alugafacil.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import br.edu.ufape.alugafacil.dtos.property.PropertyRequest;
import br.edu.ufape.alugafacil.dtos.property.PropertyResponse;
import br.edu.ufape.alugafacil.models.Property;

@Mapper(componentModel = "spring")
public interface PropertyMapper {

    // --- Request (DTO) -> Entity ---
    // Ignoramos as entidades complexas aqui para tratá-las manualmente no Service (buscando no DB)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "agency", ignore = true)
    @Mapping(target = "assignedRealtor", ignore = true)
    @Mapping(target = "propertyId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "moderationReason", ignore = true)
    Property toEntity(PropertyRequest request);

    // --- Entity -> Response (DTO) ---

    @Mapping(source = "property.owner.userId", target = "ownerId")
    @Mapping(source = "property.agency.agencyId", target = "agencyId") 
    @Mapping(source = "property.assignedRealtor.userId", target = "assignedRealtorId") 
    @Mapping(target = "viewCount", source = "viewCount")
    PropertyResponse toResponse(Property property, Long viewCount);
    
    // --- Update ---
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "agency", ignore = true)
    @Mapping(target = "assignedRealtor", ignore = true)
    @Mapping(target = "propertyId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(PropertyRequest request, @MappingTarget Property entity);
}
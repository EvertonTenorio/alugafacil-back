package br.edu.ufape.alugafacil.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import br.edu.ufape.alugafacil.dtos.property.PropertyRequest;
import br.edu.ufape.alugafacil.dtos.property.PropertyResponse;
import br.edu.ufape.alugafacil.models.Property;

// componentModel = "spring" permite injetar com @Autowired no Service
@Mapper(componentModel = "spring")
public interface PropertyMapper {

    // --- Request (DTO) -> Entity ---
    
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "agency", ignore = true)
    @Mapping(target = "assignedRealtor", ignore = true)
    @Mapping(target = "propertyId", ignore = true)
    Property toEntity(PropertyRequest request);


    @Mapping(source = "property.owner.userId", target = "ownerId")
    @Mapping(target = "viewCount", source = "viewCount")
    PropertyResponse toResponse(Property property, Long viewCount);
    
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "agency", ignore = true)
    @Mapping(target = "assignedRealtor", ignore = true)
    @Mapping(target = "propertyId", ignore = true)
    void updateEntityFromDto(PropertyRequest request, @MappingTarget Property entity);
}
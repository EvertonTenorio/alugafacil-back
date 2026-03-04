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
    
    // ignora 'user' aqui porque vamos setar manualmente no service após buscar no banco
    // ignora 'propertyId' pois é gerado automaticamente
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "propertyId", ignore = true)
    // O MapStruct converte AddressRequest -> Address e GeolocationRequest -> Geolocation automaticamente
    Property toEntity(PropertyRequest request);

    // --- Entity -> Response (DTO) ---

    // mapeia o ID do user da entidade para o campo userId do record; viewCount passado por parâmetro
    @Mapping(source = "property.user.userId", target = "ownerId")
    @Mapping(target = "viewCount", source = "viewCount")
    PropertyResponse toResponse(Property property, Long viewCount);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "propertyId", ignore = true)
    void updateEntityFromDto(PropertyRequest request, @MappingTarget Property entity);
}
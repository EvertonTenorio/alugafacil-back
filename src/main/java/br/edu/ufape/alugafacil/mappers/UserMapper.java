package br.edu.ufape.alugafacil.mappers;

import br.edu.ufape.alugafacil.dtos.user.UserRequest;
import br.edu.ufape.alugafacil.dtos.user.UserResponse;
import br.edu.ufape.alugafacil.enums.UserType;
import br.edu.ufape.alugafacil.models.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {RealStateAgencyMapper.class})
public interface UserMapper {

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "agency", ignore = true)
    @Mapping(target = "properties", ignore = true)
    @Mapping(target = "subscriptions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(UserRequest request);

    // Quebra o loop infinito dos membros da agência e traz a contagem de imóveis
    @Mapping(target = "agency.members", ignore = true)
    @Mapping(target = "propertiesCount", expression = "java(countProperties(user))")
    UserResponse toResponse(User user);
    
    default Integer countProperties(User user) {
        if (user == null) return 0;
        
        if (user.getUserType() == UserType.REALTOR) {
            return user.getAssignedProperties() != null ? user.getAssignedProperties().size() : 0;
        }
        
        return user.getProperties() != null ? user.getProperties().size() : 0;
    }

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "agency", ignore = true)
    @Mapping(target = "properties", ignore = true)
    @Mapping(target = "subscriptions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(UserRequest request, @MappingTarget User user);
}
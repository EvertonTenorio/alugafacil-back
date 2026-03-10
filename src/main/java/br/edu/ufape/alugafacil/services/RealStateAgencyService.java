package br.edu.ufape.alugafacil.services;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufape.alugafacil.controllers.UserController;
import br.edu.ufape.alugafacil.dtos.realStateAgency.MemberResponse;
import br.edu.ufape.alugafacil.dtos.realStateAgency.RealStateAgencyRequest;
import br.edu.ufape.alugafacil.dtos.realStateAgency.RealStateAgencyResponse;
import br.edu.ufape.alugafacil.dtos.user.RealtorRegistrationRequest;
import br.edu.ufape.alugafacil.dtos.user.UserResponse;
import br.edu.ufape.alugafacil.enums.UserType;
import br.edu.ufape.alugafacil.mappers.RealStateAgencyMapper;
import br.edu.ufape.alugafacil.mappers.UserMapper;
import br.edu.ufape.alugafacil.models.Address;
import br.edu.ufape.alugafacil.models.Property;
import br.edu.ufape.alugafacil.models.RealStateAgency;
import br.edu.ufape.alugafacil.models.User;
import br.edu.ufape.alugafacil.repositories.PropertyRepository;
import br.edu.ufape.alugafacil.repositories.RealStateAgencyRepository;
import br.edu.ufape.alugafacil.repositories.UserRepository;
import br.edu.ufape.alugafacil.services.interfaces.IKeycloakService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RealStateAgencyService {

    private final UserController userController;
    
    private final RealStateAgencyRepository realStateAgencyRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final RealStateAgencyMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final IKeycloakService keycloakService;
    private final UserMapper userMapper;


    @Transactional(readOnly = true)
    public Page<RealStateAgencyResponse> getAllRealStateAgencies(Pageable pageable) {
        return realStateAgencyRepository.findAll(pageable)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RealStateAgencyResponse getRealStateAgencyById(UUID agencyId) {
        RealStateAgency agency = getAgencyOrThrow(agencyId);
        return mapper.toResponse(agency);
    }

    @Transactional
    public RealStateAgencyResponse registerAgencyWithAdmin(RealStateAgencyRequest request) {
    	
    	User adminUser = new User();
    	adminUser.setName(request.adminName());
    	adminUser.setEmail(request.adminEmail());
    	adminUser.setPassword(passwordEncoder.encode(request.adminPassword()));
    	adminUser.setUserType(UserType.AGENCY_ADMIN);
    	
    	keycloakService.createUser(
                request.adminName(), 
                request.adminEmail(), 
                request.adminPassword(), 
                UserType.AGENCY_ADMIN
        );
    	
    	adminUser = userRepository.save(adminUser);
    	
    	Address address = new Address();
        address.setPostalCode(request.address().postalCode());
        address.setStreet(request.address().street());
        address.setNumber(request.address().number());
        address.setComplement(request.address().complement());
        address.setNeighborhood(request.address().neighborhood());
        address.setCity(request.address().city());
        address.setState(request.address().state());

        RealStateAgency newAgency = mapper.toEntity(request);
        
        newAgency.setUser(adminUser);
        newAgency.setAddress(address);
        
        newAgency = realStateAgencyRepository.save(newAgency);
        
        adminUser.setAgency(newAgency);
        userRepository.save(adminUser);
        
        return mapper.toResponse(newAgency);
    }
    
    @Transactional
    public MemberResponse registerRealtor(RealtorRegistrationRequest request, UUID loggerUserId) {
    	User loggedUser = getUserOrThrow(loggerUserId);
    	
    	RealStateAgency targetAgency = loggedUser.getAgency();
    	
    	if (targetAgency == null) {
    		throw new IllegalArgumentException("O usuário logado não possui uma imobiliária vinculada.");
    	}
    	
    	if (loggedUser.getUserType() != UserType.AGENCY_ADMIN) {
            throw new IllegalArgumentException("Apenas o administrador da imobiliária pode cadastrar novos corretores.");
        }
    	
    	User realtor = new User();
        realtor.setName(request.name());
        realtor.setEmail(request.email());
        realtor.setCpf(request.cpf());
        realtor.setCreciNumber(request.creciNumber());
        realtor.setPhoneNumber(request.phoneNumber());
        realtor.setPassword(passwordEncoder.encode(request.password()));
        
        keycloakService.createUser(
        		realtor.getName(), 
                realtor.getEmail(), 
                request.password(),
                UserType.REALTOR
        );
    	
        realtor.setUserType(UserType.REALTOR); 
        realtor.setAgency(targetAgency);

        realtor = userRepository.save(realtor);

        return mapper.toMemberResponse(realtor);
    }

    @Transactional
    public RealStateAgencyResponse updateRealStateAgency(UUID agencyId, RealStateAgencyRequest request) {
        RealStateAgency existingAgency = getAgencyOrThrow(agencyId);
        
        mapper.updateEntityFromRequest(request, existingAgency);
        
        RealStateAgency updatedAgency = realStateAgencyRepository.save(existingAgency);
        return mapper.toResponse(updatedAgency);
    }

    @Transactional
    public void deleteRealStateAgency(UUID agencyId) {
        if (!realStateAgencyRepository.existsById(agencyId)) {
            throw new IllegalArgumentException("Agência não encontrada.");
        }
        realStateAgencyRepository.deleteById(agencyId);
    }

    @Transactional
    public void addMember(UUID agencyId, UUID userId, UUID actingUserId) {
        RealStateAgency agency = getAgencyOrThrow(agencyId);
        User user = getUserOrThrow(userId);
        
        validateAgencyAdminOrSystemAdmin(agency, actingUserId);

        if (user.getUserType() != UserType.REALTOR) {
            throw new IllegalArgumentException("Apenas usuários do tipo CORRETOR (REALTOR) podem ser adicionados como membros.");
        }

        user.setAgency(agency);
        userRepository.save(user);
    }

    @Transactional
    public void removeMember(UUID agencyId, UUID userId, UUID actingUserId) {
        RealStateAgency agency = getAgencyOrThrow(agencyId);
        User user = getUserOrThrow(userId);

        validateAgencyAdminOrSystemAdmin(agency, actingUserId);

        if (user.getAgency() == null || !agency.getAgencyId().equals(user.getAgency().getAgencyId())) {
            throw new IllegalArgumentException("Usuário não é um membro desta agência.");
        }

        if (agency.getUser() != null && agency.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("Não é possível remover o proprietário da agência.");
        }

        propertyRepository.unassignRealtorProperties(user.getUserId());

        user.setAgency(null);
        userRepository.save(user);
    }

    @Transactional
    public void transferProperty(UUID agencyId, UUID propertyId, UUID targetUserId, UUID actingUserId) {
        RealStateAgency agency = getAgencyOrThrow(agencyId);
        User targetUser = getUserOrThrow(targetUserId);
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Imóvel não encontrado."));

        validateAgencyAdminOrSystemAdmin(agency, actingUserId);

        if (targetUser.getAgency() == null || !agency.getAgencyId().equals(targetUser.getAgency().getAgencyId())) {
            throw new IllegalArgumentException("O usuário destino deve ser membro desta agência.");
        }

        if (property.getAgency() == null || !agency.getAgencyId().equals(property.getAgency().getAgencyId())) {
            throw new IllegalArgumentException("Este imóvel não pertence à sua agência.");
        }

        property.setAssignedRealtor(targetUser);
        propertyRepository.save(property);
    }
    
    @Transactional
    public void transferAllProperties(UUID agencyId, UUID fromRealtorId, UUID toRealtorId, UUID actingUserId) {
        RealStateAgency agency = getAgencyOrThrow(agencyId);
        validateAgencyAdminOrSystemAdmin(agency, actingUserId);

        User toRealtor = getUserOrThrow(toRealtorId);

        if (toRealtor.getAgency() == null || !agency.getAgencyId().equals(toRealtor.getAgency().getAgencyId())) {
            throw new IllegalArgumentException("O corretor destino deve ser membro desta agência.");
        }

        List<Property> propertiesToTransfer = propertyRepository.findByAssignedRealtor_UserId(fromRealtorId);

        for (Property property : propertiesToTransfer) {
            // Medida de segurança extra para garantir que o imóvel é da agência
            if (property.getAgency() != null && property.getAgency().getAgencyId().equals(agencyId)) {
                property.setAssignedRealtor(toRealtor);
            }
        }
        
        propertyRepository.saveAll(propertiesToTransfer);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAgencyMembers(UUID agencyId, Pageable pageable) {
        getAgencyOrThrow(agencyId);

        return userRepository.findByAgency_AgencyId(agencyId, pageable)
                .map(userMapper::toResponse);
    }

    // =========================================================================
    // MÉTODOS PRIVADOS AUXILIARES
    // =========================================================================

    private RealStateAgency getAgencyOrThrow(UUID agencyId) {
        return realStateAgencyRepository.findById(agencyId)
                .orElseThrow(() -> new IllegalArgumentException("Agência não encontrada com o ID fornecido."));
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
    }

    private void validateAgencyAdminOrSystemAdmin(RealStateAgency agency, UUID actingUserId) {
        User actingUser = getUserOrThrow(actingUserId);
        
        boolean isOwner = agency.getUser() != null && agency.getUser().getUserId().equals(actingUserId);
        boolean isAdmin = actingUser.getUserType() == UserType.ADMIN;
        
        if (!isOwner && !isAdmin) {
            throw new IllegalArgumentException("Usuário não autorizado para realizar esta ação na agência.");
        }
    }
}
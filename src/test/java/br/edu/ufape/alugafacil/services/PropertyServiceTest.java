package br.edu.ufape.alugafacil.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import br.edu.ufape.alugafacil.dtos.property.PropertyResponse;
import br.edu.ufape.alugafacil.mappers.PropertyMapper;
import br.edu.ufape.alugafacil.models.Property;
import br.edu.ufape.alugafacil.repositories.PropertyRepository;
import br.edu.ufape.alugafacil.repositories.PropertyViewRepository;
import br.edu.ufape.alugafacil.repositories.SubscriptionRepository;
import br.edu.ufape.alugafacil.repositories.UserRepository;
import br.edu.ufape.alugafacil.repositories.UserSearchPreferenceRepository;
import br.edu.ufape.alugafacil.services.interfaces.IFileStorageService;
import br.edu.ufape.alugafacil.dtos.property.PropertyFilterRequest;
import br.edu.ufape.alugafacil.dtos.property.PropertyRequest;
import br.edu.ufape.alugafacil.enums.PropertyStatus;
import br.edu.ufape.alugafacil.enums.PaymentStatus;
import br.edu.ufape.alugafacil.models.Plan;
import br.edu.ufape.alugafacil.models.Subscription;
import br.edu.ufape.alugafacil.models.User;
import br.edu.ufape.alugafacil.enums.PropertyType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.querydsl.core.types.Predicate;

import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    @InjectMocks
    private PropertyService propertyService;
    
    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private PropertyViewRepository propertyViewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PropertyMapper propertyMapper;

    @Mock
    private IFileStorageService fileStorageService;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserSearchPreferenceRepository preferenceRepository;

    @Mock
    private NotificationService notificationService;

    @Test
    @DisplayName("Deve retornar um PropertyResponse quando o ID existir")
    void shouldReturnPropertyWhenIdExists() {
        UUID id = UUID.randomUUID();
        
        Property propertyEntity = new Property(); 
        propertyEntity.setPropertyId(id); 
        propertyEntity.setTitle("Casa Teste"); 

        PropertyResponse responseEsperado = criarResponseFake(id);

        when(propertyRepository.findById(id)).thenReturn(Optional.of(propertyEntity));
        when(propertyViewRepository.countByPropertyPropertyId(id)).thenReturn(0L);
        when(propertyMapper.toResponse(propertyEntity, 0L)).thenReturn(responseEsperado);

        PropertyResponse resultado = propertyService.getPropertyById(id);

        assertNotNull(resultado);        
        assertEquals(id, resultado.propertyId());        
        verify(propertyRepository).findById(id);
    }

    @Test
    @DisplayName("Deve lançar exceção quando o ID não existir")
    void shouldThrowExceptionWhenIdDoesNotExist() {
        UUID idInexistente = UUID.randomUUID();

        when(propertyRepository.findById(idInexistente)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            propertyService.getPropertyById(idInexistente);
        });

        assertEquals("Imóvel não encontrado", exception.getMessage());
        verify(propertyMapper, never()).toResponse(any(Property.class), anyLong());
    }

    @Test
    @DisplayName("Deve criar imóvel com sucesso quando dentro do limite do plano")
    void shouldCreatePropertySuccessfully() {
        UUID userId = UUID.randomUUID();
        User owner = new User();
        owner.setUserId(userId);

        Plan plan = new Plan();
        plan.setPropertiesCount(5); 
        plan.setHasVideo(true);    
        plan.setIsPriority(true);   

        Subscription subscription = new Subscription();
        subscription.setPlan(plan);

        
        PropertyRequest request = new PropertyRequest(
            "Casa Nova", "Descrição Top", null, null, 100000, 
            null, null, null, null, null, // weeklyRentInCents, securityDepositInCents, minimumLeaseMonths, maxOccupants, availableFrom
            2, 1, 1, true, true, true, true, 
            "http://video.com", "879999999", null, null, null, PropertyStatus.ACTIVE, PropertyType.HOUSE, userId          
        );

        Property propertyEntity = new Property();
        propertyEntity.setOwner(owner);
        
        PropertyResponse responseEsperado = criarResponseFake(UUID.randomUUID());

        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(subscriptionRepository.findFirstByUserUserIdAndStatus(userId, PaymentStatus.ACTIVE))
            .thenReturn(Optional.of(subscription));
            
        when(propertyRepository.countByOwner_UserIdAndStatus(userId, PropertyStatus.ACTIVE)).thenReturn(0L);
        when(propertyMapper.toEntity(request)).thenReturn(propertyEntity);
        when(propertyRepository.save(propertyEntity)).thenReturn(propertyEntity);
        when(propertyMapper.toResponse(propertyEntity, 0L)).thenReturn(responseEsperado);
        
        lenient().when(preferenceRepository.findMatchingPreferences(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(Collections.emptyList());

        PropertyResponse resultado = propertyService.createProperty(request);

        assertNotNull(resultado);
        assertEquals(true, propertyEntity.getIsPriority()); 

        verify(propertyRepository).save(propertyEntity);
    }

    @Test
    @DisplayName("Deve lançar exceção quando atingir o limite de imóveis do plano")
    void shouldThrowExceptionWhenPropertyLimitExceeded() {
        UUID userId = UUID.randomUUID();
        User owner = new User();
        owner.setUserId(userId);

        Plan plan = new Plan();
        plan.setPropertiesCount(2); 
        
        Subscription subscription = new Subscription();
        subscription.setPlan(plan);

        
        PropertyRequest request = new PropertyRequest(
            "Casa", "Desc", null, null, 100, 
            null, null, null, null, null, 
            1, 1, 1, false, false, false, true, 
            null, "999", null, null, null, PropertyStatus.ACTIVE, PropertyType.HOUSE, userId
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(subscriptionRepository.findFirstByUserUserIdAndStatus(userId, PaymentStatus.ACTIVE))
            .thenReturn(Optional.of(subscription));
        when(propertyRepository.countByOwner_UserIdAndStatus(userId, PropertyStatus.ACTIVE)).thenReturn(2L);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            propertyService.createProperty(request);
        });

        String mensagemEsperada = "Upgrade necessário! Seu plano permite apenas " + plan.getPropertiesCount() + " imóveis.";
        assertEquals(mensagemEsperada, exception.getMessage());

        verify(propertyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar adicionar vídeo sem permissão no plano")
    void shouldThrowExceptionWhenVideoNotAllowed() {
        UUID userId = UUID.randomUUID();
        User owner = new User();
        owner.setUserId(userId);

        Plan plan = new Plan();
        plan.setPropertiesCount(5); 
        plan.setHasVideo(false);
        
        Subscription subscription = new Subscription();
        subscription.setPlan(plan);

        
        PropertyRequest request = new PropertyRequest(
            "Casa", "Desc", null, null, 100, 
            null, null, null, null, null, 
            1, 1, 1, false, false, false, true, 
            "http://youtube.com/meuvideo", "999", null, null, null, PropertyStatus.ACTIVE, PropertyType.HOUSE, userId
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(subscriptionRepository.findFirstByUserUserIdAndStatus(userId, PaymentStatus.ACTIVE))
            .thenReturn(Optional.of(subscription));
        
        when(propertyRepository.countByOwner_UserIdAndStatus(userId, PropertyStatus.ACTIVE)).thenReturn(0L);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            propertyService.createProperty(request);
        });

        assertEquals("Seu plano atual não permite adicionar vídeos ao anúncio.", exception.getMessage());
        
        verify(propertyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve retornar página de imóveis filtrados")
    void shouldReturnAllPropertiesPaged() {
        Pageable pageable = PageRequest.of(0, 10);

        PropertyFilterRequest filters = new PropertyFilterRequest(); 
        Property property = new Property();

        property.setPropertyId(UUID.randomUUID());

        List<Property> propertyList = List.of(property);
        Page<Property> pageResult = new PageImpl<>(propertyList);
        
        PropertyResponse responseMock = criarResponseFake(property.getPropertyId());

        when(propertyRepository.findAll(any(Predicate.class), eq(pageable))).thenReturn(pageResult);
        when(propertyViewRepository.countByPropertyIdIn(any())).thenReturn(Collections.emptyList());
        when(propertyMapper.toResponse(property, 0L)).thenReturn(responseMock);

        Page<PropertyResponse> result = propertyService.getAllProperties(filters, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        
        verify(propertyRepository).findAll(any(Predicate.class), eq(pageable));
    }

    private PropertyResponse criarResponseFake(UUID id) {
        PropertyResponse resp = mock(PropertyResponse.class);
        
        lenient().when(resp.propertyId()).thenReturn(id);
        
        return resp;
    }
}
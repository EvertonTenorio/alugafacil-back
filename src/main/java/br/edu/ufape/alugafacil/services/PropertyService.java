package br.edu.ufape.alugafacil.services;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;

import br.edu.ufape.alugafacil.dtos.property.PropertyFilterRequest;
import br.edu.ufape.alugafacil.dtos.property.PropertyRequest;
import br.edu.ufape.alugafacil.dtos.property.PropertyResponse;

import br.edu.ufape.alugafacil.enums.PaymentStatus;
import br.edu.ufape.alugafacil.enums.PropertyStatus;
import br.edu.ufape.alugafacil.dtos.property.PropertyStatusDTO;

import br.edu.ufape.alugafacil.mappers.PropertyMapper;
import br.edu.ufape.alugafacil.models.Plan;
import br.edu.ufape.alugafacil.models.Property;
import br.edu.ufape.alugafacil.models.QProperty;
import br.edu.ufape.alugafacil.models.User;
import br.edu.ufape.alugafacil.models.UserSearchPreference;
import br.edu.ufape.alugafacil.repositories.PropertyRepository;
import br.edu.ufape.alugafacil.repositories.SubscriptionRepository;
import br.edu.ufape.alugafacil.repositories.UserRepository;
import br.edu.ufape.alugafacil.repositories.UserSearchPreferenceRepository;
import br.edu.ufape.alugafacil.services.interfaces.IFileStorageService;
import br.edu.ufape.alugafacil.services.interfaces.INotificationService;
import br.edu.ufape.alugafacil.services.interfaces.IPropertyService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyService implements IPropertyService {
    
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final PropertyMapper propertyMapper;
    private final IFileStorageService fileStorageService;
    private final UserSearchPreferenceRepository preferenceRepository;
    private final INotificationService notificationService;
    private final SubscriptionRepository subscriptionRepository;
    
    private Plan getUserActivePlan(User user) {
        return subscriptionRepository.findFirstByUserUserIdAndStatus(user.getUserId(), PaymentStatus.ACTIVE)
                .map(subscription -> subscription.getPlan())
                .orElseGet(() -> {
                    Plan freePlan = new Plan();
                    freePlan.setPropertiesCount(1);
                    freePlan.setImagesCount(5);
                    freePlan.setHasVideo(false);
                    freePlan.setIsPriority(false);
                    return freePlan;
                });
    }
    
    @Override
    @Transactional
    public PropertyResponse createProperty(PropertyRequest request) {
        User owner = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + request.userId()));
        
        // Plan plan = getUserActivePlan(owner);
        // long currentProperties = propertyRepository.countPropertiesByUser(owner.getUserId(), PropertyStatus.ACTIVE);
        
        // if (currentProperties >= plan.getPropertiesCount()) {
        //     throw new RuntimeException("Upgrade necessário! Seu plano permite apenas " + plan.getPropertiesCount() + " imóveis.");
        // }
        
        // if (request.videoUrl() != null && !request.videoUrl().isEmpty() && !plan.getHasVideo()) {
        //     throw new RuntimeException("Seu plano atual não permite adicionar vídeos ao anúncio.");
        // }
        
        Property property = propertyMapper.toEntity(request);
        property.setUser(owner);
        // property.setIsPriority(plan.getIsPriority()); 
        if (request.status() != null) {
            property.setStatus(request.status());
        } else {
            property.setStatus(PropertyStatus.ACTIVE);
        }
        
        Property savedProperty = propertyRepository.save(property);
        
        
        notifyInterestedUsers(savedProperty);
       
        return propertyMapper.toResponse(savedProperty);
    }

    @Override
    public List<PropertyResponse> getPropertiesByAgencyAdminId(UUID adminId) {
        List<Property> properties = propertyRepository.findByAgencyAdminId(adminId);
        
        
        return properties.stream()
                .map(propertyMapper::toResponse) 
                .toList();
    }

    /**
     * Busca usuários interessados e dispara notificação.
     * @Async garante que não trave a resposta da API.
     */
    @Async 
protected void notifyInterestedUsers(Property property) {
    
    // 1. Extração segura dos dados da entidade Property
    Integer garageCount = (property.getGarage() != null && property.getGarage()) ? 1 : 0;
    Double lat = (property.getGeolocation() != null) ? property.getGeolocation().getLatitude() : null;
    Double lon = (property.getGeolocation() != null) ? property.getGeolocation().getLongitude() : null;
    String city = (property.getAddress() != null) ? property.getAddress().getCity() : null;
    String neighborhood = (property.getAddress() != null) ? property.getAddress().getNeighborhood() : null;
    String state = (property.getAddress() != null) ? property.getAddress().getState() : null;

    // 2. Busca usando os dados extraídos do objeto property (e não do request)
    List<UserSearchPreference> matches = preferenceRepository.findMatchingPreferences(
        property.getPriceInCents(),
        property.getNumberOfBedrooms(),
        property.getNumberOfBathrooms(),
        garageCount, 
        property.getFurnished(),
        property.getPetFriendly(),
        city,
        neighborhood,
        state,
        lat,
        lon
    );

    // 3. Envio das notificações
    for (UserSearchPreference preference : matches) {
        // Evita notificar o próprio dono do imóvel
        if (property.getUser() != null && preference.getUser().getUserId().equals(property.getUser().getUserId())) {
            continue;
        }

        notificationService.notifyListingMatch(
            preference.getUser().getUserId(),
            property.getPropertyId(), 
            preference.getName()
        );
    }
    
    log.info("Notificações enviadas para {} usuários interessados.", matches.size());
}
    @Override
    public PropertyResponse getPropertyById(UUID id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Imóvel não encontrado"));
        return propertyMapper.toResponse(property);
    }

    @Override
    public Page<PropertyResponse> getAllProperties(PropertyFilterRequest filters, Pageable pageable) {
        QProperty qProperty = QProperty.property;
        BooleanBuilder builder = new BooleanBuilder();
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = false;
                
        if (auth != null && auth.isAuthenticated()) {
            isAdmin = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ADMIN"));
        }


        if (filters != null) {
            // --- Geolocalização ---
            if (filters.getLat() != null && filters.getLon() != null && filters.getRadius() != null) {
                NumberExpression<Double> dbLat = qProperty.geolocation.latitude;
                NumberExpression<Double> dbLon = qProperty.geolocation.longitude;
                
                NumberExpression<Double> distanceExpression = Expressions.numberTemplate(Double.class,
                        "(6371 * acos(cos(radians({0})) * cos(radians({1})) * cos(radians({2}) - radians({3})) + sin(radians({0})) * sin(radians({1}))))",
                        Expressions.constant(filters.getLat()), 
                        dbLat,                                
                        dbLon,                                
                        Expressions.constant(filters.getLon())  
                );
                builder.and(distanceExpression.loe(filters.getRadius()));
            }

            // --- Filtros Básicos ---
            if (filters.getMinPrice() != null) builder.and(qProperty.priceInCents.goe(filters.getMinPrice() * 100));
            if (filters.getMaxPrice() != null) builder.and(qProperty.priceInCents.loe(filters.getMaxPrice() * 100));
            if (filters.getMinRooms() != null) builder.and(qProperty.numberOfRooms.goe(filters.getMinRooms()));
            if (filters.getMinBedrooms() != null) builder.and(qProperty.numberOfBedrooms.goe(filters.getMinBedrooms()));
            if (filters.getFurnished() != null) builder.and(qProperty.furnished.eq(filters.getFurnished()));
            if (filters.getPetFriendly() != null) builder.and(qProperty.petFriendly.eq(filters.getPetFriendly()));
            if (filters.getType() != null) builder.and(qProperty.type.eq(filters.getType()));
            
            // --- Status ---
            if (filters.getStatus() != null) {
                builder.and(qProperty.status.eq(filters.getStatus()));
            } else {
                if (!isAdmin) {
                    builder.and(qProperty.status.eq(PropertyStatus.ACTIVE));                    
                }

            }
            
            if (filters.getCity() != null && !filters.getCity().isEmpty()) {
                builder.and(qProperty.address.city.containsIgnoreCase(filters.getCity()));
            }
            if (filters.getNeighborhood() != null && !filters.getNeighborhood().isEmpty()) {
                builder.and(qProperty.address.neighborhood.containsIgnoreCase(filters.getNeighborhood()));
            }
            
            if (filters.getState() != null && !filters.getState().isEmpty()) {
                builder.and(qProperty.address.state.containsIgnoreCase(filters.getState()));
            }

            if (filters.getAmenities() != null && !filters.getAmenities().isEmpty()) {
                for (String amenity : filters.getAmenities()) {
                    builder.and(qProperty.amenities.any().containsIgnoreCase(amenity));
                }
            }

            
            if (filters.getHouseRules() != null && !filters.getHouseRules().isEmpty()) {
                for (String rule : filters.getHouseRules()) {
                    builder.and(qProperty.houseRules.any().containsIgnoreCase(rule));
                }
            }
        }
        
        
        Page<Property> pageResult = propertyRepository.findAll((Predicate) builder, pageable);
        return pageResult.map(propertyMapper::toResponse);
    }
    
    @Override
    public List<PropertyResponse> getPropertiesByUserId(UUID userId) {
        return propertyRepository.findByUserUserId(userId).stream()
                .map(propertyMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PropertyResponse updateProperty(UUID id, PropertyRequest request) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Imóvel não encontrado"));

        if (property.getPhotoUrls() != null && !property.getPhotoUrls().isEmpty()) {
            List<String> fotosMantidas = request.photoUrls() != null ? request.photoUrls() : new ArrayList<>();
            
            for (String fotoAntiga : property.getPhotoUrls()) {
                if (!fotosMantidas.contains(fotoAntiga)) {
                    fileStorageService.deleteFile(fotoAntiga);
                }
            }
        }

        Plan plan = getUserActivePlan(property.getUser());

        if (request.videoUrl() != null && !request.videoUrl().isBlank() && !plan.getHasVideo()) {
            throw new RuntimeException("Seu plano atual não permite adicionar vídeos.");
        }

        if (property.getStatus() != PropertyStatus.ACTIVE && request.status() == PropertyStatus.ACTIVE) {
            long currentActive = propertyRepository.countPropertiesByUser(property.getUser().getUserId(), PropertyStatus.ACTIVE);
            
            if (currentActive >= plan.getPropertiesCount()) {
                throw new RuntimeException("Limite de imóveis ativos atingido (" + plan.getPropertiesCount() + ").");
            }
        }

        propertyMapper.updateEntityFromDto(request, property);
        property.setIsPriority(plan.getIsPriority());

        return propertyMapper.toResponse(propertyRepository.save(property));
    }

    @Override
    @Transactional
    public void deleteProperty(UUID id) {

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Imóvel não encontrado"));

        if (property.getPhotoUrls() != null) {
            for (String photoUrl : property.getPhotoUrls()) {
                fileStorageService.deleteFile(photoUrl);
            }
        }

        propertyRepository.delete(property);
    }

    @Override
    public PropertyResponse addPhotos(UUID id, List<MultipartFile> files) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Imóvel não encontrado"));
        
        Plan plan = getUserActivePlan(property.getUser());
        
        if (property.getPhotoUrls() == null) {
            property.setPhotoUrls(new ArrayList<>());
        }
        
        int currentCount = property.getPhotoUrls().size();
        int newCount = files.size();
        int limit = plan.getImagesCount();
        
        if ((currentCount + newCount) > limit) {
            throw new RuntimeException("Limite de fotos excedido! Máximo: " + limit);
        }
        
        for (MultipartFile file : files) {
            String photoUrl = fileStorageService.uploadFile(file);
            property.getPhotoUrls().add(photoUrl);
        }
        
        return propertyMapper.toResponse(propertyRepository.save(property));
    }

	@Override
	@Transactional
	public void updateStatus(UUID id, PropertyStatusDTO dto) {
	    Property property = propertyRepository.findById(id)
	        .orElseThrow(() -> new RuntimeException("Imóvel não encontrado"));
	    
	    property.setStatus(dto.status());
	    
	    if (dto.status() == PropertyStatus.REJECTED) {
	        property.setModerationReason(dto.reason());
	    } else if (dto.status() == PropertyStatus.ACTIVE) {
	        property.setModerationReason(null); 
	    }
	    
	    propertyRepository.save(property);
	}

    @Transactional
    public void incrementViewCount(UUID propertyId) {
    
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Não encontrado"));
    
        property.setViewCount(property.getViewCount() + 1);
    }

	@Override
	public List<PropertyResponse> getTop10ByViewCount() {
		return propertyRepository.findTop10ByStatusOrderByViewCountDesc(PropertyStatus.ACTIVE).stream()
				.map(propertyMapper::toResponse)
				.collect(Collectors.toList());
	}

	@Override
	public List<PropertyResponse> getRecentProperties(int limit) {
		Pageable pageable = PageRequest.of(0, limit);
		List<Property> properties = propertyRepository.findRecentProperties(PropertyStatus.ACTIVE, pageable);
		return properties.stream()
				.map(propertyMapper::toResponse)
				.collect(Collectors.toList());
	}
}
package br.edu.ufape.alugafacil.services.interfaces;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import br.edu.ufape.alugafacil.dtos.property.CombinedPropertiesResponse;
import br.edu.ufape.alugafacil.dtos.property.PropertyFilterRequest;
import br.edu.ufape.alugafacil.dtos.property.PropertyRequest;
import br.edu.ufape.alugafacil.dtos.property.PropertyResponse;
import br.edu.ufape.alugafacil.dtos.property.PropertyStatusDTO;
import br.edu.ufape.alugafacil.dtos.simpleProperty.SimplePropertyRequest;
import br.edu.ufape.alugafacil.dtos.simpleProperty.SimplePropertyResponse;
import br.edu.ufape.alugafacil.enums.PropertyStatus;

public interface IPropertyService {
	PropertyResponse createProperty(PropertyRequest request);
	PropertyResponse getPropertyById(UUID id);
	Page<PropertyResponse> getAllProperties(PropertyFilterRequest filters, Pageable pageable);
	Page<PropertyResponse> getPropertiesByUserId(UUID userId, PropertyStatus status, Pageable pageable);
	PropertyResponse updateProperty(UUID id, PropertyRequest request);
    void deleteProperty(UUID id);
    PropertyResponse addPhotos(UUID id, List<MultipartFile> files);
    void updateStatus(UUID id, PropertyStatusDTO dto);
    void incrementViewCount(UUID propertyId);
    List<PropertyResponse> getTop10ByViewCount();
    List<PropertyResponse> getRecentProperties(int limit);
    List<SimplePropertyResponse> getAllSimpleProperties();
    CombinedPropertiesResponse getAllPropertiesWithSimple(PropertyFilterRequest filters, Pageable pageable);
    SimplePropertyResponse createSimpleProperty(SimplePropertyRequest request, List<MultipartFile> photos);
    SimplePropertyResponse updateSimpleProperty(UUID id, SimplePropertyRequest request);
    void deleteSimpleProperty(UUID id);
    SimplePropertyResponse getSimplePropertyById(UUID id);
}

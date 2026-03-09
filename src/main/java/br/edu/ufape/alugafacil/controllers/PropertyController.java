package br.edu.ufape.alugafacil.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.edu.ufape.alugafacil.dtos.property.PropertyFilterRequest;
import br.edu.ufape.alugafacil.dtos.property.PropertyRequest;
import br.edu.ufape.alugafacil.dtos.property.PropertyResponse;
import br.edu.ufape.alugafacil.dtos.property.PropertyStatusDTO;
import br.edu.ufape.alugafacil.dtos.property.CombinedPropertiesResponse;
import br.edu.ufape.alugafacil.dtos.simpleProperty.SimplePropertyRequest;
import br.edu.ufape.alugafacil.dtos.simpleProperty.SimplePropertyResponse;
import br.edu.ufape.alugafacil.enums.PropertyStatus;
import br.edu.ufape.alugafacil.repositories.FavoriteRepository;
import br.edu.ufape.alugafacil.services.interfaces.IPropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/properties")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PropertyController {
	
	private final IPropertyService propertyService;
	private final FavoriteRepository favoriteRepository;
	
	@PostMapping
	public ResponseEntity<PropertyResponse> create(@Valid @RequestBody PropertyRequest request) {
		PropertyResponse response = propertyService.createProperty(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	
	@GetMapping
	public ResponseEntity<Page<PropertyResponse>> listAll(
				@ModelAttribute PropertyFilterRequest filters,
				@PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC, page = 0, size = 10) Pageable pageable
			) {
		Page<PropertyResponse> response = propertyService.getAllProperties(filters, pageable);
		
		return ResponseEntity.ok(response);
	}

	@GetMapping("/with-simple")
	public ResponseEntity<CombinedPropertiesResponse> listAllWithSimple(
				@ModelAttribute PropertyFilterRequest filters,
				@PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC, page = 0, size = 10) Pageable pageable
			) {
		CombinedPropertiesResponse response = propertyService.getAllPropertiesWithSimple(filters, pageable);
		
		return ResponseEntity.ok(response);
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<PropertyResponse> getById(@PathVariable UUID id) {
		PropertyResponse response = propertyService.getPropertyById(id);
		return ResponseEntity.ok(response);
	}
	
	
	@PutMapping("/{id}")
	public ResponseEntity<PropertyResponse> update(@PathVariable UUID id,
			@Valid @RequestBody PropertyRequest request) {
		PropertyResponse response = propertyService.updateProperty(id, request);
		return ResponseEntity.ok(response);
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		propertyService.deleteProperty(id);
		return ResponseEntity.noContent().build();
	}
	
	@PostMapping("/{id}/photos")
	public ResponseEntity<PropertyResponse> uploadPhoto(
				@PathVariable UUID id, 
				@RequestParam("files") List<MultipartFile> files
			) {
		PropertyResponse response = propertyService.addPhotos(id, files);
	    return ResponseEntity.ok(response);
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<Void> updateStatus(
	        @PathVariable UUID id, 
	        @RequestBody PropertyStatusDTO dto) {
	    
	    propertyService.updateStatus(id, dto); 
	    return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/view")
	public ResponseEntity<Void> incrementView(@PathVariable UUID id) {
		propertyService.incrementViewCount(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/top-by-views")
	public ResponseEntity<List<PropertyResponse>> topByViews() {
		return ResponseEntity.ok(propertyService.getTop10ByViewCount());
	}

	@GetMapping("/recent")
	public ResponseEntity<List<PropertyResponse>> getRecentProperties(
			@RequestParam(defaultValue = "20") int limit) {
		if (limit < 1) {
			limit = 20;
		}
		if (limit > 100) {
			limit = 100;
		}
		List<PropertyResponse> response = propertyService.getRecentProperties(limit);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/agency/{userId}")
    public ResponseEntity<List<PropertyResponse>> listByAgency(@PathVariable UUID userId) {
        return ResponseEntity.ok(propertyService.getPropertiesByAgencyAdminId(userId));
    }

	// --- SimpleProperty Endpoints ---
	
	@PostMapping("/simple")
	public ResponseEntity<SimplePropertyResponse> createSimpleProperty(
			@Valid @RequestPart SimplePropertyRequest request,
			@RequestPart(value = "files", required = false) List<MultipartFile> photos) {
		List<MultipartFile> filesToUpload = (photos != null) ? photos : List.of();
		SimplePropertyResponse created = propertyService.createSimpleProperty(request, filesToUpload);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@GetMapping("/simple/{id}")
	public ResponseEntity<SimplePropertyResponse> getSimplePropertyById(@PathVariable UUID id) {
		SimplePropertyResponse response = propertyService.getSimplePropertyById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/simple")
	public ResponseEntity<List<SimplePropertyResponse>> getAllSimpleProperties() {
		List<SimplePropertyResponse> response = propertyService.getAllSimpleProperties();
		return ResponseEntity.ok(response);
	}

	@PutMapping("/simple/{id}")
	public ResponseEntity<SimplePropertyResponse> updateSimpleProperty(
			@PathVariable UUID id,
			@Valid @RequestBody SimplePropertyRequest request) {
		SimplePropertyResponse response = propertyService.updateSimpleProperty(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/simple/{id}")
	public ResponseEntity<Void> deleteSimpleProperty(@PathVariable UUID id) {
		propertyService.deleteSimpleProperty(id);
		return ResponseEntity.noContent().build();
	}
	
	@GetMapping("/user/{userId}")
	public ResponseEntity<Page<PropertyResponse>> listByUser(
            @PathVariable UUID userId,
            @RequestParam(required = false) PropertyStatus status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC, page = 0, size = 10) Pageable pageable
    ) {
		return ResponseEntity.ok(propertyService.getPropertiesByUserId(userId, status, pageable));
	}
}


package br.edu.ufape.alugafacil.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufape.alugafacil.dtos.property.PropertyFilterRequest;
import br.edu.ufape.alugafacil.dtos.property.PropertyResponse;
import br.edu.ufape.alugafacil.enums.PropertyStatus;
import br.edu.ufape.alugafacil.models.Geolocation;
import br.edu.ufape.alugafacil.models.Property;
import br.edu.ufape.alugafacil.models.User;
import br.edu.ufape.alugafacil.repositories.PropertyRepository;
import br.edu.ufape.alugafacil.repositories.UserRepository;
import br.edu.ufape.alugafacil.services.NotificationService;
import br.edu.ufape.alugafacil.services.PropertyService;
import br.edu.ufape.alugafacil.services.interfaces.IFileStorageService;
import br.edu.ufape.alugafacil.models.Address;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PropertyIntegrationTest {

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private IFileStorageService fileStorageService;

    @MockBean
    private NotificationService notificationService;

    private User defaultUser;

    @BeforeEach
    void setUp() {
        propertyRepository.deleteAll();
        userRepository.deleteAll();

        defaultUser = new User();
        defaultUser.setName("User Teste Integração");
        defaultUser.setEmail("teste@integracao.com");
        defaultUser.setCpf("000.000.000-00");
        defaultUser = userRepository.save(defaultUser);
    }

    @Test
    @DisplayName("Deve salvar um imóvel no banco H2 e recuperar pelo ID")
    void shouldPersistAndRetrieveProperty() {
        Property property = new Property();
        property.setTitle("Casa de Praia");
        // property.setUser(defaultUser);
        property.setStatus(PropertyStatus.ACTIVE);
        property.setPriceInCents(250000); 
        property = propertyRepository.save(property);

        PropertyResponse response = propertyService.getPropertyById(property.getPropertyId());

        assertNotNull(response);
        assertEquals("Casa de Praia", response.title());
        assertEquals(250000, response.priceInCents());
    }

    @Test
    @DisplayName("Deve filtrar imóveis por faixa de preço corretamente")
    void shouldFilterPropertiesByPriceRange() {
        criarImovelNoBanco("Barato", 100000); 
        criarImovelNoBanco("Medio", 200000);
        criarImovelNoBanco("Caro", 500000);  

        PropertyFilterRequest filters = new PropertyFilterRequest();
        filters.setMinPrice(1500); 
        filters.setMaxPrice(3000);

        Page<PropertyResponse> result = propertyService.getAllProperties(filters, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertEquals("Medio", result.getContent().get(0).title());
    }

    @Test
    @DisplayName("Deve filtrar imóveis por Raio de distância (Geolocalização)")
    void shouldFilterByGeolocationRadius() {
        double latCentro = -8.0631;
        double lonCentro = -34.8711;

        criarImovelComGeo("Perto", -8.0650, -34.8720);
        criarImovelComGeo("Longe", -8.2000, -35.0000);

        PropertyFilterRequest filters = new PropertyFilterRequest();
        filters.setLat(latCentro);
        filters.setLon(lonCentro);
        filters.setRadius(5.0);

        Page<PropertyResponse> result = propertyService.getAllProperties(filters, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertEquals("Perto", result.getContent().get(0).title());
    }

    @Test
    @DisplayName("Deve filtrar por características específicas (PetFriendly E Mobiliado)")
    void shouldFilterByCombinedFeatures() {
        criarImovelComDetalhes("Alvo", true, true);   
        criarImovelComDetalhes("Incompleto", true, false); 
        criarImovelComDetalhes("Nenhum", false, false); 
        PropertyFilterRequest filters = new PropertyFilterRequest();
        filters.setPetFriendly(true);
        filters.setFurnished(true);

        Page<PropertyResponse> result = propertyService.getAllProperties(filters, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertEquals("Alvo", result.getContent().get(0).title());
    }

    @Test
    @DisplayName("Deve retornar resultados paginados corretamente")
    void shouldReturnPagedResults() {
        for (int i = 0; i < 15; i++) {
            criarImovelNoBanco("Imovel " + i, 100000);
        }

        Pageable pageable = PageRequest.of(0, 10);
        Page<PropertyResponse> result = propertyService.getAllProperties(null, pageable);

        assertEquals(15, result.getTotalElements()); 
        assertEquals(10, result.getContent().size()); 
        assertEquals(2, result.getTotalPages());     
    }

    private void criarImovelNoBanco(String titulo, Integer preco) {
        Property p = new Property();
        p.setTitle(titulo);
        p.setPriceInCents(preco);
        p.setStatus(PropertyStatus.ACTIVE);
        // p.setUser(defaultUser);
        p.setAddress(new Address());
        p.setGeolocation(new Geolocation());
        propertyRepository.save(p);
    }

    private void criarImovelComGeo(String titulo, double lat, double lon) {
        Property p = new Property();
        p.setTitle(titulo);
        p.setStatus(PropertyStatus.ACTIVE);
        // p.setUser(defaultUser);
        Geolocation geo = new Geolocation();
        geo.setLatitude(lat);
        geo.setLongitude(lon);
        p.setGeolocation(geo);
        p.setAddress(new Address());
        propertyRepository.save(p);
    }

    private void criarImovelComDetalhes(String titulo, boolean pet, boolean furnished) {
        Property p = new Property();
        p.setTitle(titulo);
        p.setStatus(PropertyStatus.ACTIVE);
        p.setPetFriendly(pet);
        p.setFurnished(furnished);
        // p.setUser(defaultUser);
        p.setAddress(new Address());
        p.setGeolocation(new Geolocation());
        propertyRepository.save(p);
    }
}
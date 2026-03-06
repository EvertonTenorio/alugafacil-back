package br.edu.ufape.alugafacil.repositories;


import br.edu.ufape.alugafacil.models.UserSearchPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserSearchPreferenceRepository extends JpaRepository<UserSearchPreference, UUID> {

    @Query("SELECT p FROM UserSearchPreference p WHERE p.user.userId = :idUser")
    List<UserSearchPreference> findByIdUser(@Param("idUser")UUID idUser);

   @Query("SELECT p FROM UserSearchPreference p WHERE " +
           // 1. AVISO: Comentei a exigência do fcmToken para você conseguir testar localmente. 
           // Quando for para produção com app mobile, você descomenta essa linha!
           // "(p.user.fcmToken IS NOT NULL) AND " + 

           // 2. Preço: Ignora se for nulo ou 0. E dá 10% de tolerância (flexibilidade) no valor!
           "(p.maxPriceInCents IS NULL OR p.maxPriceInCents <= 0 OR :price <= (p.maxPriceInCents * 1.1)) AND " +

           // 3. Cômodos: Só filtra se o usuário realmente exigiu mais que 0
           "(p.minBedrooms IS NULL OR p.minBedrooms <= 0 OR :bedrooms >= p.minBedrooms) AND " +
           "(p.minBathrooms IS NULL OR p.minBathrooms <= 0 OR :bathrooms >= p.minBathrooms) AND " +
           "(p.garageCount IS NULL OR p.garageCount <= 0 OR :garageCount >= p.garageCount) AND " +

           // 4. Booleanos: Se a preferência for false/nula, aceita tudo. Se for true, a casa tem que ter.
           "(p.furnished IS NULL OR p.furnished = false OR :furnished = true) AND " +
           "(p.petFriendly IS NULL OR p.petFriendly = false OR :petFriendly = true) AND " +

           // 5. Localização: Corta espaços em branco indesejados e ignora maiúsculas/minúsculas
           "(:city IS NULL OR p.city IS NULL OR TRIM(p.city) = '' OR LOWER(TRIM(p.city)) = LOWER(TRIM(:city))) AND " +
           "(:neighborhood IS NULL OR p.neighborhood IS NULL OR TRIM(p.neighborhood) = '' OR LOWER(TRIM(p.neighborhood)) = LOWER(TRIM(:neighborhood))) AND " +
           "(:state IS NULL OR p.state IS NULL OR TRIM(p.state) = '' OR LOWER(TRIM(p.state)) = LOWER(TRIM(:state))) AND " +

           // 6. Raio Geolocalizado: Desativa o cálculo automaticamente se a latitude ou longitude for 0
           "(p.searchCenter.latitude IS NULL OR p.searchCenter.longitude IS NULL OR p.searchRadiusInMeters IS NULL OR " +
           " p.searchCenter.latitude = 0.0 OR p.searchCenter.longitude = 0.0 OR p.searchRadiusInMeters <= 0 OR " +
           " (6371000 * acos(cos(radians(:lat)) * cos(radians(p.searchCenter.latitude)) * " +
           " cos(radians(p.searchCenter.longitude) - radians(:lon)) + " +
           " sin(radians(:lat)) * sin(radians(p.searchCenter.latitude)))) <= p.searchRadiusInMeters)")
    List<UserSearchPreference> findMatchingPreferences(
        @Param("price") Integer price,
        @Param("bedrooms") Integer bedrooms,
        @Param("bathrooms") Integer bathrooms,
        @Param("garageCount") Integer garageCount,
        @Param("furnished") Boolean furnished,
        @Param("petFriendly") Boolean petFriendly,
        @Param("city") String city,
        @Param("neighborhood") String neighborhood,
        @Param("state") String state,
        @Param("lat") Double lat,
        @Param("lon") Double lon
    );
}

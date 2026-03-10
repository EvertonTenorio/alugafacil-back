package br.edu.ufape.alugafacil.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonBackReference;

import br.edu.ufape.alugafacil.enums.PropertyStatus;
import br.edu.ufape.alugafacil.enums.PropertyType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Data
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@NoArgsConstructor
public class Property {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID propertyId;

    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;

    @Embedded
    private Address address;

    @Embedded
    private Geolocation geolocation;

    private Integer priceInCents;
    
    @Column(name = "weekly_rent_in_cents")
    private Integer weeklyRentInCents;
    
    @Column(name = "security_deposit_in_cents")
    private Integer securityDepositInCents;
    
    @Column(name = "minimum_lease_months")
    private Integer minimumLeaseMonths;
    
    @Column(name = "max_occupants")
    private Integer maxOccupants;
    
    @Column(name = "available_from")
    private LocalDate availableFrom;

    private Integer numberOfRooms;
    private Integer numberOfBedrooms;
    private Integer numberOfBathrooms;
    private Boolean furnished;
    private Boolean petFriendly;
    private Boolean garage;
    private Boolean isOwner;
    private Boolean isPriority;
    
    private String videoUrl;
    private String phoneNumber;

    @ElementCollection
    private List<String> photoUrls;

    @ElementCollection
    @Column(name = "amenity") 
    private List<String> amenities;

    @ElementCollection
    @Column(name = "house_rule") 
    private List<String> houseRules;

    @Enumerated(EnumType.STRING)
    private PropertyStatus status;

    @Enumerated(EnumType.STRING)
    private PropertyType type;


    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User owner;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "moderation_reason", columnDefinition = "TEXT", nullable = true)
    private String moderationReason;
    
    @ManyToOne
    @JoinColumn(name = "agency_id", nullable = true)
    private RealStateAgency agency;

    @ManyToOne
    @JoinColumn(name = "assigned_realtor_id", nullable = true)
    private User assignedRealtor;
}
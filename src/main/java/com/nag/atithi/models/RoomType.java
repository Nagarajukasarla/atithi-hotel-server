package com.nag.atithi.models;

import com.nag.atithi.enums.PremiumType;
import com.nag.atithi.enums.RoomTypeStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "room_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "max_adults", nullable = false)
    private Integer maxAdults;

    @Column(name = "max_children", nullable = false)
    private Integer maxChildren;

    @Column(name = "max_occupancy", nullable = false)
    private Integer maxOccupancy;

    @Column(name = "bed_type", length = 100)
    private String bedType;

    @Column(length = 50)
    private String size;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private RoomTypeStatus status = RoomTypeStatus.ACTIVE;

    @Column(name = "is_premium", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean isPremium = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "premium_type", length = 20)
    private PremiumType premiumType;

    @Column(name = "is_hourly", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean isHourly = false;

    @Column(name = "hourly_price", precision = 12, scale = 2)
    private BigDecimal hourlyPrice;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "room_type_amenities",
        joinColumns = @JoinColumn(name = "room_type_id"),
        inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    @Builder.Default
    private Set<Amenity> amenities = new HashSet<>();
}

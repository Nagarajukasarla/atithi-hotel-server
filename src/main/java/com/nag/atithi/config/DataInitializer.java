package com.nag.atithi.config;

import com.nag.atithi.enums.AmenityStatus;
import com.nag.atithi.models.Amenity;
import com.nag.atithi.repositories.AmenityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AmenityRepository amenityRepository;

    @Override
    public void run(String... args) throws Exception {
        if (amenityRepository.count() == 0) {
            log.info("Seeding default amenities into database...");
            List<Amenity> defaultAmenities = List.of(
                Amenity.builder()
                    .code("wifi")
                    .name("Wi-Fi")
                    .icon("Wifi")
                    .description("High-speed wireless internet access")
                    .status(AmenityStatus.ACTIVE)
                    .build(),
                Amenity.builder()
                    .code("ac")
                    .name("Air Conditioning")
                    .icon("Wind")
                    .description("Climate-controlled cooling system")
                    .status(AmenityStatus.ACTIVE)
                    .build(),
                Amenity.builder()
                    .code("tv")
                    .name("TV")
                    .icon("Tv")
                    .description("Flat-screen television with satellite channels")
                    .status(AmenityStatus.ACTIVE)
                    .build(),
                Amenity.builder()
                    .code("minibar")
                    .name("Mini Bar")
                    .icon("Coffee")
                    .description("Small in-room refrigerator stocked with refreshments")
                    .status(AmenityStatus.ACTIVE)
                    .build(),
                Amenity.builder()
                    .code("parking")
                    .name("Parking")
                    .icon("Car")
                    .description("On-site secure parking space for vehicles")
                    .status(AmenityStatus.ACTIVE)
                    .build(),
                Amenity.builder()
                    .code("breakfast")
                    .name("Breakfast")
                    .icon("Utensils")
                    .description("Freshly prepared morning breakfast service")
                    .status(AmenityStatus.ACTIVE)
                    .build(),
                Amenity.builder()
                    .code("roomservice")
                    .name("Room Service")
                    .icon("Bell")
                    .description("In-room food, drinks, and housekeeping service request")
                    .status(AmenityStatus.ACTIVE)
                    .build()
            );
            amenityRepository.saveAll(defaultAmenities);
            log.info("Successfully seeded {} default amenities.", defaultAmenities.size());
        }
    }
}

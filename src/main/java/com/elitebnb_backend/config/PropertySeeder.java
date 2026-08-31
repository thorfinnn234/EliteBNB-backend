package com.elitebnb_backend.config;

import com.elitebnb_backend.entity.Amenity;
import com.elitebnb_backend.entity.Property;
import com.elitebnb_backend.entity.PropertyImage;
import com.elitebnb_backend.entity.PropertyImageType;
import com.elitebnb_backend.entity.PropertyStatus;
import com.elitebnb_backend.entity.PropertyType;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;

import com.elitebnb_backend.repository.PropertyImageRepository;
import com.elitebnb_backend.repository.PropertyRepository;
import com.elitebnb_backend.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class PropertySeeder implements CommandLineRunner {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final PropertyImageRepository propertyImageRepository;

    public PropertySeeder(
            PropertyRepository propertyRepository,
            UserRepository userRepository,
            PropertyImageRepository propertyImageRepository
    ) {
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.propertyImageRepository = propertyImageRepository;
    }

    @Override
    public void run(String... args) {

        // Prevent duplicate seed data
        if (propertyRepository.count() > 0) {
            System.out.println(
                    "Properties already exist. Skipping property seeding."
            );
            return;
        }

        // Find an existing HOST account
        User host = userRepository.findAll()
                .stream()
                .filter(user ->
                        user.getRole() == Role.HOST
                )
                .findFirst()
                .orElse(null);

        if (host == null) {
            System.out.println(
                    "No HOST account found. Property seeding skipped."
            );
            return;
        }

        // =========================
        // PROPERTY 1
        // =========================

        Property lekkiApartment = Property.builder()
                .title("Lekki Luxury Apartment")
                .description(
                        "Modern luxury apartment in the heart of Lekki with stylish interiors and premium amenities."
                )
                .location("Lekki Phase 1, Lagos")
                .pricePerNight(95000.0)
                .bedrooms(2)
                .bathrooms(2)
                .maxGuests(4)
                .propertyType(PropertyType.APARTMENT)
                .status(PropertyStatus.ACTIVE)
                .amenities(Set.of(
                        Amenity.WIFI,
                        Amenity.POOL,
                        Amenity.PARKING,
                        Amenity.AIR_CONDITIONING,
                        Amenity.KITCHEN
                ))
                .host(host)
                .build();

        // =========================
        // PROPERTY 2
        // =========================

        Property ikoyiPenthouse = Property.builder()
                .title("Luxury Ikoyi Penthouse")
                .description(
                        "Elegant penthouse offering spacious rooms, beautiful city views and premium comfort."
                )
                .location("Ikoyi, Lagos")
                .pricePerNight(180000.0)
                .bedrooms(3)
                .bathrooms(3)
                .maxGuests(6)
                .propertyType(PropertyType.APARTMENT)
                .status(PropertyStatus.ACTIVE)
                .amenities(Set.of(
                        Amenity.WIFI,
                        Amenity.PARKING,
                        Amenity.AIR_CONDITIONING,
                        Amenity.KITCHEN,
                        Amenity.GYM,
                        Amenity.SECURITY
                ))
                .host(host)
                .build();

        // =========================
        // PROPERTY 3
        // =========================

        Property maitamaApartment = Property.builder()
                .title("Maitama Executive Apartment")
                .description(
                        "Comfortable serviced apartment located in a quiet and premium area of Maitama."
                )
                .location("Maitama, Abuja")
                .pricePerNight(120000.0)
                .bedrooms(2)
                .bathrooms(2)
                .maxGuests(4)
                .propertyType(PropertyType.APARTMENT)
                .status(PropertyStatus.ACTIVE)
                .amenities(Set.of(
                        Amenity.WIFI,
                        Amenity.PARKING,
                        Amenity.SECURITY,
                        Amenity.AIR_CONDITIONING,
                        Amenity.TV
                ))
                .host(host)
                .build();

        // =========================
        // PROPERTY 4
        // =========================

        Property asokoroVilla = Property.builder()
                .title("Asokoro Private Villa")
                .description(
                        "Spacious private villa designed for families and groups seeking privacy and premium comfort."
                )
                .location("Asokoro, Abuja")
                .pricePerNight(250000.0)
                .bedrooms(5)
                .bathrooms(5)
                .maxGuests(10)
                .propertyType(PropertyType.VILLA)
                .status(PropertyStatus.ACTIVE)
                .amenities(Set.of(
                        Amenity.WIFI,
                        Amenity.POOL,
                        Amenity.PARKING,
                        Amenity.KITCHEN,
                        Amenity.SECURITY,
                        Amenity.BALCONY
                ))
                .host(host)
                .build();

        // =========================
        // PROPERTY 5
        // =========================

        Property bodijaHouse = Property.builder()
                .title("Bodija Family House")
                .description(
                        "Peaceful and comfortable home suitable for families visiting Ibadan."
                )
                .location("Bodija, Ibadan")
                .pricePerNight(65000.0)
                .bedrooms(3)
                .bathrooms(2)
                .maxGuests(6)
                .propertyType(PropertyType.HOUSE)
                .status(PropertyStatus.ACTIVE)
                .amenities(Set.of(
                        Amenity.WIFI,
                        Amenity.PARKING,
                        Amenity.KITCHEN,
                        Amenity.TV
                ))
                .host(host)
                .build();

        // Save properties first
        propertyRepository.saveAll(
                List.of(
                        lekkiApartment,
                        ikoyiPenthouse,
                        maitamaApartment,
                        asokoroVilla,
                        bodijaHouse
                )
        );

        // =========================
        // IMAGES
        // =========================

        seedImages(
                lekkiApartment,

                "https://images.unsplash.com/photo-1600585154340-be6161a56a0c",
                "https://images.unsplash.com/photo-1600210492486-724fe5c67fb0",
                "https://images.unsplash.com/photo-1616486338812-3dadae4b4ace",
                "https://images.unsplash.com/photo-1556912167-f556f1f39fdf",
                "https://images.unsplash.com/photo-1552321554-5fefe8c9ef14"
        );

        seedImages(
                ikoyiPenthouse,

                "https://images.unsplash.com/photo-1600607687939-ce8a6c25118c",
                "https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3",
                "https://images.unsplash.com/photo-1615874959474-d609969a20ed",
                "https://images.unsplash.com/photo-1556912172-45b7abe8b7e1",
                "https://images.unsplash.com/photo-1584622650111-993a426fbf0a"
        );

        seedImages(
                maitamaApartment,

                "https://images.unsplash.com/photo-1600607688969-a5bfcd646154",
                "https://images.unsplash.com/photo-1618221195710-dd6b41faaea6",
                "https://images.unsplash.com/photo-1616594039964-ae9021a400a0",
                "https://images.unsplash.com/photo-1600585152915-d208bec867a1",
                "https://images.unsplash.com/photo-1564540574859-0dfb63985953"
        );

        seedImages(
                asokoroVilla,

                "https://images.unsplash.com/photo-1600047509807-ba8f99d2cdde",
                "https://images.unsplash.com/photo-1600566753086-00f18fb6b3ea",
                "https://images.unsplash.com/photo-1617098474202-0d0d7f60c56b",
                "https://images.unsplash.com/photo-1600566752355-35792bedcfea",
                "https://images.unsplash.com/photo-1600607687920-4e2a09cf159d"
        );

        seedImages(
                bodijaHouse,

                "https://images.unsplash.com/photo-1564013799919-ab600027ffc6",
                "https://images.unsplash.com/photo-1615873968403-89e068629265",
                "https://images.unsplash.com/photo-1617325247661-675ab4b64ae2",
                "https://images.unsplash.com/photo-1600585152220-90363fe7e115",
                "https://images.unsplash.com/photo-1583845112203-29329902332e"
        );

        System.out.println(
                "Property seed data with images created successfully!"
        );
    }

    // =========================
    // HELPER METHOD
    // =========================

    private void seedImages(
            Property property,
            String exterior,
            String livingRoom,
            String bedroom,
            String kitchen,
            String bathroom
    ) {

        PropertyImage exteriorImage =
                PropertyImage.builder()
                        .imageUrl(exterior)
                        .coverImage(true)
                        .imageType(PropertyImageType.EXTERIOR)
                        .property(property)
                        .build();

        PropertyImage livingRoomImage =
                PropertyImage.builder()
                        .imageUrl(livingRoom)
                        .coverImage(false)
                        .imageType(PropertyImageType.LIVING_ROOM)
                        .property(property)
                        .build();

        PropertyImage bedroomImage =
                PropertyImage.builder()
                        .imageUrl(bedroom)
                        .coverImage(false)
                        .imageType(PropertyImageType.BEDROOM)
                        .property(property)
                        .build();

        PropertyImage kitchenImage =
                PropertyImage.builder()
                        .imageUrl(kitchen)
                        .coverImage(false)
                        .imageType(PropertyImageType.KITCHEN)
                        .property(property)
                        .build();

        PropertyImage bathroomImage =
                PropertyImage.builder()
                        .imageUrl(bathroom)
                        .coverImage(false)
                        .imageType(PropertyImageType.BATHROOM)
                        .property(property)
                        .build();

        propertyImageRepository.saveAll(
                List.of(
                        exteriorImage,
                        livingRoomImage,
                        bedroomImage,
                        kitchenImage,
                        bathroomImage
                )
        );
    }
}
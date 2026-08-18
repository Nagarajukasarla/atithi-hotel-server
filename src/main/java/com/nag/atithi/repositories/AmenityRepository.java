package com.nag.atithi.repositories;

import com.nag.atithi.models.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Long> {
    Optional<Amenity> findByCode(String code);
    List<Amenity> findAllByCodeIn(Collection<String> codes);
}

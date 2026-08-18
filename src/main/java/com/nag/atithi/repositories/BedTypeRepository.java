package com.nag.atithi.repositories;

import com.nag.atithi.models.BedType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BedTypeRepository extends JpaRepository<BedType, Long> {
    List<BedType> findAllByHotelId(Long hotelId);
    Optional<BedType> findByHotelIdAndName(Long hotelId, String name);
}

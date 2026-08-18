package com.nag.atithi.repositories;

import com.nag.atithi.models.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
    List<RoomType> findAllByHotelId(Long hotelId);
    Optional<RoomType> findByHotelIdAndId(Long hotelId, Long id);
    Optional<RoomType> findByHotelIdAndName(Long hotelId, String name);
}

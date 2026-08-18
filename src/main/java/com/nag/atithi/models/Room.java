package com.nag.atithi.models;

import com.nag.atithi.enums.RoomState;
import com.nag.atithi.enums.RoomStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(name = "room_number", nullable = false, length = 50)
    private String roomNumber;

    @Column(nullable = false, length = 50)
    private String floor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @Column(nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_status", nullable = false, length = 30)
    @Builder.Default
    private RoomStatus roomStatus = RoomStatus.VACANT;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_state", nullable = false, length = 30)
    @Builder.Default
    private RoomState roomState = RoomState.CLEAN;

    @Column(name = "last_cleaned")
    private LocalDateTime lastCleaned;

    @Column(columnDefinition = "TEXT")
    private String notes;
}

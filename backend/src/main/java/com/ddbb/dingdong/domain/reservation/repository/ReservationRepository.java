package com.ddbb.dingdong.domain.reservation.repository;

import com.ddbb.dingdong.domain.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Query("SELECT r FROM Reservation r , Location l WHERE l.clusterLabel = :clusterLabel AND r.id = l.reservationId")
    List<Reservation> findAllByClusterLabel(@Param("clusterLabel") String clusterLabel);

    @Query("SELECT r FROM Reservation r WHERE r.userId = :userId AND r.startDate = :startDate AND r.status = 'PENDING' AND r.direction = :direction")
    Optional<Reservation> findPendingReservation(
            @Param("userId") Long userId,
            @Param("startDate") Date startDate,
            // TODO 예매가 유효한지 검증
}

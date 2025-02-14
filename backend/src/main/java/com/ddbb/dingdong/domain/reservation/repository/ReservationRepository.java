package com.ddbb.dingdong.domain.reservation.repository;

import com.ddbb.dingdong.domain.reservation.entity.Reservation;
import com.ddbb.dingdong.domain.reservation.entity.vo.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Query("SELECT r FROM Reservation r , Location l WHERE l.clusterLabel = :clusterLabel AND r.id = l.reservationId")
    List<Reservation> findAllByClusterLabel(@Param("clusterLabel") String clusterLabel);

    @Query("""
        SELECT r
             FROM Reservation r 
                 WHERE r.userId = :userId
                      AND (
                          (r.direction = 'TO_SCHOOL' AND r.arrivalTime = :time)
                          OR
                          (r.direction = 'TO_HOME' AND r.departureTime = :time)
                      )
                      AND r.status = 'PENDING'
                      AND r.type = 'TOGETHER'
    """)
    Optional<Reservation> findPendingReservation(
            @Param("userId") Long userId,
            @Param("status") ReservationStatus status,
            @Param("time") LocalDateTime time
    );
            // TODO 예매가 유효한지 검증
}

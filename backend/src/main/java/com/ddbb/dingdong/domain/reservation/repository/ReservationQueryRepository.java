package com.ddbb.dingdong.domain.reservation.repository;

import com.ddbb.dingdong.domain.reservation.entity.Reservation;
import com.ddbb.dingdong.domain.reservation.repository.projection.ReservationIdProjection;
import com.ddbb.dingdong.domain.reservation.repository.projection.UserReservationProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationQueryRepository extends JpaRepository<Reservation, Long> {
    @Query("""
        SELECT DISTINCT l.stationName AS userHomeStationName,
               r.id AS reservationId,
               r.startDate AS startDate,
               r.direction AS direction,
               r.arrivalTime AS expectedArrivalTime,
               r.departureTime AS expectedDepartureTime,
               bs_arrival.departureTime AS realDepartureTime,
               bs_arrival.arrivalTime AS realArrivalTime,
               r.status AS reservationStatus,
               bs_arrival.id AS busScheduleId,
               b.name AS busName,
               bs_arrival.status AS busStatus,
               bs.roadNameAddress AS busStopRoadNameAddress,
               bs.expectedArrivalTime AS busStopArrivalTime
        FROM Reservation r
        LEFT JOIN Ticket t ON r.id = t.reservation.id
        LEFT JOIN BusStop bs ON t.busStopId = bs.id
        LEFT JOIN BusSchedule bs_arrival ON bs_arrival.id = t.busScheduleId
        LEFT JOIN Bus b ON bs_arrival.bus.id = b.id
        LEFT JOIN Location l ON l.reservationId = r.id
        WHERE r.userId = :userId
            AND (
                (:category = 0)
                OR
                (:category = 1 AND CAST(r.status AS STRING) = 'ALLOCATED')
                OR
                (:category = 2 AND CAST(r.status AS STRING) = 'PENDING')
                OR
                (:category = 3 AND CAST(r.status AS STRING) = 'FAIL_ALLOCATED')
                OR
                (:category = 4 AND CAST(bs_arrival.status AS STRING) = 'ENDED')
                OR
                (:category = 5 AND CAST(r.status AS STRING) = 'CANCELED')
                OR
                (:category = 6 AND (CAST(r.status AS STRING) = 'ALLOCATED' OR CAST(r.status AS STRING) = 'PENDING'))
                )
            ORDER BY
               CASE WHEN :sort = 0 THEN r.startDate END DESC,
               CASE WHEN :sort = 1 THEN r.startDate END ASC
        """)
    Page<UserReservationProjection> queryReservationsByUserId(@Param("userId") Long userId, @Param("category") int category, @Param("sort") int sort, Pageable p);

    @Query("""
        SELECT r.id
            FROM Reservation r
        WHERE (
         ( r.direction = 'TO_SCHOOL' AND r.arrivalTime = :time )
         OR
         ( r.direction = 'TO_HOME' AND r.departureTime = :time )
        )
        AND r.userId = :userId
        AND r.status = 'ALLOCATED'
    """)
    List<ReservationIdProjection> findReservationIdByUserIdAndTime(
            @Param("userId") Long userId,
            @Param("time") LocalDateTime time
    );
}

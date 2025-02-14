package com.ddbb.dingdong.domain.transportation.repository;

import com.ddbb.dingdong.domain.transportation.entity.BusSchedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface BusScheduleRepository extends JpaRepository<BusSchedule, Long> {
    @Modifying
    @Query("UPDATE BusStop bst SET bst.expectedArrivalTime = :newArrivalTime WHERE bst.id = :busStopId")
    int updateBusStopArrivalTime(
            @Param("newArrivalTime") LocalDateTime newArrivalTime,
            @Param("busStopId") Long busStopId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BusSchedule b WHERE b.id = :busScheduleId")
    Optional<BusSchedule> findByIdWithLock(@Param("busScheduleId") Long busScheduleId);
}

package com.ddbb.dingdong.application.usecase.reservation;

import com.ddbb.dingdong.application.common.Params;
import com.ddbb.dingdong.application.common.UseCase;
import com.ddbb.dingdong.domain.reservation.entity.Reservation;
import com.ddbb.dingdong.domain.reservation.entity.vo.Direction;
import com.ddbb.dingdong.domain.reservation.entity.vo.ReservationStatus;
import com.ddbb.dingdong.domain.reservation.entity.vo.ReservationType;
import com.ddbb.dingdong.domain.reservation.repository.ReservationRepository;
import com.ddbb.dingdong.domain.reservation.service.ReservationErrors;
import com.ddbb.dingdong.domain.reservation.service.ReservationManagement;
import com.ddbb.dingdong.domain.transportation.entity.BusSchedule;
import com.ddbb.dingdong.domain.transportation.repository.BusScheduleRepository;
import com.ddbb.dingdong.infrastructure.auth.encrypt.TokenManager;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class RequestTogetherReservationUseCase implements UseCase<RequestTogetherReservationUseCase.Param, RequestTogetherReservationUseCase.Result> {
    private final TokenManager tokenManager;
    private final BusScheduleRepository busScheduleRepository;
    private final ReservationManagement reservationManagement;
    private final ReservationRepository reservationRepository;

    @Override
    @Transactional
    @Async
    public Result execute(Param param) {
        LocalDateTime hopeTime = extractTimeFromBusSchedule(param);
        checkHasDuplicatedReservation(param.userId, hopeTime);
        temporaryReserve(param.userId, param.busScheduleId);
        String token = generateToken(param);
        return new Result(token);
    }

    private String generateToken(Param param) {
        return tokenManager.generateToken(param);
    }

    private void checkHasDuplicatedReservation(Long userId, LocalDateTime hopeTime) {
        reservationManagement.checkHasDuplicatedReservation(userId, hopeTime);
    }

    private Reservation temporaryReserve(Long userId, Long busScheduleId) {
        BusSchedule schedule = busScheduleRepository.findByIdWithLock(busScheduleId)
                .orElseThrow(ReservationErrors.BUS_SCHEDULE_NOT_FOUND::toException);

        if (schedule.getRemainingSeats() <= 0) throw ReservationErrors.TICKET_SOLD_OUT.toException();

        schedule.setRemainingSeats(schedule.getRemainingSeats() - 1);
        busScheduleRepository.save(schedule);

        Reservation reservation = new Reservation(
                null,
                schedule.getDirection(),
                ReservationType.TOGETHER,
                schedule.getStartDate(),
                schedule.getDepartureTime(),
                schedule.getArrivalTime(),
                ReservationStatus.PENDING,
                userId,
                null
        );
        return reservationRepository.save(reservation);
    }

    private LocalDateTime extractTimeFromBusSchedule(Param param) {
        Long busScheduleId = param.busScheduleId;
        BusSchedule schedule = busScheduleRepository.findById(busScheduleId).orElseThrow(ReservationErrors.BUS_SCHEDULE_NOT_FOUND::toException);
        LocalDateTime hopeTime = schedule.getDirection().equals(Direction.TO_SCHOOL)
                ? schedule.getArrivalTime()
                : schedule.getDepartureTime();
        return hopeTime;
    }

    @Getter
    @AllArgsConstructor
    public static class Param implements Params {
        private Long userId;
        private Long busStopId;
        private Long busScheduleId;
    }

    @Getter
    @AllArgsConstructor
    public static class Result extends CompletableFuture<Result> {
        private String token;
    }
}

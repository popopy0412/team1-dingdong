package com.ddbb.dingdong.application.usecase.reservation;

import com.ddbb.dingdong.application.common.Params;
import com.ddbb.dingdong.application.common.UseCase;
import com.ddbb.dingdong.application.exception.APIException;
import com.ddbb.dingdong.domain.common.exception.DomainException;
import com.ddbb.dingdong.domain.payment.service.PaymentManagement;
import com.ddbb.dingdong.domain.reservation.entity.Reservation;
import com.ddbb.dingdong.domain.reservation.entity.Ticket;
import com.ddbb.dingdong.domain.reservation.entity.vo.Direction;
import com.ddbb.dingdong.domain.reservation.entity.vo.ReservationStatus;
import com.ddbb.dingdong.domain.reservation.entity.vo.ReservationType;
import com.ddbb.dingdong.domain.reservation.repository.BusStopRepository;
import com.ddbb.dingdong.domain.reservation.repository.ReservationRepository;
import com.ddbb.dingdong.domain.reservation.service.ReservationErrors;
import com.ddbb.dingdong.domain.reservation.service.ReservationManagement;
import com.ddbb.dingdong.domain.transportation.entity.BusSchedule;
import com.ddbb.dingdong.domain.transportation.entity.BusStop;
import com.ddbb.dingdong.domain.transportation.entity.vo.OperationStatus;
import com.ddbb.dingdong.domain.transportation.repository.BusScheduleRepository;
import com.ddbb.dingdong.infrastructure.auth.encrypt.TokenManager;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MakeTogetherReservationUseCase implements UseCase<MakeTogetherReservationUseCase.Param, Void> {
    private final ReservationManagement reservationManagement;
    private final PaymentManagement paymentManagement;
    private final BusStopRepository busStopRepository;
    private final TokenManager tokenManager;
    private final BusScheduleRepository busScheduleRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    @Override
    public Void execute(Param param) {
        validateToken(param);
        LocalDateTime hopeTime = extractTimeFromBusSchedule(param);
        checkHasDuplicatedReservation(param.reservationInfo.userId, hopeTime);
        checkValidateReservation(param);
        reserve(param);
        pay(param);

        return null;
    }

    private void checkHasDuplicatedReservation(Long userId, LocalDateTime hopeTime) {
        reservationManagement.checkHasDuplicatedReservation(userId, hopeTime);
    }

    private void checkValidateReservation(Param param) {
        Reservation reservation = reservationRepository.findPendingReservation(userId, scheduleId);
    }

    private LocalDateTime extractTimeFromBusSchedule(MakeTogetherReservationUseCase.Param param) {
        Long busScheduleId = param.reservationInfo.busScheduleId;
        BusSchedule schedule = busScheduleRepository.findById(busScheduleId).orElseThrow(ReservationErrors.BUS_SCHEDULE_NOT_FOUND::toException);
        LocalDateTime hopeTime = schedule.getDirection().equals(Direction.TO_SCHOOL)
                ? schedule.getArrivalTime()
                : schedule.getDepartureTime();
        return hopeTime;
    }

    private void validateToken(Param param) {
        tokenManager.validateToken(param.token, param.reservationInfo);
    }

    private void pay(Param param) {
        paymentManagement.pay(param.getReservationInfo().userId, 1);
    }

    private void reserve(Param param) {
        Long userId = param.reservationInfo.userId;
        Long busStopId = param.reservationInfo.getBusStopId();
        Long busScheduleId = param.reservationInfo.getBusScheduleId();

        BusSchedule busSchedule = busScheduleRepository.findById(busScheduleId)
                .orElseThrow(ReservationErrors.BUS_SCHEDULE_NOT_FOUND::toException);

        if (!busSchedule.getStatus().equals(OperationStatus.READY)) {
            throw ReservationErrors.EXCEEDED_RESERVATION_DATE.toException();
        }

        BusStop busStop = busStopRepository.findById(busStopId)
                .orElseThrow(ReservationErrors.BUS_STOP_NOT_FOUND::toException);

        Long queryBusScheduleId = busStop.getPath().getBusSchedule().getId();

        if (!busScheduleId.equals(queryBusScheduleId)) {
            throw ReservationErrors.INVALID_BUS_SCHEDULE.toException();
        }

        Reservation reservation = makeReservation(busSchedule, userId);
        Ticket ticket = new Ticket(null, busScheduleId, busStopId, reservation);

        reservation.issueTicket(ticket);
        busSchedule.issue();

        reservationManagement.reserve(reservation);
    }

    private Reservation makeReservation(BusSchedule busSchedule, Long userId) {
        Reservation.ReservationBuilder reservationBuilder = Reservation.builder()
                .userId(userId)
                .type(ReservationType.TOGETHER)
                .direction(busSchedule.getDirection())
                .status(ReservationStatus.ALLOCATED)
                .startDate(busSchedule.getStartDate());
        if(busSchedule.getDirection().equals(Direction.TO_SCHOOL)) {
            reservationBuilder.arrivalTime(busSchedule.getArrivalTime());
        } else {
            reservationBuilder.departureTime(busSchedule.getDepartureTime());
        }

        return reservationBuilder.build();
    }

    @Getter
    @AllArgsConstructor
    public static class Param implements Params {
        private String token;
        private MakeTogetherReservationUseCase.Param.ReservationInfo reservationInfo;

        @Getter
        @AllArgsConstructor
        public static class ReservationInfo {
            private Long userId;
            private Long busStopId;
            private Long busScheduleId;
        }
    }
}

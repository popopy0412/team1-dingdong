package com.ddbb.dingdong.application.usecase.reservation;

import com.ddbb.dingdong.application.common.Params;
import com.ddbb.dingdong.application.common.UseCase;
import com.ddbb.dingdong.domain.clustering.entity.Location;
import com.ddbb.dingdong.domain.clustering.service.ClusteringService;
import com.ddbb.dingdong.domain.payment.service.PaymentManagement;
import com.ddbb.dingdong.domain.reservation.entity.Reservation;
import com.ddbb.dingdong.domain.reservation.entity.vo.Direction;
import com.ddbb.dingdong.domain.reservation.entity.vo.ReservationStatus;
import com.ddbb.dingdong.domain.reservation.entity.vo.ReservationType;
import com.ddbb.dingdong.domain.reservation.service.ReservationErrors;
import com.ddbb.dingdong.domain.reservation.service.ReservationManagement;
import com.ddbb.dingdong.domain.user.entity.Home;
import com.ddbb.dingdong.domain.user.service.UserManagement;
import com.ddbb.dingdong.infrastructure.auth.encrypt.TokenManager;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MakeGeneralReservationUseCase implements UseCase<MakeGeneralReservationUseCase.Param, Void> {
    private final ReservationManagement reservationManagement;
    private final PaymentManagement paymentManagement;
    private final TokenManager tokenManager;
    private final UserManagement userManagement;
    private final ClusteringService clusteringService;

    @Transactional
    @Override
    public Void execute(Param param) {
        validateToken(param);
        checkHasDuplicatedReservation(param.reservationInfo.userId, param.reservationInfo.reservationDates);
        reserve(param);
        pay(param);

        return null;
    }

    private void validateToken(Param param) {
        tokenManager.validateToken(param.token, param.reservationInfo);
    }

    private void checkHasDuplicatedReservation(Long userId, List<Param.ReservationInfo.ReservationDate> hopeTimes) {
        boolean hasDuplicates = hopeTimes.stream()
                .map(Param.ReservationInfo.ReservationDate::getDate)
                .collect(Collectors.toSet()).size() < hopeTimes.size();
        if (hasDuplicates) throw ReservationErrors.DUPLICATED_RESERVATION_DATE.toException();
        for (Param.ReservationInfo.ReservationDate hopeTime : hopeTimes) {
            reservationManagement.checkHasDuplicatedReservation(userId, hopeTime.date);
        }
    }

    private void reserve(Param param) {
        Param.ReservationInfo reservationInfo = param.reservationInfo;
        Home home = userManagement.load(reservationInfo.userId).getHome();

        for(Param.ReservationInfo.ReservationDate date : reservationInfo.reservationDates) {
            Reservation.ReservationBuilder reservationBuilder = Reservation.builder()
                    .userId(reservationInfo.getUserId())
                    .type(ReservationType.GENERAL)
                    .direction(Direction.valueOf(reservationInfo.getDirection()))
                    .status(ReservationStatus.PENDING)
                    .startDate(date.date.toLocalDate());
            if(Direction.valueOf(reservationInfo.getDirection()).equals(Direction.TO_SCHOOL)) {
                reservationBuilder.arrivalTime(date.date);
            } else {
                reservationBuilder.departureTime(date.date);
            }
            Location location = new Location();
            Long reservationId = reservationManagement.reserve(reservationBuilder.build()).getId();
            location.setReservationId(reservationId);
            location.setLatitude(home.getStationLatitude());
            location.setLongitude(home.getStationLongitude());
            location.setStationName(home.getStationName());
            clusteringService.saveLocation(location);
        }
    }

    private void pay(Param param) {
        Long userId = param.getReservationInfo().userId;
        int quantity = param.getReservationInfo().getReservationDates().size();
        paymentManagement.pay(userId, quantity);
    }

    @Getter
    @AllArgsConstructor
    public static class Param implements Params {
        private String token;
        private ReservationInfo reservationInfo;

        @Getter
        @AllArgsConstructor
        public static class ReservationInfo {
            private Long userId;
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            private String direction;
            private List<ReservationDate> reservationDates;

            @Getter
            @AllArgsConstructor
            public static class ReservationDate {
                @JsonSerialize(using = LocalDateTimeSerializer.class)
                @JsonDeserialize(using = LocalDateTimeDeserializer.class)
                @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
                private LocalDateTime date;
            }
        }
    }
}

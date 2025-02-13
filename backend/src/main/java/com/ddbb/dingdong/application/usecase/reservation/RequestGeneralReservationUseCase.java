package com.ddbb.dingdong.application.usecase.reservation;

import com.ddbb.dingdong.application.common.Params;
import com.ddbb.dingdong.application.common.UseCase;
import com.ddbb.dingdong.application.usecase.reservation.error.ReservationInvalidParamErrors;
import com.ddbb.dingdong.domain.reservation.entity.vo.Direction;
import com.ddbb.dingdong.domain.reservation.service.ReservationErrors;
import com.ddbb.dingdong.domain.reservation.service.ReservationManagement;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestGeneralReservationUseCase implements UseCase<RequestGeneralReservationUseCase.Param, RequestGeneralReservationUseCase.Result> {
    private final TokenManager tokenManager;
    private final ReservationManagement reservationManagement;

    @Override
    public Result execute(Param param) {
        param.validate();
        checkHasDuplicatedReservation(param.userId, param.reservationDates);
        String token = generateToken(param);
        return new Result(token);
    }

    private String generateToken(Param param) {
        return tokenManager.generateToken(param);
    }

    private void checkHasDuplicatedReservation(Long userId, List<RequestGeneralReservationUseCase.Param.ReservationInfo> hopeTimes) {
        boolean hasDuplicates = hopeTimes.stream()
                .map(Param.ReservationInfo::getDate)
                .collect(Collectors.toSet()).size() < hopeTimes.size();
        if (hasDuplicates) throw ReservationErrors.DUPLICATED_RESERVATION_DATE.toException();
        for (RequestGeneralReservationUseCase.Param.ReservationInfo hopeTime : hopeTimes) {
            reservationManagement.checkHasDuplicatedReservation(userId, hopeTime.date);
        }
    }

    @Getter
    @AllArgsConstructor
    public static class Param implements Params {
        private Long userId;
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private String direction;
        private List<ReservationInfo> reservationDates;

        @Getter
        @AllArgsConstructor
        public static class ReservationInfo {
            @JsonSerialize(using = LocalDateTimeSerializer.class)
            @JsonDeserialize(using = LocalDateTimeDeserializer.class)
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
            private LocalDateTime date;

            public boolean equals(ReservationInfo o) {
                return o.date.isEqual(this.date);

            }
        }

        @Override
        public boolean validate() {
            if(!validateDirection(direction)) throw ReservationInvalidParamErrors.INVALID_DIRECTION.toException();
            if(!validateReservationsDate(reservationDates)) throw ReservationInvalidParamErrors.INVALID_DATE.toException();

            return true;
        }

        private static boolean validateDirection(String direction) {
            try {
                Direction.valueOf(direction);
            } catch (Exception e) {
                return false;
            }

            return true;
        }

        private static boolean validateReservationsDate(List<ReservationInfo> reservations) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime minDate, maxDate;
            minDate = now.plusHours(48);
            maxDate = now.plusMonths(2);

            return reservations.stream().anyMatch(
                    reservationInfo -> {
                        if (reservationInfo.date.isBefore(minDate) || reservationInfo.date.isAfter(maxDate)) {
                            return false;
                        }
                        if (reservationInfo.date.getMinute() != 0 && reservationInfo.date.getMinute() != 30) {
                            return false;
                        }
                        return true;
                    }
            );
        }
    }

    @Getter
    @AllArgsConstructor
    public static class Result {
        private String token;
    }
}

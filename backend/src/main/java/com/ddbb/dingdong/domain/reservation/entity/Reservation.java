package com.ddbb.dingdong.domain.reservation.entity;

import com.ddbb.dingdong.domain.reservation.entity.vo.Direction;
import com.ddbb.dingdong.domain.reservation.entity.vo.ReservationStatus;
import com.ddbb.dingdong.domain.reservation.entity.vo.ReservationType;
import com.ddbb.dingdong.domain.reservation.service.ReservationErrors;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id = null;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direction direction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationType type;

    @Column(nullable = false)
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate startDate;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime departureTime;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime arrivalTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(nullable = false)
    private Long userId;

    @OneToOne(mappedBy = "reservation", cascade = CascadeType.ALL)
    private Ticket ticket;

    public void cancel() {
        if(!ReservationStatus.PENDING.equals(this.status)) {
            throw ReservationErrors.CANCELLATION_NOT_ALLOWED.toException();
        }

        this.status = ReservationStatus.CANCELED;
    }

    public void fail() {
        if(!ReservationStatus.PENDING.equals(this.status)) {
            throw ReservationErrors.ALLOCATION_NOT_ALLOWED.toException();
        }

        this.status = ReservationStatus.FAIL_ALLOCATED;
    }

    public void allocate(Ticket ticket) {
        if(ticket == null || ticket.getBusScheduleId() == null || ticket.getBusStopId() == null) {
            throw ReservationErrors.INVALID_BUS_TICKET.toException();
        }
        if(!ReservationStatus.PENDING.equals(this.status)) {
            throw ReservationErrors.ALLOCATION_NOT_ALLOWED.toException();
        }

        this.ticket = ticket;
        this.status = ReservationStatus.ALLOCATED;
    }

    public void issueTicket(Ticket ticket) {
        if(ticket == null || ticket.getBusScheduleId() == null || ticket.getBusStopId() == null) {
            throw ReservationErrors.INVALID_BUS_TICKET.toException();
        }
        if(!ReservationStatus.ALLOCATED.equals(this.status)) {
            throw ReservationErrors.ALLOCATION_NOT_ALLOWED.toException();
        }

        this.ticket = ticket;
    }
}

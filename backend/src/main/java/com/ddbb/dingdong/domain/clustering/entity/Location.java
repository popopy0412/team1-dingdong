package com.ddbb.dingdong.domain.clustering.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "locations")
public class Location implements Comparable<Location> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private double latitude;   // 위도
    private double longitude;  // 경도
    private Integer clusterLabel; // 클러스터 라벨

    public Location() {}

    public Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public int compareTo(Location other) {
        if (this.clusterLabel < other.clusterLabel) {
            return -1;
        } else {
            if (this.clusterLabel > other.clusterLabel) return 1;
            return 0;
        }
    }
}

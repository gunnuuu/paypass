package project.paypass.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Entity
public class Geofence {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long center;
}
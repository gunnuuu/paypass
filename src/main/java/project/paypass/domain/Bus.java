package project.paypass.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@NoArgsConstructor
@Entity
public class Bus {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String busNumber;

    private int price;

    private ArrayList<String> stops;
}

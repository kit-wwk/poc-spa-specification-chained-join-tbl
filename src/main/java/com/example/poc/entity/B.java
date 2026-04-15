package com.example.poc.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "entity_b")
public class B {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String label;

    @OneToMany(mappedBy = "b")
    private List<AB> abList = new ArrayList<>();

    public B() {}

    public B(String label) {
        this.label = label;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public List<AB> getAbList() { return abList; }
    public void setAbList(List<AB> abList) { this.abList = abList; }
}

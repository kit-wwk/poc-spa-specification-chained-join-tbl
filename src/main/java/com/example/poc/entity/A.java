package com.example.poc.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "entity_a")
public class A {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "a", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AB> abList = new ArrayList<>();

    public A() {}

    public A(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<AB> getAbList() { return abList; }
    public void setAbList(List<AB> abList) { this.abList = abList; }
}

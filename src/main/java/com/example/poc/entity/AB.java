package com.example.poc.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "entity_ab")
public class AB {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "a_id")
    private A a;

    @ManyToOne
    @JoinColumn(name = "b_id")
    private B b;

    private Integer priority;

    public AB() {}

    public AB(A a, B b, Integer priority) {
        this.a = a;
        this.b = b;
        this.priority = priority;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public A getA() { return a; }
    public void setA(A a) { this.a = a; }

    public B getB() { return b; }
    public void setB(B b) { this.b = b; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
}

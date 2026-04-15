package com.example.poc.repository;

import com.example.poc.entity.A;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ARepository extends JpaRepository<A, Long>, JpaSpecificationExecutor<A> {
}

package com.example.poc.job.repository;

import com.example.poc.job.entity.JobExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobExecutionRepository extends JpaRepository<JobExecution, Long> {
    List<JobExecution> findByJobNameAndJobGroupOrderByStartTimeDesc(String jobName, String jobGroup);
}

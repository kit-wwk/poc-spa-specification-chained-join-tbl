package com.example.poc.job.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "job_execution_history")
public class JobExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String jobName;

    @Column(nullable = false)
    private String jobGroup;

    @Column(nullable = false)
    private Instant startTime;

    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    public enum ExecutionStatus { RUNNING, SUCCESS, FAILED }

    public JobExecution() {}

    public JobExecution(String jobName, String jobGroup, Instant startTime, ExecutionStatus status) {
        this.jobName = jobName;
        this.jobGroup = jobGroup;
        this.startTime = startTime;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }

    public String getJobGroup() { return jobGroup; }
    public void setJobGroup(String jobGroup) { this.jobGroup = jobGroup; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public ExecutionStatus getStatus() { return status; }
    public void setStatus(ExecutionStatus status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}

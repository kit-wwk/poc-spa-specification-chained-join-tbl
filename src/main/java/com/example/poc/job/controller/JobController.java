package com.example.poc.job.controller;

import com.example.poc.job.dto.JobStatusResponse;
import com.example.poc.job.dto.ScheduleRequest;
import com.example.poc.job.entity.JobExecution;
import com.example.poc.job.service.JobManagementService;
import jakarta.validation.Valid;
import org.quartz.SchedulerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobManagementService jobManagementService;

    public JobController(JobManagementService jobManagementService) {
        this.jobManagementService = jobManagementService;
    }

    @GetMapping
    public ResponseEntity<List<JobStatusResponse>> listJobs() throws SchedulerException {
        return ResponseEntity.ok(jobManagementService.listAllJobs());
    }

    @PostMapping("/{jobName}/schedule")
    public ResponseEntity<JobStatusResponse> scheduleJob(
            @PathVariable String jobName,
            @Valid @RequestBody ScheduleRequest request) throws SchedulerException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobManagementService.scheduleJob(jobName, request));
    }

    @PutMapping("/{jobName}/schedule")
    public ResponseEntity<JobStatusResponse> rescheduleJob(
            @PathVariable String jobName,
            @Valid @RequestBody ScheduleRequest request) throws SchedulerException {
        return ResponseEntity.ok(jobManagementService.rescheduleJob(jobName, request));
    }

    @DeleteMapping("/{jobName}")
    public ResponseEntity<Void> deleteJob(@PathVariable String jobName) throws SchedulerException {
        jobManagementService.deleteJob(jobName);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{jobName}/pause")
    public ResponseEntity<Void> pauseJob(@PathVariable String jobName) throws SchedulerException {
        jobManagementService.pauseJob(jobName);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{jobName}/resume")
    public ResponseEntity<Void> resumeJob(@PathVariable String jobName) throws SchedulerException {
        jobManagementService.resumeJob(jobName);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{jobName}/trigger")
    public ResponseEntity<Void> triggerNow(@PathVariable String jobName) throws SchedulerException {
        jobManagementService.triggerNow(jobName);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{jobName}/executions")
    public ResponseEntity<List<JobExecution>> getExecutionHistory(@PathVariable String jobName)
            throws SchedulerException {
        return ResponseEntity.ok(jobManagementService.getExecutionHistory(jobName));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(SchedulerException.class)
    public ResponseEntity<String> handleSchedulerException(SchedulerException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }
}

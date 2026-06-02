package com.example.poc.job.dto;

import java.time.Instant;

public class JobStatusResponse {

    private String jobName;
    private String jobGroup;
    private String description;
    private String status;
    private String cronExpression;
    private String timeZone;
    private Instant nextFireTime;
    private Instant previousFireTime;

    public JobStatusResponse() {}

    public static JobStatusResponse of(String jobName, String jobGroup, String description,
                                       String status, String cronExpression, String timeZone,
                                       Instant nextFireTime, Instant previousFireTime) {
        JobStatusResponse r = new JobStatusResponse();
        r.jobName = jobName;
        r.jobGroup = jobGroup;
        r.description = description;
        r.status = status;
        r.cronExpression = cronExpression;
        r.timeZone = timeZone;
        r.nextFireTime = nextFireTime;
        r.previousFireTime = previousFireTime;
        return r;
    }

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }

    public String getJobGroup() { return jobGroup; }
    public void setJobGroup(String jobGroup) { this.jobGroup = jobGroup; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }

    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }

    public Instant getNextFireTime() { return nextFireTime; }
    public void setNextFireTime(Instant nextFireTime) { this.nextFireTime = nextFireTime; }

    public Instant getPreviousFireTime() { return previousFireTime; }
    public void setPreviousFireTime(Instant previousFireTime) { this.previousFireTime = previousFireTime; }
}

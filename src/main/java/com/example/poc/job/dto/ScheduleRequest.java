package com.example.poc.job.dto;

import jakarta.validation.constraints.NotBlank;

public class ScheduleRequest {

    @NotBlank(message = "cronExpression is required")
    private String cronExpression;

    private String timeZone = "UTC";

    private String description;

    public ScheduleRequest() {}

    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }

    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

package com.example.poc.job.service;

import com.example.poc.job.annotation.JobDefinition;
import com.example.poc.job.core.JobExecutable;
import com.example.poc.job.core.QuartzJobAdapter;
import com.example.poc.job.dto.JobStatusResponse;
import com.example.poc.job.dto.ScheduleRequest;
import com.example.poc.job.entity.JobExecution;
import com.example.poc.job.repository.JobExecutionRepository;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class JobManagementService {

    private final Scheduler scheduler;
    private final ApplicationContext applicationContext;
    private final JobExecutionRepository jobExecutionRepository;

    public JobManagementService(Scheduler scheduler,
                                ApplicationContext applicationContext,
                                JobExecutionRepository jobExecutionRepository) {
        this.scheduler = scheduler;
        this.applicationContext = applicationContext;
        this.jobExecutionRepository = jobExecutionRepository;
    }

    public Map<String, JobDefinition> discoverJobDefinitions() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(JobDefinition.class);
        Map<String, JobDefinition> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            if (entry.getValue() instanceof JobExecutable) {
                JobDefinition annotation = entry.getValue().getClass().getAnnotation(JobDefinition.class);
                result.put(entry.getKey(), annotation);
            }
        }
        return result;
    }

    public List<JobStatusResponse> listAllJobs() throws SchedulerException {
        Map<String, JobDefinition> definitions = discoverJobDefinitions();
        List<JobStatusResponse> responses = new ArrayList<>();
        for (Map.Entry<String, JobDefinition> entry : definitions.entrySet()) {
            JobDefinition definition = entry.getValue();
            JobKey key = JobKey.jobKey(definition.name(), definition.group());
            responses.add(buildStatusResponse(key, definition));
        }
        return responses;
    }

    public JobStatusResponse scheduleJob(String jobName, ScheduleRequest request) throws SchedulerException {
        JobDefinition definition = findDefinitionByJobName(jobName);
        String beanName = findBeanNameByJobName(jobName);
        JobKey key = JobKey.jobKey(definition.name(), definition.group());

        if (scheduler.checkExists(key)) {
            throw new IllegalStateException("Job '" + jobName + "' is already scheduled. Use PUT to update.");
        }

        JobDetail detail = buildJobDetail(key, beanName, request.getDescription());
        Trigger trigger = buildCronTrigger(key, request);
        scheduler.scheduleJob(detail, trigger);
        return buildStatusResponse(key, definition);
    }

    public JobStatusResponse rescheduleJob(String jobName, ScheduleRequest request) throws SchedulerException {
        JobDefinition definition = findDefinitionByJobName(jobName);
        JobKey key = JobKey.jobKey(definition.name(), definition.group());
        TriggerKey triggerKey = TriggerKey.triggerKey(key.getName(), key.getGroup());

        Trigger newTrigger = buildCronTrigger(key, request);
        scheduler.rescheduleJob(triggerKey, newTrigger);
        return buildStatusResponse(key, definition);
    }

    public void deleteJob(String jobName) throws SchedulerException {
        JobDefinition definition = findDefinitionByJobName(jobName);
        scheduler.deleteJob(JobKey.jobKey(definition.name(), definition.group()));
    }

    public void pauseJob(String jobName) throws SchedulerException {
        JobDefinition definition = findDefinitionByJobName(jobName);
        scheduler.pauseJob(JobKey.jobKey(definition.name(), definition.group()));
    }

    public void resumeJob(String jobName) throws SchedulerException {
        JobDefinition definition = findDefinitionByJobName(jobName);
        scheduler.resumeJob(JobKey.jobKey(definition.name(), definition.group()));
    }

    public void triggerNow(String jobName) throws SchedulerException {
        JobDefinition definition = findDefinitionByJobName(jobName);
        JobKey key = JobKey.jobKey(definition.name(), definition.group());

        if (!scheduler.checkExists(key)) {
            throw new IllegalStateException("Job '" + jobName + "' is not scheduled. Schedule it first.");
        }
        scheduler.triggerJob(key);
    }

    public List<JobExecution> getExecutionHistory(String jobName) {
        JobDefinition definition = findDefinitionByJobName(jobName);
        return jobExecutionRepository.findByJobNameAndJobGroupOrderByStartTimeDesc(
                definition.name(), definition.group());
    }

    private JobDetail buildJobDetail(JobKey key, String beanName, String description) {
        return JobBuilder.newJob(QuartzJobAdapter.class)
                .withIdentity(key)
                .withDescription(description)
                .usingJobData(QuartzJobAdapter.JOB_BEAN_NAME_KEY, beanName)
                .storeDurably()
                .build();
    }

    private Trigger buildCronTrigger(JobKey key, ScheduleRequest request) throws SchedulerException {
        TimeZone tz = TimeZone.getTimeZone(request.getTimeZone() != null ? request.getTimeZone() : "UTC");

        try {
            CronScheduleBuilder schedule = CronScheduleBuilder
                    .cronSchedule(request.getCronExpression())
                    .inTimeZone(tz)
                    .withMisfireHandlingInstructionDoNothing();

            return TriggerBuilder.newTrigger()
                    .withIdentity(key.getName(), key.getGroup())
                    .forJob(key)
                    .withSchedule(schedule)
                    .build();
        } catch (RuntimeException e) {
            throw new SchedulerException("Invalid cron expression: " + request.getCronExpression(), e);
        }
    }

    private JobStatusResponse buildStatusResponse(JobKey key, JobDefinition definition) throws SchedulerException {
        List<? extends Trigger> triggers = scheduler.getTriggersOfJob(key);

        String status = "UNSCHEDULED";
        String cronExpression = null;
        String timeZone = null;
        java.time.Instant nextFireTime = null;
        java.time.Instant previousFireTime = null;

        if (!triggers.isEmpty()) {
            Trigger trigger = triggers.get(0);
            var state = scheduler.getTriggerState(trigger.getKey());

            status = switch (state) {
                case PAUSED -> "PAUSED";
                case BLOCKED, NORMAL -> "SCHEDULED";
                case ERROR -> "ERROR";
                default -> "UNSCHEDULED";
            };

            if (trigger instanceof CronTrigger cron) {
                cronExpression = cron.getCronExpression();
                timeZone = cron.getTimeZone().getID();
            }
            if (trigger.getNextFireTime() != null) {
                nextFireTime = trigger.getNextFireTime().toInstant();
            }
            if (trigger.getPreviousFireTime() != null) {
                previousFireTime = trigger.getPreviousFireTime().toInstant();
            }
        }

        return JobStatusResponse.of(
                definition.name(), definition.group(), definition.description(),
                status, cronExpression, timeZone, nextFireTime, previousFireTime);
    }

    private JobDefinition findDefinitionByJobName(String jobName) {
        for (Map.Entry<String, JobDefinition> entry : discoverJobDefinitions().entrySet()) {
            if (entry.getValue().name().equals(jobName)) {
                return entry.getValue();
            }
        }
        throw new NoSuchElementException("No job found with name: " + jobName);
    }

    private String findBeanNameByJobName(String jobName) {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(JobDefinition.class);
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            if (entry.getValue() instanceof JobExecutable) {
                JobDefinition annotation = entry.getValue().getClass().getAnnotation(JobDefinition.class);
                if (annotation.name().equals(jobName)) {
                    return entry.getKey();
                }
            }
        }
        throw new NoSuchElementException("No bean found for job name: " + jobName);
    }
}

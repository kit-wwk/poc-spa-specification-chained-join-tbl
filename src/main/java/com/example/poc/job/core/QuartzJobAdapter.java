package com.example.poc.job.core;

import com.example.poc.job.entity.JobExecution;
import com.example.poc.job.entity.JobExecution.ExecutionStatus;
import com.example.poc.job.repository.JobExecutionRepository;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.time.Instant;

@DisallowConcurrentExecution
public class QuartzJobAdapter implements Job {

    public static final String JOB_BEAN_NAME_KEY = "jobBeanName";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private JobExecutionRepository jobExecutionRepository;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDetail detail = context.getJobDetail();
        String beanName = detail.getJobDataMap().getString(JOB_BEAN_NAME_KEY);
        String jobName = detail.getKey().getName();
        String jobGroup = detail.getKey().getGroup();

        JobExecution record = new JobExecution(jobName, jobGroup, Instant.now(), ExecutionStatus.RUNNING);
        record = jobExecutionRepository.save(record);

        try {
            JobExecutable executable = applicationContext.getBean(beanName, JobExecutable.class);
            executable.execute(context);
            record.setStatus(ExecutionStatus.SUCCESS);
        } catch (Exception e) {
            record.setStatus(ExecutionStatus.FAILED);
            record.setErrorMessage(truncate(e.getMessage(), 2000));
            throw new JobExecutionException(e);
        } finally {
            record.setEndTime(Instant.now());
            jobExecutionRepository.save(record);
        }
    }

    private String truncate(String message, int maxLength) {
        if (message == null) return null;
        return message.length() > maxLength ? message.substring(0, maxLength) : message;
    }
}

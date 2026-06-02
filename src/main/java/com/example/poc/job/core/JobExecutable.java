package com.example.poc.job.core;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public interface JobExecutable {
    void execute(JobExecutionContext context) throws JobExecutionException;
}

package com.example.poc.job.example;

import com.example.poc.job.annotation.JobDefinition;
import com.example.poc.job.core.JobExecutable;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JobDefinition(
    name = "printHello",
    group = "examples",
    description = "Logs a hello message — demo job for POC"
)
public class PrintHelloJob implements JobExecutable {

    private static final Logger log = LoggerFactory.getLogger(PrintHelloJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("Hello from PrintHelloJob! Fired at: {}", context.getFireTime());
    }
}

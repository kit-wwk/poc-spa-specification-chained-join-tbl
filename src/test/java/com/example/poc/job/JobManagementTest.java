package com.example.poc.job;

import com.example.poc.job.entity.JobExecution;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class JobManagementTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired Scheduler scheduler;

    private static final String JOB_NAME = "printHello";
    private static final String CRON_HOURLY = "0 0 * * * ?";
    private static final String TZ = "UTC";

    @AfterEach
    void cleanup() throws SchedulerException {
        JobKey key = JobKey.jobKey(JOB_NAME, "examples");
        if (scheduler.checkExists(key)) {
            scheduler.deleteJob(key);
        }
    }

    @Test
    void testListJobsReturnsDiscoveredJobs() throws Exception {
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.jobName == 'printHello')]").exists())
                .andExpect(jsonPath("$[?(@.jobName == 'printHello')].status", hasItem("UNSCHEDULED")));
    }

    @Test
    void testScheduleJobReturns201() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("cronExpression", CRON_HOURLY, "timeZone", TZ));

        mockMvc.perform(post("/api/jobs/{name}/schedule", JOB_NAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobName").value(JOB_NAME))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.cronExpression").value(CRON_HOURLY));
    }

    @Test
    void testScheduleSameJobTwiceReturns409() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("cronExpression", CRON_HOURLY, "timeZone", TZ));

        mockMvc.perform(post("/api/jobs/{name}/schedule", JOB_NAME)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/jobs/{name}/schedule", JOB_NAME)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void testUpdateScheduleChangesCronExpression() throws Exception {
        String initialBody = objectMapper.writeValueAsString(Map.of("cronExpression", CRON_HOURLY, "timeZone", TZ));
        mockMvc.perform(post("/api/jobs/{name}/schedule", JOB_NAME)
                        .contentType(MediaType.APPLICATION_JSON).content(initialBody))
                .andExpect(status().isCreated());

        String newCron = "0 0 12 * * ?";
        String updateBody = objectMapper.writeValueAsString(Map.of("cronExpression", newCron, "timeZone", TZ));
        mockMvc.perform(put("/api/jobs/{name}/schedule", JOB_NAME)
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cronExpression").value(newCron));
    }

    @Test
    void testPauseAndResumeJob() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("cronExpression", CRON_HOURLY, "timeZone", TZ));
        mockMvc.perform(post("/api/jobs/{name}/schedule", JOB_NAME)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/jobs/{name}/pause", JOB_NAME))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/jobs"))
                .andExpect(jsonPath("$[?(@.jobName == 'printHello')].status", hasItem("PAUSED")));

        mockMvc.perform(post("/api/jobs/{name}/resume", JOB_NAME))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/jobs"))
                .andExpect(jsonPath("$[?(@.jobName == 'printHello')].status", hasItem("SCHEDULED")));
    }

    @Test
    void testTriggerNowWritesExecutionHistory() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("cronExpression", CRON_HOURLY, "timeZone", TZ));
        mockMvc.perform(post("/api/jobs/{name}/schedule", JOB_NAME)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/jobs/{name}/trigger", JOB_NAME))
                .andExpect(status().isAccepted());

        // Quartz fires asynchronously — poll up to 3 seconds
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            var result = mockMvc.perform(get("/api/jobs/{name}/executions", JOB_NAME)).andReturn();
            var executions = objectMapper.readValue(result.getResponse().getContentAsString(), JobExecution[].class);
            if (executions.length > 0) break;
            Thread.sleep(200);
        }

        mockMvc.perform(get("/api/jobs/{name}/executions", JOB_NAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].status").value(anyOf(equalTo("SUCCESS"), equalTo("FAILED"))));
    }

    @Test
    void testDeleteJobRemovesFromScheduler() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("cronExpression", CRON_HOURLY, "timeZone", TZ));
        mockMvc.perform(post("/api/jobs/{name}/schedule", JOB_NAME)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/jobs/{name}", JOB_NAME))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/jobs"))
                .andExpect(jsonPath("$[?(@.jobName == 'printHello')].status", hasItem("UNSCHEDULED")));
    }

    @Test
    void testScheduleUnknownJobReturns404() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("cronExpression", CRON_HOURLY, "timeZone", TZ));
        mockMvc.perform(post("/api/jobs/{name}/schedule", "nonExistentJob")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void testScheduleWithInvalidCronReturns500() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("cronExpression", "NOT_A_CRON", "timeZone", TZ));
        mockMvc.perform(post("/api/jobs/{name}/schedule", JOB_NAME)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isInternalServerError());
    }
}

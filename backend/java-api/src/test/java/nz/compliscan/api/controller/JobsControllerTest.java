// src/test/java/nz/compliscan/api/controller/JobsControllerTest.java
package nz.compliscan.api.controller;

import nz.compliscan.api.repo.JobsRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Standalone MVC test against a mocked JobsRepo.
 * Now uses recentFor(owner, limit) and passes a Principal ("alice").
 */
class JobsControllerTest {

    private JobsRepo jobsRepo;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        jobsRepo = Mockito.mock(JobsRepo.class);
        mvc = MockMvcBuilders.standaloneSetup(new JobsController(jobsRepo)).build();
    }

    @Test
    void recent_defaultLimit_callsRepoAndReturnsArray() throws Exception {
        Mockito.when(jobsRepo.recentFor(eq("alice"), anyInt()))
                .thenReturn(java.util.List.of());

        mvc.perform(get("/jobs/recent")
                .principal(() -> "alice")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());

        Mockito.verify(jobsRepo).recentFor("alice", 20); // controller default
    }

    @Test
    void recent_clampsBelow1To1() throws Exception {
        Mockito.when(jobsRepo.recentFor(eq("alice"), anyInt()))
                .thenReturn(java.util.List.of());

        mvc.perform(get("/jobs/recent?limit=0")
                .principal(() -> "alice")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Mockito.verify(jobsRepo).recentFor("alice", 1);
    }

    @Test
    void recent_clampsAbove200To200() throws Exception {
        Mockito.when(jobsRepo.recentFor(eq("alice"), anyInt()))
                .thenReturn(java.util.List.of());

        mvc.perform(get("/jobs/recent?limit=999")
                .principal(() -> "alice")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Mockito.verify(jobsRepo).recentFor("alice", 200);
    }
}

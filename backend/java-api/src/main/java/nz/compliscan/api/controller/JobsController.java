package nz.compliscan.api.controller;

import nz.compliscan.api.model.JobItem;
import nz.compliscan.api.repo.JobsRepo;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(value = "/jobs", produces = MediaType.APPLICATION_JSON_VALUE)
public class JobsController {
  private final JobsRepo jobs;

  public JobsController(JobsRepo jobs) {
    this.jobs = jobs;
  }

  @PreAuthorize("hasAnyRole('ANALYST','ADMIN','UPLOADER','VIEWER')")
  @GetMapping("/{jobId}")
  public Object get(@PathVariable String jobId, Principal principal) {
    String owner = principal.getName();
    Optional<JobItem> j = jobs.get(jobId);
    if (j.isEmpty())
      return Map.of("error", "Not found", "jobId", jobId);
    if (!owner.equalsIgnoreCase(j.get().owner)) {
      // Hide others' jobs
      return Map.of("error", "Not found", "jobId", jobId);
    }
    return j.get();
  }

  public record RecentResponse(List<JobItem> items) {
  }

  @PreAuthorize("hasAnyRole('ANALYST','ADMIN','UPLOADER','VIEWER')")
  @GetMapping("/recent")
  public RecentResponse recent(@RequestParam(name = "limit", defaultValue = "20") int limit,
      Principal principal) {
    String owner = principal.getName();
    int n = Math.max(1, Math.min(limit, 200));
    return new RecentResponse(jobs.recentFor(owner, n));
  }
}

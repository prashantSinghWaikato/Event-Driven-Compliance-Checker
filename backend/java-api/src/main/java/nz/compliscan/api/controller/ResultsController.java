package nz.compliscan.api.controller;

import nz.compliscan.api.model.ResultItem;
import nz.compliscan.api.repo.JobsRepo;
import nz.compliscan.api.repo.ResultsRepo;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping(value = "/results", produces = MediaType.APPLICATION_JSON_VALUE)
public class ResultsController {

  public record Page(List<ResultItem> items, String lastKey) {
  }

  private final ResultsRepo results;
  private final JobsRepo jobs;

  public ResultsController(ResultsRepo results, JobsRepo jobs) {
    this.results = results;
    this.jobs = jobs;
  }

  @GetMapping("/{jobId}")
  public Page list(@PathVariable String jobId,
      @RequestParam(required = false) Integer limit,
      @RequestParam(required = false) String lastKey,
      Principal principal) {
    // Enforce ownership
    var job = jobs.get(jobId);
    if (job.isEmpty() || !principal.getName().equalsIgnoreCase(job.get().owner)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
    }

    var p = results.list(jobId, limit, lastKey);
    return new Page(p.items, p.lastKey);
  }
}

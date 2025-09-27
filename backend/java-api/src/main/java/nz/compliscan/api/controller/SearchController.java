// src/main/java/nz/compliscan/api/controller/SearchController.java
package nz.compliscan.api.controller;

import jakarta.validation.constraints.NotBlank;
import nz.compliscan.api.repo.JobsRepo;
import nz.compliscan.api.repo.ResultsRepo;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
public class SearchController {

    private final JobsRepo jobs;
    private final ResultsRepo results;

    public SearchController(JobsRepo jobs, ResultsRepo results) {
        this.jobs = jobs;
        this.results = results;
    }

    public record SearchReq(@NotBlank String name, String country) {
    }

    public record Match(String id, String name, String list, int riskScore, String country, String lastUpdated,
            String matchName) {
    }

    public record Resp(List<Match> matches, String jobId) {
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Resp search(@RequestBody SearchReq req, Authentication auth) {
        String name = req.name().trim();
        String country = req.country() == null ? "" : req.country().trim();
        String now = Instant.now().toString();

        // ---- stub matches (keep your earlier logic if you like)
        List<Match> matches = List.of(
                new Match("stub-1", (name + " HOLDINGS").toUpperCase(), "OFAC", 81, country, now,
                        (name + " HOLDINGS").toUpperCase()),
                new Match("stub-2", (name + " SERVICES").toUpperCase(), "PEP", 56, "AU", now,
                        (name + " SERVICES").toUpperCase()));

        // ---- persist as a DONE job so dashboard/recent jobs can see it
        String owner = auth != null ? String.valueOf(auth.getName()) : "anonymous";
        String jobId = UUID.randomUUID().toString();

        int total = matches.size();
        int high = 0, medium = 0, low = 0;
        int i = 0;
        for (var m : matches) {
            int score = m.riskScore();
            if (score >= 80)
                high++;
            else if (score >= 50)
                medium++;
            else
                low++;
            results.putOne(jobId, String.valueOf(++i),
                    m.name(), m.country(), m.matchName(), score, now, owner);
        }

        String summary = total == 0
                ? "Processed 0 records."
                : String.format(Locale.ROOT,
                        "Processed %d %s. High %d, Medium %d, Low %d.",
                        total, total == 1 ? "record" : "records", high, medium, low);

        jobs.putAdhocDone(jobId, owner, total, high, medium, low, summary);

        return new Resp(matches, jobId);
    }
}

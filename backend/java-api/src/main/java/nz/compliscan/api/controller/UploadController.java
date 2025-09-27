package nz.compliscan.api.controller;

import jakarta.validation.constraints.NotBlank;
import nz.compliscan.api.repo.JobsRepo;
import nz.compliscan.api.service.S3Service;
import nz.compliscan.api.service.SqsService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/uploads", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class UploadController {
    private final S3Service s3;
    private final SqsService sqs;
    private final JobsRepo jobs;

    public UploadController(S3Service s3, SqsService sqs, JobsRepo jobs) {
        this.s3 = s3;
        this.sqs = sqs;
        this.jobs = jobs;
    }

    @PreAuthorize("hasAnyRole('UPLOADER','ADMIN')")
    @GetMapping("/presign")
    public S3Service.Presign presign(@RequestParam("filename") String filename, Principal principal) {
        // Optionally prefix with owner for nicer S3 hygiene:
        // final String owner = principal != null ? principal.getName() : "anon";
        // return s3.presign(owner + "/" + filename);
        return s3.presign(filename);
    }

    public record ConfirmBody(@NotBlank String key, String country) {
    }

    @PreAuthorize("hasAnyRole('UPLOADER','ADMIN')")
    @PostMapping("/confirm")
    public Map<String, String> confirm(@RequestBody ConfirmBody body, Principal principal) {
        String jobId = UUID.randomUUID().toString();
        String owner = principal != null ? principal.getName() : "anon";

        // Create job record with OWNER
        jobs.putQueued(jobId, owner);

        // Send message to SQS (worker must propagate owner into results)
        var msg = Map.of(
                "jobId", jobId,
                "owner", owner,
                "bucket", s3.bucket(),
                "key", body.key(),
                "country", body.country() == null ? "" : body.country(),
                "enqueuedAt", Instant.now().toString());
        String json = com.fasterxml.jackson.databind.json.JsonMapper.builder().build().valueToTree(msg).toString();
        sqs.send(json);

        return Map.of("jobId", jobId);
    }
}

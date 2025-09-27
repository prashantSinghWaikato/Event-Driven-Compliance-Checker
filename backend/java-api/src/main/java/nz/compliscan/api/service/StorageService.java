package nz.compliscan.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Service
public class StorageService {
  private final S3Presigner presigner;
  private final String bucket;
  private final int minutes;

  public record Presign(String key, URL url) {}

  public StorageService(S3Presigner presigner,
                        @Value("${app.aws.s3Bucket}") String bucket,
                        @Value("${app.aws.presignMinutes}") int minutes) {
    this.presigner = presigner; this.bucket = bucket; this.minutes = minutes;
  }

  public Presign presignCsvPut(String filename) {
    String key = "uploads/%s/%s".formatted(UUID.randomUUID(), filename);
    PutObjectRequest req = PutObjectRequest.builder()
        .bucket(bucket).key(key).contentType("text/csv").build();
    PresignedPutObjectRequest pres = presigner.presignPutObject(b -> b
        .signatureDuration(Duration.ofMinutes(minutes))
        .putObjectRequest(req));
    return new Presign(key, pres.url());
  }
}
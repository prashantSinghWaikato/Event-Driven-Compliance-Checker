package nz.compliscan.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class S3Service {
    private final S3Presigner presigner;
    private final String bucket;
    private final int minutes;

    public S3Service(S3Presigner presigner,
            @Value("${app.aws.s3Bucket}") String bucket,
            @Value("${app.aws.presignMinutes}") int minutes) {
        this.presigner = presigner;
        this.bucket = bucket;
        this.minutes = minutes;
    }

    public record Presign(String url, String key, Map<String, String> headers) {
    }

    public Presign presign(String filename) {
        String key = "uploads/" + UUID.randomUUID() + "/" + filename;
        PutObjectRequest por = PutObjectRequest.builder()
                .bucket(bucket).key(key)
                .build();

        PresignedPutObjectRequest req = presigner.presignPutObject(p -> p
                .signatureDuration(Duration.ofMinutes(minutes))
                .putObjectRequest(por));

        URL url = req.url();
        Map<String, String> hdrs = req.signedHeaders()
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> String.join(",", e.getValue())));

        return new Presign(url.toString(), key, hdrs);
    }

    public String bucket() {
        return bucket;
    }
}

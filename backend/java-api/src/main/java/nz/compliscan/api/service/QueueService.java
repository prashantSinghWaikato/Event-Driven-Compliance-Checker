package nz.compliscan.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class QueueService {
  private final SqsClient sqs;
  private final ObjectMapper om = new ObjectMapper();
  private final String queueUrl;

  public QueueService(SqsClient sqs, @Value("${app.aws.sqsQueueUrl:}") String queueUrl) {
    this.sqs = sqs; this.queueUrl = queueUrl;
  }

  public String enqueueCsvJob(String bucket, String key, String country) {
    String jobId = UUID.randomUUID().toString();
    if (queueUrl != null && !queueUrl.isBlank()) {
      try {
        Map<String,Object> body = new HashMap<>();
        body.put("jobId", jobId); body.put("bucket", bucket); body.put("key", key); body.put("country", country);
        String json = om.writeValueAsString(body);
        SendMessageRequest req = SendMessageRequest.builder().queueUrl(queueUrl).messageBody(json).build();
        sqs.sendMessage(req);
      } catch (Exception e) {
        throw new RuntimeException("Failed to enqueue SQS message", e);
      }
    }
    return jobId;
  }
}
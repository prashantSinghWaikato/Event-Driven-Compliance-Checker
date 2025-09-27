package nz.compliscan.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
public class SqsService {
    private final SqsClient sqs;
    private final String queueUrl;

    public SqsService(SqsClient sqs, @Value("${app.aws.sqsQueueUrl:}") String queueUrl) {
        this.sqs = sqs;
        this.queueUrl = queueUrl;
    }

    public void send(String body) {
        if (queueUrl == null || queueUrl.isBlank()) {
            throw new IllegalStateException("SQS_QUEUE_URL is not configured");
        }
        sqs.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(body)
                .build());
    }
}

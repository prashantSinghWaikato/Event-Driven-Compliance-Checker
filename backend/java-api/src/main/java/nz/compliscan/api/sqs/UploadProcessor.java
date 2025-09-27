// src/main/java/nz/compliscan/api/sqs/UploadProcessor.java
package nz.compliscan.api.sqs;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Background SQS consumer.
 *
 * - Reads SQS_QUEUE_URL from environment.
 * - Long-polls the queue and processes messages in a single-thread loop.
 * - Deletes messages after successful processing.
 *
 * Drop in as-is. Later, add your business logic in processMessage().
 */
@Component
public class UploadProcessor {

    private static final Logger log = LoggerFactory.getLogger(UploadProcessor.class);

    // --------- Config (env-driven) ----------
    private final String queueUrl = System.getenv("SQS_QUEUE_URL"); // required
    private final int maxMessages = getIntEnv("SQS_MAX_MESSAGES", 10); // batch size
    private final int waitTimeSec = getIntEnv("SQS_WAIT_TIME_SEC", 20); // long-poll
    private final int visibilityTimeoutSec = getIntEnv("SQS_VISIBILITY_TIMEOUT_SEC", 60);

    // --------- Runtime ----------
    private ExecutorService executor;
    private volatile boolean running = false;
    private SqsClient sqs;

    @PostConstruct
    void start() {
        if (queueUrl == null || queueUrl.isBlank()) {
            log.warn("UploadProcessor disabled: SQS_QUEUE_URL is not set.");
            return;
        }

        try {
            AwsRegionProvider regionProvider = new DefaultAwsRegionProviderChain();
            Region region = regionProvider.getRegion();

            this.sqs = SqsClient.builder()
                    .region(region)
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();

            this.executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "UploadProcessor-SQS");
                t.setDaemon(true);
                return t;
            });

            this.running = true;
            this.executor.submit(this::runLoop);

            log.info("UploadProcessor started. Queue: {}  Region: {}", queueUrl, region.id());
        } catch (Exception e) {
            log.error("Failed to start UploadProcessor", e);
            stop();
        }
    }

    @PreDestroy
    void stop() {
        running = false;
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        if (sqs != null) {
            try {
                sqs.close();
            } catch (Exception ignored) {
            }
            sqs = null;
        }
        log.info("UploadProcessor stopped.");
    }

    private void runLoop() {
        // simple backoff if the queue is empty or on transient errors
        long idleBackoffMs = 0L;

        while (running && sqs != null) {
            try {
                ReceiveMessageRequest req = ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .waitTimeSeconds(waitTimeSec) // long polling
                        .maxNumberOfMessages(maxMessages) // up to 10
                        .visibilityTimeout(visibilityTimeoutSec)
                        .build();

                List<Message> msgs = sqs.receiveMessage(req).messages();

                if (msgs == null || msgs.isEmpty()) {
                    // nothing received; small backoff
                    idleBackoffMs = Math.min((idleBackoffMs == 0 ? 250L : idleBackoffMs * 2), 5000L);
                    sleep(idleBackoffMs);
                    continue;
                } else {
                    idleBackoffMs = 0L; // reset backoff
                }

                for (Message m : msgs) {
                    String body = Objects.toString(m.body(), "");
                    String receipt = m.receiptHandle();

                    boolean ok = false;
                    try {
                        ok = processMessage(body);
                    } catch (Exception ex) {
                        log.error("Error processing message: {}", body, ex);
                    }

                    if (ok) {
                        deleteMessage(receipt);
                    } else {
                        // Leave it in-flight; it will reappear after visibility timeout for retry.
                        log.warn("Processing returned false; message will become visible again.");
                    }
                }
            } catch (Exception e) {
                log.error("SQS polling error", e);
                // transient error; pause briefly
                sleep(1000L);
            }
        }
    }

    private boolean processMessage(String body) {
        // TODO: Replace this with real business logic.
        // Example: parse JSON and call your screening service(s).
        // - parse uploadId, items, etc.
        // - call OFAC/PEP checks
        // - persist results / update JobsRepo
        log.info("Received message body: {}", body);

        // If everything is OK, return true so we delete the message.
        return true;
    }

    private void deleteMessage(String receiptHandle) {
        try {
            sqs.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(receiptHandle)
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete SQS message", e);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static int getIntEnv(String key, int def) {
        try {
            String v = System.getenv(key);
            return (v == null || v.isBlank()) ? def : Integer.parseInt(v.trim());
        } catch (Exception ignored) {
            return def;
        }
    }
}

package nz.compliscan.api.controller;

import nz.compliscan.api.repo.JobsRepo;
import nz.compliscan.api.service.S3Service;
import nz.compliscan.api.service.SqsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UploadControllerTest {

        @Test
        void confirm_enqueuesSqsMessage_andReturnsJobId() {
                // Mocks
                var s3 = Mockito.mock(S3Service.class);
                var sqs = Mockito.mock(SqsService.class);
                var jobs = Mockito.mock(JobsRepo.class);

                when(s3.bucket()).thenReturn("bucket-1");

                // System under test
                var ctl = new UploadController(s3, sqs, jobs);

                // Caller identity (controller method now requires Principal)
                Principal principal = () -> "alice";

                // Act
                var resp = ctl.confirm(new UploadController.ConfirmBody("uploads/file.csv", "NZ"), principal);

                // Assert response contains a jobId
                assertThat(resp).containsKey("jobId");
                String jobId = resp.get("jobId");
                assertThat(jobId).isNotBlank();

                // Assert SQS message was sent and includes key fields
                var msgCap = ArgumentCaptor.forClass(String.class);
                verify(sqs, times(1)).send(msgCap.capture());

                String json = msgCap.getValue();
                assertThat(json).contains("\"jobId\":\"" + jobId + "\"");
                assertThat(json).contains("\"bucket\":\"bucket-1\"");
                assertThat(json).contains("\"key\":\"uploads/file.csv\"");
                assertThat(json).contains("\"country\":\"NZ\"");

                // We intentionally do NOT verify jobs.putQueued(...) because its signature
                // (one vs two args) differs between branches.
                verifyNoMoreInteractions(sqs);
        }
}

package nz.compliscan.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AwsConfig {

  @Bean
  public Region region(@Value("${app.aws.region}") String region) {
    return Region.of(region);
  }

  @Bean
  public S3Client s3(Region region) {
    return S3Client.builder().region(region).build();
  }

  @Bean
  public S3Presigner s3Presigner(Region region) {
    return S3Presigner.builder().region(region).build();
  }

  @Bean
  public SqsClient sqs(Region region) {
    return SqsClient.builder().region(region).build();
  }

  @Bean
  public DynamoDbClient dynamo(Region region) {
    return DynamoDbClient.builder().region(region).build();
  }
}

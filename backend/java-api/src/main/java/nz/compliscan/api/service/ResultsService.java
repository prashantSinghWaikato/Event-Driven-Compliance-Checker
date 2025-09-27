package nz.compliscan.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.*;

@Service
public class ResultsService {
  private final DynamoDbClient ddb;
  private final String table;

  public ResultsService(DynamoDbClient ddb, @Value("${app.aws.ddbTable}") String table) {
    this.ddb = ddb; this.table = table;
  }

  public Page queryByJob(String jobId, Integer limit, Map<String, AttributeValue> startKey) {
    QueryRequest.Builder qb = QueryRequest.builder()
        .tableName(table)
        .keyConditionExpression("jobId = :jid")
        .expressionAttributeValues(Map.of(":jid", AttributeValue.builder().s(jobId).build()))
        .scanIndexForward(true);
    if (limit != null && limit > 0) qb = qb.limit(limit);
    if (startKey != null && !startKey.isEmpty()) qb = qb.exclusiveStartKey(startKey);
    QueryResponse resp = ddb.query(qb.build());
    return new Page(resp.items(), resp.lastEvaluatedKey());
  }

  public static class Page {
    public final List<Map<String, AttributeValue>> items;
    public final Map<String, AttributeValue> lastKey;
    public Page(List<Map<String, AttributeValue>> items, Map<String, AttributeValue> lastKey) {
      this.items = items; this.lastKey = lastKey;
    }
  }
}
package nz.compliscan.api.controller;

import nz.compliscan.api.repo.JobsRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/results")
public class ResultsCsvController {

  private final DynamoDbClient ddb;
  private final String table;
  private final JobsRepo jobs;

  public ResultsCsvController(DynamoDbClient ddb,
      @Value("${app.aws.ddbTable}") String table,
      JobsRepo jobs) {
    this.ddb = ddb;
    this.table = table;
    this.jobs = jobs;
  }

  @GetMapping(value = "/{jobId}/csv", produces = "text/csv")
  public ResponseEntity<StreamingResponseBody> downloadCsv(@PathVariable String jobId, Principal principal) {
    // Enforce ownership before streaming
    var j = jobs.get(jobId);
    if (j.isEmpty() || !principal.getName().equalsIgnoreCase(j.get().owner)) {
      return ResponseEntity.status(404).build();
    }

    String filename = "results-" + jobId + ".csv";
    String cd = String.format(
        "attachment; filename=\"%s\"; filename*=UTF-8''%s",
        safe(filename), url(filename));

    StreamingResponseBody body = (OutputStream out) -> {
      out.write(("recordId,name,country,matchName,riskScore,processedAt\n").getBytes(StandardCharsets.UTF_8));

      var keyCond = "jobId = :jid";
      var exprVals = Map.of(":jid", AttributeValue.builder().s(jobId).build());

      QueryRequest req = QueryRequest.builder()
          .tableName(table)
          .keyConditionExpression(keyCond)
          .expressionAttributeValues(exprVals)
          .scanIndexForward(true)
          .build();

      QueryResponse resp;
      do {
        resp = ddb.query(req);
        for (var item : resp.items()) {
          String recordId = s(item, "recordId");
          String name = s(item, "name");
          String country = s(item, "country");
          String matchName = s(item, "matchName");
          String riskScore = s(item, "riskScore");
          String processed = s(item, "processedAt");
          String line = String.join(",",
              csv(recordId), csv(name), csv(country), csv(matchName), csv(riskScore), csv(processed)) + "\n";
          out.write(line.getBytes(StandardCharsets.UTF_8));
        }
        if (resp.lastEvaluatedKey() != null && !resp.lastEvaluatedKey().isEmpty()) {
          req = req.toBuilder().exclusiveStartKey(resp.lastEvaluatedKey()).build();
        } else {
          break;
        }
      } while (true);
    };

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, cd)
        .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
        .body(body);
  }

  private static String s(Map<String, AttributeValue> item, String key) {
    var v = item.get(key);
    if (v == null)
      return "";
    if (v.s() != null)
      return v.s();
    if (v.n() != null)
      return v.n();
    return "";
  }

  private static String csv(String v) {
    if (v == null)
      v = "";
    boolean needQuote = v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r");
    String escaped = v.replace("\"", "\"\"");
    return needQuote ? "\"" + escaped + "\"" : escaped;
  }

  private static String safe(String v) {
    if (v == null)
      return "";
    return v.replaceAll("[\\r\\n\"]", "_");
  }

  private static String url(String v) {
    return URLEncoder.encode(v, StandardCharsets.UTF_8).replace("+", "%20");
  }
}

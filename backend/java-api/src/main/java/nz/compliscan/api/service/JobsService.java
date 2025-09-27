package nz.compliscan.api.service;

import nz.compliscan.api.dto.JobItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.*;

@Service
public class JobsService {
  private final DynamoDbClient ddb;
  private final String table;
  private final Integer ttlDays;

  public JobsService(DynamoDbClient ddb, @Value("${app.aws.jobsTable}") String table,
                     @Value("${TTL_DAYS:0}") Integer ttlDays) {
    this.ddb = ddb; this.table = table; this.ttlDays = ttlDays==null?0:ttlDays;
  }

  public void putQueued(String jobId) {
    String now = Instant.now().toString();
    Map<String, AttributeValue> item = new HashMap<>();
    item.put("jobId", AttributeValue.builder().s(jobId).build());
    item.put("status", AttributeValue.builder().s("QUEUED").build());
    item.put("createdAt", AttributeValue.builder().s(now).build());
    item.put("updatedAt", AttributeValue.builder().s(now).build());
    if (ttlDays > 0) {
      long ttl = Instant.now().plusSeconds(ttlDays * 86400L).getEpochSecond();
      item.put("ttl", AttributeValue.builder().n(Long.toString(ttl)).build());
    }
    ddb.putItem(PutItemRequest.builder().tableName(table).item(item).build());
  }

  public JobItem get(String jobId) {
    var resp = ddb.getItem(GetItemRequest.builder()
        .tableName(table)
        .key(Map.of("jobId", AttributeValue.builder().s(jobId).build()))
        .build());
    var m = resp.item();
    if (m == null || m.isEmpty()) return new JobItem(jobId, "UNKNOWN", null, null, null);
    JobItem j = new JobItem(s(m,"jobId"), s(m,"status"), s(m,"createdAt"), s(m,"updatedAt"), s(m,"error"));
    if (m.containsKey("summary")) {
      var sm = m.get("summary").m();
      JobItem.Summary sum = new JobItem.Summary();
      sum.total = i(sm,"total"); sum.high = i(sm,"high"); sum.medium = i(sm,"medium"); sum.low = i(sm,"low");
      sum.truncated = b(sm,"truncated");
      j.summary = sum;
    }
    return j;
  }

  public Map<String,Object> recent(Integer limit, Map<String, AttributeValue> startKey) {
    ScanRequest.Builder sb = ScanRequest.builder().tableName(table);
    if (limit != null && limit > 0) sb = sb.limit(limit);
    if (startKey != null && !startKey.isEmpty()) sb = sb.exclusiveStartKey(startKey);
    ScanResponse resp = ddb.scan(sb.build());
    List<Map<String,Object>> items = new ArrayList<>();
    for (var m : resp.items()) {
      Map<String,Object> o = new LinkedHashMap<>();
      o.put("jobId", s(m,"jobId"));
      o.put("status", s(m,"status"));
      o.put("createdAt", s(m,"createdAt"));
      o.put("updatedAt", s(m,"updatedAt"));
      if (m.containsKey("summary")) {
        var sm = m.get("summary").m();
        Map<String,Object> sum = new LinkedHashMap<>();
        sum.put("total", i(sm,"total")); sum.put("high", i(sm,"high")); sum.put("medium", i(sm,"medium")); sum.put("low", i(sm,"low"));
        if (sm.containsKey("truncated")) sum.put("truncated", sm.get("truncated").bool());
        o.put("summary", sum);
      }
      items.add(o);
    }
    // sort by updatedAt desc for UX
    items.sort((a,b)->String.valueOf(b.get("updatedAt")).compareTo(String.valueOf(a.get("updatedAt"))));
    Map<String, AttributeValue> lek = resp.lastEvaluatedKey();
    return Map.of("items", items, "lastKey", (lek==null || lek.isEmpty()) ? null : "lek"); // placeholder
  }

  private static String s(Map<String, AttributeValue> m, String k) { var v = m.get(k); if (v==null) return null; return v.s()!=null?v.s():v.n(); }
  private static Integer i(Map<String, AttributeValue> m, String k) {
    var v = m.get(k); if (v==null) return null; if (v.n()!=null) return Integer.parseInt(v.n()); if (v.s()!=null) try { return Integer.parseInt(v.s()); } catch(Exception ignore){} return null;
  }
  private static Boolean b(Map<String, AttributeValue> m, String k) { var v=m.get(k); return v!=null && v.bool()!=null ? v.bool() : null; }
}
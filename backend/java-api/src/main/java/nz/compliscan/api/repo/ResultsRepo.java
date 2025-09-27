package nz.compliscan.api.repo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import nz.compliscan.api.model.ResultItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64;

@Repository
public class ResultsRepo {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DynamoDbClient ddb;
    private final String table;

    public ResultsRepo(DynamoDbClient ddb, @Value("${app.aws.ddbTable}") String table) {
        this.ddb = ddb;
        this.table = table;
    }

    public static class Page {
        public List<ResultItem> items;
        public String lastKey; // opaque cursor
    }

    public Page list(String jobId, Integer limit, String lastKey) {
        QueryRequest.Builder qb = QueryRequest.builder()
                .tableName(table)
                .keyConditionExpression("jobId = :j")
                .expressionAttributeValues(Map.of(":j", AttributeValue.builder().s(jobId).build()))
                .scanIndexForward(true);

        if (limit != null && limit > 0)
            qb.limit(limit);
        if (lastKey != null && !lastKey.isBlank())
            qb.exclusiveStartKey(decodeKey(lastKey));

        QueryResponse qr = ddb.query(qb.build());
        Page p = new Page();
        p.items = new ArrayList<>();
        for (var m : qr.items())
            p.items.add(from(m));
        if (qr.hasLastEvaluatedKey() && !qr.lastEvaluatedKey().isEmpty()) {
            p.lastKey = encodeKey(qr.lastEvaluatedKey());
        }
        return p;
    }

    private static ResultItem from(Map<String, AttributeValue> m) {
        ResultItem r = new ResultItem();
        r.jobId = s(m, "jobId");
        r.recordId = s(m, "recordId");
        r.name = s(m, "name");
        r.country = s(m, "country");
        r.matchName = s(m, "matchName");
        r.riskScore = n(m, "riskScore");
        r.processedAt = s(m, "processedAt");
        return r;
    }

    private static String s(Map<String, AttributeValue> m, String k) {
        var v = m.get(k);
        return v == null ? null : v.s();
    }

    private static Integer n(Map<String, AttributeValue> m, String k) {
        var v = m.get(k);
        return (v == null || v.n() == null) ? null : Integer.valueOf(v.n());
    }

    // ----- cursor helpers (JSON -> base64url) -----
    private static String encodeKey(Map<String, AttributeValue> key) {
        try {
            // Reduce AttributeValue to { "K": {"S":"..."} } or {"N":"..."}
            Map<String, Map<String, String>> out = new LinkedHashMap<>();
            key.forEach((k, v) -> {
                Map<String, String> val = new LinkedHashMap<>();
                if (v.s() != null)
                    val.put("S", v.s());
                else if (v.n() != null)
                    val.put("N", v.n());
                out.put(k, val);
            });
            String json = MAPPER.writeValueAsString(out);
            return Base64.getUrlEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode cursor", e);
        }
    }

    private static Map<String, AttributeValue> decodeKey(String cursor) {
        try {
            String json = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            Map<String, Map<String, String>> raw = MAPPER.readValue(json,
                    new TypeReference<Map<String, Map<String, String>>>() {
                    });
            Map<String, AttributeValue> key = new LinkedHashMap<>();
            raw.forEach((k, v) -> {
                if (v.containsKey("S"))
                    key.put(k, AttributeValue.builder().s(v.get("S")).build());
                else if (v.containsKey("N"))
                    key.put(k, AttributeValue.builder().n(v.get("N")).build());
            });
            return key;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cursor", e);
        }
    }
}

package nz.compliscan.api.repo;

import nz.compliscan.api.model.JobItem;
import nz.compliscan.api.model.JobStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class JobsRepo {

    private final DynamoDbClient ddb;
    private final String table;

    public JobsRepo(DynamoDbClient ddb, @Value("${app.aws.jobsTable}") String table) {
        this.ddb = ddb;
        this.table = table;
    }

    private static Map<String, AttributeValue> key(String jobId) {
        return Map.of("jobId", AttributeValue.builder().s(jobId).build());
    }

    /** Create a new QUEUED job for a specific owner (username). */
    public void putQueued(String jobId, String owner) {
        String now = Instant.now().toString();
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("jobId", AttributeValue.builder().s(jobId).build());
        item.put("owner", AttributeValue.builder().s(owner).build());
        item.put("status", AttributeValue.builder().s(JobStatus.QUEUED.name()).build());
        item.put("createdAt", AttributeValue.builder().s(now).build());
        item.put("updatedAt", AttributeValue.builder().s(now).build());

        // Generic GSI to query "recent jobs for owner"
        // Single index "gsi1": PK=gsi1pk, SK=gsi1sk
        item.put("gsi1pk", AttributeValue.builder().s("OWNER#" + owner).build());
        item.put("gsi1sk", AttributeValue.builder().s(now).build());

        ddb.putItem(PutItemRequest.builder()
                .tableName(table)
                .item(item)
                .conditionExpression("attribute_not_exists(jobId)")
                .build());
    }

    public void updateStatus(String jobId, JobStatus status, String error, Map<String, Integer> summary) {
        String now = Instant.now().toString();
        StringBuilder expr = new StringBuilder("SET #s = :s, updatedAt = :u, gsi1sk = :u");
        Map<String, String> names = new HashMap<>(Map.of("#s", "status"));
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":s", AttributeValue.builder().s(status.name()).build());
        values.put(":u", AttributeValue.builder().s(now).build());
        if (error != null) {
            expr.append(", #e = :e");
            names.put("#e", "error");
            values.put(":e", AttributeValue.builder().s(error).build());
        }
        if (summary != null) {
            if (summary.containsKey("total")) {
                expr.append(", total = :t");
                values.put(":t", AttributeValue.builder().n(String.valueOf(summary.get("total"))).build());
            }
            if (summary.containsKey("high")) {
                expr.append(", high = :h");
                values.put(":h", AttributeValue.builder().n(String.valueOf(summary.get("high"))).build());
            }
            if (summary.containsKey("medium")) {
                expr.append(", medium = :m");
                values.put(":m", AttributeValue.builder().n(String.valueOf(summary.get("medium"))).build());
            }
            if (summary.containsKey("low")) {
                expr.append(", low = :l");
                values.put(":l", AttributeValue.builder().n(String.valueOf(summary.get("low"))).build());
            }
        }

        ddb.updateItem(UpdateItemRequest.builder()
                .tableName(table)
                .key(key(jobId))
                .updateExpression(expr.toString())
                .expressionAttributeNames(names)
                .expressionAttributeValues(values)
                .build());
    }

    public Optional<JobItem> get(String jobId) {
        var resp = ddb.getItem(GetItemRequest.builder()
                .tableName(table)
                .key(key(jobId))
                .build());
        if (!resp.hasItem())
            return Optional.empty();
        return Optional.of(from(resp.item()));
    }

    /** Recent jobs for a specific owner (fast via GSI, scan fallback otherwise). */
    public List<JobItem> recentFor(String owner, int limit) {
        // Try the GSI first
        try {
            var q = QueryRequest.builder()
                    .tableName(table)
                    .indexName("gsi1") // GSI with PK=gsi1pk, SK=gsi1sk
                    .keyConditionExpression("gsi1pk = :p")
                    .expressionAttributeValues(Map.of(
                            ":p", AttributeValue.builder().s("OWNER#" + owner).build()))
                    .scanIndexForward(false) // newest first
                    .limit(limit)
                    .build();
            var resp = ddb.query(q);
            return resp.items().stream().map(JobsRepo::from).collect(Collectors.toList());
        } catch (ResourceNotFoundException noIndex) {
            // Fallback: scan + filter (OK for demo/small data)
            var resp = ddb.scan(ScanRequest.builder().tableName(table).build());
            return resp.items().stream()
                    .map(JobsRepo::from)
                    .filter(j -> owner.equalsIgnoreCase(j.owner))
                    .sorted(Comparator.comparing(
                            (JobItem j) -> j.updatedAt == null ? "" : j.updatedAt).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());
        }
    }

    private static JobItem from(Map<String, AttributeValue> m) {
        JobItem j = new JobItem();
        j.jobId = s(m, "jobId");
        j.owner = s(m, "owner");
        var st = s(m, "status");
        j.status = st == null ? JobStatus.QUEUED : JobStatus.valueOf(st);
        j.createdAt = s(m, "createdAt");
        j.updatedAt = s(m, "updatedAt");
        j.error = s(m, "error");
        j.total = n(m, "total");
        j.high = n(m, "high");
        j.medium = n(m, "medium");
        j.low = n(m, "low");
        return j;
    }

    private static String s(Map<String, AttributeValue> m, String k) {
        var v = m.get(k);
        return v == null ? null : v.s();
    }

    private static Integer n(Map<String, AttributeValue> m, String k) {
        var v = m.get(k);
        return (v == null || v.n() == null) ? null : Integer.valueOf(v.n());
    }
}

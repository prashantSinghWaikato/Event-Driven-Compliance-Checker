package nz.compliscan.api.model;

public class ResultItem {
    public String jobId; // PK
    public String recordId; // SK
    public String name;
    public String country;
    public String matchName;
    public Integer riskScore;
    public String processedAt;
}

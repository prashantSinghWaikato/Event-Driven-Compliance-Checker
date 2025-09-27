package nz.compliscan.api.dto;
public class JobItem {
  public String jobId; public String status; public String createdAt; public String updatedAt; public String error;
  public Summary summary;
  public static class Summary { public Integer total; public Integer high; public Integer medium; public Integer low; public Boolean truncated; }
  public JobItem() {}
  public JobItem(String jobId, String status, String createdAt, String updatedAt, String error) {
    this.jobId = jobId; this.status = status; this.createdAt = createdAt; this.updatedAt = updatedAt; this.error = error;
  }
}
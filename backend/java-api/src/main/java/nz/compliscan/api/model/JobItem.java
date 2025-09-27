package nz.compliscan.api.model;

public class JobItem {
    public String jobId;
    public String owner; // <-- NEW: username of creator
    public JobStatus status;
    public String createdAt;
    public String updatedAt;
    public String error;
    // Optional summary fields (set by Lambda)
    public Integer total;
    public Integer high;
    public Integer medium;
    public Integer low;

    public static JobItem of(String id, JobStatus st, String ts) {
        JobItem j = new JobItem();
        j.jobId = id;
        j.status = st;
        j.createdAt = ts;
        j.updatedAt = ts;
        return j;
    }
}

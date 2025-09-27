package nz.compliscan.api.security;

public enum Role {
    ADMIN, // manage users, everything
    ANALYST, // view jobs/results
    UPLOADER, // upload CSV / start jobs
    VIEWER // read-only dashboards
}

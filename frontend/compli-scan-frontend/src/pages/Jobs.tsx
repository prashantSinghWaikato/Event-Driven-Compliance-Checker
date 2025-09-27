import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";

type Job = {
  jobId: string;
  status: "QUEUED" | "PROCESSING" | "DONE" | "FAILED" | string;
  updatedAt?: string;
  createdAt?: string;
  // optional summary fields
  total?: number;
  high?: number;
  medium?: number;
  low?: number;
  error?: string | null;
};

type RecentJobsResponse = {
  items: Job[];
  lastKey?: string;
};

export default function Jobs() {
  const [rows, setRows] = useState<Job[]>([]);
  const [lastKey, setLastKey] = useState<string | undefined>(undefined);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string>("");

  async function load(more = false) {
    try {
      setLoading(true);
      setErr("");
      const r = (await api.getRecentJobs(20)) as RecentJobsResponse;
      setRows((prev) => (more ? [...prev, ...(r.items || [])] : r.items || []));
      setLastKey(r.lastKey);
    } catch (e: any) {
      setErr(e?.message || "Failed to load jobs");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load(false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div>
      <div className="card">
        <div className="card-head">
          <h3>Recent Jobs</h3>
          <button className="btn" onClick={() => load(false)} disabled={loading}>
            Refresh
          </button>
        </div>

        {err && <div className="error">⚠ {err}</div>}

        {!err && rows.length === 0 && !loading && (
          <div className="muted" style={{ paddingTop: 8 }}>
            No recent jobs yet.
          </div>
        )}

        <table className="table" style={{ marginTop: 8 }}>
          <thead>
            <tr>
              <th>Job</th>
              <th>Status</th>
              <th>Updated</th>
              <th>Summary</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {rows.map((j) => {
              const updated = j.updatedAt || j.createdAt;
              const summary =
                j.total != null || j.high != null || j.medium != null || j.low != null
                  ? `Total ${j.total ?? 0} • H:${j.high ?? 0} M:${j.medium ?? 0} L:${j.low ?? 0}`
                  : "—";
              return (
                <tr key={j.jobId}>
                  <td className="nowrap break-on-small">
                    {/* full id, monospace, tooltip, colored link */}

                    <code className="mono">{j.jobId}</code>

                  </td>
                  <td>
                    <span className={`badge s-${(j.status || "").toLowerCase()}`}>
                      {j.status}
                    </span>
                  </td>
                  <td>{updated ? new Date(updated).toLocaleString() : "—"}</td>
                  <td>{summary}</td>
                  <td>
                    <Link className="link-view" to={`/app/results/${j.jobId}`}>
                      View
                    </Link>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>

        <div style={{ marginTop: 10, display: "flex", gap: 8 }}>
          <button className="btn" onClick={() => load(true)} disabled={!lastKey || loading}>
            Load more
          </button>
          {loading && <span className="muted">Loading…</span>}
        </div>
      </div>
    </div>
  );
}

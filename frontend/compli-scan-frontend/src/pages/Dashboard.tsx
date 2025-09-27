// src/pages/Dashboard.tsx
import React, { useEffect, useMemo, useState } from "react";
import { useAuth } from "../auth";

const API_BASE = import.meta.env.VITE_API_BASE ?? "/api"; // via Vite proxy -> backend root

type Job = {
  id: string;
  name?: string;
  status: string;
  risk?: string;           // e.g., "High"|"Medium"|"Low"
  riskLevel?: string;      // alternate backend field
  risk_score?: number;     // numeric 0..1
  summary?: string;
  createdAt?: string;
};

type Summary = {
  processedSum: number;
  jobsToday: number;
  inProgress: number;
  failed: number;
  riskHigh: number;
  riskMedium: number;
  riskLow: number;
  dailyCounts: { date: string; count: number }[];
};

function lastNDates(n: number): string[] {
  const out: string[] = [];
  const now = new Date();
  for (let i = n - 1; i >= 0; i--) {
    const d = new Date(now);
    d.setDate(now.getDate() - i);
    out.push(d.toISOString().slice(0, 10));
  }
  return out;
}

function normalizeRisk(j: Job): "HIGH" | "MEDIUM" | "LOW" | undefined {
  // Try multiple fields
  const raw =
    (j.risk ?? j.riskLevel ?? (j as any)?.risk_level ?? (j as any)?.riskCategory ?? "")
      .toString()
      .trim()
      .toUpperCase();

  // Named variants
  if (["HIGH", "H", "HIGH_RISK", "RISK_HIGH", "SEVERE", "3"].includes(raw)) return "HIGH";
  if (["MEDIUM", "M", "MED", "MID", "RISK_MEDIUM", "2"].includes(raw)) return "MEDIUM";
  if (["LOW", "L", "LOW_RISK", "RISK_LOW", "1"].includes(raw)) return "LOW";

  // Numeric risk_score 0..1 (or 0..100)
  const numeric =
    typeof j.risk_score === "number"
      ? j.risk_score
      : Number.isFinite(Number(raw))
        ? Number(raw)
        : undefined;

  if (typeof numeric === "number") {
    const n = numeric > 1 ? numeric / 100 : numeric; // accept 73 or 0.73
    if (n >= 0.66) return "HIGH";
    if (n >= 0.33) return "MEDIUM";
    return "LOW";
  }

  // Heuristic (optional): try to read hints from summary if present
  const text = (j.summary ?? "").toString().toUpperCase();
  if (/\bHIGH\b|\bCRITICAL\b/.test(text)) return "HIGH";
  if (/\bMED(ium)?\b|\bMODERATE\b/.test(text)) return "MEDIUM";
  if (/\bLOW\b|\bMINOR\b/.test(text)) return "LOW";

  return undefined;
}


function deriveSummary(items: Job[]): Summary {
  const today = new Date().toISOString().slice(0, 10);
  const dates14 = lastNDates(14);

  let processedSum = 0;
  let jobsToday = 0;
  let inProgress = 0;
  let failed = 0;
  let riskHigh = 0, riskMedium = 0, riskLow = 0;

  const byDate: Record<string, number> = Object.fromEntries(dates14.map(d => [d, 0]));

  for (const j of items) {
    const st = String(j.status || "").toUpperCase();
    const dt = j.createdAt ? j.createdAt.slice(0, 10) : undefined;

    if (/COMPLETED|DONE|SUCCESS/.test(st)) processedSum += 1;
    if (dt && dt === today) jobsToday += 1;
    if (/IN_PROGRESS|RUNNING|QUEUED|PENDING/.test(st)) inProgress += 1;
    if (/FAILED|ERROR|CANCELLED/.test(st)) failed += 1;

    const rk = normalizeRisk(j);
    if (rk === "HIGH") riskHigh += 1;
    else if (rk === "MEDIUM") riskMedium += 1;
    else if (rk === "LOW") riskLow += 1;

    if (dt && byDate[dt] !== undefined) byDate[dt] += 1;
  }

  return {
    processedSum,
    jobsToday,
    inProgress,
    failed,
    riskHigh,
    riskMedium,
    riskLow,
    dailyCounts: dates14.map(d => ({ date: d, count: byDate[d] || 0 })),
  };
}


function formatWhen(iso?: string) {
  if (!iso) return "-";
  try {
    return new Date(iso).toLocaleString();
  } catch {
    return iso;
  }
}

export default function Dashboard() {
  const { token, logout } = useAuth();

  async function apiGet<T = any>(path: string) {
    const res = await fetch(`${API_BASE}${path}`, {
      headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    });

    // If auth is missing/expired, clear and let RequireAuth redirect
    if (res.status === 401) {
      logout();
      return { ok: false, status: res.status, data: undefined as T };
    }

    const ct = res.headers.get("content-type") || "";
    const body = ct.includes("application/json")
      ? await res.json().catch(() => undefined)
      : await res.text().catch(() => undefined);

    return { ok: res.ok, status: res.status, data: body as T };
  }

  const [apiOk, setApiOk] = useState<boolean | null>(null);
  const [jobs, setJobs] = useState<Job[]>([]);
  const [jobsErr, setJobsErr] = useState<string>("");

  useEffect(() => {
    if (!token) return; // RequireAuth should guard, but just in case
    (async () => {
      const h = await apiGet("/health");
      setApiOk(h.ok);
      if (!h.ok) console.warn("Health failed:", h.status, h.data);

      const r = await apiGet<{ items: Job[] }>("/jobs/recent?limit=5");
      if (r.ok && r.data && Array.isArray((r.data as any).items)) {
        setJobs((r.data as any).items);
        setJobsErr("");
      } else {
        setJobs([]);
        setJobsErr(`Failed to load jobs (${r.status})`);
        console.warn("Jobs failed:", r.status, r.data);
      }
    })();
  }, [token]);

  const s = useMemo(() => deriveSummary(jobs), [jobs]);

  const linePath = useMemo(() => {
    const data = s.dailyCounts;
    const W = 600,
      H = 240,
      pad = 28;
    const max = Math.max(1, ...data.map((d) => d.count));
    const step = (W - pad * 2) / Math.max(1, data.length - 1);
    const X = (i: number) => pad + i * step;
    const Y = (c: number) => H - pad - (c / max) * (H - pad * 2);
    return data
      .map((d, i) => `${i === 0 ? "M" : "L"} ${X(i)},${Y(d.count)}`)
      .join(" ");
  }, [s.dailyCounts]);

  return (
    <div>

      {/* KPIs */}
      <div className="kpi-grid">
        <div className="card">
          <div className="kpi-label">Processed (completed)</div>
          <div className="kpi-value">{s.processedSum}</div>
        </div>
        <div className="card">
          <div className="kpi-label">Jobs today</div>
          <div className="kpi-value">{s.jobsToday}</div>
        </div>
        <div className="card">
          <div className="kpi-label">In progress</div>
          <div className="kpi-value">{s.inProgress}</div>
        </div>
        <div className={`card ${s.failed > 0 ? "danger" : ""}`}>
          <div className="kpi-label">Failed</div>
          <div className="kpi-value">{s.failed}</div>
        </div>
      </div>

      <div className="grid-2" style={{ marginTop: 14 }}>
        <div className="card">
          <div className="section-title">Risk distribution</div>
          <div className="legend">
            <span>
              <span className="swatch high" /> High {s.riskHigh}
            </span>
            <span>
              <span className="swatch low" /> Low {s.riskLow}
            </span>
            <span>
              <span className="swatch medium" /> Medium {s.riskMedium}
            </span>
          </div>
          <div className="chart">
            <svg viewBox="0 0 300 240" preserveAspectRatio="none">
              {[0, 1, 2, 3, 4].map((i) => (
                <line
                  key={i}
                  className="grid-line"
                  x1="16"
                  x2="284"
                  y1={220 - i * 40}
                  y2={220 - i * 40}
                />
              ))}
              {[
                { k: "High", v: s.riskHigh, y: 40, fill: "#ef4444" },
                { k: "Medium", v: s.riskMedium, y: 120, fill: "#f59e0b" },
                { k: "Low", v: s.riskLow, y: 200, fill: "#10b981" },
              ].map(({ k, v, y, fill }) => {
                const max = Math.max(1, s.riskHigh, s.riskMedium, s.riskLow);
                const w = 16 + (v / max) * 240;
                return (
                  <g key={k}>
                    <rect
                      x="16"
                      y={y - 14}
                      width={w - 16}
                      height="28"
                      rx="6"
                      fill={fill}
                      opacity=".9"
                    />
                    <text x={w + 6} y={y - 2} fontSize="12" fill="#e5e7eb">
                      {v}
                    </text>
                    <text x="16" y={y - 22} fontSize="12" fill="#9ca3af">
                      {k}
                    </text>
                  </g>
                );
              })}
            </svg>
          </div>
        </div>

        <div className="card">
          <div className="section-title">Jobs over last 14 days</div>
          <div className="chart">
            <svg viewBox="0 0 600 240" preserveAspectRatio="none">
              {[0, 1, 2, 3, 4].map((i) => (
                <line
                  key={i}
                  className="grid-line"
                  x1="20"
                  x2="580"
                  y1={208 - i * 40}
                  y2={208 - i * 40}
                />
              ))}
              <path d={linePath} fill="none" stroke="#60a5fa" strokeWidth="2.5" />
            </svg>
          </div>
        </div>
      </div>

      <div className="card" style={{ marginTop: 14 }}>
        <div className="card-head">
          <div className="section-title" style={{ margin: 0 }}>
            Recent Jobs
          </div>
        </div>

        {jobsErr && <div className="alert alert-danger">⚠️ {jobsErr}</div>}

        {jobs.length === 0 ? (
          <p className="subtle">No recent jobs yet.</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Job</th>
                <th>Name</th>
                <th>Status</th>
                <th>Risk</th>
                <th>Created</th>
                <th>Summary</th>
              </tr>
            </thead>
            <tbody>
              {jobs.map((j, i) => {
                const key = j.id ? `${j.id}-${j.createdAt ?? ""}-${i}` : `row-${i}`;
                return (
                  <tr key={key}>
                    <td>{j.id}</td>
                    <td>{j.name ?? "-"}</td>
                    <td>{j.status}</td>
                    <td>{(normalizeRisk(j) ?? j.risk ?? "-")}</td>
                    <td>{formatWhen(j.createdAt)}</td>
                    <td>{j.summary ?? "-"}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

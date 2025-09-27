// src/api/client.ts
const BASE = import.meta.env.VITE_API_BASE ?? '';
const isMock = !BASE; // if no API base, serve mock responses

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE';

// --- helpers ---
function isFormData(x: unknown): x is FormData {
  return typeof FormData !== 'undefined' && x instanceof FormData;
}
function safeParse(text: string) {
  try { return text ? JSON.parse(text) : null; } catch { return null; }
}
function delay(ms: number) { return new Promise((r) => setTimeout(r, ms)); }

// --- Mock API (for dev without backend) ---
const mock = {
  async health() { await delay(120); return { status: 'ok' } as const; },

  async authLogin(username: string, password: string) {
    await delay(250);
    if (username === 'admin' && password === 'password') {
      return { token: 'mock.jwt.token', user: { username: 'admin', name: 'Admin' } };
    }
    const err = new Error('Invalid credentials'); (err as any).status = 401; throw err;
  },

  async searchEntity(q: { name: string; country?: string }) {
    await delay(250);
    const now = new Date().toISOString();
    return {
      matches: [
        { id: '1', name: `${q.name} HOLDINGS`, list: 'OFAC', riskScore: 82, country: q.country || 'NZ', lastUpdated: now },
        { id: '2', name: `${q.name} SERVICES`, list: 'PEP', riskScore: 56, country: 'AU', lastUpdated: now },
      ],
    } as SearchResponse;
  },

  async presignUpload(filename: string) {
    await delay(120);
    const key = `mock/${Date.now()}-${filename}`;
    return { url: `https://example.test/upload/${key}`, key, headers: {} };
  },

  async confirmUpload(_key: string, _country?: string) {
    await delay(120);
    return { jobId: crypto.randomUUID() };
  },

  async getJob(jobId: string) {
    await delay(200);
    return { jobId, status: 'DONE', createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() } as JobItem;
  },

  async getRecentJobs(limit = 20) {
    await delay(150);
    const now = new Date().toISOString();
    const items: JobItem[] = Array.from({ length: Math.min(limit, 5) }, (_, i) => ({
      jobId: `mock-${i + 1}`,
      status: 'DONE',
      createdAt: now,
      updatedAt: now,
      summary: { total: 2, high: 1, medium: 1, low: 0 },
    }));
    return { items };
  },

  async getResults(_jobId: string, _limit?: number, _lastKey?: string) {
    await delay(200);
    return {
      items: [
        { jobId: 'mock', recordId: '1', name: 'ACME HOLDINGS', country: 'NZ', matchName: 'ACME HOLDINGS LTD', riskScore: 78, processedAt: new Date().toISOString() },
        { jobId: 'mock', recordId: '2', name: 'GLOBAL SERVICES', country: 'AU', matchName: 'GLOBAL SERVICES PTY', riskScore: 52, processedAt: new Date().toISOString() },
      ],
    };
  },

  async uploadCsv(file: File) {
    await delay(200);
    const approxRows = Math.max(1, Math.round(file.size / 40));
    return { uploaded: true, rows: approxRows };
  },
};

// --- Low-level fetch helper (adds Bearer token if present) ---
async function request<T>(
  path: string,
  opts: RequestInit & { method?: HttpMethod } = {}
): Promise<T> {
  const { body } = opts;

  const autoHeaders: Record<string, string> = {};
  if (body && !isFormData(body) && !(body instanceof Blob)) {
    autoHeaders['Content-Type'] = 'application/json';
  }

  const token = typeof localStorage !== 'undefined' ? localStorage.getItem('token') : null;
  // (OPTIONAL backward-compat: if you still have an old apiKey in storage)
  const legacyApiKey = typeof localStorage !== 'undefined' ? localStorage.getItem('apiKey') : null;

  const res = await fetch(`${BASE}${path}`, {
    ...opts,
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(legacyApiKey ? { 'x-api-key': legacyApiKey } : {}),
      ...autoHeaders,
      ...(opts.headers ?? {}),
    },
  });

  if (res.status === 204) return null as T;

  const text = await res.text();
  const data = safeParse(text);

  if (!res.ok) {
    const message =
      (data && (data.message || data.error)) ||
      text ||
      `HTTP ${res.status}`;
    const err = new Error(message) as any;
    (err.status = res.status);
    throw err;
  }
  return data as T;
}

// --- Public API surface ---
export const api = {
  // Health check
  health: () => (isMock ? mock.health() : request<{ status: 'ok' }>('/health')),

  // Auth (username/password → token)
  authLogin: (username: string, password: string) =>
    isMock ? mock.authLogin(username, password)
      : request<{ token: string; user?: { username: string; name?: string } }>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password }),
      }),

  // Search
  searchEntity: (q: { name: string; country?: string }) =>
    isMock ? mock.searchEntity(q)
      : request<SearchResponse>('/search', { method: 'POST', body: JSON.stringify(q) }),

  // Upload presign/confirm
  presignUpload: (filename: string) =>
    isMock ? mock.presignUpload(filename)
      : request<{ url: string; key: string; headers?: Record<string, string> }>(
        `/uploads/presign?filename=${encodeURIComponent(filename)}`
      ),

  confirmUpload: (key: string, country?: string) =>
    isMock ? mock.confirmUpload(key, country)
      : request<{ jobId: string }>('/uploads/confirm', {
        method: 'POST',
        body: JSON.stringify({ key, country }),
      }),

  // Jobs
  getJob: (jobId: string) =>
    isMock ? mock.getJob(jobId)
      : request<JobItem>(`/jobs/${encodeURIComponent(jobId)}`),

  getRecentJobs: (limit = 20) =>
    isMock ? mock.getRecentJobs(limit)
      : request<{ items: JobItem[] }>(`/jobs/recent?limit=${limit}`),

  // Results
  getResults: (jobId: string, limit?: number, lastKey?: string) => {
    const params = new URLSearchParams();
    if (limit) params.set('limit', String(limit));
    if (lastKey) params.set('lastKey', lastKey);
    const qs = params.toString() ? `?${params.toString()}` : '';
    return isMock
      ? mock.getResults(jobId, limit, lastKey)
      : request<{ items: ResultItem[]; lastKey?: string }>(`/results/${encodeURIComponent(jobId)}${qs}`);
  },

  // Raw CSV upload (if you still support it)
  uploadCsv: (file: File) =>
    isMock ? mock.uploadCsv(file)
      : (function realUpload() {
        const form = new FormData();
        form.append('file', file);
        const token = localStorage.getItem('token');
        const legacyApiKey = localStorage.getItem('apiKey');
        return fetch(`${BASE}/upload`, {
          method: 'POST',
          body: form,
          headers: {
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
            ...(legacyApiKey ? { 'x-api-key': legacyApiKey } : {}),
          },
        }).then(async (r) => {
          if (!r.ok) { const txt = await r.text(); throw new Error(txt || `Upload failed: ${r.status}`); }
          return r.json();
        });
      })(),
};

// --- Types ---
export type MatchResult = {
  id: string;
  name: string;
  list: 'OFAC' | 'PEP' | 'Other';
  riskScore: number;
  country?: string;
  lastUpdated?: string;
};
export type SearchResponse = { matches: MatchResult[] };

export type JobStatus = 'QUEUED' | 'PROCESSING' | 'DONE' | 'FAILED';
export type JobItem = {
  jobId: string;
  status: JobStatus;
  createdAt?: string;
  updatedAt?: string;
  error?: string | null;
  summary?: { total?: number; high?: number; medium?: number; low?: number; truncated?: boolean } | null;
};
export type ResultItem = {
  jobId: string;
  recordId: string;
  name: string;
  country?: string;
  matchName?: string;
  riskScore?: number;
  processedAt?: string;
};

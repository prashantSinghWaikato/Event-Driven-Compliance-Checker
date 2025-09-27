import { useEffect, useMemo, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api, type ResultItem, type JobItem } from '../api/client';
import { Table, Thead, Tbody, Tr, Th, Td } from '../components/Table';
import RiskBadge from '../components/RiskBadge';

type SortKey = 'recordId' | 'name' | 'country' | 'matchName' | 'riskScore' | 'processedAt';

export default function Results() {
  const { jobId } = useParams();
  const [job, setJob] = useState<JobItem | null>(null);
  const [items, setItems] = useState<ResultItem[]>([]);
  const [error, setError] = useState('');
  const [lastKey, setLastKey] = useState<string | null>(null);
  const [loadingMore, setLoadingMore] = useState(false);
  const [band, setBand] = useState<'all' | 'high' | 'medium' | 'low'>('all');
  const [country, setCountry] = useState<string>('');
  const [sortKey, setSortKey] = useState<SortKey>('riskScore');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc');
  const timer = useRef<number | null>(null);

  // helper: safely pluck lastKey from union result
  const pickLastKey = (r: unknown): string | null =>
    r && typeof r === 'object' && 'lastKey' in r && typeof (r as any).lastKey === 'string'
      ? (r as any).lastKey as string
      : null;

  useEffect(() => {
    if (!jobId) return;
    let active = true;
    const stop = () => { if (timer.current) { clearInterval(timer.current); timer.current = null; } };

    const fetchStatus = async () => {
      try {
        const j = await api.getJob(jobId);
        if (!active) return;
        setJob(j);
        if (j.status === 'DONE') {
          stop();
          const r = await api.getResults(jobId, 100);
          if (!active) return;
          setItems(r.items || []);
          setLastKey(pickLastKey(r));             // <<< changed
        } else if (j.status === 'FAILED') {
          stop();
          setError(j.error || 'Processing failed');
        }
      } catch (e: any) {
        if (!active) return;
        setError(e.message || String(e)); stop();
      }
    };
    fetchStatus();
    timer.current = window.setInterval(fetchStatus, 3000);
    return () => { active = false; stop(); };
  }, [jobId]);

  const counts = useMemo(() => items.reduce((a, it) => {
    const s = it.riskScore ?? 0;
    if (s >= 80) a.high++; else if (s >= 50) a.medium++; else a.low++; return a;
  }, { high: 0, medium: 0, low: 0 }), [items]);

  const filtered = useMemo(() => {
    const normCountry = country.trim().toLowerCase();
    const passBand = (s: number) => band === 'high' ? s >= 80 : band === 'medium' ? s >= 50 && s < 80 : band === 'low' ? s < 50 : true;

    const val = (k: SortKey, x: ResultItem): number | string => {
      switch (k) {
        case 'recordId': return x.recordId ?? '';
        case 'name': return (x.name ?? '').toLowerCase();
        case 'country': return (x.country ?? '').toLowerCase();
        case 'matchName': return (x.matchName ?? '').toLowerCase();
        case 'riskScore': return x.riskScore ?? -1;
        case 'processedAt': return x.processedAt ? new Date(x.processedAt).getTime() : 0;
      }
    };

    const rows = items.filter(it => {
      const s = it.riskScore ?? 0;
      if (!passBand(s)) return false;
      if (normCountry && (it.country ?? '').toLowerCase() !== normCountry) return false;
      return true;
    });

    rows.sort((a, b) => {
      const va = val(sortKey, a), vb = val(sortKey, b);
      if (va < vb) return sortDir === 'asc' ? -1 : 1;
      if (va > vb) return sortDir === 'asc' ? 1 : -1;
      return 0;
    });
    return rows;
  }, [items, band, country, sortDir, sortKey]);

  const API_BASE = import.meta.env.VITE_API_BASE ?? '/api';
  const downloadUrl = jobId ? `${API_BASE}/results/${encodeURIComponent(jobId)}/csv` : '#';

  const loadMore = async () => {
    if (!jobId || !lastKey) return;
    setLoadingMore(true);
    try {
      const r = await api.getResults(jobId, 100, lastKey);
      setItems(prev => [...prev, ...(r.items || [])]);
      setLastKey(pickLastKey(r));                 // <<< changed
    } catch (e: any) { setError(e.message || String(e)); }
    finally { setLoadingMore(false); }
  };

  const onSort = (k: SortKey) => {
    if (sortKey === k) setSortDir(d => d === 'asc' ? 'desc' : 'asc');
    else { setSortKey(k); setSortDir('asc'); }
  };

  return (
    <section>
      <h2>Results for Job {jobId}</h2>

      <div className="toolbar">
        <a className="btn" href={downloadUrl} target="_blank" rel="noreferrer">⬇️ Download CSV</a>
        <label>Risk:
          <select value={band} onChange={e => setBand(e.target.value as any)} style={{ marginLeft: 6 }}>
            <option value="all">All</option>
            <option value="high">High (80+)</option>
            <option value="medium">Medium (50–79)</option>
            <option value="low">Low (&lt;50)</option>
          </select>
        </label>
        <label>Country:
          <input placeholder="e.g. NZ" value={country} onChange={e => setCountry(e.target.value)} style={{ marginLeft: 6, width: 100 }} />
        </label>
      </div>

      {job && (
        <div className="muted" style={{ marginBottom: 8 }}>
          Status: <strong>{job.status}</strong>
          {job.summary ? <> — total {job.summary.total}, H:{job.summary.high} M:{job.summary.medium} L:{job.summary.low} {job.summary.truncated ? '(truncated)' : ''}</> : null}
          {job.status === 'FAILED' && job.error ? <> — {job.error}</> : null}
          {job.updatedAt ? <> (updated {new Date(job.updatedAt).toLocaleTimeString()})</> : null}
        </div>
      )}

      {error && <div className="error">⚠️ {error}</div>}

      <ul className="cards" style={{ marginBottom: 12 }}>
        <li className="card"><div className="card-title">High</div><div className="card-value">{counts.high}</div></li>
        <li className="card"><div className="card-title">Medium</div><div className="card-value">{counts.medium}</div></li>
        <li className="card"><div className="card-title">Low</div><div className="card-value">{counts.low}</div></li>
      </ul>

      <Table>
        <Thead>
          <Tr>
            <Th sortable onClick={() => onSort('recordId')}>#</Th>
            <Th sortable onClick={() => onSort('name')}>Name</Th>
            <Th sortable onClick={() => onSort('country')}>Country</Th>
            <Th sortable onClick={() => onSort('matchName')}>Match</Th>
            <Th sortable onClick={() => onSort('riskScore')}>Risk</Th>
            <Th sortable onClick={() => onSort('processedAt')}>Processed</Th>
          </Tr>
        </Thead>
        <Tbody>
          {filtered.map((it) => (
            <Tr key={it.recordId}>
              <Td>{it.recordId}</Td>
              <Td>{it.name}</Td>
              <Td>{it.country || '—'}</Td>
              <Td>{it.matchName || '—'}</Td>
              <Td><RiskBadge score={(it.riskScore ?? 0)} /></Td>
              <Td>{it.processedAt ? new Date(it.processedAt).toLocaleString() : '—'}</Td>
            </Tr>
          ))}
        </Tbody>
      </Table>

      {lastKey && (
        <div style={{ marginTop: 12 }}>
          <button onClick={loadMore} disabled={loadingMore}>{loadingMore ? 'Loading…' : 'Load more'}</button>
        </div>
      )}
    </section>
  );
}

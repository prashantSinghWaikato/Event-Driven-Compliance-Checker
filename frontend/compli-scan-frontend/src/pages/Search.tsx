import { useState } from 'react';
import { api, type MatchResult } from '../api/client';
import RiskBadge from '../components/RiskBadge';

export default function Search() {
  const [name, setName] = useState('');
  const [country, setCountry] = useState('');
  const [results, setResults] = useState<MatchResult[] | null>(null);
  const [error, setError] = useState<string>('');
  const [loading, setLoading] = useState(false);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true); setError('');
    try {
      const res = await api.searchEntity({ name, country });
      setResults(res.matches);
    } catch (err: any) {
      setError(err.message || String(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <section>
      <h2>Search</h2>
      <form onSubmit={onSubmit} style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
        <input value={name} onChange={e => setName(e.target.value)} placeholder="Name" />
        <input value={country} onChange={e => setCountry(e.target.value)} placeholder="Country" />
        <button disabled={loading}>{loading ? 'Searching…' : 'Search'}</button>
      </form>
      {error && <div className="error">⚠️ {error}</div>}
      {results && (
        <ul>
          {results.map(r => (
            <li key={r.id} style={{ padding: '6px 0' }}>
              <b>{r.name}</b> — {r.list} — <RiskBadge score={r.riskScore} />
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
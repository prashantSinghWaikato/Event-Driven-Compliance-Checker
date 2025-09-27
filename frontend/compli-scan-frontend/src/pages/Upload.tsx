// src/pages/Upload.tsx
import { useState } from 'react';
import { api } from '../api/client';
import { useNavigate } from 'react-router-dom';

function parseHeader(line: string) {
  return line.trim().replace(/^\uFEFF/, '').split(',').map(s => s.trim().toLowerCase());
}

export default function Upload() {
  const [file, setFile] = useState<File | null>(null);
  const [msg, setMsg] = useState('');
  const [busy, setBusy] = useState(false);
  const nav = useNavigate();

  const onFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0] || null;
    setFile(f);
  };

  async function validateCsvHeaders(f: File) {
    const text = await f.slice(0, 4096).text();
    const firstLine = text.split(/\r?\n/)[0] || '';
    const headers = parseHeader(firstLine);
    const required = ['name', 'country'];
    const missing = required.filter(h => !headers.includes(h));
    if (missing.length) throw new Error(`CSV missing headers: ${missing.join(', ')}`);
  }

  const onGo = async () => {
    if (!file) { setMsg('Choose a CSV first'); return; }
    setBusy(true); setMsg('Validating CSV…');
    try {
      await validateCsvHeaders(file);

      setMsg('Requesting presigned URL…');
      const pre = await api.presignUpload(file.name);

      setMsg('Uploading to S3…');
      const headers: Record<string, string> = { 'Content-Type': 'text/csv', ...(pre.headers || {}) };
      const put = await fetch(pre.url, { method: 'PUT', body: file, headers });
      if (!put.ok) throw new Error(`PUT ${put.status}`);

      setMsg('Confirming upload…');
      const conf = await api.confirmUpload(pre.key, 'NZ');

      setMsg(`Uploaded and queued. Job: ${conf.jobId}`);
      nav(`/app/results/${conf.jobId}`, { replace: true }); // keep under /app
    } catch (e: any) {
      setMsg(`Error: ${e.message || String(e)}`);
    } finally {
      setBusy(false);
    }
  };

  return (
    <section>
      <h2>Bulk Upload</h2>
      <p className="muted">CSV headers required: <code>name,country</code></p>
      <input type="file" accept=".csv,text/csv" onChange={onFile} />
      <div style={{ marginTop: 10 }}>
        <button onClick={onGo} disabled={!file || busy}>{busy ? 'Working…' : 'Upload & Process'}</button>
      </div>
      {msg && <p className="muted" style={{ marginTop: 8 }}>{msg}</p>}
    </section>
  );
}

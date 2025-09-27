import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "../auth";

const API_BASE = import.meta.env.VITE_API_BASE ?? "http://localhost:8080/api";

export default function Signup() {
  const [username, setUsername] = useState("");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const nav = useNavigate();
  const { login } = useAuth();

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(""); setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/auth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, name, email, password })
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data?.error || `Register failed (${res.status})`);
      const token = data?.token as string | undefined;
      if (!token) throw new Error("Missing token");
      login(token, data?.user ?? { username, name, email, roles: ["VIEWER"] });
      nav("/app", { replace: true });
    } catch (e: any) {
      setError(e.message || "Register failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-left">
        <h1>Join CompliScan</h1>
        <p>Create an account to screen names against sanctions & PEP datasets.</p>
      </div>

      <div className="login-right">
        <div className="login-card">
          <h2>Create account</h2>
          <form className="login-form" onSubmit={submit} noValidate>
            <div className="field">
              <label className="label" htmlFor="username">Username</label>
              <input id="username" className="input" value={username}
                onChange={e => setUsername(e.target.value)} required />
            </div>
            <div className="field">
              <label className="label" htmlFor="name">Full name</label>
              <input id="name" className="input" value={name}
                onChange={e => setName(e.target.value)} />
            </div>
            <div className="field">
              <label className="label" htmlFor="email">Email</label>
              <input id="email" type="email" className="input" value={email}
                onChange={e => setEmail(e.target.value)} required />
            </div>
            <div className="field">
              <label className="label" htmlFor="password">Password</label>
              <input id="password" type="password" className="input" value={password}
                onChange={e => setPassword(e.target.value)} required />
            </div>

            {error && <p className="error">{error}</p>}
            <button className="btn-primary" type="submit" disabled={loading || !username || !email || !password}>
              {loading ? "Creating..." : "Sign Up"}
            </button>

            <p className="help center" style={{ marginTop: 8 }}>
              Already have an account? <Link to="/login">Sign in</Link>
            </p>
          </form>
        </div>
      </div>
    </div>
  );
}

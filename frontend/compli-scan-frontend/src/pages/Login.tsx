// src/pages/Login.tsx
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/index";
import { Link } from "react-router-dom";


const API_BASE = import.meta.env.VITE_API_BASE ?? "http://localhost:8080/api";

export default function Login() {
    // ✅ start blank
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const nav = useNavigate();
    const { login } = useAuth();

    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError("");
        setLoading(true);
        try {
            const res = await fetch(`${API_BASE}/auth/login`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, password }),
            });

            let data: any = null;
            try { data = await res.json(); } catch { /* ignore non-JSON */ }

            if (!res.ok) {
                const msg = data?.message || data?.error || `Login failed (HTTP ${res.status})`;
                throw new Error(msg);
            }

            const token: string | undefined = data?.token;
            if (!token) throw new Error("Missing token in response");

            login(token, data?.user ?? { username, name: username });

            // 👉 route to your protected shell (matches your /app/* routes)
            nav("/app", { replace: true });
        } catch (err: any) {
            setError(err?.message || "Login failed");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-page">
            {/* Left: product intro */}
            <div className="login-left">
                <h1>CompliScan</h1>
                <p>
                    CompliScan is an event-driven compliance platform that screens names against
                    sanctions (OFAC) and PEP datasets to support AML/CFT obligations.
                </p>
            </div>

            {/* Right: login form */}
            <div className="login-right">
                <div className="login-card">
                    <h2>Login</h2>
                    <form
                        className="login-form"
                        onSubmit={handleSubmit}
                        noValidate
                        autoComplete="off"
                    >
                        <div className="field">
                            <label className="label" htmlFor="username">Name</label>
                            <input
                                id="username"
                                name="username"
                                className="input"
                                type="text"
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                placeholder="e.g., admin"
                                autoComplete="username"
                                required
                            />
                        </div>

                        <div className="field">
                            <label className="label" htmlFor="password">Password</label>
                            <input
                                id="password"
                                name="password"
                                className="input"
                                type="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="••••••••"
                                autoComplete="current-password"
                                required
                            />
                        </div>

                        {error && <p className="error">{error}</p>}

                        <button
                            className="btn-primary"
                            type="submit"
                            disabled={loading || !username || !password}
                        >
                            {loading ? "Signing in..." : "Sign In"}
                        </button>

                        <p className="help center" style={{ marginTop: 8 }}>
                            New here? <Link to="/signup">Create an account</Link>
                        </p>


                        {/* Remove this line if you don't want the dev hint */}
                        {/* <p className="help center">Dev creds come from AUTH_USER / AUTH_PASSWORD.</p> */}
                    </form>
                </div>
            </div>
        </div>
    );
}

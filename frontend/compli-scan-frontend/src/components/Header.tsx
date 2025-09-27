import React, { useMemo } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../auth";

export default function Header() {
    const { token, user, logout } = useAuth();
    const nav = useNavigate();

    const authed = Boolean(token || user);

    const { displayName, email, initials, roles } = useMemo(() => {
        const u: any = user ?? {};
        const rawName = u?.name ?? u?.username ?? "Admin";
        const safeName = typeof rawName === "string" ? rawName : String(rawName ?? "Admin");
        const trimmed = safeName.trim();
        const rs: string[] = Array.isArray(u?.roles) ? (u.roles as string[]) : [];
        return {
            displayName: trimmed,
            email: typeof u?.email === "string" ? u.email : "",
            initials: (trimmed.charAt(0) || "A").toUpperCase(),
            roles: rs,
        };
    }, [user]);

    const canUpload = roles.includes("ADMIN") || roles.includes("UPLOADER");
    const canAnalyse = roles.includes("ADMIN") || roles.includes("ANALYST") || roles.includes("UPLOADER");

    const handleLogout = () => {
        logout();
        nav("/login", { replace: true });
    };

    return (
        <header className="topbar">
            <div className="brand">🛡️ CompliScan</div>

            <nav className="menu">
                <NavLink to="/app" end className={({ isActive }) => (isActive ? "active" : "")}>
                    Dashboard
                </NavLink>
                {canAnalyse && (
                    <NavLink to="/app/search" className={({ isActive }) => (isActive ? "active" : "")}>
                        Search
                    </NavLink>
                )}
                {canUpload && (
                    <NavLink to="/app/upload" className={({ isActive }) => (isActive ? "active" : "")}>
                        Bulk Upload
                    </NavLink>
                )}
                {canAnalyse && (
                    <NavLink to="/app/jobs" className={({ isActive }) => (isActive ? "active" : "")}>
                        Recent Jobs
                    </NavLink>
                )}
            </nav>

            <div className="menu">
                {authed ? (
                    <details className="user">
                        <summary className="user-btn" role="button" aria-haspopup="menu">
                            <span
                                style={{
                                    width: 24,
                                    height: 24,
                                    borderRadius: "50%",
                                    display: "inline-grid",
                                    placeItems: "center",
                                    marginRight: 8,
                                    background: "var(--primary)",
                                    color: "#fff",
                                    fontWeight: 700,
                                    fontSize: 12,
                                }}
                                aria-hidden
                            >
                                {initials}
                            </span>
                            <span style={{ marginRight: 6 }}>{displayName}</span>
                            <span aria-hidden>▾</span>
                        </summary>

                        <div className="user-menu" role="menu">
                            <div className="user-menu-header">{email || "Signed in"}</div>
                            <button className="user-menu-item danger" onClick={handleLogout} role="menuitem">
                                Logout
                            </button>
                        </div>
                    </details>
                ) : null}
            </div>
        </header>
    );
}

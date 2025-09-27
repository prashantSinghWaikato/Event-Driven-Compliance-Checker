// src/layouts/AppLayout.tsx
import React from "react";
import { Outlet } from "react-router-dom";
import Header from "../components/Header";

export default function AppLayout() {
    return (
        <div className="app">
            <Header />
            <main className="main">
                <Outlet />
            </main>
            <footer className="footer">© 2025 CompliScan</footer>
        </div>
    );
}

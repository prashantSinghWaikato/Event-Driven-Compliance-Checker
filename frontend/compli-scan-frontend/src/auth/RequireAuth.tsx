import { type ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "./index";

export default function RequireAuth({ children }: { children: ReactNode }) {
    const { token, ready } = useAuth();
    const loc = useLocation();

    // Don’t decide until we know whether a token exists
    if (!ready) return null; // or a small spinner

    if (!token) {
        return <Navigate to="/login" replace state={{ from: loc }} />;
    }
    return <>{children}</>;
}

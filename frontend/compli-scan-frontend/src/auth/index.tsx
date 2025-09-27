import {
    createContext,
    useContext,
    useMemo,
    useState,
    type ReactNode,
} from "react";

export type User = {
    username: string;
    name?: string;
    email?: string;
    roles?: string[];
};

type AuthCtx = {
    user: User | null;
    token: string | null;
    ready: boolean;                 // <-- hydration flag
    login: (token: string, user: User) => void;
    logout: () => void;
};

const Ctx = createContext<AuthCtx | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
    // Read from localStorage synchronously so refresh doesn't briefly look unauthenticated
    const [token, setToken] = useState<string | null>(() => {
        if (typeof window === "undefined") return null;
        try { return localStorage.getItem("token"); } catch { return null; }
    });

    const [user, setUser] = useState<User | null>(() => {
        if (typeof window === "undefined") return null;
        try {
            const raw = localStorage.getItem("user");
            return raw ? (JSON.parse(raw) as User) : null;
        } catch {
            return null;
        }
    });

    // we’re already hydrated because we read synchronously
    const ready = true;

    const login = (t: string, u: User) => {
        try {
            localStorage.setItem("token", t);
            localStorage.setItem("user", JSON.stringify(u));
        } catch { /* ignore quota issues */ }
        setToken(t);
        setUser(u);
    };

    const logout = () => {
        try {
            localStorage.removeItem("token");
            localStorage.removeItem("user");
        } catch { /* ignore */ }
        setToken(null);
        setUser(null);
        window.location.assign("/login");
    };

    const value = useMemo<AuthCtx>(
        () => ({ user, token, ready, login, logout }),
        [user, token]
    );

    return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useAuth() {
    const ctx = useContext(Ctx);
    if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
    return ctx;
}

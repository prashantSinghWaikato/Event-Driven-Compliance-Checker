import { useEffect } from "react";
import { useAuth } from "../auth";

export default function Logout() {
    const { logout } = useAuth();
    useEffect(() => { logout(); }, [logout]);
    return null;
}

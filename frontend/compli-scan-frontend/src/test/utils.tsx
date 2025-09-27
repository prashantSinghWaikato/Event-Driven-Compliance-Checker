import React from "react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { render } from "@testing-library/react";
import { AuthProvider } from "../auth";

export function renderWithProviders(
    ui: React.ReactNode,
    {
        route = "/",
        routes = [{ path: "/", element: ui }],
    }: { route?: string; routes?: { path: string; element: React.ReactNode }[] } = {}
) {
    window.history.pushState({}, "", route);

    return render(
        <AuthProvider>
            <MemoryRouter initialEntries={[route]}>
                <Routes>
                    {routes.map((r, i) => (
                        <Route key={i} path={r.path} element={r.element as any} />
                    ))}
                </Routes>
            </MemoryRouter>
        </AuthProvider>
    );
}

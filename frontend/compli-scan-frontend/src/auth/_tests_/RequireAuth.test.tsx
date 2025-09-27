import React from "react";
import { screen } from "@testing-library/react";
import { renderWithProviders } from "../../test/utils";
import RequireAuth from "../RequireAuth";

function Protected() {
    return <div>PROTECTED</div>;
}

describe("RequireAuth", () => {
    test("redirects to /login if no token", () => {
        const routes = [
            { path: "/login", element: <div>LOGIN</div> },
            { path: "/app", element: <RequireAuth><Protected /></RequireAuth> },
        ];

        renderWithProviders(<div />, { route: "/app", routes });
        expect(screen.getByText(/login/i)).toBeInTheDocument();
    });

    test("renders children if authed", () => {
        localStorage.setItem("token", "t");
        localStorage.setItem("user", JSON.stringify({ username: "a" }));

        const routes = [
            { path: "/login", element: <div>LOGIN</div> },
            { path: "/app", element: <RequireAuth><Protected /></RequireAuth> },
        ];

        renderWithProviders(<div />, { route: "/app", routes });
        expect(screen.getByText("PROTECTED")).toBeInTheDocument();
    });
});

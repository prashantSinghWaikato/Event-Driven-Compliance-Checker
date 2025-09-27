import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import Header from "../Header";
import { AuthProvider } from "../../auth";

describe("Header", () => {
    beforeEach(() => {
        localStorage.setItem("token", "test-token");
        localStorage.setItem(
            "user",
            JSON.stringify({ username: "alice", name: "Alice", email: "a@example.com" })
        );
    });

    afterEach(() => {
        localStorage.clear();
    });

    it("shows brand, a nav link, and user dropdown when authed", () => {
        render(
            <AuthProvider>
                <MemoryRouter initialEntries={["/app"]}>
                    <Header />
                </MemoryRouter>
            </AuthProvider>
        );

        // Brand
        expect(screen.getByText(/compliscan/i)).toBeInTheDocument();

        // At least the active link (Dashboard) is visible
        expect(screen.getByRole("link", { name: /dashboard/i })).toBeInTheDocument();

        // User info
        expect(screen.getByText("Alice")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: /alice/i })).toBeInTheDocument();
    });
});

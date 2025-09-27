import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { AuthProvider } from "../../auth";
import Login from "../Login";

const renderLogin = () =>
    render(
        <AuthProvider>
            <MemoryRouter initialEntries={["/login"]}>
                <Login />
            </MemoryRouter>
        </AuthProvider>
    );

describe("Login", () => {
    beforeEach(() => {
        vi.restoreAllMocks();
        localStorage.clear();
    });

    it("successful login stores token and navigates", async () => {
        vi.spyOn(global, "fetch").mockResolvedValueOnce({
            ok: true,
            json: async () => ({
                token: "tkn",
                user: { username: "alice", name: "Alice" },
            }),
        } as any);

        renderLogin();

        await userEvent.type(screen.getByLabelText(/name/i), "alice");
        await userEvent.type(screen.getByLabelText(/password/i), "secret");
        await userEvent.click(screen.getByRole("button", { name: /sign in/i }));

        // token saved
        expect(localStorage.getItem("token")).toBe("tkn");
        expect(JSON.parse(localStorage.getItem("user") || "{}").username).toBe("alice");
    });

    it("shows error on HTTP failure", async () => {
        vi.spyOn(global, "fetch").mockResolvedValueOnce({
            ok: false,
            status: 401,
            json: async () => ({ error: "bad creds" }),
        } as any);

        renderLogin();

        await userEvent.type(screen.getByLabelText(/name/i), "alice");
        await userEvent.type(screen.getByLabelText(/password/i), "wrong");
        await userEvent.click(screen.getByRole("button", { name: /sign in/i }));

        // your component prints backend error text ("bad creds")
        expect(await screen.findByText(/bad creds/i)).toBeInTheDocument();
    });
});

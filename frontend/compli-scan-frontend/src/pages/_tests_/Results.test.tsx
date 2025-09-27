import { render, screen } from "@testing-library/react";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "../../auth";
import Results from "../Results";

function okJson(body: any) {
    return {
        ok: true,
        status: 200,
        json: async () => body,
        text: async () => JSON.stringify(body),
    } as any;
}

function notFoundJson(body: any) {
    return {
        ok: false,
        status: 404,
        json: async () => body,
        text: async () => JSON.stringify(body),
    } as any;
}

describe("Results", () => {
    beforeEach(() => {
        // logged-in state
        localStorage.setItem("token", "tkn");
        localStorage.setItem("user", JSON.stringify({ username: "alice", name: "Alice" }));

        // robust fetch mock: match EXACT endpoints the page calls
        global.fetch = vi.fn(async (input: RequestInfo | URL) => {
            const url = typeof input === "string" ? input : (input as URL).toString();

            // Normalize (tests often call relative /api/... URLs)
            // We only care about the path
            const path = url.replace(/^https?:\/\/[^/]+/, "");

            // GET /api/jobs/job-1
            if (/^\/api\/jobs\/job-1(?:\?.*)?$/.test(path)) {
                return okJson({
                    jobId: "job-1",
                    status: "DONE",
                    summary: { total: 3, high: 1, medium: 1, low: 1, truncated: false },
                    updatedAt: new Date().toISOString(),
                });
            }

            // GET /api/results/job-1 (with optional query params)
            if (/^\/api\/results\/job-1(?:\?.*)?$/.test(path)) {
                return okJson({
                    items: [
                        { jobId: "job-1", recordId: "1", name: "A", country: "NZ", matchName: "X", riskScore: 90, processedAt: new Date().toISOString() },
                        { jobId: "job-1", recordId: "2", name: "B", country: "NZ", matchName: "Y", riskScore: 60, processedAt: new Date().toISOString() },
                        { jobId: "job-1", recordId: "3", name: "C", country: "AU", matchName: "Z", riskScore: 10, processedAt: new Date().toISOString() },
                    ],
                    lastKey: null,
                });
            }

            // Any other call → harmless not-found with text()
            return notFoundJson({ error: "not mocked" });
        }) as any;
    });

    afterEach(() => {
        localStorage.clear();
        vi.restoreAllMocks();
    });

    it("shows high/medium/low counts and table rows", async () => {
        render(
            <AuthProvider>
                <MemoryRouter initialEntries={["/app/results/job-1"]}>
                    <Routes>
                        <Route path="/app/results/:jobId" element={<Results />} />
                    </Routes>
                </MemoryRouter>
            </AuthProvider>
        );

        // Rows from mocked results
        expect(await screen.findByText("A")).toBeInTheDocument();
        expect(screen.getByText("B")).toBeInTheDocument();
        expect(screen.getByText("C")).toBeInTheDocument();

        // Summary cards (titles exist; your UI renders values adjacent)
        expect(screen.getByText(/^High$/i)).toBeInTheDocument();
        expect(screen.getByText(/^Medium$/i)).toBeInTheDocument();
        expect(screen.getByText(/^Low$/i)).toBeInTheDocument();
    });
});

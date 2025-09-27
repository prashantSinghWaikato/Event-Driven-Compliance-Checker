// src/main.tsx
import React from "react";
import ReactDOM from "react-dom/client";
import { createBrowserRouter, RouterProvider, Navigate } from "react-router-dom";
import { AuthProvider } from "./auth";
import RequireAuth from "./auth/RequireAuth";
import AppLayout from "./layouts/AppLayout";

import Login from "./pages/Login";
import Signup from "./pages/Signup";
import Dashboard from "./pages/Dashboard";
import Search from "./pages/Search";
import Upload from "./pages/Upload";
import Jobs from "./pages/Jobs";
import Results from "./pages/Results";

import "./styles.css";

const router = createBrowserRouter([
  { path: "/", element: <Navigate to="/login" replace /> },
  { path: "/login", element: <Login /> },
  { path: "/signup", element: <Signup /> },

  {
    path: "/app",
    element: (
      <RequireAuth>
        <AppLayout />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <Dashboard /> },
      { path: "search", element: <Search /> },
      { path: "upload", element: <Upload /> },
      { path: "jobs", element: <Jobs /> },
      { path: "results/:jobId", element: <Results /> },
    ],
  },

  { path: "*", element: <Navigate to="/login" replace /> },
]);

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>
  </React.StrictMode>
);

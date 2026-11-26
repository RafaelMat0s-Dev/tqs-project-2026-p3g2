import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

const backend = process.env.VITE_BACKEND_URL ?? "http://localhost:8080";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": backend,
      "/actuator": backend,
      "/v3/api-docs": backend,
      "/swagger-ui": backend,
      "/gbfs": backend,
    },
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./src/test/setup.ts"],
    include: ["src/**/*.test.{ts,tsx}"],
    coverage: {
      provider: "v8",
      reporter: ["text", "lcov", "html"],
      include: ["src/**/*.{ts,tsx}"],
      exclude: ["src/main.tsx", "src/**/*.test.{ts,tsx}", "src/test/**", "src/**/*.d.ts"],
      thresholds: { lines: 80, statements: 80, functions: 80, branches: 80 },
    },
  },
});

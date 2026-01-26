import { defineConfig } from '@rsbuild/core';
import { pluginReact } from '@rsbuild/plugin-react';
import * as path from "path";

// Docs: https://rsbuild.rs/config/
export default defineConfig({
  plugins: [pluginReact()],
  resolve: {
    alias: {
      "@/": path.resolve(__dirname, "src"),
    }
  },
  server: {
    proxy: {
      '/api': 'http://127.0.0.1:8090',
      '/open': 'http://127.0.0.1:8090',
      '/user': 'http://127.0.0.1:8090',
    }
  }
});

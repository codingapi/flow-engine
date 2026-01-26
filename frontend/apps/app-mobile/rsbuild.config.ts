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
}
});

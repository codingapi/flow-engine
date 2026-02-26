import {pluginReact} from '@rsbuild/plugin-react';
import {defineConfig} from '@rstest/core';
import {pluginSass} from "@rsbuild/plugin-sass";
import * as path from "path";

export default defineConfig({
    testEnvironment: 'jsdom',
    setupFiles: ['./rstest.setup.ts'],
    plugins: [pluginReact(), pluginSass()],
    resolve: {
        alias: {
            "@/": path.resolve(__dirname, "src"),
        }
    }
});

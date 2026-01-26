import { pluginReact } from '@rsbuild/plugin-react';
import { defineConfig } from '@rslib/core';
import * as path from 'node:path';

export default defineConfig({
    source: {
        entry: {
            index: './src/**',
        },
    },
    resolve: {
        alias: {
            "@/": path.resolve(__dirname, "src"),
        }
    },
    lib: [
        {
            bundle: false,
            dts: true,
            format: 'esm',
        },
    ],
    output: {
        target: 'web',
    },
    plugins: [pluginReact()],
});

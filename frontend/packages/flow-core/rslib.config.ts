import { defineConfig } from '@rslib/core';
import * as path from 'node:path';

export default defineConfig({
  source: {
    entry: {
      index: './src/index.ts',
    },
  },
  resolve: {
    alias: {
      "@/": path.resolve(__dirname, "src"),
    }
  },
  lib: [
    {
      bundle: true,
      dts: true,
      format: 'esm',
    },
  ],
  output: {
    target: 'web',
  },
});

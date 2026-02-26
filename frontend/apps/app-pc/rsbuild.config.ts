import { defineConfig } from '@rsbuild/core';
import { pluginReact } from '@rsbuild/plugin-react';
import { pluginLess } from '@rsbuild/plugin-less';
import { pluginSass } from '@rsbuild/plugin-sass';
import * as path from "path";

// Docs: https://rsbuild.rs/config/
export default defineConfig({
    plugins: [pluginReact(), pluginLess(), pluginSass()],
    resolve: {
        alias: {
            "@/": path.resolve(__dirname, "src"),
        }
    },

    source: {
        entry: {
            index: ['./src/index.tsx'],
        },

        /**
         * support inversify @injectable() and @inject decorators
         */
        decorators: {
            version: 'legacy',
        },
    },
    server: {
        proxy: {
            '/api': 'http://127.0.0.1:8090',
            '/open': 'http://127.0.0.1:8090',
            '/user': 'http://127.0.0.1:8090',
        }
    },
    tools: {
        rspack: {
            /**
             * ignore warnings from @coze-design-editor/design-editor/language-typescript
             */
            ignoreWarnings: [/Critical dependency: the request of a dependency is an expression/],
            // 添加 node 配置来模拟 __filename 和 __dirname
            node: {
                __filename: false,
                __dirname: false,
                global: true,
            },
        },
    },
});

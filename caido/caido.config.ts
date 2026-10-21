import { defineConfig } from '@caido-community/dev';
import vue from '@vitejs/plugin-vue';
import path from "path";
import prefixwrap from "postcss-prefixwrap";

const id = "pwnfox-color";
export default defineConfig({
  id,
  name: "PwnFoxColor",
  description: "PwnFox container colors and session labels for Caido",
  version: "0.1.0",
  author: {
    name: "malwarian",
    email: "malwarian@pm.me",
    url: "https://github.com/malwarian/PwnFoxColor",
  },
  plugins: [
    {
      kind: "backend",
      id: "backend",
      root: "packages/backend",
    },
    {
      kind: 'frontend',
      id: "frontend",
      root: 'packages/frontend',
      backend: {
        id: "backend",
      },
      vite: {
        plugins: [vue()],
        build: {
          rollupOptions: {
            external: ["vue"]
          }
        },
        resolve: {
          alias: [
            {
              find: "@",
              replacement: path.resolve(__dirname, "packages/frontend/src"),
            },
          ],
        },
        css: {
          postcss: {
            plugins: [prefixwrap(`#plugin--${id}`)]
          }
        }
      }
    }
  ]
});

import { defaultConfig } from "@caido/eslint-config";

export default [
  ...defaultConfig(),
  {
    rules: {
      "@typescript-eslint/no-restricted-types": "off",
    },
  },
]

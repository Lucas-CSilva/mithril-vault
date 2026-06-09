import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";
import prettierConfig from "eslint-config-prettier";
import boundaries from "eslint-plugin-boundaries";
import importPlugin from "eslint-plugin-import";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  {
    plugins: {
      import: importPlugin,
      boundaries,
    },
    settings: {
      "boundaries/elements": [
        { type: "app", pattern: "src/app/**" },
        {
          type: "features",
          pattern: "src/features/*/**",
          capture: ["featureName"],
        },
        { type: "core", pattern: "src/core/**" },
        { type: "shared", pattern: "src/shared/**" },
      ],
      "boundaries/ignore": ["**/*.test.*", "**/*.spec.*"],
    },
    rules: {
      "@typescript-eslint/no-unused-vars": [
        "error",
        {
          argsIgnorePattern: "^_",
          varsIgnorePattern: "^_",
        },
      ],
      "@typescript-eslint/no-explicit-any": "error",
      "prefer-const": "error",
      "no-console": ["warn", { allow: ["warn", "error"] }],

      "boundaries/dependencies": [
        "error",
        {
          default: "disallow",
          rules: [
            {
              from: { type: "app" },
              allow: [
                { to: { type: "app" } },
                { to: { type: "features" } },
                { to: { type: "core" } },
                { to: { type: "shared" } },
              ],
            },
            {
              from: [
                {
                  type: "features",
                  captured: { featureName: "{{featureName}}" },
                },
              ],
              allow: [
                { to: { type: "core" } },
                { to: { type: "shared" } },
                {
                  to: {
                    type: "features",
                    captured: { featureName: "{{from.featureName}}" },
                  },
                },
              ],
            },
            {
              from: { type: "core" },
              allow: [{ to: { type: "core" } }, { to: { type: "shared" } }],
            },
            {
              from: { type: "shared" },
              allow: [{ to: { type: "shared" } }],
            },
          ],
        },
      ],

      "import/order": [
        "error",
        {
          groups: [
            "builtin",
            "external",
            "internal",
            ["parent", "sibling"],
            "index",
            "object",
            "type",
          ],
          pathGroups: [
            { pattern: "react", group: "external", position: "before" },
            { pattern: "next/**", group: "external", position: "before" },
            { pattern: "@/**", group: "internal", position: "before" },
          ],
          pathGroupsExcludedImportTypes: ["react", "next"],
          "newlines-between": "always",
          alphabetize: { order: "asc", caseInsensitive: true },
        },
      ],
      "import/newline-after-import": "error",
      "import/no-duplicates": "error",
    },
  },
  prettierConfig,

  globalIgnores([
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
    "node_modules/**",
    "*.config.js",
    "*.config.ts",
  ]),
]);

export default eslintConfig;

import type { SDK } from "caido:plugin";

export type ColorRule = {
  token: string;
  label: string;
  hex: string;
  portHint: number | null;
};

export type Language = "en" | "es";

const DEFAULTS: ColorRule[] = [
  { token: "blue", label: "PwnFox Blue", hex: "#0A84FF", portHint: 8081 },
  { token: "cyan", label: "PwnFox Turquoise", hex: "#00FEFF", portHint: 8082 },
  { token: "green", label: "PwnFox Green", hex: "#30E60B", portHint: 8083 },
  { token: "yellow", label: "PwnFox Yellow", hex: "#FFE900", portHint: 8084 },
  { token: "orange", label: "PwnFox Orange", hex: "#FF9400", portHint: 8085 },
  { token: "red", label: "PwnFox Red", hex: "#FF0039", portHint: 8086 },
  { token: "magenta", label: "PwnFox Pink", hex: "#FF1AD9", portHint: 8087 },
  { token: "purple", label: "PwnFox Purple", hex: "#9400FF", portHint: 8088 },
];

const normalize = (rule: ColorRule): ColorRule => {
  const token = rule.token.trim().toLowerCase();
  const label = rule.label.trim();
  let hex = rule.hex.trim().toUpperCase();
  if (/^#[0-9A-F]{3}$/.test(hex)) {
    hex = `#${hex[1]}${hex[1]}${hex[2]}${hex[2]}${hex[3]}${hex[3]}`;
  }
  if (!/^[a-z0-9_-]{1,32}$/.test(token)) {
    throw new Error(
      "El identificador de PwnFox debe tener 1–32 caracteres ASCII.",
    );
  }
  if (!label || label.length > 80) {
    throw new Error("La etiqueta debe tener 1–80 caracteres.");
  }
  if (!/^#[0-9A-F]{6}$/.test(hex)) {
    throw new Error("El color debe ser #RRGGBB o #RGB.");
  }
  const portHint =
    rule.portHint === null || rule.portHint === undefined
      ? null
      : Number(rule.portHint);
  if (
    portHint !== null &&
    (!Number.isInteger(portHint) || portHint < 1 || portHint > 65535)
  ) {
    throw new Error("El puerto de referencia debe estar entre 1 y 65535.");
  }
  return { token, label, hex, portHint };
};

export type API = {
  getLanguage: () => Language;
  saveLanguage: (language: Language) => Promise<Language>;
  getRules: () => ColorRule[];
  saveRules: (rules: ColorRule[]) => Promise<ColorRule[]>;
  getPwnFoxDefaults: () => ColorRule[];
  getRequestLabel: (requestId: string) => Promise<ColorRule | null>;
};

export async function init(sdk: SDK<API>): Promise<void> {
  const db = await sdk.meta.db();
  await db.exec(`
    CREATE TABLE IF NOT EXISTS settings (
      key TEXT PRIMARY KEY,
      value TEXT NOT NULL
    );
    CREATE TABLE IF NOT EXISTS request_labels (
      request_id TEXT PRIMARY KEY,
      token TEXT NOT NULL,
      label TEXT NOT NULL,
      hex TEXT NOT NULL,
      port_hint INTEGER
    );
  `);

  const readSetting = await db.prepare(
    "SELECT value FROM settings WHERE key = 'rules'",
  );
  const saveSetting = await db.prepare(
    "INSERT OR REPLACE INTO settings(key, value) VALUES ('rules', ?)",
  );
  const saved = await readSetting.get<{ value: string }>();
  const initialRules: ColorRule[] = saved
    ? (JSON.parse(saved.value) as ColorRule[])
    : DEFAULTS;
  if (!saved) await saveSetting.run(JSON.stringify(initialRules));
  let currentRules = initialRules;
  let byToken = new Map(currentRules.map((rule) => [rule.token, rule]));

  const readLanguage = await db.prepare(
    "SELECT value FROM settings WHERE key = 'language'",
  );
  const saveLanguageSetting = await db.prepare(
    "INSERT OR REPLACE INTO settings(key, value) VALUES ('language', ?)",
  );
  const savedLanguage = await readLanguage.get<{ value: string }>();
  let currentLanguage: Language = savedLanguage?.value === "es" ? "es" : "en";

  sdk.api.register("getLanguage", () => currentLanguage);
  sdk.api.register("saveLanguage", async (_sdk, value) => {
    if (value !== "en" && value !== "es") {
      throw new Error("Unsupported language.");
    }
    await saveLanguageSetting.run(value);
    currentLanguage = value;
    return currentLanguage;
  });

  sdk.api.register("getRules", () => currentRules);
  sdk.api.register("saveRules", async (_sdk, input) => {
    if (!Array.isArray(input) || input.length > 128) {
      throw new Error("Se admiten como máximo 128 reglas.");
    }
    const rules = input.map(normalize);
    if (new Set(rules.map((rule) => rule.token)).size !== rules.length) {
      throw new Error("Cada identificador de PwnFox debe ser único.");
    }
    await saveSetting.run(JSON.stringify(rules));
    currentRules = rules;
    byToken = new Map(rules.map((rule) => [rule.token, rule]));
    return rules;
  });
  sdk.api.register("getPwnFoxDefaults", () => DEFAULTS);
  sdk.api.register("getRequestLabel", async (_sdk, requestId) => {
    const statement = await db.prepare(
      "SELECT token, label, hex, port_hint FROM request_labels WHERE request_id = ?",
    );
    const row = await statement.get<{
      token: string;
      label: string;
      hex: string;
      port_hint: number | null;
    }>(requestId);
    return row
      ? {
          token: row.token,
          label: row.label,
          hex: row.hex,
          portHint: row.port_hint,
        }
      : null;
  });

  sdk.events.onInterceptRequest(async (_eventSdk, request) => {
    const values = request.getHeader("X-PwnFox-Color");
    if (!values || values.length !== 1) return;
    const token = values[0]?.trim().toLowerCase();
    const rule =
      token !== undefined && token !== "" ? byToken.get(token) : undefined;
    if (!rule) return;
    try {
      const statement = await db.prepare(
        "INSERT OR REPLACE INTO request_labels(request_id, token, label, hex, port_hint) VALUES (?, ?, ?, ?, ?)",
      );
      await statement.run(
        request.getId(),
        rule.token,
        rule.label,
        rule.hex,
        rule.portHint,
      );
      const mutation = `mutation PwnFoxColorSetColor {
        updateRequestMetadata(id: ${JSON.stringify(request.getId())}, input: {color: ${JSON.stringify(rule.hex)}}) {
          metadata { id color }
        }
      }`;
      const result = await sdk.graphql.execute<{
        updateRequestMetadata: { metadata: { color: string } | null };
      }>(mutation);
      if (
        (result.errors !== undefined && result.errors.length > 0) ||
        !result.data?.updateRequestMetadata?.metadata
      ) {
        sdk.console.error(
          `PwnFoxColor: no se pudo colorear la petición ${request.getId()}`,
        );
      }
    } catch (error) {
      sdk.console.error(
        `PwnFoxColor: error al colorear la petición: ${String(error)}`,
      );
    }
  });
}

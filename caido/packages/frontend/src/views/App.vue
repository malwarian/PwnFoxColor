<script setup lang="ts">
import type { ColorRule, Language } from "backend";
import { onMounted, ref } from "vue";

import {
  copy,
  ensureLanguageLoaded,
  errorDetails,
  language,
  saveLanguage as persistLanguage,
} from "@/i18n";
import { useSDK } from "@/plugins/sdk";

const sdk = useSDK();
const rules = ref<ColorRule[]>([]);
const busy = ref(false);
const status = ref("");
const stripRuleName = "PwnFoxColor · remove X-PwnFox-Color";
const legacyStripRuleName = "PwnFoxColor · eliminar X-PwnFox-Color";
const stripInstalled = ref(false);
const isStripRule = (name: string) =>
  name === stripRuleName || name === legacyStripRuleName;

const refreshStripStatus = () => {
  stripInstalled.value = sdk.matchReplace
    .getRules()
    .some((rule) => isStripRule(rule.name) && rule.isEnabled);
};

onMounted(async () => {
  try {
    await ensureLanguageLoaded(sdk);
  } catch (error) {
    status.value = `${copy.value.loadFailed}: ${errorDetails(error)}`;
  }
  try {
    rules.value = await sdk.backend.getRules();
    refreshStripStatus();
  } catch (error) {
    status.value = `${copy.value.loadFailed}: ${errorDetails(error)}`;
  }
});

const changeLanguage = async (event: Event) => {
  const selector = event.target as HTMLSelectElement;
  const next = selector.value;
  if (next !== "en" && next !== "es") return;
  busy.value = true;
  try {
    await persistLanguage(sdk, next as Language);
    status.value = copy.value.languageSaved;
  } catch (error) {
    selector.value = language.value;
    status.value = `${copy.value.languageFailed}: ${errorDetails(error)}`;
  } finally {
    busy.value = false;
  }
};

const save = async () => {
  busy.value = true;
  status.value = "";
  try {
    rules.value = await sdk.backend.saveRules(rules.value);
    status.value = copy.value.rulesSaved;
  } catch (error) {
    status.value = `${copy.value.saveFailed}: ${errorDetails(error)}`;
  } finally {
    busy.value = false;
  }
};

const addDefaults = async () => {
  try {
    const defaults = await sdk.backend.getPwnFoxDefaults();
    const present = new Set(
      rules.value.map((rule) => rule.token.trim().toLowerCase()),
    );
    rules.value.push(...defaults.filter((rule) => !present.has(rule.token)));
    status.value = copy.value.defaultsAdded;
  } catch (error) {
    status.value = `${copy.value.defaultsFailed}: ${errorDetails(error)}`;
  }
};

const addRule = () => {
  rules.value.push({ token: "", label: "", hex: "#0A84FF", portHint: null });
};

const setPort = (rule: ColorRule, event: Event) => {
  const value = (event.target as HTMLInputElement).value.trim();
  rule.portHint = value === "" ? null : Number(value);
};

const setPickedColor = (rule: ColorRule, event: Event) => {
  rule.hex = (event.target as HTMLInputElement).value.toUpperCase();
};

const colorPickerValue = (hex: string) => {
  if (/^#[0-9a-fA-F]{6}$/.test(hex)) return hex;
  if (/^#[0-9a-fA-F]{3}$/.test(hex)) {
    return `#${hex[1]}${hex[1]}${hex[2]}${hex[2]}${hex[3]}${hex[3]}`;
  }
  return "#000000";
};

const installStripRule = async () => {
  busy.value = true;
  status.value = "";
  try {
    let collection = sdk.matchReplace
      .getCollections()
      .find((item) => item.name === "PwnFoxColor");
    if (!collection)
      collection = await sdk.matchReplace.createCollection({
        name: "PwnFoxColor",
      });
    const section = {
      kind: "SectionRequestHeader" as const,
      operation: {
        kind: "OperationHeaderRemove" as const,
        matcher: { kind: "MatcherName" as const, name: "X-PwnFox-Color" },
      },
    };
    const sources = ["INTERCEPT" as const, "REPLAY" as const];
    const existing = sdk.matchReplace
      .getRules()
      .find((rule) => isStripRule(rule.name));
    const rule = existing
      ? await sdk.matchReplace.updateRule(existing.id, {
          name: stripRuleName,
          query: "",
          section,
          sources,
        })
      : await sdk.matchReplace.createRule({
          name: stripRuleName,
          query: "",
          section,
          collectionId: collection.id,
          sources,
        });
    await sdk.matchReplace.toggleRule(rule.id, true);
    refreshStripStatus();
    status.value = copy.value.stripActivated;
  } catch (error) {
    status.value = `${copy.value.stripFailed}: ${errorDetails(error)}`;
  } finally {
    busy.value = false;
  }
};
</script>

<template>
  <main class="pfc-page">
    <header class="pfc-header">
      <div>
        <h1>PwnFoxColor · Caido</h1>
        <p>{{ copy.description }}</p>
      </div>
      <label class="pfc-language">
        {{ copy.language }}
        <select :value="language" :disabled="busy" @change="changeLanguage">
          <option value="en">English</option>
          <option value="es">Español</option>
        </select>
      </label>
    </header>

    <div class="pfc-actions">
      <button type="button" :disabled="busy" @click="addDefaults">
        {{ copy.addDefaults }}
      </button>
      <button type="button" :disabled="busy" @click="addRule">
        {{ copy.addRule }}
      </button>
      <button type="button" class="pfc-primary" :disabled="busy" @click="save">
        {{ copy.saveRules }}
      </button>
    </div>

    <div class="pfc-table-wrap">
      <table>
        <thead>
          <tr>
            <th>{{ copy.header }}</th>
            <th>{{ copy.label }}</th>
            <th>HEX</th>
            <th>{{ copy.portHint }}</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(rule, index) in rules" :key="index">
            <td>
              <input
                v-model="rule.token"
                :aria-label="copy.headerAria"
                placeholder="blue"
                maxlength="32"
              />
            </td>
            <td>
              <input
                v-model="rule.label"
                :aria-label="copy.labelAria"
                placeholder="Session 1"
                maxlength="80"
              />
            </td>
            <td>
              <div class="pfc-color">
                <input
                  type="color"
                  :value="colorPickerValue(rule.hex)"
                  :aria-label="copy.chooseColor"
                  @input="setPickedColor(rule, $event)"
                /><input
                  v-model="rule.hex"
                  :aria-label="copy.hexAria"
                  placeholder="#0A84FF"
                  maxlength="7"
                />
              </div>
            </td>
            <td>
              <input
                type="number"
                min="1"
                max="65535"
                :value="rule.portHint ?? ''"
                :aria-label="copy.portHint"
                @input="setPort(rule, $event)"
              />
            </td>
            <td>
              <button
                type="button"
                class="pfc-remove"
                :disabled="busy"
                :aria-label="`${copy.removeAria} ${rule.token || copy.ruleFallback}`"
                @click="rules.splice(index, 1)"
              >
                {{ copy.remove }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <section class="pfc-panel">
      <h2>{{ copy.stripHeading }}</h2>
      <p>{{ copy.stripDescription }}</p>
      <button type="button" :disabled="busy" @click="installStripRule">
        {{ stripInstalled ? copy.updateStrip : copy.installStrip }}
      </button>
      <p class="pfc-hint">{{ copy.stripHint }}</p>
    </section>

    <section class="pfc-panel">
      <h2>{{ copy.historyHeading }}</h2>
      <p>{{ copy.historyDescription }}</p>
    </section>
    <p v-if="status" class="pfc-status" role="status">{{ status }}</p>
  </main>
</template>

<style scoped>
.pfc-page {
  color: var(--p-text-color, #e8e8e8);
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
  font:
    14px/1.45 system-ui,
    sans-serif;
}
.pfc-page h1 {
  font-size: 22px;
  margin: 0 0 8px;
}
.pfc-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 20px;
}
.pfc-language {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}
.pfc-language select {
  border: 1px solid #666;
  border-radius: 4px;
  background: #292929;
  color: white;
  padding: 6px;
}
.pfc-page h2 {
  font-size: 16px;
  margin: 0 0 8px;
}
.pfc-page p {
  margin: 0 0 12px;
}
.pfc-actions {
  display: flex;
  gap: 8px;
  margin: 22px 0 14px;
  flex-wrap: wrap;
}
.pfc-page button {
  border: 1px solid #737373;
  border-radius: 5px;
  padding: 7px 11px;
  background: #363636;
  color: white;
  cursor: pointer;
}
.pfc-page button:hover {
  background: #4a4a4a;
}
.pfc-page button:disabled {
  opacity: 0.5;
  cursor: wait;
}
.pfc-page .pfc-primary {
  background: #1769aa;
  border-color: #1c7dcb;
}
.pfc-page .pfc-remove {
  background: transparent;
}
.pfc-table-wrap {
  overflow-x: auto;
}
.pfc-page table {
  border-collapse: collapse;
  width: 100%;
  min-width: 760px;
}
.pfc-page th,
.pfc-page td {
  border-bottom: 1px solid #555;
  text-align: left;
  padding: 7px;
}
.pfc-page th {
  color: #aaa;
  font-weight: 600;
}
.pfc-page input:not([type="color"]) {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #666;
  border-radius: 4px;
  background: #292929;
  color: white;
  padding: 6px;
}
.pfc-color {
  display: flex;
  gap: 7px;
  align-items: center;
  min-width: 145px;
}
.pfc-color input[type="color"] {
  width: 34px;
  height: 30px;
  padding: 0;
  border: 0;
  background: none;
  cursor: pointer;
  flex: none;
}
.pfc-panel {
  margin-top: 22px;
  padding: 16px;
  border: 1px solid #555;
  border-radius: 6px;
}
.pfc-hint {
  color: #aaa;
  margin-top: 10px !important;
}
.pfc-status {
  margin-top: 16px !important;
  padding: 10px;
  border: 1px solid #777;
  border-radius: 4px;
}
</style>

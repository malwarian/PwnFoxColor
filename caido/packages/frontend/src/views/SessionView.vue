<script setup lang="ts">
import type { RequestReadableViewModeProps } from "@caido/sdk-frontend";
import type { ColorRule } from "backend";
import { onMounted, ref, watch } from "vue";

import { copy, ensureLanguageLoaded } from "@/i18n";
import type { FrontendSDK } from "@/types";

const props = defineProps<RequestReadableViewModeProps>();
const pluginSdk = props.sdk as unknown as FrontendSDK;
const rule = ref<ColorRule | null>(null);
const error = ref("");

onMounted(() => {
  void ensureLanguageLoaded(pluginSdk).catch(() => {});
});

watch(
  () => props.request.id,
  async (id) => {
    rule.value = null;
    error.value = "";
    try {
      rule.value = await pluginSdk.backend.getRequestLabel(id);
    } catch (cause) {
      error.value = String(cause);
    }
  },
  { immediate: true },
);
</script>

<template>
  <div style="display: flex; align-items: center; gap: 12px; padding: 20px">
    <template v-if="rule">
      <span
        :style="{
          display: 'block',
          width: '26px',
          height: '26px',
          flex: 'none',
          border: '1px solid #888',
          borderRadius: '5px',
          backgroundColor: rule.hex,
        }"
      ></span>
      <div>
        <strong>{{ rule.label }}</strong>
        <div style="margin-top: 4px; color: #aaa">
          PwnFox: {{ rule.token }} · {{ rule.hex
          }}<span v-if="rule.portHint">
            · {{ copy.sessionPortHint }}: {{ rule.portHint }}</span
          >
        </div>
      </div>
    </template>
    <p v-else-if="error">{{ copy.sessionReadFailed }}: {{ error }}</p>
    <p v-else>{{ copy.sessionNoLabel }}</p>
  </div>
</template>

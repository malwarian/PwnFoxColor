import type { Language } from "backend";
import { computed, ref } from "vue";

import type { FrontendSDK } from "./types";

export const language = ref<Language>("en");

const strings = {
  en: {
    language: "Language",
    languageSaved: "Language set to English.",
    languageFailed: "Could not save the language",
    description:
      "Exact HEX colors for PwnFox requests. The container is identified by X-PwnFox-Color; the port is only a visual reference.",
    addDefaults: "Add PwnFox 8081–8088",
    addRule: "Add rule",
    saveRules: "Save rules",
    header: "PwnFox header",
    label: "Label / Session",
    portHint: "Port hint",
    headerAria: "Header identifier",
    labelAria: "Session label",
    chooseColor: "Choose color",
    hexAria: "Hex color",
    remove: "Remove",
    removeAria: "Remove",
    ruleFallback: "rule",
    stripHeading: "Keep the header from reaching the server",
    stripDescription:
      "Enable PwnFox's Add container header option. To remove the header before it reaches the destination, install this Match & Replace rule for Proxy and Replay.",
    installStrip: "Install header removal rule",
    updateStrip: "Update header removal rule",
    stripHint:
      "Check processing order in your Caido version with a test request: History should retain the color, and the destination must not receive X-PwnFox-Color.",
    historyHeading: "Labels in HTTP History",
    historyDescription:
      "Open a request and select the Session view to see its label. Caido's API does not provide an extra column in the History table. Configure ports 8081–8088 when starting Caido; this plugin cannot create them through the SDK.",
    loadFailed: "Could not load the configuration",
    rulesSaved: "Rules saved. New requests will use these colors.",
    saveFailed: "Could not save the rules",
    defaultsAdded:
      "PwnFox colors added to the editor. Click Save rules to apply them.",
    defaultsFailed: "Could not load PwnFox colors",
    stripActivated:
      "Header removal is enabled for Proxy and Replay. Use a test request to check that History gets the color and the header does not reach the destination.",
    stripFailed: "Could not enable header removal",
    sessionPortHint: "Port hint",
    sessionReadFailed: "Could not read the label",
    sessionNoLabel: "This request has no PwnFoxColor label.",
  },
  es: {
    language: "Idioma",
    languageSaved: "Idioma cambiado a español.",
    languageFailed: "No se pudo guardar el idioma",
    description:
      "Colores HEX exactos para peticiones de PwnFox. La identidad se obtiene de X-PwnFox-Color; el puerto es solo una referencia visual.",
    addDefaults: "Añadir PwnFox 8081–8088",
    addRule: "Añadir regla",
    saveRules: "Guardar reglas",
    header: "Cabecera PwnFox",
    label: "Etiqueta / Session",
    portHint: "Puerto orientativo",
    headerAria: "Identificador de cabecera",
    labelAria: "Etiqueta de sesión",
    chooseColor: "Elegir color",
    hexAria: "Color hexadecimal",
    remove: "Quitar",
    removeAria: "Quitar",
    ruleFallback: "regla",
    stripHeading: "Evitar que la cabecera llegue al servidor",
    stripDescription:
      "Activa PwnFox «Add container header» y, si quieres retirarla antes del destino, instala esta regla de Match & Replace para Proxy y Replay.",
    installStrip: "Instalar regla de limpieza",
    updateStrip: "Actualizar regla de limpieza",
    stripHint:
      "Comprueba el orden de procesamiento en tu versión de Caido con una petición de prueba: debe conservar el color en History y el destino no debe recibir X-PwnFox-Color.",
    historyHeading: "Etiquetas en HTTP History",
    historyDescription:
      "Abre una petición y selecciona la vista Session para ver su etiqueta. La API de Caido no ofrece una columna adicional en la tabla. Los puertos 8081–8088 se configuran al iniciar Caido; esta extensión no puede crearlos desde el SDK.",
    loadFailed: "No se pudo cargar la configuración",
    rulesSaved: "Reglas guardadas. Las peticiones nuevas usarán estos colores.",
    saveFailed: "No se guardaron las reglas",
    defaultsAdded:
      "Colores PwnFox añadidos al editor. Pulsa Guardar reglas para aplicarlos.",
    defaultsFailed: "No se pudieron cargar los colores PwnFox",
    stripActivated:
      "Regla de limpieza activada para Proxy y Replay. Comprueba con una petición de prueba que el color se aplica y la cabecera no llega al destino.",
    stripFailed: "No se pudo activar la regla de limpieza",
    sessionPortHint: "Puerto orientativo",
    sessionReadFailed: "No se pudo leer la etiqueta",
    sessionNoLabel: "Esta petición no tiene una etiqueta de PwnFoxColor.",
  },
} as const;

export const copy = computed(() => strings[language.value]);

let loaded = false;
let pending: Promise<void> | null = null;
let revision = 0;

export const ensureLanguageLoaded = async (sdk: FrontendSDK): Promise<void> => {
  if (loaded) return;
  if (!pending) {
    const startedAt = revision;
    pending = sdk.backend
      .getLanguage()
      .then((value) => {
        if (revision === startedAt) {
          language.value = value === "es" ? "es" : "en";
          loaded = true;
        }
      })
      .finally(() => {
        pending = null;
      });
  }
  await pending;
};

export const saveLanguage = async (
  sdk: FrontendSDK,
  value: Language,
): Promise<void> => {
  revision++;
  const saved = await sdk.backend.saveLanguage(value);
  language.value = saved;
  loaded = true;
};

const backendErrors: Record<string, string> = {
  "Se admiten como máximo 128 reglas.": "At most 128 rules are allowed.",
  "Cada identificador de PwnFox debe ser único.":
    "Each PwnFox identifier must be unique.",
  "El identificador de PwnFox debe tener 1–32 caracteres ASCII.":
    "The PwnFox identifier must have 1–32 ASCII characters.",
  "La etiqueta debe tener 1–80 caracteres.":
    "The label must have 1–80 characters.",
  "El color debe ser #RRGGBB o #RGB.": "The color must be #RRGGBB or #RGB.",
  "El puerto de referencia debe estar entre 1 y 65535.":
    "The port hint must be between 1 and 65535.",
};

export const errorDetails = (error: unknown): string => {
  let message = String(error);
  if (language.value === "en") {
    for (const [spanish, english] of Object.entries(backendErrors)) {
      message = message.replace(spanish, english);
    }
  }
  return message;
};

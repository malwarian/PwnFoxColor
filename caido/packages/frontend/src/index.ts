import { createApp } from "vue";

import { SDKPlugin } from "./plugins/sdk";
import type { FrontendSDK } from "./types";
import App from "./views/App.vue";
import SessionView from "./views/SessionView.vue";

export const init = (sdk: FrontendSDK) => {
  const app = createApp(App);

  app.use(SDKPlugin, sdk);

  const root = document.createElement("div");
  Object.assign(root.style, {
    height: "100%",
    width: "100%",
  });

  root.id = `plugin--pwnfox-color`;

  app.mount(root);

  sdk.navigation.addPage("/pwnfox-color", {
    body: root,
  });

  sdk.sidebar.registerItem("PwnFoxColor", "/pwnfox-color");

  sdk.httpHistory.addRequestViewMode({
    label: "Session",
    view: { component: SessionView },
  });
};

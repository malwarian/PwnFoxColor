import burp.api.montoya.utilities.json.JsonArrayNode;
import burp.api.montoya.utilities.json.JsonNode;
import burp.api.montoya.utilities.json.JsonObjectNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ProxyListenerConfig {
    private ProxyListenerConfig() {
    }

    static Prepared prepare(String exportedJson) {
        JsonObjectNode root = JsonNode.jsonNode(exportedJson).asObject();
        boolean nested = !root.hasObject("proxy") && root.hasObject("project_options");
        JsonObjectNode container = nested ? root.get("project_options").asObject() : root;
        if (!container.hasObject("proxy")) {
            throw new IllegalStateException("Burp no exportó la sección proxy del proyecto.");
        }
        JsonObjectNode proxy = container.get("proxy").asObject();
        if (!proxy.hasArray("request_listeners")) {
            throw new IllegalStateException("Burp no exportó la lista de proxy listeners.");
        }

        JsonArrayNode updatedListeners = JsonArrayNode.jsonArrayNode();
        Set<Integer> existingPorts = new HashSet<>();
        for (JsonNode listenerNode : proxy.get("request_listeners").asArray().asList()) {
            if (!listenerNode.isObject()) {
                throw new IllegalStateException("La configuración contiene un listener inválido.");
            }
            JsonObjectNode listener = listenerNode.asObject();
            Number port = listener.getNumber("listener_port");
            if (port == null || port.intValue() != port.longValue()) {
                throw new IllegalStateException("La configuración contiene un listener sin puerto válido.");
            }
            existingPorts.add(port.intValue());
            updatedListeners.add(listenerNode);
        }

        List<Integer> addedPorts = new ArrayList<>();
        for (PortRule rule : PwnFoxDefaults.all()) {
            if (existingPorts.add(rule.port())) {
                JsonObjectNode listener = JsonObjectNode.jsonObjectNode();
                listener.putString("certificate_mode", "per_host");
                listener.putString("listen_mode", "loopback_only");
                listener.putNumber("listener_port", rule.port());
                listener.putBoolean("running", true);
                updatedListeners.add(listener);
                addedPorts.add(rule.port());
            }
        }

        JsonObjectNode partialProxy = JsonObjectNode.jsonObjectNode();
        partialProxy.put("request_listeners", updatedListeners);
        JsonObjectNode payload = JsonObjectNode.jsonObjectNode();
        if (nested) {
            JsonObjectNode projectOptions = JsonObjectNode.jsonObjectNode();
            projectOptions.put("proxy", partialProxy);
            payload.put("project_options", projectOptions);
        } else {
            payload.put("proxy", partialProxy);
        }
        return new Prepared(payload.toJsonString(), List.copyOf(addedPorts));
    }

    static Set<Integer> ports(String exportedJson) {
        JsonObjectNode root = JsonNode.jsonNode(exportedJson).asObject();
        JsonObjectNode container = root.hasObject("proxy") ? root
                : root.get("project_options").asObject();
        JsonArrayNode listeners = container.get("proxy").asObject()
                .get("request_listeners").asArray();
        Set<Integer> ports = new HashSet<>();
        for (JsonNode listenerNode : listeners.asList()) {
            ports.add(listenerNode.asObject().getNumber("listener_port").intValue());
        }
        return ports;
    }

    record Prepared(String json, List<Integer> addedPorts) {
    }
}

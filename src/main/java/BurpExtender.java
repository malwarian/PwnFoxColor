import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.bambda.BambdaImportResult;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.persistence.Preferences;
import burp.api.montoya.proxy.http.InterceptedRequest;
import burp.api.montoya.proxy.http.ProxyRequestHandler;
import burp.api.montoya.proxy.http.ProxyRequestReceivedAction;
import burp.api.montoya.proxy.http.ProxyRequestToBeSentAction;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class BurpExtender implements BurpExtension, ProxyRequestHandler {
    private static final String SETTINGS_KEY = "pwnfox-color.rules.v2";
    private static final String OLD_SETTINGS_KEY = "pwnfox-color.rules.v1";
    private static final String LANGUAGE_KEY = "pwnfox-color.language.v1";

    private final AtomicReference<Map<Integer, PortRule>> rules =
            new AtomicReference<>(Map.of());
    private MontoyaApi api;
    private Preferences preferences;
    private String language = "en";

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName("PwnFoxColor");
        preferences = api.persistence().preferences();
        try {
            language = "es".equals(preferences.getString(LANGUAGE_KEY)) ? "es" : "en";
        } catch (RuntimeException error) {
            api.logging().logToError("PwnFoxColor: could not read language preference: " + error);
        }

        try {
            String saved = preferences.getString(SETTINGS_KEY);
            rules.set(index(saved != null
                    ? RuleCodec.decode(saved)
                    : RuleCodec.decodeLegacy(preferences.getString(OLD_SETTINGS_KEY))));
        } catch (RuntimeException error) {
            api.logging().logToError("PwnFoxColor: no se pudieron leer las reglas: " + error);
        }

        api.proxy().registerRequestHandler(this);
        SwingUtilities.invokeLater(() -> {
            PortColorTab tab = new PortColorTab(this);
            api.userInterface().applyThemeToComponent(tab);
            api.userInterface().registerSuiteTab("PwnFoxColor", tab);
        });
    }

    @Override
    public ProxyRequestReceivedAction handleRequestReceived(InterceptedRequest request) {
        int port = ListenerPorts.fromInterface(request.listenerInterface());
        PortRule rule = rules.get().get(port);
        if (rule == null) {
            return ProxyRequestReceivedAction.continueWith(request);
        }
        return ProxyRequestReceivedAction.continueWith(
                request, request.annotations().withHighlightColor(
                        HighlightColor.valueOf(rule.color())));
    }

    @Override
    public ProxyRequestToBeSentAction handleRequestToBeSent(InterceptedRequest request) {
        return ProxyRequestToBeSentAction.continueWith(request);
    }

    List<PortRule> currentRules() {
        List<PortRule> snapshot = new ArrayList<>(rules.get().values());
        snapshot.sort(Comparator.comparingInt(PortRule::port));
        return snapshot;
    }

    String language() {
        return language;
    }

    void saveLanguage(String value) {
        if (!"en".equals(value) && !"es".equals(value)) {
            throw new IllegalArgumentException("Unsupported language: " + value);
        }
        preferences.setString(LANGUAGE_KEY, value);
        language = value;
    }

    void saveRules(List<PortRule> updated) {
        Map<Integer, PortRule> indexed = index(updated);
        preferences.setString(SETTINGS_KEY, RuleCodec.encode(updated));
        rules.set(indexed);
    }

    void importSessionColumn(String source) {
        BambdaImportResult result = api.bambda().importBambda(
                BambdaGenerator.importableSessionColumn(source));
        if (result.status() != BambdaImportResult.Status.LOADED_WITHOUT_ERRORS) {
            String details = String.join("; ", result.importErrors());
            throw new IllegalStateException(details.isBlank()
                    ? "Burp rechazó el Bambda de la columna."
                    : details);
        }
    }

    int applyToExistingHistory() {
        Map<Integer, PortRule> snapshot = rules.get();
        int changed = 0;
        for (var entry : api.proxy().history()) {
            PortRule rule = snapshot.get(entry.listenerPort());
            if (rule != null) {
                entry.annotations().setHighlightColor(HighlightColor.valueOf(rule.color()));
                changed++;
            }
        }
        return changed;
    }

    ListenerCreationResult createPwnFoxListeners() {
        ProxyListenerConfig.Prepared prepared = ProxyListenerConfig.prepare(
                api.burpSuite().exportProjectOptionsAsJson());
        if (prepared.addedPorts().isEmpty()) {
            return new ListenerCreationResult(0, List.of());
        }
        api.burpSuite().importProjectOptionsFromJson(prepared.json());
        var configuredPorts = ProxyListenerConfig.ports(
                api.burpSuite().exportProjectOptionsAsJson());
        List<Integer> missing = prepared.addedPorts().stream()
                .filter(port -> !configuredPorts.contains(port)).toList();
        return new ListenerCreationResult(prepared.addedPorts().size(), missing);
    }

    record ListenerCreationResult(int requested, List<Integer> missingAfterImport) {
    }

    void logError(String message, Throwable error) {
        api.logging().logToError(message + ": " + error);
    }

    Color nativeColor(String name) {
        return api.userInterface().swingUtils().colorForHighLight(HighlightColor.valueOf(name));
    }

    private static Map<Integer, PortRule> index(List<PortRule> list) {
        Map<Integer, PortRule> indexed = new HashMap<>();
        for (PortRule rule : list) {
            if (indexed.putIfAbsent(rule.port(), rule) != null) {
                throw new IllegalArgumentException("Puerto repetido: " + rule.port());
            }
        }
        return Map.copyOf(indexed);
    }
}

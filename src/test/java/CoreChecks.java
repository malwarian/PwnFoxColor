import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CoreChecks {
    public static void main(String[] args) {
        List<PortRule> rules = List.of(
                new PortRule(8081, "Blue Container", "#0000FF"),
                new PortRule(8082, "Sesión 2: \"roja\"", "#FF0000"));
        check(RuleCodec.decode(RuleCodec.encode(rules)).equals(rules), "Persistencia de reglas");
        check(RuleCodec.decodeLegacy("8081\tBLUE\tQmx1ZSBDb250YWluZXI=\n")
                .equals(List.of(rules.get(0))), "Migración de reglas antiguas");
        check(ColorPalette.normalize("#abc").equals("#AABBCC"), "HEX abreviado");
        check(ColorPalette.nearestNative("#0000FF").equals("BLUE"), "Equivalencia nativa");
        check(ColorPalette.nearestNative("#6666FF").equals("BLUE"), "Azul claro conserva tono");
        check(ColorPalette.nearestNative("#9999FF").equals("BLUE"), "Azul pastel conserva tono");
        check(ColorPalette.nearestNative("#BDBDBD").equals("GRAY"), "Gris poco saturado");
        check(ColorPalette.nearestNative("#FFB6C1").equals("PINK"), "Rosa pálido");
        List<PortRule> defaults = PwnFoxDefaults.all();
        check(defaults.size() == 8, "Ocho colores PwnFox");
        for (int i = 0; i < defaults.size(); i++) {
            check(defaults.get(i).port() == 8081 + i, "Puertos PwnFox consecutivos");
        }
        Set<String> nativeColors = defaults.stream().map(PortRule::color)
                .collect(Collectors.toSet());
        check(nativeColors.size() == 8, "Resaltados PwnFox distintos en Burp");
        List<PortRule> missing = PwnFoxDefaults.missingFrom(
                List.of(new PortRule(8081, "Mi regla", "#123456")));
        check(missing.size() == 7 && missing.stream().noneMatch(rule -> rule.port() == 8081),
                "No sobrescribir reglas existentes");
        check(ListenerPorts.fromInterface("127.0.0.1:8081") == 8081, "Listener IPv4");
        check(ListenerPorts.fromInterface("[::1]:8082") == 8082, "Listener IPv6");
        check(ListenerPorts.fromInterface("sin-puerto") == -1, "Listener inválido");
        String script = BambdaGenerator.generate(rules);
        check(script.contains("case 8081: return \"Blue Container\";"), "Bambda para puerto 8081");
        check(script.contains("case 8082: return \"Sesión 2: \\\"roja\\\"\";"), "Escapado de Bambda");
        check(script.contains("default: return \"\";"), "Bambda sin coincidencia");
        String importable = BambdaGenerator.importableSessionColumn(script);
        check(importable.contains("function: CUSTOM_COLUMN\nlocation: PROXY_HTTP_HISTORY\n"),
                "Bambda importable para HTTP history");
        check(importable.contains("      case 8081: return \"Blue Container\";"),
                "Reglas incluidas en la importación");
        System.out.println("CoreChecks: OK");
    }

    private static void check(boolean condition, String name) {
        if (!condition) {
            throw new AssertionError(name);
        }
    }
}

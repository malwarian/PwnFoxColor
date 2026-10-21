import java.util.Comparator;
import java.util.List;

final class BambdaGenerator {
    private static final String SESSION_BAMBDA_ID = "e9f88d09-2a75-4cbb-972d-4940985c687f";

    private BambdaGenerator() {
    }

    static String generate(List<PortRule> rules) {
        StringBuilder code = new StringBuilder();
        code.append("switch (requestResponse.listenerPort()) {\n");
        rules.stream().filter(rule -> !rule.label().isBlank())
                .sorted(Comparator.comparingInt(PortRule::port))
                .forEach(rule -> code.append("    case ").append(rule.port())
                        .append(": return \"").append(escapeJava(rule.label())).append("\";\n"));
        code.append("    default: return \"\";\n}");
        return code.toString();
    }

    static String importableSessionColumn(String source) {
        StringBuilder bambda = new StringBuilder();
        bambda.append("id: ").append(SESSION_BAMBDA_ID).append('\n')
                .append("name: PwnFoxColor Session\n")
                .append("function: CUSTOM_COLUMN\n")
                .append("location: PROXY_HTTP_HISTORY\n")
                .append("source: |+\n");
        for (String line : source.split("\n", -1)) {
            bambda.append("  ").append(line).append('\n');
        }
        return bambda.toString();
    }

    static String escapeJava(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\%03o", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }
}

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

final class RuleCodec {
    private RuleCodec() {
    }

    static String encode(List<PortRule> rules) {
        StringBuilder result = new StringBuilder();
        for (PortRule rule : rules) {
            result.append(rule.port()).append('\t').append(rule.hex()).append('\t')
                    .append(Base64.getEncoder().encodeToString(
                            rule.label().getBytes(StandardCharsets.UTF_8)))
                    .append('\n');
        }
        return result.toString();
    }

    static List<PortRule> decode(String data) {
        if (data == null || data.isEmpty()) {
            return List.of();
        }
        List<PortRule> rules = new ArrayList<>();
        Set<Integer> ports = new java.util.HashSet<>();
        for (String line : data.split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split("\t", -1);
            if (fields.length != 3) {
                throw new IllegalArgumentException("Regla guardada con formato inválido.");
            }
            int port = Integer.parseInt(fields[0]);
            String label = new String(Base64.getDecoder().decode(fields[2]), StandardCharsets.UTF_8);
            PortRule rule = new PortRule(port, label, fields[1]);
            if (!ports.add(port)) {
                throw new IllegalArgumentException("Puerto repetido en las reglas guardadas: " + port);
            }
            rules.add(rule);
        }
        return List.copyOf(rules);
    }

    static List<PortRule> decodeLegacy(String data) {
        if (data == null || data.isEmpty()) {
            return List.of();
        }
        List<PortRule> rules = new ArrayList<>();
        for (String line : data.split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split("\t", -1);
            if (fields.length != 3) {
                throw new IllegalArgumentException("Regla antigua con formato inválido.");
            }
            String label = new String(Base64.getDecoder().decode(fields[2]), StandardCharsets.UTF_8);
            rules.add(new PortRule(Integer.parseInt(fields[0]), label,
                    ColorPalette.sampleHex(fields[1])));
        }
        return List.copyOf(rules);
    }
}

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class PwnFoxDefaults {
    private static final List<PortRule> RULES = List.of(
            new PortRule(8081, "PwnFox Blue", "#0A84FF"),
            new PortRule(8082, "PwnFox Turquoise", "#00FEFF"),
            new PortRule(8083, "PwnFox Green", "#30E60B"),
            new PortRule(8084, "PwnFox Yellow", "#FFE900"),
            new PortRule(8085, "PwnFox Orange", "#FF9400"),
            new PortRule(8086, "PwnFox Red", "#FF0039"),
            new PortRule(8087, "PwnFox Pink", "#FF1AD9"),
            new PortRule(8088, "PwnFox Purple", "#9400FF"));

    private PwnFoxDefaults() {
    }

    static List<PortRule> all() {
        return RULES;
    }

    static List<PortRule> missingFrom(List<PortRule> current) {
        Set<Integer> usedPorts = new HashSet<>();
        for (PortRule rule : current) {
            usedPorts.add(rule.port());
        }
        return RULES.stream().filter(rule -> !usedPorts.contains(rule.port())).toList();
    }
}

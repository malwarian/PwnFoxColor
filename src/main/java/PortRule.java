record PortRule(int port, String label, String hex) {
    PortRule {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("El puerto debe estar entre 1 y 65535.");
        }
        if (label == null || hex == null) {
            throw new IllegalArgumentException("Etiqueta y color HEX son obligatorios.");
        }
        hex = ColorPalette.normalize(hex);
    }

    String color() {
        return ColorPalette.nearestNative(hex);
    }
}

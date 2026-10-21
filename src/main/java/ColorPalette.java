import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

final class ColorPalette {
    private static final Map<String, String> NATIVE_HEX = new LinkedHashMap<>();

    static {
        NATIVE_HEX.put("RED", "#FF0000");
        NATIVE_HEX.put("ORANGE", "#FF8000");
        NATIVE_HEX.put("YELLOW", "#FFFF00");
        NATIVE_HEX.put("GREEN", "#00FF00");
        NATIVE_HEX.put("CYAN", "#00FFFF");
        NATIVE_HEX.put("BLUE", "#0000FF");
        NATIVE_HEX.put("PINK", "#FFB6C1");
        NATIVE_HEX.put("MAGENTA", "#FF00FF");
        NATIVE_HEX.put("GRAY", "#808080");
    }

    private ColorPalette() {
    }

    static String normalize(String input) {
        String value = input.trim().toUpperCase(java.util.Locale.ROOT);
        if (value.matches("#[0-9A-F]{3}")) {
            value = "#" + value.charAt(1) + value.charAt(1)
                    + value.charAt(2) + value.charAt(2)
                    + value.charAt(3) + value.charAt(3);
        }
        if (!value.matches("#[0-9A-F]{6}")) {
            throw new IllegalArgumentException("Usa un HEX #RRGGBB (o #RGB): " + input);
        }
        return value;
    }

    static Color asColor(String hex) {
        return Color.decode(normalize(hex));
    }

    static String sampleHex(String nativeColor) {
        String sample = NATIVE_HEX.get(nativeColor);
        if (sample == null) {
            throw new IllegalArgumentException("Color de Burp no válido: " + nativeColor);
        }
        return sample;
    }

    static String nearestNative(String hex) {
        Color desired = asColor(hex);
        float[] hsb = Color.RGBtoHSB(desired.getRed(), desired.getGreen(),
                desired.getBlue(), null);
        float hue = hsb[0] * 360;
        float saturation = hsb[1];

        if (saturation < 0.18f) {
            return "GRAY";
        }
        if (hue < 15 || hue >= 340) {
            return saturation < 0.55f ? "PINK" : "RED";
        }

        String[] names = {"RED", "ORANGE", "YELLOW", "GREEN", "CYAN", "BLUE", "MAGENTA", "PINK"};
        float[] hues = {0, 30, 60, 120, 180, 225, 275, 315};
        String nearest = "RED";
        float smallestDistance = Float.MAX_VALUE;
        for (int i = 0; i < names.length; i++) {
            float difference = Math.abs(hue - hues[i]);
            float distance = Math.min(difference, 360 - difference);
            if (distance < smallestDistance) {
                smallestDistance = distance;
                nearest = names[i];
            }
        }
        return nearest;
    }
}

/*
 * Convierte texto normal a distintos "estilos" usando bloques de caracteres
 * Unicode (negrita matematica, fraktur, con circulos, etc.) o marcas
 * combinantes (tachado, zalgo, etc.). No usa IA, todo es instantaneo y local.
 */
package org.dslul.openboard.inputmethod.latin;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public final class FontStyles {

    private FontStyles() {
        // No instanciable.
    }

    /** Un estilo simple con una base para A-Z y otra para a-z (formula de Unicode). */
    private static final class OffsetStyle {
        final int upperBase; // -1 si no soporta mayusculas
        final int lowerBase; // -1 si no soporta minusculas
        final int digitBase; // -1 si no soporta numeros
        final Map<Character, Integer> exceptions;

        OffsetStyle(final int upperBase, final int lowerBase, final int digitBase,
                final Map<Character, Integer> exceptions) {
            this.upperBase = upperBase;
            this.lowerBase = lowerBase;
            this.digitBase = digitBase;
            this.exceptions = exceptions;
        }
    }

    private static Map<Character, Integer> exc(final Object... pairs) {
        final Map<Character, Integer> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((Character) pairs[i], (Integer) pairs[i + 1]);
        }
        return map;
    }

    private static final Map<String, OffsetStyle> OFFSET_STYLES = new HashMap<>();
    static {
        OFFSET_STYLES.put("Bold", new OffsetStyle(0x1D400, 0x1D41A, 0x1D7CE, null));
        OFFSET_STYLES.put("Italic", new OffsetStyle(0x1D434, 0x1D44E, -1,
                exc('h', 0x210E)));
        OFFSET_STYLES.put("BoldItalic", new OffsetStyle(0x1D468, 0x1D482, -1, null));
        OFFSET_STYLES.put("Script", new OffsetStyle(0x1D49C, 0x1D4B6, -1,
                exc('B', 0x212C, 'E', 0x2130, 'F', 0x2131, 'H', 0x210B, 'I', 0x2110,
                        'L', 0x2112, 'M', 0x2133, 'R', 0x211B,
                        'e', 0x212F, 'g', 0x210A, 'o', 0x2134)));
        OFFSET_STYLES.put("BoldScript", new OffsetStyle(0x1D4D0, 0x1D4EA, -1, null));
        OFFSET_STYLES.put("Empire", new OffsetStyle(0x1D504, 0x1D51E, -1,
                exc('C', 0x212D, 'H', 0x210C, 'I', 0x2111, 'R', 0x211C, 'Z', 0x2128)));
        OFFSET_STYLES.put("Compire", new OffsetStyle(0x1D56C, 0x1D586, -1, null));
        OFFSET_STYLES.put("Premium", new OffsetStyle(0x1D538, 0x1D552, -1,
                exc('C', 0x2102, 'H', 0x210D, 'N', 0x2115, 'P', 0x2119, 'Q', 0x211A,
                        'R', 0x211D, 'Z', 0x2124)));
        OFFSET_STYLES.put("Sans", new OffsetStyle(0x1D5A0, 0x1D5BA, 0x1D7E2, null));
        OFFSET_STYLES.put("SansBold", new OffsetStyle(0x1D5D4, 0x1D5EE, -1, null));
        OFFSET_STYLES.put("SansItalic", new OffsetStyle(0x1D608, 0x1D622, -1, null));
        OFFSET_STYLES.put("Typewriter", new OffsetStyle(0x1D670, 0x1D68A, 0x1D7F6, null));
        OFFSET_STYLES.put("Wide", new OffsetStyle(0xFF21, 0xFF41, 0xFF10, null));
        OFFSET_STYLES.put("Circle", new OffsetStyle(0x24B6, 0x24D0, -1, null));
        // Estos 3 solo tienen version en Unicode para mayusculas: convertimos
        // el texto a mayusculas primero y luego aplicamos el estilo.
        OFFSET_STYLES.put("Box", new OffsetStyle(0x1F130, -1, -1, null));
        OFFSET_STYLES.put("Square", new OffsetStyle(0x1F170, -1, -1, null));
        OFFSET_STYLES.put("Round", new OffsetStyle(0x1F150, -1, -1, null));
        OFFSET_STYLES.put("Blue", new OffsetStyle(0x1F1E6, -1, -1, null));
    }

    // Estilos con tabla fija (no siguen una formula simple).
    private static final String[] SMALLCAP_LOWER_ARR = {
            "\u1D00", "\u0299", "\u1D04", "\u1D05", "\u1D07", "\uA730", "\u0262",
            "\u029C", "\u026A", "\u1D0A", "\u1D0B", "\u029F", "\u1D0D", "\u0274",
            "\u1D0F", "\u1D18", "q", "\u0280", "s", "t", "\u1D1C", "\u1D20",
            "\u1D21", "x", "\u028F", "\u1D22"};
    // Nota: q, s y x no tienen version oficial en Unicode, se dejan igual.

    private static final String[] SUPER_LOWER = {
            "\u1D43", "\u1D47", "\u1D9C", "\u1D48", "\u1D49", "\u1DA0", "\u1D4D",
            "\u02B0", "\u2071", "\u02B2", "\u1D4F", "\u02E1", "\u1D50", "\u207F",
            "\u1D52", "\u1D56", null, "\u02B3", "\u02E2", "\u1D57", "\u1D58",
            "\u1D5B", "\u02B7", "\u02E3", "\u02B8", "\u1DBB"};
    private static final String[] SUPER_UPPER = {
            "\u1D2C", "\u1D2E", null, "\u1D30", "\u1D31", null, "\u1D33", "\u1D34",
            "\u1D35", "\u1D36", "\u1D37", "\u1D38", "\u1D39", "\u1D3A", null,
            "\u1D3C", null, "\u1D3F", null, "\u1D40", "\u1D41", "\u2C7D", "\u1D42",
            null, null, null};
    private static final String[] SUPER_DIGIT = {
            "\u2070", "\u00B9", "\u00B2", "\u00B3", "\u2074", "\u2075", "\u2076",
            "\u2077", "\u2078", "\u2079"};

    private static final String[] SUB_LOWER = {
            "\u2090", null, null, null, "\u2091", null, null, "\u2095", "\u1D62",
            "\u2C7C", "\u2096", "\u2097", "\u2098", "\u2099", "\u2092", "\u209A",
            null, "\u1D63", "\u209B", "\u209C", "\u1D64", "\u1D65", null, "\u2093",
            null, null};
    private static final String[] SUB_DIGIT = {
            "\u2080", "\u2081", "\u2082", "\u2083", "\u2084", "\u2085", "\u2086",
            "\u2087", "\u2088", "\u2089"};

    private static final String[] REVERSE_LOWER = {
            "\u0250", "q", "\u0254", "p", "\u01DD", "\u025F", "\u0183", "\u0265",
            "\u1D09", "\u027E", "\u029E", "l", "\u026F", "u", "o", "d", "b",
            "\u0279", "s", "\u0287", "n", "\u028C", "\u028D", "x", "\u028E", "z"};
    private static final String[] REVERSE_UPPER = {
            "\u2200", "B", "\u0186", "D", "\u018E", "\u2132", "\u2141", "H", "I",
            "\u017F", "K", "\u02E5", "W", "N", "O", "\u0500", "Q", "\u1D1A", "S",
            "\u22A5", "\u2229", "\u039B", "M", "X", "\u2144", "Z"};

    /**
     * Convierte el texto al estilo indicado. Si un caracter no tiene version
     * en ese estilo, se deja igual (letras acentuadas, numeros, espacios, etc.).
     */
    public static String convert(final String text, final String styleKey) {
        switch (styleKey) {
            case "SmallCap":
                return applyLowerTable(text, SMALLCAP_LOWER_ARR);
            case "Superscript":
                return applyThreeTables(text, SUPER_UPPER, SUPER_LOWER, SUPER_DIGIT);
            case "Subscript":
                return applyThreeTables(text, null, SUB_LOWER, SUB_DIGIT);
            case "Reverse":
                return reverseStyle(text);
            case "StickyCaps":
                return stickyCaps(text);
            case "Strike":
                return combining(text, "\u0336");
            case "Crossed":
                return combining(text, "\u033D");
            case "Clouds":
                return combining(text, "\u0330");
            case "Happyface":
                return combining(text, "\u030A");
            case "Stop":
                return combining(text, "\u20E0");
            case "Triad":
                return combining(text, "\u20E4");
            case "Diamond":
                return combining(text, "\u20DF");
            case "Rect":
                return combining(text, "\u20DE");
            case "Devil":
                return zalgoStyle(text, 2);
            case "Zalgo":
                return zalgoStyle(text, 8);
            default:
                final OffsetStyle style = OFFSET_STYLES.get(styleKey);
                if (style == null) {
                    return text;
                }
                return applyOffsetStyle(text, style);
        }
    }

    /** Estilos que solo tienen mayusculas en Unicode: pasamos el texto a mayusculas primero. */
    private static boolean isUpperOnlyStyle(final String styleKey) {
        return "Box".equals(styleKey) || "Square".equals(styleKey)
                || "Round".equals(styleKey) || "Blue".equals(styleKey);
    }

    private static String applyOffsetStyle(final String text, final OffsetStyle style) {
        final String workingText = (style.lowerBase == -1) ? text.toUpperCase() : text;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < workingText.length(); i++) {
            final char c = workingText.charAt(i);
            final Integer exceptionCode = style.exceptions == null
                    ? null : style.exceptions.get(c);
            if (exceptionCode != null) {
                sb.appendCodePoint(exceptionCode);
            } else if (c >= 'A' && c <= 'Z' && style.upperBase != -1) {
                sb.appendCodePoint(style.upperBase + (c - 'A'));
            } else if (c >= 'a' && c <= 'z' && style.lowerBase != -1) {
                sb.appendCodePoint(style.lowerBase + (c - 'a'));
            } else if (c >= '0' && c <= '9' && style.digitBase != -1) {
                sb.appendCodePoint(style.digitBase + (c - '0'));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String applyLowerTable(final String text, final String[] lowerTable) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (c >= 'a' && c <= 'z') {
                sb.append(lowerTable[c - 'a']);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String applyThreeTables(final String text, final String[] upperTable,
            final String[] lowerTable, final String[] digitTable) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            String replacement = null;
            if (c >= 'A' && c <= 'Z' && upperTable != null) {
                replacement = upperTable[c - 'A'];
            } else if (c >= 'a' && c <= 'z' && lowerTable != null) {
                replacement = lowerTable[c - 'a'];
            } else if (c >= '0' && c <= '9' && digitTable != null) {
                replacement = digitTable[c - '0'];
            }
            sb.append(replacement != null ? replacement : String.valueOf(c));
        }
        return sb.toString();
    }

    private static String reverseStyle(final String text) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (c >= 'a' && c <= 'z') {
                sb.append(REVERSE_LOWER[c - 'a']);
            } else if (c >= 'A' && c <= 'Z') {
                sb.append(REVERSE_UPPER[c - 'A']);
            } else {
                sb.append(c);
            }
        }
        return sb.reverse().toString();
    }

    private static String stickyCaps(final String text) {
        final StringBuilder sb = new StringBuilder();
        boolean upper = true;
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (Character.isLetter(c)) {
                sb.append(upper ? Character.toUpperCase(c) : Character.toLowerCase(c));
                upper = !upper;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String combining(final String text, final String mark) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            sb.append(c);
            if (!Character.isWhitespace(c)) {
                sb.append(mark);
            }
        }
        return sb.toString();
    }

    private static final String[] ZALGO_MARKS = {
            "\u0301", "\u0302", "\u0303", "\u0308", "\u030A", "\u0316", "\u0317",
            "\u0323", "\u0324", "\u0325", "\u0330", "\u0338"};

    private static String zalgoStyle(final String text, final int intensity) {
        final Random random = new Random();
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            sb.append(c);
            if (!Character.isWhitespace(c)) {
                for (int m = 0; m < intensity; m++) {
                    sb.append(ZALGO_MARKS[random.nextInt(ZALGO_MARKS.length)]);
                }
            }
        }
        return sb.toString();
    }

    /** Nombre para mostrar en el panel, en el mismo orden que se listan los botones. */
    public static final String[] STYLE_KEYS_IN_ORDER = {
            "Premium", "Typewriter", "Bold", "Italic", "BoldItalic", "Sans",
            "SansBold", "SansItalic", "Script", "BoldScript", "Compire", "Empire",
            "SmallCap", "StickyCaps", "Wide", "Superscript", "Subscript",
            "Circle", "Box", "Square", "Round", "Blue",
            "Stop", "Triad", "Diamond", "Rect",
            "Reverse", "Strike", "Crossed", "Clouds", "Happyface", "Devil", "Zalgo"
    };

    public static final String[] STYLE_LABELS_IN_ORDER = {
            "Premium", "Typewriter", "Bold", "Italic", "Bold Italic", "Sans",
            "Sans Bold", "Sans Italic", "Script", "Bold Script", "Compire", "Empire",
            "Smallcap (solo minus.)", "StickyCaps", "Wide", "Superscript", "Subscript",
            "Circle", "Box (solo mayus.)", "Square (solo mayus.)", "Round (solo mayus.)",
            "Blue (solo mayus.)", "Stop", "Triad", "Diamond", "Rect",
            "Reverse", "Strike", "Crossed", "Clouds", "Happyface", "Devil", "Zalgo"
    };
}


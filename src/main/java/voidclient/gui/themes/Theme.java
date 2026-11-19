package voidclient.gui.themes;

/** Immutable colour palette. accent1..3 form the animated neon gradient. */
public record Theme(
        String name,
        int background,
        int surface,
        int surfaceHover,
        int stroke,
        int accent1,
        int accent2,
        int accent3,
        int text,
        int textDim,
        int success,
        int warning,
        int danger
) {
    public static final Theme CYBERPUNK = new Theme("Cyberpunk", 0xF205050D, 0xD90D0C1A, 0xE6181630, 0x40B026FF,
            0xFFB026FF, 0xFF00F0FF, 0xFFFF2BD6, 0xFFF4F1FF, 0xFF8E8AA8, 0xFF3CFFA0, 0xFFFFC93C, 0xFFFF3C6E);
    public static final Theme GALAXY = new Theme("Galaxy", 0xF2040616, 0xD90A0F2C, 0xE6141B44, 0x405B6CFF,
            0xFF5B6CFF, 0xFFB45CFF, 0xFF4FD1FF, 0xFFEFF2FF, 0xFF8890C0, 0xFF5CFFC8, 0xFFFFD36B, 0xFFFF5C8A);
    public static final Theme MINIMAL = new Theme("Minimal", 0xF20E0E10, 0xD9161619, 0xE6222227, 0x30FFFFFF,
            0xFFE8E8EE, 0xFFA0A0AE, 0xFFCFCFD8, 0xFFF0F0F2, 0xFF8A8A94, 0xFF7DDC9F, 0xFFE8C46A, 0xFFE87A7A);
    public static final Theme PLASMA = new Theme("Plasma", 0xF20A0406, 0xD9180A12, 0xE6261020, 0x40FF5E3A,
            0xFFFF5E3A, 0xFFFF2BD6, 0xFF2E7BFF, 0xFFFFF1EC, 0xFFB08A94, 0xFF4CFFB0, 0xFFFFC23C, 0xFFFF3C3C);
    public static final Theme AMOLED = new Theme("AMOLED", 0xFF000000, 0xF5050505, 0xF5101014, 0x3000E5FF,
            0xFF00E5FF, 0xFF2979FF, 0xFF7C4DFF, 0xFFFFFFFF, 0xFF7A7A85, 0xFF00E676, 0xFFFFD600, 0xFFFF1744);
}

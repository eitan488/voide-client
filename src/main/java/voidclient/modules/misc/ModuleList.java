package voidclient.modules.misc;

import net.minecraft.client.gui.DrawContext;
import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.HudModule;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.gui.animations.Animation;
import voidclient.gui.animations.Easing;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Animated list of enabled modules (arraylist). */
public class ModuleList extends HudModule {
    public final ModeSetting sort = add(new ModeSetting("Sort", "Ordering.", "Length", "Length", "Alphabetical", "Category"));
    public final ModeSetting colorMode = add(new ModeSetting("Color Mode", "Text colouring.", "Theme", "Theme", "Chroma", "Static", "Category"));
    public final ColorSetting staticColor = add(new ColorSetting("Static Color", "Colour for Static mode.", 0xFFB026FF).visibleWhen(() -> colorMode.is("Static")));
    public final BooleanSetting info = add(new BooleanSetting("Show Info", "Mode/value suffix.", true));
    public final BooleanSetting sideBar = add(new BooleanSetting("Side Bar", "Accent bar beside each entry.", true));
    public final BooleanSetting hideHud = add(new BooleanSetting("Hide HUD Modules", "Do not list HUD elements.", true));
    public final BooleanSetting lowercase = add(new BooleanSetting("Lowercase", "Lowercase names.", false));
    public final BooleanSetting rowBackground = add(new BooleanSetting("Row Background", "Background per row instead of one panel.", true));

    private final Map<Module, Animation> anims = new HashMap<>();

    public ModuleList() {
        super("Module List", "Animated list of active modules.", Category.MISC, 0.84, 0.40);
        alsoListIn(Category.VISUALS);
        background.defaultTo(false);
    }

    private String label(Module m) {
        String n = lowercase.get() ? m.getName().toLowerCase(Locale.ROOT) : m.getName();
        String i = info.get() ? m.getInfo() : null;
        return i == null ? n : n + " " + i;
    }

    private int color(Module m, int index) {
        return switch (colorMode.get()) {
            case "Chroma" -> ColorUtil.chroma(index * 0.04f, 0.7f, 1f);
            case "Static" -> staticColor.getColor();
            case "Category" -> ColorUtil.hsb(m.getCategory().ordinal() / 15f, 0.6f, 1f);
            default -> voidclient.gui.themes.ThemeManager.accent(index * 0.05f);
        };
    }

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        List<Module> list = new ArrayList<>();
        for (Module m : VoidClient.get().modules().all()) {
            Animation a = anims.computeIfAbsent(m, k -> new Animation(0, 260, Easing.CUBIC_OUT));
            boolean show = m.isEnabled() && m.isToggleable() && m.showInModuleList.get() && !(hideHud.get() && m instanceof HudModule) && m != this;
            a.target(show ? 1 : 0);
            if (a.get() > 0.01f) list.add(m);
        }
        if (list.isEmpty() && editor) list.add(this);
        Comparator<Module> cmp = switch (sort.get()) {
            case "Alphabetical" -> Comparator.comparing(Module::getName);
            case "Category" -> Comparator.comparing((Module m) -> m.getCategory().ordinal()).thenComparing(Module::getName);
            default -> Comparator.comparing((Module m) -> -textWidth(label(m)));
        };
        list.sort(cmp);
        float maxW = 0;
        for (Module m : list) maxW = Math.max(maxW, textWidth(label(m)));
        float lh = fontHeight() + 3;
        float w = maxW + 8, h = list.size() * lh + 2;
        size(w, Math.max(h, 10));
        boolean rightAligned = position.x() > 0.5;
        if (!rowBackground.get()) panel(ctx, 0, 0, w, h);
        float y = 1;
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            float a = m == this ? 1 : anims.get(m).get();
            String s = label(m);
            float tw = textWidth(s) + 6;
            float x = rightAligned ? w - tw * a : -tw * (1 - a);
            int c = color(m, i);
            if (rowBackground.get()) Render2D.rect(ctx, x, y, tw, lh * a, ColorUtil.multiplyAlpha(backgroundColor.getColor(), a));
            if (sideBar.get()) Render2D.rect(ctx, rightAligned ? w - 1.5f : x, y, 1.5f, lh * a, c);
            text(ctx, s, x + 3, y + 1.5f, ColorUtil.multiplyAlpha(c, a));
            y += lh * a;
        }
    }
}

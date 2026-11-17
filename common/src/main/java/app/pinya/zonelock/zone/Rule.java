package app.pinya.zonelock.zone;

import java.util.Locale;

/** Things a zone can forbid. Stored as a bit mask, so never reorder or remove entries; only append. */
public enum Rule {
    /** Players breaking blocks. */
    BREAK,
    /** Players placing blocks, fluids or entities such as boats and armor stands. */
    PLACE,
    /** Explosions destroying blocks. */
    EXPLOSIONS,
    /** Pistons moving blocks into, out of or inside the zone. */
    PISTONS,
    /** Fluids flowing across the zone border, and fire spreading or burning blocks. */
    FLUIDS_AND_FIRE,
    /** Mobs changing blocks (endermen, withers, ravagers, trampling farmland...). */
    MOB_GRIEFING,
    /** Ender pearls and chorus fruit landing inside the zone. */
    TELEPORT,
    /** Gliding with elytra and launching with riptide inside the zone. */
    FLIGHT,
    /** Item frames, paintings and armor stands being broken or emptied. */
    DECORATIONS,
    /** Tools modifying blocks: stripping logs, tilling, bone meal, flint and steel... */
    BLOCK_TOOLS;

    public int bit() {
        return 1 << ordinal();
    }

    public boolean in(int mask) {
        return (mask & bit()) != 0;
    }

    public int toggle(int mask) {
        return mask ^ bit();
    }

    public String translationKey() {
        return "rule.zonelock." + name().toLowerCase(Locale.ROOT);
    }

    /** Every rule on: the zone is fully sealed. */
    public static int all() {
        int mask = 0;
        for (Rule rule : values())
            mask |= rule.bit();
        return mask;
    }
}

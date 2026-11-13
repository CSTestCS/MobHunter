package com.mobhunter.profile;

import com.mobhunter.core.Element;
import com.mobhunter.core.Tier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * Everything MobHunter knows about one creature type.
 *
 * @param epithet     the name of its Deity form
 * @param signature   abilities unique to this creature, on top of its archetype set
 * @param movement    how it gets around (affects which abilities make sense)
 * @param contract    whether hunt contracts may target it
 */
public record CreatureProfile(Identifier id, Archetype archetype, Element element, int color, String epithet,
                              List<String> signature, Entrance entrance, Movement movement, boolean contract) {

    public enum Movement { GROUND, FLYING, SWIMMING, IMMOBILE }

    /** Abilities used at a given tier. Alphas get a reduced kit, Deities get eldritch extras. */
    public List<String> moveset(Tier tier) {
        List<String> list = new ArrayList<>();
        switch (tier) {
            case ALPHA -> {
                list.add("strike");
                if (!signature.isEmpty()) list.add(signature.get(0));
                for (String a : archetype.abilities) {
                    if (list.size() >= 3) break;
                    if (!list.contains(a) && !a.equals("summon_brood") && !a.equals("rampage")) list.add(a);
                }
            }
            case MONSTROUS -> {
                list.addAll(archetype.abilities);
                for (String s : signature) if (!list.contains(s)) list.add(s);
            }
            case DEITY -> {
                list.addAll(archetype.abilities);
                for (String s : signature) if (!list.contains(s)) list.add(s);
                list.add("meteor_rain");
                list.add("void_pull");
                list.add("eldritch_gaze");
                list.add("summon_alphas");
            }
            default -> { }
        }
        if (movement == Movement.IMMOBILE) list.removeIf(a -> a.equals("charge") || a.equals("leap_slam") || a.equals("pounce") || a.equals("lunge") || a.equals("torpedo") || a.equals("rampage"));
        if (movement == Movement.FLYING) list.removeIf(a -> a.equals("burrow_ambush"));
        return list;
    }
}

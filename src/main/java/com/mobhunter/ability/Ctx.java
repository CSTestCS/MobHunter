package com.mobhunter.ability;

import com.mobhunter.core.Element;
import com.mobhunter.core.Tier;
import com.mobhunter.profile.CreatureProfile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Per-use state of an ability. */
public final class Ctx {
    public final ServerLevel level;
    public final Mob mob;
    public LivingEntity target;
    public final Tier tier;
    public final CreatureProfile profile;
    public final Element element;
    public final float damageMult;
    public final RandomSource random;

    /** Where the attack is aimed. Follows the target until {@link #aimLocked}. */
    public Vec3 aim;
    public boolean aimLocked;
    public Vec3 dir = Vec3.ZERO;
    public final List<Vec3> marks = new ArrayList<>();
    public final Set<UUID> hit = new HashSet<>();
    public int counter;
    public boolean finished;
    public boolean savedNoGravity;
    public Object extra;

    public Ctx(ServerLevel level, Mob mob, LivingEntity target, Tier tier, CreatureProfile profile, float damageMult) {
        this.level = level;
        this.mob = mob;
        this.target = target;
        this.tier = tier;
        this.profile = profile;
        this.element = profile.element();
        this.damageMult = damageMult;
        this.random = mob.getRandom();
        this.aim = target.position();
    }

    /** Horizontal body size; used to scale reaches and radii with the creature. */
    public float size() {
        return mob.getBbWidth();
    }

    public float height() {
        return mob.getBbHeight();
    }

    public Vec3 pos() {
        return mob.position();
    }

    public Vec3 center() {
        return mob.position().add(0, mob.getBbHeight() * 0.5, 0);
    }

    public Vec3 facing() {
        float yaw = mob.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    public Vec3 flatDirToAim() {
        Vec3 d = aim.subtract(mob.position());
        d = new Vec3(d.x, 0, d.z);
        return d.lengthSqr() < 1.0E-4 ? facing() : d.normalize();
    }

    public void faceAim() {
        Vec3 d = aim.subtract(mob.position());
        if (d.x * d.x + d.z * d.z < 1.0E-4) return;
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f;
        mob.setYRot(yaw);
        mob.setYBodyRot(yaw);
        mob.setYHeadRot(yaw);
    }

    public int potency() {
        return tier.rank;
    }

    public float dmg(float base) {
        return base * damageMult;
    }
}

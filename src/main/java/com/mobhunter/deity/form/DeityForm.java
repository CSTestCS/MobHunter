package com.mobhunter.deity.form;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The body of a Deity: an eldritch arrangement of creature copies and block structures that expresses
 * the <em>concept</em> of the creature rather than a bigger version of it.
 *
 * @param concept one-line description shown when the Deity awakens
 * @param movement how the god moves
 * @param width    hitbox width (blocks)
 * @param height   hitbox height (blocks)
 */
public record DeityForm(String concept, Movement movement, float width, float height, List<Part> parts) {

    public enum Movement {
        /** Strides over the land. */
        GROUND,
        /** Hovers above its prey. */
        FLOAT,
        /** Rooted to its shrine; turns but never moves. */
        ANCHORED
    }

    public enum Layout {
        /** One copy at the centre. */
        SINGLE,
        /** Horizontal ring, copies run around it. */
        RING,
        /** Vertical wheel behind the body (a halo of copies). */
        WHEEL,
        /** Copies spread over a rotating sphere shell. */
        SPHERE,
        /** Copies orbit chaotically at different radii and heights. */
        SWARM,
        /** Copies stacked into a tower. */
        STACK,
        /** A stepped pyramid / throne of copies. */
        PYRAMID,
        /** A rising spiral, like a serpent or a vortex. */
        HELIX,
        /** Vertical columns standing in a ring. */
        SPIRES,
        /** A ring of upright pieces atop the body. */
        CROWN,
        /** Writhing chains that hang and sway. */
        TENTACLES,
        /** Arched chains that reach out and down to the ground. */
        LEGS,
        /** Vertical bars enclosing the body. */
        CAGE
    }

    /**
     * One piece of the body. Exactly one of {@code creature} / {@code block} is set.
     *
     * @param count  number of copies (or chains)
     * @param radius layout radius
     * @param scale  size of each copy (creatures: model scale; blocks: block size)
     * @param y      height of the layout centre above the feet
     * @param spin   rotation speed (degrees per tick)
     * @param length chain length in segments (chains, spires, stacks)
     */
    public record Part(@Nullable EntityType<?> creature, @Nullable BlockState block, Layout layout, int count, float radius,
                       float scale, float y, float spin, int length, @Nullable Consumer<Entity> setup) {
    }
}

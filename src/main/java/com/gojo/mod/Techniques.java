package com.gojo.mod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Server-side implementations of every technique. Each returns false if nothing happened.
 */
public final class Techniques {
    private static final DustParticleOptions BLUE_DUST = new DustParticleOptions(new Vector3f(0.15F, 0.45F, 1.0F), 2.5F);
    private static final DustParticleOptions RED_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 2.5F);
    private static final DustParticleOptions PURPLE_DUST = new DustParticleOptions(new Vector3f(0.6F, 0.1F, 1.0F), 3.0F);

    private static final double BLUE_RANGE = 12.0;
    private static final double BLUE_RADIUS = 10.0;
    private static final double RED_RANGE = 30.0;
    private static final double RED_RADIUS = 8.0;
    private static final int PURPLE_RANGE = 48;
    private static final int PURPLE_RADIUS = 2;
    private static final double VOID_RADIUS = 20.0;
    private static final double TELEPORT_RANGE = 40.0;
    private static final double SIX_EYES_RADIUS = 48.0;
    private static final double CURTAIN_RADIUS = 32.0;

    private Techniques() {
    }

    public static boolean infinity(ServerLevel level, ServerPlayer player) {
        boolean active = !CursedEnergy.isInfinity(player);
        CursedEnergy.setInfinity(player, active);
        player.displayClientMessage(Component.literal(active ? "Infinity: ON" : "Infinity: OFF")
                .withStyle(active ? ChatFormatting.AQUA : ChatFormatting.GRAY), true);
        playSound(level, player.position(), active ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE, 1.5F);
        return true;
    }

    public static boolean blue(ServerLevel level, ServerPlayer player) {
        Vec3 center = rayEnd(level, player, BLUE_RANGE);
        level.sendParticles(BLUE_DUST, center.x, center.y, center.z, 80, 0.7, 0.7, 0.7, 0.0);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 120, 3.0, 3.0, 3.0, 0.5);
        playSound(level, center, SoundEvents.WARDEN_SONIC_BOOM, 1.6F);

        DamageSource source = level.damageSources().indirectMagic(player, player);
        for (LivingEntity target : livingAround(level, player, center, BLUE_RADIUS)) {
            Vec3 pull = center.subtract(target.position().add(0, target.getBbHeight() / 2.0, 0));
            double distance = pull.length();
            if (distance > BLUE_RADIUS) {
                continue;
            }
            if (distance > 0.5) {
                target.setDeltaMovement(target.getDeltaMovement().scale(0.2).add(pull.normalize().scale(1.6)));
                target.hurtMarked = true;
            }
            if (distance < 3.0) {
                target.hurt(source, 10.0F);
            }
        }
        return true;
    }

    public static boolean red(ServerLevel level, ServerPlayer player) {
        Vec3 center = rayEnd(level, player, RED_RANGE);
        level.sendParticles(RED_DUST, center.x, center.y, center.z, 120, 1.2, 1.2, 1.2, 0.0);
        level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 60, 1.0, 1.0, 1.0, 0.3);
        level.explode(player, center.x, center.y, center.z, 3.5F, Level.ExplosionInteraction.BLOCK);

        DamageSource source = level.damageSources().indirectMagic(player, player);
        for (LivingEntity target : livingAround(level, player, center, RED_RADIUS)) {
            Vec3 away = target.position().subtract(center);
            Vec3 flat = new Vec3(away.x, 0, away.z);
            Vec3 dir = flat.lengthSqr() < 1.0E-4 ? player.getLookAngle() : flat.normalize();
            target.setDeltaMovement(dir.x * 2.8, 0.9, dir.z * 2.8);
            target.hurtMarked = true;
            if (away.length() < RED_RADIUS * 0.75) {
                target.hurt(source, 14.0F);
            }
        }
        return true;
    }

    public static boolean purple(ServerLevel level, ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Set<LivingEntity> hit = new HashSet<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int radiusSq = PURPLE_RADIUS * PURPLE_RADIUS + 1;

        playSound(level, player.position(), SoundEvents.END_PORTAL_SPAWN, 1.4F);
        for (int step = 3; step <= PURPLE_RANGE; step++) {
            Vec3 point = start.add(look.scale(step));
            level.sendParticles(PURPLE_DUST, point.x, point.y, point.z, 8, 0.8, 0.8, 0.8, 0.0);

            BlockPos center = BlockPos.containing(point);
            for (int dx = -PURPLE_RADIUS; dx <= PURPLE_RADIUS; dx++) {
                for (int dy = -PURPLE_RADIUS; dy <= PURPLE_RADIUS; dy++) {
                    for (int dz = -PURPLE_RADIUS; dz <= PURPLE_RADIUS; dz++) {
                        if (dx * dx + dy * dy + dz * dz > radiusSq) {
                            continue;
                        }
                        cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                        BlockState state = level.getBlockState(cursor);
                        // Negative destroy speed = unbreakable (bedrock, barriers, portals).
                        if (!state.isAir() && state.getDestroySpeed(level, cursor) >= 0) {
                            level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
            }
            hit.addAll(livingAround(level, player, point, PURPLE_RADIUS + 1.0));
        }

        DamageSource source = level.damageSources().indirectMagic(player, player);
        for (LivingEntity target : hit) {
            target.hurt(source, 1000.0F);
        }
        return true;
    }

    public static boolean unlimitedVoid(ServerLevel level, ServerPlayer player) {
        Vec3 center = player.position();
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 1.0, center.z, 500,
                VOID_RADIUS / 2.0, 4.0, VOID_RADIUS / 2.0, 0.02);
        level.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y + 1.0, center.z, 300,
                VOID_RADIUS / 2.0, 3.0, VOID_RADIUS / 2.0, 0.0);
        playSound(level, center, SoundEvents.END_PORTAL_SPAWN, 0.6F);

        List<LivingEntity> targets = livingAround(level, player, center, VOID_RADIUS);
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 9));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 9));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 4));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
        }
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
        player.displayClientMessage(Component.literal("Domain Expansion: Unlimited Void (" + targets.size() + " trapped)")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), true);
        return true;
    }

    public static boolean teleport(ServerLevel level, ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(TELEPORT_RANGE));
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 eyeTarget = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation().subtract(look.scale(0.6));

        // Convert eye position to feet position, then back off until the player fits.
        Vec3 feet = new Vec3(eyeTarget.x, eyeTarget.y - player.getEyeHeight(), eyeTarget.z);
        for (int i = 0; i < 10 && !fits(level, player, feet); i++) {
            feet = feet.subtract(look.scale(0.5));
        }
        if (!fits(level, player, feet)) {
            player.displayClientMessage(Component.literal("No room to teleport").withStyle(ChatFormatting.RED), true);
            return false;
        }

        Vec3 from = player.position();
        level.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1.0, from.z, 40, 0.4, 0.8, 0.4, 0.2);
        player.connection.teleport(feet.x, feet.y, feet.z, player.getYRot(), player.getXRot());
        player.fallDistance = 0.0F;
        level.sendParticles(ParticleTypes.PORTAL, feet.x, feet.y + 1.0, feet.z, 40, 0.4, 0.8, 0.4, 0.2);
        playSound(level, feet, SoundEvents.ENDERMAN_TELEPORT, 1.2F);
        return true;
    }

    public static boolean reverseCursedTechnique(ServerLevel level, ServerPlayer player) {
        player.heal(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
        player.clearFire();
        for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
            if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                player.removeEffect(effect.getEffect());
            }
        }
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));

        Vec3 pos = player.position();
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y + 1.0, pos.z, 30, 0.5, 0.8, 0.5, 0.0);
        playSound(level, pos, SoundEvents.PLAYER_LEVELUP, 1.4F);
        player.displayClientMessage(Component.literal("Reverse Cursed Technique").withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    public static boolean sixEyes(ServerLevel level, ServerPlayer player) {
        List<LivingEntity> sensed = livingAround(level, player, player.position(), SIX_EYES_RADIUS);
        for (LivingEntity target : sensed) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0, false, false));
        }
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 900, 0, false, false));
        playSound(level, player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F);
        player.displayClientMessage(Component.literal("Six Eyes: " + sensed.size() + " presences sensed")
                .withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    public static boolean curtain(ServerLevel level, ServerPlayer player) {
        level.setWeatherParameters(0, 6000, true, true);
        for (LivingEntity target : livingAround(level, player, player.position(), CURTAIN_RADIUS)) {
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 300, 0));
        }
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 600, 0, false, false));

        Vec3 pos = player.position();
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y + 6.0, pos.z, 300, 10.0, 2.0, 10.0, 0.0);
        playSound(level, pos, SoundEvents.WITHER_SPAWN, 0.5F);
        player.displayClientMessage(Component.literal("Emerge from the darkness, blacker than black...")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), true);
        return true;
    }

    /** Where the player's look ray hits a block, or the point at max range. */
    private static Vec3 rayEnd(ServerLevel level, ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    private static List<LivingEntity> livingAround(ServerLevel level, ServerPlayer caster, Vec3 center, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                entity -> entity != caster && entity.isAlive());
    }

    private static boolean fits(ServerLevel level, ServerPlayer player, Vec3 feet) {
        AABB box = player.getBoundingBox().move(feet.subtract(player.position()));
        return level.noCollision(player, box);
    }

    private static void playSound(ServerLevel level, Vec3 pos, SoundEvent sound, float pitch) {
        level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, 1.0F, pitch);
    }
}

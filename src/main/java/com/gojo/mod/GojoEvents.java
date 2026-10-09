package com.gojo.mod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = GojoMod.MODID)
public final class GojoEvents {
    private static final double INFINITY_RADIUS = 4.0;
    private static final float BLACK_FLASH_CHANCE = 0.12F;
    private static final float BLACK_FLASH_MULTIPLIER = 2.5F;
    private static final DustParticleOptions BLACK_DUST = new DustParticleOptions(new Vector3f(0.05F, 0.0F, 0.0F), 2.5F);
    private static final DustParticleOptions FLASH_RED_DUST = new DustParticleOptions(new Vector3f(0.9F, 0.0F, 0.05F), 1.5F);

    private GojoEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        CursedEnergy.regen(player);

        boolean infinity = CursedEnergy.isInfinity(player);
        if (infinity) {
            infinityField(player);
        }

        // Cursed energy HUD while holding a technique.
        if (player.tickCount % 20 == 0
                && (player.getMainHandItem().getItem() instanceof TechniqueItem
                || player.getOffhandItem().getItem() instanceof TechniqueItem)) {
            player.displayClientMessage(Component.literal("Cursed Energy: " + CursedEnergy.get(player) + " / " + CursedEnergy.MAX
                    + (infinity ? "  |  Infinity ON" : "")).withStyle(ChatFormatting.AQUA), true);
        }
    }

    /** Projectiles crawl to a halt and hostile mobs get pushed back near a player with Infinity. */
    private static void infinityField(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        AABB field = player.getBoundingBox().inflate(INFINITY_RADIUS);

        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, field, p -> p.getOwner() != player)) {
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(0.2));
            projectile.hurtMarked = true;
        }

        if (player.tickCount % 5 == 0) {
            for (Monster monster : level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(2.5))) {
                Vec3 away = monster.position().subtract(player.position());
                Vec3 flat = new Vec3(away.x, 0, away.z);
                if (flat.lengthSqr() < 1.0E-4) {
                    continue;
                }
                Vec3 dir = flat.normalize();
                monster.setDeltaMovement(dir.x * 0.6, 0.1, dir.z * 0.6);
                monster.hurtMarked = true;
            }
        }
    }

    /** Infinity: block anything caused by an entity, projectile, explosion, or fire. */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !CursedEnergy.isInfinity(player)) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        if (source.getEntity() != null
                || source.getDirectEntity() != null
                || source.is(DamageTypeTags.IS_PROJECTILE)
                || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
        }
    }

    /** Black Flash: melee hits with Infinity active have a chance to hit 2.5x harder. */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)
                || !source.is(DamageTypes.PLAYER_ATTACK)
                || !CursedEnergy.isInfinity(player)) {
            return;
        }
        if (player.getRandom().nextFloat() >= BLACK_FLASH_CHANCE) {
            return;
        }

        event.setAmount(event.getAmount() * BLACK_FLASH_MULTIPLIER);

        LivingEntity target = event.getEntity();
        ServerLevel level = player.serverLevel();
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() / 2.0;
        double z = target.getZ();
        level.sendParticles(BLACK_DUST, x, y, z, 50, 0.5, 0.5, 0.5, 0.0);
        level.sendParticles(FLASH_RED_DUST, x, y, z, 25, 0.6, 0.6, 0.6, 0.0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 30, 0.5, 0.5, 0.5, 0.4);
        level.playSound(null, x, y, z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.6F, 1.8F);
        player.displayClientMessage(Component.literal("BLACK FLASH!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
    }
}

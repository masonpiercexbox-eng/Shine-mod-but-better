package dev.shineenhanced;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Original, lightweight ambient particle effects. This intentionally uses
 * vanilla particles so the mod does not need custom texture assets.
 */
public final class ShineEnhancedClient implements ClientModInitializer {
    private int tick;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null || client.isPaused()) {
                return;
            }

            tick++;
            Player player = client.player;
            var level = client.level;
            var random = player.getRandom();

            // Gentle magical motes around the player, with a restrained spawn rate.
            if (random.nextInt(3) == 0) {
                double x = player.getX() + (random.nextDouble() - 0.5) * 9.0;
                double y = player.getY() + 0.6 + random.nextDouble() * 3.5;
                double z = player.getZ() + (random.nextDouble() - 0.5) * 9.0;
                level.addParticle(ParticleTypes.END_ROD, x, y, z,
                    (random.nextDouble() - 0.5) * 0.008, 0.006 + random.nextDouble() * 0.012,
                    (random.nextDouble() - 0.5) * 0.008);
            }

            // Tiny green glints evoke pollen and fireflies near grass and leaves.
            if (tick % 3 == 0 && random.nextInt(2) == 0) {
                double x = player.getX() + (random.nextDouble() - 0.5) * 6.0;
                double y = player.getY() + random.nextDouble() * 2.0;
                double z = player.getZ() + (random.nextDouble() - 0.5) * 6.0;
                level.addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0.0, 0.006, 0.0);
            }

            // Movement trails: subtle sparkle when sprinting or falling.
            if (player.isSprinting() && tick % 2 == 0) {
                Vec3 motion = player.getDeltaMovement();
                level.addParticle(ParticleTypes.CRIT,
                    player.getX() - motion.x * 0.6,
                    player.getY() + 0.15,
                    player.getZ() - motion.z * 0.6,
                    0.0, 0.015, 0.0);
            }

            // Water bubbles and small splashes when swimming.
            if (player.isInWater() && tick % 2 == 0) {
                level.addParticle(ParticleTypes.BUBBLE,
                    player.getX() + (random.nextDouble() - 0.5) * 0.7,
                    player.getY() + random.nextDouble() * 1.4,
                    player.getZ() + (random.nextDouble() - 0.5) * 0.7,
                    0.0, 0.025 + random.nextDouble() * 0.02, 0.0);
            }

            // Boat/watercraft trail: detect a ridden vehicle by its entity type name.
            Entity vehicle = player.getVehicle();
            if (vehicle != null && vehicle.getType().toString().toLowerCase().contains("boat")
                    && tick % 2 == 0) {
                Vec3 motion = vehicle.getDeltaMovement();
                double rearX = vehicle.getX() - motion.x * 1.2;
                double rearZ = vehicle.getZ() - motion.z * 1.2;
                level.addParticle(ParticleTypes.SPLASH, rearX, vehicle.getY() + 0.15, rearZ,
                    (random.nextDouble() - 0.5) * 0.08, 0.015, (random.nextDouble() - 0.5) * 0.08);
                if (random.nextInt(3) == 0) {
                    level.addParticle(ParticleTypes.BUBBLE, rearX, vehicle.getY() + 0.1, rearZ,
                        motion.x * -0.12, 0.025, motion.z * -0.12);
                }
            }
        });
    }
}

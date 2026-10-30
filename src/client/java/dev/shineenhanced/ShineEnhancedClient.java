package dev.shineenhanced;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Lightweight client-only particle effects using vanilla particles.
 * Spawn rates are deliberately capped to reduce visual clutter and tick work.
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
            Vec3 playerMotion = player.getDeltaMovement();
            double playerHorizontalSpeedSqr =
                playerMotion.x * playerMotion.x + playerMotion.z * playerMotion.z;

            // Ambient particles are infrequent and kept close to the player.
            if (tick % 5 == 0 && random.nextInt(2) == 0) {
                double x = player.getX() + (random.nextDouble() - 0.5) * 7.0;
                double y = player.getY() + 0.8 + random.nextDouble() * 2.5;
                double z = player.getZ() + (random.nextDouble() - 0.5) * 7.0;
                level.addParticle(ParticleTypes.END_ROD, x, y, z,
                    (random.nextDouble() - 0.5) * 0.006,
                    0.004 + random.nextDouble() * 0.008,
                    (random.nextDouble() - 0.5) * 0.006);
            }

            // Occasional green glints rather than a particle every few ticks.
            if (tick % 8 == 0 && random.nextInt(2) == 0) {
                double x = player.getX() + (random.nextDouble() - 0.5) * 5.0;
                double y = player.getY() + 0.5 + random.nextDouble() * 1.5;
                double z = player.getZ() + (random.nextDouble() - 0.5) * 5.0;
                level.addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0.0, 0.004, 0.0);
            }

            // Trail particles only while actually moving, not merely holding sprint.
            if (player.isSprinting() && playerHorizontalSpeedSqr > 0.015
                    && tick % 3 == 0) {
                level.addParticle(ParticleTypes.CRIT,
                    player.getX() - playerMotion.x * 0.6,
                    player.getY() + 0.15,
                    player.getZ() - playerMotion.z * 0.6,
                    0.0, 0.012, 0.0);
            }

            // Bubbles only while swimming and moving through water.
            if (player.isInWater() && playerHorizontalSpeedSqr > 0.01
                    && tick % 4 == 0) {
                level.addParticle(ParticleTypes.BUBBLE,
                    player.getX() + (random.nextDouble() - 0.5) * 0.6,
                    player.getY() + random.nextDouble() * 1.2,
                    player.getZ() + (random.nextDouble() - 0.5) * 0.6,
                    0.0, 0.02 + random.nextDouble() * 0.015, 0.0);
            }

            // Boat wake particles are emitted only when the ridden boat is moving.
            Entity vehicle = player.getVehicle();
            if (vehicle != null && vehicle.getType().toString().toLowerCase().contains("boat")) {
                Vec3 motion = vehicle.getDeltaMovement();
                double vehicleSpeedSqr = motion.x * motion.x + motion.z * motion.z;
                if (vehicleSpeedSqr > 0.005 && tick % 3 == 0) {
                    double rearX = vehicle.getX() - motion.x * 1.2;
                    double rearZ = vehicle.getZ() - motion.z * 1.2;
                    level.addParticle(ParticleTypes.SPLASH, rearX, vehicle.getY() + 0.12, rearZ,
                        (random.nextDouble() - 0.5) * 0.05, 0.01,
                        (random.nextDouble() - 0.5) * 0.05);
                    if (random.nextInt(4) == 0) {
                        level.addParticle(ParticleTypes.BUBBLE, rearX, vehicle.getY() + 0.08, rearZ,
                            motion.x * -0.1, 0.02, motion.z * -0.1);
                    }
                }
            }
        });
    }
}

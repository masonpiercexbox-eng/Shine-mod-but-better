package dev.shineenhanced;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** Client-side particle effects with an in-game settings screen. */
public final class ShineEnhancedClient implements ClientModInitializer {
    private int tick;
    private static final Properties CONFIG = new Properties();
    private static final Path CONFIG_PATH =
        FabricLoader.getInstance().getConfigDir().resolve("shine-enhanced.properties");

    private static boolean ambient = true;
    private static boolean greenGlints = true;
    private static boolean sprintTrail = true;
    private static boolean swimBubbles = true;
    private static boolean boatWake = true;
    // 0 = low, 1 = medium, 2 = high
    private static int amount = 1;

    private static KeyMapping menuKey;

    @Override
    public void onInitializeClient() {
        loadConfig();
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.shine_enhanced.open_menu", GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.shine_enhanced"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.consumeClick()) {
                if (client.player != null) {
                    client.setScreen(new SettingsScreen());
                }
            }
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
            int interval = amount == 0 ? 2 : amount == 1 ? 1 : 1;
            boolean extraRoll = amount == 2 || tick % 2 == 0;

            if (ambient && tick % (5 / interval) == 0 && extraRoll && random.nextInt(amount == 2 ? 1 : 2) == 0) {
                double x = player.getX() + (random.nextDouble() - 0.5) * 7.0;
                double y = player.getY() + 0.8 + random.nextDouble() * 2.5;
                double z = player.getZ() + (random.nextDouble() - 0.5) * 7.0;
                level.addParticle(ParticleTypes.END_ROD, x, y, z,
                    (random.nextDouble() - 0.5) * 0.006,
                    0.004 + random.nextDouble() * 0.008,
                    (random.nextDouble() - 0.5) * 0.006);
            }

            if (greenGlints && tick % (amount == 2 ? 4 : 8) == 0
                    && random.nextInt(amount == 2 ? 1 : 2) == 0) {
                double x = player.getX() + (random.nextDouble() - 0.5) * 5.0;
                double y = player.getY() + 0.5 + random.nextDouble() * 1.5;
                double z = player.getZ() + (random.nextDouble() - 0.5) * 5.0;
                level.addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0.0, 0.004, 0.0);
            }

            if (sprintTrail && player.isSprinting() && playerHorizontalSpeedSqr > 0.015
                    && tick % (amount == 2 ? 2 : 3) == 0) {
                level.addParticle(ParticleTypes.CRIT,
                    player.getX() - playerMotion.x * 0.6,
                    player.getY() + 0.15,
                    player.getZ() - playerMotion.z * 0.6,
                    0.0, 0.012, 0.0);
            }

            if (swimBubbles && player.isInWater() && playerHorizontalSpeedSqr > 0.01
                    && tick % (amount == 2 ? 2 : 4) == 0) {
                level.addParticle(ParticleTypes.BUBBLE,
                    player.getX() + (random.nextDouble() - 0.5) * 0.6,
                    player.getY() + random.nextDouble() * 1.2,
                    player.getZ() + (random.nextDouble() - 0.5) * 0.6,
                    0.0, 0.02 + random.nextDouble() * 0.015, 0.0);
            }

            Entity vehicle = player.getVehicle();
            if (boatWake && vehicle != null && vehicle.getType().toString().toLowerCase().contains("boat")) {
                Vec3 motion = vehicle.getDeltaMovement();
                double vehicleSpeedSqr = motion.x * motion.x + motion.z * motion.z;
                if (vehicleSpeedSqr > 0.005 && tick % (amount == 2 ? 2 : 3) == 0) {
                    double rearX = vehicle.getX() - motion.x * 1.2;
                    double rearZ = vehicle.getZ() - motion.z * 1.2;
                    level.addParticle(ParticleTypes.SPLASH, rearX, vehicle.getY() + 0.12, rearZ,
                        (random.nextDouble() - 0.5) * 0.05, 0.01,
                        (random.nextDouble() - 0.5) * 0.05);
                    if (random.nextInt(amount == 2 ? 2 : 4) == 0) {
                        level.addParticle(ParticleTypes.BUBBLE, rearX, vehicle.getY() + 0.08, rearZ,
                            motion.x * -0.1, 0.02, motion.z * -0.1);
                    }
                }
            }
        });
    }

    private static void loadConfig() {
        if (!Files.exists(CONFIG_PATH)) return;
        try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
            CONFIG.load(in);
            ambient = Boolean.parseBoolean(CONFIG.getProperty("ambient", "true"));
            greenGlints = Boolean.parseBoolean(CONFIG.getProperty("greenGlints", "true"));
            sprintTrail = Boolean.parseBoolean(CONFIG.getProperty("sprintTrail", "true"));
            swimBubbles = Boolean.parseBoolean(CONFIG.getProperty("swimBubbles", "true"));
            boatWake = Boolean.parseBoolean(CONFIG.getProperty("boatWake", "true"));
            amount = Math.max(0, Math.min(2, Integer.parseInt(CONFIG.getProperty("amount", "1"))));
        } catch (IOException | NumberFormatException ignored) {
            // Defaults are used if the configuration file cannot be read.
        }
    }

    private static void saveConfig() {
        CONFIG.setProperty("ambient", Boolean.toString(ambient));
        CONFIG.setProperty("greenGlints", Boolean.toString(greenGlints));
        CONFIG.setProperty("sprintTrail", Boolean.toString(sprintTrail));
        CONFIG.setProperty("swimBubbles", Boolean.toString(swimBubbles));
        CONFIG.setProperty("boatWake", Boolean.toString(boatWake));
        CONFIG.setProperty("amount", Integer.toString(amount));
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
                CONFIG.store(out, "Shine Enhanced settings");
            }
        } catch (IOException ignored) {
            // The mod remains usable for this session if saving fails.
        }
    }

    private static final class SettingsScreen extends Screen {
        protected SettingsScreen() {
            super(Component.literal("Shine Enhanced Settings"));
        }

        @Override
        protected void init() {
            int center = this.width / 2;
            int left = center - 105;
            int y = this.height / 4;
            int gap = 26;
            addRenderableWidget(toggleButton("Ambient sparkles", () -> ambient,
                value -> ambient = value, left, y));
            addRenderableWidget(toggleButton("Green glints", () -> greenGlints,
                value -> greenGlints = value, left, y + gap));
            addRenderableWidget(toggleButton("Sprint trail", () -> sprintTrail,
                value -> sprintTrail = value, left, y + gap * 2));
            addRenderableWidget(toggleButton("Swimming bubbles", () -> swimBubbles,
                value -> swimBubbles = value, left, y + gap * 3));
            addRenderableWidget(toggleButton("Boat wake", () -> boatWake,
                value -> boatWake = value, left, y + gap * 4));
            addRenderableWidget(Button.builder(amountLabel(), button -> {
                amount = (amount + 1) % 3;
                button.setMessage(amountLabel());
                saveConfig();
            }).bounds(left, y + gap * 5, 210, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Done"), button -> {
                saveConfig();
                Minecraft.getInstance().setScreen(null);
            }).bounds(left, y + gap * 6, 210, 20).build());
        }

        private Button toggleButton(String name, java.util.function.BooleanSupplier getter,
                                    java.util.function.Consumer<Boolean> setter, int x, int y) {
            Button button = Button.builder(toggleLabel(name, getter.getAsBoolean()), b -> {
                setter.accept(!getter.getAsBoolean());
                b.setMessage(toggleLabel(name, getter.getAsBoolean()));
                saveConfig();
            }).bounds(x, y, 210, 20).build();
            return button;
        }

        private static Component toggleLabel(String name, boolean enabled) {
            return Component.literal(name + ": " + (enabled ? "ON" : "OFF"));
        }

        private static Component amountLabel() {
            return Component.literal("Particle amount: " +
                (amount == 0 ? "Low" : amount == 1 ? "Medium" : "High"));
        }

        @Override
        public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics, mouseX, mouseY, partialTick);
            graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
            graphics.drawCenteredString(this.font, "Right Shift opens this menu", this.width / 2,
                this.height - 18, 0xAAAAAA);
            super.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}

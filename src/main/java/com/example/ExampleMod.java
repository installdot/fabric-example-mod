package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class AutoAimMod implements ClientModInitializer {
    private static KeyBinding toggleKey;
    private static boolean enabled = false;
    private static boolean wasJumping = false;
    private static MinecraftClient mc;

    @Override
    public void onInitializeClient() {
        mc = MinecraftClient.getInstance();
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autoaim.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.autoaim"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                enabled = !enabled;
                if (mc.player != null) {
                    mc.player.sendMessage(Text.literal("AutoAim: " + (enabled ? "ON" : "OFF")), true);
                }
            }

            if (enabled && mc.player != null && mc.world != null && mc.interactionManager != null) {
                PlayerEntity nearest = null;
                double minDist = 4.0;
                Vec3d pos = mc.player.getPos();

                for (PlayerEntity player : mc.world.getPlayers()) {
                    if (player != mc.player) {
                        double dist = pos.distanceTo(player.getPos());
                        if (dist < minDist) {
                            minDist = dist;
                            nearest = player;
                        }
                    }
                }

                boolean jumping = mc.options.jumpKey.isPressed();
                if (jumping && !wasJumping && nearest != null) {
                    mc.interactionManager.attackEntity(mc.player, nearest);
                }
                wasJumping = jumping;

                if (nearest != null) {
                    Vec3d eyePos = mc.player.getEyePos();
                    Vec3d targetPos = nearest.getPos().add(0, nearest.getHeight() / 2.0, 0);
                    Vec3d direction = targetPos.subtract(eyePos);

                    double horizontalDistance = MathHelper.sqrt((float) (direction.x * direction.x + direction.z * direction.z));
                    float yaw = (float) (MathHelper.atan2(direction.z, direction.x) * 180.0 / Math.PI) - 90.0f;
                    float pitch = (float) -(MathHelper.atan2(direction.y, horizontalDistance) * 180.0 / Math.PI);

                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGroundPacket(yaw, pitch, mc.player.isOnGround()));
                }
            } else {
                wasJumping = false;
            }
        });
    }
}

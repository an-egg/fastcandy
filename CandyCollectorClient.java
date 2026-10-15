package com.example.candymod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class CandyCollectorClient implements ClientModInitializer {

    private static KeyBinding triggerKey;
    private static boolean waitingForGui = false;
    private static int delayTicks = 0; 
    private static final int DELAY_TARGET = 2; // Đợi ~0.1s để server phản hồi GUI

    @Override
    public void onInitializeClient() {
        triggerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.candymod.collect",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.candymod.general"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            while (triggerKey.wasPressed()) {
                magneticCollect(client);
            }

            if (waitingForGui) {
                if (client.player.currentScreenHandler instanceof GenericContainerScreenHandler handler) {
                    delayTicks++;

                    if (delayTicks >= DELAY_TARGET) {
                        for (Slot slot : handler.slots) {
                            ItemStack stack = slot.getStack();
                            if (stack.isOf(Items.GOLDEN_CARROT)) {
                                client.interactionManager.clickSlot(
                                        handler.syncId,
                                        slot.id,
                                        0,
                                        SlotActionType.PICKUP,
                                        client.player
                                );
                                break;
                            }
                        }
                        waitingForGui = false;
                        delayTicks = 0;
                    }
                }
            }
        });
    }

    private void magneticCollect(MinecraftClient client) {
        BlockPos playerPos = client.player.getBlockPos();
        BlockPos targetPumpkin = null;

        for (BlockPos pos : BlockPos.iterate(playerPos.add(-4, -2, -4), playerPos.add(4, 2, 4))) {
            var state = client.world.getBlockState(pos);
            if (state.isOf(Blocks.CARVED_PUMPKIN) || state.isOf(Blocks.PUMPKIN) || state.isOf(Blocks.JACK_O_LANTERN)) {
                targetPumpkin = pos.toImmutable();
                break;
            }
        }

        if (targetPumpkin != null) {
            Vec3d hitVec = Vec3d.ofCenter(targetPumpkin);
            BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, targetPumpkin, false);
            client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
            
            waitingForGui = true;
            delayTicks = 0;
        }
    }
}

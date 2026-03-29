package com.ifamishedx.tridentchanger.mixin;

import com.ifamishedx.tridentchanger.InventoryChangedCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts every {@link PlayerInventory#setStack(int, ItemStack)} call on the
 * server side and fires {@link InventoryChangedCallback#EVENT}.
 *
 * <p>The injection point is TAIL so the new stack is already committed when
 * listeners run. Listeners that call {@code inventory.setStack} again will
 * trigger a second injection, but because the replacement stack is never a
 * Gungnir, {@link com.ifamishedx.tridentchanger.GungnirNormalizationHandler#isGungnir}
 * returns {@code false} immediately and no further action is taken.</p>
 */
@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {

    @Shadow
    public PlayerEntity player;

    @Inject(method = "setStack", at = @At("TAIL"))
    private void tridentchanger$onSetStack(int slot, ItemStack stack, CallbackInfo ci) {
        if (this.player instanceof ServerPlayerEntity serverPlayer) {
            PlayerInventory inventory = (PlayerInventory) (Object) this;
            InventoryChangedCallback.EVENT.invoker()
                    .onInventoryChanged(serverPlayer, inventory, slot, stack);
        }
    }
}

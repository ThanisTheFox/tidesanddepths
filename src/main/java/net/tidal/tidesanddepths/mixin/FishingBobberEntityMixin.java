package net.tidal.tidesanddepths.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingBobberEntity.class)
public class FishingBobberEntityMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onUse(ItemStack usedItem, CallbackInfoReturnable<Integer> cir) {
        FishingBobberEntity bobber = (FishingBobberEntity)(Object)this;

        if (!bobber.getWorld().isClient) {
            PlayerEntity player = bobber.getPlayerOwner();

            if (player != null) {
                // Chat Nachricht senden
                player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                        (syncId, inventory, p) -> new FishingGameScreenHandler(syncId, inventory),
                        Text.literal("Angel GUI")
                ));

                // Loot verhindern / Standardverhalten stoppen
                cir.cancel();

                // Optional: Bobber entfernen
                bobber.discard();
            }
        }
    }
}
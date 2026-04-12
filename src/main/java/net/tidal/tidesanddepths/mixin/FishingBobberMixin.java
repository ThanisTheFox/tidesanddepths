package net.tidal.tidesanddepths.mixin;

import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.tidal.tidesanddepths.fishing.FishingBobberAccess;
import net.tidal.tidesanddepths.fishing.FishingMinigameManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberMixin implements FishingBobberAccess {
    @Shadow private boolean caughtFish;
    @Shadow @Final private static TrackedData<Boolean> CAUGHT_FISH;

    @Shadow public abstract PlayerEntity getPlayerOwner();

    @Unique
    private boolean tidesanddepths$lastCaughtFish;

    @Inject(method = "tick", at = @At("TAIL"))
    private void tidesanddepths$startMinigameOnBite(CallbackInfo ci) {
        FishingBobberEntity bobber = (FishingBobberEntity) (Object) this;
        if (bobber.getWorld().isClient()) {
            return;
        }

        if (this.caughtFish && !this.tidesanddepths$lastCaughtFish && this.getPlayerOwner() instanceof ServerPlayerEntity player) {
            if (FishingMinigameManager.tryStartChallenge(player, bobber)) {
                this.caughtFish = false;
            }
        }

        this.tidesanddepths$lastCaughtFish = this.caughtFish;
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void tidesanddepths$blockVanillaCatchUntilResolved(ItemStack usedItem, CallbackInfoReturnable<Integer> cir) {
        FishingBobberEntity bobber = (FishingBobberEntity) (Object) this;
        if (this.getPlayerOwner() instanceof ServerPlayerEntity player) {
            if (FishingMinigameManager.shouldBlockReel(player, bobber)) {
                cir.setReturnValue(0);
            }
        }
    }

    @Inject(method = "onRemoved", at = @At("TAIL"))
    private void tidesanddepths$cleanupChallenge(CallbackInfo ci) {
        FishingBobberEntity bobber = (FishingBobberEntity) (Object) this;
        if (!bobber.getWorld().isClient()) {
            FishingMinigameManager.clearForBobber(bobber.getId());
        }
    }

    @Override
    public void tidesanddepths$setCaughtFish(boolean caughtFish) {
        this.caughtFish = caughtFish;
        ((FishingBobberEntity) (Object) this).getDataTracker().set(CAUGHT_FISH, caughtFish);
        this.tidesanddepths$lastCaughtFish = caughtFish;
    }
}

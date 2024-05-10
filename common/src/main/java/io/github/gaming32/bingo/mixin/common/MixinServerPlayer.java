package io.github.gaming32.bingo.mixin.common;

import io.github.gaming32.bingo.Bingo;
import io.github.gaming32.bingo.game.PlayerTracker;
import io.github.gaming32.bingo.triggers.BingoTriggers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.stream.Stream;

import static net.minecraft.core.component.DataComponents.ITEM_NAME;
import static net.minecraft.core.component.DataComponents.CUSTOM_DATA;

@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayer extends MixinPlayer {
    @Inject(
        method = "doTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/game/ClientboundSetExperiencePacket;<init>(FII)V"
        )
    )
    private void experienceChanged(CallbackInfo ci) {
        BingoTriggers.EXPERIENCE_CHANGED.get().trigger((ServerPlayer)(Object)this);
    }

    @Inject(method = "onItemPickup", at = @At("TAIL"))
    private void itemPickedUpTrigger(ItemEntity itemEntity, CallbackInfo ci) {
        BingoTriggers.ITEM_PICKED_UP.get().trigger((ServerPlayer)(Object)this, itemEntity);
    }

    @Inject(method = "awardKillScore", at = @At("HEAD"))
    @SuppressWarnings("UnreachableCode")
    private void killSelfTrigger(Entity killed, int scoreValue, DamageSource source, CallbackInfo ci) {
        if (killed == (Object)this) {
            BingoTriggers.KILL_SELF.get().trigger((ServerPlayer)killed, source);
        }
    }

    @Inject(method = "die", at = @At("RETURN"))
    private void deathTrigger(DamageSource damageSource, CallbackInfo ci) {
        BingoTriggers.DEATH.get().trigger((ServerPlayer)(Object)this, damageSource);
    }

    @Inject(
        method = "checkMovementStatistics",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/stats/Stats;CROUCH_ONE_CM:Lnet/minecraft/resources/ResourceLocation;"
        )
    )
    @SuppressWarnings("UnreachableCode")
    private void sneakingTrigger(double distanceX, double distanceY, double distanceZ, CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayer serverPlayer) {
            if (bingo$startSneakingPos == null) {
                Bingo.LOGGER.warn("bingo$startSneakingPos was null but player was sneaking");
            } else {
                BingoTriggers.CROUCH.get().trigger(serverPlayer, bingo$startSneakingPos);
            }
        }
    }

    @Inject(method = "doTick", at = @At("TAIL"))
    private void ensurePlayerTracker(CallbackInfo ci) {
        PlayerTracker playerTracker = PlayerTracker.getInstance();
        if (!playerTracker.enabled)
            return;
        Stream<ItemStack> allInventoryItems = Stream.concat(this.getInventory().items.stream(), Stream.of(this.containerMenu.getCarried()));
        boolean hasCompass = allInventoryItems.anyMatch(itemStack -> {
            if (!(itemStack.getItem() instanceof CompassItem))
                return false;
            CustomData customData = itemStack.get(CUSTOM_DATA);
            if (customData == null)
                return false;
            return customData.copyTag().getBoolean("bingo:player_tracker");
        });
        if (!hasCompass) {
            ItemStack newCompass = new ItemStack(Items.COMPASS);
            Component itemName = Component.literal("Player Tracker");
            newCompass.set(ITEM_NAME, itemName);
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("bingo:player_tracker", true);
            newCompass.set(CUSTOM_DATA, CustomData.of(tag));
            this.addItem(newCompass);
        }
    }
}

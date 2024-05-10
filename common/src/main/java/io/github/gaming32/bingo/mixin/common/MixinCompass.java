package io.github.gaming32.bingo.mixin.common;

import io.github.gaming32.bingo.game.PlayerTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

import static net.minecraft.core.component.DataComponents.CUSTOM_DATA;
import static net.minecraft.core.component.DataComponents.LODESTONE_TRACKER;

@Mixin(CompassItem.class)
public class MixinCompass {
    @Inject(method = "inventoryTick", at = @At("HEAD"))
    private void onInventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected, CallbackInfo ci) {
        if (entity instanceof ServerPlayer serverPlayer) {
            CustomData customData = stack.get(CUSTOM_DATA);
            if (customData == null)
                return;
            if (!customData.copyTag().getBoolean("bingo:player_tracker"))
                return;
            PlayerTracker playerTracker = PlayerTracker.getInstance();
            if (!playerTracker.enabled)
                return;
            ServerPlayer nowTracking = playerTracker.trackedPlayers.get(serverPlayer);
            if (nowTracking == null)
                return;
            BlockPos blockPos = nowTracking.getOnPos();
            LodestoneTracker lodestoneTracker = new LodestoneTracker(Optional.of(GlobalPos.of(nowTracking.level().dimension(), blockPos)), false);
            stack.set(LODESTONE_TRACKER, lodestoneTracker);
        }
    }
}

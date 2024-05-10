package io.github.gaming32.bingo.mixin.common;

import io.github.gaming32.bingo.Bingo;
import io.github.gaming32.bingo.game.PlayerTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.minecraft.core.component.DataComponents.CUSTOM_DATA;

@Mixin(Item.class)
public class MixinCompassSuper {
    @Inject(method = "use", at = @At("HEAD"))
    private void onUse(Level level, Player player, InteractionHand usedHand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack item = player.getItemInHand(usedHand);
        if (player instanceof ServerPlayer serverPlayer && item.getItem() instanceof CompassItem) {
            CustomData customData = item.get(CUSTOM_DATA);
            if (customData == null)
                return;
            if (!customData.copyTag().getBoolean("bingo:player_tracker"))
                return;
            PlayerTracker playerTracker = PlayerTracker.getInstance();
            if (!playerTracker.enabled)
                return;
            ServerPlayer currentlyTracking = playerTracker.trackedPlayers.get(serverPlayer);
            List<ServerPlayer> players = new ArrayList<>(Objects.requireNonNull(level.getServer()).getPlayerList().getPlayers());
            // players.remove(player);
            int currentlyTrackingIdx = players.indexOf(currentlyTracking);
            int nowTrackingIdx = (currentlyTrackingIdx + 1) % players.size();
            ServerPlayer nowTracking = players.get(nowTrackingIdx);
            playerTracker.trackedPlayers.put(serverPlayer, nowTracking);
            Component message = Bingo.translatable("bingo.now_tracking", Objects.requireNonNull(nowTracking.getDisplayName()).getString()).withStyle(ChatFormatting.GOLD);
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
        }
    }
}

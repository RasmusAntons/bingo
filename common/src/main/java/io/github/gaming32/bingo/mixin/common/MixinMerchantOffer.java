package io.github.gaming32.bingo.mixin.common;

import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.core.component.DataComponents.CUSTOM_DATA;

@Mixin(MerchantOffer.class)
public class MixinMerchantOffer {
    @Inject(method = "satisfiedBy", at = @At("HEAD"), cancellable = true)
    private void onSatisfiedBy(ItemStack playerOfferA, ItemStack playerOfferB, CallbackInfoReturnable<Boolean> cir) {
        for (ItemStack itemStack : new ItemStack[]{playerOfferA, playerOfferB}) {
            if (itemStack.getItem() instanceof CompassItem) {
                CustomData customData = itemStack.get(CUSTOM_DATA);
                if (customData == null)
                    continue;
                if (customData.copyTag().getBoolean("bingo:player_tracker"))
                    cir.setReturnValue(false);
            }
        }
    }
}

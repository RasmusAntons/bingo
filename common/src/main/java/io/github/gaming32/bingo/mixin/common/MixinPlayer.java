package io.github.gaming32.bingo.mixin.common;

import io.github.gaming32.bingo.ext.ItemEntityExt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class MixinPlayer extends MixinEntity {
    @Shadow public abstract Inventory getInventory();

    @Shadow public abstract boolean addItem(ItemStack stack);

    @Shadow public abstract ItemStack getItemBySlot(EquipmentSlot equipmentSlot);

    @Shadow @Final public InventoryMenu inventoryMenu;

    @Shadow public AbstractContainerMenu containerMenu;

    @Inject(
        method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
        at = @At("RETURN")
    )
    private void setDroppedBy(ItemStack droppedItem, boolean dropAround, boolean includeThrowerName, CallbackInfoReturnable<ItemEntity> cir) {
        if (cir.getReturnValue() instanceof ItemEntityExt itemEntity) {
            itemEntity.bingo$setDroppedBy((Entity)(Object)this);
        }
    }
}

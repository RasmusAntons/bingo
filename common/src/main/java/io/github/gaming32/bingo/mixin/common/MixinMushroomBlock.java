package io.github.gaming32.bingo.mixin.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.gaming32.bingo.triggers.GrowFeatureTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MushroomBlock.class)
public class MixinMushroomBlock {
    @WrapOperation(method = "growMushroom", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/ConfiguredFeature;place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean onPlaceMushroom(
        ConfiguredFeature<?, ?> feature,
        WorldGenLevel level,
        ChunkGenerator chunkGen,
        RandomSource rand,
        BlockPos pos,
        Operation<Boolean> operation
    ) {
        return GrowFeatureTrigger.wrapPlaceOperation(feature, level, chunkGen, rand, pos, operation);
    }
}

package io.github.gaming32.bingo.ext;

import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.TagPredicate;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.LevelStem;

public interface LocationPredicateExt {
    void bingo$setDimensionTag(TagPredicate<LevelStem> dimensionTag);
    void bingo$setBiomeTag(TagPredicate<Biome> biomeTag);

    static LocationPredicate inDimension(TagPredicate<LevelStem> dimension) {
        LocationPredicate predicate = LocationPredicate.Builder.location().build();
        ((LocationPredicateExt) predicate).bingo$setDimensionTag(dimension);
        return predicate;
    }

    static LocationPredicate inDimension(TagKey<LevelStem> dimension) {
        return inDimension(TagPredicate.is(dimension));
    }

    static LocationPredicate inBiome(TagPredicate<Biome> biome) {
        LocationPredicate predicate = LocationPredicate.Builder.location().build();
        ((LocationPredicateExt) predicate).bingo$setBiomeTag(biome);
        return predicate;
    }

    static LocationPredicate inBiome(TagKey<Biome> biome) {
        return inBiome(TagPredicate.is(biome));
    }
}

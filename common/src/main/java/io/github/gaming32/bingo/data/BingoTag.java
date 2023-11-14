package io.github.gaming32.bingo.data;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import io.github.gaming32.bingo.Bingo;
import io.github.gaming32.bingo.util.BingoUtil;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

public record BingoTag(
    ResourceLocation id,
    FloatList difficultyMax,
    boolean allowedOnSameLine,
    SpecialType specialType
) {
    private static Map<ResourceLocation, BingoTag> tags = Collections.emptyMap();

    @Nullable
    public static BingoTag getTag(ResourceLocation id) {
        return tags.get(id);
    }

    public static BingoTag deserialize(ResourceLocation id, JsonObject json) {
        final JsonArray difficultyMaxArray = GsonHelper.getAsJsonArray(json, "difficulty_max");
        final float[] difficultyMax = new float[difficultyMaxArray.size()];
        for (int i = 0; i < difficultyMax.length; i++) {
            difficultyMax[i] = GsonHelper.convertToFloat(difficultyMaxArray.get(i), "difficulty_max[" + i + "]");
        }
        return new BingoTag(
            id,
            FloatList.of(difficultyMax),
            GsonHelper.getAsBoolean(json, "allowed_on_same_line", true),
            json.has("special_type")
                ? BingoUtil.fromJsonElement(SpecialType.CODEC, json.get("special_type"))
                : SpecialType.NONE
        );
    }

    public JsonObject serialize() {
        final JsonObject result = new JsonObject();

        final JsonArray difficultyMaxArray = new JsonArray(difficultyMax.size());
        for (final Float max : difficultyMax) {
            difficultyMaxArray.add(new JsonPrimitive(max));
        }
        result.add("difficulty_max", difficultyMaxArray);

        if (!allowedOnSameLine) {
            result.addProperty("allowed_on_same_line", false);
        }

        if (specialType != SpecialType.NONE) {
            result.addProperty("special_type", specialType.getSerializedName());
        }

        return result;
    }

    public float getUnscaledMaxForDifficulty(int difficulty) {
        if (difficulty < 0) {
            throw new IllegalArgumentException("difficulty < 0 is invalid");
        }
        final int size = difficultyMax.size();
        return difficulty < size ? difficultyMax.getFloat(difficulty) : difficultyMax.getFloat(size - 1);
    }

    public int getMaxForDifficulty(int difficulty, int boardSize) {
        return Mth.ceil(getUnscaledMaxForDifficulty(difficulty) * boardSize * boardSize);
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private FloatList difficultyMax = new FloatArrayList();
        private boolean allowedOnSameLine = true;
        private SpecialType specialType = SpecialType.NONE;

        private Builder(ResourceLocation id) {
            this.id = id;
        }

        public Builder difficultyMax(int... scaledBy5x5) {
            final float[] unscaled = new float[scaledBy5x5.length];
            for (int i = 0; i < scaledBy5x5.length; i++) {
                unscaled[i] = scaledBy5x5[i] / 25f;
            }
            return this.difficultyMax(unscaled);
        }

        public Builder difficultyMax(float... unscaledMaxes) {
            this.difficultyMax = FloatList.of(unscaledMaxes);
            return this;
        }

        public Builder disallowOnSameLine() {
            this.allowedOnSameLine = false;
            return this;
        }

        public Builder specialType(SpecialType specialType) {
            this.specialType = specialType;
            return this;
        }

        public BingoTag build() {
            return new BingoTag(id, difficultyMax, allowedOnSameLine, specialType);
        }
    }

    public static class ReloadListener extends SimpleJsonResourceReloadListener {
        public static final ResourceLocation ID = new ResourceLocation("bingo:tags");
        private static final Gson GSON = new GsonBuilder().create();

        public ReloadListener() {
            super(GSON, "bingo/tags");
        }

        @NotNull
        @Override
        public String getName() {
            return ID.toString();
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager, ProfilerFiller profiler) {
            final ImmutableMap.Builder<ResourceLocation, BingoTag> result = ImmutableMap.builder();
            for (final var entry : jsons.entrySet()) {
                try {
                    final JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), "bingo tag");
                    result.put(entry.getKey(), deserialize(entry.getKey(), json));
                } catch (Exception e) {
                    Bingo.LOGGER.error("Parsing error in bingo tag {}: {}", entry.getKey(), e.getMessage());
                }
            }
            tags = result.build();
            Bingo.LOGGER.info("Loaded {} bingo tags", tags.size());
        }
    }

    public enum SpecialType implements StringRepresentable {
        NONE(0), NEVER(0xff5555), FINISH(0x5555ff);

        @SuppressWarnings("deprecation")
        public static final EnumCodec<SpecialType> CODEC = StringRepresentable.fromEnum(SpecialType::values);

        public final int incompleteColor;

        SpecialType(int incompleteColor) {
            this.incompleteColor = incompleteColor;
        }

        @NotNull
        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}

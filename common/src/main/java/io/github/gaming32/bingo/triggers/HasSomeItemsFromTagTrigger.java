package io.github.gaming32.bingo.triggers;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.gaming32.bingo.util.BingoCodecs;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public class HasSomeItemsFromTagTrigger extends SimpleCriterionTrigger<HasSomeItemsFromTagTrigger.TriggerInstance> {
    @NotNull
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, Inventory inventory) {
        trigger(player, triggerInstance -> triggerInstance.matches(player, inventory));
    }

    public static Builder builder() {
        return new Builder();
    }

    public record TriggerInstance(
        Optional<ContextAwarePredicate> player,
        TagKey<Item> tag,
        int requiredCount
    ) implements SimpleInstance {
        private static final int ALL = -1;
        private static final Codec<Integer> REQUIRED_COUNT_CODEC = BingoCodecs.firstValid(ExtraCodecs.POSITIVE_INT, BingoCodecs.exactly(ALL));
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(TriggerInstance::player),
                TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(TriggerInstance::tag),
                REQUIRED_COUNT_CODEC.fieldOf("required_count").forGetter(TriggerInstance::requiredCount)
            ).apply(instance, TriggerInstance::new)
        );

        public boolean matches(ServerPlayer player, Inventory inventory) {
            int requiredCount = this.requiredCount;
            if (requiredCount == ALL) {
                var tag = BuiltInRegistries.ITEM.getTag(this.tag);
                if (tag.isEmpty()) {
                    return false;
                }
                requiredCount = tag.get().size();
            }

            Set<Item> foundItems = Sets.newIdentityHashSet();
            for (int i = 0, l = inventory.getContainerSize(); i < l; i++) {
                final ItemStack item = inventory.getItem(i);
                if (item.is(tag) && foundItems.add(item.getItem()) && foundItems.size() >= requiredCount) {
//                    setProgress(player, requiredCount, requiredCount);
                    return true;
                }
            }

//            setProgress(player, foundItems.size(), requiredCount);
            return false;
        }
    }

    public static final class Builder {
        private Optional<ContextAwarePredicate> player = Optional.empty();
        private TagKey<Item> tag;
        @Nullable
        private Integer requiredCount = null;

        private Builder() {
        }

        public Builder player(ContextAwarePredicate player) {
            this.player = Optional.ofNullable(player);
            return this;
        }

        public Builder tag(TagKey<Item> tag) {
            this.tag = tag;
            return this;
        }

        public Builder requiredCount(int requiredCount) {
            this.requiredCount = requiredCount;
            return this;
        }

        public Builder requiresAll() {
            return requiredCount(TriggerInstance.ALL);
        }

        public Criterion<TriggerInstance> build() {
            if (tag == null) {
                throw new IllegalStateException("Did not specify tag");
            }
            if (requiredCount == null) {
                throw new IllegalStateException("Did not specify requiredCount");
            }
            return BingoTriggers.HAS_SOME_ITEMS_FROM_TAG.createCriterion(
                new TriggerInstance(player, tag, requiredCount)
            );
        }
    }
}

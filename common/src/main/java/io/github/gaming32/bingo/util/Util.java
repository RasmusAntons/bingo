package io.github.gaming32.bingo.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.util.RandomSource;

import java.util.stream.Collector;

public class Util {
    public static int[] generateIntArray(int length) {
        final int[] result = new int[length];
        for (int i = 1; i < length; i++) {
            result[i] = i;
        }
        return result;
    }

    // Copied from IntArrays, but made to use RandomSource
    public static int[] shuffle(int[] a, RandomSource random) {
        for (int i = a.length; i-- != 0;) {
            final int p = random.nextInt(i + 1);
            final int t = a[i];
            a[i] = a[p];
            a[p] = t;
        }
        return a;
    }

    public static Collector<JsonElement, ?, JsonArray> toJsonArray() {
        return Collector.of(JsonArray::new, JsonArray::add, (a, b) -> {
            a.addAll(b);
            return a;
        });
    }
}

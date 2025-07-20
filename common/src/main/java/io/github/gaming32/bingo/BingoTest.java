package io.github.gaming32.bingo;

import io.github.gaming32.bingo.data.BingoDifficulty;
import io.github.gaming32.bingo.data.BingoRegistries;
import io.github.gaming32.bingo.data.goal.GoalHolder;
import io.github.gaming32.bingo.game.BingoBoard;
import io.github.gaming32.bingo.game.mode.BingoGameMode;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;

import java.util.Arrays;
import java.util.Collections;

public class BingoTest {
    private final MinecraftServer server;

    public BingoTest(MinecraftServer server) {
        this.server = server;
    }

    private boolean testBingoBoardGenerateGoals() {
        Registry<BingoDifficulty> difficultyLookup = server.registryAccess().lookupOrThrow(BingoRegistries.DIFFICULTY);
        int size = 5;
        difficultyLookup.stream().forEach(difficulty -> {
            if (difficulty.distribution() == null) {
                return;
            }
            BingoGameMode gameMode = server.registryAccess().lookupOrThrow(BingoRegistries.GAME_MODE).getValueOrThrow(BingoGameMode.LOCKOUT.key());
            var goals = BingoBoard.generateGoals(
                    difficultyLookup,
                    size,
                    difficulty,
                    RandomSource.create(),
                    gameMode::isGoalAllowed,
                    Collections.emptyList(),
                    HolderSet.empty(),
                    true
            );
            float[] scaledDistribution = new float[difficulty.distribution().size()];
            for (int i = 0; i < difficulty.distribution().size(); ++i) {
                scaledDistribution[i] = difficulty.distribution().get(i) * size * size;
            }
            int[] occurrences = new int[difficultyLookup.size()];
            for (GoalHolder goal : goals) {
                ++occurrences[goal.goal().getDifficulty().value().number()];
            }
            System.out.println(difficulty.description().getString() + ": expected " + Arrays.toString(scaledDistribution) + " got " + Arrays.toString(occurrences));
        });
        return true;
    }

    public int runAll() {
        int failedTests = 0;
        if (!testBingoBoardGenerateGoals()) ++failedTests;
        return failedTests;
    }
}

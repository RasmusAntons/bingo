package io.github.gaming32.bingo.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import io.github.gaming32.bingo.Bingo;
import io.github.gaming32.bingo.client.config.BingoClientConfig;
import io.github.gaming32.bingo.client.icons.DefaultIconRenderers;
import io.github.gaming32.bingo.client.icons.IconRenderer;
import io.github.gaming32.bingo.client.icons.IconRenderers;
import io.github.gaming32.bingo.client.recipeviewer.RecipeViewerPlugin;
import io.github.gaming32.bingo.data.icons.GoalIcon;
import io.github.gaming32.bingo.game.BingoBoard;
import io.github.gaming32.bingo.game.BingoGameMode;
import io.github.gaming32.bingo.game.GoalProgress;
import io.github.gaming32.bingo.network.ClientGoal;
import io.github.gaming32.bingo.platform.BingoPlatform;
import io.github.gaming32.bingo.platform.event.ClientEvents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;

import java.util.*;

@Environment(EnvType.CLIENT)
public class BingoClient {
    private static final ResourceLocation BOARD_TEXTURE = new ResourceLocation("bingo:board");
    public static final Component BOARD_TITLE = Component.translatable("bingo.board.title");

    public static final int BOARD_OFFSET = 3;

    public static BingoBoard.Teams clientTeam = BingoBoard.Teams.NONE;
    public static BingoBoard.Teams receivedClientTeam = BingoBoard.Teams.NONE;
    public static ClientGame clientGame;

    public static final BingoClientConfig CONFIG = new BingoClientConfig(
        BingoPlatform.platform.getConfigDir().resolve("bingo-client.toml")
    );
    private static RecipeViewerPlugin recipeViewerPlugin;

    public static void init() {
        CONFIG.load();
        CONFIG.save();

        ClientEvents.KEY_RELEASED_PRE.register((screen, keyCode, scanCode, modifiers) -> {
            if (clientGame == null || !(screen instanceof ChatScreen)) {
                return false;
            }
            final Window window = Minecraft.getInstance().getWindow();
            final float scale = CONFIG.getBoardScale();
            final float x = CONFIG.getBoardCorner().getX(window.getGuiScaledWidth(), scale);
            final float y = CONFIG.getBoardCorner().getY(window.getGuiScaledHeight(), scale);
            return detectPress(keyCode, scanCode, x, y, scale);
        });

        ClientEvents.MOUSE_RELEASED_PRE.register((screen, mouseX, mouseY, button) -> {
            if (clientGame == null || !(screen instanceof ChatScreen)) {
                return false;
            }
            final Window window = Minecraft.getInstance().getWindow();
            final float scale = CONFIG.getBoardScale();
            final float x = CONFIG.getBoardCorner().getX(window.getGuiScaledWidth(), scale);
            final float y = CONFIG.getBoardCorner().getY(window.getGuiScaledHeight(), scale);
            return detectClick(button, x, y, scale);
        });

        ClientEvents.PLAYER_QUIT.register(player -> {
            clientTeam = receivedClientTeam = BingoBoard.Teams.NONE;
            clientGame = null;
        });

        ClientEvents.CLIENT_TICK_END.register(minecraft -> {
            if (minecraft.player == null || !minecraft.player.isSpectator()) {
                clientTeam = receivedClientTeam;
            }
        });

        DefaultIconRenderers.setup();

        final KeyMapping boardKey = new KeyMapping("bingo.key.board", InputConstants.KEY_B, "bingo.key.category");
        BingoPlatform.platform.registerKeyMappings(registrar -> registrar.accept(boardKey));
        ClientEvents.CLIENT_TICK_START.register(minecraft -> {
            while (boardKey.consumeClick()) {
                if (clientGame != null) {
                    minecraft.setScreen(new BoardScreen());
                }
            }
        });

        BingoPlatform.platform.registerClientTooltips(registrar -> registrar.register(IconTooltip.class, ClientIconTooltip::new));
    }

    public static RecipeViewerPlugin getRecipeViewerPlugin() {
        if (recipeViewerPlugin == null) {
            recipeViewerPlugin = RecipeViewerPlugin.detect();
        }
        return recipeViewerPlugin;
    }

    public static void renderBoardOnHud(Minecraft minecraft, GuiGraphics graphics) {
        if (clientGame == null || minecraft.getDebugOverlay().showDebugScreen() || minecraft.screen instanceof BoardScreen) {
            return;
        }
        final float scale = CONFIG.getBoardScale();
        final float x = CONFIG.getBoardCorner().getX(graphics.guiWidth(), scale);
        final float y = CONFIG.getBoardCorner().getY(graphics.guiHeight(), scale);
        renderBingo(graphics, minecraft.screen instanceof ChatScreen, x, y, scale);

        if (CONFIG.isShowScoreCounter() && clientGame.renderMode() == BingoGameMode.RenderMode.ALL_TEAMS) {
            class TeamValue {
                final BingoBoard.Teams team;
                int score;

                TeamValue(BingoBoard.Teams team) {
                    this.team = team;
                }
            }

            final TeamValue[] teams = new TeamValue[clientGame.teams().length];
            for (int i = 0; i < teams.length; i++) {
                teams[i] = new TeamValue(BingoBoard.Teams.fromOne(i));
            }

            int totalScore = 0;
            for (final BingoBoard.Teams state : clientGame.states()) {
                if (state.count() == 1) {
                    totalScore++;
                    teams[state.getFirstIndex()].score++;
                }
            }

            Arrays.sort(teams, Comparator.comparing(v -> -v.score)); // Sort in reverse

            final Font font = minecraft.font;
            final int scoreX = (int)(x * scale + getBoardWidth() * scale / 2);
            int scoreY;
            if (CONFIG.getBoardCorner().isOnBottom) {
                scoreY = (int)((y - BOARD_OFFSET) * scale - font.lineHeight);
            } else {
                scoreY = (int)(y * scale + (getBoardHeight() + BOARD_OFFSET) * scale);
            }
            final int shift = CONFIG.getBoardCorner().isOnBottom ? -12 : 12;
            for (final TeamValue teamValue : teams) {
                if (teamValue.score == 0) break;
                final PlayerTeam team = clientGame.teams()[teamValue.team.getFirstIndex()];
                final MutableComponent leftText = getDisplayName(team).copy();
                final MutableComponent rightText = Component.literal(" - " + teamValue.score);
                if (team.getColor() != ChatFormatting.RESET) {
                    leftText.withStyle(team.getColor());
                    rightText.withStyle(team.getColor());
                }
                graphics.drawString(font, leftText, scoreX - font.width(leftText), scoreY, 0xffffffff);
                graphics.drawString(font, rightText, scoreX, scoreY, 0xffffffff);
                scoreY += shift;
            }

            final MutableComponent leftText = Component.translatable("bingo.unclaimed");
            final MutableComponent rightText = Component.literal(" - " + (clientGame.states().length - totalScore));
            graphics.drawString(font, leftText, scoreX - font.width(leftText), scoreY, 0xffffffff);
            graphics.drawString(font, rightText, scoreX, scoreY, 0xffffffff);
        }
    }

    public static int getBoardWidth() {
        return 14 + 18 * clientGame.size();
    }

    public static int getBoardHeight() {
        return 24 + 18 * clientGame.size();
    }

    public static void renderBingo(GuiGraphics graphics, boolean mouseHover, float x, float y, float scale) {
        if (clientGame == null) {
            Bingo.LOGGER.warn("BingoClient.renderBingo() called when Bingo.clientGame == null!");
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();

        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1);
        graphics.pose().translate(x, y, 0);

        final BingoMousePos mousePos = mouseHover ? BingoMousePos.getPos(minecraft, clientGame.size(), x, y, scale) : null;

        graphics.blitSprite(
            BOARD_TEXTURE, 0, 0,
            7 + 18 * clientGame.size() + 7,
            17 + 18 * clientGame.size() + 7
        );
        graphics.drawString(minecraft.font, BOARD_TITLE, 8, 6, 0x404040, false);

        final boolean spectator = minecraft.player != null && minecraft.player.isSpectator();
        for (int sx = 0; sx < clientGame.size(); sx++) {
            for (int sy = 0; sy < clientGame.size(); sy++) {
                final ClientGoal goal = clientGame.getGoal(sx, sy);
                final int slotX = sx * 18 + 8;
                final int slotY = sy * 18 + 18;
                final GoalIcon icon = goal.icon();
                final IconRenderer<? super GoalIcon> renderer = IconRenderers.getRenderer(icon);
                renderer.render(icon, graphics, slotX, slotY);
                renderer.renderDecorations(icon, minecraft.font, graphics, slotX, slotY);
                final BingoBoard.Teams state = clientGame.getState(sx, sy);
                boolean isGoalCompleted = state.and(clientTeam);

                final List<Integer> colors = switch (clientGame.renderMode()) {
                    case FANCY -> Collections.singletonList(isGoalCompleted ? Integer.valueOf(0x55ff55) : goal.specialType().incompleteColor);
                    case ALL_TEAMS -> {
                        if (!state.any()) {
                            yield Collections.emptyList();
                        }
                        yield state.stream().map((team) -> clientGame.teams()[team].getColor().getColor()).boxed().toList();
                    }
                };
                for (int i = 0; i < colors.size(); ++i) {
                    int color = colors.get(i);
                    int start = 16 * i / colors.size();
                    int end = 16 * (i + 1) / colors.size();
                    int base = (colors.size() == 1) ? 0xA0000000 : 0x50000000;

                    graphics.fill(slotX + start, slotY, slotX + end, slotY + 16, base | color);
                }

                GoalProgress progress = clientGame.getProgress(sx, sy);
                if (progress != null && !isGoalCompleted && progress.progress() > 0 && !spectator) {
                    graphics.pose().pushPose();
                    graphics.pose().translate(0, 0, 200);

                    final int pWidth = Math.round(progress.progress() * 13f / progress.maxProgress());
                    final int pColor = Mth.hsvToRgb((float)progress.progress() / progress.maxProgress() / 3f, 1f, 1f);
                    final int pX = slotX + 2;
                    final int pY = slotY + 13;
                    graphics.fill(RenderType.guiOverlay(), pX, pY, pX + 13, pY + 2, 0xff000000);
                    graphics.fill(RenderType.guiOverlay(), pX, pY, pX + pWidth, pY + 1, pColor | 0xff000000);

                    graphics.pose().popPose();
                }
            }
        }

        if (BingoMousePos.hasSlotPos(mousePos)) {
            graphics.pose().pushPose();
            graphics.pose().translate(0f, 0f, 200f);
            final int slotX = mousePos.slotIdX() * 18 + 8;
            final int slotY = mousePos.slotIdY() * 18 + 18;
            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0x80ffffff);
            graphics.pose().popPose();
        }

        graphics.pose().popPose();
        if (BingoMousePos.hasSlotPos(mousePos)) {
            final ClientGoal goal = clientGame.getGoal(mousePos.slotIdX(), mousePos.slotIdY());
            final GoalProgress progress = clientGame.getProgress(mousePos.slotIdX(), mousePos.slotIdY());
            final TooltipBuilder tooltip = new TooltipBuilder();
            tooltip.add(goal.name());
            if (progress != null && (progress.maxProgress() > 1 || minecraft.options.advancedItemTooltips)) {
                tooltip.add(Component.translatable("bingo.progress", progress.progress(), progress.maxProgress()));
            }
            if (minecraft.options.advancedItemTooltips) {
                tooltip.add(Component.literal(goal.id().toString()).withStyle(ChatFormatting.DARK_GRAY));
            }
            goal.tooltip().ifPresent(component -> {
                final int width = Math.max(300, minecraft.font.width(goal.name()));
                tooltip.add(FormattedCharSequence.EMPTY);
                minecraft.font.split(component, width).forEach(tooltip::add);
            });
            goal.tooltipIcon().map(IconTooltip::new).ifPresent(tooltip::add);
            tooltip.draw(minecraft.font, graphics, (int)mousePos.mouseX(), (int)mousePos.mouseY());
        }
    }

    public static boolean detectClick(int button, float x, float y, float scale) {
        return detectClickOrPress(InputConstants.Type.MOUSE.getOrCreate(button), x, y, scale);
    }

    public static boolean detectPress(int keyCode, int scanCode, float x, float y, float scale) {
        return detectClickOrPress(InputConstants.getKey(keyCode, scanCode), x, y, scale);
    }

    public static boolean detectClickOrPress(InputConstants.Key key, float x, float y, float scale) {
        if (clientGame == null) {
            return false;
        }

        final BingoMousePos mousePos = BingoMousePos.getPos(Minecraft.getInstance(), clientGame.size(), x, y, scale);
        if (!mousePos.hasSlotPos()) {
            return false;
        }

        final ClientGoal goal = clientGame.getGoal(mousePos.slotIdX(), mousePos.slotIdY());

        final RecipeViewerPlugin plugin = getRecipeViewerPlugin();
        if (plugin.isViewRecipe(key)) {
            plugin.showRecipe(IconRenderers.getRenderer(goal.icon()).getIconItem(goal.icon()));
            return true;
        }
        if (plugin.isViewUsages(key)) {
            plugin.showUsages(IconRenderers.getRenderer(goal.icon()).getIconItem(goal.icon()));
            return true;
        }
        return false;
    }

    public static Component getDisplayName(PlayerTeam team) {
        final ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            final Iterator<PlayerInfo> players = team.getPlayers()
                .stream()
                .map(connection::getPlayerInfo)
                .filter(Objects::nonNull)
                .iterator();
            if (players.hasNext()) {
                final PlayerInfo playerInfo = players.next();
                if (!players.hasNext()) {
                    final ClientLevel level = Minecraft.getInstance().level;
                    if (level != null) {
                        final Player player = level.getPlayerByUUID(playerInfo.getProfile().getId());
                        if (player != null) {
                            return player.getName();
                        }
                    }
                    return Component.literal(playerInfo.getProfile().getName());
                }
            }
        }
        return team.getDisplayName();
    }
}

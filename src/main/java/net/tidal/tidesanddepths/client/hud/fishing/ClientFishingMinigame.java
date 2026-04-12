package net.tidal.tidesanddepths.client.hud.pressure.fishing;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.tidal.tidesanddepths.TidesAndDepths;
import net.tidal.tidesanddepths.networking.ModPackets;

public final class ClientFishingMinigame {
    private static final int FRAME_COLOR = 0xCC0B1F28;
    private static final int BAR_BG_COLOR = 0xCC122B37;
    private static final int PLAYER_BAR_COLOR = 0xFF4FD38A;
    private static final int FISH_COLOR = 0xFFE8B84A;
    private static final int PROGRESS_BG_COLOR = 0xAA111111;
    private static final int PROGRESS_COLOR = 0xFF5AD7FF;

    //Fishing Attributes
    public static float fishingSpeed = 1f;
    public static float progressBarSpeed = 0.01f;
    public static int catchBarHeight = 1; //1 * catchBarHeight
    public static float fishBiteSpeed = 1f;
    public static float durability = 1f;
    public static float depth = 1;
    public static int fishBarHeight = 1; //25 * fishBarHeight

    // Fraction of the lane height covered by the controllable player bar.
    private static final float PLAYER_BAR_SIZE = 0.24f;
    // Overall HUD panel size.
    private static final int BOX_WIDTH = 48;
    private static final int BOX_HEIGHT = 128;
    // Play lane texture size and placement inside the HUD panel.
    private static final int LANE_WIDTH = 32;
    private static final int LANE_HEIGHT = 128;
    private static final int LANE_X_OFFSET = 0;
    private static final int LANE_Y_OFFSET = 0;
    // Fish marker texture size.
    private static final int FISH_WIDTH = 18;
    private static final int FISH_HEIGHT = 25 * fishBarHeight;
    // Catch progress bar texture size and placement inside the HUD panel.
    private static final int PROGRESS_WIDTH = 16;
    private static final int PROGRESS_HEIGHT = 64;
    private static final int PROGRESS_X_OFFSET = 32;
    private static final int PROGRESS_Y_OFFSET = 32;
    private static final int PROGRESS_FILL_WIDTH = 7;
    private static final int PROGRESS_FILL_HEIGHT = 54;
    private static final int PROGRESS_FILL_X_OFFSET = 4;
    private static final int PROGRESS_FILL_Y_OFFSET = 5;

    private static final Identifier BACKGROUND_TEXTURE = new Identifier(TidesAndDepths.MOD_ID, "textures/gui/fishing/fishing_minigame_gui_background.png");
    private static final Identifier LANE_TEXTURE = new Identifier(TidesAndDepths.MOD_ID, "textures/gui/fishing/fishing_minigame_lane.png");
    private static final Identifier PLAYER_BAR_TEXTURE = new Identifier(TidesAndDepths.MOD_ID, "textures/gui/fishing/fishing_minigame_player_bar.png");
    private static final Identifier FISH_MARKER_TEXTURE = new Identifier(TidesAndDepths.MOD_ID, "textures/gui/fishing/fishing_minigame_fish_marker.png");
    private static final Identifier PROGRESS_BACKGROUND_TEXTURE = new Identifier(TidesAndDepths.MOD_ID, "textures/gui/fishing/fishing_minigame_progress_bar_background.png");
    private static final Identifier PROGRESS_FILL_TEXTURE = new Identifier(TidesAndDepths.MOD_ID, "textures/gui/fishing/fishing_minigame_progress_bar_fill.png");

    private static boolean active;
    private static boolean awaitingServerResponse;
    private static int bobberEntityId = -1;
    private static float playerBarCenter = 0.45f;
    private static float playerVelocity;
    private static float fishPosition = 0.55f;
    private static float fishVelocity = 0.01f;
    private static float catchProgress = 0.35f;


    private static int fishMoveTimer;
    private static boolean lastAttackPressed;

    private ClientFishingMinigame() {
    }

    public static void start(int bobberEntityId) {
        active = true;
        awaitingServerResponse = false;
        ClientFishingMinigame.bobberEntityId = bobberEntityId;
        // Starting vertical position of the player bar.
        playerBarCenter = 0.45f;
        playerVelocity = 0.0f;
        // Starting vertical position and speed of the fish marker.
        fishPosition = 0.55f;
        fishVelocity = 0.012f;
        // Starting fill amount of the catch meter.
        catchProgress = 0.35f;
        // Initial delay before the fish picks a new movement direction/speed.
        fishMoveTimer = 12;
        lastAttackPressed = false;
    }

    public static void stop(boolean success) {
        active = false;
        awaitingServerResponse = false;
        bobberEntityId = -1;
        lastAttackPressed = false;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal(success ? "Fishing minigame cleared." : "Fishing minigame failed."), true);
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static void tick(MinecraftClient client) {
        if (!active || client.player == null || client.world == null) {
            return;
        }

        if (client.world.getEntityById(bobberEntityId) == null) {
            active = false;
            awaitingServerResponse = false;
            bobberEntityId = -1;
            return;
        }

        if (awaitingServerResponse) {
            return;
        }

        boolean attackPressed = client.options.attackKey.isPressed();
        if (attackPressed && !lastAttackPressed) {
            // Upward boost applied when the player taps left click once.
            playerVelocity += 0.045f;
        }
        lastAttackPressed = attackPressed;

        // Passive downward pull, movement damping, and allowed movement speed range.
        playerVelocity -= 0.0085f;
        playerVelocity *= 0.98f;
        playerVelocity = clamp(playerVelocity, -0.03f, 0.05f);
        playerBarCenter = clamp(playerBarCenter + playerVelocity, PLAYER_BAR_SIZE / 2.0f, 1.0f - PLAYER_BAR_SIZE / 2.0f);

        if (playerBarCenter <= PLAYER_BAR_SIZE / 2.0f || playerBarCenter >= 1.0f - PLAYER_BAR_SIZE / 2.0f) {
            playerVelocity = 0.0f;
        }

        fishMoveTimer--;
        if (fishMoveTimer <= 0) {
            // How often the fish chooses a new move: 10-21 ticks.
            fishMoveTimer = 10 + client.world.random.nextInt(12);
            // Random fish speed range when it changes direction.
            float targetVelocity = 0.0075f + client.world.random.nextFloat() * 0.02f * fishingSpeed;
            fishVelocity = client.world.random.nextBoolean() ? targetVelocity : -targetVelocity;
        }

        fishPosition += fishVelocity;
        // Fish movement bounds and bounce strength near the lane edges.
        if (fishPosition <= 0.04f || fishPosition >= 0.96f) {
            fishVelocity *= -0.85f;
        }
        fishPosition = clamp(fishPosition, 0.04f, 0.96f);

        float playerBarMin = playerBarCenter - PLAYER_BAR_SIZE / 2.0f;
        float playerBarMax = playerBarCenter + PLAYER_BAR_SIZE / 2.0f;
        if (fishPosition >= playerBarMin && fishPosition <= playerBarMax) {
            // Catch meter gain per tick while the fish stays inside the player bar.
            catchProgress += 0.022f;
        } else {
            // Catch meter loss per tick while the fish is outside the player bar.
            catchProgress -= progressBarSpeed;
        }

        catchProgress = clamp(catchProgress, 0.0f, 1.0f);
        if (catchProgress >= 1.0f) {
            sendResult(true);
        } else if (catchProgress <= 0.0f) {
            sendResult(false);
        }
    }

    public static void render(DrawContext ctx, float tickDelta) {
        if (!active) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;

        int screenWidth = ctx.getScaledWindowWidth();
        int screenHeight = ctx.getScaledWindowHeight();

        // HUD anchor: 24 px from the right side, vertically centered.
        int x = screenWidth - BOX_WIDTH - 24;
        int y = screenHeight / 2 - BOX_HEIGHT / 2;

        if (hasTexture(client, BACKGROUND_TEXTURE)) {
            ctx.drawTexture(BACKGROUND_TEXTURE, x, y, 0, 0, BOX_WIDTH, BOX_HEIGHT, BOX_WIDTH, BOX_HEIGHT);
        } else {
            ctx.fill(x - 4, y - 4, x + BOX_WIDTH + 4, y + BOX_HEIGHT + 4, FRAME_COLOR);
            ctx.fill(x, y, x + BOX_WIDTH, y + BOX_HEIGHT, BAR_BG_COLOR);
        }

        // Lane placement inside the background panel.
        int laneX = x + LANE_X_OFFSET;
        int laneTop = y + LANE_Y_OFFSET;
        int laneBottom = laneTop + LANE_HEIGHT;

        if (hasTexture(client, LANE_TEXTURE)) {
            ctx.drawTexture(LANE_TEXTURE, laneX, laneTop, 0, 0, LANE_WIDTH, LANE_HEIGHT, LANE_WIDTH, LANE_HEIGHT);
        } else {
            ctx.fill(laneX, laneTop, laneX + LANE_WIDTH, laneBottom, 0xAA091419);
        }


        // Fish marker placement and size around the lane.
        int fishX = laneX + (LANE_WIDTH - FISH_WIDTH) / 2;
        int fishY = laneTop + Math.round((1.0f - fishPosition) * (LANE_HEIGHT - FISH_HEIGHT));
        if (hasTexture(client, FISH_MARKER_TEXTURE)) {
            ctx.drawTexture(FISH_MARKER_TEXTURE, fishX, fishY, 0, 0, FISH_WIDTH, FISH_HEIGHT, FISH_WIDTH, FISH_HEIGHT);
        } else {
            ctx.fill(fishX, fishY, fishX + FISH_WIDTH, fishY + FISH_HEIGHT, FISH_COLOR);
        }

        int playerBarHeight = 1 * catchBarHeight;
        int playerBarWidth = 22;
        int playerBarX = laneX + (LANE_WIDTH - playerBarWidth) / 2;
        int playerBarY = laneTop + Math.round((1.0f - playerBarCenter) * (LANE_HEIGHT - playerBarHeight));
        playerBarY = clamp(playerBarY, laneTop, laneBottom - playerBarHeight);
        if (hasTexture(client, PLAYER_BAR_TEXTURE)) {
            ctx.drawTexture(PLAYER_BAR_TEXTURE, playerBarX, playerBarY, 0, 0, playerBarWidth, playerBarHeight, playerBarWidth, playerBarHeight);
        } else {
            ctx.fill(playerBarX, playerBarY, playerBarX + playerBarWidth, playerBarY + playerBarHeight, PLAYER_BAR_COLOR);
        }

        // Progress bar placement on the right side of the panel.
        int progressX = x + PROGRESS_X_OFFSET;
        int progressTop = y + PROGRESS_Y_OFFSET;
        int progressBottom = progressTop + PROGRESS_HEIGHT;

        if (hasTexture(client, PROGRESS_BACKGROUND_TEXTURE)) {
            ctx.drawTexture(PROGRESS_BACKGROUND_TEXTURE, progressX, progressTop, 0, 0, PROGRESS_WIDTH, PROGRESS_HEIGHT, PROGRESS_WIDTH, PROGRESS_HEIGHT);
        } else {
            ctx.fill(progressX, progressTop, progressX + PROGRESS_WIDTH, progressBottom, PROGRESS_BG_COLOR);
        }

        int fillX = progressX + PROGRESS_FILL_X_OFFSET;
        int fillBottom = progressTop + PROGRESS_FILL_Y_OFFSET + PROGRESS_FILL_HEIGHT;
        int filledHeight = Math.round(catchProgress * PROGRESS_FILL_HEIGHT);
        if (filledHeight > 0) {
            if (hasTexture(client, PROGRESS_FILL_TEXTURE)) {
                ctx.drawTexture(PROGRESS_FILL_TEXTURE, fillX, fillBottom - filledHeight, 0, PROGRESS_FILL_HEIGHT - filledHeight, PROGRESS_FILL_WIDTH, filledHeight, PROGRESS_FILL_WIDTH, PROGRESS_FILL_HEIGHT);
            } else {
                ctx.fill(fillX, fillBottom - filledHeight, fillX + PROGRESS_FILL_WIDTH, fillBottom, PROGRESS_COLOR);
            }
        }

        String title = "Catch";
        // Title offset above the panel.
        int titleX = x + (BOX_WIDTH - textRenderer.getWidth(title)) / 2;
        ctx.drawText(textRenderer, title, titleX, y - 14, 0xFFFFFFFF, true);

        String hint = "Tap LMB";
        // Hint offset below the panel.
        int hintX = x + (BOX_WIDTH - textRenderer.getWidth(hint)) / 2;
        ctx.drawText(textRenderer, hint, hintX, y + BOX_HEIGHT + 6, 0xFFD6F1FF, false);
    }

    public static void registerPackets() {
        ClientPlayNetworking.registerGlobalReceiver(ModPackets.START_MINIGAME, (client, handler, buf, responseSender) -> {
            int bobberEntityId = buf.readVarInt();
            client.execute(() -> start(bobberEntityId));
        });

        ClientPlayNetworking.registerGlobalReceiver(ModPackets.STOP_MINIGAME, (client, handler, buf, responseSender) -> {
            boolean success = buf.readBoolean();
            client.execute(() -> stop(success));
        });
    }

    private static void sendResult(boolean success) {
        awaitingServerResponse = true;

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(bobberEntityId);
        buf.writeBoolean(success);
        ClientPlayNetworking.send(ModPackets.MINIGAME_RESULT, buf);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean hasTexture(MinecraftClient client, Identifier texture) {
        return client.getResourceManager().getResource(texture).isPresent();
    }
}

package net.tidal.tidesanddepths.fishing;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.tidal.tidesanddepths.networking.ModPackets;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FishingMinigameManager {
    private static final Map<UUID, FishingChallenge> ACTIVE_CHALLENGES = new HashMap<>();

    private FishingMinigameManager() {
    }

    public static void registerNetworking() {
        ServerPlayNetworking.registerGlobalReceiver(ModPackets.MINIGAME_RESULT, (server, player, handler, buf, responseSender) -> {
            int bobberEntityId = buf.readVarInt();
            boolean success = buf.readBoolean();
            server.execute(() -> handleMinigameResult(player, bobberEntityId, success));
        });
    }

    public static boolean tryStartChallenge(ServerPlayerEntity player, FishingBobberEntity bobber) {
        FishingChallenge existing = ACTIVE_CHALLENGES.get(player.getUuid());
        if (existing != null) {
            if (existing.bobberEntityId() == bobber.getId()) {
                return false;
            }

            ACTIVE_CHALLENGES.remove(player.getUuid());
        }

        ACTIVE_CHALLENGES.put(player.getUuid(), new FishingChallenge(bobber.getId()));
        sendStartPacket(player, bobber.getId());
        player.sendMessage(Text.literal("A fish bites. Tap left click to keep the catch zone on the fish."), true);
        return true;
    }

    public static boolean shouldBlockReel(ServerPlayerEntity player, FishingBobberEntity bobber) {
        FishingChallenge challenge = ACTIVE_CHALLENGES.get(player.getUuid());
        return challenge != null && challenge.bobberEntityId() == bobber.getId() && !challenge.resolved();
    }

    public static void clearForBobber(int bobberEntityId) {
        Iterator<Map.Entry<UUID, FishingChallenge>> iterator = ACTIVE_CHALLENGES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, FishingChallenge> entry = iterator.next();
            FishingChallenge challenge = entry.getValue();
            if (challenge.bobberEntityId() != bobberEntityId) {
                continue;
            }

            iterator.remove();
        }
    }

    private static void handleMinigameResult(ServerPlayerEntity player, int bobberEntityId, boolean success) {
        FishingChallenge challenge = ACTIVE_CHALLENGES.get(player.getUuid());
        if (challenge == null || challenge.bobberEntityId() != bobberEntityId || challenge.resolved()) {
            return;
        }

        Entity entity = player.getWorld().getEntityById(bobberEntityId);
        if (!(entity instanceof FishingBobberEntity bobber)) {
            ACTIVE_CHALLENGES.remove(player.getUuid());
            sendStopPacket(player, false);
            return;
        }

        if (!success) {
            ACTIVE_CHALLENGES.remove(player.getUuid());
            ((FishingBobberAccess) bobber).tidesanddepths$setCaughtFish(false);
            bobber.discard();
            sendStopPacket(player, false);
            player.sendMessage(Text.literal("The fish got away."), true);
            return;
        }

        challenge.setResolved(true);
        challenge.setSuccessful(true);
        rewardCatch(player, bobber);
        ACTIVE_CHALLENGES.remove(player.getUuid());
        sendStopPacket(player, true);
        bobber.discard();
        player.sendMessage(Text.literal("Catch secured."), true);
    }

    private static void sendStartPacket(ServerPlayerEntity player, int bobberEntityId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(bobberEntityId);
        ServerPlayNetworking.send(player, ModPackets.START_MINIGAME, buf);
    }

    private static void sendStopPacket(ServerPlayerEntity player, boolean success) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(success);
        ServerPlayNetworking.send(player, ModPackets.STOP_MINIGAME, buf);
    }

    private static void rewardCatch(ServerPlayerEntity player, FishingBobberEntity bobber) {
        ServerWorld world = player.getServerWorld();
        ItemStack fishingRod = getFishingRod(player);

        LootContextParameterSet context = new LootContextParameterSet.Builder(world)
                .add(LootContextParameters.ORIGIN, bobber.getPos())
                .add(LootContextParameters.TOOL, fishingRod)
                .add(LootContextParameters.THIS_ENTITY, bobber)
                .luck(player.getLuck())
                .build(LootContextTypes.FISHING);

        LootTable lootTable = world.getServer().getLootManager().getLootTable(LootTables.FISHING_GAMEPLAY);
        List<ItemStack> generatedLoot = lootTable.generateLoot(context);

        Vec3d bobberPos = bobber.getPos();
        for (ItemStack stack : generatedLoot) {
            ItemEntity itemEntity = new ItemEntity(world, bobberPos.x, bobberPos.y, bobberPos.z, stack);

            double deltaX = player.getX() - bobberPos.x;
            double deltaY = player.getY() - bobberPos.y;
            double deltaZ = player.getZ() - bobberPos.z;
            double distanceBoost = Math.sqrt(Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)) * 0.08D;

            itemEntity.setVelocity(deltaX * 0.1D, deltaY * 0.1D + distanceBoost, deltaZ * 0.1D);
            world.spawnEntity(itemEntity);

            if (stack.isIn(ItemTags.FISHES)) {
                player.increaseStat(Stats.FISH_CAUGHT, 1);
            }
        }

        world.spawnEntity(new ExperienceOrbEntity(
                world,
                player.getX(),
                player.getY() + 0.5D,
                player.getZ() + 0.5D,
                world.random.nextInt(6) + 1
        ));
    }

    private static ItemStack getFishingRod(ServerPlayerEntity player) {
        if (player.getMainHandStack().isOf(Items.FISHING_ROD)) {
            return player.getMainHandStack();
        }

        if (player.getOffHandStack().isOf(Items.FISHING_ROD)) {
            return player.getOffHandStack();
        }

        return new ItemStack(Items.FISHING_ROD);
    }
}

package com.khanghy.revivebydeath.server;

import com.khanghy.revivebydeath.ReviveByDeath;
import com.khanghy.revivebydeath.config.ReviveByDeathConfig;
import com.khanghy.revivebydeath.network.ReviveCutscenePayload;
import com.khanghy.revivebydeath.network.ReviveEffectPayload;
import com.khanghy.revivebydeath.registry.ModSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class CheckpointManager {
	private static final Map<UUID, Checkpoint> CHECKPOINTS = new HashMap<>();
	private static final Map<UUID, PendingRevive> PENDING_REVIVES = new HashMap<>();
	private static final Map<UUID, ReturnEffect> RETURN_EFFECTS = new HashMap<>();
	private static final Map<UUID, Integer> POST_REVIVE_PROTECTION = new HashMap<>();
	private static final Map<UUID, Integer> LAST_CHECKPOINT_CHECKS = new HashMap<>();
	private static final float REVIVE_HEALTH = 8.0F;
	private static final int REVIVE_FOOD = 8;

	private CheckpointManager() {
	}

	public static void tickServer(MinecraftServer server) {
		tickPendingRevives(server);
		tickReturnEffects(server);
		tickPostReviveProtection(server);

		int serverTicks = server.getTickCount();
		ReviveByDeathConfig config = ReviveByDeathConfig.get();

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			UUID uuid = player.getUUID();
			if (isRevivePending(player)) {
				continue;
			}

			boolean hasCheckpoint = CHECKPOINTS.containsKey(uuid);
			int interval = hasCheckpoint ? config.checkpointRecheckTicks : config.checkpointInitialCheckTicks;
			interval = Math.max(1, interval);
			int lastCheckedTick = LAST_CHECKPOINT_CHECKS.getOrDefault(uuid, serverTicks - interval);
			if (serverTicks - lastCheckedTick < interval) {
				continue;
			}

			LAST_CHECKPOINT_CHECKS.put(uuid, serverTicks);
			if (isSafeCheckpoint(player)) {
				saveCheckpoint(player, serverTicks);
			}
		}
	}

	public static boolean saveManualCheckpoint(ServerPlayer player) {
		if (isRevivePending(player) || !isSafeCheckpoint(player)) {
			return false;
		}

		int serverTicks = player.getServer().getTickCount();
		saveCheckpoint(player, serverTicks);
		LAST_CHECKPOINT_CHECKS.put(player.getUUID(), serverTicks);
		return true;
	}

	public static boolean beginReviveSequence(ServerPlayer player, DamageSource source) {
		if (isRevivePending(player)) {
			stabilizePendingPlayer(player);
			return true;
		}

		ReviveByDeathConfig config = ReviveByDeathConfig.get();
		Checkpoint checkpoint = CHECKPOINTS.get(player.getUUID());
		if (checkpoint == null) {
			DebugLogManager.send(player, "No safe checkpoint is saved yet.");
			return false;
		}

		ServerLevel level = player.getServer().getLevel(checkpoint.dimension());
		if (level == null || !isCheckpointStillSafe(level, checkpoint)) {
			CHECKPOINTS.remove(player.getUUID());
			DebugLogManager.send(player, "Saved checkpoint is no longer safe.");
			return false;
		}

		ActivationResult activation = ActivationRules.evaluate(player);
		if (!activation.allowed()) {
			ReviveByDeath.LOGGER.debug("Revive denied for {}: {}.", player.getGameProfile().getName(), activation.failureReason());
			player.sendSystemMessage(Component.literal("Death Rewind failed: " + activation.failureReason() + "."));
			DebugLogManager.send(player, "Activation failed: " + activation.failureReason() + ".");
			return false;
		}

		int introFadeTicks = cutsceneIntroFadeTicks(config);
		int cutsceneTicks = cutsceneTicks(config);
		int cutsceneFrames = cutsceneFrames(config);
		int pendingTicks = pendingTicks(config);

		PendingRevive pendingRevive = new PendingRevive(
				checkpoint,
				player.getX(),
				player.getY(),
				player.getZ(),
				player.getYRot(),
				player.getXRot(),
				pendingTicks,
				pendingTicks);
		PENDING_REVIVES.put(player.getUUID(), pendingRevive);
		activation.consume().run();
		stabilizePendingPlayer(player);
		playCutsceneStartFeedback(player, (ServerLevel) player.level(), cutsceneTicks, cutsceneFrames, introFadeTicks, config.enableCutscene);

		ReviveByDeath.LOGGER.debug("Started revive cutscene for {} from {} at checkpoint saved on tick {}.",
				player.getGameProfile().getName(), source.getMsgId(), checkpoint.savedAtTick());
		return true;
	}

	public static boolean isRevivePending(ServerPlayer player) {
		return PENDING_REVIVES.containsKey(player.getUUID());
	}

	public static boolean hasPostReviveProtection(ServerPlayer player) {
		return POST_REVIVE_PROTECTION.containsKey(player.getUUID());
	}

	public static boolean hasUsableCheckpoint(ServerPlayer player) {
		Checkpoint checkpoint = CHECKPOINTS.get(player.getUUID());
		if (checkpoint == null) {
			return false;
		}

		ServerLevel level = player.getServer().getLevel(checkpoint.dimension());
		return level != null && isCheckpointStillSafe(level, checkpoint);
	}

	private static void tickPendingRevives(MinecraftServer server) {
		if (PENDING_REVIVES.isEmpty()) {
			return;
		}

		List<UUID> finished = new ArrayList<>();
		for (Map.Entry<UUID, PendingRevive> entry : PENDING_REVIVES.entrySet()) {
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				finished.add(entry.getKey());
				continue;
			}

			PendingRevive pendingRevive = entry.getValue();
			stabilizePendingPlayer(player);
			player.teleportTo(pendingRevive.holdX(), pendingRevive.holdY(), pendingRevive.holdZ());

			int nextTicks = pendingRevive.remainingTicks() - 1;
			if (nextTicks <= 0) {
				finished.add(entry.getKey());
				completeRevive(player, pendingRevive);
			} else {
				entry.setValue(pendingRevive.withRemainingTicks(nextTicks));
			}
		}

		for (UUID uuid : finished) {
			PENDING_REVIVES.remove(uuid);
		}
	}

	private static void tickReturnEffects(MinecraftServer server) {
		if (RETURN_EFFECTS.isEmpty()) {
			return;
		}

		List<UUID> finished = new ArrayList<>();
		for (Map.Entry<UUID, ReturnEffect> entry : RETURN_EFFECTS.entrySet()) {
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				finished.add(entry.getKey());
				continue;
			}

			ReturnEffect effect = entry.getValue();
			int ageTicks = effect.totalTicks() - effect.remainingTicks();
			if (player.level() instanceof ServerLevel level) {
				ParticlePatterns.returnPulse(level, player, ageTicks, effect.totalTicks());
			}

			int nextTicks = effect.remainingTicks() - 1;
			if (nextTicks <= 0) {
				finished.add(entry.getKey());
			} else {
				entry.setValue(effect.withRemainingTicks(nextTicks));
			}
		}

		for (UUID uuid : finished) {
			RETURN_EFFECTS.remove(uuid);
		}
	}

	private static void tickPostReviveProtection(MinecraftServer server) {
		if (POST_REVIVE_PROTECTION.isEmpty()) {
			return;
		}

		List<UUID> finished = new ArrayList<>();
		for (Map.Entry<UUID, Integer> entry : POST_REVIVE_PROTECTION.entrySet()) {
			if (server.getPlayerList().getPlayer(entry.getKey()) == null) {
				finished.add(entry.getKey());
				continue;
			}

			int nextTicks = entry.getValue() - 1;
			if (nextTicks <= 0) {
				finished.add(entry.getKey());
			} else {
				entry.setValue(nextTicks);
			}
		}

		for (UUID uuid : finished) {
			POST_REVIVE_PROTECTION.remove(uuid);
		}
	}

	private static void completeRevive(ServerPlayer player, PendingRevive pendingRevive) {
		Checkpoint checkpoint = pendingRevive.checkpoint();
		ServerLevel level = player.getServer().getLevel(checkpoint.dimension());
		if (level == null || !isCheckpointStillSafe(level, checkpoint)) {
			ReviveByDeath.LOGGER.warn("Could not complete revive for {}; checkpoint became unsafe.", player.getGameProfile().getName());
			return;
		}

		player.deathTime = 0;
		player.hurtTime = 0;
		player.hurtDuration = 0;
		player.clearFire();
		player.setDeltaMovement(Vec3.ZERO);
		player.resetFallDistance();
		player.setHealth(Math.min(player.getMaxHealth(), REVIVE_HEALTH));
		player.setAbsorptionAmount(0.0F);

		FoodData foodData = player.getFoodData();
		foodData.setFoodLevel(Math.max(foodData.getFoodLevel(), REVIVE_FOOD));
		foodData.setSaturation(Math.max(foodData.getSaturationLevel(), 2.0F));

		player.teleportTo(level, checkpoint.x(), checkpoint.y(), checkpoint.z(), checkpoint.yaw(), checkpoint.pitch());
		int protectionTicks = Math.max(0, ReviveByDeathConfig.get().postReviveInvulnerabilityTicks);
		if (protectionTicks > 0) {
			POST_REVIVE_PROTECTION.put(player.getUUID(), protectionTicks);
		}
		int returnTicks = Math.max(1, ReviveByDeathConfig.get().returnEffectTicks);
		playReturnFeedback(player, level, returnTicks);
		RETURN_EFFECTS.put(player.getUUID(), new ReturnEffect(returnTicks, returnTicks));
	}

	private static void stabilizePendingPlayer(ServerPlayer player) {
		player.deathTime = 0;
		player.hurtTime = 0;
		player.hurtDuration = 0;
		player.clearFire();
		player.setDeltaMovement(Vec3.ZERO);
		player.resetFallDistance();
		player.setHealth(Math.max(player.getHealth(), 1.0F));
	}

	private static void playCutsceneStartFeedback(ServerPlayer player, ServerLevel level, int cutsceneTicks, int cutsceneFrames, int introFadeTicks, boolean enabled) {
		if (enabled) {
			level.sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 1.0D, player.getZ(), 10, 0.2D, 0.35D, 0.2D, 0.015D);
		}

		ReviveCutscenePayload payload = new ReviveCutscenePayload(cutsceneTicks, cutsceneFrames, introFadeTicks);
		if (ServerPlayNetworking.canSend(player, ReviveCutscenePayload.TYPE)) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	private static void playReturnFeedback(ServerPlayer player, ServerLevel level, int returnTicks) {
		level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.RETURN_WHOOSH, SoundSource.PLAYERS, 0.28F, 1.0F);
		ParticlePatterns.returnBurst(level, player);
		ReviveEffectPayload payload = new ReviveEffectPayload(returnTicks);
		if (ServerPlayNetworking.canSend(player, ReviveEffectPayload.TYPE)) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	private static void saveCheckpoint(ServerPlayer player, int serverTicks) {
		CHECKPOINTS.put(player.getUUID(), new Checkpoint(
				player.level().dimension(),
				player.getX(),
				player.getY(),
				player.getZ(),
				player.getYRot(),
				player.getXRot(),
				serverTicks));
	}

	private static boolean isSafeCheckpoint(ServerPlayer player) {
		if (!player.isAlive() || player.isDeadOrDying() || player.isSpectator() || !player.onGround()) {
			return false;
		}

		if (player.isInLava() || player.isOnFire() || player.getY() < player.level().getMinBuildHeight() + 2) {
			return false;
		}

		if (!(player.level() instanceof ServerLevel level)) {
			return false;
		}

		return isCheckpointStillSafe(level, new Checkpoint(
				level.dimension(),
				player.getX(),
				player.getY(),
				player.getZ(),
				player.getYRot(),
				player.getXRot(),
				level.getServer().getTickCount()));
	}

	private static boolean isCheckpointStillSafe(ServerLevel level, Checkpoint checkpoint) {
		BlockPos feet = BlockPos.containing(checkpoint.x(), checkpoint.y(), checkpoint.z());
		BlockPos head = feet.above();
		BlockPos floor = feet.below();

		if (!level.isInWorldBounds(feet) || !level.isInWorldBounds(head) || !level.isInWorldBounds(floor)) {
			return false;
		}

		if (level.getFluidState(feet).is(FluidTags.LAVA) || level.getFluidState(head).is(FluidTags.LAVA)) {
			return false;
		}

		BlockState feetState = level.getBlockState(feet);
		BlockState headState = level.getBlockState(head);
		BlockState floorState = level.getBlockState(floor);
		boolean feetClear = feetState.getCollisionShape(level, feet).isEmpty();
		boolean headClear = headState.getCollisionShape(level, head).isEmpty();

		return feetClear
				&& headClear
				&& !floorState.isAir()
				&& !floorState.liquid()
				&& floorState.isFaceSturdy(level, floor, Direction.UP);
	}

	private static int cutsceneIntroFadeTicks(ReviveByDeathConfig config) {
		return config.enableCutscene ? Math.max(0, config.cutsceneIntroFadeTicks) : 0;
	}

	private static int cutsceneTicks(ReviveByDeathConfig config) {
		return config.enableCutscene ? Math.max(1, config.cutsceneTicks) : 0;
	}

	private static int cutsceneFrames(ReviveByDeathConfig config) {
		return Math.max(1, config.cutsceneFrames);
	}

	private static int pendingTicks(ReviveByDeathConfig config) {
		return config.enableCutscene ? cutsceneIntroFadeTicks(config) + cutsceneTicks(config) : 1;
	}
}

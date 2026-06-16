package com.khanghy.revivebydeath.network;

import com.khanghy.revivebydeath.ReviveByDeath;
import com.khanghy.revivebydeath.client.ReviveByDeathClient;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class ForgeNetwork {
	private static final int PROTOCOL_VERSION = 1;
	private static final SimpleChannel CHANNEL = ChannelBuilder
			.named(ResourceLocation.fromNamespaceAndPath(ReviveByDeath.MOD_ID, "main"))
			.networkProtocolVersion(PROTOCOL_VERSION)
			.simpleChannel();

	private ForgeNetwork() {
	}

	public static void register() {
		CHANNEL.messageBuilder(ReviveCutscenePayload.class, 0)
				.direction(PacketFlow.CLIENTBOUND)
				.encoder(ReviveCutscenePayload::encode)
				.decoder(ReviveCutscenePayload::decode)
				.consumerMainThread((payload, context) -> handleCutscene(payload))
				.add();

		CHANNEL.messageBuilder(ReviveEffectPayload.class, 1)
				.direction(PacketFlow.CLIENTBOUND)
				.encoder(ReviveEffectPayload::encode)
				.decoder(ReviveEffectPayload::decode)
				.consumerMainThread((payload, context) -> handleEffect(payload))
				.add();

		CHANNEL.build();
	}

	public static void sendTo(ServerPlayer player, ReviveCutscenePayload payload) {
		CHANNEL.send(payload, PacketDistributor.PLAYER.with(player));
	}

	public static void sendTo(ServerPlayer player, ReviveEffectPayload payload) {
		CHANNEL.send(payload, PacketDistributor.PLAYER.with(player));
	}

	@OnlyIn(Dist.CLIENT)
	private static void handleCutscene(ReviveCutscenePayload payload) {
		ReviveByDeathClient.startCutscene(payload.durationTicks(), payload.frameCount(), payload.introFadeTicks());
	}

	@OnlyIn(Dist.CLIENT)
	private static void handleEffect(ReviveEffectPayload payload) {
		ReviveByDeathClient.startReturnEffect(payload.durationTicks());
	}
}

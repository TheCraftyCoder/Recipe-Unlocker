package me.declipsonator.recipeunlocker.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.UUID;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Unique
	private static final Logger RECIPEUNLOCKER_LOGGER = LoggerFactory.getLogger("RecipeUnlocker");

	@Shadow
	public ClientPlayerEntity player;

	@Shadow
	public abstract IntegratedServer getServer();

	@Unique
	private volatile boolean recipeunlocker$serverUnlockDone = false;

	@Unique
	private int recipeunlocker$unlockAttemptCooldownTicks = 0;

	@Unique
	private boolean recipeunlocker$loggedSuccess = false;

	@Inject(method = "tick", at = @At("TAIL"))
	private void recipeunlocker$unlockAllRecipesOnIntegratedServer(CallbackInfo ci) {
		if (this.player == null) {
			// Disconnected/back to menu: reset so next world join works.
			this.recipeunlocker$serverUnlockDone = false;
			this.recipeunlocker$unlockAttemptCooldownTicks = 0;
			this.recipeunlocker$loggedSuccess = false;
			return;
		}

		if (this.recipeunlocker$serverUnlockDone) {
			if (!this.recipeunlocker$loggedSuccess) {
				RECIPEUNLOCKER_LOGGER.info("Integrated server recipe unlock completed.");
				this.recipeunlocker$loggedSuccess = true;
			}
			return;
		}

		final IntegratedServer server = this.getServer();
		if (server == null) {
			// Multiplayer or not fully started yet.
			return;
		}

		if (this.recipeunlocker$unlockAttemptCooldownTicks > 0) {
			this.recipeunlocker$unlockAttemptCooldownTicks--;
			return;
		}
		this.recipeunlocker$unlockAttemptCooldownTicks = 20; // ~1s

		final UUID playerUuid = this.player.getUuid();
		server.execute(() -> {
			ServerPlayerEntity serverPlayer = server.getPlayerManager().getPlayer(playerUuid);
			if (serverPlayer == null) {
				return;
			}

			ServerRecipeManager recipeManager = server.getRecipeManager();
			Collection<RecipeEntry<?>> allRecipes = recipeManager.values();
			int unlocked = serverPlayer.getRecipeBook().unlockRecipes(allRecipes, serverPlayer);

			RECIPEUNLOCKER_LOGGER.info("Unlocked {} recipes on integrated server.", unlocked);
			this.recipeunlocker$serverUnlockDone = true;
		});
	}
}

package me.declipsonator.recipeunlocker.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerRecipeBook;
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
			ServerRecipeBook recipeBook = serverPlayer.getRecipeBook();
			
			// Force-unlock every recipe by directly calling unlock() for each one.
			// This bypasses the "already unlocked" check that unlockRecipes() does.
			int count = 0;
			for (RecipeEntry<?> entry : allRecipes) {
				recipeBook.unlock(entry.id());
				count++;
			}
			
			// Re-send the full recipe book to the client so it receives all display entries.
			recipeBook.sendInitRecipesPacket(serverPlayer);

			RECIPEUNLOCKER_LOGGER.info("Force-unlocked {} recipes on integrated server and re-sent to client.", count);
			this.recipeunlocker$serverUnlockDone = true;
		});
	}
}

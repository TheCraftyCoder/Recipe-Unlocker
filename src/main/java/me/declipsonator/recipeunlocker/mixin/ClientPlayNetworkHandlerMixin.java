package me.declipsonator.recipeunlocker.mixin;

import me.declipsonator.recipeunlocker.RecipeUnlocker;
import me.declipsonator.recipeunlocker.util.RecipeBookRecipes;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.network.packet.s2c.play.SynchronizeRecipesS2CPacket;
import net.minecraft.network.packet.s2c.play.RecipeBookAddS2CPacket;
import net.minecraft.network.packet.s2c.play.RecipeBookRemoveS2CPacket;
import net.minecraft.client.recipebook.ClientRecipeManager;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.RecipeDisplayEntry;
import java.util.ArrayList;
import java.util.List;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.RecipeBookAddS2CPacket;
import net.minecraft.network.packet.s2c.play.RecipeBookRemoveS2CPacket;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {

	@Shadow
	@Final
	private ClientRecipeManager recipeManager;

	@Inject(method = "onRecipeBookAdd", at = @At("HEAD"))
	private void onRecipeBookAdd(RecipeBookAddS2CPacket packet, CallbackInfo ci) {
		for (RecipeBookAddS2CPacket.Entry entry : packet.entries()) {
			// If the server adds it, it's unlocked.
			// The mod wants to unlock everything.
			// If we assume RecipeBookRecipes stores ALL recipes, then we don't need to remove it.
			// But if RecipeBookRecipes stores LOCKED recipes (to be unlocked by the mod), then we remove it.
			// However, we need to populate RecipeBookRecipes with ALL recipes first.
			// Since we don't have a source for ALL recipes, we can only work with what we get.
			// If we get it here, we know about it.
			// So we should ADD it to our cache so we can use it for auto-crafting if needed.
			RecipeBookRecipes.addRecipe(entry.contents());
		}
	}

	@Inject(method = "onRecipeBookRemove", at = @At("HEAD"))
	private void onRecipeBookRemove(RecipeBookRemoveS2CPacket packet, CallbackInfo ci) {
		for (NetworkRecipeId id : packet.recipes()) {
			// If removed, we should remove it from our cache?
			// Or keep it?
			RecipeBookRecipes.removeRecipeFromCache(id);
		}
	}
}
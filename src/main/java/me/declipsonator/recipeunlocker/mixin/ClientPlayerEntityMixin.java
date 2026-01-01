package me.declipsonator.recipeunlocker.mixin;

import me.declipsonator.recipeunlocker.RecipeUnlocker;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forces limited crafting to be treated as disabled client-side.
 *
 * In vanilla, when limited crafting is enabled, the recipe book only shows
 * recipes the player has unlocked. This mod's goal is to show all recipes.
 */
@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
	@Shadow
	private boolean limitedCraftingEnabled;

	private static boolean recipeunlocker$loggedLimitedCraftingOverride;

	@Inject(method = "isLimitedCraftingEnabled", at = @At("HEAD"), cancellable = true)
	private void recipeunlocker$alwaysShowRecipes(CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(false);
	}

	@Inject(method = "setLimitedCraftingEnabled", at = @At("HEAD"), cancellable = true)
	private void recipeunlocker$neverEnableLimitedCrafting(boolean limitedCraftingEnabled, CallbackInfo ci) {
		if (limitedCraftingEnabled && !recipeunlocker$loggedLimitedCraftingOverride) {
			recipeunlocker$loggedLimitedCraftingOverride = true;
			RecipeUnlocker.LOG.info("Overriding limited crafting: forcing recipe book unlock behavior client-side.");
		}

		// Some code paths may read the field directly, so force the backing field off too.
		this.limitedCraftingEnabled = false;
		ci.cancel();
	}
}

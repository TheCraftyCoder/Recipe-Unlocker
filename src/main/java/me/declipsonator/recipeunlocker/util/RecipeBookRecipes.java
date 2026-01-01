package me.declipsonator.recipeunlocker.util;

import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.RecipeDisplayEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeBookRecipes {
	private static final Map<NetworkRecipeId, RecipeDisplayEntry> RECIPES = new HashMap<>();

	public static boolean isCached(NetworkRecipeId recipeId) {
		return RECIPES.containsKey(recipeId);
	}

	public static RecipeDisplayEntry get(NetworkRecipeId recipeId) {
		return RECIPES.get(recipeId);
	}

	public static void setRecipes(List<RecipeDisplayEntry> recipeCache) {
		RECIPES.clear();
		recipeCache.forEach(RecipeBookRecipes::addRecipe);
	}

	public static void addRecipe(RecipeDisplayEntry recipe) {
		RECIPES.put(recipe.id(), recipe);
	}

	public static void removeRecipeFromCache(NetworkRecipeId recipeId) {
		RECIPES.remove(recipeId);
	}
}
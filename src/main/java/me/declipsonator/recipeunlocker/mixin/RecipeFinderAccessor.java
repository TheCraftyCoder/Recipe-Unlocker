package me.declipsonator.recipeunlocker.mixin;

import net.minecraft.item.Item;
import net.minecraft.recipe.RecipeFinder;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(RecipeFinder.class)
public interface RecipeFinderAccessor {
    @Invoker("isCraftable")
    boolean invokeIsCraftable(List<? extends RecipeMatcher.RawIngredient<RegistryEntry<Item>>> ingredients, int amount, RecipeMatcher.ItemCallback<RegistryEntry<Item>> callback);
}

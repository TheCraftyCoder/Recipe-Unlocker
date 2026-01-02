package me.declipsonator.recipeunlocker.mixin;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.declipsonator.recipeunlocker.RecipeUnlocker;
import me.declipsonator.recipeunlocker.util.MoveUtils;
import me.declipsonator.recipeunlocker.util.RecipeBookRecipes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeFinder;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.display.ShapedCraftingRecipeDisplay;
import net.minecraft.recipe.RecipeGridAligner;
import net.minecraft.screen.AbstractCraftingScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

import static me.declipsonator.recipeunlocker.RecipeUnlocker.mc;

import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin implements RecipeGridAligner {
	@Shadow @Final private MinecraftClient client;
	@Unique
	PlayerInventory inventory;
	@Unique
	AbstractCraftingScreenHandler handler;
	@Unique
	RecipeFinder matcher = new RecipeFinder();
	@Unique
	ClientPlayerInteractionManager im;

	@Inject(method = "clickRecipe", at = @At("HEAD"), cancellable = true)
	public void clickedRecipe(int syncId, NetworkRecipeId recipeId, boolean craftAll, CallbackInfo ci) {
		// Only handle recipes we have cached (i.e., recipes we've unlocked client-side)
		if (!RecipeBookRecipes.isCached(recipeId)) {
			// Let vanilla handle it - this recipe is legitimately unlocked on server
			return;
		}
		
		if (RecipeUnlocker.mc.player == null) return;
		if (!(RecipeUnlocker.mc.currentScreen instanceof HandledScreen<?> handledScreen)) return;
		
		RecipeBookWidget widget = getRecipeBookWidget(handledScreen);
		if (widget == null) return;

		ClientPlayerEntity entity = RecipeUnlocker.mc.player;

		im = MinecraftClient.getInstance().interactionManager;
		if (!(RecipeUnlocker.mc.player.currentScreenHandler instanceof AbstractCraftingScreenHandler craftingHandler)) {
			return;
		}
		handler = craftingHandler;
		inventory = entity.getInventory();

		// Cancel vanilla behavior now - we're handling this recipe ourselves
		// This prevents the server from receiving a click for a recipe it doesn't know about
		ci.cancel();

		if (!canReturnInputs() && !entity.isCreative()) {
			return;
		}
		matcher.clear();
		entity.getInventory().populateRecipeFinder(matcher);
		handler.populateRecipeFinder(matcher);
		
		RecipeDisplayEntry entry = RecipeBookRecipes.get(recipeId);
		var reqs = entry.craftingRequirements();
		if (reqs.isPresent()) {
			if (((RecipeFinderAccessor)matcher).invokeIsCraftable(reqs.get(), 1, null)) {
				fillInputSlots(entry, craftAll);
			} else {
				returnInputs();
				// Vanilla normally populates the ghost recipe overlay when a craft fails.
				// Since we cancel clickRecipe, we must trigger this ourselves.
				widget.onCraftFailed(entry.display());
			}
		}
		entity.getInventory().markDirty();
	}

	@Unique
	@Nullable
	private static RecipeBookWidget getRecipeBookWidget(HandledScreen<?> handledScreen) {
		if (handledScreen instanceof RecipeBookScreen<?> recipeBookScreen) {
			return ((RecipeBookScreenAccessor)recipeBookScreen).getRecipeBook();
		}
		return null;
	}

	/*
		The code below was pretty much copied from the server side
		I couldn't interface with it by just referencing it or smth so I just pasted

		From InputSlotFiller
	 */

	@Unique
	protected void returnInputs() {
		// handler is already an AbstractCraftingScreenHandler
		for (Slot slot : handler.getInputSlots()) {
			mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
			MoveUtils.pickupId(slot.id, slot.getStack().getCount());
			mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot.id, 0, SlotActionType.THROW, mc.player);
		}
	}

	@Unique
	private int countCrafts(java.util.List<net.minecraft.recipe.Ingredient> ingredients, int limit) {
		int low = 0;
		int high = limit;
		int ans = 0;
		while (low <= high) {
			int mid = low + (high - low) / 2;
			if (mid == 0) {
				low = 1;
				continue;
			}
			if (((RecipeFinderAccessor)matcher).invokeIsCraftable(ingredients, mid, null)) {
				ans = mid;
				low = mid + 1;
			} else {
				high = mid - 1;
			}
		}
		return ans;
	}

	@Unique
	protected void fillInputSlots(RecipeDisplayEntry recipe, boolean craftAll) {
		var reqs = recipe.craftingRequirements();
		if (reqs.isEmpty()) return;

		int j;
		boolean bl = false; // handler.matches(recipe);
		int i = countCrafts(reqs.get(), Integer.MAX_VALUE);
		
		j = getAmountToFill(craftAll, i, bl);
		IntArrayList intList = new IntArrayList();
		if (((RecipeFinderAccessor)matcher).invokeIsCraftable(reqs.get(), j, (item) -> {
			intList.add(Item.getRawId(item.value()));
		})) {
			int k = j;
			for (Integer integer : intList) {
				int m;
				int l = integer;
				ItemStack itemStack2 = Item.byRawId(l).getDefaultStack();
				if (itemStack2.isEmpty() || (m = itemStack2.getMaxCount()) >= k) continue;
				k = m;
			}
			j = k;
			intList.clear();
			if (((RecipeFinderAccessor)matcher).invokeIsCraftable(reqs.get(), j, (item) -> {
				intList.add(Item.getRawId(item.value()));
			})) {
				returnInputs();
				RecipeDisplay display = recipe.display();
				int width = handler.getWidth();
				int height = handler.getHeight();
				if (display instanceof ShapedCraftingRecipeDisplay shaped) {
					width = shaped.width();
					height = shaped.height();
				}

				RecipeGridAligner.alignRecipeToGrid(handler.getWidth(), handler.getHeight(), width, height, intList, (slot, item, x, y) -> {
					acceptAlignedInput(item, slot, x, y, 0);
				});
			}
		}
	}

	// @Override
	public void acceptAlignedInput(Integer integer, int i, int j, int k, int l) {
		Slot slot = handler.getSlot(i);
		ItemStack itemStack = Item.byRawId(integer).getDefaultStack();
		if (itemStack.isEmpty()) {
			return;
		}
		int m = j;
		while (m > 0) {
			if ((m = fillInputSlot(slot, itemStack, m)) != -1) continue;
			return;
		}
	}

	@Unique
	protected int getAmountToFill(boolean craftAll, int limit, boolean recipeInCraftingSlots) {
		int i = 1;
		if (craftAll) {
			i = limit;
		} else if (recipeInCraftingSlots) {
			i = Integer.MAX_VALUE;
			for (Slot slot : handler.getInputSlots()) {
				ItemStack itemStack = slot.getStack();
				if (itemStack.isEmpty() || i <= itemStack.getCount()) continue;
				i = itemStack.getCount();
			}
			if (i != Integer.MAX_VALUE) {
				++i;
			}
		}
		return i;
	}

	@Unique
	protected int fillInputSlot(Slot slot, ItemStack stack, int i) {

		int k;
		int max = -1;
		int itemStackSlot = 0;

		for (int p = 0; p < 36; p++) {
			if (inventory.getStack(p).isEmpty()) {
				continue;
			}
			if (inventory.getStack(p).getItem() == stack.getItem()) {
				if (inventory.getStack(p).getCount() > max) {
					max = inventory.getStack(p).getCount();
					itemStackSlot = p;
				}
			}
		}

		ItemStack itemStack = inventory.getStack(itemStackSlot);


		if (i <= itemStack.getCount()) {
//			inventory.removeStack(j, i);
			MoveUtils.pickup(itemStackSlot, i);

			k = i;
		} else {
//			inventory.removeStack(j);
			MoveUtils.pickupAll(itemStackSlot);
			k = client.player.currentScreenHandler.getCursorStack().getCount();

		}
		if (slot.getStack().isEmpty()) {
//			slot.setStackNoCallbacks(itemStack.copyWithCount(k));
			MoveUtils.putId(slot.id, k);
		} else {
//			slot.getStack().increment(k);
			MoveUtils.putAllId(slot.id);

		}
		return i - k;
	}

	@Unique
	private boolean canReturnInputs() {
		ArrayList<ItemStack> list = Lists.newArrayList();
		int i = getFreeInventorySlots();
		for (Slot slot : handler.getInputSlots()) {
			ItemStack itemStack = slot.getStack().copy();
			if (itemStack.isEmpty()) continue;
			int k = inventory.getOccupiedSlotWithRoomForStack(itemStack);
			if (k == -1 && list.size() <= i) {
				for (ItemStack itemStack2 : list) {
					if (!ItemStack.areItemsEqual(itemStack2, itemStack) || itemStack2.getCount() == itemStack2.getMaxCount() || itemStack2.getCount() + itemStack.getCount() > itemStack2.getMaxCount()) continue;
					itemStack2.increment(itemStack.getCount());
					itemStack.setCount(0);
					break;
				}
				if (itemStack.isEmpty()) continue;
				if (list.size() < i) {
					list.add(itemStack);
					continue;
				}
				return false;
			}
			if (k != -1) continue;
			return false;
		}
		return true;
	}

	@Unique
	private int getFreeInventorySlots() {
		int i = 0;
		// PlayerInventory internals changed; treat slots 0-35 as the main inventory.
		for (int slot = 0; slot < 36; slot++) {
			if (!inventory.getStack(slot).isEmpty()) continue;
			i++;
		}
		return i;
	}
}
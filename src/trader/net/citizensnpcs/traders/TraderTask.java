package net.citizensnpcs.traders;

import java.util.HashMap;
import java.util.Map;

import net.citizensnpcs.economy.Economy;
import net.citizensnpcs.lib.HumanNPC;
import net.citizensnpcs.utils.InventoryUtils;
import net.citizensnpcs.utils.MessageUtils;
import net.citizensnpcs.utils.StringUtils;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.Packet103SetSlot;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftInventoryPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class TraderTask implements Runnable {
	private final HumanNPC npc;
	private final CraftPlayer player;
	private final PlayerInventory previousTraderInv;
	private final PlayerInventory previousPlayerInv;
	private final TraderMode mode;
	private boolean stop;
	private boolean said;
	private int taskID;
	private int prevTraderSlot = -1;
	private int prevPlayerSlot = -1;

	// Gets run every tick, checks the inventory for changes.
	public TraderTask(HumanNPC npc, Player player, TraderMode mode) {
		this.npc = npc;
		this.player = (CraftPlayer) player;
		// Create the inventory objects
		this.previousTraderInv = new CraftInventoryPlayer(
				new net.minecraft.server.PlayerInventory(null));
		this.previousPlayerInv = new CraftInventoryPlayer(
				new net.minecraft.server.PlayerInventory(null));
		// clone the items to the newly created inventory objects
		clonePlayerInventory(npc.getInventory(), this.previousTraderInv);
		clonePlayerInventory(player.getInventory(), this.previousPlayerInv);

		this.mode = mode;
		sendJoinMessage();
	}

	public void addID(int ID) {
		this.taskID = ID;
	}

	private boolean checkContainer(EntityPlayer handle) {
		return handle.activeContainer == handle.defaultContainer;
	}

	private boolean checkMiscellaneous(PlayerInventory inv,
			Stockable stockable, boolean buying) {
		ItemStack stocking = stockable.getStocking();
		if (buying) {
			if (!InventoryUtils.has(npc.getPlayer(), stocking)) {
				sendNoMoneyMessage(stocking, false);
				return true;
			}
			if (!Economy.hasEnough(player, stockable.getPrice().getPrice())) {
				sendNoMoneyMessage(stocking, true);
				return true;
			}
		} else {
			if (!InventoryUtils.has(player, stocking)) {
				sendNoMoneyMessage(stocking, true);
				return true;
			}
			if (mode != TraderMode.INFINITE
					&& !Economy.hasEnough(npc, stockable.getPrice().getPrice())) {
				sendNoMoneyMessage(stocking, false);
				return true;
			}
		}

		return false;
	}

	private ItemStack cloneItemStack(ItemStack source) {
		return source == null ? null : source.clone();
	}

	private void clonePlayerInventory(PlayerInventory source,
			PlayerInventory target) {
		target.setContents(source.getContents()); // getContents() clones for
													// us.

		target.setHelmet(cloneItemStack(source.getHelmet()));
		target.setChestplate(cloneItemStack(source.getChestplate()));
		target.setLeggings(cloneItemStack(source.getLeggings()));
		target.setBoots(cloneItemStack(source.getBoots()));
	}

	private Stockable getStockable(ItemStack item, String keyword,
			boolean selling) {
		// durability needs to be reset to 0 for tools / weapons / armor
		short durability = item.getDurability();
		if (InventoryUtils.isTool(item.getTypeId())
				|| InventoryUtils.isArmor(item.getTypeId())) {
			durability = 0;
		}
		Trader trader = npc.getType("trader");
		if (!trader.isStocked(item.getTypeId(), durability, selling)) {
			player.sendMessage(StringUtils.wrap(
					MessageUtils.getItemName(item.getTypeId()), ChatColor.RED)
					+ " isn't being " + keyword + " here.");
			return null;
		}
		return trader.getStockable(item.getTypeId(), durability, selling);
	}

	@SuppressWarnings("deprecation")
	private void handlePlayerClick(int slot, PlayerInventory playerInv) {
		playerInv.setItem(slot, previousPlayerInv.getItem(slot));
		Stockable stockable = getStockable(playerInv.getItem(slot), "bought",
				true);
		if (stockable == null) {
			return;
		}
		if (prevPlayerSlot != slot) {
			prevPlayerSlot = slot;
			sendStockableMessage(stockable);
			return;
		}
		prevPlayerSlot = slot;
		prevTraderSlot = -1;
		if (checkMiscellaneous(playerInv, stockable, false)) {
			return;
		}
		ItemStack selling = stockable.getStocking().clone();
		if (mode != TraderMode.INFINITE) {
			Economy.pay(npc, stockable.getPrice().getPrice());
		}
		InventoryUtils.removeItems(player, selling, slot);
		Map<Integer, ItemStack> unsold = new HashMap<Integer, ItemStack>();
		Trader trader = npc.getType("trader");
		if (mode != TraderMode.INFINITE) {
			if (!trader.isLocked())
				unsold = npc.getInventory().addItem(selling);
		}
		if (unsold.size() >= 1) {
			rewind();
			player.sendMessage(ChatColor.RED
					+ "Not enough room available to add "
					+ MessageUtils.getStackString(selling, ChatColor.RED)
					+ " to the trader's stock.");
			return;
		}
		double price = stockable.getPrice().getPrice();
		Economy.add(player.getName(), price);
		npc.getPlayer().updateInventory();
		player.updateInventory();
		player.sendMessage(ChatColor.GREEN + "Transaction successful.");
	}

	@SuppressWarnings("deprecation")
	private void handleTraderClick(int slot, PlayerInventory npcInv) {
		npcInv.setItem(slot, previousTraderInv.getItem(slot));
		Stockable stockable = getStockable(npcInv.getItem(slot), "sold", false);
		if (stockable == null) {
			return;
		}
		if (prevTraderSlot != slot) {
			prevTraderSlot = slot;
			sendStockableMessage(stockable);
			return;
		}
		prevTraderSlot = slot;
		prevPlayerSlot = -1;
		if (checkMiscellaneous(npcInv, stockable, true)) {
			return;
		}
		ItemStack buying = stockable.getStocking().clone();
		if (mode != TraderMode.INFINITE) {
			InventoryUtils.removeItems(npc.getPlayer(), buying, slot);
		}
		Map<Integer, ItemStack> unbought = player.getInventory()
				.addItem(buying);
		if (unbought.size() >= 1) {
			rewind();
			player.sendMessage(ChatColor.RED
					+ "Not enough room in your inventory to add "
					+ MessageUtils.getStackString(buying, ChatColor.RED) + ".");
			return;
		}
		npc.getAccount().add(stockable.getPrice().getPrice());
		npc.getPlayer().updateInventory();
		player.updateInventory();
		player.sendMessage(ChatColor.GREEN + "Transaction successful.");
	}

	public void kill() {
		Trader trader = npc.getType("trader");
		trader.setFree(true);
		if (!said) {
			sendLeaveMessage();
			said = true;
		}
		Bukkit.getScheduler().cancelTask(taskID);
		run();
	}

	@SuppressWarnings("deprecation")
	private void rewind() {
		player.getInventory().setContents(previousPlayerInv.getContents());
		npc.getInventory().setContents(previousTraderInv.getContents());
		player.updateInventory();
		npc.getPlayer().updateInventory();
	}

	@Override
	public void run() {
		if (stop) {
			return;
		}
		if (!said
				&& (npc == null || player == null
						|| checkContainer(player.getHandle()) || !player
							.isOnline())) {
			kill();
			return;
		}
		if (mode == TraderMode.STOCK) {
			return;
		}
		stop = true;
		int count = 0;
		boolean found = false;
		net.minecraft.server.ItemStack inhand = player.getHandle().inventory
				.l();
		for (ItemStack i : npc.getInventory().getContents()) {
			if (!previousTraderInv.getItem(count).equals(i) && inhand == null) {
				rewind();
				break;
			}
			if (!previousTraderInv.getItem(count).equals(i)
					&& previousTraderInv.getItem(count).getTypeId() == inhand.id) {
				found = true;
				handleTraderClick(count, npc.getInventory());
				break;
			}
			count += 1;
		}

		count = 0;
		if (!found) {
			for (ItemStack i : player.getInventory().getContents()) {
				if (!previousPlayerInv.getItem(count).equals(i)
						&& inhand == null) {
					rewind();
					break;
				}
				if (!previousPlayerInv.getItem(count).equals(i)
						&& previousPlayerInv.getItem(count).getTypeId() == inhand.id) {
					handlePlayerClick(count, player.getInventory());
					break;
				}
				count += 1;
			}
		}

		clonePlayerInventory(npc.getInventory(), this.previousTraderInv);
		clonePlayerInventory(player.getInventory(), this.previousPlayerInv);

		// Set the itemstack in the player's cursor to null.
		player.getHandle().inventory.b((net.minecraft.server.ItemStack) null);
		// Get rid of the picture on the cursor.
		Packet103SetSlot packet = new Packet103SetSlot(-1, -1, null);
		player.getHandle().netServerHandler.sendPacket(packet);
		stop = false;
	}

	private void sendJoinMessage() {
		switch (mode) {
		case INFINITE:
		case NORMAL:
			player.sendMessage(ChatColor.GREEN + "Transaction log");
			player.sendMessage(ChatColor.GOLD
					+ "-------------------------------");
			break;
		case STOCK:
			player.sendMessage(ChatColor.GOLD + "Stocking of "
					+ StringUtils.wrap(npc.getName(), ChatColor.GOLD)
					+ " started.");
			break;
		}
	}

	private void sendLeaveMessage() {
		switch (mode) {
		case INFINITE:
		case NORMAL:
			player.sendMessage(ChatColor.GOLD
					+ "-------------------------------");
			break;
		case STOCK:
			player.sendMessage(ChatColor.GOLD + "Stocking of "
					+ StringUtils.wrap(npc.getName(), ChatColor.GOLD)
					+ " finished.");
			break;
		}
	}

	private void sendNoMoneyMessage(ItemStack stocking, boolean selling) {
		String start = "The trader doesn't";
		if (selling) {
			start = "You don't";
		}
		player.sendMessage(ChatColor.RED + start
				+ " have enough money available to buy "
				+ MessageUtils.getStackString(stocking) + ".");
	}

	private void sendStockableMessage(Stockable stockable) {
		String[] message = TraderMessageUtils.getStockableMessage(stockable,
				ChatColor.AQUA).split("for");
		player.sendMessage(ChatColor.AQUA + "Item: " + message[0].trim());
		player.sendMessage(ChatColor.AQUA + "Price: " + message[1].trim());
		player.sendMessage(ChatColor.GOLD + "Click to confirm.");
	}
}
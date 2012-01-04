package net.citizensnpcs.npcdata;

import java.util.List;
import java.util.Map;

import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.lib.HumanNPC;
import net.citizensnpcs.utils.InventoryUtils;
import net.citizensnpcs.utils.InventoryUtils.Armor;
import net.citizensnpcs.utils.MessageUtils;
import net.citizensnpcs.utils.Messaging;
import net.citizensnpcs.utils.StringUtils;
import net.citizensnpcs.waypoints.PathEditor;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class NPCDataManager {
	// TODO: make editors an interface.
	public static final Map<String, PathEditor> pathEditors = Maps.newHashMap();
	public static final Map<String, Integer> equipmentEditors = Maps
			.newHashMap();
	public static final Map<String, Integer> selectedNPCs = Maps.newHashMap();

	// Adds items to an npc so that they are visible.
	public static void addItems(HumanNPC npc, List<ItemData> items) {
		if (items != null) {
			npc.setItemInHand(items.get(0).getID() == 0 ? null : items.get(0)
					.createStack());
			for (int i = 0; i < items.size() - 1; i++) {
				Armor.getArmor(i).set(
						npc.getInventory(),
						items.get(i + 1).getID() == 0 ? null : items.get(i + 1)
								.createStack());
			}
			npc.getNPCData().setItems(items);
		}
	}

	public static void deselectNPC(Player player) {
		selectedNPCs.remove(player.getName());
	}

	// equip an NPC based on a player's item-in-hand
	@SuppressWarnings("deprecation")
	private static void equip(Player player, HumanNPC npc) {
		// TODO: cleanup
		ItemStack hand = player.getItemInHand();
		PlayerInventory npcInv = npc.getInventory();
		List<ItemData> items = Lists.newArrayList();
		items.add(new ItemData(npc.getItemInHand().getTypeId(), npc
				.getItemInHand().getDurability()));
		for (Armor armor : Armor.values()) {
			items.add(new ItemData(armor.get(npcInv).getTypeId(), armor.get(
					npcInv).getDurability()));
		}
		List<ItemStack> toAdd = Lists.newArrayList();
		if (player.getItemInHand() == null
				|| player.getItemInHand().getType() == Material.AIR) {
			boolean found = false;
			for (int i = 0; i < items.size(); i++) {
				if (items.get(i).getID() != 0) {
					toAdd.add(items.get(i).createStack());
					found = true;
				}
				items.set(i, new ItemData(0, (short) 0));
			}
			player.sendMessage(found ? StringUtils.wrap(npc.getName())
					+ " is now naked. Here are the items!" : ChatColor.GRAY
					+ "There were no items to take.");
		} else {
			int itemID = hand.getTypeId();
			String error = npc.getName() + " is already equipped with "
					+ MessageUtils.getMaterialName(itemID) + ".";
			String slot = "";
			if (player.isSneaking()) {
				if (Material.getMaterial(items.get(0).getID()) == Material
						.getMaterial(itemID)) {
					Messaging.sendError(player, error);
					return;
				}
				slot = "item-in-hand";
				if (npc.getItemInHand().getType() != Material.AIR) {
					toAdd.add(items.get(0).createStack());
				}
				items.set(0,
						new ItemData(hand.getTypeId(), hand.getDurability()));
			} else {
				Armor armor = Armor.getArmorSlot(itemID);
				if (armor != null) {
					if (armor.get(npcInv).getType() == Material
							.getMaterial(itemID)) {
						Messaging.sendError(player, error);
						return;
					}
					slot = armor.name().toLowerCase();
					if (armor.get(npcInv).getType() != Material.AIR) {
						toAdd.add(items.get(armor.getSlot() + 1).createStack());
					}
					items.set(
							armor.getSlot() + 1,
							new ItemData(hand.getTypeId(), hand.getDurability()));
				} else {
					if (Material.getMaterial(items.get(0).getID()) == Material
							.getMaterial(itemID)) {
						Messaging.sendError(player, error);
						return;
					}
					slot = "item-in-hand";
					if (npc.getItemInHand().getType() != Material.AIR) {
						toAdd.add(items.get(0).createStack());
					}
					items.set(
							0,
							new ItemData(hand.getTypeId(), hand.getDurability()));
				}
			}
			player.sendMessage(StringUtils.wrap(npc.getName() + "'s ") + slot
					+ " was set to "
					+ StringUtils.wrap(MessageUtils.getMaterialName(itemID))
					+ ".");
		}
		// remove item that was added to NPC
		InventoryUtils.decreaseItemInHand(player);
		// add all items to the player's inventory AFTER in-hand item was
		// removed
		boolean drop = false;
		for (ItemStack i : toAdd) {
			// drop items that don't fit in a player's inventory
			for (ItemStack unadded : player.getInventory().addItem(i).values()) {
				player.getWorld().dropItemNaturally(player.getLocation(),
						unadded);
				drop = true;
			}
		}
		if (drop) {
			Messaging
					.sendError(player,
							"Some items couldn't fit in your inventory and were dropped at your location.");
		}
		player.updateInventory();

		addItems(npc, items);
		npc.despawn();
		npc.spawn();
	}

	public static int getSelected(Player player) {
		return selectedNPCs.get(player.getName());
	}

	public static void handleEquipmentEditor(NPCRightClickEvent event) {
		Player player = event.getPlayer();
		HumanNPC npc = event.getNPC();
		if (equipmentEditors.containsKey(player.getName())
				&& equipmentEditors.get(player.getName()) == npc.getUID()) {
			equip(player, npc);
		}
	}

	public static void handlePathEditor(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (pathEditors.get(player.getName()) == null)
			return;
		PathEditor session = pathEditors.get(player.getName());
		switch (event.getAction()) {
		case LEFT_CLICK_BLOCK:
			session.onLeftClick(event.getClickedBlock());
			break;
		case RIGHT_CLICK_BLOCK:
		case RIGHT_CLICK_AIR:
			session.onRightClick(event.getClickedBlock());
			break;
		}
	}

	public static void selectNPC(Player player, HumanNPC npc) {
		selectedNPCs.put(player.getName(), npc.getUID());
	}
}
package net.citizensnpcs.listeners;

import net.citizensnpcs.Citizens;
import net.citizensnpcs.Settings;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.event.NPCTalkEvent;
import net.citizensnpcs.api.event.NPCTargetEvent;
import net.citizensnpcs.lib.HumanNPC;
import net.citizensnpcs.lib.NPCManager;
import net.citizensnpcs.lib.creatures.CreatureTask;
import net.citizensnpcs.npcdata.NPCDataManager;
import net.citizensnpcs.npctypes.CitizensNPC;
import net.citizensnpcs.permissions.PermissionManager;
import net.citizensnpcs.properties.properties.UtilityProperties;
import net.citizensnpcs.utils.MessageUtils;
import net.citizensnpcs.utils.Messaging;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.plugin.PluginManager;

public class EntityListen extends EntityListener implements Listener {

	@Override
	public void onEntityDamage(EntityDamageEvent event) {
		CreatureTask.onDamage(event.getEntity(), event);
		HumanNPC npc = NPCManager.get(event.getEntity());
		if (npc != null) {
			npc.callDamageEvent(event);
		}
		if (!(event instanceof EntityDamageByEntityEvent))
			return;
		EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
		if (npc != null) {
			if (e.getDamager() instanceof Player) {
				Player player = (Player) e.getDamager();
				for (CitizensNPC type : npc.types())
					type.onLeftClick(player, npc);
			}
		} else if (e.getDamager() instanceof Player) {
			CreatureTask.onDamage(e.getEntity(), event);
		}
	}

	@Override
	public void onEntityDeath(EntityDeathEvent event) {
		CreatureTask.onEntityDeath(event.getEntity());
		if (!NPCManager.isNPC(event.getEntity()))
			return;
		for (CitizensNPC type : NPCManager.get(event.getEntity()).types()) {
			type.onDeath(event);
		}
	}

	@Override
	public void onEntityTarget(EntityTargetEvent event) {
		if (!(event instanceof NPCTargetEvent)) {
			return;
		}
		if (CreatureTask.getCreature(event.getEntity()) != null) {
			CreatureTask.getCreature(event.getEntity()).onRightClick(
					(Player) event.getTarget());
		}
		if (NPCManager.isNPC(event.getTarget())) {
			NPCManager.get(event.getTarget()).callTargetEvent(event);
		}
		NPCTargetEvent e = (NPCTargetEvent) event;
		HumanNPC npc = NPCManager.get(e.getEntity());
		if (npc != null && event.getTarget() instanceof Player) {
			Player player = (Player) event.getTarget();
			if (npc.getNPCData().isLookClose()) {
				NPCManager.faceEntity(npc.getPlayer(), player);
			}
			if (UtilityProperties.isHoldingTool("SelectItems", player)) {
				if (!NPCManager.hasSelected(player, npc.getUID())) {
					NPCDataManager.selectNPC(player, npc);
					if (PermissionManager.hasPermission(player,
							"citizens.basic.modify.select"))
						Messaging.send(player, npc,
								Settings.getString("SelectionMessage"));
					if (!Settings.getBoolean("QuickSelect")) {
						return;
					}
				}
			}
			// Call NPC talk event
			if (npc.getNPCData().isTalk()
					&& UtilityProperties.isHoldingTool("TalkItems", player)) {
				Player target = (Player) e.getTarget();
				NPCTalkEvent talkEvent = new NPCTalkEvent(npc, target,
						MessageUtils.getText(npc, target));
				Bukkit.getServer().getPluginManager().callEvent(talkEvent);
				if (!talkEvent.isCancelled()) {
					if (!talkEvent.getText().isEmpty()) {
						Messaging.send(target, npc, talkEvent.getText());
					}
				}
			}
			NPCRightClickEvent rightClickEvent = new NPCRightClickEvent(npc,
					player);
			Bukkit.getServer().getPluginManager().callEvent(rightClickEvent);
			if (rightClickEvent.isCancelled())
				return;
			NPCDataManager.handleEquipmentEditor(rightClickEvent);
			npc.getWaypoints().delay(Settings.getInt("RightClickPause"));
			for (CitizensNPC type : npc.types()) {
				type.onRightClick(player, npc);
			}
		}
	}

	@Override
	public void registerEvents(Citizens plugin) {
		PluginManager pm = plugin.getServer().getPluginManager();
		pm.registerEvent(Type.ENTITY_DAMAGE, this, Priority.Normal, plugin);
		pm.registerEvent(Type.ENTITY_TARGET, this, Priority.Normal, plugin);
		pm.registerEvent(Type.ENTITY_DEATH, this, Priority.Normal, plugin);
	}
}
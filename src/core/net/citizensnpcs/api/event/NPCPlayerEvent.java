package net.citizensnpcs.api.event;

import net.citizensnpcs.lib.HumanNPC;

import org.bukkit.entity.Player;

public class NPCPlayerEvent extends NPCEvent {
	protected Player player;
	public NPCPlayerEvent(String name, HumanNPC npc, Player player) {
		super(name, npc);
		this.player = player;
	}

	/**
	 * Get the player who right-clicked an NPC
	 * 
	 * @return player who right-clicked NPC
	 */
	public Player getPlayer() {
		return this.player;
	}

	private static final long serialVersionUID = 1L;
}

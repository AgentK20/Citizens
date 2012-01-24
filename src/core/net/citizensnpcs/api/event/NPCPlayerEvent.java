package net.citizensnpcs.api.event;

import net.citizensnpcs.resources.npclib.HumanNPC;

import org.bukkit.entity.Player;

public abstract class NPCPlayerEvent extends NPCEvent {
    protected Player player;
    private static final long serialVersionUID = 1L;

    protected NPCPlayerEvent(String name, HumanNPC npc, Player player) {
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
}

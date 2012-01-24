package net.citizensnpcs.guards;

import net.citizensnpcs.commands.CommandHandler;
import net.citizensnpcs.guards.listeners.GuardCitizensListen;
import net.citizensnpcs.npctypes.CitizensNPC;
import net.citizensnpcs.npctypes.CitizensNPCType;
import net.citizensnpcs.npctypes.NPCTypeManager;
import net.citizensnpcs.properties.Properties;

public class GuardType extends CitizensNPCType {

    @Override
    public Properties getProperties() {
        return GuardProperties.INSTANCE;
    }

    @Override
    public CommandHandler getCommands() {
        return GuardCommands.INSTANCE;
    }

    @Override
    public void registerEvents() {
        NPCTypeManager.registerEvents(new GuardCitizensListen());
    }

    @Override
    public String getName() {
        return "guard";
    }

    @Override
    public CitizensNPC getInstance() {
        return new Guard();
    }
}
package net.citizensnpcs.questers.quests.types;

import net.citizensnpcs.questers.QuestUtils;
import net.citizensnpcs.questers.quests.progress.ObjectiveProgress;
import net.citizensnpcs.questers.quests.progress.QuestUpdater;
import net.citizensnpcs.utils.StringUtils;

import org.bukkit.event.Event;
import org.bukkit.event.Event.Type;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class CollectQuest implements QuestUpdater {
    private static final Type[] EVENTS = { Type.PLAYER_PICKUP_ITEM };

    @Override
    public boolean update(Event event, ObjectiveProgress progress) {
        if (event instanceof PlayerPickupItemEvent) {
            PlayerPickupItemEvent ev = (PlayerPickupItemEvent) event;
            ItemStack stack = ev.getItem().getItemStack();
            progress.addAmount(stack.getAmount());
        }
        return progress.getAmount() >= progress.getObjective().getAmount();
    }

    @Override
    public Type[] getEventTypes() {
        return EVENTS;
    }

    @Override
    public String getStatus(ObjectiveProgress progress) {
        return QuestUtils.defaultAmountProgress(progress, StringUtils.formatter(progress.getObjective().getMaterial())
                .wrap().plural(progress.getAmount())
                + " collected");
    }
}
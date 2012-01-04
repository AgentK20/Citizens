package net.citizensnpcs.alchemists;

import java.util.Map;

import net.citizensnpcs.commands.CommandHandler;
import net.citizensnpcs.lib.HumanNPC;
import net.citizensnpcs.permissions.PermissionManager;
import net.citizensnpcs.sk89q.Command;
import net.citizensnpcs.sk89q.CommandContext;
import net.citizensnpcs.sk89q.CommandPermissions;
import net.citizensnpcs.sk89q.CommandRequirements;
import net.citizensnpcs.sk89q.ServerCommand;
import net.citizensnpcs.utils.HelpUtils;
import net.citizensnpcs.utils.MessageUtils;
import net.citizensnpcs.utils.Messaging;
import net.citizensnpcs.utils.PageUtils;
import net.citizensnpcs.utils.PageUtils.PageInstance;
import net.citizensnpcs.utils.StringUtils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandRequirements(
		requireSelected = true,
		requireOwnership = true,
		requiredType = "alchemist")
public class AlchemistCommands extends CommandHandler {
	private AlchemistCommands() {
	}

	@Override
	public void addPermissions() {
		PermissionManager.addPermission("alchemist.use.help");
		PermissionManager.addPermission("alchemist.use.recipes.select");
		PermissionManager.addPermission("alchemist.use.recipes.view");
		PermissionManager.addPermission("alchemist.modify.recipes");
		PermissionManager.addPermission("alchemist.use.interact");
	}

	@Override
	public void sendHelpPage(CommandSender sender) {
		HelpUtils.header(sender, "Alchemist", 1, 1);
		HelpUtils.format(sender, "alchemist", "recipes",
				"view all of an alchemist's recipes");
		HelpUtils.format(sender, "alchemist", "select [itemID]",
				"select a recipe");
		HelpUtils.format(sender, "alchemist", "view (page)",
				"view an alchemist's selected recipe");
		HelpUtils.format(sender, "alchemist", "add [itemID] [itemID(:amt),]",
				"add a recipe to an alchemist");
		HelpUtils.footer(sender);
	}

	public static final AlchemistCommands INSTANCE = new AlchemistCommands();

	@Command(
			aliases = { "alchemist", "alch" },
			usage = "add [itemID] [itemID(:amt),]",
			desc = "add a custom recipe to an alchemist",
			modifiers = "add",
			min = 3,
			max = 3)
	@CommandPermissions("alchemist.modify.recipes.add")
	public static void add(CommandContext args, Player player, HumanNPC npc) {
		if (!AlchemistManager.checkValidID(player, args.getString(1))) {
			return;
		}
		int itemID = args.getInteger(1);
		Alchemist alchemist = npc.getType("alchemist");
		String recipe = args.getString(2);
		String[] items = recipe.split(",");
		for (String item : items) {
			String[] split = item.split(":");
			switch (split.length) {
			case 1:
				if (Material.getMaterial(StringUtils.parse(split[0])) == null) {
					Messaging.sendError(player,
							MessageUtils.invalidItemIDMessage);
				}
				break;
			case 2:
				if (!StringUtils.isNumber(split[1])) {
					Messaging.sendError(player,
							"The amount specified is not a proper number.");
				}
				break;
			}
		}
		alchemist.addRecipe(itemID, recipe);
		alchemist.setCurrentRecipeID(itemID);
		player.sendMessage(StringUtils.wrap(npc.getName())
				+ " has changed the recipe for "
				+ StringUtils.wrap(MessageUtils.getMaterialName(itemID)) + ".");
	}

	@CommandRequirements()
	@ServerCommand()
	@Command(
			aliases = { "alchemist", "alch" },
			usage = "help",
			desc = "view the alchemist help page",
			modifiers = "help",
			min = 1,
			max = 1)
	@CommandPermissions("alchemist.use.help")
	public static void alchemistHelp(CommandContext args, CommandSender sender,
			HumanNPC npc) {
		INSTANCE.sendHelpPage(sender);
	}

	@CommandRequirements(requireSelected = true, requiredType = "alchemist")
	@Command(
			aliases = { "alchemist", "alch" },
			usage = "recipes (page)",
			desc = "view an alchemist's list of recipes",
			modifiers = "recipes",
			min = 1,
			max = 2)
	@CommandPermissions("alchemist.use.recipes.view")
	public static void recipes(CommandContext args, Player player, HumanNPC npc) {
		Map<Integer, String> recipes = ((Alchemist) npc.getType("alchemist"))
				.getRecipes();
		if (recipes.size() == 0) {
			Messaging.sendError(player, npc.getName()
					+ " has no recipes.");
			return;
		}
		PageInstance instance = PageUtils.newInstance(player);
		int page = 1;
		if (args.argsLength() == 2) {
			if (!StringUtils.isNumber(args.getString(1))) {
				Messaging.sendError(player, "That is not a valid number.");
				return;
			}
			page = args.getInteger(1);
		}
		instance.header(ChatColor.GREEN
				+ StringUtils.listify(StringUtils.wrap(npc.getName()
						+ "'s Recipes " + ChatColor.WHITE + "<%x/%y>")));
		for (int entry : recipes.keySet()) {
			instance.push(" - "
					+ StringUtils.wrap(MessageUtils.getMaterialName(entry))
					+ " (ID: " + entry + ")");
		}
		instance.push(ChatColor.GREEN + "Type "
				+ StringUtils.wrap("/alchemist select [itemID]")
				+ " to select a recipe.");
		instance.push(ChatColor.GREEN + "Type "
				+ StringUtils.wrap("/alchemist view")
				+ " to view ingredients for the recipe.");
		instance.process(page);
	}

	@CommandRequirements(requireSelected = true, requiredType = "alchemist")
	@Command(
			aliases = { "alchemist", "alch" },
			usage = "select [itemID]",
			desc = "select a recipe",
			modifiers = "select",
			min = 2,
			max = 2)
	@CommandPermissions("alchemist.use.recipes.select")
	public static void select(CommandContext args, Player player, HumanNPC npc) {
		if (!AlchemistManager.checkValidID(player, args.getString(1))) {
			return;
		}
		int itemID = args.getInteger(1);
		Alchemist alchemist = npc.getType("alchemist");
		if (alchemist.getRecipe(itemID) == null) {
			Messaging.sendError(player, npc.getName()
					+ " does not have that recipe.");
			return;
		}
		alchemist.setCurrentRecipeID(itemID);
		player.sendMessage(StringUtils.wrap(npc.getName() + "'s")
				+ " recipe has been set to "
				+ StringUtils.wrap(MessageUtils.getMaterialName(itemID)) + ".");
	}

	@CommandRequirements(requireSelected = true, requiredType = "alchemist")
	@Command(
			aliases = { "alchemist", "alch" },
			usage = "view (page)",
			desc = "view the selected alchemist recipe",
			modifiers = "view",
			min = 1,
			max = 2)
	@CommandPermissions("alchemist.use.recipes.view")
	public static void view(CommandContext args, Player player, HumanNPC npc) {
		int page = 1;
		if (args.argsLength() == 2) {
			if (!StringUtils.isNumber(args.getString(1))) {
				Messaging.sendError(player, "That is not a valid number.");
				return;
			}
			page = args.getInteger(1);
		}
		AlchemistManager.sendRecipeMessage(player, npc, page);
	}
}
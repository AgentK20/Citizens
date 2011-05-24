package com.fullwall.Citizens.Utils;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import com.fullwall.Citizens.Properties.Properties.UtilityProperties;

public class StringUtils {

	public static String parseColour(String s) {
		if (s.startsWith("&")) {
			String colour = "";
			colour = s.replace("&", "�").substring(0, 1);
			return colour;
		}
		return "�f";
	}

	public static String stripColour(String toStrip) {
		return ChatColor.stripColor(toStrip);
	}

	public static ArrayList<String> colourise(ArrayList<String> text) {
		int index = 0;
		ArrayList<String> newText = new ArrayList<String>();
		for (String string : text) {
			newText.add(index, string.replace("&", "�"));
			index += 1;
		}
		return newText;
	}

	public static Material parseMaterial(String material) {
		Material mat = Material.matchMaterial(material);
		if (mat == null) {
			if (!UtilityProperties.itemlookups.getString(material).isEmpty()) {
				mat = parseMaterial(UtilityProperties.itemlookups
						.getString(material));
			} else if (isNumber(material)) {
				mat = Material.getMaterial(Integer.parseInt(material));
			} else {
				return mat;
			}
		}
		return mat;
	}

	private static boolean isNumber(String material) {
		try {
			Integer.parseInt(material);
			return true;
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	/**
	 * Makes a string yellow, with a trailing green colour.
	 * 
	 * @param string
	 * @return
	 */
	public static String wrap(String string) {
		return ChatColor.YELLOW + string + ChatColor.GREEN;
	}

	/**
	 * Makes a double a yellow string with a trailing green colour.
	 * 
	 * @param string
	 * @return
	 */
	public static String wrap(double string) {
		return ChatColor.YELLOW + "" + (int) string + ChatColor.GREEN;
	}

	/**
	 * Makes a string yellow, with a given colour trailing.
	 * 
	 * @param string
	 * @param trailing
	 * @return
	 */
	public static String wrap(String string, ChatColor trailing) {
		return ChatColor.YELLOW + string + trailing;
	}

	/**
	 * Makes a double a yellow string, with a given colour trailing.
	 * 
	 * @param string
	 * @param trailing
	 * @return
	 */
	public static String wrap(double string, ChatColor trailing) {
		return ChatColor.YELLOW + "" + (int) string + trailing;
	}

	/**
	 * Makes a string yellow, with a trailing green colour. Based on '{' and '}'
	 * in the string.
	 * 
	 * @param string
	 * @return
	 */
	public static String wrapFull(String string) {
		return ChatColor.GREEN
				+ string.replace("{", "" + ChatColor.YELLOW).replace("}",
						"" + ChatColor.GREEN);
	}

	/**
	 * Makes a string yellow, with a specified tailing colour. Based on '{' and
	 * '}' in the string.
	 * 
	 * @param string
	 * @return
	 */
	public static String wrapFull(String string, ChatColor colour) {
		return colour
				+ string.replace("{", "" + ChatColor.YELLOW).replace("}",
						"" + colour);
	}

	/**
	 * Parse an int from a string value
	 * 
	 * @param passed
	 * @return
	 */
	public static int parse(String passed) {
		return Integer.parseInt(passed);
	}
}
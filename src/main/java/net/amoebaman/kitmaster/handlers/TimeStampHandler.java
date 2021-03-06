package net.amoebaman.kitmaster.handlers;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;

import net.amoebaman.kitmaster.KitMaster;
import net.amoebaman.kitmaster.enums.Attribute;
import net.amoebaman.kitmaster.enums.GiveKitResult;
import net.amoebaman.kitmaster.objects.Kit;
import net.amoebaman.kitmaster.sql.SQLQueries;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;


public class TimeStampHandler {
	
	private static YamlConfiguration yamlConfig;
	
	public static void load(File file) throws IOException{
		yamlConfig = YamlConfiguration.loadConfiguration(file);
	}
	
	public static void save(File file) throws IOException{
		OfflinePlayer player;
		Kit kit;
		for(String name : yamlConfig.getKeys(false))
			if(yamlConfig.isConfigurationSection(name))
				/*
				 * Purge expired/irrelevant timestamps
				 * We need this to keep the file from becoming horribly massive on large servers
				 */
				for(String kitName : yamlConfig.getConfigurationSection(name).getKeys(false)){
					player = Bukkit.getOfflinePlayer(name);
					kit = KitHandler.getKit(kitName);
					if(kit == null || (!kit.booleanAttribute(Attribute.SINGLE_USE) && !timeoutRemaining(player, kit).isEmpty()))
						yamlConfig.set(name + "." + kitName, null);
				}
		
		yamlConfig.save(file);
	}
	
	public static boolean hasOverride(Player player, Kit kit){
		return player.hasPermission("kitmaster.notimeout") || player.hasPermission("kitmaster.notimeout." + kit.name);
	}
	
	public static long getTimeStamp(OfflinePlayer player, Kit kit){
		String name = player == null ? "global" : player.getName();
		if(KitMaster.isSQLRunning()){
			String query = SQLQueries.GET_TIMESTAMP.replace(SQLQueries.PLAYER_MACRO, name).replace(SQLQueries.KIT_MACRO, kit.name);
			ResultSet set = KitMaster.getSQL().executeQuery(query);
			Long stamp = KitMaster.getSQL().getFirstResult(set, kit.name, Long.class);
			return stamp == null ? 0 : stamp;
		}
		else{
			ConfigurationSection playerSection = yamlConfig.getConfigurationSection(name);
			if(playerSection == null){
				yamlConfig.createSection(name);
				return 0;
			}
			return playerSection.getLong(kit.name, 0);
		}
	}
	
	public static void setTimeStamp(OfflinePlayer player, Kit kit){
		String name = player == null ? "global" : player.getName();
		if(KitMaster.isSQLRunning())
			KitMaster.getSQL().executeCommand(SQLQueries.UPDATE_TIMESTAMP.replace(SQLQueries.PLAYER_MACRO, name).replace(SQLQueries.KIT_MACRO, kit.name).replace(SQLQueries.TIMESTAMP_MACRO, "" + System.currentTimeMillis()));
		else{
			ConfigurationSection playerSection = yamlConfig.getConfigurationSection(name);
			if(playerSection == null)
				playerSection = yamlConfig.createSection(name);
			playerSection.set(kit.name, System.currentTimeMillis());
		}
	}
	
	public static void clearTimeStamp(OfflinePlayer player, Kit kit){
		String name = player == null ? "global" : player.getName();
		if(KitMaster.isSQLRunning())
			KitMaster.getSQL().executeCommand(SQLQueries.UPDATE_TIMESTAMP.replace(SQLQueries.PLAYER_MACRO, name).replace(SQLQueries.KIT_MACRO, kit.name).replace(SQLQueries.TIMESTAMP_MACRO, "0"));
		else{
			ConfigurationSection playerSection = yamlConfig.getConfigurationSection(name);
			if(playerSection == null)
				playerSection = yamlConfig.createSection(name);
			playerSection.set(kit.name, null);
		}
	}
	
	public static GiveKitResult timeoutCheck(OfflinePlayer player, Kit kit){
		long stamp = getTimeStamp(kit.booleanAttribute(Attribute.GLOBAL_TIMEOUT) ? null : player, kit);
		long timeout = kit.integerAttribute(Attribute.TIMEOUT);
		if(System.currentTimeMillis() - stamp < timeout * 1000)
			return GiveKitResult.FAIL_TIMEOUT;	
		if(stamp > 0 && (kit.booleanAttribute(Attribute.SINGLE_USE) || kit.booleanAttribute(Attribute.SINGLE_USE_LIFE)))
			return GiveKitResult.FAIL_SINGLE_USE;
		return GiveKitResult.SUCCESS;
	}
	
	public static String timeoutRemaining(OfflinePlayer player, Kit kit){
		long stamp = getTimeStamp(kit.booleanAttribute(Attribute.GLOBAL_TIMEOUT) ? null : player, kit);
		long timeout = kit.integerAttribute(Attribute.TIMEOUT) * 1000;
		int seconds = (int)((stamp + timeout - System.currentTimeMillis()) / 1000), minutes = 0, hours = 0, days = 0;
		seconds = seconds < 0 ? 0 : seconds;
		if(seconds >= 60){
			minutes = seconds / 60;
			seconds %= 60;
			if(minutes >= 60){
				hours = minutes / 60;
				minutes %= 60;
				if(hours >= 24){
					days = hours / 24;
					hours %= 24;
				}
			}
		}
		String result = "";
		if(days > 0) result += days + " days ";
		if(hours > 0) result += hours + " hours ";
		if(minutes > 0) result += minutes + " minutes ";
		if(seconds > 0) result += seconds + " seconds ";
		return result.trim();
	}
	
}

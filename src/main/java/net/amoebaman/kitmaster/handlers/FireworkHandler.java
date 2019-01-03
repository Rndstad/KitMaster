package net.amoebaman.kitmaster.handlers;

import java.util.ArrayList;
import java.util.List;

import net.amoebaman.kitmaster.KitMaster;

import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

public class FireworkHandler {

	public static ConfigurationSection yaml;
	
	private static ConfigurationSection getSection(String name){
		ConfigurationSection section = yaml.getConfigurationSection(name);
		if(section == null)
			for(String key : yaml.getKeys(false))
				if(key.equalsIgnoreCase(name))
					section = yaml.getConfigurationSection(key);
		return section;
	}

	public static boolean isFirework(String name){
		return getSection(name) != null;
	}
	
	public static void saveFirework(ItemStack firework, String name){
		if(firework.getType() != Material.LEGACY_FIREWORK)
			return;
		FireworkMeta meta = (FireworkMeta) firework.getItemMeta();
		ConfigurationSection fireworkYaml = yaml.createSection(name);
		fireworkYaml.set("fuse", meta.getPower());
		List<String> bursts = new ArrayList<String>();
		for(int i = 0; i < meta.getEffectsSize(); i++){
			FireworkEffect burst = meta.getEffects().get(i);
			String burstName;
			if(FireworkEffectHandler.getEffectName(burst) == null){
				burstName =  name + "_burst_" + i;
				FireworkEffectHandler.saveFirework(burst, burstName);
			}
			else
				burstName = FireworkEffectHandler.getEffectName(burst);
			bursts.add(burstName);
		}
		fireworkYaml.set("bursts", bursts);
	}
	
	public static ItemStack loadFirework(ItemStack firework, String name){
		if(firework.getType() != Material.LEGACY_FIREWORK)
			return firework;
		ConfigurationSection fireworkYaml = getSection(name);
		if(fireworkYaml == null)
			return firework;
		FireworkMeta meta = (FireworkMeta) firework.getItemMeta();
		if(KitMaster.config().getBoolean("inventory.renameFireworks", true))
			meta.setDisplayName(fireworkYaml.getName());
		meta.setPower(fireworkYaml.getInt("fuse"));
		for(String burstName : fireworkYaml.getStringList("bursts"))
			meta.addEffect(FireworkEffectHandler.getFirework(burstName));
		firework.setItemMeta(meta);
		return firework;
	}
	
	public static ItemStack getFirework(String name){
		return loadFirework(new ItemStack(Material.LEGACY_FIREWORK), name);
	}
	
	public static String getFireworkName(ItemStack firework){
		for(String name : yaml.getKeys(false))
			if(getFirework(name).getItemMeta().equals(firework.getItemMeta()))
				return name;
		return null;
	}

}

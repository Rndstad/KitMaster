package net.amoebaman.kitmaster.controllers;

import java.util.Random;

import net.amoebaman.kitmaster.KitMaster;
import net.amoebaman.kitmaster.handlers.BookHandler;
import net.amoebaman.kitmaster.handlers.CustomItemHandler;
import net.amoebaman.kitmaster.handlers.CustomPotionHandler;
import net.amoebaman.kitmaster.handlers.FireworkEffectHandler;
import net.amoebaman.kitmaster.handlers.FireworkHandler;
import net.amoebaman.kitmaster.objects.Armor;
import net.amoebaman.kitmaster.objects.Enchant;
import net.amoebaman.kitmaster.objects.Weapon;
import net.amoebaman.kitmaster.utilities.ParseItemException;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@SuppressWarnings("deprecation")
public class ItemController {

	/**
	 * Parses an ItemStack from a String.  The proper format of the String is [item name](:[tag]):[amount] ([enchantment name]:[level]...).
	 * @param str the string
	 * @return the result
	 * @throws ParseItemException if the argument was not formatted properly
	 */
	public static ItemStack parseItem(String str) throws ParseItemException{
		try{
			String[] alphaSplit = str.split(" ");
			String[] betaSplit = alphaSplit[0].split(":");

			ItemStack stack = getBaseStack(betaSplit[0]);

			if(betaSplit.length > 1){
				if(isInt(betaSplit[betaSplit.length - 1]))
					stack.setAmount(Integer.parseInt(betaSplit[betaSplit.length - 1]));
				else
					throw new ParseItemException("invalid amount specified", str);
			}

			if(betaSplit.length > 2)
				for(int i = 1; i < betaSplit.length - 1; i++){
					try{
						stack = applyTag(stack, betaSplit[i]);
					}
					catch(ParseItemException pie){ throw pie; }
				}

			for (int i = 1; i < alphaSplit.length;i++){
				Enchant ewl = parseEnchantment(alphaSplit[i].trim());
				if(ewl == null)
					throw new ParseItemException("invalid enchantment specified", str);
				stack.addUnsafeEnchantment(ewl.enc, ewl.lvl);
			}

			return stack;
		}
		catch(Exception e){
			e.printStackTrace();
			throw new ParseItemException("unable to parse item", str);
		}
	}
	
	/**
	 * Parses a PotionEffect from a String.  The proper format of the String is [effect name]:[level](:[duration in seconds]).
	 * @param str the string
	 * @return the result
	 * @throws ParseItemException if the argument was not formatted properly
	 */
	public static PotionEffect parseEffect(String str) throws ParseItemException{
		try{
			String[] split = str.split(":");
			PotionEffectType type = PotionEffectType.getByName(split[0]);
			if(type == null)
				type = matchPotionEffect(split[0]);
			if(type == null && isInt(split[0])){
				type = PotionEffectType.getById(Integer.parseInt(split[0]));
			}
			if(type == null)
				type = PotionEffectType.getByName(split[0]);
			if(type == null)
				throw new ParseItemException("invalid effect specified", str);
			return new PotionEffect(type, split.length > 2 ? Integer.parseInt(split[2]) * 20 : 1200, split.length > 1 ? Integer.parseInt(split[1]) - 1 : 0);
		}
		catch(NumberFormatException e){
			throw new ParseItemException("invalid amplifier/duration", str);
		}
	}
	
	/**
	 * Parses an Enchant from a String.  The proper format of the String is [enchantment name]:[level].
	 * @param str the String
	 * @return the result
	 */
	public static Enchant parseEnchantment(String str) {
		str = str.toLowerCase();
		String[] split = str.split(":");
		Enchantment enc = Enchantment.getByName(split[0].toUpperCase());
		if(enc == null)
			enc = matchEnchantment(split[0]);
		if(enc == null)
			return null;
		int lvl = 1;
		if(split.length > 1)
			lvl = isInt(split[1]) ? Integer.parseInt(split[1]) : 1;
			return new Enchant(enc, lvl);
	}
	
	/**
	 * Retrives a base ItemStack by its name.  If Vault is enabled, this will also attempt to use Vault's item matching methods.  Unless a custom item is found, the result will always have no data, no metadata, no enchantments, and a quantity of 1.
	 * @param name the name of the desired ItemStack.  This can be the name of a saved custom item, the name of a material, or an integer item ID value.
	 * @return the item
	 */
	public static ItemStack getBaseStack(String name) throws ParseItemException{
		if(CustomItemHandler.isCustomItem(name))
			return CustomItemHandler.getCustomItem(name);
		name = name.toLowerCase();
		Material type = null;
		if(type == null){
			try{ type = Material.getMaterial(name); }
			catch(NumberFormatException e){}
		}
		if(type == null)
			type = Material.matchMaterial(name);
		if((name.toLowerCase().contains("spawn") || name.toLowerCase().contains("mob")) && name.toLowerCase().contains("egg"))
			type = Material.LEGACY_MONSTER_EGG;
		if(name.toLowerCase().contains("skull"))
			type = Material.LEGACY_SKULL_ITEM;
		if(name.toLowerCase().contains("dye"))
			type = Material.LEGACY_INK_SACK;
		if(type == null && KitMaster.isVaultEnabled() && !name.toLowerCase().contains("legg")) {
			if(name.contains("DIAMOND")) {
				type = Material.DIAMOND_LEGGINGS;
			} else if (name.contains("GOLD")) {
				type = Material.GOLDEN_LEGGINGS;
			} else if (name.contains("IRON")) {
				type = Material.IRON_LEGGINGS;
			} else if (name.contains("LEATHER")) {
				type = Material.LEATHER_LEGGINGS;
			} else {
				type = null;
			}
		}
		if(type == null)
			throw new ParseItemException("unable to find matching material", name);
		return new ItemStack(type);
	}

	/**
	 * Applys a String tag to an item.  Depending on the Material of the item, this may be handled different ways.
	 * @param stack the item
	 * @param tag the tag
	 * @return a copy of the item with the tag applied
	 * @throws ParseItemException if the tag is improperly formatted
	 */
	public static ItemStack applyTag(ItemStack stack, String tag) throws ParseItemException{
		if(!tag.isEmpty()){
			try{
				switch(stack.getType()){

				case LEGACY_MONSTER_EGG:
				case LEGACY_MOB_SPAWNER:
					EntityType entity = EntityType.fromName(tag);
					if(entity != null && entity.isSpawnable() && entity.isAlive())
						stack.setDurability(entity.getTypeId());
					break;
				case WRITTEN_BOOK:
				case LEGACY_BOOK_AND_QUILL:
						stack = BookHandler.loadBook(stack, tag);
					break;
				case LEATHER_HELMET:
				case LEATHER_CHESTPLATE:
				case LEATHER_LEGGINGS:
				case LEATHER_BOOTS:
					if(tag.startsWith("0x") || tag.startsWith("#")){
						LeatherArmorMeta leatherMeta = (LeatherArmorMeta) stack.getItemMeta();
						leatherMeta.setColor(Color.fromRGB(Integer.decode(tag.replace("#", "0x"))));
						stack.setItemMeta(leatherMeta);
					}
					break;
				case LEGACY_SKULL_ITEM:
					SkullMeta skullMeta = (SkullMeta) stack.getItemMeta();
					skullMeta.setOwner(tag);
					stack.setItemMeta(skullMeta);
					stack.setDurability((short) 3);
					break;
				case LEGACY_SKULL:
					if(tag.toLowerCase().toLowerCase().contains("skeleton"))
						stack.setDurability((short) 0);
					if(tag.toLowerCase().toLowerCase().contains("wither"))
						stack.setDurability((short) 1); 
					if(tag.toLowerCase().toLowerCase().contains("zombie"))
						stack.setDurability((short) 2);
					if(tag.toLowerCase().toLowerCase().contains("steve") || tag.toLowerCase().toLowerCase().contains("human"))
						stack.setDurability((short) 3);
					if(tag.toLowerCase().toLowerCase().contains("creeper"))
						stack.setDurability((short) 4);
					break;
				case LEGACY_FIREWORK_CHARGE:
					FireworkEffectMeta fireworkMeta = (FireworkEffectMeta) stack.getItemMeta();
					fireworkMeta.setEffect(FireworkEffectHandler.getFirework(tag));
					stack.setItemMeta(fireworkMeta);
					break;
				case LEGACY_FIREWORK:
					stack = FireworkHandler.loadFirework(stack, tag);
					break;
				case POTION:
					if(CustomPotionHandler.isPotion(tag))
						stack = CustomPotionHandler.loadPotion(stack, tag);
					else
						stack.setDurability(parsePotionData(tag));
					break;
				case LEGACY_LOG:
				case LEGACY_LEAVES:
				case LEGACY_WOOD:
				case LEGACY_WOOD_STEP:
				case LEGACY_WOOD_DOUBLE_STEP:
				case LEGACY_WOOD_STAIRS:
					if(tag.equalsIgnoreCase("oak") || tag.equalsIgnoreCase("normal"))
						stack.setDurability((short) 0);
					if(tag.equalsIgnoreCase("spruce") || tag.equalsIgnoreCase("pine") || tag.equalsIgnoreCase("dark"))
						stack.setDurability((short) 1);
					if(tag.equalsIgnoreCase("birch") || tag.equalsIgnoreCase("light"))
						stack.setDurability((short) 2);
					if(tag.equalsIgnoreCase("jungle") || tag.equalsIgnoreCase("red"))
						stack.setDurability((short) 3);
					break;
				case LEGACY_WOOL:
					if(!isInt(tag) && DyeColor.valueOf(tag.toUpperCase()) != null)
						stack.setDurability(DyeColor.valueOf(tag.toUpperCase()).getWoolData());
					break;
				case LEGACY_INK_SACK:
					if(!isInt(tag) && DyeColor.valueOf(tag.toUpperCase()) != null)
						stack.setDurability(DyeColor.valueOf(tag.toUpperCase()).getDyeData());
					break;
				case LEGACY_STEP:
					if(tag.equalsIgnoreCase("stone"))
						stack.setDurability((short) 0);
					if(tag.toLowerCase().contains("sandstone"))
						stack.setDurability((short) 1);
					if(tag.toLowerCase().contains("cobble"))
						stack.setDurability((short) 3);
					if(tag.equalsIgnoreCase("brick"))
						stack.setDurability((short) 4);
					if(tag.toLowerCase().contains("stone") && tag.toLowerCase().contains("brick"))
						stack.setDurability((short) 5);
					if(tag.toLowerCase().contains("seam"))
						stack.setDurability((short) 6);
					break;
				case SANDSTONE:
					if(tag.toLowerCase().contains("chiseled") || tag.toLowerCase().contains("creeper"))
						stack.setDurability((short) 1);
					if(tag.toLowerCase().contains("smooth") || tag.toLowerCase().contains("brick"))
						stack.setDurability((short) 2);
					break;
				case LEGACY_MONSTER_EGGS:
					if(tag.equalsIgnoreCase("stone") || tag.toLowerCase().contains("smooth"))
						stack.setDurability((short) 0);
					if(tag.toLowerCase().contains("brick"))
						stack.setDurability((short) 1);
					if(tag.toLowerCase().contains("cobble"))
						stack.setDurability((short) 2);
					break;
				case LEGACY_SMOOTH_BRICK:
					if(tag.toLowerCase().contains("mossy"))
						stack.setDurability((short) 1);
					if(tag.toLowerCase().contains("cracked") || tag.toLowerCase().contains("broken"))
						stack.setDurability((short) 2);
					if(tag.toLowerCase().contains("chiseled") || tag.toLowerCase().contains("circle"))
						stack.setDurability((short) 3);
					break;
				case COBBLESTONE:
					if(tag.toLowerCase().contains("mossy"))
						stack.setDurability((short) 1);
					break;
				case GOLDEN_APPLE:
					if(tag.toLowerCase().contains("enchanted") || tag.toLowerCase().contains("power") || tag.toLowerCase().contains("super"))
						stack.setDurability((short) 1);
					break;
				default: }

				if(isInt(tag))
					stack.setDurability(Short.parseShort(tag)); 
			}	
			catch(NumberFormatException nfe){ throw new ParseItemException("invalid item tag specified", tag); }
		}
		return stack;
	}

	/**
	 * Gets the reader- and parser-friendly tag that represents an ItemStack's data value or metadata set.
	 * @param stack the item
	 * @return the result
	 */
	public static String getTag(ItemStack stack, boolean create){
		String name = null;
		switch(stack.getType()){
		case LEGACY_MONSTER_EGG:
		case LEGACY_MOB_SPAWNER:
			return EntityType.fromId(stack.getDurability()).name().toLowerCase();
		case WRITTEN_BOOK:
		case LEGACY_BOOK_AND_QUILL:
			name = BookHandler.getBookName(stack);
			if(name != null)
				return name;
			else if(create){
				name = "autosaved_" + (new Random()).nextInt(Short.MAX_VALUE);
				BookHandler.saveBook(stack, name);
				return name;
			}
			break;
		case LEATHER_HELMET:
		case LEATHER_CHESTPLATE:
		case LEATHER_LEGGINGS:
		case LEATHER_BOOTS:
			return "0x" + Integer.toHexString(((LeatherArmorMeta) stack.getItemMeta()).getColor().asRGB()).toUpperCase();
		case LEGACY_SKULL_ITEM:
			return ((SkullMeta) stack.getItemMeta()).getOwner();
		case LEGACY_FIREWORK_CHARGE:
			name = FireworkEffectHandler.getEffectName(((FireworkEffectMeta) stack.getItemMeta()).getEffect());
			if(name != null)
				return name;
			else if(create){
				name = "autosaved_" + (new Random()).nextInt(Short.MAX_VALUE);
				FireworkEffectHandler.saveFirework(((FireworkEffectMeta) stack.getItemMeta()).getEffect(), name);
				return name;
			}
			break;
		case LEGACY_FIREWORK:
			name = FireworkHandler.getFireworkName(stack);
			if(name != null)
				return name;
			else if(create){
				name = "autosaved_" + (new Random()).nextInt(Short.MAX_VALUE);
				FireworkHandler.saveFirework(stack, name);
				return name;
			}
			break;
		case POTION:
			name = CustomPotionHandler.getPotionName(stack);
			if(name != null)
				return stack.getDurability() + ":" + name;
			else if(create){
				name = "autosaved_" + (new Random()).nextInt(Short.MAX_VALUE);
				CustomPotionHandler.savePotion(stack, name);
				return stack.getDurability() + ":" + name;
			}
			break;
		case LEGACY_LOG:
		case LEGACY_LEAVES:
		case LEGACY_WOOD:
		case LEGACY_WOOD_STEP:
		case LEGACY_WOOD_DOUBLE_STEP:
		case LEGACY_WOOD_STAIRS:
			switch(stack.getDurability()){
			case 0: return "oak";
			case 1: return "spruce";
			case 2: return "birch";
			case 3: return "jungle";
			}
		case LEGACY_WOOL:
			return DyeColor.getByWoolData((byte) stack.getDurability()).name().toLowerCase();
		case LEGACY_INK_SACK:
			return DyeColor.getByDyeData((byte) stack.getDurability()).name().toLowerCase();
		case LEGACY_STEP:
			switch(stack.getDurability()){
			case 0: return "stone";
			case 1: return "sandstone";
			case 2: return null;
			case 3: return "cobblestone";
			case 4: return "brick";
			case 5: return "stone_brick";
			case 6: return "seamless";
			}
		case SANDSTONE:
			switch(stack.getDurability()){
			case 0: return null;
			case 1: return "chiseled";
			case 2: return "smooth";
			}
		case LEGACY_MONSTER_EGGS:
			switch(stack.getDurability()){
			case 0: return "stone";
			case 1: return "stone_brick";
			case 2: return "cobblestone";
			}
		case LEGACY_SMOOTH_BRICK:
			switch(stack.getDurability()){
			case 0: return null;
			case 1: return "mossy";
			case 2: return "cracked";
			case 3: return "chiseled";
			}
		case COBBLESTONE:
			switch(stack.getDurability()){
			case 0: return null;
			case 1: return "mossy";
			}
		case GOLDEN_APPLE:
			switch(stack.getDurability()){
			case 0: return null;
			case 1: return "enchanted";
			}
		default: }
		if(stack.getDurability() != 0)
			return String.valueOf(stack.getDurability());
		else
			return null;
	}

	/**
	 * Gets a parser-friendly String that represents an ItemStack.
	 * @param stack the item
	 * @return the result
	 */
	public static String itemToString(ItemStack stack, boolean includeNameAndLore){
		if(stack == null)
			return null;
		String str = stack.getType().name().toLowerCase();
		if((stack.getItemMeta().hasDisplayName() || stack.getItemMeta().hasLore()) && includeNameAndLore){
			str += "_autosaved_" + (new Random()).nextInt(Short.MAX_VALUE);
			CustomItemHandler.saveCustomItem(stack, str);
		}
		if(getTag(stack, true) != null)
			str += ":" + getTag(stack, true);
		str += ":" + stack.getAmount();
		for(Enchantment enc : stack.getEnchantments().keySet())
			str += " " + enc.getName().toLowerCase() + ":" + stack.getEnchantmentLevel(enc);
		return str;
	}

	/**
	 * Gets a reader-friendly String that represents an ItemStack.
	 * @param stack the item
	 * @return the result
	 */
	public static String friendlyItemString(ItemStack stack){
		String str = capitalize(stack.getItemMeta().hasDisplayName() ? stack.getItemMeta().getDisplayName() : stack.getType().name().toLowerCase().replace("_", " ")) + (stack.getAmount() > 1 ? "s" : "");
		if(!stack.getItemMeta().hasDisplayName()){
			String tag = getTag(stack, false);
			if(tag != null)
				str = capitalize(tag) + " " + str;
		}
		str = stack.getAmount() + " " + str;
		if(!stack.getEnchantments().isEmpty())
			str += " with ";
		boolean first = true;
		for(Enchantment enc : stack.getEnchantments().keySet()){
			str += capitalize((first ? " " : ", ") + getEnchantmentName(enc)) + " " + romanNumerals(stack.getEnchantmentLevel(enc));
			first = false;
		}
		return str;
	}

	/**
	 * Gets a parser-friendly String that represents a PotionEffect.
	 * @param effect the effect
	 * @return the result
	 */
	public static String effectToString(PotionEffect effect){
		return effect.getType().getName().toLowerCase() + ":" + (effect.getAmplifier() + 1) + ":" + (effect.getDuration() / 20);
	}
	
	/**
	 * Gets a reader-friendly String that represents a PotionEffect.
	 * @param effect the effect
	 * @return the result
	 */
	public static String friendlyEffectString(PotionEffect effect){
		return capitalize(getPotionEffectName(effect.getType())) + " " + romanNumerals(effect.getAmplifier() + 1) + " for " + effect.getDuration()/20 + " seconds";
	}
	
	/**
	 * Matches an Enchantment from a more common name.
	 * @param name the common name of an Enchantment
	 * @return the Enchantment matched, or null if no match was found
	 */
	public static Enchantment matchEnchantment(String name){
		name = name.toLowerCase();
		if(name.contains("fire") && name.toLowerCase().contains("prot")) return Enchantment.PROTECTION_FIRE;
		if((name.contains("exp") || name.toLowerCase().contains("blast")) && name.toLowerCase().contains("prot")) return Enchantment.PROTECTION_EXPLOSIONS;
		if((name.contains("arrow") || name.toLowerCase().contains("proj")) && name.toLowerCase().contains("prot")) return Enchantment.PROTECTION_PROJECTILE;
		if(name.contains("prot")) return Enchantment.PROTECTION_ENVIRONMENTAL;
		if(name.contains("fall")) return Enchantment.PROTECTION_FALL;
		if(name.contains("respiration")) return Enchantment.OXYGEN;
		if(name.contains("aqua")) return Enchantment.WATER_WORKER;
		if(name.contains("sharp")) return Enchantment.DAMAGE_ALL;
		if(name.contains("smite")) return Enchantment.DAMAGE_UNDEAD;
		if(name.contains("arth")) return Enchantment.DAMAGE_ARTHROPODS;
		if(name.contains("knockback")) return Enchantment.KNOCKBACK;
		if(name.contains("fire")) return Enchantment.FIRE_ASPECT;
		if(name.contains("loot")) return Enchantment.LOOT_BONUS_MOBS;
		if(name.contains("power")) return Enchantment.ARROW_DAMAGE;
		if(name.contains("punch")) return Enchantment.ARROW_KNOCKBACK;
		if(name.contains("flame")) return Enchantment.ARROW_FIRE;
		if(name.contains("infin")) return Enchantment.ARROW_INFINITE; 
		if(name.contains("dig") || name.toLowerCase().contains("eff")) return Enchantment.DIG_SPEED;
		if(name.contains("dura") || name.toLowerCase().contains("break")) return Enchantment.DURABILITY;
		if(name.contains("silk")) return Enchantment.SILK_TOUCH;
		if(name.contains("fort")) return Enchantment.LOOT_BONUS_BLOCKS;
		if(name.contains("thorn")) return Enchantment.THORNS;
		return null;
	}
	
	/**
	 * Gets the more common name of an Enchantment.
	 * @param enc the Enchantment
	 * @return the common name
	 */
	public static String getEnchantmentName(Enchantment enc){
		if(enc.equals(Enchantment.ARROW_DAMAGE)) return "power";
		if(enc.equals(Enchantment.ARROW_FIRE)) return "flame";
		if(enc.equals(Enchantment.ARROW_INFINITE)) return "infinity";
		if(enc.equals(Enchantment.ARROW_KNOCKBACK)) return "punch";
		if(enc.equals(Enchantment.DAMAGE_ALL)) return "sharpness";
		if(enc.equals(Enchantment.DAMAGE_ARTHROPODS)) return "bane of arthropods";
		if(enc.equals(Enchantment.DAMAGE_UNDEAD)) return "smite";
		if(enc.equals(Enchantment.DIG_SPEED)) return "efficiency";
		if(enc.equals(Enchantment.DURABILITY)) return "unbreaking";
		if(enc.equals(Enchantment.FIRE_ASPECT)) return "fire aspect";
		if(enc.equals(Enchantment.KNOCKBACK)) return "knockback";
		if(enc.equals(Enchantment.LOOT_BONUS_BLOCKS)) return "fortune";
		if(enc.equals(Enchantment.LOOT_BONUS_MOBS)) return "looting";
		if(enc.equals(Enchantment.OXYGEN)) return "respiration";
		if(enc.equals(Enchantment.PROTECTION_ENVIRONMENTAL)) return "protection";
		if(enc.equals(Enchantment.PROTECTION_EXPLOSIONS)) return "blast protection";
		if(enc.equals(Enchantment.PROTECTION_FALL)) return "feather falling";
		if(enc.equals(Enchantment.PROTECTION_FIRE)) return "fire protection";
		if(enc.equals(Enchantment.PROTECTION_PROJECTILE)) return "projectile protection";
		if(enc.equals(Enchantment.SILK_TOUCH)) return "silk touch";
		if(enc.equals(Enchantment.THORNS)) return "thorns";
		if(enc.equals(Enchantment.WATER_WORKER)) return "aqua affinity";
		return null;
	}

	/**
	 * Matches a PotionEffectType from a more common name.
	 * @param name the common name of a PotionEffectType
	 * @return the PotionEffectType matched, or null if none was found
	 */
	public static PotionEffectType matchPotionEffect(String name){
		name = name.toLowerCase();
		if(name.contains("regen")) return PotionEffectType.REGENERATION;
		if(name.contains("poison")) return PotionEffectType.POISON;
		if(name.contains("strength")) return PotionEffectType.INCREASE_DAMAGE;
		if(name.contains("weak")) return PotionEffectType.WEAKNESS;
		if(name.contains("abs")) return PotionEffectType.ABSORPTION;
		if(name.contains("health")) return PotionEffectType.HEALTH_BOOST;
		if(name.contains("sat")) return PotionEffectType.SATURATION;
		if(name.contains("heal")) return PotionEffectType.HEAL;
		if(name.contains("harm")) return PotionEffectType.HARM;
		if(name.contains("speed") || name.toLowerCase().contains("swift")) return PotionEffectType.SPEED;
		if(name.contains("slow")) return PotionEffectType.SLOW;
		if(name.contains("haste")) return PotionEffectType.FAST_DIGGING;
		if(name.contains("fat")) return PotionEffectType.SLOW_DIGGING;
		if(name.contains("hung")) return PotionEffectType.HUNGER;
		if(name.contains("resist")) return PotionEffectType.DAMAGE_RESISTANCE;
		if(name.contains("blind")) return PotionEffectType.BLINDNESS;
		if(name.contains("confus") || name.toLowerCase().contains("naus")) return PotionEffectType.CONFUSION;
		if(name.contains("fire")) return PotionEffectType.FIRE_RESISTANCE;
		if(name.contains("jump")) return PotionEffectType.JUMP;
		if(name.contains("water") || name.toLowerCase().contains("aqua")) return PotionEffectType.WATER_BREATHING;
		if(name.contains("invis")) return PotionEffectType.INVISIBILITY;
		if(name.contains("night")) return PotionEffectType.NIGHT_VISION;
		if(name.contains("wither")) return PotionEffectType.WITHER;
		return null;
	}

	/**
	 * Gets the more common name of a PotionEffectType.
	 * @param effect the PotionEffectType
	 * @return the common name
	 */
	public static String getPotionEffectName(PotionEffectType effect){
		if(effect == null)
			return "null";
		if(effect.equals(PotionEffectType.BLINDNESS)) return "blindness";
		if(effect.equals(PotionEffectType.CONFUSION)) return "nausea";
		if(effect.equals(PotionEffectType.DAMAGE_RESISTANCE)) return "resistance";
		if(effect.equals(PotionEffectType.FAST_DIGGING)) return "haste";
		if(effect.equals(PotionEffectType.FIRE_RESISTANCE)) return "fire resistance";
		if(effect.equals(PotionEffectType.HARM)) return "harming";
		if(effect.equals(PotionEffectType.HEAL)) return "instant health";
		if(effect.equals(PotionEffectType.HUNGER)) return "hunger";
		if(effect.equals(PotionEffectType.INCREASE_DAMAGE)) return "strength";
		if(effect.equals(PotionEffectType.INVISIBILITY)) return "invisibility";
		if(effect.equals(PotionEffectType.JUMP)) return "jump boost";
		if(effect.equals(PotionEffectType.NIGHT_VISION)) return "night vision";
		if(effect.equals(PotionEffectType.POISON)) return "poison";
		if(effect.equals(PotionEffectType.REGENERATION)) return "regeneration";
		if(effect.equals(PotionEffectType.SLOW)) return "slowness";
		if(effect.equals(PotionEffectType.SLOW_DIGGING)) return "mining fatigue";
		if(effect.equals(PotionEffectType.SPEED)) return "swiftness";
		if(effect.equals(PotionEffectType.WATER_BREATHING)) return "water breathing";
		if(effect.equals(PotionEffectType.WEAKNESS)) return "weakness";
		if(effect.equals(PotionEffectType.WITHER)) return "wither";
		if(effect.equals(PotionEffectType.ABSORPTION)) return "absorption";
		if(effect.equals(PotionEffectType.HEALTH_BOOST)) return "health boost";
		if(effect.equals(PotionEffectType.SATURATION)) return "saturation";
		return "unknown";
	}

	/**
	 * Attempts to generate potion data based on a common name.
	 * @param name the common name of the desired potion
	 * @return the corresponding item data value
	 */
	public static short parsePotionData(String name){
		name = name.toLowerCase();
		if(isInt(name))
			return Short.parseShort(name);
		short data = 0;
		if(name.contains("water")) return 0;
		if(name.contains("mundane")) data = 0;
		if(name.contains("clear")) data = 7;
		if(name.contains("diffuse")) data = 11;
		if(name.contains("artless")) data = 13;
		if(name.contains("thin")) data = 15;
		if(name.contains("awkward")) data = 16;
		if(name.contains("bungling")) data = 23;
		if(name.contains("smooth")) data = 27;
		if(name.contains("suave")) data = 29;
		if(name.contains("debonair")) data = 31;
		if(name.contains("thick")) data = 32;
		if(name.contains("charming")) data = 39;
		if(name.contains("refined")) data = 43;
		if(name.contains("cordial")) data = 45;
		if(name.contains("sparkling")) data = 47;
		if(name.contains("potent")) data = 48;
		if(name.contains("rank")) data = 55;
		if(name.contains("acrid")) data = 59;
		if(name.contains("gross")) data = 61;
		if(name.contains("stinky")) data = 63;
		
		if(name.contains("regen")) data = 1;
		if(name.contains("speed") || name.contains("swift")) data = 2;
		if(name.contains("fire")) data = 3;
		if(name.contains("poison")) data = 4;
		if(name.contains("heal")) data = 5;
		if(name.contains("night")) data = 6;
		if(name.contains("weak")) data = 8;
		if(name.contains("strength")) data = 9;
		if(name.contains("slow")) data = 10;
		if(name.contains("harm") || name.contains("damage")) data = 12;
		if(name.contains("invis")) data = 14;

		if(name.contains("power") || name.contains("level")) data += 32;
		if(name.contains("ext") || name.contains("long")) data += 64;
		if(name.contains("splash") || name.contains("throw")) data += 16384; else data += 8192;
		
		return data;
	}

	private static final String[] romanNumerals = new String[]{"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
	private static final int[] ints = new int[]{1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
	
	/**
	 * Gets a roman numeral value for any number less than 4000.
	 * @param number the number to convert
	 * @return the roman numeral in String form
	 */
	public static String romanNumerals(int number){
		if(number <= 0 || number > 4000)
			return String.valueOf(number);
		String roman = "";
		for(int i = 0; i < romanNumerals.length; i++)
			while(number >= ints[i]){
				number -= ints[i];
				roman += romanNumerals[i];
			}
		return roman;
	}

	/**
	 * Captializes every word in a String, regardless of grammar rules.
	 * @param str the String to capitalize
	 * @return a capitalized copy of the String
	 */
	public static String capitalize(String str){
		String[] words = str.split(" ");
		String result = "";
		for(String word : words){
			if(word.length() <= 1)
				result += word.toUpperCase() + " ";
			else
				result += word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase() + " ";
		}
		return result.trim();
	}

	private static boolean isInt(String str){ try{ Integer.parseInt(str); return true; }catch(Exception e){ return false; } }
	
	public static boolean areSimilar(ItemStack a, ItemStack b){
		return a.isSimilar(b) || ((Weapon.isValid(a.getType()) || Armor.isValid(a.getType())) && a.getType() == b.getType());
	}
	
}

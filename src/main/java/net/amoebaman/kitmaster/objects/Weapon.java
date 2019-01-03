package net.amoebaman.kitmaster.objects;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Represents a weapon
 * 
 * @author Dennison
 */
public class Weapon{

	/** The type of the weapon */
	public WeaponType type;

	/** The level of the weapon */
	public WeaponLevel lvl;

	/** The interally saved ItemStack */
	private ItemStack item;

	/**
	 * Constructs a weapon with the given type and level
	 * @param type the type of weapon
	 * @param lvl the level of weapon
	 */
	public Weapon(WeaponType type, WeaponLevel lvl){
		this.type = type;
		this.lvl = lvl;
		item = getRawItem();
	}

	/**
	 * Constructs a Weapon from the Material that it should represent
	 * @param mat the Material
	 */
	public Weapon(Material mat){
		if(mat != null){
			if(mat.name().contains("SWORD"))
				type = WeaponType.SWORD;
			if(mat.name().contains("AXE"))
				type = WeaponType.AXE;
			if(mat.name().contains("BOW")){
				type = WeaponType.BOW;
				lvl = WeaponLevel.WOOD;
			}

			if(mat.name().contains("DIAMOND"))
				lvl = WeaponLevel.DIAMOND;
			if(mat.name().contains("IRON"))
				lvl = WeaponLevel.IRON;
			if(mat.name().contains("GOLD"))
				lvl = WeaponLevel.GOLD;
			if(mat.name().contains("STONE"))
				lvl = WeaponLevel.STONE;
			if(mat.name().contains("WOOD"))
				lvl = WeaponLevel.WOOD;
			item = getRawItem();
		}
		
	}

	/**
	 * Constructs an Weapon from an ItemStack of the Material that it should represent
	 * @param stack the ItemStack
	 */
	public Weapon(ItemStack stack){
		this(stack == null ? null : stack.getType());
		item = stack;
	}


	/**
	 * Gets the ItemStack that is currently in a player's weapon (held item) slot
	 * @param player the player to check
	 * @return the ItemStack
	 */
	public static ItemStack getExisting(Player player){
		return player.getItemInHand();
	}


	/**
	 * Tests whether or not this Weapon is superior to another
	 * @param other the other Weapon
	 * @return true if this Weapon is superior to the other
	 */
	public boolean isBetterThan(Weapon other){
		if(!isValid())
			return false;
		if(!other.isValid())
			return true;
		if(type == other.type)
			return lvl.ordinal() >= other.lvl.ordinal();
			else
				return type.ordinal() >= other.type.ordinal();
	}

	/**
	 * Puts this Weapon in a Player's held item slot
	 * @param player the Player to give this Weapon to
	 */
	public void putInSlot(Player player){
		player.setItemInHand(item);
	}

	/**
	 * Tests whether or not this Weapon represents a real Material
	 * @return true if this Weapon is valid
	 */
	public boolean isValid(){
		return type != null && lvl != null && item != null;
	}

	/**
	 * Tests whether or not a valid Weapon can be constructed from this Material
	 * @return true if a construction call with this Material will produce a valid Weapon
	 */
	public static boolean isValid(Material mat){
		return new Weapon(mat).isValid();
	}

	/**
	 * Gets the Material that would represent this Weapon
	 * @return the Material
	 */
	public Material getMaterial(){
		if(type == null || lvl == null)
			return null;
		switch(type){
		case AXE:
			switch(lvl){
			case DIAMOND:
				return Material.DIAMOND_AXE;
			case GOLD:
				return Material.GOLDEN_AXE;
			case IRON:
				return Material.IRON_AXE;
			case STONE:
				return Material.STONE_AXE;
			case WOOD:
				return Material.WOODEN_AXE;
			default:
				return Material.AIR;
			}
		case SWORD:
			switch(lvl){
			case DIAMOND:
				return Material.DIAMOND_SWORD;
			case GOLD:
				return Material.GOLDEN_SWORD;
			case IRON:
				return Material.IRON_SWORD;
			case STONE:
				return Material.STONE_SWORD;
			case WOOD:
				return Material.WOODEN_SWORD;
			default:
				return Material.AIR;
			}
		case BOW:
			return Material.BOW;
		default:
			return Material.AIR;
		}
	}

	public WeaponType getType(){ return type; }

	public WeaponLevel getLevel(){ return lvl; }

	public ItemStack getItem(){ return item; }

	/**
	 * Gets an ItemStack containing one of the Material that this Weapon represents
	 * @return the ItemStack
	 */
	public ItemStack getRawItem(){
		if(getMaterial() == null)
			return null;
		return new ItemStack(getMaterial());
	}

	/** The levels of weapon.  Higher ordinals correspond to more powerful weapons. */
	public enum WeaponLevel{
		/** Wooden weapons */
		WOOD,
		/** Stone weapons */
		STONE,
		/** Golden (butter) weapons */
		GOLD,
		/** Iron weapons */
		IRON,
		/** Diamond weapons */
		DIAMOND
	};

	/** The types of weapon.  Higher ordinals correspond to more powerful weapons. */
	public enum WeaponType{
		/** Axes */
		AXE,
		/** Bows */
		BOW,
		/** Swords */
		SWORD
	};
}

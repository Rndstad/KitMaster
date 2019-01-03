package net.amoebaman.kitmaster.handlers;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.google.common.collect.Lists;

public class BookHandler {

	public static ConfigurationSection yaml;
	
	private static ConfigurationSection getSection(String name){
		ConfigurationSection section = yaml.getConfigurationSection(name);
		if(section == null)
			for(String key : yaml.getKeys(false))
				if(key.equalsIgnoreCase(name))
					section = yaml.getConfigurationSection(key);
		return section;
	}

	public static boolean isBook(String name){
		return getSection(name) != null;
	}
	
	public static void saveBook(ItemStack book, String name){
		if(book.getType() != Material.WRITTEN_BOOK && book.getType() != Material.LEGACY_BOOK_AND_QUILL)
			return;
		BookMeta meta = (BookMeta) book.getItemMeta();
		ConfigurationSection bookYaml = yaml.createSection(name);
		bookYaml.set("title", meta.getTitle());
		bookYaml.set("author", meta.getAuthor());
		List<String> pages = Lists.newArrayList(meta.getPages());
		for(int i = 0; i < pages.size(); i++)
			pages.set(i, new String(pages.get(i)).replace("\n", "|n").replace("\r", "|r"));
		bookYaml.set("pages", pages);
	}
	
	public static ItemStack loadBook(ItemStack book, String name){
		if(book.getType() != Material.WRITTEN_BOOK && book.getType() != Material.LEGACY_BOOK_AND_QUILL)
			return book;
		ConfigurationSection bookYaml = getSection(name);
		if(bookYaml == null)
			return book;
		BookMeta meta = (BookMeta) book.getItemMeta();
		meta.setTitle(bookYaml.getString("title"));
		meta.setAuthor(bookYaml.getString("author"));
		List<String> pages = bookYaml.getStringList("pages");
		for(int i = 0; i < pages.size(); i++)
			pages.set(i, pages.get(i).replace("|n", "\n").replace("|r", "\r"));
		meta.setPages(pages);
		book.setItemMeta(meta);
		return book;
	}
	
	public static ItemStack getBook(String name){
		return loadBook(new ItemStack(Material.WRITTEN_BOOK), name);
	}
	
	public static ItemStack getEditableBook(String name){
		return loadBook(new ItemStack(Material.LEGACY_BOOK_AND_QUILL), name);
	}
	
	public static String getBookName(ItemStack book){
		for(String name : yaml.getKeys(false))
			if(getBook(name).getItemMeta().equals(book.getItemMeta()))
				return name;
		return null;
	}
	
}

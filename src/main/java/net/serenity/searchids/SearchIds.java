package net.serenity.searchids;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;
import java.lang.String;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;

/**
 * @title SearchIds
 * @description Search for data id's in game or from the console
 * @date 2011-01-16
 * @author croemmich
 * @maintainer mrapple
 */
public class SearchIds extends JavaPlugin {

	protected static final Logger log = Logger.getLogger("Minecraft");
	public String name = "";
	public String version = "";

	private static String propFile = "search-ids.properties";
	private static iProperty props;

	// Properties
	public static String searchType = "all";
	public static String dataXml = "search-ids-data.xml";
	public static String updateSource = "https://github.com/skipperguy12/SearchIds/raw/master/search-ids-data.xml";
	public static boolean autoUpdate = true;
	public static String base = "decimal";
	public static String baseId = "decimal";
	public static int nameWidth = 24;
	public static int numWidth = 4;
	public static String delimiter = "-";
	public static int autoUpdateInterval = 86400;

	public static DataParser parser;
	private UpdateThread updateThread;

	@Override
	public void onDisable() {
		parser = null;
		log.info(name + " " + version + " disabled");
	}

	@Override
	public void onEnable() {
		PluginDescriptionFile pdfFile = this.getDescription();

		name = pdfFile.getName();
		version = pdfFile.getVersion();

		log.info(name + " version " + version + " is enabled!.");

		if (!initProps()) {
			log.severe(name + ": Could not initialise " + propFile);
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		if (parser == null)
			parser = new DataParser();

		if (!initData()) {
			log.severe(name + ": Could not init the search data from: " + dataXml + ". Please check that the file exists and is not corrupt.");
			if (!autoUpdate) {
				log.severe(name + ": Set auto-update-data=true in " + propFile + " to automatically download the search data file " + dataXml);
			}
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		if (autoUpdate) {
			if (updateThread == null)
				updateThread = new UpdateThread(this);
			updateThread.start();
		}
	}

	public boolean initProps() {
		File folder = new File("plugins/SearchIds/");
		folder.mkdir();

		props = new iProperty("plugins/SearchIds/" + propFile);

		// Properties
		searchType = props.getString("search-type", "all");
		base = props.getString("base", "decimal");
		baseId = props.getString("base-id", "decimal");
		dataXml = props.getString("data-xml", "search-ids-data.xml");
		updateSource = props.getString("update-source", "https://github.com/mrapple/SearchIds/raw/master/search-ids-data.xml");
		autoUpdate = props.getBoolean("auto-update-data", true);
		autoUpdateInterval = props.getInt("auto-update-interval", 86400);
		nameWidth = props.getInt("width-blockname", 24);
		numWidth = props.getInt("width-number", 4);
		delimiter = props.getString("delimiter", "-");

		if (autoUpdateInterval < 600) {
			autoUpdateInterval = 600;
			log.info(name + ": auto-update-interval cannot be less than 600");
		}

		File file = new File("plugins/SearchIds/" + propFile);
		return file.exists();
	}

	public boolean initData() {
		if (dataXml == null || dataXml.equals("")) {
			return false;
		}

		File f = new File("plugins/SearchIds/" + dataXml);
		if (!updateData() && !f.exists()) {
			return false;
		}

		if (parser.search("test") == null) {
			return false;
		}
		return true;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		String commandName = command.getName();

		if (commandName.equalsIgnoreCase("search")) {
			if (args.length > 0) {
				String query = "";
				for (int i = 0; i < args.length; i++) {
					query += (args[i] + " ");
				}
				query = query.trim();
				printSearchResults(sender, parser.search(query, base), query);
				return true;
			} else {
				return false;
			}
		} else
			return false;
	}

	public boolean updateData() {
		if (autoUpdate) {
			try {
				URL url = new URL(updateSource);
				log.info(name + ": Updating data from " + updateSource + "...");
				InputStream is = url.openStream();
				FileOutputStream fos = null;
				fos = new FileOutputStream("plugins/SearchIds/" + dataXml);
				int oneChar, count = 0;
				while ((oneChar = is.read()) != -1) {
					fos.write(oneChar);
					count++;
				}
				is.close();
				fos.close();
				log.info(name + ": Update complete!");
				return true;
			} catch (MalformedURLException e) {
				log.severe(e.toString());
			} catch (IOException e) {
				log.severe(e.toString());
			}
			log.info(name + ": Could not update search data.");
			return false;
		} else {
			return true;
		}
	}

	public void printSearchResults(CommandSender sender, ArrayList<Result> results, String query) {
		if (results != null && results.size() > 0) {
			sender.sendMessage(ChatColor.AQUA + "Search results for \"" + query + "\":");
			Iterator<Result> itr = results.iterator();
			String line = "";
			int num = 0;
			while (itr.hasNext()) {
				num++;
				Result result = itr.next();
				line += (rightPad(result.getFullValue(), result.getValuePad()) + " " + delimiter + " " + rightPad(result.getName(), nameWidth));
				if (num % 2 == 0 || !itr.hasNext()) {
					sender.sendMessage(ChatColor.GOLD + line.trim());
					line = "";
				}
				if (num > 16) {
					sender.sendMessage(ChatColor.RED + "Not all results are displayed. Make your term more specific!");
					break;
				}
			}
		} else {
			sender.sendMessage(ChatColor.RED + "No results found");
		}
	}

	public static String leftPad(String s, int width) {
		return String.format("%" + width + "s", s);
	}

	public static String rightPad(String s, int width) {
		return String.format("%-" + width + "s", s);
	}
}

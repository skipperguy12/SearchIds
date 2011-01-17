package org.bukkit.croemmich.searchids;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;

/**
* @title  SearchIds
* @description Search for data id's in game or from the console
* @date 2011-01-16
* @author croemmich
*/
public class SearchIds extends JavaPlugin  {

	protected static final Logger log = Logger.getLogger("Minecraft");
    private final SearchIdsPlayerListener playerListener = new SearchIdsPlayerListener(this);
	public  static String name    = "SearchIds";
	public  static String version = "1.1";

	private static String propFile = "search-ids.properties";
	private static iProperty props;
	
	// Properties
	public  static String  searchType         = "all";
	public  static String  dataXml            = "search-ids-data.xml";
	public  static String  updateSource       = "https://cr-wd.com/minecraft/plugins/SearchIds/data.xml";
	public  static boolean autoUpdate         = true;
	public  static String  searchCommand      = "search";
	public  static String  base               = "decimal";
	public  static int     nameWidth          = 24;
	public  static int     numWidth           = 4;
	public  static String  delimiter          = "-";
	public  static int     autoUpdateInterval = 86400;
	
	public  static DataParser  parser;
	private UpdateThread updateThread;
	
	public SearchIds(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
		super(pluginLoader, instance, desc, folder, plugin, cLoader);
		
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_COMMAND, playerListener, Priority.Normal, this);
	}
	

	@Override
	public void onDisable() {
		parser = null;
		log.info(name + " " + version + " disabled");
	}

	@Override
	public void onEnable() {
		log.info(name + " " + version + " enabled");
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
		props = new iProperty(propFile);
		
		// Properties
		searchType         = props.getString("search-type", "all");
		base               = props.getString("base", "decimal");
		searchCommand      = props.getString("command", "search");
		dataXml            = props.getString("data-xml", "search-ids-data.xml");
		updateSource       = props.getString("update-source", "https://github.com/croemmich/SearchIds/raw/master/search-ids-data.xml");
		autoUpdate         = props.getBoolean("auto-update-data", true);
		autoUpdateInterval = props.getInt("auto-update-interval", 86400);
		nameWidth          = props.getInt("width-blockname", 24);
		numWidth           = props.getInt("width-number", 4);
		delimiter          = props.getString("delimiter", "-");
		
		if (autoUpdateInterval < 600) {
			autoUpdateInterval = 600;
			log.info(name + ": auto-update-interval cannot be less than 600");
		}
		
        File file = new File(propFile);
        return file.exists();
	}
	
	public boolean initData() {
		if (dataXml == null || dataXml.equals("")) {
			return false;
		}
		
		File f = new File(dataXml);
		if (!updateData() && !f.exists()) {
			return false;
		}

		if (parser.search("test") == null) {
			return false;
		}
		return true;
	}
	
	public boolean updateData() {
		if (autoUpdate) {
			try {
				URL url = new URL(updateSource);
				log.info(name + ": Updating data from " + updateSource+"...");
				InputStream is = url.openStream();
				FileOutputStream fos = null;
				fos = new FileOutputStream(dataXml);
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
	
	public void printSearchResults(Player player, TreeMap<Integer,String> results, String query) {
		if (results.size() > 0) {
			player.sendMessage(Colors.LightBlue+"Search results for \"" + query + "\":");
			Iterator<Integer> itr = results.keySet().iterator();
			String line = "";
			int num = 0;
			while (itr.hasNext()) {
				num++;
				int number = itr.next();
				String blockname = results.get(number);

				line += (rightPad(getBlockId(number), numWidth) + " " + delimiter + " " +rightPad(blockname, nameWidth));
				if (num % 2 == 0 || !itr.hasNext()) {
					player.sendMessage(Colors.Gold + line.trim());
					line = "";
				}
				if (num > 16) {
					player.sendMessage(Colors.Rose + "Not all results are displayed. Make your term more specific!");
					break;
				}
			}
		} else {
			player.sendMessage(Colors.Rose + "No results found");
		}
	}
	
	private String getBlockId(int block) {
		if (base.equalsIgnoreCase("hex") || base.equalsIgnoreCase("hexadecimal")) {
			return Integer.toHexString(block).toUpperCase();
		} else {
			return String.valueOf(block);
		}
	}
	
	public static String leftPad(String s, int width) {
        return String.format("%" + width + "s", s);
    }

    public static String rightPad(String s, int width) {
        return String.format("%-" + width + "s", s);
    }
}
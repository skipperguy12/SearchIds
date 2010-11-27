import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* @title  SearchIds
* @description Search for data id's in game or from the console
* @date 2010-11-26
* @author croemmich
*/
public class SearchIds extends Plugin  {
	private Listener l = new Listener(this);
	protected static final Logger log = Logger.getLogger("Minecraft");
	public  static String name    = "SearchIds";
	public  static String version = "1.0";

	private static String propFile = "search-ids.properties";
	private static PropertiesFile props;
	
	// Properties
	public  static String  searchType         = "all";
	public  static String  dataXml            = "search-ids-data.xml";
	public  static String  updateSource       = "https://cr-wd.com/minecraft/plugins/SearchIds/data.xml";
	public  static boolean autoUpdate         = true;
	public  static String  searchCommand      = "search";
	public  static String  consoleCommand     = "search";
	public  static String  base               = "decimal";
	public  static int     nameWidth          = 24;
	public  static int     numWidth           = 4;
	public  static String  delimiter          = "-";
	public  static int     autoUpdateInterval = 86400;
	
	public  static DataParser  parser;
	private UpdateThread updateThread;
	
	public void enable() {
		log.info(name + " " + version + " enabled");
		if (!initProps()) {
			log.severe(name + ": Could not initialise " + propFile);
			this.disable();
			return;
		}
		
		if (parser == null)
			parser = new DataParser();
		
		if (!initData()) {
			log.severe(name + ": Could not init the search data from: " + dataXml + ". Please check that the file exists and is not corrupt.");
			if (!autoUpdate) {
				log.severe(name + ": Set auto-update-data=true in " + propFile + " to automatically download the search data file " + dataXml);
			}			
			this.disable();
			return;
		}
		
		if (autoUpdate) {
			if (updateThread == null)
				updateThread = new UpdateThread(this);
			updateThread.start();
		}		
		etc.getInstance().addCommand("/"+searchCommand, " - Search for a block id");
	}
	
	public void disable() {
		etc.getInstance().removeCommand("/"+searchCommand);
		if (updateThread != null) {
			updateThread.stop();
			updateThread = null;
		}
		parser = null;
		log.info(name + " " + version + " disabled");
	}

	public void initialize() {
		etc.getLoader().addListener( PluginLoader.Hook.COMMAND, l, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener( PluginLoader.Hook.SERVERCOMMAND, l, this, PluginListener.Priority.MEDIUM);
	}
	
	public boolean initProps() {
		props = new PropertiesFile(propFile);
		
		// Properties
		searchType         = props.getString("search-type", "all");
		base               = props.getString("base", "decimal");
		searchCommand      = props.getString("command", "search");
		consoleCommand     = props.getString("console-command", "search");
		dataXml            = props.getString("data-xml", "search-ids-data.xml");
		updateSource       = props.getString("update-source", "https://cr-wd.com/minecraft/plugins/SearchIds/data.xml");
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
	
	private void printConsoleResults(TreeMap<Integer,String> results) {
		boolean selfLevel = true;
		boolean hasParent = false;
		Level oldLevel = log.getLevel();
		if (oldLevel == null) {
			selfLevel = false;
		}
		if (!selfLevel && log.getParent() != null) {
			oldLevel = log.getParent().getLevel();
			if (oldLevel != null) {
				hasParent = true;
			}
		}
		
		if (oldLevel != null)
			log.setLevel(Level.INFO);
		
		if (results.size() > 0) {
		    Iterator<Integer> itr = results.keySet().iterator();
			StringBuffer sb = new StringBuffer("\n");
			while (itr.hasNext()) {
				int number = itr.next();
				String blockname = results.get(number);
				sb.append(leftPad(getBlockId(number), numWidth) + " " + delimiter + " " +rightPad(blockname, nameWidth) + "\n");
			}
			log.info(sb.toString());
		} else {
			log.info("No results found");
		}
		
		if (hasParent && oldLevel != null) {
			log.getParent().setLevel(oldLevel);
		} else if (selfLevel && oldLevel != null) {
			log.setLevel(oldLevel);
		}
	}
	
	private void printSearchResults(Player player, TreeMap<Integer,String> results) {
		if (results.size() > 0) {
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
				if (num > 18) {
					player.sendMessage(Colors.Gold + "Not all results are displayed. Make your term more specific!");
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

	public class Listener extends PluginListener {
		SearchIds p;
		
		public Listener(SearchIds plugin) {
			p = plugin;
		}

		public boolean onCommand(Player player, String[] split) {
			if (split[0].equalsIgnoreCase("/"+searchCommand) && player.canUseCommand("/"+searchCommand)) {
				if (split.length > 1) {
					String query = "";
					for (int i = 1; i<split.length; i++) {
						query += (split[i] + " ");
					}
					query = query.trim();
					printSearchResults(player, parser.search(query, base));
				} else {
					player.sendMessage(Colors.Rose + "Correct usage is: " + "/"+searchCommand + " [item to search for]");
				}
				return true;
			}
			return false;
		}

		public boolean onConsoleCommand(String[] split) {
			if (split.length > 1 && split[0].equalsIgnoreCase(consoleCommand)) {
				String query = "";
				for (int i = 1; i<split.length; i++) {
					query += (split[i] + " ");
				}
				query = query.trim();
				printConsoleResults(parser.search(query, base));
				return true;
			}
			return false;
		}
	}
}
package org.bukkit.croemmich.searchids;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerListener;

public class SearchIdsPlayerListener extends PlayerListener {

	private SearchIds plugin;
	
    public SearchIdsPlayerListener(SearchIds instance) {
        plugin = instance;
    }
    
    @Override
    public void onPlayerCommand(PlayerChatEvent event) {
    	String[] split = event.getMessage().split(" ");
    	Player player = event.getPlayer();
    	
		if (split.length > 1 && split[0].equalsIgnoreCase(SearchIds.consoleCommand)) {
			String query = "";
			for (int i = 1; i<split.length; i++) {
				query += (split[i] + " ");
			}
			query = query.trim();
			plugin.printSearchResults(player, SearchIds.parser.search(query, SearchIds.base));
		} else {
			player.sendMessage(Colors.Rose + "Correct usage is: " + "/"+SearchIds.searchCommand + " [item to search for]");
		}
    }
}
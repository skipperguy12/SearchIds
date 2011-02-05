package org.bukkit.croemmich.searchids;

import org.bukkit.ChatColor;
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
		String command = split[0];
	
		if((!event.isCancelled()) && command.equalsIgnoreCase("/" + SearchIds.searchCommand)) {
			if (split.length > 1) {
				String query = "";
				for (int i = 1; i<split.length; i++) {
					query += (split[i] + " ");
				}
				query = query.trim();
				plugin.printSearchResults(player, SearchIds.parser.search(query, SearchIds.base), query);
			} else {
				player.sendMessage(ChatColor.RED + "Correct usage is: " + "/"+SearchIds.searchCommand + " [item to search for]");
			}
			
			event.setCancelled(true);
		}
    }
}
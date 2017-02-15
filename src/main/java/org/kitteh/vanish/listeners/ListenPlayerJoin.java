package org.kitteh.vanish.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.LazyMetadataValue;
import org.bukkit.metadata.LazyMetadataValue.CacheStrategy;
import org.kitteh.vanish.BungeeHelper;
import org.kitteh.vanish.VanishCheck;
import org.kitteh.vanish.VanishPerms;
import org.kitteh.vanish.VanishPlugin;
import org.kitteh.vanish.metrics.MetricsOverlord;

import au.com.addstar.bc.event.ProxyJoinEvent;

public final class ListenPlayerJoin implements Listener {
    private final VanishPlugin plugin;

    public ListenPlayerJoin(VanishPlugin instance) {
        this.plugin = instance;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoinEarly(ProxyJoinEvent event) {
        if (VanishPerms.joinVanished(event.getPlayer())) {
            MetricsOverlord.getJoinInvisTracker().increment();
            if(!this.plugin.getManager().isVanished(event.getPlayer())) // Changed: Added check for is vanished
            	this.plugin.getManager().toggleVanishQuiet(event.getPlayer(), false, true);
            BungeeHelper.setVanishState(event.getPlayer(), true);
            this.plugin.hooksVanish(event.getPlayer());
        }
        this.plugin.hooksJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoinLate(ProxyJoinEvent event) {
        final StringBuilder statusUpdate = new StringBuilder();
        if (VanishPerms.joinVanished(event.getPlayer())) {
            String message = ChatColor.DARK_AQUA + "You have joined vanished.";
            if (VanishPerms.canVanish(event.getPlayer())) {
                message += " To appear: /vanish";
            }
            event.getPlayer().sendMessage(message);
            statusUpdate.append("vanished");
        }
        if (VanishPerms.joinWithoutAnnounce(event.getPlayer())) {
            this.plugin.getManager().getAnnounceManipulator().addToDelayedAnnounce(event.getPlayer().getName());
            BungeeHelper.setOnlineState(event.getPlayer(), false);
            event.setJoinMessage(null);
            if (statusUpdate.length() != 0) {
                statusUpdate.append(" and ");
            }
            statusUpdate.append("silently");
        }
        if (statusUpdate.length() != 0) {
            this.plugin.messageStatusUpdate(ChatColor.DARK_AQUA + event.getPlayer().getName() + " has joined " + statusUpdate.toString());
        }
        if (VanishPerms.canReceiveAdminAlerts(event.getPlayer()) && this.plugin.versionDifference()) {
            event.getPlayer().sendMessage(ChatColor.AQUA + "[Vanish] This is version " + ChatColor.DARK_AQUA + this.plugin.getCurrentVersion() + ChatColor.AQUA + ", latest is " + ChatColor.DARK_AQUA + this.plugin.getLatestKnownVersion());
            event.getPlayer().sendMessage(ChatColor.AQUA + "Visit " + ChatColor.DARK_AQUA + "http://www.curse.com/server-mods/minecraft/vanish");
        }
    }
    
    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerJoinServer(final PlayerJoinEvent event)
    {
    	event.getPlayer().setMetadata("vanished", new LazyMetadataValue(this.plugin, CacheStrategy.NEVER_CACHE, new VanishCheck(this.plugin.getManager(), event.getPlayer().getName())));
        this.plugin.getManager().resetSeeing(event.getPlayer());
        
        // Force players to be vanished (if they can vanish) so they dont show up if they are meant to be vanished
    	if(VanishPerms.canVanish(event.getPlayer()))
            this.plugin.getManager().toggleVanishQuiet(event.getPlayer(), false, false);
    	
    	final boolean vanish = this.plugin.getManager().isVanished(event.getPlayer());
    	
    	// Update all states from stored data
    	Bukkit.getScheduler().runTaskLater(this.plugin, new Runnable()
		{
			public void run()
			{
				BungeeHelper.setTabGroup(event.getPlayer(), vanish);
				BungeeHelper.loadStateFromProxy(event.getPlayer());
				BungeeHelper.setSeeState(event.getPlayer(), VanishPerms.canSeeAll(event.getPlayer()));
			}
		}, 10);
    }
}
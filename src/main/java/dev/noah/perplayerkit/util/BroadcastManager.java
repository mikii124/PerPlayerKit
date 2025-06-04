/*
 * Copyright 2022-2025 Noah Ross
 *
 * This file is part of PerPlayerKit.
 *
 * PerPlayerKit is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * PerPlayerKit is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with PerPlayerKit. If not, see <https://www.gnu.org/licenses/>.
 */
package dev.noah.perplayerkit.util;

import dev.noah.perplayerkit.PerPlayerKit;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

// WorldGuard imports (soft dependency)
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class BroadcastManager {

    private static BroadcastManager instance;
    private final Plugin plugin;
    private final net.kyori.adventure.platform.bukkit.BukkitAudiences audience;
    private final CooldownManager kitroomBroadcastCooldown;

    public BroadcastManager(Plugin plugin) {
        this.plugin = plugin;
        this.audience = net.kyori.adventure.platform.bukkit.BukkitAudiences.create(plugin);
        this.kitroomBroadcastCooldown = new CooldownManager(10); // 10 seconds cooldown for kitroom broadcasts
        instance = this;
    }

    public static BroadcastManager get() {
        return instance;
    }

    /**
     * Checks if a broadcast should be sent for the given player, based on world and region restrictions.
     * - Disables broadcast if the player is in a world listed in `broadcast.disabled-worlds`
     * - Disables broadcast if the player is in a WorldGuard region listed in `broadcast.disabled-regions.<world>`
     *   (only if WorldGuard is present)
     * @param player the Player to check
     * @return true if a broadcast should be sent, false otherwise
     */
    public static boolean shouldBroadcast(Player player) {
        Plugin plugin = PerPlayerKit.getPlugin();
        String worldName = player.getWorld().getName();

        // Check disabled worlds
        List<String> disabledWorlds = plugin.getConfig().getStringList("broadcast.disabled-worlds");
        if (disabledWorlds.contains(worldName)) return false;

        // Check disabled regions if WorldGuard is present
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            List<String> disabledRegions = plugin.getConfig().getStringList("broadcast.disabled-regions." + worldName);
            if (!disabledRegions.isEmpty()) {
                RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));
                if (regionManager != null) {
                    ApplicableRegionSet regions = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(player.getLocation()));
                    for (ProtectedRegion region : regions) {
                        if (disabledRegions.contains(region.getId())) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    public void broadcastPlayerHealed(Player player) {
        if (!shouldBroadcast(player)) return;
        broadcastMessage(player, MessageKey.PLAYER_HEALED, null);
    }

    public void broadcastPlayerOpenedKitRoom(Player player) {
        if (!shouldBroadcast(player)) return;
        broadcastMessage(player, MessageKey.PLAYER_OPENED_KIT_ROOM, kitroomBroadcastCooldown);
    }

    public void broadcastPlayerLoadedPrivateKit(Player player) {
        if (!shouldBroadcast(player)) return;
        broadcastMessage(player, MessageKey.PLAYER_LOADED_PRIVATE_KIT, null);
    }

    public void broadcastPlayerLoadedPublicKit(Player player) {
        if (!shouldBroadcast(player)) return;
        broadcastMessage(player, MessageKey.PLAYER_LOADED_PUBLIC_KIT, null);
    }

    public void broadcastPlayerLoadedEnderChest(Player player) {
        if (!shouldBroadcast(player)) return;
        broadcastMessage(player, MessageKey.PLAYER_LOADED_ENDER_CHEST, null);
    }

    public void broadcastPlayerCopiedKit(Player player) {
        if (!shouldBroadcast(player)) return;
        broadcastMessage(player, MessageKey.PLAYER_COPIED_KIT, null);
    }

    public void broadcastPlayerCopiedEC(Player player) {
        if (!shouldBroadcast(player)) return;
        broadcastMessage(player, MessageKey.PLAYER_COPIED_EC, null);
    }

    public void broadcastPlayerRegeared(Player player) {
        if (!shouldBroadcast(player)) return;
        broadcastMessage(player, MessageKey.PLAYER_REGEARED, null);
    }

    // ---- Added missing method ----
    public void broadcastPlayerRepaired(Player player) {
        if (!shouldBroadcast(player)) return;
        broadcastMessage(player, MessageKey.PLAYER_REPAIRED, null);
    }
    // -----------------------------

    // Scheduled broadcast example, unchanged
    public void startScheduledBroadcast() {
        List<Component> messages = new ArrayList<>();
        plugin.getConfig().getStringList("scheduled-broadcast.messages").forEach(message -> messages.add(generateBroadcastComponent(message)));

        int[] index = {0};

        if (plugin.getConfig().getBoolean("scheduled-broadcast.enabled") && !messages.isEmpty()) {
            Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    audience.player(player).sendMessage(messages.get(index[0]));
                }
                index[0] = (index[0] + 1) % messages.size();
            }, 0, plugin.getConfig().getInt("scheduled-broadcast.period") * 20L);
        }
    }

    public void sendComponentMessage(Player player, Component message) {
        audience.player(player).sendMessage(message);
    }

    /**
     * Helper: Use your own way to get message and broadcast, this is just a placeholder
     */
    public void broadcastMessage(Player player, MessageKey key, CooldownManager cooldown) {
        // Implement your message sending logic here, example:
        String msg = ChatColor.GRAY + "[Kits] " + player.getName() + " has loaded a kit.";
        Bukkit.broadcastMessage(msg);
    }

    public Component generateBroadcastComponent(String message) {
        // Use MiniMessage or similar for formatting if needed
        return Component.text(ChatColor.translateAlternateColorCodes('&', message));
    }

    public enum MessageKey {
        PLAYER_REPAIRED("messages.player-repaired"),
        PLAYER_HEALED("messages.player-healed"),
        PLAYER_OPENED_KIT_ROOM("messages.player-opened-kit-room"),
        PLAYER_LOADED_PRIVATE_KIT("messages.player-loaded-private-kit"),
        PLAYER_LOADED_PUBLIC_KIT("messages.player-loaded-public-kit"),
        PLAYER_LOADED_ENDER_CHEST("messages.player-loaded-enderchest"),
        PLAYER_COPIED_KIT("messages.player-copied-kit"),
        PLAYER_COPIED_EC("messages.player-copied-ec"),
        PLAYER_REGEARED("messages.player-regeared");

        private final String key;

        MessageKey(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }
}

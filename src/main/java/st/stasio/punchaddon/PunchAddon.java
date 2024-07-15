package st.stasio.punchaddon;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PunchAddon extends JavaPlugin implements Listener, CommandExecutor {

    private String permission;
    private List<String> worlds;
    private int cooldownTime;
    private String punchMessage;
    private String cooldownMessage;
    private Particle particleType;
    private double punchHeight;
    private Sound punchSound;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void loadConfig() {
        permission = getConfig().getString("punch.permission");
        worlds = getConfig().getStringList("punch.worlds");
        cooldownTime = getConfig().getInt("punch.cooldown");
        punchMessage = ChatColor.translateAlternateColorCodes('&', getConfig().getString("punch.messages.punch"));
        cooldownMessage = ChatColor.translateAlternateColorCodes('&', getConfig().getString("punch.messages.cooldown"));
        particleType = Particle.valueOf(getConfig().getString("punch.particle.type", "VILLAGER_HAPPY").toUpperCase());
        punchHeight = getConfig().getDouble("punch.height", 3.0);
        punchSound = Sound.valueOf(getConfig().getString("punch.sound", "ENTITY_GENERIC_EXPLODE").toUpperCase());
    }

    private final Map<UUID, Integer> cooldowns = new HashMap<>();

    public void setCooldown(UUID player, int time) {
        if (time < 1) {
            cooldowns.remove(player);
        } else {
            cooldowns.put(player, time);
            new BukkitRunnable() {
                @Override
                public void run() {
                    int currentCooldown = cooldowns.getOrDefault(player, 0);
                    if (currentCooldown <= 1) {
                        cooldowns.remove(player);
                        this.cancel();
                    } else {
                        cooldowns.put(player, currentCooldown - 1);
                    }
                }
            }.runTaskTimer(this, 20, 20);
        }
    }

    public int getCooldown(UUID player) {
        return cooldowns.getOrDefault(player, 0);
    }

    @EventHandler
    public void onDamageInLobby(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }
        Player damager = (Player) event.getDamager();
        Player damaged = (Player) event.getEntity();
        World world = damaged.getWorld();
        int cooldownLeft = getCooldown(damager.getUniqueId());
        if (damager.hasPermission(permission) && worlds.contains(world.getName()) && cooldownLeft <= 0) {
            setCooldown(damager.getUniqueId(), cooldownTime);
            damaged.setVelocity(new Vector(0, punchHeight, 0));
            spawnCustomExplosion(world, damaged.getLocation());
            world.playSound(damaged.getLocation(), punchSound, 1.0F, 1.0F);
            String formattedPunchMessage = punchMessage.replace("{damaged}", damaged.getName()).replace("{damager}", damager.getName());
            Bukkit.broadcastMessage(formattedPunchMessage);
        } else {
            String formattedCooldownMessage = cooldownMessage.replace("{cooldown}", String.valueOf(cooldownLeft));
            damager.sendMessage(formattedCooldownMessage);
        }
    }

    private void spawnCustomExplosion(World world, Location location) {
        for (int i = 0; i < 5; i++) {
            double offsetX = (Math.random() - 0.5) * 2.0;
            double offsetY = (Math.random() - 0.5) * 2.0;
            double offsetZ = (Math.random() - 0.5) * 2.0;
            world.spawnParticle(particleType, location, 10, offsetX, offsetY, offsetZ, 0.1);
        }
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("punchreload")) {
            if (sender.hasPermission("punch.reload")) {
                reloadConfig();
                loadConfig();
                sender.sendMessage(ChatColor.GREEN + "punchAddon configuration reloaded.");
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }
        }
        return false;
    }
}

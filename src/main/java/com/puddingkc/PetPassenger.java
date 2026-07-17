package com.puddingkc;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.puddingkc.commands.ReloadCommand;
import com.puddingkc.commands.ToggleCommand;
import com.puddingkc.utils.PapiExpansion;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;

public class PetPassenger extends JavaPlugin implements Listener {

    private final Set<UUID> enabledPlayers = new HashSet<>();
    private final Map<UUID, Long> lastSneakTime = new HashMap<>();

    private List<String> allowedAnimals;
    private List<String> enabledWorlds;
    private boolean checkPermissions;
    private boolean defaultEnabled;
    private boolean blacklistMode;
    private boolean passengerFallDamage;
    private boolean throwEnabled;
    private double throwPower;
    private double throwUpPower;
    private long doubleSneakInterval;
    private Residence residence;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();

        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("petpassenger")).setExecutor(new ReloadCommand(this));
        Objects.requireNonNull(getCommand("petToggle")).setExecutor(new ToggleCommand(this));

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PapiExpansion(this).register();
            getLogger().info("发现 PlaceholderAPI 插件，已启用变量功能");
        }

        getLogger().info("插件启用成功，作者QQ: 3116078709");
    }

    public void loadConfig() {
        reloadConfig();
        allowedAnimals = getConfig().getStringList("allowed-animals");
        enabledWorlds = getConfig().getStringList("enabled-worlds");
        checkPermissions = getConfig().getBoolean("check-permissions");
        blacklistMode = getConfig().getString("list-mode", "whitelist").equalsIgnoreCase("blacklist");
        passengerFallDamage = getConfig().getBoolean("passenger-fall-damage", false);
        throwEnabled = getConfig().getBoolean("throw-enabled", false);
        throwPower = getConfig().getDouble("throw-power", 1.5);
        throwUpPower = getConfig().getDouble("throw-up-power", 0.4);
        doubleSneakInterval = getConfig().getLong("double-sneak-interval", 400);
        residence = (Residence) Bukkit.getPluginManager().getPlugin("Residence");
        defaultEnabled = getConfig().getBoolean("default-enabled");
    }

    private boolean isEnabledWorld(World world) {
        if (enabledWorlds.contains("all")) { return true; }
        return enabledWorlds.contains(world.getName());
    }

    private boolean isAllowedAnimals(Entity entity) {
        if (entity.getType() == EntityType.PLAYER) { return false; }

        if (blacklistMode) {
            return !allowedAnimals.contains(entity.getType().name());
        }

        if (allowedAnimals.contains(entity.getType().name())) { return true; }

        if (allowedAnimals.contains("animals")) {
            return entity instanceof Animals;
        }

        return false;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.isCancelled()) { return; }

        if (!enabledPlayers.contains(event.getPlayer().getUniqueId())) { return; }

        if (event.getHand() != EquipmentSlot.OFF_HAND && event.getPlayer().isSneaking()) {

            Player player = event.getPlayer();
            Entity entity = event.getRightClicked();
            boolean hasPermission = !checkPermissions || player.hasPermission("petpassenger.use");

            if (residence != null) {
                ClaimedResidence residence = Residence.getInstance().getResidenceManager().getByLoc(entity.getLocation());
                if (residence != null) {
                    if (!residence.isOwner(player) && !residence.isTrusted(player) && !player.hasPermission("petpassenger.bypass")) {
                        return;
                    }
                }
            }

            if (isAllowedAnimals(entity) && player.getPassengers().isEmpty() && hasPermission) {
                if (isEnabledWorld(player.getWorld())) {
                    player.addPassenger(entity);
                }
            }
        }
    }

    public String getMessages(String key) {
        return getConfig().getString("messages." + key,"&c[PetPassenger] 读取文本错误，请检查配置文件内容").replace("&", "§");
    }

    public void toggle(Player player) {
        if (enabledPlayers.contains(player.getUniqueId())) {
            enabledPlayers.remove(player.getUniqueId());
            player.sendMessage(getMessages("disabled"));
        } else {
            enabledPlayers.add(player.getUniqueId());
            player.sendMessage(getMessages("enabled"));
        }
    }

    public boolean getEnabled(Player player) {
        return enabledPlayers.contains(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (defaultEnabled && !enabledPlayers.contains(event.getPlayer().getUniqueId())) {
            toggle(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) { return; }

        Player player = event.getPlayer();
        if (player.getPassengers().isEmpty()) {
            lastSneakTime.remove(player.getUniqueId());
            return;
        }

        long now = System.currentTimeMillis();
        Long last = lastSneakTime.get(player.getUniqueId());
        if (last != null && now - last <= doubleSneakInterval) {
            player.getPassengers().forEach(player::removePassenger);
            lastSneakTime.remove(player.getUniqueId());
        } else {
            lastSneakTime.put(player.getUniqueId(), now);
        }
    }

    @EventHandler
    public void onPlayerAnimation(PlayerAnimationEvent event) {
        if (!throwEnabled) { return; }
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) { return; }

        Player player = event.getPlayer();
        if (!player.isSneaking()) { return; }
        if (player.getPassengers().isEmpty()) { return; }

        List<Entity> passengers = new ArrayList<>(player.getPassengers());
        Vector velocity = player.getLocation().getDirection().normalize().multiply(throwPower);
        velocity.setY(velocity.getY() + throwUpPower);

        for (Entity passenger : passengers) {
            player.removePassenger(passenger);
            passenger.setVelocity(velocity);
        }

        String message = getMessages("thrown");
        if (!message.isEmpty()) {
            player.sendMessage(message);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.getPlayer().getPassengers().forEach(event.getPlayer()::removePassenger);
        lastSneakTime.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (passengerFallDamage) { return; }
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) { return; }
        if (event.getEntity().getVehicle() instanceof Player) {
            event.setCancelled(true);
        }
    }
}
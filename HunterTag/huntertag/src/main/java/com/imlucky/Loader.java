package com.imlucky;

import java.util.*;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.command.*;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Loader extends JavaPlugin implements Listener {
  private static final Logger LOGGER = Logger.getLogger("huntertag");

  private GameManager gameManager;
  private GameScoreboard gameScoreboard;

  @Override
  public void onEnable() {
    gameManager = new GameManager();
    gameScoreboard = new GameScoreboard();
    getServer().getPluginManager().registerEvents(this, this);
    getCommand("sethunter").setExecutor(new SetHunterCommand());
    getCommand("setrunner").setExecutor(new SetRunnerCommand());
    getCommand("startgame").setExecutor(new StartGameCommand());
    getCommand("resetall").setExecutor(new ResetAllCommand());
    getCommand("resetpoints").setExecutor(new ResetPointsCommand());
    getCommand("challenge").setExecutor(new ChallengeCommand());
    getCommand("huntkit").setExecutor(new HuntkitCommand());
    getCommand("runkit").setExecutor(new RunkitCommand());

    gameManager.loadPoints(getConfig());
    LOGGER.info("huntertag enabled");

    new org.bukkit.scheduler.BukkitRunnable() {
      @Override
      public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
          if (gameManager.isFrozen(p)) {
            p.getWorld().spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(0, 2, 0),
                2, 0.5, 0.5, 0.5, 0.01);
          }
        }
      }
    }.runTaskTimer(this, 0L, 50L);

    new org.bukkit.scheduler.BukkitRunnable() {
      @Override
      public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
          for (ItemStack item : p.getInventory().getContents()) {
            if (item != null && item.hasItemMeta()) {
              String display = ChatColor.stripColor(item.getItemMeta().getDisplayName());
              if ((item.getType() == Material.RED_TERRACOTTA && display.equals("Infinite Red Terracotta"))
                  || (item.getType() == Material.BLUE_TERRACOTTA && display.equals("Infinite Blue Terracotta"))) {
                item.setAmount(64);
              }
            }
          }
        }
      }
    }.runTaskTimer(this, 0L, 100L);
  }

  @Override
  public void onDisable() {
    gameManager.savePoints(getConfig());
    saveConfig();
    LOGGER.info("huntertag disabled");
  }

  @EventHandler
  public void onPlayerHit(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player))
      return;
    Player damager = (Player) event.getDamager();
    Player target = (Player) event.getEntity();

    if (gameManager.isHunter(damager) && gameManager.isRunner(target) && !gameManager.isFrozen(target)) {
      gameManager.freezeRunner(target);
      gameManager.addPoints(damager.getName(), 5);
      gameScoreboard.update();
      damager.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
          new net.md_5.bungee.api.chat.TextComponent(ChatColor.YELLOW + "You have frozen "
              + ChatColor.AQUA + target.getName() + ChatColor.GREEN + " " + ChatColor.GRAY + "[" + ChatColor.GREEN
              + "+5 points" + ChatColor.GRAY + "]"));
      Bukkit.broadcastMessage(ChatColor.AQUA + "❄ " + ChatColor.AQUA + target.getName()
          + ChatColor.YELLOW + " was frozen by " + ChatColor.GOLD + "🗡 "
          + damager.getName() + ChatColor.GREEN + " " + ChatColor.GRAY + "[" + ChatColor.GREEN + "+5 points"
          + ChatColor.GRAY + "]");
      target.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
          new net.md_5.bungee.api.chat.TextComponent(ChatColor.RED + "You have been frozen by "
              + ChatColor.GOLD + damager.getName()));
      for (Player p : Bukkit.getOnlinePlayers()) {
        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_LAND, 1f, 1f);
      }
      if (gameManager.allRunnersFrozen()) {
        gameManager.addPoints(damager.getName(), 10);
        gameScoreboard.update();
        Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "🏆 " + ChatColor.YELLOW + damager.getName()
            + ChatColor.GOLD + " has frozen all runners and won the game! " + ChatColor.GRAY + "[" + ChatColor.GREEN
            + "+10 points" + ChatColor.GRAY + "]");

        for (Player p : Bukkit.getOnlinePlayers()) {
          p.sendTitle(ChatColor.YELLOW + "" + ChatColor.BOLD + "GAME OVER!", ChatColor.GOLD + "The hunter has won!", 10,
              70, 20);
          p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
        gameManager.finishGame();
        gameScoreboard.update();
      }
      event.setCancelled(true);
      return;
    }

    else if (gameManager.isHunter(damager) && gameManager.isRunner(target) && gameManager.isFrozen(target)) {
      event.setCancelled(true);
      return;
    }

    // Prevent runners from damaging each other normally. DISABLED, uncomment if
    // needed.
    // else if (gameManager.isRunner(damager) && gameManager.isRunner(target) &&
    // !gameManager.isFrozen(target)) {
    // // Here runners cannot hit each other; only unfreeze events are allowed.
    // event.setCancelled(true);
    // return;
    // }

    else if (gameManager.isRunner(damager) && !gameManager.isFrozen(damager)
        && gameManager.isRunner(target) && gameManager.isFrozen(target)) {
      gameManager.unfreezeRunner(target);
      gameManager.addPoints(damager.getName(), 5);
      gameScoreboard.update();
      damager.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
          new net.md_5.bungee.api.chat.TextComponent(ChatColor.YELLOW + "You have unfrozen "
              + ChatColor.AQUA + target.getName() + ChatColor.GREEN + " " + ChatColor.GRAY + "[" + ChatColor.GREEN
              + "+5 points" + ChatColor.GRAY + "]"));
      Bukkit.broadcastMessage(ChatColor.GOLD + "🗡 " + damager.getName()
          + ChatColor.YELLOW + " unfroze " + ChatColor.AQUA + target.getName() + ChatColor.GREEN + " "
          + ChatColor.GRAY + "[" + ChatColor.GREEN + "+5 points" + ChatColor.GRAY + "]");
      target.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
          new net.md_5.bungee.api.chat.TextComponent(ChatColor.GREEN + "You have been unfrozen by "
              + ChatColor.AQUA + damager.getName()));
      for (Player p : Bukkit.getOnlinePlayers()) {
        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1f);
      }
      event.setCancelled(true);
      return;
    }
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    Player p = event.getPlayer();
    if (gameManager.isFrozen(p)) {
      long lastFreeze = gameManager.getLastFreezeTime(p);
      if (System.currentTimeMillis() - lastFreeze > 3000) {
        p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
            new net.md_5.bungee.api.chat.TextComponent(ChatColor.RED + "You are frozen!"));
      }
      event.setCancelled(true);
    }
  }

  class SetHunterCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length != 1)
        return false;
      Player target = getServer().getPlayer(args[0]);
      if (target != null) {
        if (gameManager.hasRole(target)) {
          sender.sendMessage(target.getName() + " already has a role. Unset them first.");
          return true;
        }
        gameManager.setHunter(target);
        sender.sendMessage(target.getName() + " set as hunter.");
      }
      return true;
    }
  }

  class SetRunnerCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length != 1)
        return false;
      Player target = getServer().getPlayer(args[0]);
      if (target != null) {
        if (gameManager.hasRole(target)) {
          sender.sendMessage(target.getName() + " already has a role. Unset them first.");
          return true;
        }
        gameManager.setRunner(target);
        sender.sendMessage(target.getName() + " set as runner.");
      }
      return true;
    }
  }

  class StartGameCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      gameManager.resetGame();
      gameScoreboard.update();
      for (Player p : Bukkit.getOnlinePlayers()) {
        p.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "GAME STARTED!", "", 10, 70, 20);
      }
      for (Player p : Bukkit.getOnlinePlayers()) {
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
      }
      return true;
    }
  }

  class ResetAllCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      gameManager.resetAllRoles();
      gameScoreboard.update();
      sender.sendMessage("All player roles have been reset.");
      return true;
    }
  }

  class ResetPointsCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length == 0) {
        sender.sendMessage("Usage: /resetpoints <all|player1 [player2 ...]>");
        return true;
      }
      if (args[0].equalsIgnoreCase("all")) {
        gameManager.resetAllPoints();
        sender.sendMessage("All player points have been reset.");
      } else {
        for (String playerName : args) {
          gameManager.resetPoints(playerName);
          sender.sendMessage("Points reset for " + playerName);
        }
      }
      gameScoreboard.update();
      return true;
    }
  }

  class ChallengeCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player)) {
        sender.sendMessage(ChatColor.RED + "Only players can use this command.");
        return true;
      }
      Player player = (Player) sender;
      new org.bukkit.scheduler.BukkitRunnable() {
        int timeLeft = 30;

        @Override
        public void run() {
          if (gameManager.isGameFinished()) {
            cancel();
            return;
          }
          if (gameManager.allRunnersFrozen()) {
            Bukkit.broadcastMessage(ChatColor.GREEN + "Challenge complete! Hunter wins the challenge!");
            cancel();
            return;
          }
          if (timeLeft == 0) {
            if (!gameManager.allRunnersFrozen()) {
              gameManager.penalizeHunters();
              gameScoreboard.update();
              Bukkit.broadcastMessage(ChatColor.RED + "Time's up! Hunter lost the challenge and is penalized "
                  + ChatColor.DARK_RED + " " + ChatColor.GRAY + "[" + ChatColor.RED + "-5 points" + ChatColor.GRAY
                  + "]");
              for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "GAME OVER!",
                    ChatColor.YELLOW + "The runners have won!", 10, 70, 20);
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
              }
              gameManager.finishGame();
            }
            cancel();
            return;
          }
          if (timeLeft == 30 || timeLeft == 15 || timeLeft == 10 ||
              timeLeft == 5 || timeLeft == 4 || timeLeft == 3 ||
              timeLeft == 2 || timeLeft == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
              p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                  new net.md_5.bungee.api.chat.TextComponent(
                      ChatColor.YELLOW + String.valueOf(timeLeft) + " seconds left!"));
            }
          }
          timeLeft--;
        }
      }.runTaskTimer(Loader.this, 0L, 20L);
      return true;
    }
  }

  class HuntkitCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player)) {
        sender.sendMessage(ChatColor.RED + "Only players can use this command.");
        return true;
      }
      Player player = (Player) sender;
      ItemStack kit = new ItemStack(Material.RED_TERRACOTTA, 64);
      ItemMeta meta = kit.getItemMeta();
      meta.setDisplayName(ChatColor.RED + "Infinite Red Terracotta");
      kit.setItemMeta(meta);
      player.getInventory().setItemInOffHand(kit);
      ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
      ItemMeta swordMeta = sword.getItemMeta();
      swordMeta.setUnbreakable(true);
      sword.setItemMeta(swordMeta);
      sword.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.KNOCKBACK, 1);
      player.getInventory().addItem(sword);
      ItemStack pickaxe = new ItemStack(Material.NETHERITE_PICKAXE);
      ItemMeta pickaxeMeta = pickaxe.getItemMeta();
      pickaxeMeta.setUnbreakable(true);
      pickaxe.setItemMeta(pickaxeMeta);
      player.getInventory().addItem(pickaxe);
      player.sendMessage(ChatColor.GREEN + "Infinite Red Terracotta kit applied to your offhand.");
      return true;
    }
  }

  class RunkitCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player)) {
        sender.sendMessage(ChatColor.RED + "Only players can use this command.");
        return true;
      }
      Player player = (Player) sender;
      ItemStack kit = new ItemStack(Material.BLUE_TERRACOTTA, 64);
      ItemMeta meta = kit.getItemMeta();
      meta.setDisplayName(ChatColor.BLUE + "Infinite Blue Terracotta");
      kit.setItemMeta(meta);
      player.getInventory().setItemInOffHand(kit);
      ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
      ItemMeta swordMeta = sword.getItemMeta();
      swordMeta.setUnbreakable(true);
      sword.setItemMeta(swordMeta);
      sword.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.KNOCKBACK, 1);
      player.getInventory().addItem(sword);
      ItemStack pickaxe = new ItemStack(Material.NETHERITE_PICKAXE);
      ItemMeta pickaxeMeta = pickaxe.getItemMeta();
      pickaxeMeta.setUnbreakable(true);
      pickaxe.setItemMeta(pickaxeMeta);
      player.getInventory().addItem(pickaxe);
      player.sendMessage(ChatColor.GREEN + "Infinite Blue Terracotta kit applied to your offhand.");
      return true;
    }
  }

  class GameManager {
    private Set<UUID> hunters = new HashSet<>();
    private Set<UUID> runners = new HashSet<>();
    private Set<UUID> frozenRunners = new HashSet<>();
    private Map<String, Integer> points = new HashMap<>();
    private static final long FREEZE_COOLDOWN = 1000L;
    private Map<UUID, Long> lastFreezeTimes = new HashMap<>();
    private boolean gameFinished = false;

    void setHunter(Player p) {
      hunters.add(p.getUniqueId());
      runners.remove(p.getUniqueId());
    }

    void setRunner(Player p) {
      runners.add(p.getUniqueId());
      hunters.remove(p.getUniqueId());
    }

    boolean hasRole(Player p) {
      return hunters.contains(p.getUniqueId()) || runners.contains(p.getUniqueId());
    }

    void resetAllRoles() {
      hunters.clear();
      runners.clear();
      frozenRunners.clear();
    }

    boolean isHunter(Player p) {
      return hunters.contains(p.getUniqueId());
    }

    boolean isRunner(Player p) {
      return runners.contains(p.getUniqueId());
    }

    void freezeRunner(Player p) {
      UUID uid = p.getUniqueId();
      long now = System.currentTimeMillis();
      if (lastFreezeTimes.containsKey(uid)) {
        long lastFreeze = lastFreezeTimes.get(uid);
        if (now - lastFreeze < FREEZE_COOLDOWN) {
          return;
        }
      }
      lastFreezeTimes.put(uid, now);
      frozenRunners.add(uid);
    }

    void unfreezeRunner(Player p) {
      frozenRunners.remove(p.getUniqueId());
    }

    boolean isFrozen(Player p) {
      return frozenRunners.contains(p.getUniqueId());
    }

    boolean allRunnersFrozen() {
      return !runners.isEmpty() && frozenRunners.containsAll(runners);
    }

    void addPoints(String playerName, int pts) {
      points.put(playerName, points.getOrDefault(playerName, 0) + pts);
    }

    Map<String, Integer> getPoints() {
      return points;
    }

    void resetGame() {
      frozenRunners.clear();
      gameFinished = false;
    }

    void finishGame() {
      hunters.clear();
      runners.clear();
      frozenRunners.clear();
      gameFinished = true;
    }

    boolean isGameFinished() {
      return gameFinished;
    }

    void loadPoints(Configuration config) {
      if (config.contains("points")) {
        for (String key : config.getConfigurationSection("points").getKeys(false))
          points.put(key, config.getInt("points." + key));
      }
    }

    void savePoints(Configuration config) {
      for (Map.Entry<String, Integer> entry : points.entrySet())
        config.set("points." + entry.getKey(), entry.getValue());
    }

    void resetAllPoints() {
      points.clear();
    }

    void resetPoints(String playerName) {
      points.put(playerName, 0);
    }

    void penalizeHunters() {
      for (UUID uid : hunters) {
        Player hunter = Bukkit.getPlayer(uid);
        if (hunter != null) {
          addPoints(hunter.getName(), -5);
        }
      }
    }

    public long getLastFreezeTime(Player p) {
      return lastFreezeTimes.getOrDefault(p.getUniqueId(), 0L);
    }
  }

  class GameScoreboard {
    private Scoreboard scoreboard;
    private Objective objective;

    GameScoreboard() {
      scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
      objective = scoreboard.registerNewObjective("points", "dummy", ChatColor.BOLD + "POINTS");
      objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    void update() {
      for (Player p : Bukkit.getOnlinePlayers()) {
        if (!gameManager.getPoints().containsKey(p.getName())) {
          gameManager.getPoints().put(p.getName(), 0);
        }
      }
      for (String entry : scoreboard.getEntries())
        scoreboard.resetScores(entry);
      gameManager.getPoints().entrySet().stream()
          .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
          .forEach(e -> {
            String playerName = e.getKey();
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
              if (gameManager.isHunter(player)) {
                playerName = ChatColor.RED + "" + ChatColor.BOLD + playerName;
              } else if (gameManager.isRunner(player)) {
                playerName = ChatColor.WHITE + playerName;
              }
            }
            Score score = objective.getScore(playerName);
            score.setScore(e.getValue());
          });
      for (Player player : Bukkit.getOnlinePlayers()) {
        player.setScoreboard(scoreboard);
      }
    }
  }
}

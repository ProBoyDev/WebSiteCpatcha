package me.salman.websiteCaptcha.Command;

import me.salman.websiteCaptcha.Main;
import me.salman.websiteCaptcha.Manager.VerificationManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class WebCaptchaCommand implements CommandExecutor, TabCompleter {

    private final VerificationManager verificationManager;
    private final Set<UUID> whitelist = new HashSet<>();

    public WebCaptchaCommand(VerificationManager verificationManager) {
        this.verificationManager = verificationManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("webcaptcha")) {
            if (args.length == 0) {
                sender.sendMessage("§eUsage: /webcaptcha <help|verify|reload|portcheck>");
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("webcaptcha.reload") || sender.isOp()) {
                    Main.getInstance().reloadConfigFile();
                    String successMessage = Main.getInstance().getConfig().getString("messages.reload_success");
                    sender.sendMessage(successMessage);
                } else {
                    sender.sendMessage(Main.getInstance().getConfig().getString("messages.no_permission"));
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("help")) {
                if (sender.hasPermission("webcaptcha.help") || sender.isOp()) {
                    sender.sendMessage("");
                    sender.sendMessage(" §8==========================");
                    sender.sendMessage(" §6WebsiteCaptcha Help Page  ");
                    sender.sendMessage(" §8==========================");
                    sender.sendMessage("");
                    sender.sendMessage(" §a/webcaptcha help §7- Show this help page");
                    sender.sendMessage(" §a/webcaptcha reload §7- Reload the plugin configuration");
                    sender.sendMessage(" §a/webcaptcha portcheck §7- Check if a port is available for WebSite verification");
                    sender.sendMessage(" §a/webcaptcha verify clearverify §7- Clear verification for a player");
                    sender.sendMessage(" §a/webcaptcha verify setverify §7- Set verification done for a player");
                    sender.sendMessage(" §a/webcaptcha verify whitelist add §7- Add a player to the whitelist");
                    sender.sendMessage(" §a/webcaptcha verify whitelist remove §7- Remove a player from the whitelist");
                    sender.sendMessage(" §a/webcaptcha verify whitelist list §7- List all players in the whitelist");
                    sender.sendMessage("");
                    sender.sendMessage(" §8==========================");
                    sender.sendMessage(" §7For more information, visit ");
                    sender.sendMessage(" §6the documentation coming soon!");
                    sender.sendMessage(" §8==========================");
                } else {
                    sender.sendMessage(Main.getInstance().getConfig().getString("messages.no_permission"));
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("verify")) {
                if (args.length < 2) {
                    sender.sendMessage("§eUsage: /webcaptcha verify <clearverify|setverify|whitelist>");
                    return true;
                }

                String action = args[1];
                // Handle whitelist subcommands
                if (action.equalsIgnoreCase("whitelist")) {
                    if (!sender.hasPermission("webcaptcha.whitelist") && !sender.isOp()) {
                        sender.sendMessage(Main.getInstance().getConfig().getString("messages.no_permission"));
                        return true;
                    }

                    if (args.length < 3) {
                        sender.sendMessage("§eUsage: /webcaptcha verify whitelist <add|remove|list>");
                        return true;
                    }

                    String subAction = args[2];

                    if (subAction.equalsIgnoreCase("add")) {
                        if (args.length < 4) {
                            sender.sendMessage("§eUsage: /webcaptcha verify whitelist add <playername>");
                            return true;
                        }

                        String playerName = args[3];
                        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerName);
                        if (targetPlayer == null || !targetPlayer.hasPlayedBefore()) {
                            sender.sendMessage("§cPlayer not found or has never joined the server.");
                            return true;
                        }

                        verificationManager.addPlayerToWhitelist(targetPlayer.getUniqueId());
                        sender.sendMessage("§aAdded " + playerName + " to the whitelist.");
                        return true;
                    }

                    if (subAction.equalsIgnoreCase("remove")) {
                        if (args.length < 4) {
                            sender.sendMessage("§eUsage: /webcaptcha verify whitelist remove <playername>");
                            return true;
                        }

                        String playerName = args[3];
                        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerName);
                        if (targetPlayer == null || !targetPlayer.hasPlayedBefore()) {
                            sender.sendMessage("§cPlayer not found or has never joined the server.");
                            return true;
                        }

                        if (verificationManager.isPlayerWhitelisted(targetPlayer.getUniqueId())) {
                            verificationManager.removePlayerFromWhitelist(targetPlayer.getUniqueId());
                            sender.sendMessage("§aRemoved " + playerName + " from the whitelist.");
                        } else {
                            sender.sendMessage("§c" + playerName + " is not on the whitelist.");
                        }
                        return true;
                    }

                    if (subAction.equalsIgnoreCase("list")) {
                        Set<UUID> whitelist = verificationManager.getWhitelist();

                        if (whitelist.isEmpty()) {
                            sender.sendMessage("§eThe whitelist is currently empty.");
                        } else {
                            sender.sendMessage("§eWhitelist:");
                            for (UUID uuid : whitelist) {
                                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                                sender.sendMessage("§a- " + (player.getName() != null ? player.getName() : "Unknown"));
                            }
                        }
                        return true;
                    }

                    sender.sendMessage("§eUnknown whitelist subcommand. Use /webcaptcha verify whitelist <add|remove|list>");
                    return true;
                }

                if (action.equalsIgnoreCase("clearverify")) {
                    if (args.length < 3) {
                        sender.sendMessage("§eUsage: /webcaptcha verify clearverify <playername>");
                        return true;
                    }

                    String playerName = args[2];
                    OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerName);
                    if (targetPlayer == null || !targetPlayer.hasPlayedBefore()) {
                        sender.sendMessage("§cPlayer not found or has never joined the server.");
                        return true;
                    }

                    UUID playerUUID = targetPlayer.getUniqueId();

                    if (sender.hasPermission("webcaptcha.clearverify") || sender.isOp()) {
                        verificationManager.clearPlayerVerification(playerUUID);
                        sender.sendMessage(Main.getInstance().getConfig()
                                .getString("messages.clear_success").replace("%player%", targetPlayer.getName()));
                    } else {
                        sender.sendMessage(Main.getInstance().getConfig().getString("messages.no_permission"));
                    }
                    return true;
                }

                if (action.equalsIgnoreCase("setverify")) {
                    if (args.length < 3) {
                        sender.sendMessage("§eUsage: /webcaptcha verify setverify <playername>");
                        return true;
                    }

                    String playerName = args[2];
                    OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerName);
                    if (targetPlayer == null || !targetPlayer.hasPlayedBefore()) {
                        sender.sendMessage("§cPlayer not found or has never joined the server.");
                        return true;
                    }

                    UUID playerUUID = targetPlayer.getUniqueId();

                    if (sender.hasPermission("webcaptcha.setverify") || sender.isOp()) {
                        verificationManager.setPlayerVerified(playerUUID, true);
                        verificationManager.setPlayerRecentlyVerified(playerUUID, true);
                        sender.sendMessage(Main.getInstance().getConfig()
                                .getString("messages.set_success").replace("%player%", targetPlayer.getName()));
                    } else {
                        sender.sendMessage(Main.getInstance().getConfig().getString("messages.no_permission"));
                    }
                    return true;
                }
            }

            if (args[0].equalsIgnoreCase("portcheck")) {
                if (args.length < 3) {
                    sender.sendMessage("§eUsage: /webcaptcha portcheck <IP> <port>");
                    return true;
                }

                String ip = args[1];
                int port;
                try {
                    port = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid port number. Please enter a valid number.");
                    return true;
                }

                sender.sendMessage("§eChecking port...");
                Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
                    boolean isOpen = isPortOpen(ip, port);
                    if (isOpen) {
                        sender.sendMessage("§aThe port " + port + " on " + ip + " is open!");
                    } else {
                        sender.sendMessage("§cThe port " + port + " on " + ip + " is closed.");
                    }
                });
                return true;
            }

            sender.sendMessage("§eUnknown subcommand. Use /webcaptcha <verify|reload|portcheck>");
            return true;
        }
        return false;
    }

    private boolean isPortOpen(String ip, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 2000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if("help".startsWith(args[0].toLowerCase())) completions.add("help");
            if ("verify".startsWith(args[0].toLowerCase())) completions.add("verify");
            if ("reload".startsWith(args[0].toLowerCase())) completions.add("reload");
            if ("portcheck".startsWith(args[0].toLowerCase())) completions.add("portcheck");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("verify")) {
            if ("clearverify".startsWith(args[1].toLowerCase())) completions.add("clearverify");
            if ("setverify".startsWith(args[1].toLowerCase())) completions.add("setverify");
            if ("whitelist".startsWith(args[1].toLowerCase())) completions.add("whitelist");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("verify") && args[1].equalsIgnoreCase("whitelist")) {
            if ("add".startsWith(args[2].toLowerCase())) completions.add("add");
            if ("remove".startsWith(args[2].toLowerCase())) completions.add("remove");
            if ("list".startsWith(args[2].toLowerCase())) completions.add("list");
        } else if (args.length == 4 && args[0].equalsIgnoreCase("verify") && args[1].equalsIgnoreCase("whitelist")) {
            if (args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("remove")) {
                for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                    if (player.getName() != null && player.getName().toLowerCase().startsWith(args[3].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            }
        }

        return completions;
    }
}

package eu.bierandfriends.bafsync;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class SyncCommand implements CommandExecutor {

    private final BAFSync plugin;

    public SyncCommand(BAFSync plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Nur Spieler können diesen Befehl nutzen.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("§6Nutzung: §f/biersync <Token>");
            sender.sendMessage("§7Den Token bekommst du in der BierAndFriends App unter Profil.");
            return true;
        }

        Player player = (Player) sender;
        String token = args[0];
        String uuid = player.getUniqueId().toString();
        String name = player.getName();

        player.sendMessage("§6⏳ Verknüpfung wird durchgeführt...");

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String apiUrl = plugin.getApiUrl() + "/biersync";
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                // Feldnamen passend zur Firebase Function: token, uuid, name
                String json = String.format(
                    "{\"token\":\"%s\",\"uuid\":\"%s\",\"name\":\"%s\"}",
                    token, uuid, name
                );

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                Scanner scanner = new Scanner(
                    responseCode == 200
                        ? conn.getInputStream()
                        : conn.getErrorStream()
                );

                StringBuilder response = new StringBuilder();
                while (scanner.hasNext()) response.append(scanner.nextLine());
                scanner.close();

                JsonObject result = JsonParser.parseString(response.toString()).getAsJsonObject();
                boolean success = result.get("success").getAsBoolean();

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        String rank = result.has("rank")
                            ? result.get("rank").getAsString() : "malzbier";
                        player.sendMessage("§a✅ Minecraft-Account erfolgreich verknüpft!");
                        player.sendMessage("§6Dein Rang: §f" + formatRank(rank));
                    } else {
                        String msg = result.has("message")
                            ? result.get("message").getAsString() : "Unbekannter Fehler";
                        player.sendMessage("§c❌ Verknüpfung fehlgeschlagen: " + msg);
                        player.sendMessage("§7Generiere in der App einen neuen Token.");
                    }
                });

            } catch (Exception e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§c❌ Server nicht erreichbar. Bitte später nochmal versuchen.");
                    plugin.getLogger().severe("BAFSync Fehler: " + e.getMessage());
                });
            }
        });

        return true;
    }

    private String formatRank(String rank) {
        switch (rank.toLowerCase()) {
            case "malzbier":      return "🍺 Malzbier";
            case "feierabendbier": return "🍻 Feierabendbier";
            case "vollwieneimer": return "🪣 Vollwieneimer";
            case "absturzlegende": return "💀 Absturzlegende";
            case "moderator":     return "🛡️ Moderator";
            case "supporter":     return "💙 Supporter";
            case "trainee":       return "📋 Trainee";
            case "admin":         return "⚡ Admin";
            case "cheffe":        return "👑 Cheffe";
            case "builder":       return "🔨 Builder";
            default:              return rank;
        }
    }
}

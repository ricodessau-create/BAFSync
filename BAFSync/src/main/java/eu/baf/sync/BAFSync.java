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
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Nur Spieler können diesen Befehl nutzen.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("§6Nutzung: §f/biersync <Token>");
            sender.sendMessage("§7Den Token findest du in der BierAndFriends App unter Profil.");
            return true;
        }

        Player player = (Player) sender;
        String token = args[0];
        String uuid  = player.getUniqueId().toString();
        String name  = player.getName();

        // Bedrock-Spieler erkennen (Floodgate UUID beginnt mit Nullen)
        boolean isBedrock = uuid.startsWith("00000000-0000-0000");
        if (isBedrock) {
            // Floodgate Prefix vom Namen entfernen falls vorhanden (z.B. "." prefix)
            if (name.startsWith(".")) {
                name = name.substring(1);
            }
        }

        final String finalName = name;
        final String finalUuid = uuid;
        final boolean finalIsBedrock = isBedrock;

        player.sendMessage("§6⏳ Verknüpfung wird durchgeführt...");
        if (finalIsBedrock) {
            player.sendMessage("§7(Bedrock Spieler erkannt)");
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(plugin.getApiUrl());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                String json = String.format(
                    "{\"token\":\"%s\",\"uuid\":\"%s\",\"name\":\"%s\",\"bedrock\":%s}",
                    token, finalUuid, finalName, finalIsBedrock
                );

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                Scanner sc = new Scanner(
                    code == 200 ? conn.getInputStream() : conn.getErrorStream()
                );
                StringBuilder resp = new StringBuilder();
                while (sc.hasNext()) resp.append(sc.nextLine());
                sc.close();

                JsonObject result = JsonParser.parseString(resp.toString()).getAsJsonObject();
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
                        player.sendMessage("§c❌ Fehlgeschlagen: " + msg);
                        if (msg.contains("abgelaufen")) {
                            player.sendMessage("§7Öffne die App und generiere einen neuen Token.");
                        }
                    }
                });

            } catch (Exception e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§c❌ Server nicht erreichbar. Bitte später versuchen.");
                    plugin.getLogger().severe("BAFSync Fehler: " + e.getMessage());
                });
            }
        });

        return true;
    }

    private String formatRank(String rank) {
        switch (rank.toLowerCase()) {
            case "malzbier":       return "🍺 Malzbier";
            case "feierabendbier": return "🍻 Feierabendbier";
            case "vollwieneimer":  return "🪣 Vollwieneimer";
            case "absturzlegende": return "💀 Absturzlegende";
            case "moderator":      return "🛡️ Moderator";
            case "supporter":      return "💙 Supporter";
            case "trainee":        return "📋 Trainee";
            case "admin":          return "⚡ Admin";
            case "cheffe":         return "👑 Cheffe";
            case "builder":        return "🔨 Builder";
            default:               return rank;
        }
    }
}

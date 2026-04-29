package eu.baf.sync;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class SyncCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl nutzen.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage("§6§l[BierAndFriends]");
            player.sendMessage("§eBenutzung: §f/biersync <token>");
            player.sendMessage("§7Den Token findest du in der §eBierAndFriends App§7 → Profil → Minecraft Account verknüpfen");
            return true;
        }

        String token = args[0].trim();

        if (!token.matches("\\d{6}")) {
            player.sendMessage("§6§l[BierAndFriends] §cUngültiger Token!");
            player.sendMessage("§7Der Token muss genau §e6 Ziffern§7 lang sein.");
            return true;
        }

        String uuid = player.getUniqueId().toString();
        String name = player.getName();

        LuckPerms lp;
        try {
            lp = LuckPermsProvider.get();
        } catch (Exception e) {
            player.sendMessage("§6§l[BierAndFriends] §cFehler: LuckPerms nicht gefunden.");
            return true;
        }

        User user = lp.getUserManager().getUser(player.getUniqueId());

        if (user == null) {
            player.sendMessage("§6§l[BierAndFriends] §cFehler: Konnte deinen Rang nicht abrufen.");
            player.sendMessage("§7Bitte versuche es erneut oder kontaktiere einen Admin.");
            return true;
        }

        String group = user.getPrimaryGroup();
        String rank = mapRank(group);

        player.sendMessage("§6§l[BierAndFriends] §eSynchronisiere deinen Account...");

        final String apiUrl = BAFSync.getInstance().getApiUrl();

        Bukkit.getScheduler().runTaskAsynchronously(BAFSync.getInstance(), () -> {
            try {
                URL url = new URL(apiUrl);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                con.setConnectTimeout(10000);
                con.setReadTimeout(10000);

                String json = String.format(
                    "{\"token\":\"%s\",\"uuid\":\"%s\",\"minecraftName\":\"%s\",\"rank\":\"%s\"}",
                    token, uuid, name, rank
                );

                try (OutputStream os = con.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = con.getResponseCode();

                BufferedReader reader;
                if (responseCode >= 200 && responseCode < 300) {
                    reader = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
                } else {
                    reader = new BufferedReader(new InputStreamReader(con.getErrorStream(), StandardCharsets.UTF_8));
                }

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                final int code = responseCode;

                Bukkit.getScheduler().runTask(BAFSync.getInstance(), () -> {
                    if (code == 200) {
                        player.sendMessage("§6§l[BierAndFriends]");
                        player.sendMessage("§a✔ Account erfolgreich verknüpft!");
                        player.sendMessage("§7Spieler: §e" + name);
                        player.sendMessage("§7Rang: §e" + getRankDisplayName(rank));
                        player.sendMessage("§7Du kannst nun alle Features der BierAndFriends App nutzen!");
                    } else if (code == 404) {
                        player.sendMessage("§6§l[BierAndFriends] §c✗ Ungültiger Token!");
                        player.sendMessage("§7Erstelle einen neuen Token in der §eBierAndFriends App§7.");
                        player.sendMessage("§7Token ist möglicherweise abgelaufen.");
                    } else {
                        player.sendMessage("§6§l[BierAndFriends] §cSync fehlgeschlagen!");
                        player.sendMessage("§7Fehlercode: §e" + code);
                        player.sendMessage("§7Bitte versuche es später erneut.");
                        BAFSync.getInstance().getLogger().warning(
                            "Sync fehlgeschlagen für " + name + ": " + code + " - " + response
                        );
                    }
                });

            } catch (Exception e) {
                BAFSync.getInstance().getLogger().severe("Verbindungsfehler: " + e.getMessage());
                Bukkit.getScheduler().runTask(BAFSync.getInstance(), () -> {
                    player.sendMessage("§6§l[BierAndFriends] §cVerbindungsfehler zur Sync API!");
                    player.sendMessage("§7Bitte kontaktiere einen Admin.");
                });
            }
        });

        return true;
    }

    private String mapRank(String group) {
        switch (group.toLowerCase()) {
            case "default": return "malzbier";
            case "feierabendbier": return "feierabendbier";
            case "vollwieneimer": return "vollwieneimer";
            case "absturzlegende": return "absturzlegende";
            case "builder": return "builder";
            case "moderator": return "moderator";
            case "supporter": return "supporter";
            case "trainee": return "trainee";
            case "admin": return "admin";
            case "cheffe": return "cheffe";
            default: return "malzbier";
        }
    }

    private String getRankDisplayName(String rank) {
        switch (rank.toLowerCase()) {
            case "malzbier": return "🍺 Malzbier";
            case "feierabendbier": return "🍻 Feierabendbier";
            case "vollwieneimer": return "🪣 Vollwieneimer";
            case "absturzlegende": return "💀 Absturzlegende";
            case "builder": return "🔨 Builder";
            case "moderator": return "🛡️ Moderator";
            case "supporter": return "💬 Supporter";
            case "trainee": return "🌱 Trainee";
            case "admin": return "⚡ Admin";
            case "cheffe": return "👑 Cheffe";
            default: return rank;
        }
    }
}

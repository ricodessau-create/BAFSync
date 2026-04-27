package eu.baf.sync;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SyncCommand implements CommandExecutor {

    private static final String API_URL = "https://us-central1-bierandfriends-c1764.cloudfunctions.net/sync";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Nur Spieler können diesen Befehl nutzen.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage("§cBenutzung: /biersync <token>");
            return true;
        }

        String token = args[0];
        String uuid = player.getUniqueId().toString();
        String name = player.getName();

        LuckPerms lp = LuckPermsProvider.get();
        User user = lp.getUserManager().getUser(player.getUniqueId());

        if (user == null) {
            player.sendMessage("§cFehler: Konnte deinen Rang nicht abrufen.");
            return true;
        }

        String group = user.getPrimaryGroup();
        String ingameRank = mapRank(group);

        sendSyncRequest(token, uuid, name, ingameRank, player);
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

    private void sendSyncRequest(String token, String uuid, String name, String rank, Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(BAFSync.getInstance(), () -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "application/json");
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);

                String json = "{"
                    + "\"token\":\"" + token + "\","
                    + "\"uuid\":\"" + uuid + "\","
                    + "\"minecraftName\":\"" + name + "\","
                    + "\"rank\":\"" + rank + "\""
                    + "}";

                OutputStream os = con.getOutputStream();
                os.write(json.getBytes());
                os.flush();
                os.close();

                int responseCode = con.getResponseCode();

                Bukkit.getScheduler().runTask(BAFSync.getInstance(), () -> {
                    if (responseCode == 200) {
                        player.sendMessage("§aDein Account wurde erfolgreich synchronisiert!");
                    } else if (responseCode == 404) {
                        player.sendMessage("§cUngültiger Token! Erstelle einen neuen in der App.");
                    } else {
                        player.sendMessage("§cSync fehlgeschlagen. Bitte versuche es später erneut.");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTask(BAFSync.getInstance(), () ->
                    player.sendMessage("§cVerbindungsfehler. Bitte versuche es später erneut.")
                );
            }
        });
    }
}

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

        // LuckPerms API
        LuckPerms lp = LuckPermsProvider.get();
        User user = lp.getUserManager().getUser(player.getUniqueId());

        if (user == null) {
            player.sendMessage("§cFehler: Konnte deinen Rang nicht abrufen.");
            return true;
        }

        String group = user.getPrimaryGroup();

        // Mapping
        String ingameRank = mapRank(group);

        // HTTP Request an App
        sendSyncRequest(token, uuid, ingameRank);

        player.sendMessage("§aSync-Anfrage gesendet!");
        return true;
    }

    private String mapRank(String group) {
        switch (group.toLowerCase()) {
            case "default":
                return "malzbier";
            case "feierabendbier":
                return "feierabendbier";
            case "vollwieneimer":
                return "vollwieneimer";
            case "absturzlegende":
                return "absturzlegende";
            default:
                return "unbekannt";
        }
    }

    private void sendSyncRequest(String token, String uuid, String rank) {
        Bukkit.getScheduler().runTaskAsynchronously(BAFSync.getInstance(), () -> {
            try {
                URL url = new URL("https://baf.bierandfriends.eu/api/sync");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "application/json");

                String json = "{ \"token\": \"" + token + "\", \"uuid\": \"" + uuid + "\", \"rank\": \"" + rank + "\" }";

                OutputStream os = con.getOutputStream();
                os.write(json.getBytes());
                os.flush();
                os.close();

                con.getResponseCode(); // send request
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}

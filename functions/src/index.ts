import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();

export const biersync = functions.https.onRequest(async (req, res) => {
    res.set("Access-Control-Allow-Origin", "*");
    res.set("Access-Control-Allow-Methods", "POST, OPTIONS");
    res.set("Access-Control-Allow-Headers", "Content-Type");

    if (req.method === "OPTIONS") {
        res.status(204).send("");
        return;
    }

    if (req.method !== "POST") {
        res.status(405).send("Method not allowed");
        return;
    }

    try {
        const { token, uuid, name, bedrock } = req.body || {};

        if (!token || !uuid || !name) {
            res.status(400).json({ success: false, message: "Fehlende Felder" });
            return;
        }

        const tokenDoc = await db.collection("sync_tokens").doc(token).get();

        if (!tokenDoc.exists) {
            res.status(400).json({
                success: false,
                message: "Ungültiger Token. Bitte neu generieren."
            });
            return;
        }

        const tokenData = tokenDoc.data();
        const uid = tokenData?.uid;

        if (!uid) {
            res.status(400).json({ success: false, message: "Token ungültig" });
            return;
        }

        // Ablauf nur prüfen wenn createdAt vorhanden
        if (tokenData?.createdAt) {
            const createdAt = tokenData.createdAt.toMillis();
            const tenMinutes = 10 * 60 * 1000;
            if (Date.now() - createdAt > tenMinutes) {
                await tokenDoc.ref.delete();
                res.status(400).json({
                    success: false,
                    message: "Token abgelaufen. Bitte neu generieren."
                });
                return;
            }
        }

        // Minecraft-Daten im User-Profil speichern
        const userRef = db.collection("users").doc(uid);
        await userRef.set(
            {
                minecraftUuid: uuid,
                minecraftName: name,
                isBedrock: bedrock || false
            },
            { merge: true }
        );

        // Token nach Verwendung löschen
        await tokenDoc.ref.delete();

        // Rang + Username aus Firestore holen
        const userSnap = await userRef.get();
        const rank = userSnap.get("rank") || "malzbier";
        const username = userSnap.get("username") || name;

        console.log(`✅ ${name} (${uuid}) [Bedrock: ${bedrock}] → UID ${uid}, Rang: ${rank}`);

        res.json({
            success: true,
            message: "Sync erfolgreich",
            rank: rank,
            username: username
        });

    } catch (e) {
        console.error("Fehler:", e);
        res.status(500).json({ success: false, message: "Serverfehler" });
    }
});

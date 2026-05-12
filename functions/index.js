const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();
const reg = "us-central1";

// 1. BIERSYNC (HTTP Trigger für Minecraft Plugin)
exports.biersync = functions.region(reg).https.onRequest(async (req, res) => {
    res.set("Access-Control-Allow-Origin", "*");
    if (req.method === "OPTIONS") {
        res.status(204).send("");
        return;
    }
    try {
        const { token, uuid, name, bedrock, rank, roles } = req.body || {};
        const tDoc = await db.collection("sync_tokens").doc(token).get();

        if (!tDoc.exists) {
            res.status(400).send("No");
            return;
        }

        const uid = tDoc.data() ? tDoc.data().uid : null;

        if (uid) {
            // Hier werden jetzt auch rank und roles gespeichert wie in deinem alten Code
            await db.collection("users").doc(uid).set({ 
                minecraftUuid: uuid, 
                minecraftName: name, 
                isBedrock: !!bedrock,
                rank: rank || "Spieler",
                roles: roles || []
            }, { merge: true });
            
            await tDoc.ref.delete();
            res.json({ success: true });
        } else {
            res.status(400).send("No UID");
        }
    } catch (e) {
        res.status(500).send("Error");
    }
});

// 2. AUTOMATISCHE PUSH BENACHRICHTIGUNGEN (Trigger für Firestore)
const collections = ["tickets", "private_chats", "public_chat", "events", "forum", "market", "private_messages"];

collections.forEach(col => {
    const funcName = `onNew${col.charAt(0).toUpperCase() + col.slice(1)}`;
    exports[funcName] = functions.region(reg).firestore.document(`${col}/{id}`).onCreate(async (snap) => {
        const newData = snap.data();
        const receiverId = newData.receiverId;

        if (!receiverId) return null;

        try {
            const userDoc = await db.collection("users").doc(receiverId).get();
            const fcmToken = userDoc.data() ? userDoc.data().fcmToken : null;

            if (fcmToken) {
                const payload = {
                    notification: {
                        title: "Neue Nachricht",
                        body: newData.text || `Du hast eine neue Nachricht in ${col}`,
                    }
                };
                await admin.messaging().sendToDevice(fcmToken, payload);
                console.log(`Push gesendet an ${receiverId}`);
            }
        } catch (error) {
            console.error("Push Fehler:", error);
        }
        return null;
    });
});

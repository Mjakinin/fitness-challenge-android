# 4X Challenge (Nerv-Coach) – Native Android App 📱🔥

> Deine persönliche native Android-App für die wöchentliche 4-Einheiten Sportchallenge (zu dritt).  
> Ausgestattet mit **echtem `AlarmManager`**, **Android Home-Screen Widget**, **freier Wochentags-Bearbeitung** und **flexiblem Trainingsplan-Modus**!

---

## 🎯 Challenge-Regeln
- **4 Pflicht-Einheiten pro Woche:** Nur Fitness (Gym/Kraft) oder Joggen (Laufen).
- **Maximal 2 Einheiten pro Kalendertag:** Limit wird strikt durchgesetzt.
- **Deadline-Logik („Spätestens wann?“):**  
  - 4 Einheiten offen: Spätestens Samstag (Notfall-Doppelschicht Sa 2x + So 2x).  
  - Puffer-Alarm schlägt rechtzeitig an, bevor das Wochenende zur Hölle wird!

---

## ✨ Features
1. **🗓️ Jeder Tag der Woche frei bearbeitbar:**
   - 7-Tage-Leiste (`Mo`, `Di`, `Mi`, `Do`, `Fr`, `Sa`, `So`).
   - Jeder beliebige Tag kann angewählt werden, um Einheiten nachzutragen oder Missklicks mit `[-]` zu korrigieren.
2. **📱 Android Home-Screen Widget:**
   - Widget für deinen Startbildschirm mit Live-Status (`3 / 4 Einheiten`).
   - Direkt auf dem Homescreen zwei 1-Klick Buttons: `[ + 🏋️ ]` und `[ + 🏃 ]`.
3. **🎯 Trainingsplan & Modi:**
   - **🔄 Sporadisch / Flexibel:** Keine festen Tage – dynamische Mahnungen je nach verbleibendem Puffer.
   - **📅 Feste Tage:** Wähle z. B. Dienstag, Donnerstag, Samstag, Sonntag – Erinnerungen erinnern dich gezielt an deinen Trainingstagen.
   - **Kombination wählen:** Frei einstellbares Verhältnis zwischen Fitness und Joggen (Summe = 4).
4. **⏰ 100% Offline-Alarme:**
   - Nutzt `AlarmManager.setExactAndAllowWhileIdle()`, um dein Handy selbst im Flugmodus und aus dem tiefsten Android-Akkuschlaf (*Doze Mode*) aufzuwecken.
   - High-Priority Notification Channel mit Vibration und Alarmton.
   - Schnelle Buchung direkt aus der Benachrichtigungsleiste.

---

## 🚀 Entwicklung & Build

### In Android Studio öffnen:
1. Android Studio starten ➔ `Open`.
2. Den Projektordner auswählen.
3. Gradle synchronisieren lassen.
4. Auf den grünen **▶ Run-Button** klicken.

### Per Terminal bauen:
```bash
# Debug APK kompilieren
./gradlew assembleDebug

# Output liegt unter:
# app/build/outputs/apk/debug/app-debug.apk
```

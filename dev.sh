#!/bin/bash
set -e

# Se positionner dans le dossier du projet
cd "$(dirname "$0")"

# Vérifier la connexion ADB
DEVICE=$(adb devices | grep -E "device$" | head -n1 | cut -f1)
if [ -z "$DEVICE" ]; then
    echo "❌ Aucun appareil Android détecté en adb. Vérifiez le débogage sans fil."
    exit 1
fi
echo "📱 Appareil cible : $DEVICE"

echo "⚙️  Compilation et installation incrémentale..."
./gradlew installDebug --build-cache

echo "🚀 Lancement de l'application..."
adb shell am start -n com.aglae.form/.MainActivity

echo "✅ Prêt ! Pour voir les logs en direct : adb logcat | grep -i com.aglae.form"

#!/bin/bash
# Compile X4 Boutons sans Gradle (build-tools Android + android.jar + JDK 17+).
#
# Configurer les chemins via variables d'environnement, p.ex. :
#   BT=/chemin/vers/build-tools AJ=/chemin/vers/android-34/android.jar \
#   KS_PASS=monMotDePasse ./build.sh
#
# BT      = dossier build-tools (aapt2, d8, zipalign, apksigner)  [build-tools 34+ requis]
# AJ      = android.jar de la plateforme (API 34)
# KS_PASS = mot de passe du keystore de signature (par defaut : "changeme")
set -e
cd "$(dirname "$0")"
BT="${BT:-/c/pt/androidsdk/bt36/android-16}"
AJ="${AJ:-/c/pt/androidsdk/android-34/android.jar}"
KS_PASS="${KS_PASS:-changeme}"
W() { cygpath -w "$1" 2>/dev/null || echo "$1"; }
rm -rf build && mkdir -p build/res build/classes build/dex
"$BT/aapt2.exe" compile --dir "$(W res)" -o "$(W build/res.zip)"
"$BT/aapt2.exe" link -I "$(W $AJ)" --manifest "$(W AndroidManifest.xml)" -o "$(W build/base.apk)" "$(W build/res.zip)" --min-sdk-version 29 --target-sdk-version 34 --java "$(W build)"
javac --release 17 -XDstringConcat=inline -encoding UTF-8 -Xlint:-options -classpath "$(W $AJ)" -d "$(W build/classes)" $(find src build/fr -name '*.java' | while read f; do W "$f"; done)
"$BT/d8.bat" --release --min-api 29 --lib "$(W $AJ)" --output "$(W build/dex)" $(find build/classes -name '*.class' | while read f; do W "$f"; done)
cp build/base.apk build/unsigned.apk
python -I -c "import zipfile,sys; z=zipfile.ZipFile(sys.argv[1],'a',zipfile.ZIP_DEFLATED); z.write(sys.argv[2],'classes.dex'); z.close()" "$(W build/unsigned.apk)" "$(W build/dex/classes.dex)"
"$BT/zipalign.exe" -f -p 4 "$(W build/unsigned.apk)" "$(W build/aligned.apk)"
# Genere un keystore de developpement s'il n'existe pas (gardez le votre en prive).
[ -f x4boutons.keystore ] || keytool -genkeypair -keystore x4boutons.keystore -alias x4 -keyalg RSA -keysize 2048 -validity 36500 -storepass "$KS_PASS" -keypass "$KS_PASS" -dname "CN=X4 Boutons"
"$BT/apksigner.bat" sign --ks "$(W x4boutons.keystore)" --ks-pass pass:"$KS_PASS" --key-pass pass:"$KS_PASS" --out "$(W X4Boutons.apk)" "$(W build/aligned.apk)"
"$BT/apksigner.bat" verify "$(W X4Boutons.apk)" && ls -la X4Boutons.apk

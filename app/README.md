# X4 Boutons

Appli Android qui rend les **2 boutons physiques programmables** du Crosscall Core-X4 sous GSI
(la gestion native Crosscall est perdue en passant en LineageOS, car elle était codée dans le
framework Android 10 d'origine).

## Fonctionnement

- Un `AccessibilityService` capte les touches **F11** (bouton 1) et **F12** (bouton 2), émises par
  `gpio-keys.kl` (voir `../patches/gpio-keys.kl`).
- Gestes reconnus par bouton : **appui court / double / long / triple**.
- Actions possibles : rien, lampe torche, appareil photo, lancer une application, capture d'écran,
  notifications, réglages rapides, assistant vocal, sonnerie/vibreur.
- **Écran éteint** : le `WAKE` du keylayout rallume l'écran au 1er appui ; l'appui suivant déclenche
  l'action (limite Android : l'appui qui réveille l'écran n'est pas transmis aux applis).
- Interface façon Paramètres LineageOS (Material You, couleurs dynamiques).

Package : `fr.x4.boutons`. `minSdk 29`, `targetSdk 34`.

## Compiler

Sans Gradle. Il faut : **build-tools Android 34+**, `android.jar` (API 34), **JDK 17+**, Python 3.

```bash
BT=/chemin/vers/build-tools \
AJ=/chemin/vers/android-34/android.jar \
KS_PASS=votreMotDePasse \
./build.sh
```

Produit `X4Boutons.apk` (signé). Au 1er lancement, `build.sh` crée un keystore de développement
`x4boutons.keystore` s'il n'existe pas — **gardez le vôtre en privé** (il n'est pas publié ici) pour
pouvoir signer les mises à jour avec la même clé.

> Note : `build.sh` utilise les build-tools 34+ car `d8` version 34 peut échouer avec des JDK récents ;
> `javac` est appelé avec `--release 17 -XDstringConcat=inline`.

## Installer

- **Par le firmware** : l'APK est intégré en `/system/app/X4Boutons` par `patch_gsi.sh`, et le service
  est activé au 1er démarrage par `x4defaults.sh`.
- **À la main** : `adb install app/prebuilt/X4Boutons.apk`, puis activer « X4 Boutons » dans
  Paramètres > Accessibilité.

## Fichiers

```
AndroidManifest.xml
build.sh
src/fr/x4/boutons/   MainActivity.java  KeyService.java  Actions.java
res/                 valeurs, icône, config accessibilité
prebuilt/            X4Boutons.apk (pré-compilé)
```

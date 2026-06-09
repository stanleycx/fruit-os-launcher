# Construire une release signée

Pour distribuer un APK (par exemple en l'attachant à une **GitHub Release**), il faut le
**signer** avec ta propre clé. Une release non signée ne s'installe pas.

> Pour un build de développement, pas besoin de tout ça : `./gradlew :app:assembleDebug` suffit.

## 1. Générer ta clé (une seule fois)

Garde le `.jks` **et** les mots de passe en lieu sûr (gestionnaire de mots de passe) :
si tu les perds, tu ne pourras plus signer de mises à jour cohérentes.

```bash
keytool -genkeypair -v \
  -keystore launcher-release.jks \
  -alias launcher \
  -keyalg RSA -keysize 2048 -validity 10000
```

## 2. Brancher la clé au build

```bash
cp keystore.properties.example keystore.properties
```

Édite `keystore.properties` avec tes valeurs :
```
storeFile=../launcher-release.jks      # chemin vers ton .jks (relatif au dossier app/)
storePassword=...
keyAlias=launcher
keyPassword=...
```

> `keystore.properties` et `*.jks` sont gitignorés → jamais committés. Le build se signe
> automatiquement dès que `keystore.properties` existe.

## 3. Construire

```bash
# APK release signé (à attacher à une GitHub Release)
./gradlew :app:assembleRelease
# → app/build/outputs/apk/release/app-release.apk

# Ou un Android App Bundle (.aab), si besoin
./gradlew :app:bundleRelease
# → app/build/outputs/bundle/release/app-release.aab
```

## Versions suivantes

Pour chaque nouvelle version : incrémente `versionCode` (entier, +1) et `versionName`
dans `app/build.gradle.kts`, puis reconstruis.

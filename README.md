# 🎲 Neon 5K

**Neon 5K** est une application Android du jeu de dés traditionnel **5000** (*stop ou encore*), pensée avec une direction artistique **minimaliste, cyberpunk et pixel art**, un moteur de règles **100 % paramétrable**, un aléatoire hybride forgé par les **capteurs du téléphone**, des bots intelligents et un multijoueur progressif (Pass & Play, Local sans Internet et En Ligne).

---

## ✨ Fonctionnalités principales

### 🎲 Le 5000 complet & 100 % paramétrable
* **Préréglages instantanés** : *Classique 5 dés*, *Variante 6 dés* et *Partie rapide (2 500)*.
* **Variantes optionnelles activables à la création de partie** : seuil d'ouverture (`0`, `500`, `750`, `1 000`), score exact obligatoire, main pleine (relance obligatoire), carré / quinte, suite, 3 paires (6 dés), full, **repiquage** (reprise du tour adverse), **chapeau / écrasement** et pénalité de 3 coups nuls consécutifs.
* **Guide interactif intégré** : rappel clair du barème actif et des combinaisons à tout moment.

### 🌀 Aléatoire hybride par capteurs (Entropie traçable)
* Chaque lancer combine les mesures temps réel du **gyroscope**, de **l'accéléromètre** (avec option *Secouer pour lancer*), l'horodatage à la nanoseconde (`nanoTime`), les coordonnées/durée de votre appui tactile et un sel cryptographique, hachés en **`SHA-256`** pour un tirage parfaitement uniforme.
* **Inspecteur d'entropie** : touchez le hash du lancer (`#A4F9C2`) pour voir quelles données capteurs ont forgé votre jet. Fallback silencieux garanti sur les appareils sans gyroscope.

### 🎨 Esthétique Cyberpunk × Pixel Art & Fenêtre personnalisable
* **Rendu 100 % natif Compose `Canvas` (zéro librairie graphique lourde)** : dés pixel art dessinés sur grille `16×16` sans flou, icône XML adaptative et Splash Screen Android 12+.
* **5 thèmes prédéfinis** : *Neon Night*, *Terminal Phosphor*, *CRT Amber*, *Pocket Mono* et *OLED Minimal*.
* **Studio sur-mesure** : choisissez librement vos couleurs (roue HSV + `#RRGGBB` avec indicateur de contraste WCAG), votre style de dés (*Pixel Solid*, *Neon Wireframe*, *Dot Matrix*), les scanlines CRT, le mode droitier/gaucher, les vibrations haptiques et le maintien de l'écran allumé.

### 🤖 Bots Locaux & Dataset IA ouvert
* Affrontez 3 profils d'IA probabiliste en Kotlin pur (*Safe Runner*, *Cyber Balanced*, *Overclock Kamikaze*).
* Chaque décision de partie est journalisée localement dans Room (`decision_logs`) et exportable en **`.jsonl` / `.csv`** afin d'entraîner de futures IA (RL / supervisé) sans alourdir l'application.

### 🏆 Succès & Multijoueur progressif
* **Solo & Pass & Play** sur un même écran (Lot 1–2).
* **Google Play Jeux (v2)** : connexion silencieuse et succès à débloquer (Lot 4).
* **Multijoueur Local sans Internet** via Google Nearby Connections (`P2P_STAR`, 1 appareil hôte) (Lot 5).
* **Multijoueur En Ligne** via salons privés à code 6 caractères sur Firebase Realtime Database (plan Spark 0 €) + partage opt-in anonyme de parties pour l'entraînement IA (Lot 6).

---

## 🛠️ Stack Technique & Architecture

* **Namespace / Application ID** : `fr.antoinehory.neon5k`
* **Langage** : Kotlin `2.2.10` (`jvmTarget = 11`)
* **UI** : Jetpack Compose (Material 3, BOM `2026.02.01`), Edge-to-Edge, `Canvas` pixel art natif, `core-splashscreen`.
* **Architecture** : MVVM + Clean Architecture (`ui`, `domain` Kotlin pur, `data`)
* **Persistance** : Room Database (`2.8.5`, KSP `2.3.13`) + DataStore Preferences (`1.1.3`)
* **Capteurs & Services** : `SensorManager`, Storage Access Framework (SAF), Play Games Services v2 (Lot 4), Nearby Connections (Lot 5), Firebase RTDB (Lot 6).

---

## 🚀 Compilation et Installation

### Prérequis
* **Android Studio** (Meerkat ou plus récent)
* **JDK 17+**
* **Android SDK 37** (`minSdk = 29` / Android 10+)

### Commandes Gradle
```bash
# Compiler l'APK de debug
./gradlew :app:assembleDebug

# Lancer tous les tests unitaires (moteur 5000, entropie, IA)
./gradlew :app:testDebugUnitTest

# Compiler le bundle Release optimisé R8
./gradlew :app:bundleRelease
```

---

## 🔒 Confidentialité

Le jeu est **100 % jouable hors-ligne**, sans publicité et sans achat intégré. Les mesures capteurs utilisées pour générer l'entropie restent locales et ne sont jamais stockées. Consultez la [Politique de Confidentialité](POLITIQUE_DE_CONFIDENTIALITE.md) pour plus de détails.

---

## 👤 Auteur

Développé par **Antoine « Crock » Hory**
* 🌐 Site web : [antoinehory.fr](https://antoinehory.fr)
* 📧 Contact : [contact@antoinehory.fr](mailto:contact@antoinehory.fr)
* ☕ Soutenir le projet : [PayPal.me/kuroku](https://paypal.me/kuroku)

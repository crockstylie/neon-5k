# 🎲 Neon 5K

**Neon 5K** est une application Android du jeu de dés traditionnel **5000** (*stop ou encore*), pensée avec une direction artistique **minimaliste, cyberpunk et pixel art**, un moteur de règles **100 % paramétrable**, un aléatoire hybride forgé par les **capteurs du téléphone**, des bots intelligents et un multijoueur progressif (Pass & Play, Local sans Internet et En Ligne).

---

## ✨ Fonctionnalités principales

### 🎲 Le 5000 complet & 100 % paramétrable
* **Préréglages instantanés** : *Classique 5 dés*, *Variante 6 dés* et *Partie rapide (2 500)*.
* **Toutes les variantes classiques et familiales activables à la création de partie** :
  * Seuil d'ouverture (`0`, `500`, `750`, `1 000`) et **seuil minimal par tour après ouverture** (`0`, `200`, `250`, `500`),
  * **Chiffre rond obligatoire pour banquer** (multiple de `100` pts uniquement : impossible de banquer un `5` isolé ou un score finissant par `50`),
  * **Sprint final** (*les 1 000 derniers points en un seul tour*) et score exact obligatoire (`5 000` pile),
  * Main pleine (relance obligatoire ou libre), Carré (`×2 brelan` ou désactivé), Quinte (`×4 brelan`, `5×Cinq = 5 000 pts` ou désactivée) et **Victoire instantanée sur 5 As (`5×1`)** indépendante,
  * Suite à 5 dés (`1-5` / `2-6`) et **Grande Suite à 6 dés (`1-2-3-4-5-6`)** configurables séparément, 3 Paires (6 dés), Full avec bonus,
  * **Repiquage** (reprise du pot et des dés restants du joueur précédent), **Chapeau / Écrasement** (`-500 pts` ou recul au palier précédent) et **Pénalité des 3 Encoches (`✕✕✕`)** (`-500 pts`, `-1 000 pts`, ou **recul au palier précédent avec ou sans mémoire des encoches historiques**).
* **Plateau ergonomique Portrait & Paysage** : carrousel des joueurs centré automatiquement sur le joueur actif, historique en direct des lancers et des paliers d'encoches (`✕`) au centre de l'écran et en modale dédiée, bloc de dés à hauteur verrouillée (zéro saut d'interface) et mode paysage 2 colonnes sans défilement.

### 🌀 Aléatoire hybride par capteurs (Entropie traçable)
* Chaque lancer combine les mesures temps réel du **gyroscope**, de **l'accéléromètre** (avec option *Secouer pour lancer*), l'horodatage à la nanoseconde (`nanoTime`), les coordonnées/durée de votre appui tactile et un sel cryptographique, hachés en **`SHA-256`** pour un tirage parfaitement uniforme.
* **Inspecteur d'entropie** : touchez le hash du lancer (`#A4F9C2`) pour voir quelles données capteurs ont forgé votre jet. Fallback silencieux garanti sur les appareils sans gyroscope.

### 🎨 Esthétique Cyberpunk × Pixel Art, Accessibilité & Easter Eggs
* **Rendu 100 % natif Compose `Canvas` (zéro librairie graphique lourde)** : dés pixel art dessinés sur grille `16×16` sans flou, icônes pixel art `8×8`, icône XML adaptative et Splash Screen Android 12+.
* **14 thèmes prédéfinis en 4 familles** : *Cyberpunk & Rétro* (*Neon Night*, *Terminal Phosphor*, *CRT Amber*, *Pocket Mono*, *OLED Minimal*), *Niveaux de Gris* (3 contrastes), *Sépia* (3 contrastes) et *Accessibilité Daltonisme* (Protanopie/Deutéranopie Okabe-Ito, Tritanopie, Achromatopsie) + générateurs aléatoires (*Random Harmonieux* & *Random Chaos*).
* **Studio sur-mesure** : choisissez librement vos couleurs (sélecteur hybride HSV / RGB `0..255` + `#RRGGBB` + bouton Random avec indicateur de contraste WCAG 2.1), votre style de dés (*Pixel Solid* 3D, *Neon Wireframe* CAO, *Dot Matrix* LED), les scanlines CRT, le mode droitier/gaucher, les vibrations haptiques et le maintien de l'écran allumé.

### 🤖 Bots Locaux Auto-Apprenants, Graphiques & Dataset IA ouvert
* Affrontez jusqu'à 7 bots simultanément parmi 3 profils d'IA (*Safe Runner*, *Cyber Balanced*, *Overclock Kamikaze*).
* **Auto-apprentissage local (`AdaptiveBotLearner`)** : l'IA locale se nourrit automatiquement de la base `decision_logs` au fil de vos parties (apprentissage par renforcement tabulaire à régularisation bayésienne hybridé avec les probabilités exactes $6^N$).
* **Graphiques & Historique des parties** : camemberts/donuts (victoires/défaites, points banqués vs perdus en glitch), courbes d'évolution des scores, graphiques en barres (équité SHA-256 des faces `1..6` et collection de brelans), journal complet des parties terminées avec détail lancer par lancer, et export SAF en **`.jsonl` / `.csv`**.

### 🏆 20 Succès à Compteurs Infinis & Multijoueur progressif
* **Solo & Pass & Play (jusqu'à 8 joueurs)** sur un même écran (Lots 1–2).
* **Google Play Jeux (v2)** : connexion silencieuse et **20 succès** (1 000 XP) dont les compteurs locaux continuent de progresser à l'infini après déblocage (*Bronze → Argent → Or → Cyber-Platine*) (Lot 4).
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

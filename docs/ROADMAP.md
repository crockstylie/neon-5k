# ROADMAP.md — Suivi d'Avancement du Projet (6 Lots)

> Ce fichier trace l'état exact du développement de **Neon 5K**. L'agent IA doit le consulter avant chaque session et cocher chaque tâche une fois implémentée et vérifiée par les tests.

Légende : `[x]` Terminé | `[/]` En cours | `[ ]` À faire

---

## Lot 1 : Socle, Moteur Pur du 5000, Entropie Capteurs & Plateau Pixel (Solo / Pass & Play)
- [x] Mise en place de la documentation "Source de Vérité" (`AGENTS.md`, `docs/*`, `README.md`, `CHANGELOG.md`, `PLAY_STORE.md`, `POLITIQUE_DE_CONFIDENTIALITE.md`)
- [x] Création des assets XML de démarrage (`ic_launcher_foreground.xml`, `ic_launcher_background.xml`, `colors.xml`, `themes.xml`)
- [x] Initialisation du projet Android `fr.antoinehory.neon5k` (`gradle/libs.versions.toml`, `app/build.gradle.kts`, `AndroidManifest.xml`, R8 global sans `packageScope`)
- [x] Implémentation du moteur pur Kotlin dans `domain/rules/` :
    - `GameRulesConfig` (presets *Classique 5 dés*, *Variante 6 dés*, *Rapide 2 500*, *Personnalisé* + toutes les variantes optionnelles : ouverture, score exact, main pleine, carré/quinte, suite, 3 paires, full, repiquage, chapeau/écrasement, 3 coups nuls)
    - `DiceScorer` (détection exhaustive des combinaisons scorantes et validation stricte de sélection)
    - `FiveThousandEngine` (machine à états complète d'une partie multi-joueurs)
- [x] Implémentation de l'aléatoire hybride par capteurs :
    - `EntropySeedGenerator` (`SHA-256` → seed → tirage uniforme `1..6`) dans `domain/entropy/`
    - `SensorEntropyCollector` (gyroscope + accéléromètre + `nanoTime` + coordonnées/durée tactile + sel + détection secousse + fallback sans capteur) dans `data/sensors/`
- [x] UI Pixel Art / Cyberpunk V1 :
    - `Neon5KTheme` + palette par défaut *Neon Night*
    - `PixelDiceCanvas` (matrices `16×16` pour faces `1..6`, animations courtes, états verrouillé / sélectionné / scorant / inactif)
    - `HomeScreen`, `SetupScreen` (choix joueurs Pass & Play + variantes de règles) et `GameScreen` (plateau complet + modale « Trace d'Entropie »)
- [x] Tests unitaires JVM complets sur `DiceScorer`, `FiveThousandEngine` (toutes variantes et cas limites) et `EntropySeedGenerator`

---

## Lot 2 : IA Locale, Persistance Room (Reprise + Logs IA) & Personnalisation Complète
- [x] Création de la base Room (`data/local/AppDatabase.kt`) :
    - `SavedMatchEntity` & `MatchDao` (sauvegarde automatique en cours de partie + reprise depuis l'accueil + historique)
    - `PlayerStatsEntity` & `StatsDao` (parties jouées/gagnées, meilleur tour, nb de mains pleines, nb de crashs, plus gros repiquage)
    - `DecisionLogEntity` & `DecisionLogDao` (enregistrement état/action/issue + `rulesFingerprint`)
- [x] Création de `AiDatasetExporter` (`data/export/`) : export `.jsonl` et `.csv` via SAF (zéro permission stockage) pour entraîner de futures IA
- [x] Implémentation de l'IA locale dans `domain/ai/` :
    - `RollProbabilityTable` (probabilité exacte de coup nul et espérance selon nb de dés restants)
    - Interface `BotPolicy` et implémentation `HeuristicBotPolicy` (3 profils : *Safe Runner*, *Cyber Balanced*, *Overclock Kamikaze*)
    - Intégration transparente des bots dans `SetupScreen` et `GameViewModel` (avec délai lisible pour voir les choix du bot)
- [x] Création de `UserPreferencesRepository` (DataStore) et `CustomizationScreen` :
    - 5 presets cyberpunk/pixel (*Neon Night*, *Terminal Phosphor*, *CRT Amber*, *Pocket Mono*, *OLED Minimal*)
    - Mode `CUSTOM` avec `ColorPickerDialog` (roue HSV + `#RRGGBB`) et contrôle de contraste WCAG
    - Choix du style de dés (*Pixel Solid*, *Neon Wireframe*, *Dot Matrix*), scanlines CRT, mode droitier/gaucher, haptique, secousse pour lancer et `keepScreenOn`
- [x] Création de `StatsScreen` (statistiques locales + bouton d'export dataset IA)
- [x] Tests unitaires JVM sur `HeuristicBotPolicy`, `RollProbabilityTable` et tests instrumentés Room (`DecisionLogDao`, `MatchDao`)

---

## Lot 3 : Identité Visuelle Finale, Règles Interactives, InfoScreen (OCRAurebesh), i18n (4 Langues) & Préparation Play Store
- [x] Intégration finale de l'icône XML adaptative (`mipmap-anydpi/ic_launcher*.xml`) et du Splash Screen Android 12+ (`installSplashScreen()` dans `MainActivity`)
- [x] Intégration de la typographie pixel/monospace cyberpunk dans `Neon5KTheme`
- [x] Création de `RulesScreen` (guide clair et interactif des règles du 5000 + simulateur rapide du barème Pixel Art 5/6 dés + modale `[?]` en pleine partie + explication des Paliers & 3 Encoches `✕`)
- [x] Création de `InfoScreen` basé sur le modèle d'`OCRAurebesh` :
    - Import des 6 icônes vectorielles XML de réseaux sociaux (`ic_linkedin_logo.xml`, `ic_behance_logo.xml`, `ic_instagram_logo.xml`, `ic_facebook_logo.xml`, `ic_spotify_logo.xml`, `ic_steam_logo.xml`)
    - Coordonnées développeur (`Antoine Crock HORY`, `antoinehory.fr`, `contact@antoinehory.fr`, `paypal.me/kuroku`)
    - Bouton de don PayPal (`https://paypal.me/kuroku`), bouton de notation 5 ⭐ sur le Play Store (`market://details?id=fr.antoinehory.neon5k`), bouton « Découvrir mes autres applis », numéro de version et résumé confidentialité
- [x] Traduction complète des ressources en 4 langues : `values/`, `values-fr/`, `values-en/`, `values-es/`, `values-pt/` + `locales_config.xml` + sélecteur de langue in-app (`FR | EN | ES | PT`)
- [x] Audit Edge-to-Edge, UI adaptative (portrait/paysage/tablette), performance Compose (`Canvas` recompositions) et vérification build Release R8 (`:app:assembleRelease`)

---

## Lot 4 : Intégration Google Play Jeux (PGS v2), 20 Succès (Compteurs Infinis & Assets Pixel Art) & Télémétrie Fun
- [x] Ajout de `com.google.android.gms:play-services-games-v2` dans `libs.versions.toml`
- [x] Enrichissement de `PlayerStatsEntity` et `StatsScreen` (onglets **Télémétrie & Fun Stats** / **Succès & Badges** / **Dataset IA**) :
    - **Entropie & Capteurs** : lancers totaux, dés individuels jetés, lancers par secousse physique, durée cumulée d'appui tactile, histogramme de distribution des faces `1..6` (vérification d'équité SHA-256)
    - **Brelans & Combinaisons** : total des brelans et détail par face (Brelans d'As, de 2, de 3, de 4, de 5, de 6), carrés, quintes, suites, 3 paires, fulls, As et 5 isolés
    - **Prise de risque & Psychologie** : points cumulés banqués vs points perdus en Glitch, pire crash en un tour, mains pleines, repiquages réussis, chapeaux infligés vs subis, pénalités 3 coups nuls
    - **Bilan vs IA** : victoires/défaites détaillées contre *Safe Runner*, *Cyber Balanced* et *Overclock Kamikaze*
- [x] Système de **20 Succès avec comptage continu post-déverrouillage** (`AchievementEntity` + `AchievementEvaluator`) :
    - Chaque succès conserve `currentCount` qui **continue de s'incrémenter indéfiniment** même après `isUnlocked = true` (affichage du palier + compteur total à vie `Total : ×N` et rangs de maîtrise locaux *Bronze / Argent / Or / Cyber-Platine*)
    1. *First Boot* — Réussir son ouverture (palier : `1`, compteur : total d'ouvertures)
    2. *High Voltage Opening* — Ouvrir avec `>= 1 500 pts` en un seul tour (palier : `1`, compteur : total)
    3. *Triple Ace Protocol* — Faire un brelan d'As en un lancer (palier : `1`, compteur : total de brelans d'As)
    4. *Brelan Collector* — Réaliser au moins un brelan de chaque face (`1` à `6`) (palier : `6/6`, compteur : total tous brelans)
    5. *Quad Core* — Obtenir un Carré (4 dés identiques) en un lancer (palier : `1`, compteur : total de carrés)
    6. *Five of a Kind / God Roll* — Obtenir une Quinte (5 dés identiques) en un lancer (palier : `1`, compteur : total de quintes)
    7. *Straight Runner* — Réussir une Suite en un lancer (palier : `1`, compteur : total de suites)
    8. *Twin Matrix* — Réussir 3 Paires en mode 6 dés (palier : `1`, compteur : total de 3 paires)
    9. *Full House Cyber* — Réussir un Full avec bonus actif (palier : `1`, compteur : total de fulls)
    10. *Hot Dice!* — Réaliser une Main pleine (palier : `1`, compteur : total de mains pleines)
    11. *Full Overclock* — Enchaîner 2 mains pleines dans le même tour (palier : `1`, compteur : total de doubles mains pleines)
    12. *One Die Miraculé* — Scorer en ne relançant qu'un seul dé restant (palier : `1`, compteur : total de sauvetages à 1 dé)
    13. *System Crash* — Perdre un pot de tour d'au moins `1 000 pts` sur un Glitch (palier : `1`, compteur : total de gros crashs)
    14. *Three Strikes Out* — Subir la pénalité de 3 coups nuls consécutifs (palier : `1`, compteur : total)
    15. *Pixel Perfect 5000* — Gagner une partie avec le score exact de la cible (palier : `1`, compteur : total de victoires exactes)
    16. *Overkill Turn* — Banquer `>= 2 000 pts` en un seul tour (palier : `1`, compteur : total de tours >= 2 000 pts)
    17. *Hijacker* — Gagner des points grâce au Repiquage du pot adverse (palier : `1`, compteur : total de repiquages)
    18. *Headshot* — Écraser le score d'un adversaire avec la variante Chapeau (palier : `1`, compteur : total de chapeaux infligés)
    19. *Beat the Machine* — Battre l'IA *Overclock Kamikaze* (palier : `1`, compteur : total de victoires vs Kamikaze)
    20. *Entropy Master* — Jouer 50 parties complètes (palier : `50`, compteur : total de parties terminées) + *Shake It Off* (25 lancers à la secousse)
- [x] Création des **assets visuels Pixel Art Cyberpunk** pour les 20 icônes de succès :
    - `VectorDrawable` XML ou grilles `16×16` dans l'application (affichés en couleur néon si débloqué, wireframe grisé si verrouillé)
    - Générateur/exporteur des 20 icônes au format `512 × 512 px` PNG prêtes à téléverser dans la Google Play Console (`docs/PLAY_GAMES_ACHIEVEMENTS.md`)
- [x] Implémentation de `PlayGamesService` et `GooglePlayGamesService` (authentification silencieuse + `AchievementsClient.unlock()` / `setSteps()`)
- [x] Tests unitaires JVM de `AchievementEvaluator` (vérifiant le déblocage ET l'incrémentation continue au-delà du seuil)
- [x] Recettage UI/UX & Easter Eggs post-Lot 4 :
    - Harmonisation des en-têtes sur 1 seule ligne avec espacement garanti (`NeonSubScreenTopBar`) et ajout d'icônes Pixel Art `8×8` (`PixelIconCanvas`) sur les boutons
    - Réorganisation ergonomique de `CustomizationScreen` (sélecteur de style de dés placé juste entre l'aperçu des dés et les palettes)
    - Différenciation franche des 3 styles de dés (`PIXEL_SOLID` plein 3D biseauté, `NEON_WIREFRAME` filaire CAO creux, `DOT_MATRIX` diodes LED circulaires)
    - Ajout des générateurs de thèmes aléatoires (*Random Harmonieux* & *Random Chaos*), des presets *Niveaux de Gris* (3 contrastes), *Sépia* (3 contrastes) et *Accessibilité Daltonisme* (Protanopie/Deutéranopie Okabe-Ito, Tritanopie, Achromatopsie)
    - Évolution de `ColorPickerDialog` avec bascule **Mode HSV / Mode RGB (`0..255`)** et bouton **Random**
    - Système d'Easter Eggs multi-taps (`EasterEggOverlay.kt`) : **42 taps** sur les dés d'accueil (générique Star Wars `42` en dés jaunes avec ciel étoilé aléatoire, intro cyan en fondu et inclinaison 3D renforcée + *"Answer to the Ultimate Question of Life, the Universe, and Everything"* en anglais), **13 taps** sur l'entropie d'accueil (*Overclock 1337 Matrix*) et **7 taps** sur la version dans `InfoScreen` (*Holocron Aurebesh / Sabacc*)
    - **Recettage Gameplay, Plateau Portrait/Paysage, Graphiques & IA Auto-Apprenante** :
        - Nouvelles variantes : **Chiffre rond multiple de 100 obligatoire pour banquer** (`requireMultipleOf100ToBank`), **Seuil minimal par tour après ouverture** (`minTurnScoreAfterOpening`), **Sprint final des 1 000 derniers points en 1 tour** (`finalSprintLast1000InOneTurn`), découplage **Quinte (`×4` / `5×5 = 5000`) & Victoire instantanée sur `5×As`**, et séparation **Suite 5 dés (`straightScore`) / Grande Suite 6 dés (`fullStraight6DiceScore`)**
        - **IA Locale Auto-Apprenante (`AdaptiveBotLearner`)** : apprentissage par renforcement tabulaire bayésien nourri automatiquement en temps réel par `decision_logs`
        - **Refonte `GameScreen`** : carrousel joueurs centré automatiquement (`LazyRow`), bandeau tour/pot sur 1 ligne, historique en direct des lancers & paliers d'encoches (`✕`) au centre + modale dédiée, hauteur du bloc d'actions sous les dés verrouillée (zéro saut des dés), bouton retour avec icône Pixel Art `BACK`, et mode Paysage 2 colonnes sans scroll
        - **Correction `NEON_WIREFRAME`** : points `2×2` dessinés en bloc unifié (sans effet « mini-fenêtre »)
        - **Graphiques & Historique des parties (`StatsScreen`)** : onglet `PARTIES` (journal dépliable des parties et lancers) + graphiques Compose `Canvas` (camemberts/donuts, courbe de scores, barres verticales)
    - **Recettage Play Store, Ergonomie Bots & Easter Eggs (v1.0.3 / `versionCode 4`)** :
        - **Play Games Services v2 & Release** : intégration des identifiants officiels Google Play Console (`res/values/games-ids.xml`, App ID `59639547526`, 20 succès), initialisation `PlayGamesSdk.initialize()` dans `Neon5KApplication.onCreate()`, auto-sync des succès locaux à la connexion + diagnostic `ApiException`, et passage en `versionCode = 4` / `versionName = "1.0.3"`
        - **Conformité Play Console (Edge-to-Edge & SDK)** : mise à jour forcée de `androidx.fragment:fragment` (`1.1.0` → `1.9.1`), `enableEdgeToEdge()` avant `super.onCreate()`, `decorFitsSystemWindows = false` + `WindowInsets.safeDrawing` sur `StarWars42EasterEggDialog`, et `app/proguard-rules.pro` conservant `EdgeToEdge.enable*` pour l'analyseur statique R8
        - **Room Migration `v2 -> v3` (`AppDatabase`)** : migration non-destructive de `saved_matches` préservant l'historique et les parties en cours lors des mises à jour
        - **Historique Multi-Joueurs (`StatsScreen`)** : remplacement de la ligne tassée par un `FlowRow` responsive de badges de scores (`3` à `8` joueurs)
        - **Pause & Pas-à-Pas Bots (`GameViewModel` / `GameScreen`)** : bouton rapide `⏸` / `▶` et bandeau de pause sur les tours de bots (dont parties 100 % Bots) avec mise en pause automatique sur `ON_PAUSE` pour inspecter les lancers
        - **Nommage Joueurs & Bots (`SetupScreen`)** : champ d'édition du nom disponible pour tous les emplacements (humains et bots) avec conservation du nom personnalisé lors du changement de profil IA
        - **Correctifs Easter Eggs (`EasterEggOverlay.kt`)** : départ du crawl Star Wars `42` depuis le tout bas de l'écran (`TransformOrigin(0.5f, 0f)` + `maxHeightPx`), fondu progressif (`fade out`) de la phrase bleue à l'approche du `42` jaune, et délai anti-fermeture accidentelle (`900 ms`) sur les 3 modales Easter Egg

---

## Lot 4.5 : Pré-Lot 5 — Polish Audio 8-bit, Vitesse Bots & Audit Qualité (Avant Multijoueur Local)
- [x] **Sélecteur de vitesse des Bots (`BotSpeed` : `NORMAL ×1`, `FAST ×2`, `TURBO ×4`, `INSTANT ⚡`)** : préférence persistée dans `UserPreferencesRepository`, changement réactif à la volée sur `GameScreen` (prise en compte `<= 25 ms` même en cours d'attente) et dans `CustomizationScreen`, plus garde stricte empêchant les bots de débloquer des succès (`GameViewModel`, `GameRepository`, `AchievementEvaluator`)
- [x] **Synthèse Audio 8-bit Rétro sans asset lourd (`SoundEffectsEngine`)** : génération PCM légère en Kotlin (`AudioTrack`) pour lancers, sélection de dés, Glitch/Crash, Main pleine, banque et victoire, avec toggle `soundEnabled` dans `CustomizationScreen`
- [x] **Version dynamique & Diagnostic Play Jeux (`InfoScreen` / `StatsScreen`)** : lecture dynamique de `versionName` / `versionCode` via `PackageManager` (remplacement du texte figé `v1.0.0 (BUILD 1)`) et statut détaillé de synchronisation PGS v2
- [x] **Inspecteur & Reset IA (`StatsScreen`)** : visualisation enrichie des statistiques `AdaptiveBotLearner` (maturité par contexte/profil) et action dédiée de réinitialisation ciblée de l'apprentissage sans perte des stats joueur
- [x] **Polish Fin de Partie & Onboarding 1er lancement (`GameScreen`)** : micro-bulle d'aide contextuelle au premier lancer (*sélectionner les dés scorants avant de relancer/banquer*), récapitulatif de fin de match enrichi (MVP du tour, succès débloqués dans la partie, bouton *Rejouer avec les mêmes règles*) et vérification Release R8 + Play Games sur appareil réel
- [x] **Blindage Play Store PGS v2 (`v1.0.5 / BUILD 6`)** : verrouillage `translatable="false"` + `tools:ignore="MissingTranslation"` dans `res/values/games-ids.xml`, nouvelle clé `pgs_app_id` dans `AndroidManifest.xml` (immunité contre l'écrasement `split_config.<lang>.apk` par les caches de traduction Play Console `v1.0.0`), repli Kotlin dans `Neon5KApplication` / `GooglePlayGamesService` et documentation du SHA-1 Play App Signing (`AE:21:8F:...`)

---

## Lot 5 : Multijoueur Local sans Internet (Google Nearby Connections)
- [ ] Ajout de `com.google.android.gms:play-services-nearby` dans `libs.versions.toml`
- [ ] Définition de l'interface commune `MultiplayerTransport` et des messages sérialisés
- [ ] Implémentation de `NearbyTransport` en topologie étoile (`Strategy.P2P_STAR`) :
    - Mode Hôte (advertise du salon + vérification PIN Néon `authenticationDigits` + arbitrage via `FiveThousandEngine` + diffusion de `MatchSnapshot`)
    - Mode Client (discovery + connexion par nom de salon + lancer physique local avec `SensorEntropyCollector` et envoi de la preuve d'entropie `SHA-256` à la table)
- [ ] Gestion pédagogique des permissions Android 10–16 (`BLUETOOTH_*`, `NEARBY_WIFI_DEVICES`, `ACCESS_FINE_LOCATION` API 29–30) uniquement à l'entrée du mode local
- [ ] Écran `LobbyScreen` (création/recherche de salon local, code PIN Néon à 4 chiffres, liste des joueurs prêts, synchronisation en direct du « Manifeste des Règles » choisi par l'hôte)
- [ ] Tolérance aux micro-coupures : reconnexion transparente ou remplacement à chaud d'un joueur déconnecté par un Bot IA (*Cyber Balanced*) ou reprise de contrôle sur l'écran Hôte

---

## Lot 6 (Optionnel) : Multijoueur En Ligne & Sync Dataset IA (Firebase Realtime Database Spark 0 €)
- [ ] Configuration projet Firebase (Spark 0 €, Realtime Database + Auth anonyme ou lien Play Jeux)
- [ ] Implémentation de `FirebaseRtdbTransport` :
    - Création/jonction de salon privé via code court à 6 caractères
    - Écritures minimalistes (uniquement sur lancer/validation de tour) avec tirage client via `SensorEntropyCollector` + `entropyHash`
    - Gestion présence via `onDisconnect()` et nettoyage des salons expirés
- [ ] Règles de sécurité Firebase Realtime Database (`database.rules.json`) versionnées dans le repo
- [ ] Synchronisation opt-in du dataset IA (`shareAnonymousAiLogs` désactivé par défaut) :
    - 1 seule écriture agrégée JSON par partie terminée dans `/ai_datasets/{rulesFingerprint}/{matchUuid}`
    - Marquage `uploadedToCloud = true` dans Room pour éviter tout doublon
- [ ] Mise à jour de `POLITIQUE_DE_CONFIDENTIALITE.md` et de la section *Data Safety* dans `PLAY_STORE.md` avant mise en production du Lot 6

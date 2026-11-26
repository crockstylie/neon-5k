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
- [ ] Création de la base Room (`data/local/AppDatabase.kt`) :
    - `SavedMatchEntity` & `MatchDao` (sauvegarde automatique en cours de partie + reprise depuis l'accueil + historique)
    - `PlayerStatsEntity` & `StatsDao` (parties jouées/gagnées, meilleur tour, nb de mains pleines, nb de crashs, plus gros repiquage)
    - `DecisionLogEntity` & `DecisionLogDao` (enregistrement état/action/issue + `rulesFingerprint`)
- [ ] Création de `AiDatasetExporter` (`data/export/`) : export `.jsonl` et `.csv` via SAF (zéro permission stockage) pour entraîner de futures IA
- [ ] Implémentation de l'IA locale dans `domain/ai/` :
    - `RollProbabilityTable` (probabilité exacte de coup nul et espérance selon nb de dés restants)
    - Interface `BotPolicy` et implémentation `HeuristicBotPolicy` (3 profils : *Safe Runner*, *Cyber Balanced*, *Overclock Kamikaze*)
    - Intégration transparente des bots dans `SetupScreen` et `GameViewModel` (avec délai lisible pour voir les choix du bot)
- [ ] Création de `UserPreferencesRepository` (DataStore) et `CustomizationScreen` :
    - 5 presets cyberpunk/pixel (*Neon Night*, *Terminal Phosphor*, *CRT Amber*, *Pocket Mono*, *OLED Minimal*)
    - Mode `CUSTOM` avec `ColorPickerDialog` (roue HSV + `#RRGGBB`) et contrôle de contraste WCAG
    - Choix du style de dés (*Pixel Solid*, *Neon Wireframe*, *Dot Matrix*), scanlines CRT, mode droitier/gaucher, haptique, secousse pour lancer et `keepScreenOn`
- [ ] Création de `StatsScreen` (statistiques locales + bouton d'export dataset IA)
- [ ] Tests unitaires JVM sur `HeuristicBotPolicy`, `RollProbabilityTable` et tests instrumentés Room (`DecisionLogDao`, `MatchDao`)

---

## Lot 3 : Identité Visuelle Finale, Règles Interactives, i18n (4 Langues) & Préparation Play Store
- [ ] Intégration finale de l'icône XML adaptative (`mipmap-anydpi/ic_launcher*.xml`) et du Splash Screen Android 12+ (`installSplashScreen()` dans `MainActivity`)
- [ ] Intégration de la police pixel légère (titres/compteurs) couplée à la monospace (textes/menus)
- [ ] Création de `RulesScreen` (guide clair et interactif des règles du 5000 + simulateur rapide du barème selon les variantes actives)
- [ ] Création de `InfoScreen` (À propos, version, politique de confidentialité, crédits, lien Play Store, PayPal)
- [ ] Traduction complète des ressources en 4 langues : `values/` (FR), `values-en/` (EN), `values-es/` (ES), `values-pt/` (PT)
- [ ] Audit Edge-to-Edge, UI adaptative (portrait/paysage/tablette), performance Compose (`Canvas` recompositions) et vérification build Release R8 signé

---

## Lot 4 : Intégration Google Play Jeux (PGS v2) & Succès
- [ ] Ajout de `com.google.android.gms:play-services-games-v2` dans `libs.versions.toml`
- [ ] Implémentation de `PlayGamesService` et `GooglePlayGamesService` :
    - Authentification silencieuse au lancement (`PlayGamesSdk.initialize`)
    - Bouton connexion/profil optionnel sur `HomeScreen` et `StatsScreen` (aucun blocage hors-ligne)
- [ ] Déclenchement et miroir local des 10 succès initiaux :
    1. *First Boot* — Réussir sa première ouverture
    2. *Triple Ace Protocol* — Faire un brelan d'As en un lancer
    3. *Full Overclock* — Enchaîner 2 mains pleines dans le même tour
    4. *System Crash* — Perdre un tour contenant au moins 1 000 points
    5. *Pixel Perfect 5000* — Gagner une partie avec le score exact de 5 000
    6. *Straight Runner* — Réussir une suite en un lancer
    7. *Hijacker* — Gagner un tour grâce au repiquage
    8. *Headshot* — Écraser le score d'un adversaire (variante Chapeau)
    9. *Beat the Machine* — Battre l'IA *Overclock Kamikaze* en 1v1
    10. *Entropy Master* — Jouer 50 parties complètes
- [ ] Tests unitaires de l'évaluateur de succès (`AchievementEvaluator`) sur JVM

---

## Lot 5 : Multijoueur Local sans Internet (Google Nearby Connections)
- [ ] Ajout de `com.google.android.gms:play-services-nearby` dans `libs.versions.toml`
- [ ] Définition de l'interface commune `MultiplayerTransport` et des messages sérialisés (`kotlinx.serialization`)
- [ ] Implémentation de `NearbyTransport` en topologie étoile (`Strategy.P2P_STAR`) :
    - Mode Hôte (advertise du salon + arbitrage via `FiveThousandEngine` + diffusion de `MatchSnapshot`)
    - Mode Client (discovery + connexion par nom de salon + envoi des actions et de la trace d'entropie)
- [ ] Gestion pédagogique des permissions Android 10–16 (`BLUETOOTH_*`, `NEARBY_WIFI_DEVICES`, `ACCESS_FINE_LOCATION` API 29–30) uniquement à l'entrée du mode local
- [ ] Écran `LobbyScreen` (création/recherche de salon local, liste des joueurs prêts, aperçu des variantes choisies par l'hôte)
- [ ] Gestion de la reconnexion courte et passage en mode contrôle local/bot en cas de départ d'un joueur

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

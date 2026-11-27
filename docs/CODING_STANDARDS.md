# CODING_STANDARDS.md — Standards de Code & Best Practices

> Ce document définit les conventions de code, les idiomes Kotlin/Compose et les bonnes pratiques obligatoires pour garantir un code propre, léger, performant et testable sur **Neon 5K**.

---

## 1. Conventions Kotlin & Architecture Pure

- **Langue du code** :
    - Identifiants (classes, fonctions, propriétés, colonnes SQL, clés JSON) en **anglais** clair et concis (`GameRulesConfig`, `DiceScorer`, `EntropySnapshot`, `DecisionLogEntity`).
    - Documentation KDoc et commentaires explicatifs en **français**.
- **Pureté absolue de `domain/`** :
    - Aucun import `android.*`, `androidx.*` ou `com.google.*` dans `domain/` (à l'exception éventuelle de `@Immutable` si mappé en couche UI, ou privilégier des `data class` immuables pures Kotlin).
    - Toute logique du 5000 (`DiceScorer`, `FiveThousandEngine`, `HeuristicBotPolicy`, `EntropySeedGenerator`) doit tourner en quelques millisecondes sous `./gradlew :app:testDebugUnitTest`.
- **Immuabilité & Null-safety** :
    - Toujours privilégier `val` à `var` et des collections immuables (`List`, `Set`, `Map`) dans les états exposés à l'UI.
    - Proscrire `!!`. Utiliser `?:`, `let`, `require(...)` ou des retours anticipés.
- **Injection des sources non-déterministes** :
    - Injecter `Clock`, `EntropyProvider` et `CoroutineDispatcher` dans les constructeurs (avec valeurs par défaut) afin de rendre les tirages de dés, animations et tests d'IA 100 % reproductibles.
- **Gestion des erreurs** :
    - Utiliser des `sealed interface` explicites pour les erreurs métier (`MoveValidationError`), d'export SAF et de transport réseau (`MultiplayerError`).
    - Ne jamais avaler une `CancellationException`.

---

## 2. Coroutines, Capteurs & Threading (Zéro ANR)

- **Main Thread strictement réservé à l'UI** :
    - Requêtes Room (`MatchDao`, `DecisionLogDao`), écritures DataStore, hachage/export SAF JSONL/CSV, calculs d'IA et sérialisation réseau doivent s'exécuter sur `Dispatchers.IO` ou `Dispatchers.Default`.
- **Gestion propre de `SensorManager`** :
    - Enregistrer les listeners (`TYPE_GYROSCOPE`, `TYPE_ACCELEROMETER`) **uniquement pendant que `GameScreen` est actif (`ON_RESUME` / `ON_PAUSE`)** via `LifecycleEventEffect` ou un `callbackFlow` lié au cycle de vie. Ne jamais laisser un capteur actif en arrière-plan (drain batterie interdit).
    - Utiliser `SensorManager.SENSOR_DELAY_UI` (suffisant pour l'entropie et la détection de secousse sans saturer le CPU).
    - Toujours gérer gracieusement `sensorManager.getDefaultSensor(...) == null` : un appareil sans gyroscope continue de fonctionner sans erreur.

---

## 3. Jetpack Compose, Pixel Art & Performance UI

- **State Hoisting strict** :
    - Séparer `XxxRoute(viewModel)` (qui collecte `collectAsStateWithLifecycle()`) et `XxxScreen(uiState, onEvent, modifier)` (stateless et testable).
- **Rendu Pixel Art sur `Canvas` sans flou ni surcharge** :
    - Ne pas allouer d'objets `Paint`, `Path` ou de matrices à l'intérieur du bloc `Canvas { ... }` : définir les motifs `16×16` des faces `1..6` dans des tableaux constants immuables (`IntArray` / `BooleanArray` en `object PixelDiceSprites`).
    - Arrondir la taille d'un « pixel logique » à l'entier inférieur (`val px = floor(size.minDimension / 16f)`) et centrer la grille afin d'éviter les artefacts sub-pixels.
    - Limiter les effets néon (halo) à 1 ou 2 rectangles semi-transparents concentriques sur `Canvas` plutôt que des filtres de flou GPU coûteux (`BlurMaskFilter`) qui dégradent le framerate sur les appareils milieu de gamme.
- **Animations fluides et sobres** :
    - Animer les dés lors d'un lancer avec des `Animatable` / `animateFloatAsState` (durée courte `250ms–400ms`, respectant une option « Réduire les animations » si activée). **Seuls les dés non verrouillés (`!die.isLocked`) tournent lors d'une relance** ; un dé qui vient d'être verrouillé ou était déjà verrouillé reste strictement immobile (`0°`).
    - Ne jamais bloquer les boutons trop longtemps : la boucle *Lancer → Sélectionner → Relancer/Banquer* doit rester nerveuse et agréable sur des parties de 10–15 minutes.
- **Ergonomie à une main (Zone du pouce), Stabilité du bloc Dés & Mode Paysage** :
    - Sur `GameScreen`, regrouper les panneaux de lecture seule (carrousel des joueurs centré automatiquement sur le joueur actif, bandeau de statut avec pilule du pot sur une seule ligne, historique en direct des lancers et paliers d'encoches `✕`) dans la moitié supérieure de l'écran, et ancrer la ligne de dés interactive ainsi que les boutons d'action (`LANCER`, `RELANCER`, `BANQUER`) dans la moitié inférieure.
    - **Zéro saut vertical des dés** : le bouton `TOUT PRENDRE` est logé dans la barre d'en-tête fixe du panneau de dés et la zone de contrôles sous les dés conserve une hauteur fixe (`112.dp` portrait, `96.dp` paysage) afin que les dés ne bougent jamais verticalement lorsqu'on les sélectionne.
    - **Mode Paysage (`maxWidth > maxHeight`)** : basculer automatiquement sur une disposition 2 colonnes (colonne gauche : carrousel joueurs + statut + historique des lancers/paliers ; colonne droite : plateau de dés + boutons d'action verrouillés) tenant à 100 % dans l'écran sans aucun défilement vertical.
    - **Harmonisation des boutons Retour** : utiliser systématiquement `icon = PixelIconId.BACK` sur tous les `PixelButton` de retour (`NeonSubScreenTopBar` et bouton `MENU` de `GameScreen`), sans flèche ASCII texte (`←`).
- **Edge-to-Edge, Contraste & `Keep Screen On`** :
    - Calculer `backgroundColor.luminance() > 0.5f` pour mettre à jour `WindowInsetsControllerCompat.isAppearanceLightStatusBars` et `isAppearanceLightNavigationBars` sur tous les thèmes (prédéfinis et custom).
    - Appliquer `Modifier.keepScreenOn()` sur `GameScreen` si la préférence utilisateur est active (pratique en Pass & Play posé sur la table).
    - Respecter `WindowInsets.safeDrawing` pour qu'aucun bouton d'action ne passe sous la barre de geste Android.

---

## 4. Persistance Room & Logging IA

- **Séparation Entité / Domaine** :
    - Les `@Entity` restent dans `data/local/entity/`, mappées par extensions `toDomain()` / `toEntity()`.
- **Écritures non-bloquantes du logger IA** :
    - L'insertion dans `decision_logs` se fait en asynchrone sur `Dispatchers.IO` après validation du coup ; une erreur d'écriture log ne doit jamais bloquer ou crasher une partie en cours.
- **Complétion rétroactive propre** :
    - En fin de tour : `updateTurnOutcome(matchUuid, turnIndex, finalPoints)` en une seule requête SQL `UPDATE`.
    - En fin de partie : `updateMatchOutcome(matchUuid, winnerActorIds)` en une seule requête SQL `UPDATE`.
- **Zéro permission de stockage** :
    - L'export du dataset IA (`.jsonl` / `.csv`) utilise exclusivement le Storage Access Framework (`ActivityResultContracts.CreateDocument`).

---

## 5. Services Google, Permissions & Build Release

- **Google Play Jeux v2 (Lot 4)** :
    - Encapsuler toute l'API derrière `PlayGamesService`. Si le joueur refuse la connexion ou est hors-ligne, l'application fonctionne sans popup insistante et conserve un miroir local des succès débloqués dans Room/DataStore (synchronisés à la reconnexion suivante).
- **Nearby Connections (Lot 5)** :
    - Ne demander les permissions de proximité (`BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `NEARBY_WIFI_DEVICES`, ou `ACCESS_FINE_LOCATION` sur API 29–30) **qu'au moment exact où l'utilisateur ouvre le mode Multijoueur Local**, avec écran explicatif clair avant la boîte système.
    - Toujours appeler `stopAdvertising()`, `stopDiscovery()` et `stopAllEndpoints()` dans `onCleared()` / sortie de salon.
- **Firebase Realtime Database (Lot 6)** :
    - Isoler dans `FirebaseRtdbTransport`. Configurer des règles de sécurité Firebase strictes (lecture/écriture uniquement sur les salons valides, validation de schéma de taille pour empêcher le spam, et quota d'écriture minimal).
    - Ne jamais embarquer de secrets serveur dans le code client.
- **Configuration R8 en Release** :
    - Activer `isMinifyEnabled = true` et `isShrinkResources = true` en Release avec optimisation R8 globale (**jamais de `packageScope` partiel**), et ajouter les règles ProGuard nécessaires pour Room, `kotlinx.serialization`, Play Games et Firebase.

# DECISIONS.md — Journal des Décisions Techniques (ADR)

> Ce fichier consigne les choix d'architecture, de gameplay et d'infrastructure arbitrés pour **Neon 5K** afin de garder un cap clair sur les 6 lots.

---

## ADR-001 : Identité du projet — « Neon 5K » (`fr.antoinehory.neon5k`)
- **Date** : 2026-11-26
- **Contexte** : Repartir d'une base saine et moderne pour le jeu de dés du 5000 sans confusion avec d'anciens prototypes, avec une esthétique **minimaliste, futuriste, cyberpunk et pixel art**, et un nom lisible à l'international (FR/EN/ES/PT).
- **Décision** : Nommer l'application **Neon 5K** avec le namespace `fr.antoinehory.neon5k`.
- **Conséquences** : Identité visuelle forte, courte (7 caractères / 30 sur Google Play) et cohérente entre l'icône XML, le Splash Screen et le plateau.

## ADR-002 : Documentation modulaire orientée IA (Workflow AntiGravity)
- **Date** : 2026-11-26
- **Contexte** : Répliquer le workflow éprouvé de *Mon Simple Journal* pour limiter la consommation de tokens, guider AntiGravity dans Android Studio et éviter les régressions.
- **Décision** : Maintenir `AGENTS.md` à la racine comme point d'entrée unique et découper la connaissance en `docs/ARCHITECTURE.md`, `docs/CODING_STANDARDS.md`, `docs/DECISIONS.md` et `docs/ROADMAP.md`, plus les fichiers de distribution (`README.md`, `CHANGELOG.md`, `PLAY_STORE.md`, `POLITIQUE_DE_CONFIDENTIALITE.md`).
- **Conséquences** : Chaque session de dev cible uniquement le lot et les fichiers concernés.

## ADR-003 : Moteur du 5000 pur Kotlin et 100 % paramétrable (`GameRulesConfig`)
- **Date** : 2026-11-26
- **Contexte** : Les règles du 5000 sur Wikipédia comportent de nombreuses variantes familiales (5 ou 6 dés, seuil d'ouverture 750/1000, score exact, main pleine, suite, 3 paires, carré/quinte, repiquage, chapeau/écrasement, 3 coups nuls).
- **Décision** : Modéliser toutes ces règles dans une `data class GameRulesConfig` immuable passée au moteur pur `FiveThousandEngine` (`domain/rules/`), avec 3 préréglages rapides (*Classique 5 dés*, *Variante 6 dés*, *Rapide 2 500*) et un mode *Personnalisé* à la création de partie.
- **Conséquences** : Zéro règle codée en dur dans l'UI, testabilité unitaire exhaustive sur JVM, et partage natif de la config en multijoueur (l'hôte impose le `GameRulesConfig` du salon).

## ADR-004 : Aléatoire hybride par entropie multi-capteurs + hachage `SHA-256`
- **Date** : 2026-11-26
- **Contexte** : L'utilisateur souhaite une gestion ludique et originale de l'aléatoire combinant gyroscope, horodatage haute précision et autres signaux matériels, sans introduire de biais statistique ni planter sur des appareils sans gyroscope.
- **Décision** : Collecter gyroscope, accéléromètre, `currentTimeMillis`, `nanoTime`, `elapsedRealtimeNanos`, coordonnées/durée d'appui tactile et un sel `SecureRandom`, puis hacher l'ensemble en `SHA-256` pour initialiser un `kotlin.random.Random(seed)` tirant uniformément sur `1..6`. Exposer la trace d'entropie dans un badge cyberpunk optionnel.
- **Conséquences** : Tirages mathématiquement équitables (aucun biais de capteur), expérience amusante (secousse + inspecteur de graine), et fallback transparent sur émulateur ou téléphone sans gyroscope.

## ADR-005 : Rendu Pixel Art / Cyberpunk natif Compose `Canvas` (Zéro librairie lourde)
- **Date** : 2026-11-26
- **Contexte** : Préserver un APK léger, rapide et maintenable sans embarquer de moteur de jeu ou de librairie graphique lourde.
- **Décision** : Dessiner les 6 faces des dés et éléments pixel art sur une grille `16×16` directement en Compose `Canvas` (rectangles sur coordonnées entières), et concevoir l'icône adaptative + Splash Screen (`core-splashscreen`) en `VectorDrawable` XML pur.
- **Conséquences** : Poids APK minimal, rendu net sur toutes résolutions, et changement instantané de couleurs/styles de dés sans regénérer de bitmaps.

## ADR-006 : Personnalisation complète de la fenêtre de jeu avec contrôle WCAG
- **Date** : 2026-11-26
- **Contexte** : Les joueurs doivent pouvoir styliser librement leur table de jeu tout en conservant une interface minimaliste et lisible.
- **Décision** : Combiner 5 presets cyberpunk/pixel (*Neon Night*, *Terminal Phosphor*, *CRT Amber*, *Pocket Mono*, *OLED Minimal*), un mode `CUSTOM` (roue HSV + `#RRGGBB` avec calcul de contraste WCAG), 3 styles de dés (*Pixel Solid*, *Neon Wireframe*, *Dot Matrix*) et des réglages ergonomiques (scanlines, mode droitier/gaucher, haptique, secousse, `keepScreenOn`) stockés dans DataStore.
- **Conséquences** : Personnalisation poussée sans compromettre la lisibilité ni le contraste des barres système Android.

## ADR-007 : Journalisation locale des décisions (`decision_logs`) & architecture IA évolutive
- **Date** : 2026-11-26
- **Contexte** : Enregistrer les données des parties des joueurs pour pouvoir entraîner ultérieurement des IA performantes au 5000, sans alourdir l'app ni saturer les quotas cloud.
- **Décision** :
    1. Stocker localement dans Room (`DecisionLogEntity`) chaque paire `(état compact, action, issue)` taguée avec un `rulesFingerprint`.
    2. Permettre l'export local en `.jsonl` / `.csv` via SAF dès le Lot 2.
    3. Abstraire les bots derrière `BotPolicy` : heuristique probabiliste en Kotlin pur dès le Lot 2, avec possibilité future de charger des tables de politiques ou petits poids entraînés sans dépendre de TensorFlow Lite.
- **Conséquences** : Dataset propre exploitable immédiatement en Python, empreinte APK minimale et conformité RGPD (données 100 % anonymes, upload distant strictement opt-in au Lot 6).

## ADR-008 : Découpage incrémental en 6 lots avec priorité au jeu hors-ligne et local
- **Date** : 2026-11-26
- **Contexte** : Éviter l'effet tunnel d'un développement réseau trop précoce et garantir une application publiable et solide avant d'ajouter les couches réseau.
- **Décision** : Séquencer le développement en 6 lots :
    - Lots 1 à 3 : Moteur, Entropie, Solo/Pass & Play, IA locale, Room, Personnalisation, Assets XML & i18n/Play Store.
    - Lot 4 : Google Play Jeux v2 (succès).
    - Lot 5 : Multijoueur local sans Internet via **Google Nearby Connections** (`P2P_STAR`, préféré au Bluetooth RFCOMM brut pour sa fiabilité et sa gestion hybride Wi-Fi Direct/BLE).
    - Lot 6 (Optionnel) : Multijoueur en ligne et sync dataset IA sur **Firebase Realtime Database** (plan Spark 0 €).
- **Conséquences** : Livraison rapide d'une V1 complète et jouable, puis ajout modulaire des transports multijoueur sans refactoriser le moteur ni l'UI.

## ADR-009 : Équité en ligne pragmatique (Tirage client sur plan Spark 0 €)
- **Date** : 2026-11-26
- **Contexte** : Faire valider ou générer chaque lancer par une Cloud Function impose le plan Firebase Blaze (facturation activée), alors que le projet vise un fonctionnement garanti à 0 €.
- **Décision** : Conserver le tirage client par entropie capteurs en mode en ligne (Lot 6), en transmettant `rolledDice`, `entropyHash` et `rulesFingerprint` dans l'action Firebase. Les règles de sécurité RTDB valident la cohérence structurelle (tour actif, taille des données).
- **Conséquences** : Coût 0 € garanti sur le plan Spark, cohérence totale avec la mécanique fun des capteurs, adapté à un jeu convivial entre amis et salons privés.

## ADR-010 : Optimisation R8 globale sans `packageScope` partiel
- **Date** : 2026-11-26
- **Contexte** : Retour d'expérience critique d'un crash Release sur ART (`IllegalAccessError` sur `kotlin.sequences.*`) provoqué par un `packageScope` R8 partiel sur AGP 9.
- **Décision** : Utiliser `optimization { enable = true }` global en build Release sans restriction `packageScope`, et tester systématiquement un APK/AAB Release signé avant chaque publication.
- **Conséquences** : Build Release stable sur ART et taille d'APK optimisée.

## ADR-011 : Succès à comptage infini post-déverrouillage & Télémétrie Fun
- **Date** : 2026-11-26
- **Contexte** : Sur Google Play Jeux, un succès une fois déverrouillé n'évolue plus. Or dans un jeu de dés comme le 5000, les joueurs adorent savoir combien de fois ils ont réalisé un exploit au total (ex: nombre total de Brelans d'As, de Mains pleines, de Chapeaux ou de crashs à +1 000 pts) ainsi que consulter des statistiques décalées sur leurs lancers.
- **Décision** :
    1. Étendre la liste à **20 succès** couvrant toutes les combinaisons, prises de risque et variantes.
    2. Dans Room (`AchievementEntity`), dissocier l'état de déblocage (`isUnlocked: Boolean`, `unlockedAtEpochMs: Long?`, synchronisé avec Play Jeux) du compteur à vie (`lifetimeCount: Int`) qui **continue de s'incrémenter à chaque occurrence même après déverrouillage**, avec des paliers de maîtrise locaux (*Bronze → Argent → Or → Cyber-Platine*).
    3. Enrichir `PlayerStatsEntity` avec la télémétrie complète : compteur de chaque face `1..6` tirée (vérification de l'équité du SHA-256), détail des brelans par face (`1` à `6`), carrés, quintes, suites, 3 paires, fulls, lancers par secousse, points totaux banqués vs perdus en glitch, et bilan par profil de Bot.
- **Conséquences** : Forte rejouabilité locale même après avoir débloqué un succès Play Jeux, et compatibilité 100 % avec l'API `AchievementsClient` (`unlock` / `setSteps`) de Google Play Games Services v2.

## ADR-012 : Alignement de `InfoScreen` sur le standard `OCRAurebesh` & Protocole de suggestion pré-Lot
- **Date** : 2026-11-26
- **Contexte** : Harmoniser la fiche Développeur / À propos avec l'application sœur `OCRAurebesh` et valider les idées d'amélioration avant de coder chaque nouveau lot.
- **Décision** :
    1. Intégrer dans `Neon 5K` la même fiche `InfoScreen` que `OCRAurebesh` (Antoine Crock HORY, `antoinehory.fr`, `contact@antoinehory.fr`, `paypal.me/kuroku`, boutons PayPal et notation 5 ⭐ Play Store, et les 6 icônes vectorielles XML LinkedIn, Behance, Instagram, Facebook, Spotify, Steam) stylisée aux couleurs néon du thème actif.
    2. Avant d'entamer le code de chaque nouveau Lot (`Lot N`), l'agent présente systématiquement 3 à 5 suggestions d'améliorations ciblées pour ce lot afin que l'utilisateur puisse retenir celles qu'il souhaite inclure.
- **Conséquences** : Identité développeur uniforme sur le Play Store et pilotage collaboratif lot par lot.

## ADR-013 : Pile de Paliers de Score (`ScoreCheckpoint`) & Variante des 3 Encoches (`✕` / Strikes) avec Mémoire
- **Date** : 2026-11-26
- **Contexte** : Sur feuille de score papier, une variante populaire entre amis consiste à ne pas retirer un forfait de points fixe après 3 coups nuls consécutifs, mais à **reculer au palier de score précédent**, tout en **conservant en mémoire les encoches (`✕`) déjà notées sur cette ligne** (ex. `1000` → `1200` avec 2 encoches `[✕✕·]` → `1700` avec 3 encoches ⇒ retombe à `1200 [✕✕·]` → 1 seul nouveau coup nul suffit à atteindre 3 encoches sur `1200` et fait retomber à `1000`).
- **Décision** :
    1. Adopter la terminologie officielle **« Palier »** (`ScoreCheckpoint`) et **« Encoches (`✕` / Strikes) »** (affichées visuellement `[✕✕·]` sur les cartes joueurs de `GameScreen`).
    2. Stocker dans `PlayerState` une pile immuable `checkpoints: List<ScoreCheckpoint>` où chaque `ScoreCheckpoint(val score: Int, val strikes: Int = 0)` mémorise le score du palier et ses encoches (`0..2`).
    3. Remplacer `threeStrikesPenalty: Int` par `ThreeStrikesMode` offrant 5 modes dans `SetupScreen` : `DISABLED`, `MINUS_500`, `MINUS_1000`, `REVERT_TO_PREVIOUS_CLEAR_STRIKES` (recul au palier précédent avec remise à `0 ✕`) et `REVERT_TO_PREVIOUS_PERSIST_STRIKES` (recul au palier précédent avec conservation des encoches historiques `✕`).
- **Conséquences** : Reproduction 100 % fidèle du comportement d'une feuille de match papier, également exploitée par la variante Chapeau (`BumpOpponentMode.RESET_TO_PREVIOUS`) et persistée en JSON dans `SavedMatchEntity`.

## ADR-014 : Auto-Apprentissage Local de l'IA (`AdaptiveBotLearner`) & Nouvelles Variantes Classiques du 5000
- **Date** : 2026-11-27
- **Contexte** :
    1. Exploiter directement sur l'appareil les milliers de décisions enregistrées dans la table Room `decision_logs` pour que les bots locaux (*Safe Runner*, *Cyber Balanced*, *Overclock Kamikaze*) s'améliorent tout seuls au fil des parties sans embarquer de librairie ML lourde.
    2. Prendre en charge les variantes classiques de table : interdiction de banquer un score non multiple de 100 (`5` isolé interdit en banque seule), seuil minimal par tour après ouverture (`200/250/500`), sprint final des `1 000` derniers points en un seul tour, découplage entre Quinte (`×4 brelan` / `5×Cinq = 5 000`) et Victoire instantanée sur `5×As`, et distinction en mode 6 dés entre Suite à 5 dés (`1-5` / `2-6`) et Grande Suite à 6 dés (`1-2-3-4-5-6`).
- **Décision** :
    1. Créer `AdaptiveBotLearner` (`domain/ai/AdaptiveBotLearner.kt`) en Kotlin pur : un apprenant par renforcement tabulaire contextuel (`(remainingDice, potBucket, isOpened)`) avec régularisation bayésienne empirique (`PRIOR_WEIGHT = 10.0`) qui pondère les trajectoires issues de parties gagnées et hybride en temps réel l'espérance empirique multi-lancers observée avec l'espérance théorique à 1 coup de `RollProbabilityTable`.
    2. Étendre `GameRulesConfig`, `DiceScorer` et `FiveThousandEngine` avec `requireMultipleOf100ToBank`, `minTurnScoreAfterOpening`, `finalSprintLast1000InOneTurn`, `instantWinOnFiveOnes`, `FiveOfAKindMode.FIVE_FIVES_5000` et `fullStraight6DiceScore`, tout en préservant la compatibilité du schéma Room `version = 2` (puis migration explicite `MIGRATION_2_3` en `version = 3`).
- **Conséquences** : Une IA locale vivante qui progresse en < 2 ms de RAM sans dépendance externe, et une couverture exhaustive des règles officielles et familiales du 5000.

## ADR-015 : Lot intermédiaire Pré-Lot 5 (Audio PCM 8-bit, Contrôle Bots & Qualité) avant le Multijoueur Local
- **Date** : 2026-11-27
- **Contexte** : Avant d'introduire la couche réseau Nearby Connections (Lot 5), le recettage en conditions réelles et l'audit produit ont mis en évidence le besoin de peaufiner l'expérience solo/bots (pause & vitesse de jeu des bots, nommage complet, synthèse sonore 8-bit légère, lecture dynamique de version et inspecteur IA).
- **Décision** :
    1. Ajouter un **Lot 4.5 (Pré-Lot 5)** dédié au polish solo/ergonomie avant de démarrer le Lot 5.
    2. Pour les effets sonores rétro (Lot 4.5), privilégier une synthèse PCM 8-bit générée en Kotlin (`AudioTrack`) plutôt que d'embarquer des fichiers audio compressés, afin de respecter l'ADR-005 (zéro dépendance lourde et APK minimal).
- **Conséquences** : Socle solo/local irréprochable avant d'aborder la complexité temps réel du multijoueur Nearby.



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

# 📋 Changelog — Neon 5K

Toutes les modifications notables apportées à ce projet sont documentées dans ce fichier.
Le format est basé sur [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/).

---

## [Unreleased]

### 🛠️ Recettage Play Store, Bots & Easter Eggs (Pré-Lot 5)
- **Identifiants Play Jeux v2 & Fiabilisation PGS (`Neon5KApplication`, `GooglePlayGamesService`)** : ajout de `games-ids.xml` (App ID `59639547526` et les 20 IDs de succès), initialisation officielle `PlayGamesSdk.initialize()` dans `Neon5KApplication.onCreate()`, synchronisation automatique des succès locaux dès l'authentification Play Jeux et remontée des codes `ApiException` dans le diagnostic, ainsi que migration Room non-destructive `v2 -> v3` (`AppDatabase`).
- **Conformité Play Console (`EdgeToEdge`, `androidx.fragment`)** : contrainte Gradle vers `androidx.fragment:fragment:1.9.1` (remplace `1.1.0` tiré transitivement), appel `enableEdgeToEdge()` avant `super.onCreate()`, `decorFitsSystemWindows = false` + `safeDrawing` sur le plein écran `StarWars42EasterEggDialog` et conservation R8 de `EdgeToEdge.enable*` via `app/proguard-rules.pro`.
- **Pause & Pas-à-Pas sur les tours de Bots (`GameViewModel`, `GameScreen`)** : bouton rapide `⏸` / `▶` permettant de suspendre une partie IA (y compris 100 % Bots) pour inspecter chaque lancer ou avancer coup par coup, avec mise en pause automatique lorsque l'écran passe en arrière-plan.
- **Nommage personnalisé des joueurs & bots (`SetupScreen`)** : édition libre du nom de chaque participant (humain comme IA) avec conservation du nom personnalisé lors du changement de profil de bot.
- **Historique multi-joueurs lisible (`StatsScreen`)** : remplacement du résumé en ligne tassé par un `FlowRow` responsive de badges de scores (de 3 à 8 joueurs).
- **Correctifs Easter Eggs (`EasterEggOverlay.kt`)** :
  - Générique Star Wars `42` démarrant depuis tout en bas de l'écran dès `p > 0` (`TransformOrigin(0.5f, 0f)` + hauteur réelle du conteneur), avec fondu doux (`fade out`) de la phrase d'intro cyan à l'approche du `42` jaune.
  - Délai de sécurité anti-fermeture accidentelle (`900 ms`) sur les 3 Easter Eggs (`StarWars42`, `Matrix1337`, `HoloAurebesh`) pour ignorer les clics rapides consécutifs à l'ouverture.

---

## [1.0.3] - Lot 4 & Recettage Plateau / IA / Graphiques
- **Nouvelles variantes paramétrables du 5000 (`GameRulesConfig`, `DiceScorer`, `FiveThousandEngine`)** :
  - **Chiffre rond obligatoire pour banquer (`requireMultipleOf100ToBank`)** : impose un multiple de `100` pts pour banquer un tour (interdit de banquer un `5` isolé ou un total finissant par `50`).
  - **Seuil minimal par tour après ouverture (`minTurnScoreAfterOpening`)** : configurable à `0` (libre), `200`, `250` ou `500` pts.
  - **Sprint final des 1 000 derniers points (`finalSprintLast1000InOneTurn`)** : une fois à moins de `1 000` pts de la cible (ex. `>= 4 000` pts), le joueur doit boucler la partie en un seul tour.
  - **Découplage Quinte (`FiveOfAKindMode`) & Victoire instantanée sur 5 As (`instantWinOnFiveOnes`)** : possibilité de cumuler `×4 brelan` (ou `5×Cinq = 5 000 pts`) avec l'option indépendante de victoire immédiate sur `5×As`.
  - **Grande Suite à 6 dés (`fullStraight6DiceScore`) séparée de la Suite à 5 dés (`straightScore`)** : barèmes configurables indépendamment en mode 6 dés (`1-2-3-4-5-6` à `1 500`, `2 000`, `2 500` ou `3 000` pts).
  - **Pile de Paliers (`ScoreCheckpoint`) & 3 Encoches (`✕` / Strikes)** : recul au palier précédent avec ou sans conservation des encoches historiques (`REVERT_TO_PREVIOUS_PERSIST_STRIKES` / `CLEAR_STRIKES`).
- **Auto-Apprentissage Local de l'IA (`AdaptiveBotLearner`)** :
  - Les bots locaux (*Safe Runner*, *Cyber Balanced*, *Overclock Kamikaze*) s'améliorent désormais automatiquement à partir du dataset SQLite `decision_logs` par apprentissage par renforcement tabulaire à régularisation bayésienne hybridé avec la table exacte $6^N$.
- **Refonte Ergonomique du Plateau (`GameScreen`) en Portrait & Paysage** :
  - Carrousel horizontal des joueurs (`LazyRow`) avec centrage animé automatique sur le joueur actif (idéal de 3 à 8 joueurs).
  - Bandeau de statut du tour sur une seule ligne avec pilule dédiée pour le pot dynamique (`+XXX PTS`).
  - Panneau central en direct et modale dédiée **Historique des Lancers & Paliers d'Encoches (`✕`)**.
  - Hauteur verrouillée du bloc d'actions sous les dés et bouton `TOUT PRENDRE` intégré à l'en-tête du plateau de dés pour empêcher tout saut vertical des dés.
  - Mode Paysage 2 colonnes optimisé sans aucun défilement vertical.
- **Graphiques Visuels & Journal des Parties (`StatsScreen`)** :
  - Nouvel onglet **PARTIES** listant toutes les parties terminées avec classement, historique des paliers d'encoches (`✕`) et journal dépliable lancer par lancer.
  - Ajout de graphiques vectoriels Compose `Canvas` : camemberts/donuts (*Victoires / Défaites*, *Points Banqués / Glitchés*), courbe d'évolution des scores finaux et graphiques en barres verticales (*Distribution des faces 1..6* avec cible théorique `16,7 %` et *Brelans par face*).
- **Personnalisation, Accessibilité & Easter Eggs** :
  - 14 presets de thèmes (Cyberpunk, Niveaux de Gris, Sépia, Daltonisme Okabe-Ito/Tritanopie/Achromatopsie) + générateurs *Random Harmonieux* et *Random Chaos* + sélecteur couleur hybride HSV/RGB.
  - Correction du rendu des points `2×2` du style de dés `NEON_WIREFRAME` (points unifiés sans effet « mini-fenêtre »).
  - Amélioration de l'Easter Egg Star Wars `42` (ciel étoilé pseudo-aléatoire sans motifs en lignes, intro cyan en fondu avant le défilement 3D accentué du `42` en dés jaunes).

---

## [1.0.0] - Lots 1 à 4 (Base Solo, Pass & Play, IA, Customisation & Play Jeux v2)
- Moteur complet du 5000 paramétrable (5 ou 6 dés + toutes les variantes classiques).
- Aléatoire hybride par capteurs (gyroscope, accéléromètre, nanosecondes, tactile) + inspecteur de graine `SHA-256`.
- Modes Solo contre 3 profils d'IA probabiliste et Pass & Play jusqu'à 8 joueurs sur un appareil.
- Sauvegarde automatique Room, statistiques locales, 20 succès à comptage infini (1 000 XP Google Play Jeux v2) et export dataset IA (`.jsonl` / `.csv`).
- Fiche développeur `InfoScreen` alignée sur `OCRAurebesh` et interface traduite en 4 langues (`fr`, `en`, `es`, `pt`).

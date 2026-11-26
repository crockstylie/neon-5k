# Guide Google Play Jeux v2 — Déclaration des 20 Succès & Icônes 512×512 (Neon 5K)

> Ce document explique pas à pas comment déclarer les **20 succès** de **Neon 5K** dans la **Google Play Console** et où récupérer leurs **20 icônes Pixel Art Cyberpunk (`512 × 512 px` PNG)** générées automatiquement par le projet.

---

## 1. Où se trouvent les 20 icônes PNG `512 × 512 px` ?

Les 20 icônes au format officiel exigé par Google Play (`512 × 512 px`, PNG 32-bit) sont générées automatiquement à partir des matrices Pixel Art `16×16` de [`AchievementDefinition.kt`](../app/src/main/java/fr/antoinehory/neon5k/domain/achievement/AchievementDefinition.kt) lors de l'exécution des tests unitaires (`./gradlew :app:testDebugUnitTest`).

Elles sont disponibles directement dans le dossier :
- `docs/play_console_achievements_icons/achievement_01_first_boot.png` jusqu'à `achievement_20_entropy_master.png`.

> **Astuce Google Play Console** : Tu n'as besoin d'uploader qu'**une seule icône en couleur** par succès (`512 × 512 px`). La Google Play Console génère automatiquement la version grisée pour l'état « verrouillé » dans l'application Google Play Jeux.

---

## 2. Étapes de configuration dans la Google Play Console

1. Ouvre la **Google Play Console** → sélectionne l'application **Neon 5K**.
2. Dans le menu latéral gauche, va dans **Croissance > Services de jeux Play > Configuration et gestion > Configuration**.
3. Choisis **Créer un projet de services de jeux Play** (si ce n'est pas encore fait) et associe ton empreinte de certificat **SHA-1** (Debug et Release/Upload key) dans **Identifiants**.
4. Copie l'**ID du projet** à 12 chiffres (ex. `123456789012`) et colle-le dans [`app/src/main/res/values/games-ids.xml`](../app/src/main/res/values/games-ids.xml) dans `<string name="game_services_project_id">`.
5. Va ensuite dans **Services de jeux Play > Configuration et gestion > Succès** et clique sur **Créer un succès** pour chacun des 20 succès du tableau ci-dessous.
6. Une fois les 20 succès créés, clique sur **Obtenir les ressources** en haut de la liste des succès dans la Play Console : copie les 20 chaînes `<string name="achievement_...">CgkI...</string>` dans [`app/src/main/res/values/games-ids.xml`](../app/src/main/res/values/games-ids.xml).

---

## 3. Tableau récapitulatif des 20 Succès (Total = 1 000 XP pile)

| # | Clé XML (`games-ids.xml`) | Nom (FR / EN) | Description (FR) | Type Play Console | Étapes | XP | Fichier Icône `512×512` |
| :-: | :--- | :--- | :--- | :---: | :-: | :-: | :--- |
| **01** | `achievement_first_boot` | **First Boot** | Franchir le seuil d'ouverture pour la première fois. | Standard | — | **25** | `achievement_01_first_boot.png` |
| **02** | `achievement_high_voltage_opening` | **High Voltage Opening** | Réussir son ouverture avec au moins 1 500 points en un seul tour. | Standard | — | **50** | `achievement_02_high_voltage_opening.png` |
| **03** | `achievement_triple_ace_protocol` | **Triple Ace Protocol** | Obtenir un Brelan d'As (1 000 pts) en un seul lancer. | Standard | — | **50** | `achievement_03_triple_ace_protocol.png` |
| **04** | `achievement_brelan_collector` | **Brelan Collector** | Réaliser au moins un brelan de chacune des 6 faces (1 à 6). | **Incrémentiel** | **6** | **50** | `achievement_04_brelan_collector.png` |
| **05** | `achievement_quad_core` | **Quad Core** | Obtenir un Carré (4 dés identiques) en un seul lancer. | Standard | — | **50** | `achievement_05_quad_core.png` |
| **06** | `achievement_five_of_a_kind` | **God Roll (Quinte)** | Obtenir une Quinte (5 dés identiques) en un seul lancer. | Standard | — | **75** | `achievement_06_five_of_a_kind.png` |
| **07** | `achievement_straight_runner` | **Straight Runner** | Réussir une Suite (1-2-3-4-5 ou 2-3-4-5-6) en un seul lancer. | Standard | — | **50** | `achievement_07_straight_runner.png` |
| **08** | `achievement_twin_matrix` | **Twin Matrix** | Obtenir 3 Paires distinctes en un lancer (variante 6 dés). | Standard | — | **50** | `achievement_08_twin_matrix.png` |
| **09** | `achievement_full_house_cyber` | **Full House Cyber** | Réussir un Full (Brelan + Paire) avec le bonus Full activé. | Standard | — | **50** | `achievement_09_full_house_cyber.png` |
| **10** | `achievement_hot_dice` | **Hot Dice!** | Réaliser une Main pleine en faisant scorer tous les dés. | Standard | — | **25** | `achievement_10_hot_dice.png` |
| **11** | `achievement_full_overclock` | **Full Overclock** | Enchaîner 2 Mains pleines au cours du même tour et banquer. | Standard | — | **75** | `achievement_11_full_overclock.png` |
| **12** | `achievement_one_die_miracle` | **One Die Miraculé** | Scorer un As ou un 5 en ne relançant qu'un seul dé restant. | Standard | — | **50** | `achievement_12_one_die_miracle.png` |
| **13** | `achievement_system_crash` | **System Crash** | Perdre un pot de tour d'au moins 1 000 points sur un Glitch. | Standard | — | **25** | `achievement_13_system_crash.png` |
| **14** | `achievement_three_strikes_out` | **Three Strikes Out (✕✕✕)** | Subir la pénalité ou le recul de palier après 3 Encoches (✕✕✕). | Standard | — | **25** | `achievement_14_three_strikes_out.png` |
| **15** | `achievement_pixel_perfect_5000` | **Pixel Perfect 5000** | Remporter une partie en tombant pile sur le score cible exact. | Standard | — | **50** | `achievement_15_pixel_perfect_5000.png` |
| **16** | `achievement_overkill_turn` | **Overkill Turn** | Banquer au moins 2 000 points en un seul tour. | Standard | — | **75** | `achievement_16_overkill_turn.png` |
| **17** | `achievement_hijacker` | **Hijacker (Repiquage)** | Reprendre le pot d'un adversaire en Repiquage et banquer le tour. | Standard | — | **50** | `achievement_17_hijacker.png` |
| **18** | `achievement_headshot` | **Headshot (Chapeau)** | Infliger un Chapeau en égalisant exactement le score d'un adversaire. | Standard | — | **50** | `achievement_18_headshot.png` |
| **19** | `achievement_beat_the_machine` | **Beat the Machine** | Remporter une partie face au bot Overclock Kamikaze. | Standard | — | **75** | `achievement_19_beat_the_machine.png` |
| **20** | `achievement_entropy_master` | **Entropy Master** | Terminer 50 parties complètes propulsées par l'entropie SHA-256. | **Incrémentiel** | **50** | **50** | `achievement_20_entropy_master.png` |

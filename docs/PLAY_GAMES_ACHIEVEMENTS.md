# Guide Google Play Jeux v2 — Import ZIP en 1 Clic des 20 Succès & Configuration (Neon 5K)

> Ce document explique pas à pas comment configurer **Google Play Jeux v2** dans la **Google Play Console** et importer **d'un seul coup** les **20 succès** (avec leurs 20 icônes PNG `512 × 512 px` et leurs traductions en **4 langues : Anglais, Français, Espagnol, Portugais**) grâce à l'archive `.zip` générée automatiquement par le projet.

---

## 1. L'archive ZIP « Import en 1 clic » (3 CSV officiels + 20 Icônes 512×512)

La Google Play Console propose un bouton **« Importer des réussites »** (*Import achievements*) qui exige une archive `.zip` contenant exactement **4 éléments à sa racine** (sans ligne d'en-tête dans les CSV et sans virgule à l'intérieur des textes) :
1. **[`AchievementsMetadata.csv`](file:///C:/Users/hory/AndroidStudioProjects/Neon5K/docs/play_store_assets/AchievementsMetadata.csv)** (`Name,Description,Incremental,Steps,Initial State,Points,Sort Order`) — informations des 20 réussites dans la langue par défaut du projet.
2. **[`AchievementsLocalizations.csv`](file:///C:/Users/hory/AndroidStudioProjects/Neon5K/docs/play_store_assets/AchievementsLocalizations.csv)** (`Name,Localized Name,Localized Description,Locale`) — traductions des noms et descriptions dans les autres langues (sans répéter la langue par défaut).
3. **[`AchievementsIconsMappings.csv`](file:///C:/Users/hory/AndroidStudioProjects/Neon5K/docs/play_store_assets/AchievementsIconsMappings.csv)** (`Name,Icon File`) — association entre le nom de chaque réussite et son fichier PNG.
4. **Les 20 icônes PNG (`512 × 512 px`)** : `achievement_01_first_boot.png` à `achievement_20_entropy_master.png`.

Selon les **langues activées dans tes Propriétés Services de jeux Play** (*Services de jeux Play > Configuration et gestion > Configuration > Propriétés > Gérer les traductions*), plusieurs archives `.zip` prêtes à l'emploi sont générées dans [`docs/play_store_assets/`](file:///C:/Users/hory/AndroidStudioProjects/Neon5K/docs/play_store_assets) :

> ⚠️ **Pourquoi l'erreur « Paramètres régionaux non pris en charge » apparaît-elle si on importe `AchievementsLocalizations.csv` directement ?**
> Les **Services de jeux Play** possèdent leur propre liste de langues activées (indépendante de la fiche du Play Store !). Par défaut, un nouveau projet Play Jeux n'a **qu'une seule langue activée** (sa langue par défaut). Si `AchievementsLocalizations.csv` contient `fr-FR`, `es-ES` ou `pt-PT` alors que ces langues n'ont pas encore été cochées dans **Services de jeux Play > Configuration et gestion > Configuration > Propriétés > Gérer les traductions**, la Play Console refuse le fichier.

- **Option 1 (Import immédiat sans configurer de langues supplémentaires dans Play Jeux — `AchievementsLocalizations.csv` vide)** :
  - Si ton projet Play Jeux est en **Français (`fr-FR`)** : **[`docs/play_store_assets/play_games_achievements_ONLY_FR.zip`](file:///C:/Users/hory/AndroidStudioProjects/Neon5K/docs/play_store_assets/play_games_achievements_ONLY_FR.zip)**
  - Si ton projet Play Jeux est en **Anglais (`en-US`)** : **[`docs/play_store_assets/play_games_achievements_ONLY_EN.zip`](file:///C:/Users/hory/AndroidStudioProjects/Neon5K/docs/play_store_assets/play_games_achievements_ONLY_EN.zip)**
- **Option 2 (Bilingue FR + EN — après avoir ajouté `en-US` ou `fr-FR` dans les Propriétés Play Jeux)** :
  - Défaut `fr-FR` + traduction `en-US` : **[`docs/play_store_assets/play_games_achievements_FR_plus_EN.zip`](file:///C:/Users/hory/AndroidStudioProjects/Neon5K/docs/play_store_assets/play_games_achievements_FR_plus_EN.zip)**
  - Défaut `en-US` + traduction `fr-FR` : **[`docs/play_store_assets/play_games_achievements_EN_plus_FR.zip`](file:///C:/Users/hory/AndroidStudioProjects/Neon5K/docs/play_store_assets/play_games_achievements_EN_plus_FR.zip)**
- **Option 3 (4 langues `en-US` + `fr-FR` + `es-ES` + `pt-PT` — après avoir activé ces 4 langues dans les Propriétés Play Jeux)** :
  - Défaut `en-US` : **[`docs/play_store_assets/play_games_achievements_default_EN.zip`](file:///C:/Users/hory/AndroidStudioProjects/Neon5K/docs/play_store_assets/play_games_achievements_default_EN.zip)**
  - Défaut `fr-FR` : **[`docs/play_store_assets/play_games_achievements_default_FR.zip`](file:///C:/Users/hory/AndroidStudioProjects/Neon5K/docs/play_store_assets/play_games_achievements_default_FR.zip)**

---

## 2. Visuels de la Fiche Google Play Store (Icône 512×512 & Bannière 1024×500)

Les deux visuels principaux exigés par la fiche Google Play Store ont également été générés dans [`docs/play_store_assets/`](file:///C:/Users/hory/AndroidStudioProjects/Neon5K/docs/play_store_assets) :
1. **Icône de l'application (`512 × 512 px` PNG 32-bit)** :
   - [`docs/play_store_assets/play_store_icon_512x512.png`](file:///C:/Users/hory/AndroidStudioProjects/Neon5K/docs/play_store_assets/play_store_icon_512x512.png)
2. **Image de présentation / Feature Graphic (`1 024 × 500 px` PNG 32-bit)** :
   - [`docs/play_store_assets/play_store_feature_graphic_1024x500.png`](file:///C:/Users/hory/AndroidStudioProjects/Neon5K/docs/play_store_assets/play_store_feature_graphic_1024x500.png)

---

## 3. Étapes de configuration Google Play Jeux v2 dans la Play Console

### Étape A — Créer le projet Services de jeux Play & Lier les certificats SHA-1
1. Ouvre la **Google Play Console** → sélectionne l'application **Neon 5K**.
2. Dans le menu de gauche, va dans **Croissance > Services de jeux Play > Configuration et gestion > Configuration**.
3. Sélectionne **« Non, mon jeu n'utilise pas les API Google »** → clique sur **Créer**.
4. Dans la section **Identifiants** (*Credentials*) :
   - Clique sur **Ajouter un identifiant** (*Add credential*) → Type **Android**.
   - Crée un identifiant OAuth pour ta **clé de signature de publication (`C:\android\KeyStorePass` / alias `OCRAurebesh`)** :
     - **SHA-1 Release** : `86:7A:8E:13:C3:F1:06:30:CA:F1:F5:46:E5:F9:28:61:D0:D5:85:AE`
   - Crée un second identifiant OAuth pour ta **clé Debug** (pour tester directement depuis Android Studio) :
     - **SHA-1 Debug** : `21:05:A9:54:6A:65:21:21:FE:86:42:2F:8D:46:D1:27:97:BD:B7:DF`
5. En haut de la page **Configuration**, copie l'**ID du projet** à 12 chiffres (ex. `123456789012`).

### Étape B — Importer les 20 réussites en 1 clic via le ZIP
1. Va dans **Services de jeux Play > Configuration et gestion > Réussites**.
2. Clique sur le bouton **Importer des réussites** (*Import achievements*).
3. Sélectionne **[`play_games_achievements_default_EN.zip`](file:///C:/Users/hory/AndroidStudioProjects/Neon5K/docs/play_store_assets/play_games_achievements_default_EN.zip)** (si la langue par défaut de ton projet Play Jeux est `en-US`) ou **[`play_games_achievements_default_FR.zip`](file:///C:/Users/hory/AndroidStudioProjects/Neon5K/docs/play_store_assets/play_games_achievements_default_FR.zip)** (si la langue par défaut de ton projet Play Jeux est `fr-FR`).
4. La Play Console crée automatiquement les **20 réussites** (total = **1 000 XP**), associe leurs **20 icônes Pixel Art `512 × 512 px`** via `AchievementsIconsMappings.csv` et importe les traductions via `AchievementsLocalizations.csv` !

### Étape C — Copier les IDs générés dans `games-ids.xml`
1. En haut de la page **Succès** dans la Play Console, clique sur **Obtenir les ressources** (*Get resources*).
2. Copie l'`app_id` (`game_services_project_id`) et les 20 identifiants `CgkI...` dans [`app/src/main/res/values/games-ids.xml`](file:///C:/Users/hory/AndroidStudioProjects/Neon5K/app/src/main/res/values/games-ids.xml).
3. Ajoute ton adresse Gmail dans **Services de jeux Play > Configuration et gestion > Testeurs** pour que les succès se débloquent immédiatement avant même la publication publique.

---

## 4. Astuce Google Play Console : Fiche Store Multilingue & Traduction Automatique depuis l'Anglais

Sur la fiche principale du Play Store (**Développer la présence > Présence sur le Play Store > Fiche Play Store principale**) :
1. **Définir l'Anglais (`en-US`) comme langue par défaut** de la fiche : copie-colle le titre, la description courte et la description complète de la section **🇬🇧 ENGLISH (`en-US`)** de [`PLAY_STORE.md`](file:///C:/Users/hory/AndroidStudioProjects/Neon5K/PLAY_STORE.md).
2. **Pour le Français (`fr-FR`), l'Espagnol (`es-ES`) et le Portugais (`pt-PT`)** : clique sur **Gérer les traductions > Gérer vos propres traductions** et copie-colle nos textes sur-mesure de [`PLAY_STORE.md`](file:///C:/Users/hory/AndroidStudioProjects/Neon5K/PLAY_STORE.md) (qui utilisent le vrai vocabulaire officiel du 5000 dans chaque langue : *Main pleine / Repiquage / Chapeau / Encoches*, *Hot Dice / Piggyback / Bump*, *Mano plena / Repóker*, *Mão cheia / Quina*).
3. **Pour toutes les autres langues du monde (Allemand, Italien, Japonais, Coréen, Polonais, etc.)** : dans **Croissance > Traductions > Traduire la fiche Play Store**, coche toutes les langues additionnelles gratuites proposées par Google Play et choisis **Traduire depuis l'anglais (`en-US`)**. Google traduira automatiquement ta fiche anglaise vers +40 langues gratuitement en quelques secondes.

---

## 5. Tableau récapitulatif des 20 Succès (Total = 1 000 XP pile)

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

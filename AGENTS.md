# AGENTS.md — Source de Vérité Principale (Neon 5K)

> **RÈGLE CRITIQUE POUR L'AGENT IA :**
> Ce fichier est ton point d'entrée obligatoire. Pour préserver ta fenêtre de contexte et garantir un code cohérent, **ne lis pas l'intégralité du codebase à l'aveugle**. Consulte d'abord cet index et uniquement le fichier de documentation ou le Skill correspondant à ta tâche.

---

## 1. Index des Sources de Vérité

| Fichier | Rôle | Quand le lire / mettre à jour ? |
| :--- | :--- | :--- |
| [`AGENTS.md`](./AGENTS.md) | Règles globales, stack technique, skills, garde-fous | Au début de chaque tâche |
| [`docs/ARCHITECTURE.md`](./docs/ARCHITECTURE.md) | Structure des packages, flux de données, moteur 5000, entropie, IA, réseau, index des fichiers | Avant de créer/modifier une classe ou l'architecture |
| [`docs/CODING_STANDARDS.md`](./docs/CODING_STANDARDS.md) | Standards Kotlin/Compose, Canvas pixel art, Room, capteurs, PGS v2, Nearby & Firebase | Avant d'écrire ou refactoriser du code |
| [`docs/ROADMAP.md`](./docs/ROADMAP.md) | Découpage en 6 lots, état d'avancement (fait / en cours / à faire) | Au début et à la fin de chaque fonctionnalité |
| [`docs/DECISIONS.md`](./docs/DECISIONS.md) | Décisions techniques (ADR), variantes du 5000, entropie, logging IA, multijoueur | Lors d'un choix technique ou d'un arbitrage |
| [`README.md`](./README.md) | Présentation générale du jeu, règles, fonctionnalités et commandes Gradle | Lors d'un changement majeur de fonctionnalité ou de build |
| [`CHANGELOG.md`](./CHANGELOG.md) | Historique des versions et nouveautés | À chaque livraison de version |
| [`PLAY_STORE.md`](./PLAY_STORE.md) | Textes Google Play Console traduits en 4 langues (`fr`, `en`, `es`, `pt`) + specs assets | Avant publication sur le Play Store |
| [`docs/play_store_assets/RELEASE_NOTES_PLAY_CONSOLE.txt`](./docs/play_store_assets/RELEASE_NOTES_PLAY_CONSOLE.txt) | Notes de version Google Play Console (14 langues, ≤ 500 car.) | À chaque montée de version ou livraison de Lot |
| [`POLITIQUE_DE_CONFIDENTIALITE.md`](./POLITIQUE_DE_CONFIDENTIALITE.md) | Politique de confidentialité officielle (capteurs, Play Jeux, Nearby, Firebase opt-in) | En cas d'évolution touchant aux données ou permissions |

---

## 2. Protocole d'Économie de Contexte

1. **Ciblage chirurgical** : Utilise la cartographie dans [`docs/ARCHITECTURE.md`](./docs/ARCHITECTURE.md) pour identifier directement le fichier concerné sans scanner tout le projet.
2. **Lectures partielles** : Utilise les outlines de fichiers (`view_file_outline`) ou des plages de lignes (`startLine`/`endLine`) sur les fichiers volumineux.
3. **Avancement strict par Lot & Suggestions pré-Lot** :
    - Ne démarre pas le Lot `N+1` tant que les tests unitaires et la checklist du Lot `N` dans [`docs/ROADMAP.md`](./docs/ROADMAP.md) ne sont pas validés.
    - **Avant de coder un nouveau Lot**, propose brièvement à l'utilisateur 3 à 5 suggestions d'améliorations pertinentes (ergonomie, fun, finition) pour ce lot afin qu'il valide le périmètre exact.
4. **Mise à jour continue** :
    - Dès qu'un nouveau fichier Kotlin est créé ou déplacé, mets à jour la cartographie dans [`docs/ARCHITECTURE.md`](./docs/ARCHITECTURE.md).
    - Dès qu'une étape est validée, coche-la dans [`docs/ROADMAP.md`](./docs/ROADMAP.md).
    - Dès qu'une décision structurante est prise, ajoute une entrée concise dans [`docs/DECISIONS.md`](./docs/DECISIONS.md).
    - À chaque livraison de Lot ou nouvelle version, mets à jour [`CHANGELOG.md`](./CHANGELOG.md) ainsi que [`docs/play_store_assets/RELEASE_NOTES_PLAY_CONSOLE.txt`](./docs/play_store_assets/RELEASE_NOTES_PLAY_CONSOLE.txt) pour les 14 langues Play Console (≤ 500 caractères par langue).

---

## 3. Skills Agentiques à Activer selon la Tâche

Avant d'implémenter l'un des sujets ci-dessous, **lis le fichier `SKILL.md` correspondant** via `read_file` pour appliquer les patterns officiels Android Studio :

| Domaine d'intervention | Skill à activer | Chemin du Skill |
| :--- | :--- | :--- |
| **Insets & Affichage plein écran** (Barres système, gestion Edge-to-Edge du plateau) | `edge-to-edge` | `~/.android-studio/skills/edge-to-edge/SKILL.md` |
| **Performance UI Compose** (Stabilité, recompositions des animations de dés sur `Canvas`) | `jetpack-compose-performance` | `~/.android-studio/skills/jetpack-compose-performance/SKILL.md` |
| **Audit ANR & Threading** (Room, export dataset IA, capteurs, Nearby Connections, Firebase) | `anr-audit-and-fix` | `~/.android-studio/skills/anr-audit-and-fix/SKILL.md` |
| **UI Adaptative** (Portrait, paysage, tablettes, pliables pour le Pass & Play) | `adaptive` | `~/.android-studio/skills/adaptive/SKILL.md` |
| **Tests Unitaires & UI** (JUnit moteur 5000, DAO Room, entropie, BotPolicy, UI Compose) | `testing-setup` | `~/.android-studio/skills/testing-setup/SKILL.md` |

---

## 4. Stack Technique de Référence

- **Nom de l'application** : `Neon 5K`
- **Package racine / Namespace** : `fr.antoinehory.neon5k`
- **SDK** : `minSdk = 29`, `targetSdk = 37`, `compileSdk = 37`
- **Langage & Build** : Kotlin `2.2.10`, AGP `9.5.1`, KSP `2.3.13`, Java 11, Version Catalog (`gradle/libs.versions.toml`)
- **UI & Direction Artistique** :
    - Jetpack Compose (BOM `2026.02.01`), Material 3, Edge-to-Edge activé (`Neon5KTheme`).
    - Style **Minimaliste × Cyberpunk × Pixel Art** : rendu natif 100 % Compose `Canvas` (grille pixel sans antialiasing) et `VectorDrawable` XML — **aucune librairie graphique ou moteur de jeu tiers**.
    - **Icône XML VectorDrawable pixel-cyberpunk** + **Splash Screen Android 12+** (`androidx.core:core-splashscreen:1.2.0`).
    - **Personnalisation complète de la fenêtre de jeu** : 14 thèmes prédéfinis classés en 4 familles (*Cyberpunk & Rétro*, *Niveaux de Gris* en 3 contrastes, *Sépia* en 3 contrastes, *Accessibilité Daltonisme* Okabe-Ito / Tritanopie / Achromatopsie) + générateurs aléatoires (*Random Harmonieux* & *Random Chaos*) + couleurs sur-mesure (sélecteur hybride HSV / RGB `0..255` + `#RRGGBB` avec contrôle contraste WCAG 2.1), 3 styles de dés (*Pixel Solid* 3D, *Neon Wireframe* CAO, *Dot Matrix* LED), scanlines optionnelles, mode droitier/gaucher, haptique & secousse.
- **Persistance Locale & IA Auto-Apprenante** :
    - AndroidX Room (`2.8.5` via KSP) : parties sauvegardées (en cours et historique complet des parties terminées avec journal des lancers et paliers d'encoches `✕`), statistiques locales, 20 succès à comptage infini post-déverrouillage et journal des décisions (`decision_logs`).
    - **IA Locale Auto-Apprenante (`AdaptiveBotLearner`)** : les 3 profils de bots locaux (*Safe Runner*, *Cyber Balanced*, *Overclock Kamikaze*) se nourrissent automatiquement en temps réel du dataset local `decision_logs` par apprentissage par renforcement tabulaire (régularisation bayésienne empirique) hybridé avec la table exacte $6^N$ (`RollProbabilityTable`), sans aucune dépendance ML externe.
    - AndroidX DataStore Preferences (`1.1.3`) : thème, personnalisation visuelle, préférences de jeu, presets et variantes favorites du 5000, consentement opt-in dataset IA.
- **Aléatoire Hybride Capteurs** : `SensorManager` (gyroscope + accéléromètre) + `nanoTime` + coordonnées/durée tactiles + sel `SecureRandom` → hash `SHA-256` → seed déterministe traçable.
- **Services & Multijoueur (par lots)** :
    - **Lot 4** : Google Play Games Services v2 (`com.google.android.gms:play-services-games-v2`) — authentification silencieuse & succès.
    - **Lot 5** : Google Nearby Connections (`com.google.android.gms:play-services-nearby`) — multijoueur local sans Internet en topologie étoile (`P2P_STAR`).
    - **Lot 6 (Optionnel)** : Firebase Realtime Database (plan Spark 0 €) — salons privés par code à 6 caractères & upload opt-in compressé des logs de parties en fin de match.

---

## 5. Garde-Fous Critiques (Résumé)

> Pour le détail complet des règles de code, consulter [`docs/CODING_STANDARDS.md`](./docs/CODING_STANDARDS.md).

1. **Isolation stricte du domaine (`domain/`)** : Le moteur de règles du 5000 (`FiveThousandEngine`), le validateur de combinaisons, `GameRulesConfig`, `BotPolicy` et le générateur de seed `EntropyHasher` doivent être du **Kotlin pur sans aucun import `android.*`**, testables à 100 % sur JVM.
2. **MVVM & UDF strict** : État immuable (`@Immutable`) exposé via `StateFlow` (`collectAsStateWithLifecycle()`), Composables sans logique de règles ni accès capteurs/BDD/réseau.
3. **Zéro dépendance lourde** : Ne jamais ajouter de moteur de jeu (LibGDX, Unity), de runtime ML (TensorFlow Lite, ONNX) ou de librairie d'animation lourde (Lottie). Tout rendu passe par Compose `Canvas` / `VectorDrawable` et toute IA future passe par des poids légers lus en Kotlin pur.
4. **R8 complet en Release (Leçon ADR-013)** : Ne jamais restreindre `optimization` avec un `packageScope` partiel dans `app/build.gradle.kts` pour éviter tout `IllegalAccessError` sur `kotlin.sequences.*` au démarrage en Release.
5. **Résilience Hors-Ligne & Capteurs** : L'application doit rester 100 % jouable sans Internet, sans compte Google Play Jeux et sur des appareils sans gyroscope (fallback silencieux garanti).
6. **Quotas Firebase 0 € (Lot 6)** : Ne jamais écrire à chaque micro-événement. N'écrire en base distante qu'à la validation d'un lancer/tour en ligne, et limiter l'envoi du dataset IA à **1 seule écriture agrégée en fin de partie** (uniquement si l'utilisateur a activé l'opt-in).

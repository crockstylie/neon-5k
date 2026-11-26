# 📋 Changelog — Neon 5K

Toutes les modifications notables apportées à ce projet sont documentées dans ce fichier.
Le format est basé sur [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/).

---

## [Unreleased]

### 🛠️ Initialisation & Spécifications
- Mise en place de la documentation "Source de Vérité" pour le workflow AntiGravity (`AGENTS.md`, `docs/ARCHITECTURE.md`, `docs/CODING_STANDARDS.md`, `docs/DECISIONS.md`, `docs/ROADMAP.md`).
- Définition exhaustive du moteur paramétrable du 5000 (`GameRulesConfig`) et de toutes ses variantes optionnelles.
- Spécification du collecteur d'entropie multi-capteurs (`SHA-256`), du logger Room pour l'entraînement IA et du découpage en 6 lots (Solo/Pass & Play → IA/Custom → Play Store → Play Jeux → Nearby → Firebase).
- Création des assets vectoriels XML de démarrage (icône pixel-art cyberpunk et Splash Screen Android 12+).
- Rédaction de la fiche Google Play Store en 4 langues (`fr`, `en`, `es`, `pt`) et de la politique de confidentialité.

---

## [1.0.0] - À définir (Fin Lot 4)

### 🎉 Objectif Première Publication Google Play
- Moteur complet du 5000 paramétrable (5 ou 6 dés + toutes les variantes classiques).
- Aléatoire hybride par capteurs (gyroscope, accéléromètre, nanosecondes, tactile) + inspecteur de graine.
- Modes Solo contre 3 profils d'IA probabiliste et Pass & Play sur un appareil.
- Personnalisation complète de l'interface cyberpunk / pixel art (5 presets + couleurs custom + 3 styles de dés).
- Sauvegarde automatique Room, statistiques locales et export dataset IA (`.jsonl` / `.csv`).
- Intégration Google Play Jeux v2 avec 10 succès à débloquer.
- Interface traduite en 4 langues : Français (`fr`), Anglais (`en`), Espagnol (`es`) et Portugais (`pt`).

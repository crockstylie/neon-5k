# Politique de Confidentialité — Neon 5K

**Dernière mise à jour :** 27 novembre 2026

La présente Politique de Confidentialité décrit la manière dont le jeu mobile **Neon 5K** (ci-après « l'Application »), développé par **Antoine « Crock » Hory**, traite les informations sur votre appareil.

L'Application est conçue selon un principe de **minimisation stricte des données** : aucune publicité, aucun traceur marketing et aucune vente de données. Le jeu reste intégralement jouable hors-ligne.

---

## 1. Données traitées localement sur votre appareil

* **Parties, historique des lancers, statistiques, succès et préférences :** Vos parties en cours, l'historique local de vos parties terminées (scores, paliers et journal des lancers), vos statistiques locales, la progression de vos 20 succès, vos variantes de règles et vos réglages d'interface (thème, couleurs, styles de dés, langue) sont stockés exclusivement en local dans l'espace privé de l'Application (bases AndroidX Room & DataStore).
* **Capteurs de mouvement (Gyroscope / Accéléromètre) :** Les valeurs instantanées des capteurs de mouvement sont lues en mémoire vive uniquement lorsque l'écran de jeu est actif afin de générer la graine aléatoire (`SHA-256`) de vos lancers de dés et, si vous l'activez, détecter une secousse physique. **Ces mesures brutes ne sont jamais enregistrées de façon permanente ni transmises à un serveur.**
* **Journal local des décisions & Auto-apprentissage local de l'IA (`decision_logs`) :** L'Application enregistre localement les états abstraits des tours (scores, dés tirés, dés conservés, choix de relancer ou banquer, issue du tour) sans aucune donnée personnelle. Ce journal sert uniquement :
    1. À permettre aux bots locaux (*Safe Runner*, *Cyber Balanced*, *Overclock Kamikaze*) d'affiner leur stratégie de jeu directement sur votre appareil (**apprentissage 100 % local hors-ligne**).
    2. À vous permettre d'exporter vous-même ce dataset en `.jsonl` ou `.csv` via le sélecteur de fichiers système Android (Storage Access Framework, sans aucune permission d'accès global à votre stockage), ou de le purger à tout moment depuis l'onglet *Stats & Succès > IA & Data*.

---

## 2. Services optionnels et fonctionnalités connectées

Certaines fonctionnalités facultatives font appel à des services tiers uniquement si vous choisissez de les utiliser :

* **Google Play Jeux (v2 — Optionnel) :** Si vous disposez d'un profil Google Play Jeux sur votre appareil, l'Application synchronise le déblocage de vos 20 succès avec Google Play Services. Si vous n'êtes pas connecté ou jouez hors-ligne, l'intégralité des 20 succès et leurs compteurs de maîtrise fonctionnent normalement en local sur votre appareil.
* **Multijoueur local sans Internet — Google Nearby Connections (Lot 5 — Optionnel) :** Lorsque vous hébergez ou rejoignez une partie locale à proximité, Android utilise le Bluetooth / Wi-Fi Direct pour échanger l'état de la partie directement entre les téléphones participants. Les permissions système de proximité ne sont demandées qu'à l'ouverture explicite de ce mode et aucune donnée ne transite par un serveur distant.
* **Multijoueur en ligne & Partage anonyme IA — Firebase Realtime Database (Lot 6 — Optionnel) :**
    * En mode **Salon en ligne**, seuls les pseudonymes de session, la configuration de règles et les actions de jeu sont échangés via Firebase Realtime Database (Google LLC) le temps de la partie.
    * Le **partage anonyme de parties pour entraîner de futures IA** est **désactivé par défaut** (*opt-in* explicite). S'il est activé par vos soins, un résumé strictement anonyme des coups joués (sans nom, sans e-mail, sans identifiant publicitaire) est envoyé une seule fois en fin de partie.

---

## 3. Partage avec des tiers et Publicité

* **Zéro publicité et zéro courtier en données :** L'Application n'intègre aucun SDK publicitaire, aucun outil d'analyse comportementale tiers (Analytics) ni aucun outil de profilage marketing.
* **Sous-traitants techniques (uniquement si les modes connectés sont utilisés) :** Google LLC (Google Play Jeux, Nearby Connections et Firebase Realtime Database), soumis à la [Politique de confidentialité de Google](https://policies.google.com/privacy).

---

## 4. Conservation et suppression des données

* **Données locales :** Vous pouvez purger à tout moment le journal des décisions IA depuis l'écran *Stats & Succès > IA & Data*, réinitialiser les données de l'Application depuis les paramètres Android, ou tout supprimer en désinstallant l'Application.
* **Salons en ligne (Lot 6) :** Les données de salon temporaires sont purgées automatiquement après la fin de la partie. Les journaux anonymes partagés volontairement (opt-in) ne permettent pas de vous identifier.

---

## 5. Liens externes

L'écran « Informations & Crédits » contient des liens externes vers le site web du développeur, ses profils de réseaux sociaux (LinkedIn, Behance, Instagram, Facebook, Spotify, Steam), la fiche Google Play Store ou une page de soutien volontaire (PayPal). En cliquant sur ces liens, vous quittez l'Application et êtes soumis aux politiques de confidentialité des sites consultés.

---

## 6. Contact

Pour toute question relative à cette Politique de Confidentialité :

* **Développeur :** Antoine « Crock » Hory
* **Courriel :** [contact@antoinehory.fr](mailto:contact@antoinehory.fr)
* **Site web :** [https://antoinehory.fr](https://antoinehory.fr)

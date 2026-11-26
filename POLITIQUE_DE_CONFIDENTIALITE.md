# Politique de Confidentialité — Neon 5K

**Dernière mise à jour :** 26 novembre 2026

La présente Politique de Confidentialité décrit la manière dont le jeu mobile **Neon 5K** (ci-après « l'Application »), développé par **Antoine « Crock » Hory**, traite les informations sur votre appareil.

L'Application est conçue selon un principe de **minimisation stricte des données** : aucune publicité, aucun traceur marketing et aucune vente de données. Le jeu reste intégralement jouable hors-ligne.

---

## 1. Données traitées localement sur votre appareil

* **Parties, statistiques et préférences :** Vos parties en cours, vos statistiques locales, vos variantes de règles et vos réglages d'interface (thème, couleurs, styles de dés) sont stockés localement dans l'espace privé de l'Application (Room & DataStore).
* **Capteurs de mouvement (Gyroscope / Accéléromètre) :** Les valeurs instantanées des capteurs de mouvement sont lues uniquement lorsque vous jouez afin de générer la graine aléatoire (`SHA-256`) de vos lancers de dés et, si vous l'activez, détecter une secousse. **Ces mesures brutes ne sont jamais enregistrées en base de données ni transmises à un serveur.**
* **Journal local des décisions de jeu (Entraînement IA) :** L'Application enregistre localement les états abstraits des tours (scores, dés tirés, dés conservés, choix de relancer ou banquer) sans aucune donnée personnelle. Vous pouvez exporter ce fichier sur votre appareil ou le supprimer à tout moment depuis les paramètres.

---

## 2. Services optionnels et fonctionnalités connectées

Certaines fonctionnalités facultatives font appel à des services tiers uniquement si vous choisissez de les utiliser :

* **Google Play Jeux (Lot 4 — Optionnel) :** Si vous vous connectez à votre profil Google Play Jeux, l'Application échange votre identifiant de joueur avec Google Play Services uniquement pour débloquer et synchroniser vos succès. Vous pouvez jouer sans jamais vous connecter.
* **Multijoueur local sans Internet — Google Nearby Connections (Lot 5 — Optionnel) :** Lorsque vous hébergez ou rejoignez une partie locale à proximité, Android utilise le Bluetooth / Wi-Fi Direct pour échanger l'état de la partie directement entre les téléphones participants. Les permissions de proximité ne sont demandées qu'au lancement de ce mode et aucune donnée ne transite par nos serveurs.
* **Multijoueur en ligne & Partage anonyme IA — Firebase Realtime Database (Lot 6 — Optionnel) :**
    * En mode **Salon en ligne**, seuls les pseudonymes de session, la configuration de règles et les actions de jeu sont échangés via Firebase Realtime Database (Google LLC) le temps de la partie.
    * Le **partage anonyme de parties pour entraîner de futures IA** est **désactivé par défaut** (*opt-in* explicite dans les paramètres). S'il est activé par vos soins, un résumé strictement anonyme des coups joués (sans nom, sans e-mail, sans identifiant appareil) est envoyé une seule fois en fin de partie.

---

## 3. Partage avec des tiers et Publicité

* **Zéro publicité et zéro courtier en données :** L'Application n'intègre aucun SDK publicitaire ni outil de profilage marketing.
* **Sous-traitants techniques (uniquement si les modes connectés sont utilisés) :** Google LLC (Google Play Jeux, Nearby Connections et Firebase Realtime Database), soumis à la [Politique de confidentialité de Google](https://policies.google.com/privacy).

---

## 4. Conservation et suppression des données

* **Données locales :** Vous pouvez effacer vos statistiques et journaux locaux depuis les paramètres de l'Application, ou tout supprimer en désinstallant l'Application.
* **Salons en ligne (Lot 6) :** Les données de salon temporaires sont purgées automatiquement après la fin de la partie. Les journaux anonymes partagés volontairement (opt-in) ne permettent pas de vous identifier.

---

## 5. Liens externes

L'écran « À propos » contient des liens externes vers le site du développeur, la fiche Google Play Store ou une page de soutien volontaire (PayPal). En cliquant sur ces liens, vous quittez l'Application et êtes soumis aux politiques de confidentialité des sites consultés.

---

## 6. Contact

Pour toute question relative à cette Politique de Confidentialité :

* **Développeur :** Antoine « Crock » Hory
* **Courriel :** [contact@antoinehory.fr](mailto:contact@antoinehory.fr)
* **Site web :** [https://antoinehory.fr](https://antoinehory.fr)

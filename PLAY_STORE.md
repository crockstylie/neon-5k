# 🚀 Fiche Google Play Store — Neon 5K

Ce document rassemble tous les textes prêts à être copiés-collés dans la **Google Play Console** pour les **4 langues supportées** ainsi que les spécifications des visuels store :
- 🇫🇷 **Français (`fr-FR`)**
- 🇬🇧 **Anglais (`en-US` / `en-GB`)** — *Langue par défaut*
- 🇪🇸 **Espagnol (`es-ES` / `es-419`)**
- 🇵🇹 **Portugais (`pt-PT` / `pt-BR`)**

> 💡 **Rappel des limites Google Play Console (toutes vérifiées ci-dessous) :**
> - **Titre de l'application** : 30 caractères maximum
> - **Description courte** : 80 caractères maximum
> - **Description complète** : 4 000 caractères maximum
> - **Notes de version (Nouveautés)** : 500 caractères maximum

---

## ⚙️ Paramètres généraux Google Play Console

- **Type :** Jeu (*Game*)
- **Catégorie :** Jeux de dés (*Board* / *Dice*) ou Jeux de société (*Board*)
- **Tags suggérés :** `Jeu de dés`, `5000`, `Farkle`, `Pixel Art`, `Cyberpunk`, `Hors-ligne`, `Multijoueur`
- **Contenu publicitaire :** Non (Aucune publicité)
- **Achats intégrés :** Non (100 % gratuit)
- **Classification du contenu (IARC) :** Tout public (PEGI 3 / Everyone) — aucune mise d'argent réel ni jeu de hasard simulé avec monnaie.
- **Sécurité des données (Data Safety) :**
    - *Lots 1–3 (Hors-ligne pur) :* Aucune donnée collectée ni partagée.
    - *Lot 4 (Google Play Jeux v2) :* Déclarer l'identifiant joueur Play Jeux uniquement pour la fonctionnalité de l'application (succès/profil), optionnel et chiffré en transit.
    - *Lot 6 (Firebase RTDB + Opt-in IA) :* Déclarer l'activité de jeu anonyme transmise en transit (chiffrée HTTPS/TLS) pour les salons en ligne et le partage volontaire opt-in de parties.

---

## 🎨 Spécifications des Visuels Store à produire

| Asset | Dimensions / Format | Direction Artistique |
| :--- | :--- | :--- |
| **Icône Play Store** | `512 × 512 px` PNG (sans transparence alpha superflue) | Rendu direct de `ic_launcher_foreground.xml` + `ic_launcher_background.xml` (dé pixel art face 5 cyan/magenta sur nuit `#090A10`). |
| **Image de présentation (Feature Graphic)** | `1024 × 500 px` PNG/JPG | Fond `#090A10` avec grille perspective néon subtile, 5 dés pixel art alignés en combinaison « 5000 », titre **NEON 5K** en police pixel cyan + sous-titre court. |
| **Captures Smartphone (×4 à ×6)** | `1080 × 1920 px` (9:16) | 1. Plateau de jeu actif (*Neon Night*, sélection scorante visible). 2. Modale « Trace d'Entropie » (capteurs + hash). 3. Écran des variantes paramétrables du 5000. 4. Studio de thèmes (*Terminal Phosphor* / *CRT Amber* / *Custom*). 5. Partie vs IA *Overclock Kamikaze* & Succès Play Jeux. |

---

# 🇫🇷 1. FRANÇAIS (`fr-FR`)

### Titre de l'application *(7 / 30 caractères)*
```text
Neon 5K
```

### Description courte *(77 / 80 caractères)*
```text
Le jeu de dés 5000 en pixel art cyberpunk : règles sur-mesure, IA et succès.
```

### Description complète
```text
Relancez… ou sécurisez vos points avant le crash ! Neon 5K revisite le célèbre jeu de dés traditionnel du 5000 (stop ou encore) dans une interface minimaliste, cyberpunk et pixel art, 100 % gratuite et sans publicité.

Que vous jouiez selon les règles classiques à 5 dés ou vos variantes familiales à 6 dés, Neon 5K s'adapte à votre façon de jouer et met vos nerfs à l'épreuve.

🎲 TOUTES LES RÈGLES ET VARIANTES DU 5000
• Préréglages prêts à jouer : Classique 5 dés, Variante 6 dés et Partie Rapide (2 500).
• Personnalisez chaque détail : seuil d'ouverture (0, 500, 750 ou 1 000), score exact obligatoire, main pleine avec relance forcée, carré et quinte, suite, 3 paires, full, repiquage (reprise du tour adverse), chapeau/écrasement et pénalité de 3 coups nuls.
• Barème clair et rappel des règles accessible à tout moment pendant la partie.

🌀 UN ALÉATOIRE FORGÉ PAR VOS CAPTEURS
• Chaque lancer combine en direct les valeurs du gyroscope, de l'accéléromètre, l'horodatage à la nanoseconde et votre toucher (ou une secousse du téléphone), hachés en SHA-256 pour un tirage équitable et unique.
• Touchez le badge d'entropie d'un lancer pour inspecter les signaux qui ont généré vos dés.

🎨 FENÊTRE DE JEU PIXEL ART & CYBERPUNK SUR-MESURE
• 5 thèmes prêts à l'emploi : Neon Night, Terminal Phosphor, CRT Amber, Pocket Mono et OLED Minimal.
• Créez votre propre ambiance avec la roue de couleurs (et codes Hex #RRGGBB), 3 styles de dés pixel art (Plein, Wireframe néon, Matrice), effet scanlines CRT, mode droitier/gaucher et retour haptique.

🤖 SOLO CONTRE L'IA, PASS & PLAY & SUCCÈS
• Entraînez-vous seul contre 3 profils d'IA (Safe Runner, Cyber Balanced, Overclock Kamikaze) ou jouez entre amis sur le même écran en Pass & Play.
• Suivez vos statistiques locales et débloquez les succès Google Play Jeux (Brelan d'As, Double main pleine, Score exact au pixel près…).
• Reprise automatique de votre partie en cours à tout moment.

🔒 LÉGER, SANS PUB ET RESPECTUEUX DE VOTRE VIE PRIVÉE
• Jouable hors-ligne, sans publicité ni achat intégré.
• Application minimaliste conçue sans moteur lourd pour un lancement instantané et une consommation batterie maîtrisée.
```

### Notes de version (`1.0.0`) *(398 / 500 caractères)*
```text
🎲 Bienvenue dans Neon 5K (v1.0.0) !
• Le jeu de dés du 5000 complet et paramétrable (5 ou 6 dés + variantes repiquage, chapeau, suite, 3 paires…).
• Tirage des dés par entropie multi-capteurs (gyroscope, accéléromètre, nanosecondes, tactile) + secousse optionnelle.
• Solo contre 3 profils d'IA + mode Pass & Play.
• 5 thèmes cyberpunk/pixel art + couleurs et dés sur-mesure.
• Succès Google Play Jeux.
```

---

# 🇬🇧 2. ENGLISH (`en-US` / `en-GB`)

### App Title *(7 / 30 characters)*
```text
Neon 5K
```

### Short Description *(76 / 80 characters)*
```text
Cyberpunk pixel-art 5000 (Farkle) dice game: custom rules, AI & achievements.
```

### Full Description
```text
Roll again… or bank your points before the crash! Neon 5K brings the classic push-your-luck 5000 (Farkle) dice game into a minimalist cyberpunk pixel-art interface—100% free and ad-free.

Whether you play traditional 5-dice rules or house 6-dice variants, Neon 5K lets you tailor every rule to match your table.

🎲 FULL 5000 / FARKLE RULES & CUSTOM VARIANTS
• Quick presets: Classic 5-Dice, 6-Dice Variant, and Fast 2,500 mode.
• Customize every rule: opening threshold (0, 500, 750, or 1,000), exact finish required, mandatory hot-dice reroll, four/five of a kind, straights, three pairs, full house, piggyback (steal the previous turn), score bumping, and three-strikes penalty.
• Built-in interactive rules guide synced with your active configuration.

🌀 SENSOR-DRIVEN ENTROPY ROLLS
• Every roll blends real-time gyroscope and accelerometer readings, nanosecond timestamps, and touch coordinates/duration (or phone shake), hashed with SHA-256 for fair, unique randomness.
• Tap the entropy badge on any roll to inspect the exact sensor seed behind your dice.

🎨 CUSTOMIZABLE CYBERPUNK PIXEL-ART TABLE
• 5 handcrafted themes: Neon Night, Terminal Phosphor, CRT Amber, Pocket Mono, and OLED Minimal.
• Build your own look with a full HSV/Hex (#RRGGBB) color picker, 3 pixel-art dice styles (Solid, Neon Wireframe, Dot Matrix), optional CRT scanlines, left/right-handed layout, and haptic feedback.

🤖 AI OPPONENTS, PASS & PLAY & ACHIEVEMENTS
• Challenge 3 distinct AI personalities (Safe Runner, Cyber Balanced, Overclock Kamikaze) or play locally with friends in Pass & Play mode.
• Track detailed local stats and unlock Google Play Games achievements.
• Automatic save & resume so you never lose a match.

🔒 LIGHTWEIGHT, AD-FREE & PRIVACY-FIRST
• Playable offline with zero ads and zero in-app purchases.
• Built natively in Kotlin & Jetpack Compose with no bloated game engine for instant startup and smooth battery life.
```

### Release Notes (`1.0.0`) *(389 / 500 caractères)*
```text
🎲 Welcome to Neon 5K (v1.0.0)!
• Classic 5000 / Farkle dice game with customizable 5-dice & 6-dice house rules.
• Fun sensor-driven SHA-256 entropy dice rolls (gyroscope, accelerometer, nanoseconds, touch) + optional shake-to-roll.
• Solo mode vs 3 AI profiles + local Pass & Play.
• 5 cyberpunk pixel-art themes + custom colors and dice styles.
• Google Play Games achievements support.
```

---

# 🇪🇸 3. ESPAÑOL (`es-ES` / `es-419`)

### Título de la aplicación *(7 / 30 caracteres)*
```text
Neon 5K
```

### Descripción breve *(78 / 80 caracteres)*
```text
Juego de dados 5000 en pixel art cyberpunk: reglas a medida, IA y logros.
```

### Descripción completa
```text
¿Vuelves a tirar o aseguras tus puntos antes del fallo? Neon 5K reinventa el clásico juego de dados 5000 (Farkle) con una estética minimalista cyberpunk y pixel art, 100 % gratis y sin anuncios.

Juega con las reglas clásicas de 5 dados o configura tus propias variantes de 6 dados en segundos.

🎲 REGLAS COMPLETAS Y VARIANTES PERSONALIZABLES
• Modos rápidos: Clásico 5 dados, Variante 6 dados y Partida rápida (2 500).
• Ajusta cada detalle: umbral de apertura (0, 500, 750 o 1 000), puntuación exacta obligatoria, relanzamiento obligatorio con todos los dados, póker y repóker, escalera, tres pares, full, robo de turno, derribo de rival y penalización por 3 fallos seguidos.
• Guía de reglas integrada y adaptada a tu configuración activa.

🌀 ALEATORIEDAD CON LOS SENSORES DEL TELÉFONO
• Cada tirada combina el giroscopio, el acelerómetro, la marca temporal en nanosegundos y tu toque en pantalla (o agitar el móvil), procesados con SHA-256 para obtener tiradas justas y únicas.
• Toca la insignia de entropía para ver la semilla exacta de cada lanzamiento.

🎨 MESA CYBERPUNK PIXEL ART A TU GUSTO
• 5 temas incluidos: Neon Night, Terminal Phosphor, CRT Amber, Pocket Mono y OLED Minimal.
• Personaliza tus colores (rueda HSV y código #RRGGBB), 3 estilos de dados pixel art, líneas CRT opcionales, modo diestro/zurdo y vibración háptica.

🤖 IA LOCAL, PASS & PLAY Y LOGROS
• Enfréntate a 3 perfiles de IA (Safe Runner, Cyber Balanced, Overclock Kamikaze) o juega con amigos en el mismo dispositivo.
• Consulta tus estadísticas locales y desbloquea logros de Google Play Juegos.

🔒 LIGERO, SIN ANUNCIOS Y PRIVADO
• Funciona sin conexión, sin publicidad ni compras integradas.
```

### Notas de la versión (`1.0.0`) *(381 / 500 caracteres)*
```text
🎲 ¡Bienvenido a Neon 5K (v1.0.0)!
• Juego de dados 5000 con reglas personalizables de 5 y 6 dados y todas las variantes clásicas.
• Tiradas con entropía de sensores (giroscopio, acelerómetro, nanosegundos, toque) y opción de agitar.
• Modo individual contra 3 perfiles de IA y modo Pass & Play local.
• 5 temas cyberpunk pixel art + colores y dados personalizables.
• Logros de Google Play Juegos.
```

---

# 🇵🇹 4. PORTUGUÊS (`pt-PT` / `pt-BR`)

### Título da aplicação *(7 / 30 caracteres)*
```text
Neon 5K
```

### Breve descrição *(76 / 80 caracteres)*
```text
Jogo de dados 5000 em pixel art cyberpunk: regras à medida, IA e conquistas.
```

### Descrição completa
```text
Arrisca mais um lançamento ou guarda os seus pontos antes de perder tudo? Neon 5K traz o clássico jogo de dados 5000 (Farkle) com visual minimalista cyberpunk em pixel art, 100% gratuito e sem anúncios.

Jogue com as regras tradicionais de 5 dados ou personalize todas as variantes de 6 dados em segundos.

🎲 REGRAS COMPLETAS E VARIANTES PERSONALIZÁVEIS
• Predefinições rápidas: Clássico 5 dados, Variante 6 dados e Partida Rápida (2 500).
• Configure tudo: abertura mínima (0, 500, 750 ou 1 000), pontuação exata obrigatória, relançamento obrigatório, quadra/quina, sequência, três pares, full house, retoma de turno, queda de adversário e penalidade de 3 jogadas nulas.
• Guia de regras integrado sempre acessível durante o jogo.

🌀 ALEATORIEDADE COM OS SENSORES DO TELEMÓVEL
• Cada lançamento combina giroscópio, acelerómetro, tempo em nanossegundos e o seu toque (ou agitar o aparelho), combinados em SHA-256 para resultados justos e únicos.
• Toque no selo de entropia para ver os dados que geraram a sua jogada.

🎨 MESA PIXEL ART CYBERPUNK PERSONALIZÁVEL
• 5 temas incluídos: Neon Night, Terminal Phosphor, CRT Amber, Pocket Mono e OLED Minimal.
• Escolha as suas cores (roda HSV e código #RRGGBB), 3 estilos de dados pixel art, efeito scanlines CRT, modo destro/esquerdino e resposta háptica.

🤖 IA LOCAL, PASS & PLAY E CONQUISTAS
• Enfrente 3 perfis de IA (Safe Runner, Cyber Balanced, Overclock Kamikaze) ou jogue com amigos no mesmo dispositivo.
• Acompanhe as suas estatísticas locais e desbloqueie conquistas no Google Play Jogos.

🔒 LEVE, SEM ANÚNCIOS E PRIVADO
• Jogável offline, sem publicidade e sem compras na aplicação.
```

### Notas de lançamento (`1.0.0`) *(377 / 500 caracteres)*
```text
🎲 Bem-vindo ao Neon 5K (v1.0.0)!
• Jogo de dados 5000 com regras personalizáveis de 5 e 6 dados e variantes clássicas.
• Lançamentos com entropia de sensores (giroscópio, acelerómetro, nanossegundos, toque) e opção de agitar.
• Modo solo contra 3 perfis de IA + modo Pass & Play local.
• 5 temas cyberpunk pixel art + cores e dados personalizáveis.
• Conquistas do Google Play Jogos.
```

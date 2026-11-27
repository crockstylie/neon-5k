# 🚀 Fiche Google Play Store — Neon 5K

Ce document rassemble tous les textes prêts à être copiés-collés dans la **Google Play Console** pour les **4 langues officielles de l'application** ainsi que les liens directs vers les visuels PNG générés dans [`docs/play_store_assets/`](file:///C:/Users/hory/AndroidStudioProjects/Neon5K/docs/play_store_assets) :
- 🇬🇧 **Anglais (`en-US` / `en-GB`)** — *À définir comme langue par défaut de la fiche pour activer la traduction automatique gratuite de Google Play vers +40 autres langues*
- 🇫🇷 **Français (`fr-FR`)**
- 🇪🇸 **Espagnol (`es-ES` / `es-419`)**
- 🇵🇹 **Portugais (`pt-PT` / `pt-BR`)**

> 💡 **Rappel des limites Google Play Console (toutes vérifiées ci-dessous) :**
> - **Titre de l'application** : 30 caractères maximum (`Neon 5K` = 7 car.)
> - **Description courte** : 80 caractères maximum
> - **Description complète** : 4 000 caractères maximum
> - **Notes de version (Nouveautés)** : 500 caractères maximum

---

## 🎨 Visuels Store Prêts à Uploader (`docs/play_store_assets/`)

| Asset | Dimensions / Format | Fichier généré prêt à uploader |
| :--- | :--- | :--- |
| **Icône Play Store** | `512 × 512 px` PNG 32-bit | [`docs/play_store_assets/play_store_icon_512x512.png`](file:///C:/Users/hory/AndroidStudioProjects/Neon5K/docs/play_store_assets/play_store_icon_512x512.png) |
| **Image de présentation (Feature Graphic)** | `1024 × 500 px` PNG 32-bit | [`docs/play_store_assets/play_store_feature_graphic_1024x500.png`](file:///C:/Users/hory/AndroidStudioProjects/Neon5K/docs/play_store_assets/play_store_feature_graphic_1024x500.png) |
| **Import ZIP des 20 Succès Play Jeux (4 langues + 20 PNG `512×512`)** | Archive `.zip` officielle Play Console (`AchievementsMetadata.csv` + 20 PNG) | [`docs/play_store_assets/play_games_achievements_import_fr_en.zip`](file:///C:/Users/hory/AndroidStudioProjects/Neon5K/docs/play_store_assets/play_games_achievements_import_fr_en.zip) |
| **Captures Smartphone (×4 à ×6)** | `1080 × 1920 px` (9:16) | 1. Plateau de jeu actif (*Neon Night*, sélection scorante + historique lancers/encoches `✕`). 2. Modale « Trace d'Entropie » (`SHA-256`). 3. Écran des variantes paramétrables du 5000. 4. Studio de thèmes (14 presets + Custom HSV/RGB). 5. Écran Stats (Camemberts, Barres, Courbe & 20 Succès). |

---

## 🌍 Comment utiliser la traduction multi-langues dans la Google Play Console ?

1. **Pour la Fiche Play Store principale** (*Présence sur le Play Store > Fiche Play Store principale*) :
   - Choisis **`English (United States) – en-US`** comme **langue par défaut** et colle la section **🇬🇧 1. ENGLISH** ci-dessous.
   - Clique sur **Gérer les traductions > Gérer vos propres traductions**, ajoute **`fr-FR`**, **`es-ES`** et **`pt-PT`**, puis colle les sections **🇫🇷 2. FRANÇAIS**, **🇪🇸 3. ESPAÑOL** et **🇵🇹 4. PORTUGUÊS** ci-dessous (elles utilisent le vocabulaire exact des règles du 5000 dans chaque langue).
   - Ensuite, va dans **Croissance > Traductions > Traduire la fiche Play Store** → clique sur **Créer une commande > Traduction automatique gratuite** en choisissant **Anglais (`en-US`) comme langue source** et coche toutes les autres langues suggérées par Google (Allemand, Italien, Japonais, Coréen, Polonais, Néerlandais, etc.).
2. **Pour les 20 Succès Google Play Jeux** (*Services de jeux Play > Configuration et gestion > Succès*) :
   - Clique sur **Importer des succès** (*Import achievements*) et sélectionne directement **[`docs/play_store_assets/play_games_achievements_import_fr_en.zip`](file:///C:/Users/hory/AndroidStudioProjects/Neon5K/docs/play_store_assets/play_games_achievements_import_fr_en.zip)**.
   - Un seul upload crée les **20 succès (1 000 XP)** avec leurs **20 icônes `512 × 512 px`** et leurs textes dans les **4 langues (`en-US`, `fr-FR`, `es-ES`, `pt-PT`)** !

---

# 🇬🇧 1. ENGLISH (`en-US` / `en-GB`) — *Langue par défaut recommandée*

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

🎲 FULL 5000 / FARKLE RULES & CUSTOM HOUSE VARIANTS
• Quick presets: Classic 5-Dice, 6-Dice Variant, Strategic Pro, and Fast Arcade mode.
• Customize every detail: opening threshold (0, 500, 750, or 1,000), post-opening minimum turn score, round multiples of 100 required to bank (no banking a lone 50), final 1,000-point single-turn sprint, exact finish required, mandatory hot-dice reroll, four/five of a kind, 5-Ones instant win, 5-dice & 6-dice straights, three pairs, full house, piggyback (steal the previous turn), score bumping, and 3-strikes penalty or checkpoint rollback with strike memory.
• Live in-game roll history, score checkpoints with strike marks (✕), and an interactive rules simulator synced with your active configuration.

🌀 SENSOR-DRIVEN SHA-256 ENTROPY ROLLS
• Every roll blends real-time gyroscope and accelerometer readings, nanosecond timestamps, and touch coordinates/duration (or phone shake), hashed with SHA-256 for provably fair randomness.
• Tap the entropy badge on any roll to inspect the exact sensor seed behind your dice.

🎨 CUSTOMIZABLE CYBERPUNK PIXEL-ART TABLE & ACCESSIBILITY
• 14 handcrafted presets across 4 families: Cyberpunk & Retro (Neon Night, Terminal Phosphor, CRT Amber, Pocket Mono, OLED Minimal), Grayscale (3 contrasts), Sepia (3 contrasts), and Colorblind Accessibility (Okabe-Ito Protanopia/Deuteranopia, Tritanopia, Achromatopsia), plus Harmonious & Chaos random palette generators.
• Full HSV & RGB (0–255) / Hex (#RRGGBB) color picker with live WCAG 2.1 contrast checker, 3 pixel-art dice styles (3D Pixel Solid, Neon CAD Wireframe, LED Dot Matrix), CRT scanlines, left/right-handed layout, portrait & 2-column landscape modes, and haptic feedback.

🤖 SELF-LEARNING LOCAL AI, CHARTS, PASS & PLAY & 20 ACHIEVEMENTS
• Challenge up to 7 AI bots across 3 personalities (Safe Runner, Cyber Balanced, Overclock Kamikaze) powered by exact 6^N probability tables blended with on-device tabular reinforcement learning that adapts from your matches—or play with up to 8 friends in Pass & Play mode.
• Explore visual charts (win/loss & bank/bust donut charts, score trend curves, SHA-256 die face distribution & triple collector bar charts), full match & roll logs, and export your AI decision dataset (.jsonl / .csv).
• Unlock 20 Google Play Games achievements (1,000 XP) with infinite lifetime mastery counters (Bronze to Cyber-Platinum).

🔒 LIGHTWEIGHT, AD-FREE & PRIVACY-FIRST
• Playable 100% offline with zero ads and zero in-app purchases.
```

### Release Notes (`1.0.0`) *(438 / 500 characters)*
```text
🎲 Welcome to Neon 5K (v1.0.0)!
• Complete 5000 / Farkle dice game (5 or 6 dice) with all house rules (round-100 banking, piggyback, bump, 3-strikes checkpoint rollback, 5-dice & 6-dice straights…).
• Sensor-driven SHA-256 entropy dice rolls + optional shake-to-roll.
• Self-learning local AI bots + up to 8-player Pass & Play.
• 14 themes (including colorblind & monochrome), 3 pixel dice styles, visual stats charts & 20 Play Games achievements.
```

---

# 🇫🇷 2. FRANÇAIS (`fr-FR`)

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
• Préréglages prêts à jouer : Classique 5 dés, Variante 6 dés, Stratégique Pro et Arcade Rapide.
• Personnalisez chaque détail : seuil d'ouverture (0, 500, 750 ou 1 000), minimum par tour après ouverture, chiffre rond multiple de 100 obligatoire pour banquer (interdiction de banquer un 5 isolé), sprint final des 1 000 derniers points en un seul tour, score exact obligatoire, main pleine avec relance forcée, carré et quinte (×4 ou 5×Cinq = 5 000), victoire instantanée sur 5 As, suite à 5 dés et grande suite à 6 dés (1-6), 3 paires, full, repiquage (reprise du tour adverse), chapeau/écrasement et pénalité des 3 encoches (✕✕✕ : -500, -1 000 ou recul au palier précédent avec mémoire des encoches).
• Historique en direct des lancers et des paliers d'encoches (✕) sur le plateau + simulateur interactif du barème actif.

🌀 UN ALÉATOIRE FORGÉ PAR VOS CAPTEURS (SHA-256)
• Chaque lancer combine en direct les valeurs du gyroscope, de l'accéléromètre, l'horodatage à la nanoseconde et votre toucher (ou une secousse du téléphone), hachés en SHA-256 pour un tirage équitable et unique.
• Touchez le badge d'entropie d'un lancer pour inspecter les signaux qui ont généré vos dés.

🎨 FENÊTRE DE JEU PIXEL ART, ACCESSIBILITÉ & MODE PAYSAGE
• 14 thèmes classés en 4 familles : Cyberpunk & Rétro (Neon Night, Terminal Phosphor, CRT Amber, Pocket Mono, OLED Minimal), Niveaux de Gris (3 contrastes), Sépia (3 contrastes) et Accessibilité Daltonisme (Okabe-Ito Protanopie/Deutéranopie, Tritanopie, Achromatopsie) + générateurs aléatoires Harmonieux et Chaos.
• Sélecteur de couleurs hybride HSV / RGB (0..255) / Hex (#RRGGBB) avec contrôle de contraste WCAG 2.1, 3 styles de dés pixel art (Plein 3D, Filaire CAO, Matrice LED), scanlines CRT, mode droitier/gaucher, modes Portrait et Paysage 2 colonnes, et retour haptique.

🤖 IA LOCALE AUTO-APPRENANTE, GRAPHIQUES, PASS & PLAY & 20 SUCCÈS
• Affrontez jusqu'à 7 bots parmi 3 profils d'IA (Safe Runner, Cyber Balanced, Overclock Kamikaze) qui s'améliorent automatiquement en local au fil de vos parties, ou jouez jusqu'à 8 joueurs en Pass & Play avec carrousel auto-centré.
• Consultez vos graphiques (camemberts victoires/banques, courbe de scores, barres d'équité SHA-256 et collection de brelans), le journal complet de vos parties et lancers, et exportez votre dataset IA (.jsonl / .csv).
• Débloquez 20 succès Google Play Jeux (1 000 XP) avec compteurs de maîtrise infinis (Bronze à Cyber-Platine).

🔒 LÉGER, SANS PUB ET RESPECTUEUX DE VOTRE VIE PRIVÉE
• Jouable 100 % hors-ligne, sans publicité ni achat intégré.
```

### Notes de version (`1.0.0`) *(446 / 500 caractères)*
```text
🎲 Bienvenue dans Neon 5K (v1.0.0) !
• Le jeu de dés du 5000 complet et paramétrable (5 ou 6 dés + chiffre rond multiple de 100, repiquage, chapeau, recul de palier aux 3 encoches ✕, suites 5 et 6 dés…).
• Tirage des dés par entropie multi-capteurs SHA-256 + secousse optionnelle.
• Solo contre 3 profils d'IA auto-apprenante + Pass & Play jusqu'à 8 joueurs.
• 14 thèmes (dont daltonisme), 3 styles de dés, graphiques stats et 20 succès Play Jeux.
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
• Modos rápidos: Clásico 5 dados, Variante 6 dados, Estratégico Pro y Arcade Rápido.
• Ajusta cada detalle: umbral de apertura (0, 500, 750 o 1 000), mínimo por turno tras abrir, múltiplo redondo de 100 obligatorio para plantar, sprint final de los últimos 1 000 puntos en un turno, puntuación exacta obligatoria, mano plena con relanzamiento obligatorio, póker y repóker, victoria instantánea con 5 Ases, escaleras de 5 y 6 dados, tres pares, full, robo de turno (repiquage), derribo de rival y penalización o retroceso de escalón con memoria de marcas (✕✕✕) tras 3 fallos seguidos.
• Historial en vivo de tiradas y escalones de puntuación en pantalla y simulador interactivo de reglas.

🌀 ALEATORIEDAD CON LOS SENSORES DEL TELÉFONO (SHA-256)
• Cada tirada combina el giroscopio, el acelerómetro, la marca temporal en nanosegundos y tu toque en pantalla (o agitar el móvil), procesados con SHA-256 para obtener tiradas justas y únicas.
• Toca la insignia de entropía para inspeccionar la semilla exacta de cada lanzamiento.

🎨 MESA CYBERPUNK PIXEL ART, ACCESIBILIDAD Y MODO HORIZONTAL
• 14 temas incluidos (Cyberpunk, Escala de grises, Sepia y Accesibilidad para daltonismo Okabe-Ito / Tritanopia / Acromatopsia) + generadores aleatorios Armonioso y Caos.
• Selector de colores HSV / RGB (0..255) y #RRGGBB con control de contraste WCAG 2.1, 3 estilos de dados pixel art (Sólido 3D, Wireframe neón, Matriz LED), líneas CRT, modo diestro/zurdo y diseño adaptado a vertical y horizontal.

🤖 IA AUTOAPRENDIZAJE, GRÁFICOS, PASS & PLAY Y 20 LOGROS
• Enfréntate a 3 perfiles de IA que aprenden localmente de tus partidas o juega hasta 8 jugadores en Pass & Play.
• Analiza tus estadísticas con gráficos circulares, curvas y barras, revisa el historial completo de partidas y tiradas, y desbloquea 20 logros de Google Play Juegos (1 000 XP).

🔒 LIGERO, SIN ANUNCIOS Y PRIVADO
• Funciona 100 % sin conexión, sin publicidad ni compras integradas.
```

### Notas de la versión (`1.0.0`) *(415 / 500 caracteres)*
```text
🎲 ¡Bienvenido a Neon 5K (v1.0.0)!
• Juego de dados 5000 completo (5 y 6 dados) con todas las variantes (múltiplo de 100, robo de turno, derribo, retroceso por 3 fallos ✕, escaleras de 5 y 6 dados…).
• Tiradas con entropía SHA-256 de sensores y opción de agitar.
• IA local con autoaprendizaje y modo Pass & Play hasta 8 jugadores.
• 14 temas, 3 estilos de dados, gráficos de estadísticas y 20 logros de Play Juegos.
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
• Predefinições rápidas: Clássico 5 dados, Variante 6 dados, Estratégico Pro e Arcade Rápido.
• Configure tudo: abertura mínima (0, 500, 750 ou 1 000), mínimo por turno após abrir, múltiplo redondo de 100 obrigatório para guardar, sprint final dos últimos 1 000 pontos num único turno, pontuação exata obrigatória, mão cheia com relançamento forçado, quadra e quina, vitória instantânea com 5 Ases, sequências de 5 e 6 dados, três pares, full house, retoma de turno, queda de adversário e penalidade ou recuo de patamar com memória de marcas (✕✕✕) após 3 jogadas nulas.
• Histórico ao vivo de lançamentos e patamares de pontuação no ecrã + simulador interativo de regras.

🌀 ALEATORIEDADE COM OS SENSORES DO TELEMÓVEL (SHA-256)
• Cada lançamento combina giroscópio, acelerómetro, tempo em nanossegundos e o seu toque (ou agitar o aparelho), combinados em SHA-256 para resultados justos e únicos.
• Toque no selo de entropia para ver os sinais que geraram a sua jogada.

🎨 MESA PIXEL ART CYBERPUNK, ACESSIBILIDADE E MODO PAISAGEM
• 14 temas incluídos (Cyberpunk, Escala de cinzentos, Sépia e Acessibilidade para daltonismo Okabe-Ito / Tritanopia / Acromatopsia) + geradores aleatórios Harmonioso e Caos.
• Seletor de cores HSV / RGB (0..255) e #RRGGBB com verificação de contraste WCAG 2.1, 3 estilos de dados pixel art (Sólido 3D, Wireframe néon, Matriz LED), efeito scanlines CRT, modo destro/esquerdino e suporte para retrato e paisagem.

🤖 IA COM AUTOAPRENDIZAGEM, GRÁFICOS, PASS & PLAY E 20 CONQUISTAS
• Enfrente 3 perfis de IA que aprendem localmente com as suas partidas ou jogue até 8 jogadores em Pass & Play.
• Consulte gráficos visuais (setores, curvas e barras), histórico detalhado de partidas e lançamentos, e desbloqueie 20 conquistas no Google Play Jogos (1 000 XP).

🔒 LEVE, SEM ANÚNCIOS E PRIVADO
• Jogável 100% offline, sem publicidade e sem compras na aplicação.
```

### Notas de lançamento (`1.0.0`) *(418 / 500 caracteres)*
```text
🎲 Bem-vindo ao Neon 5K (v1.0.0)!
• Jogo de dados 5000 completo (5 e 6 dados) com todas as variantes (múltiplo de 100, retoma de turno, queda, recuo de patamar após 3 falhas ✕, sequências de 5 e 6 dados…).
• Lançamentos com entropia SHA-256 de sensores e opção de agitar.
• IA local com autoaprendizagem + Pass & Play até 8 jogadores.
• 14 temas, 3 estilos de dados, gráficos de estatísticas e 20 conquistas Play Jogos.
```

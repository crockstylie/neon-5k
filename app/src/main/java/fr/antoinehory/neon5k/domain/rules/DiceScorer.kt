package fr.antoinehory.neon5k.domain.rules

/**
 * Évaluateur exhaustif et validateur de combinaisons scorantes du jeu de dés 5000.
 * Pur Kotlin (zéro dépendance Android), 100 % paramétré par [GameRulesConfig].
 *
 * Règles immuables :
 * - Les combinaisons sont évaluées uniquement sur le lancer courant (ou le sous-ensemble de dés sélectionnés sur le lancer courant).
 * - Une sélection n'est valide ([SelectionEvaluation.isValid]) que si elle est non vide et qu'elle ne comporte **aucun dé mort**.
 * - Lorsqu'un même sous-ensemble de dés peut être interprété de plusieurs manières (ex: Full House vs Brelan + paire d'As/5,
 *   ou 3 Paires vs Carré + Paire), [evaluateSelection] retient toujours l'interprétation qui consomme le maximum de dés
 *   et maximise le nombre de points.
 */
object DiceScorer {

    /**
     * Valeur de base d'un brelan (3 dés identiques) pour une face donnée (`1..6`).
     * - Trois As (`1`) = 1 000 points
     * - Trois `N` (`2..6`) = `N * 100` points
     */
    fun tripleBasePoints(face: Int): Int {
        require(face in 1..6) { "Die face must be in 1..6 (was $face)" }
        return if (face == 1) 1000 else face * 100
    }

    /**
     * Nom court en français d'une face de dé (`As`, `2`, `3`, `4`, `5`, `6`).
     */
    fun faceLabelFr(face: Int): String = if (face == 1) "As" else face.toString()

    /**
     * Évalue un sous-ensemble de dés sélectionnés par le joueur (ou l'ensemble d'un lancer).
     *
     * @param diceFaces Liste des faces (`1..6`) à évaluer (max 6 dés).
     * @param config Règles et variantes actives de la partie.
     * @return [SelectionEvaluation] détaillant la validité stricte (zéro dé mort), les points et les combinaisons.
     */
    fun evaluateSelection(
        diceFaces: List<Int>,
        config: GameRulesConfig
    ): SelectionEvaluation {
        if (diceFaces.isEmpty()) {
            return SelectionEvaluation(
                isValid = false,
                totalPoints = 0,
                combinations = emptyList(),
                scoringDiceCount = 0,
                deadDiceCount = 0,
                triggersInstantWin = false
            )
        }
        require(diceFaces.all { it in 1..6 }) { "All dice faces must be in 1..6: $diceFaces" }

        val counts = IntArray(7)
        for (face in diceFaces) {
            counts[face]++
        }

        val candidates = mutableListOf<PartitionResult>()

        // 1. Partition standard face par face (Brelan / Carré / Quinte / Sextuplé + As et 5 isolés)
        candidates += evaluateFaceByFace(counts, diceFaces.size, config)

        // 2. Suite (Straight) : en 5 dés -> (1..5) ou (2..6) ; en 6 dés -> (1..6) ou suite de 5 dés + éventuel 1/5
        if (config.straightScore > 0 && diceFaces.size >= 5) {
            evaluateStraights(counts, diceFaces.size, config)?.let { candidates += it }
        }

        // 3. Trois Paires (uniquement lorsque 6 dés sont évalués et threePairsScore > 0)
        if (config.diceCount == 6 && config.threePairsScore > 0 && diceFaces.size == 6) {
            evaluateThreePairs(counts, config)?.let { candidates += it }
        }

        // 4. Full House (3 identiques + 1 paire distincte, soit 5 dés exacts, si fullHouseBonus > 0)
        if (config.fullHouseBonus > 0 && diceFaces.size >= 5) {
            evaluateFullHouse(counts, diceFaces.size, config)?.let { candidates += it }
        }

        // On privilégie d'abord une interprétation sans dé mort (valid == true), puis le score maximal
        val bestValid = candidates.filter { it.deadDiceCount == 0 }
            .maxWithOrNull(compareBy<PartitionResult> { it.triggersInstantWin }.thenBy { it.totalPoints })

        val chosen = bestValid ?: candidates.maxWith(
            compareBy<PartitionResult> { it.scoringDiceCount }
                .thenBy { it.triggersInstantWin }
                .thenBy { it.totalPoints }
        )

        val isValid = chosen.scoringDiceCount == diceFaces.size && chosen.deadDiceCount == 0 && chosen.totalPoints > 0
        return SelectionEvaluation(
            isValid = isValid,
            totalPoints = if (isValid) chosen.totalPoints else 0,
            combinations = chosen.combinations,
            scoringDiceCount = chosen.scoringDiceCount,
            deadDiceCount = chosen.deadDiceCount,
            triggersInstantWin = isValid && chosen.triggersInstantWin
        )
    }

    /**
     * Retourne la meilleure évaluation possible en prenant le sous-ensemble valide optimal des dés fournis
     * (ignore les dés morts pour connaître le potentiel maximum d'un lancer).
     */
    fun evaluateMaxScoringSubset(
        rolledFaces: List<Int>,
        config: GameRulesConfig
    ): SelectionEvaluation {
        if (rolledFaces.isEmpty()) {
            return evaluateSelection(emptyList(), config)
        }
        val bestSubsetIndices = findBestValidSubsetIndices(rolledFaces, config)
        if (bestSubsetIndices.isEmpty()) {
            return SelectionEvaluation(
                isValid = false,
                totalPoints = 0,
                combinations = emptyList(),
                scoringDiceCount = 0,
                deadDiceCount = rolledFaces.size,
                triggersInstantWin = false
            )
        }
        val scoringFaces = bestSubsetIndices.sorted().map { rolledFaces[it] }
        val eval = evaluateSelection(scoringFaces, config)
        return eval.copy(deadDiceCount = rolledFaces.size - eval.scoringDiceCount)
    }

    /**
     * Vrai si au moins un dé ou une combinaison du lancer rapporte des points (c.-à-d. le lancer n'est pas un Coup nul / Bust).
     */
    fun hasAnyScoringCombination(
        rolledFaces: List<Int>,
        config: GameRulesConfig
    ): Boolean = findBestValidSubsetIndices(rolledFaces, config).isNotEmpty()

    /**
     * Identifie le meilleur sous-ensemble d'indices (`0 until rolledFaces.size`) qui forme **ensemble** une sélection
     * 100 % valide (zéro dé mort) maximisant la victoire instantanée, puis les points, puis le nombre de dés utilisés.
     * Utilisé par [evaluateMaxScoringSubset] et par le bouton « Tout sélectionner ».
     */
    fun findBestValidSubsetIndices(
        rolledFaces: List<Int>,
        config: GameRulesConfig
    ): Set<Int> {
        if (rolledFaces.isEmpty()) return emptySet()

        val fullEval = evaluateSelection(rolledFaces, config)
        if (fullEval.isValid && fullEval.totalPoints > 0) {
            return rolledFaces.indices.toSet()
        }

        val n = rolledFaces.size
        val maxMask = (1 shl n) - 1
        var bestMask = 0
        var bestInstantWin = false
        var bestPoints = 0
        var bestDiceCount = 0

        for (mask in 1..maxMask) {
            val subsetFaces = mutableListOf<Int>()
            for (bit in 0 until n) {
                if ((mask and (1 shl bit)) != 0) {
                    subsetFaces += rolledFaces[bit]
                }
            }
            val eval = evaluateSelection(subsetFaces, config)
            if (eval.isValid && eval.totalPoints > 0) {
                val isBetter = when {
                    eval.triggersInstantWin != bestInstantWin -> eval.triggersInstantWin
                    eval.totalPoints != bestPoints -> eval.totalPoints > bestPoints
                    subsetFaces.size != bestDiceCount -> subsetFaces.size > bestDiceCount
                    else -> false
                }
                if (isBetter) {
                    bestMask = mask
                    bestInstantWin = eval.triggersInstantWin
                    bestPoints = eval.totalPoints
                    bestDiceCount = subsetFaces.size
                }
            }
        }

        if (bestMask == 0) return emptySet()
        val result = mutableSetOf<Int>()
        for (bit in 0 until n) {
            if ((bestMask and (1 shl bit)) != 0) {
                result += bit
            }
        }
        return result
    }

    /**
     * Identifie tous les indices (`0 until rolledFaces.size`) des dés du lancer qui peuvent participer à **au moins une**
     * combinaison scorante valide.
     *
     * Garantit une cohérence mathématique totale entre l'UI (`canScoreOnCurrentRoll`) et l'IA (`HeuristicBotPolicy`) :
     * par exemple, sur `[1, 2, 3, 4, 5, 2]` en mode 6 dés, les deux `2` (indices 1 et 5) appartiennent chacun à une
     * Suite `1-5` valide et sont donc tous deux cliquables.
     */
    fun findScoringDieIndices(
        rolledFaces: List<Int>,
        config: GameRulesConfig
    ): Set<Int> {
        if (rolledFaces.isEmpty()) return emptySet()

        val fullEval = evaluateSelection(rolledFaces, config)
        if (fullEval.isValid && fullEval.totalPoints > 0) {
            return rolledFaces.indices.toSet()
        }

        val n = rolledFaces.size
        val maxMask = (1 shl n) - 1
        var unionMask = 0

        for (mask in 1..maxMask) {
            // Optimisation : si tous les bits de ce masque sont déjà dans unionMask, inutile de réévaluer
            if ((mask and unionMask.inv()) == 0) continue

            val subsetFaces = mutableListOf<Int>()
            for (bit in 0 until n) {
                if ((mask and (1 shl bit)) != 0) {
                    subsetFaces += rolledFaces[bit]
                }
            }
            val eval = evaluateSelection(subsetFaces, config)
            if (eval.isValid && eval.totalPoints > 0) {
                unionMask = unionMask or mask
            }
        }

        if (unionMask == 0) return emptySet()
        val scoringIndices = mutableSetOf<Int>()
        for (bit in 0 until n) {
            if ((unionMask and (1 shl bit)) != 0) {
                scoringIndices += bit
            }
        }
        return scoringIndices
    }

    private data class PartitionResult(
        val totalPoints: Int,
        val combinations: List<ScoringCombination>,
        val scoringDiceCount: Int,
        val deadDiceCount: Int,
        val triggersInstantWin: Boolean = false
    )

    private fun evaluateFaceByFace(
        counts: IntArray,
        totalDice: Int,
        config: GameRulesConfig
    ): PartitionResult {
        val combinations = mutableListOf<ScoringCombination>()
        var totalPoints = 0
        var scoringDice = 0
        var instantWin = false

        for (face in 1..6) {
            var remaining = counts[face]
            if (remaining == 0) continue

            val baseTriple = tripleBasePoints(face)
            val label = faceLabelFr(face)

            // 6 dés identiques (en variante 6 dés)
            if (remaining == 6) {
                when {
                    config.fiveOfAKindMode != FiveOfAKindMode.DISABLED -> {
                        val pts = baseTriple * 8
                        val win = face == 1 && config.fiveOfAKindMode == FiveOfAKindMode.INSTANT_WIN_ON_ONES
                        combinations += ScoringCombination(
                            kind = CombinationKind.SIX_OF_A_KIND,
                            diceFaces = List(6) { face },
                            points = pts,
                            labelFr = "Sextuplé de $label",
                            triggersInstantWin = win
                        )
                        totalPoints += pts
                        scoringDice += 6
                        if (win) instantWin = true
                        remaining = 0
                    }
                    else -> {
                        // Deux brelans
                        repeat(2) {
                            combinations += ScoringCombination(
                                kind = CombinationKind.THREE_OF_A_KIND,
                                diceFaces = List(3) { face },
                                points = baseTriple,
                                labelFr = "Brelan de $label"
                            )
                        }
                        totalPoints += baseTriple * 2
                        scoringDice += 6
                        remaining = 0
                    }
                }
            }

            // 5 dés identiques (Quinte)
            if (remaining == 5 && config.fiveOfAKindMode != FiveOfAKindMode.DISABLED) {
                val isInstantOnes = face == 1 && config.fiveOfAKindMode == FiveOfAKindMode.INSTANT_WIN_ON_ONES
                val pts = if (isInstantOnes) config.targetScore.coerceAtLeast(baseTriple * 4) else baseTriple * 4
                combinations += ScoringCombination(
                    kind = CombinationKind.FIVE_OF_A_KIND,
                    diceFaces = List(5) { face },
                    points = pts,
                    labelFr = if (isInstantOnes) "Quinte d'As (Victoire !)" else "Quinte de $label",
                    triggersInstantWin = isInstantOnes
                )
                totalPoints += pts
                scoringDice += 5
                if (isInstantOnes) instantWin = true
                remaining = 0
            }

            // 4 dés identiques (Carré)
            if (remaining >= 4 && config.fourOfAKindMode == FourOfAKindMode.DOUBLE_TRIPLE) {
                val pts = baseTriple * 2
                combinations += ScoringCombination(
                    kind = CombinationKind.FOUR_OF_A_KIND,
                    diceFaces = List(4) { face },
                    points = pts,
                    labelFr = "Carré de $label"
                )
                totalPoints += pts
                scoringDice += 4
                remaining -= 4
            }

            // 3 dés identiques (Brelan)
            if (remaining >= 3) {
                combinations += ScoringCombination(
                    kind = CombinationKind.THREE_OF_A_KIND,
                    diceFaces = List(3) { face },
                    points = baseTriple,
                    labelFr = if (face == 1) "Brelan d'As" else "Brelan de $label"
                )
                totalPoints += baseTriple
                scoringDice += 3
                remaining -= 3
            }

            // Dés individuels restants : seuls les As (100 pts) et les 5 (50 pts) scorent individuellement
            if (remaining > 0) {
                when (face) {
                    1 -> {
                        repeat(remaining) {
                            combinations += ScoringCombination(
                                kind = CombinationKind.SINGLE_ONE,
                                diceFaces = listOf(1),
                                points = 100,
                                labelFr = "As"
                            )
                        }
                        totalPoints += remaining * 100
                        scoringDice += remaining
                    }
                    5 -> {
                        repeat(remaining) {
                            combinations += ScoringCombination(
                                kind = CombinationKind.SINGLE_FIVE,
                                diceFaces = listOf(5),
                                points = 50,
                                labelFr = "Cinq"
                            )
                        }
                        totalPoints += remaining * 50
                        scoringDice += remaining
                    }
                }
            }
        }

        return PartitionResult(
            totalPoints = totalPoints,
            combinations = combinations,
            scoringDiceCount = scoringDice,
            deadDiceCount = totalDice - scoringDice,
            triggersInstantWin = instantWin
        )
    }

    private fun evaluateStraights(
        counts: IntArray,
        totalDice: Int,
        config: GameRulesConfig
    ): PartitionResult? {
        // Grande suite de 6 dés (1-2-3-4-5-6) en variante 6 dés
        if (totalDice == 6 && (1..6).all { counts[it] == 1 }) {
            val combo = ScoringCombination(
                kind = CombinationKind.STRAIGHT,
                diceFaces = listOf(1, 2, 3, 4, 5, 6),
                points = config.straightScore,
                labelFr = "Grande Suite (1-6)"
            )
            return PartitionResult(
                totalPoints = config.straightScore,
                combinations = listOf(combo),
                scoringDiceCount = 6,
                deadDiceCount = 0
            )
        }

        // Suite de 5 dés : 1-2-3-4-5 ou 2-3-4-5-6
        val isLowStraight = (1..5).all { counts[it] >= 1 }
        val isHighStraight = (2..6).all { counts[it] >= 1 }
        if (!isLowStraight && !isHighStraight) return null

        val straightRange = if (isLowStraight) 1..5 else 2..6
        val remainingCounts = counts.copyOf()
        for (face in straightRange) {
            remainingCounts[face]--
        }

        val straightCombo = ScoringCombination(
            kind = CombinationKind.STRAIGHT,
            diceFaces = straightRange.toList(),
            points = config.straightScore,
            labelFr = if (isLowStraight) "Suite (1-5)" else "Suite (2-6)"
        )

        val extraResult = evaluateFaceByFace(remainingCounts, totalDice - 5, config)
        return PartitionResult(
            totalPoints = config.straightScore + extraResult.totalPoints,
            combinations = listOf(straightCombo) + extraResult.combinations,
            scoringDiceCount = 5 + extraResult.scoringDiceCount,
            deadDiceCount = extraResult.deadDiceCount
        )
    }

    private fun evaluateThreePairs(
        counts: IntArray,
        config: GameRulesConfig
    ): PartitionResult? {
        // 3 paires en 6 dés : soit 3 faces à 2 dés, soit 1 face à 4 dés + 1 face à 2 dés, soit 6 dés identiques
        val pairCount = (1..6).sumOf { counts[it] / 2 }
        val hasOddRemainder = (1..6).any { counts[it] % 2 != 0 }
        if (pairCount != 3 || hasOddRemainder) return null

        val sortedFaces = (1..6).flatMap { face -> List(counts[face]) { face } }
        val combo = ScoringCombination(
            kind = CombinationKind.THREE_PAIRS,
            diceFaces = sortedFaces,
            points = config.threePairsScore,
            labelFr = "Trois Paires"
        )
        return PartitionResult(
            totalPoints = config.threePairsScore,
            combinations = listOf(combo),
            scoringDiceCount = 6,
            deadDiceCount = 0
        )
    }

    private fun evaluateFullHouse(
        counts: IntArray,
        totalDice: Int,
        config: GameRulesConfig
    ): PartitionResult? {
        // Chercher un brelan (counts[t] >= 3) et une paire d'une face distincte (counts[p] >= 2)
        var bestResult: PartitionResult? = null

        for (tripleFace in 1..6) {
            if (counts[tripleFace] < 3) continue
            for (pairFace in 1..6) {
                if (pairFace == tripleFace || counts[pairFace] < 2) continue

                val remainingCounts = counts.copyOf()
                remainingCounts[tripleFace] -= 3
                remainingCounts[pairFace] -= 2

                val fullPoints = tripleBasePoints(tripleFace) + config.fullHouseBonus
                val fullFaces = List(3) { tripleFace } + List(2) { pairFace }
                val fullCombo = ScoringCombination(
                    kind = CombinationKind.FULL_HOUSE,
                    diceFaces = fullFaces,
                    points = fullPoints,
                    labelFr = "Full (${faceLabelFr(tripleFace)} par les ${faceLabelFr(pairFace)})"
                )

                val extra = evaluateFaceByFace(remainingCounts, totalDice - 5, config)
                val candidate = PartitionResult(
                    totalPoints = fullPoints + extra.totalPoints,
                    combinations = listOf(fullCombo) + extra.combinations,
                    scoringDiceCount = 5 + extra.scoringDiceCount,
                    deadDiceCount = extra.deadDiceCount
                )
                if (bestResult == null ||
                    candidate.deadDiceCount < bestResult.deadDiceCount ||
                    (candidate.deadDiceCount == bestResult.deadDiceCount && candidate.totalPoints > bestResult.totalPoints)
                ) {
                    bestResult = candidate
                }
            }
        }
        return bestResult
    }
}

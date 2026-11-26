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
     * Retourne la meilleure évaluation possible en prenant le sous-ensemble optimal des dés fournis
     * (ignore les dés morts pour connaître le potentiel maximum d'un lancer).
     */
    fun evaluateMaxScoringSubset(
        rolledFaces: List<Int>,
        config: GameRulesConfig
    ): SelectionEvaluation {
        if (rolledFaces.isEmpty()) {
            return evaluateSelection(emptyList(), config)
        }
        val indices = findScoringDieIndices(rolledFaces, config)
        if (indices.isEmpty()) {
            return SelectionEvaluation(
                isValid = false,
                totalPoints = 0,
                combinations = emptyList(),
                scoringDiceCount = 0,
                deadDiceCount = rolledFaces.size,
                triggersInstantWin = false
            )
        }
        val scoringFaces = indices.sorted().map { rolledFaces[it] }
        val eval = evaluateSelection(scoringFaces, config)
        return eval.copy(deadDiceCount = rolledFaces.size - eval.scoringDiceCount)
    }

    /**
     * Vrai si au moins un dé ou une combinaison du lancer rapporte des points (c.-à-d. le lancer n'est pas un Coup nul / Bust).
     */
    fun hasAnyScoringCombination(
        rolledFaces: List<Int>,
        config: GameRulesConfig
    ): Boolean = findScoringDieIndices(rolledFaces, config).isNotEmpty()

    /**
     * Identifie les indices (`0 until rolledFaces.size`) des dés du lancer qui peuvent participer à au moins une combinaison scorante.
     * Sert à :
     * - allumer l'aide visuelle néon sur les dés sélectionnables,
     * - empêcher la sélection de dés morts,
     * - proposer la sélection rapide de tous les dés scorants.
     */
    fun findScoringDieIndices(
        rolledFaces: List<Int>,
        config: GameRulesConfig
    ): Set<Int> {
        if (rolledFaces.isEmpty()) return emptySet()

        // Si la totalité du lancer forme une combinaison globale valide (ex: Suite, 3 Paires, Full, ou Main pleine),
        // tous les indices sont scorants.
        val fullEval = evaluateSelection(rolledFaces, config)
        if (fullEval.isValid) {
            return rolledFaces.indices.toSet()
        }

        val counts = IntArray(7)
        for (face in rolledFaces) {
            counts[face]++
        }

        // Vérifier le cas d'une suite de 5 dés parmi 6 dés lancés ou d'un Full de 5 dés parmi 6 dés lancés
        if (rolledFaces.size == 6) {
            var bestSubsetIndices: Set<Int>? = null
            var bestSubsetPoints = 0
            for (excludedIdx in rolledFaces.indices) {
                val subsetFaces = rolledFaces.filterIndexed { idx, _ -> idx != excludedIdx }
                val subEval = evaluateSelection(subsetFaces, config)
                if (subEval.isValid && subEval.totalPoints > bestSubsetPoints) {
                    bestSubsetPoints = subEval.totalPoints
                    bestSubsetIndices = rolledFaces.indices.filter { it != excludedIdx }.toSet()
                }
            }
            // Comparer avec le score face par face standard
            val faceByFaceResult = evaluateFaceByFace(counts, rolledFaces.size, config)
            if (bestSubsetIndices != null && bestSubsetPoints > faceByFaceResult.totalPoints) {
                return bestSubsetIndices
            }
        }

        // Sinon, tout dé qui est un 1, un 5, ou dont la face apparaît au moins 3 fois est scorant
        val scoringIndices = mutableSetOf<Int>()
        for ((index, face) in rolledFaces.withIndex()) {
            when {
                face == 1 || face == 5 -> scoringIndices += index
                counts[face] >= 3 -> {
                    // Si un joueur a 4 ou 5 dés d'une face non-(1,5) mais que Carré/Quinte sont DISABLED,
                    // seuls `maxUsableForFace` dés de cette face peuvent scorer.
                    val usableCount = maxUsableCountForNonSingleFace(counts[face], config)
                    val alreadyAddedForFace = scoringIndices.count { rolledFaces[it] == face }
                    if (alreadyAddedForFace < usableCount) {
                        scoringIndices += index
                    }
                }
            }
        }
        return scoringIndices
    }

    private fun maxUsableCountForNonSingleFace(count: Int, config: GameRulesConfig): Int {
        if (count < 3) return 0
        return when (count) {
            3 -> 3
            4 -> if (config.fourOfAKindMode != FourOfAKindMode.DISABLED) 4 else 3
            5 -> when {
                config.fiveOfAKindMode != FiveOfAKindMode.DISABLED -> 5
                config.fourOfAKindMode != FourOfAKindMode.DISABLED -> 4
                else -> 3
            }
            else -> when {
                config.fiveOfAKindMode != FiveOfAKindMode.DISABLED -> 6
                config.fourOfAKindMode != FourOfAKindMode.DISABLED -> 4
                else -> 3
            }
        }
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

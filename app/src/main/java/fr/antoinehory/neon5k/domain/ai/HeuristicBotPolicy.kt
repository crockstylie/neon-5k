package fr.antoinehory.neon5k.domain.ai

import fr.antoinehory.neon5k.domain.model.MatchState
import fr.antoinehory.neon5k.domain.rules.CombinationKind
import fr.antoinehory.neon5k.domain.rules.DiceScorer
import fr.antoinehory.neon5k.domain.rules.GameRulesConfig
import fr.antoinehory.neon5k.domain.rules.SelectionEvaluation

/**
 * Implémentation probabiliste et heuristique de [BotPolicy] en Kotlin pur.
 *
 * Principes de décision :
 * 1. Génère tous les sous-ensembles valides de dés scorants parmi les dés libres (`!isLocked && canScoreOnCurrentRoll`).
 * 2. Règles de cohérence humaine au 5000 :
 *    - **En cas de `BANK`** : le bot prend **toujours** le sous-ensemble maximal qui ne dépasse pas `targetScore`
 *      (il ne laisse jamais de points gratuits sur la table au moment de banquer).
 *    - **En cas de `ROLL_AGAIN`** :
 *      - Si prendre tous les dés scorants déclenche une **Main pleine** (`remainingDiceToRoll == 0`), le bot prend toujours tout.
 *      - Un bot ne laisse **jamais** passer un Brelan (`>= 200 pts`), un Carré, une Quinte, une Suite, 3 Paires, un Full
 *        ni un As (`100 pts`), sauf si les prendre ferait dépasser `targetScore` en mode score exact.
 *      - L'économie tactique de dés (garder l'As et relancer un `5` isolé de `50 pts`) est réservée au profil
 *        [BotProfile.OVERCLOCK_KAMIKAZE] ou au cas critique où garder le `5` isolé forcerait à ne relancer qu'un seul dé (`P(bust) = 66,7 %`)
 *        alors qu'il est interdit de banquer.
 */
object HeuristicBotPolicy : BotPolicy {

    override fun shouldAcceptPiggyback(
        state: MatchState,
        profile: BotProfile
    ): Boolean {
        val offer = state.turn.piggybackOffer ?: return false
        if (!state.rules.piggybackEnabled || !state.activePlayer.isOpened) return false

        // Si le pot hérité fait déjà dépasser le score cible, refuser
        if (state.rules.exactFinishRequired && state.activePlayer.score + offer.inheritedPoints >= state.rules.targetScore) {
            return false
        }

        val odds = RollProbabilityTable.getOdds(offer.remainingDiceCount, state.rules)
        return when (profile) {
            BotProfile.SAFE_RUNNER -> offer.remainingDiceCount >= 3 && offer.inheritedPoints >= 400
            BotProfile.CYBER_BALANCED -> {
                val expectedValue = odds.scoringProbability * (offer.inheritedPoints + odds.expectedMaxGain)
                expectedValue >= 380.0 && offer.remainingDiceCount >= 2
            }
            BotProfile.OVERCLOCK_KAMIKAZE -> offer.inheritedPoints >= 300 || offer.remainingDiceCount >= 2
        }
    }

    override fun planMove(
        state: MatchState,
        profile: BotProfile
    ): BotMovePlan {
        val rules = state.rules
        val turn = state.turn
        val player = state.activePlayer

        val freeDice = turn.dice.filter { !it.isLocked }
        val selectableFreeDice = freeDice.filter { it.canScoreOnCurrentRoll }
        require(selectableFreeDice.isNotEmpty()) { "BotPolicy.planMove called with no scoring dice available." }

        val candidateSubsets = generateCandidateSelections(
            freeDiceIndices = selectableFreeDice.map { it.index },
            allDiceFaces = turn.dice.map { it.face },
            config = rules
        )

        val totalFreeDiceCount = freeDice.size
        val bestOpponentScore = state.players
            .filter { it.id != player.id }
            .maxOfOrNull { it.score } ?: 0
        val trailingBehind = (bestOpponentScore - player.score) >= 1200

        val riskAversion = when (profile) {
            BotProfile.SAFE_RUNNER -> 1.45
            BotProfile.CYBER_BALANCED -> if (trailingBehind) 0.85 else 1.05
            BotProfile.OVERCLOCK_KAMIKAZE -> 0.55
        }

        val minBankTargetWhenOpened = when (profile) {
            BotProfile.SAFE_RUNNER -> 250
            BotProfile.CYBER_BALANCED -> if (trailingBehind) 450 else 350
            BotProfile.OVERCLOCK_KAMIKAZE -> 600
        }

        // Sous-ensembles qui ne font pas dépasser targetScore (si exactFinishRequired)
        val nonOverflowCandidates = candidateSubsets.filter { candidate ->
            val projectedTotal = player.score + turn.bankedInTurn + candidate.points
            !rules.exactFinishRequired || projectedTotal <= rules.targetScore
        }

        if (nonOverflowCandidates.isEmpty()) {
            // Tous les sous-ensembles dépassent la cible : prendre le plus petit sous-ensemble valide
            val smallest = candidateSubsets.minBy { it.points }
            return BotMovePlan(
                indicesToSelect = smallest.indices,
                nextAction = BotTurnAction.ROLL_AGAIN
            )
        }

        // 1. Victoire immédiate possible en banquant l'un des sous-ensembles ?
        for (candidate in nonOverflowCandidates.sortedByDescending { it.points }) {
            val projectedTurnPot = turn.bankedInTurn + candidate.points
            val projectedTotalScore = player.score + projectedTurnPot
            val remainingDiceToRoll = totalFreeDiceCount - candidate.indices.size
            val triggersHotDice = remainingDiceToRoll == 0

            val winsOnBank = if (rules.exactFinishRequired) {
                projectedTotalScore == rules.targetScore
            } else {
                projectedTotalScore >= rules.targetScore
            }
            val canBankThis = (player.isOpened || projectedTurnPot >= rules.openingThreshold) &&
                (!rules.exactFinishRequired || projectedTotalScore <= rules.targetScore) &&
                (!triggersHotDice || !rules.mandatoryRerollOnHotDice)

            if (canBankThis && winsOnBank) {
                return BotMovePlan(
                    indicesToSelect = candidate.indices,
                    nextAction = BotTurnAction.BANK
                )
            }
        }

        // 2. Identifier le sous-ensemble maximal (celui qui maximise les points, puis le nombre de dés pour favoriser la Main pleine)
        val maxCandidate = nonOverflowCandidates.maxWith(
            compareBy<CandidateSelection> { it.points }.thenBy { it.indices.size }
        )

        // Si le sous-ensemble maximal déclenche une Main pleine (remainingDiceToRoll == 0), on prend TOUJOURS tout !
        if (totalFreeDiceCount - maxCandidate.indices.size == 0) {
            val projectedTurnPot = turn.bankedInTurn + maxCandidate.points
            val canBankHotDice = !rules.mandatoryRerollOnHotDice &&
                (player.isOpened || projectedTurnPot >= rules.openingThreshold)
            // Même si mandatoryRerollOnHotDice est false, relancer 5 ou 6 dés a ~92-97% de réussite
            return BotMovePlan(
                indicesToSelect = maxCandidate.indices,
                nextAction = if (canBankHotDice && profile == BotProfile.SAFE_RUNNER && projectedTurnPot >= 1500) {
                    BotTurnAction.BANK
                } else {
                    BotTurnAction.ROLL_AGAIN
                }
            )
        }

        // 3. Filtrer les candidats autorisés pour ROLL_AGAIN :
        //    Un bot ne doit JAMAIS abandonner un Brelan, Carré, Quinte, Suite, 3 Paires, Full, ni un As (100 pts) présent dans maxCandidate.
        val maxHighValueCount = maxCandidate.highValueDiceCount
        val eligibleRollCandidates = nonOverflowCandidates.filter { candidate ->
            if (candidate.indices == maxCandidate.indices) {
                true
            } else if (candidate.highValueDiceCount < maxHighValueCount) {
                // Ce sous-ensemble abandonne un As ou une combinaison majeure -> interdit !
                false
            } else {
                // Ce sous-ensemble ne laisse de côté qu'un ou plusieurs 5 isolés (50 pts) :
                // Autorisé pour OVERCLOCK_KAMIKAZE, ou si prendre maxCandidate ne laisserait qu'1 seul dé à relancer sans pouvoir banquer
                val maxRemainingDice = totalFreeDiceCount - maxCandidate.indices.size
                val maxProjectedPot = turn.bankedInTurn + maxCandidate.points
                val canBankMax = player.isOpened || maxProjectedPot >= rules.openingThreshold
                profile == BotProfile.OVERCLOCK_KAMIKAZE || (!canBankMax && maxRemainingDice == 1)
            }
        }.ifEmpty { listOf(maxCandidate) }

        var bestPlan: BotMovePlan? = null
        var bestUtility = Double.NEGATIVE_INFINITY

        for (candidate in eligibleRollCandidates) {
            val projectedTurnPot = turn.bankedInTurn + candidate.points
            val projectedTotalScore = player.score + projectedTurnPot
            val remainingDiceToRoll = totalFreeDiceCount - candidate.indices.size
            val triggersHotDice = remainingDiceToRoll == 0

            val canBankThis = candidate.points == maxCandidate.points &&
                (player.isOpened || projectedTurnPot >= rules.openingThreshold) &&
                (!rules.exactFinishRequired || projectedTotalScore <= rules.targetScore) &&
                (!triggersHotDice || !rules.mandatoryRerollOnHotDice)

            val nextDiceCount = if (triggersHotDice) rules.diceCount else remainingDiceToRoll
            val odds = RollProbabilityTable.getOdds(nextDiceCount, rules)

            val remainingToTarget = (rules.targetScore - projectedTotalScore).coerceAtLeast(0)
            val overflowPenaltyFactor = if (rules.exactFinishRequired && remainingToTarget in 1..250) 0.45 else 1.0

            val expectedPotIfRoll = odds.scoringProbability * overflowPenaltyFactor * (projectedTurnPot + odds.expectedMaxGain) -
                odds.bustProbability * projectedTurnPot * riskAversion

            val hotDiceBonus = if (triggersHotDice) 220.0 else 0.0
            val rollUtility = expectedPotIfRoll + hotDiceBonus

            if (!canBankThis) {
                if (rollUtility > bestUtility) {
                    bestUtility = rollUtility
                    bestPlan = BotMovePlan(candidate.indices, BotTurnAction.ROLL_AGAIN)
                }
            } else {
                val bankUtility = projectedTurnPot.toDouble()
                val shouldRoll = when {
                    triggersHotDice -> true
                    !player.isOpened && projectedTurnPot >= rules.openingThreshold && profile != BotProfile.OVERCLOCK_KAMIKAZE ->
                        false
                    projectedTurnPot < minBankTargetWhenOpened && nextDiceCount >= 3 -> true
                    profile == BotProfile.SAFE_RUNNER && odds.bustProbability > 0.28 -> false
                    else -> rollUtility > bankUtility
                }

                val chosenAction = if (shouldRoll) BotTurnAction.ROLL_AGAIN else BotTurnAction.BANK
                val chosenUtility = if (shouldRoll) rollUtility else bankUtility

                if (chosenUtility > bestUtility) {
                    bestUtility = chosenUtility
                    bestPlan = BotMovePlan(candidate.indices, chosenAction)
                }
            }
        }

        val finalPlan = bestPlan ?: BotMovePlan(
            indicesToSelect = maxCandidate.indices,
            nextAction = BotTurnAction.ROLL_AGAIN
        )

        // Garantie finale : si l'action retenue est BANK, toujours banquer le sous-ensemble maximal (maxCandidate)
        return if (finalPlan.nextAction == BotTurnAction.BANK && finalPlan.indicesToSelect != maxCandidate.indices) {
            finalPlan.copy(indicesToSelect = maxCandidate.indices)
        } else {
            finalPlan
        }
    }

    private data class CandidateSelection(
        val indices: List<Int>,
        val points: Int,
        val evaluation: SelectionEvaluation
    ) {
        /**
         * Nombre de dés appartenant à une combinaison à haute valeur (As isolé = 100 pts, Brelan, Carré, Quinte, Suite, 3 Paires, Full),
         * c'est-à-dire tous les dés scorants hors `5` isolés (`SINGLE_FIVE`).
         */
        val highValueDiceCount: Int
            get() = evaluation.combinations
                .filter { it.kind != CombinationKind.SINGLE_FIVE }
                .sumOf { it.diceFaces.size }
    }

    private fun generateCandidateSelections(
        freeDiceIndices: List<Int>,
        allDiceFaces: List<Int>,
        config: GameRulesConfig
    ): List<CandidateSelection> {
        val results = mutableListOf<CandidateSelection>()
        val n = freeDiceIndices.size

        // Énumérer les sous-ensembles non vides (2^n - 1, au maximum 2^6 - 1 = 63 combinaisons -> < 0.05 ms)
        val maxMask = (1 shl n) - 1
        for (mask in 1..maxMask) {
            val chosenIndices = mutableListOf<Int>()
            val chosenFaces = mutableListOf<Int>()
            for (bit in 0 until n) {
                if ((mask and (1 shl bit)) != 0) {
                    val dieIdx = freeDiceIndices[bit]
                    chosenIndices += dieIdx
                    chosenFaces += allDiceFaces[dieIdx]
                }
            }
            val eval = DiceScorer.evaluateSelection(chosenFaces, config)
            if (eval.isValid && eval.totalPoints > 0) {
                results += CandidateSelection(
                    indices = chosenIndices,
                    points = eval.totalPoints,
                    evaluation = eval
                )
            }
        }

        return results
    }
}

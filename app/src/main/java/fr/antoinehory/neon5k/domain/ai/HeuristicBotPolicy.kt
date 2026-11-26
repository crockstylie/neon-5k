package fr.antoinehory.neon5k.domain.ai

import fr.antoinehory.neon5k.domain.model.MatchState
import fr.antoinehory.neon5k.domain.rules.DiceScorer
import fr.antoinehory.neon5k.domain.rules.GameRulesConfig

/**
 * Implémentation probabiliste et heuristique de [BotPolicy] en Kotlin pur.
 *
 * Principes de décision :
 * 1. Génère les sous-ensembles valides de dés scorants parmi les dés libres du lancer courant :
 *    - Le sous-ensemble maximal (tous les dés scorants : indispensable pour déclencher une Main pleine ou banquer le max),
 *    - Les sous-ensembles « économiques » (ex: garder un seul As `100 pts` au lieu de `As + Cinq` pour conserver plus de dés à relancer).
 * 2. Évalue pour chaque sous-ensemble :
 *    - la valeur immédiate de `BANK` (si autorisée : ouvert, pas de dépassement, pas de relance obligatoire sur Main pleine),
 *    - l'espérance mathématique nette de `ROLL_AGAIN` grâce à [RollProbabilityTable] :
 *      $\text{EV}_{\text{roll}} = (1 - P_{\text{bust}}) \times (\text{pot} + E[\text{gain}]) - P_{\text{bust}} \times \text{pot} \times \text{riskAversion}$.
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
        require(freeDice.any { it.canScoreOnCurrentRoll }) { "BotPolicy.planMove called with no scoring dice available." }

        val candidateSubsets = generateCandidateSelections(
            freeDiceIndices = freeDice.map { it.index },
            allDiceFaces = turn.dice.map { it.face },
            config = rules
        )

        val totalFreeDiceCount = turn.dice.count { !it.isLocked }
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

        var bestPlan: BotMovePlan? = null
        var bestUtility = Double.NEGATIVE_INFINITY

        for (candidate in candidateSubsets) {
            val projectedTurnPot = turn.bankedInTurn + candidate.points
            val projectedTotalScore = player.score + projectedTurnPot
            val remainingDiceToRoll = totalFreeDiceCount - candidate.indices.size
            val triggersHotDice = remainingDiceToRoll == 0

            // Si ce choix fait dépasser la cible en mode score exact, on l'évite autant que possible
            if (rules.exactFinishRequired && projectedTotalScore > rules.targetScore) {
                continue
            }

            // Victoire immédiate possible en banquant ce sous-ensemble ?
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

            // Évaluation de l'action ROLL_AGAIN avec ce sous-ensemble
            val nextDiceCount = if (triggersHotDice) rules.diceCount else remainingDiceToRoll
            val odds = RollProbabilityTable.getOdds(nextDiceCount, rules)

            // Si en relançant près de 5000 en mode score exact il ne reste qu'une marge infime (< 150 pts), le risque d'overflow est élevé
            val remainingToTarget = (rules.targetScore - projectedTotalScore).coerceAtLeast(0)
            val overflowPenaltyFactor = if (rules.exactFinishRequired && remainingToTarget in 1..250) 0.45 else 1.0

            val expectedPotIfRoll = odds.scoringProbability * overflowPenaltyFactor * (projectedTurnPot + odds.expectedMaxGain) -
                odds.bustProbability * projectedTurnPot * riskAversion

            // Bonus si le choix déclenche une Main pleine (on récupère tous les dés avec ~97% de réussite !)
            val hotDiceBonus = if (triggersHotDice) 220.0 else 0.0
            val rollUtility = expectedPotIfRoll + hotDiceBonus

            if (!canBankThis) {
                // Impossible de banquer (ex: pas encore ouvert, ou Main pleine avec relance obligatoire) -> ROLL_AGAIN obligatoire
                if (rollUtility > bestUtility) {
                    bestUtility = rollUtility
                    bestPlan = BotMovePlan(candidate.indices, BotTurnAction.ROLL_AGAIN)
                }
            } else {
                // Comparer BANK vs ROLL_AGAIN
                val bankUtility = projectedTurnPot.toDouble()
                val shouldRoll = when {
                    triggersHotDice -> true // Toujours rentable de relancer une main pleine à 5 ou 6 dés
                    !player.isOpened && projectedTurnPot >= rules.openingThreshold && profile != BotProfile.OVERCLOCK_KAMIKAZE ->
                        false // Sécuriser l'ouverture dès qu'elle est atteinte (sauf Kamikaze s'il reste >= 3 dés)
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

        // Fallback si tous les sous-ensembles faisaient dépasser la cible : sélectionner le plus petit sous-ensemble valide
        return bestPlan ?: BotMovePlan(
            indicesToSelect = candidateSubsets.minBy { it.points }.indices,
            nextAction = BotTurnAction.ROLL_AGAIN
        )
    }

    private data class CandidateSelection(
        val indices: List<Int>,
        val points: Int
    )

    private fun generateCandidateSelections(
        freeDiceIndices: List<Int>,
        allDiceFaces: List<Int>,
        config: GameRulesConfig
    ): List<CandidateSelection> {
        val results = mutableMapOf<List<Int>, Int>()
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
                results[chosenIndices] = eval.totalPoints
            }
        }

        return results.map { (indices, pts) -> CandidateSelection(indices, pts) }
    }
}

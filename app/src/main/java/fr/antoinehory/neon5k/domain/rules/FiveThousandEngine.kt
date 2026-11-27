package fr.antoinehory.neon5k.domain.rules

import fr.antoinehory.neon5k.domain.model.DieState
import fr.antoinehory.neon5k.domain.model.EngineResult
import fr.antoinehory.neon5k.domain.model.EntropySnapshot
import fr.antoinehory.neon5k.domain.model.MatchState
import fr.antoinehory.neon5k.domain.model.MoveValidationError
import fr.antoinehory.neon5k.domain.model.PiggybackOffer
import fr.antoinehory.neon5k.domain.model.PlayerState
import fr.antoinehory.neon5k.domain.model.RollHistoryEntry
import fr.antoinehory.neon5k.domain.model.ScoreCheckpoint
import fr.antoinehory.neon5k.domain.model.TurnPhase
import fr.antoinehory.neon5k.domain.model.TurnState

/**
 * Machine à états pure Kotlin du jeu de dés 5000.
 * Aucune dépendance Android : toutes les transitions sont déterministes et immuables.
 */
object FiveThousandEngine {

    private const val MAX_HISTORY_ENTRIES = 45

    /**
     * Vérifie si un pot de tour (`totalTurnPoints`) remplit toutes les conditions de banque
     * pour le joueur `player` selon `rules`.
     */
    fun canPlayerBankPot(
        player: PlayerState,
        totalTurnPoints: Int,
        triggersHotDice: Boolean,
        rules: GameRulesConfig
    ): Boolean = validateBankConditions(player, totalTurnPoints, triggersHotDice, rules) == null

    /**
     * Retourne l'erreur de validation empêchant de banquer `totalTurnPoints`, ou `null` si la banque est légale.
     */
    fun validateBankConditions(
        player: PlayerState,
        totalTurnPoints: Int,
        triggersHotDice: Boolean,
        rules: GameRulesConfig
    ): MoveValidationError? {
        if (totalTurnPoints <= 0) {
            return MoveValidationError.SelectionContainsDeadDiceOrEmpty
        }
        if (triggersHotDice && rules.mandatoryRerollOnHotDice) {
            return MoveValidationError.MandatoryHotDiceRerollRequired
        }
        if (!player.isOpened && totalTurnPoints < rules.openingThreshold) {
            return MoveValidationError.OpeningThresholdNotMet(
                required = rules.openingThreshold,
                current = totalTurnPoints
            )
        }
        if (player.isOpened && rules.minTurnScoreAfterOpening > 0 && totalTurnPoints < rules.minTurnScoreAfterOpening) {
            return MoveValidationError.MinTurnScoreAfterOpeningNotMet(
                required = rules.minTurnScoreAfterOpening,
                current = totalTurnPoints
            )
        }
        if (rules.requireMultipleOf100ToBank && totalTurnPoints % 100 != 0) {
            return MoveValidationError.RoundMultipleOf100Required(
                currentTurnPot = totalTurnPoints
            )
        }
        val projectedScore = player.score + totalTurnPoints
        if (rules.exactFinishRequired && projectedScore > rules.targetScore) {
            return MoveValidationError.TargetOverflowNotAllowed(
                target = rules.targetScore,
                projected = projectedScore
            )
        }
        if (rules.finalSprintLast1000InOneTurn &&
            player.score >= rules.targetScore - 1000 &&
            projectedScore < rules.targetScore
        ) {
            return MoveValidationError.FinalSprint1000Required(
                targetScore = rules.targetScore,
                projectedScore = projectedScore
            )
        }
        return null
    }

    /**
     * Démarre une nouvelle partie avec les joueurs et la configuration de règles fournis.
     */
    fun startNewMatch(
        matchId: String,
        rules: GameRulesConfig,
        players: List<PlayerState>
    ): MatchState {
        require(players.isNotEmpty()) { "A match requires at least 1 player." }
        val initialDice = List(rules.diceCount) { index ->
            DieState(index = index, face = 0, isLocked = false, isSelected = false, canScoreOnCurrentRoll = false)
        }
        val firstPlayer = players.first()
        val initialTurn = TurnState(
            turnNumber = 1,
            activePlayerIndex = 0,
            phase = TurnPhase.AWAITING_INITIAL_ROLL,
            dice = initialDice,
            statusMessageFr = "${firstPlayer.name} — Prêt à lancer ${rules.diceCount} dés"
        )
        return MatchState(
            matchId = matchId,
            rules = rules,
            players = players,
            turn = initialTurn
        )
    }

    /**
     * Exécute un lancer de dés normal (premier lancer du tour, relance après sélection, ou relance obligatoire de Main pleine).
     */
    fun rollDice(
        state: MatchState,
        rolledFaces: List<Int>,
        entropySnapshot: EntropySnapshot? = null
    ): EngineResult {
        if (state.isFinished) {
            return EngineResult.Rejected(state, MoveValidationError.MatchAlreadyFinished)
        }

        val turn = state.turn
        val rules = state.rules

        val diceBeforeRoll: List<DieState>
        val bankedBeforeRoll: Int
        val hotDiceBeforeRoll: Int

        when (turn.phase) {
            TurnPhase.AWAITING_INITIAL_ROLL -> {
                diceBeforeRoll = List(rules.diceCount) { idx ->
                    DieState(index = idx, face = 0, isLocked = false, isSelected = false)
                }
                bankedBeforeRoll = 0
                hotDiceBeforeRoll = 0
            }

            TurnPhase.HOT_DICE_MANDATORY_REROLL -> {
                diceBeforeRoll = List(rules.diceCount) { idx ->
                    DieState(index = idx, face = 0, isLocked = false, isSelected = false)
                }
                bankedBeforeRoll = turn.bankedInTurn
                hotDiceBeforeRoll = turn.hotDiceCountInTurn
            }

            TurnPhase.SELECTING_DICE -> {
                if (!turn.isSelectionValid || turn.selectedPoints <= 0) {
                    return EngineResult.Rejected(state, MoveValidationError.SelectionContainsDeadDiceOrEmpty)
                }
                val newBanked = turn.bankedInTurn + turn.selectedPoints
                val triggersHotDice = turn.selectionTriggersHotDice
                bankedBeforeRoll = newBanked
                hotDiceBeforeRoll = if (triggersHotDice) turn.hotDiceCountInTurn + 1 else turn.hotDiceCountInTurn
                diceBeforeRoll = if (triggersHotDice) {
                    List(rules.diceCount) { idx ->
                        DieState(index = idx, face = 0, isLocked = false, isSelected = false)
                    }
                } else {
                    turn.dice.map { die ->
                        if (die.isLocked || die.isSelected) {
                            die.copy(isLocked = true, isSelected = false, canScoreOnCurrentRoll = false)
                        } else {
                            die.copy(isLocked = false, isSelected = false, canScoreOnCurrentRoll = false)
                        }
                    }
                }
            }

            TurnPhase.BUSTED, TurnPhase.OVERFLOW_BUSTED -> {
                return EngineResult.Rejected(state, MoveValidationError.InvalidPhaseForAction)
            }
        }

        val freeDiceIndices = diceBeforeRoll.mapIndexedNotNull { idx, die -> if (!die.isLocked) idx else null }
        require(rolledFaces.size >= freeDiceIndices.size) {
            "Not enough rolled faces provided: needed ${freeDiceIndices.size}, got ${rolledFaces.size}"
        }
        val activeRollFaces = freeDiceIndices.mapIndexed { i, _ ->
            val face = rolledFaces[i]
            require(face in 1..6) { "Rolled face must be in 1..6 (was $face)" }
            face
        }

        return applyRollToFreeDice(
            state = state,
            baseDice = diceBeforeRoll,
            freeDiceIndices = freeDiceIndices,
            activeRollFaces = activeRollFaces,
            bankedBeforeRoll = bankedBeforeRoll,
            hotDiceBeforeRoll = hotDiceBeforeRoll,
            usedPiggyback = turn.usedPiggyback,
            entropySnapshot = entropySnapshot
        )
    }

    /**
     * Exécute un lancer en acceptant l'offre de Repiquage (`piggybackOffer`) laissée par le joueur précédent.
     */
    fun acceptPiggybackAndRoll(
        state: MatchState,
        rolledFaces: List<Int>,
        entropySnapshot: EntropySnapshot? = null
    ): EngineResult {
        if (state.isFinished) {
            return EngineResult.Rejected(state, MoveValidationError.MatchAlreadyFinished)
        }
        val turn = state.turn
        val rules = state.rules
        if (turn.phase != TurnPhase.AWAITING_INITIAL_ROLL) {
            return EngineResult.Rejected(state, MoveValidationError.InvalidPhaseForAction)
        }
        val offer = turn.piggybackOffer
        if (!rules.piggybackEnabled || offer == null) {
            return EngineResult.Rejected(state, MoveValidationError.PiggybackNotAvailable)
        }
        if (!state.activePlayer.isOpened) {
            return EngineResult.Rejected(state, MoveValidationError.MustBeOpenedToPiggyback)
        }

        val lockedCount = rules.diceCount - offer.remainingDiceCount
        val baseDice = List(rules.diceCount) { idx ->
            DieState(
                index = idx,
                face = if (idx < lockedCount) 1 else 0,
                isLocked = idx < lockedCount,
                isSelected = false,
                canScoreOnCurrentRoll = false
            )
        }
        val freeDiceIndices = (lockedCount until rules.diceCount).toList()
        require(rolledFaces.size >= freeDiceIndices.size) {
            "Not enough rolled faces for piggyback: needed ${freeDiceIndices.size}, got ${rolledFaces.size}"
        }
        val activeRollFaces = freeDiceIndices.mapIndexed { i, _ ->
            val face = rolledFaces[i]
            require(face in 1..6) { "Rolled face must be in 1..6 (was $face)" }
            face
        }

        return applyRollToFreeDice(
            state = state,
            baseDice = baseDice,
            freeDiceIndices = freeDiceIndices,
            activeRollFaces = activeRollFaces,
            bankedBeforeRoll = offer.inheritedPoints,
            hotDiceBeforeRoll = 0,
            usedPiggyback = true,
            entropySnapshot = entropySnapshot
        )
    }

    /**
     * Bascule la sélection d'un dé libre sur le lancer courant et recalcule instantanément la validité et les points.
     */
    fun toggleDieSelection(
        state: MatchState,
        dieIndex: Int
    ): EngineResult {
        if (state.isFinished) {
            return EngineResult.Rejected(state, MoveValidationError.MatchAlreadyFinished)
        }
        val turn = state.turn
        if (turn.phase != TurnPhase.SELECTING_DICE) {
            return EngineResult.Rejected(state, MoveValidationError.InvalidPhaseForAction)
        }
        val targetDie = turn.dice.getOrNull(dieIndex)
            ?: return EngineResult.Rejected(state, MoveValidationError.DieNotSelectable)
        if (targetDie.isLocked || !targetDie.canScoreOnCurrentRoll) {
            return EngineResult.Rejected(state, MoveValidationError.DieNotSelectable)
        }

        val updatedDice = turn.dice.mapIndexed { idx, die ->
            if (idx == dieIndex) die.copy(isSelected = !die.isSelected) else die
        }
        return EngineResult.Success(recalculateSelectionState(state, updatedDice))
    }

    /**
     * Sélectionne automatiquement le meilleur sous-ensemble complet de dés scorants du lancer courant.
     */
    fun selectAllScoringDice(state: MatchState): EngineResult {
        if (state.isFinished) {
            return EngineResult.Rejected(state, MoveValidationError.MatchAlreadyFinished)
        }
        val turn = state.turn
        if (turn.phase != TurnPhase.SELECTING_DICE) {
            return EngineResult.Rejected(state, MoveValidationError.InvalidPhaseForAction)
        }
        val freeDice = turn.dice.filter { !it.isLocked }
        val freeFaces = freeDice.map { it.face }
        val bestFreePositions = DiceScorer.findBestValidSubsetIndices(freeFaces, state.rules)
        val bestDieIndices = bestFreePositions.map { pos -> freeDice[pos].index }.toSet()

        val updatedDice = turn.dice.map { die ->
            if (!die.isLocked) {
                die.copy(isSelected = die.index in bestDieIndices)
            } else {
                die
            }
        }
        return EngineResult.Success(recalculateSelectionState(state, updatedDice))
    }

    /**
     * Banque les points accumulés au cours du tour et passe au joueur suivant (ou termine la partie si le score cible est atteint).
     */
    fun bankTurn(state: MatchState): EngineResult {
        if (state.isFinished) {
            return EngineResult.Rejected(state, MoveValidationError.MatchAlreadyFinished)
        }
        val turn = state.turn
        val rules = state.rules
        val player = state.activePlayer

        val totalTurnPoints: Int
        val remainingFreeDiceCount: Int
        val triggersHotDice: Boolean

        when (turn.phase) {
            TurnPhase.SELECTING_DICE -> {
                if (!turn.isSelectionValid || turn.selectedPoints <= 0) {
                    return EngineResult.Rejected(state, MoveValidationError.SelectionContainsDeadDiceOrEmpty)
                }
                totalTurnPoints = turn.bankedInTurn + turn.selectedPoints
                remainingFreeDiceCount = turn.remainingDiceAfterSelection
                triggersHotDice = turn.selectionTriggersHotDice
            }

            TurnPhase.HOT_DICE_MANDATORY_REROLL -> {
                totalTurnPoints = turn.bankedInTurn
                remainingFreeDiceCount = 0
                triggersHotDice = true
            }

            else -> return EngineResult.Rejected(state, MoveValidationError.InvalidPhaseForAction)
        }

        val validationError = validateBankConditions(
            player = player,
            totalTurnPoints = totalTurnPoints,
            triggersHotDice = triggersHotDice,
            rules = rules
        )
        if (validationError != null) {
            return EngineResult.Rejected(state, validationError)
        }

        val finalScore = player.score + totalTurnPoints

        // Application de la variante Chapeau / Écrasement (bumpOpponentMode) si un adversaire possède exactement ce score (> 0)
        var bumpedPlayerName: String? = null
        val updatedPlayers = state.players.mapIndexed { idx, p ->
            when {
                idx == turn.activePlayerIndex -> {
                    val currentStack = p.normalizedCheckpoints()
                    val newStack = currentStack + ScoreCheckpoint(
                        score = finalScore,
                        strikes = 0
                    )
                    val newPreviousScore = currentStack.lastOrNull()?.score ?: 0
                    p.copy(
                        previousScore = newPreviousScore,
                        score = finalScore,
                        isOpened = true,
                        consecutiveBusts = 0,
                        checkpoints = newStack
                    )
                }

                rules.bumpOpponentMode != BumpOpponentMode.DISABLED && finalScore > 0 && p.score == finalScore -> {
                    bumpedPlayerName = p.name
                    val currentStack = p.normalizedCheckpoints()
                    when (rules.bumpOpponentMode) {
                        BumpOpponentMode.DISABLED -> p
                        BumpOpponentMode.MINUS_500 -> {
                            val newOpponentScore = (p.score - 500).coerceAtLeast(0)
                            val updatedStack = if (currentStack.isEmpty()) {
                                listOf(ScoreCheckpoint(newOpponentScore, 0))
                            } else {
                                currentStack.dropLast(1) + ScoreCheckpoint(newOpponentScore, 0)
                            }
                            val prevScore = updatedStack.dropLast(1).lastOrNull()?.score ?: 0
                            p.copy(
                                score = newOpponentScore,
                                previousScore = prevScore,
                                consecutiveBusts = 0,
                                isOpened = if (newOpponentScore == 0 && rules.openingThreshold > 0) false else p.isOpened,
                                checkpoints = updatedStack
                            )
                        }
                        BumpOpponentMode.RESET_TO_PREVIOUS -> {
                            val poppedStack = currentStack.dropLast(1)
                            val restored = poppedStack.lastOrNull()
                            val newOpponentScore = restored?.score ?: 0
                            val prevScore = poppedStack.dropLast(1).lastOrNull()?.score ?: 0
                            val restoredStrikes = restored?.strikes ?: 0
                            p.copy(
                                score = newOpponentScore,
                                previousScore = prevScore,
                                consecutiveBusts = restoredStrikes,
                                isOpened = if (newOpponentScore == 0 && rules.openingThreshold > 0) false else p.isOpened,
                                checkpoints = poppedStack
                            )
                        }
                    }
                }

                else -> p
            }
        }

        val bumpSuffix = bumpedPlayerName?.let { " • Chapeau sur $it !" }.orEmpty()
        val bankHistoryEntry = RollHistoryEntry(
            turnNumber = turn.turnNumber,
            rollNumber = turn.rollIndexInTurn,
            playerId = player.id,
            playerName = player.name,
            rolledFaces = turn.dice.filter { !it.isLocked && it.isSelected }.map { it.face },
            summaryFr = "BANQUE +$totalTurnPoints pts → Palier $finalScore pts$bumpSuffix",
            playerScoreAfter = finalScore,
            playerStrikesAfter = 0,
            isBust = false,
            isBank = true,
            isHotDice = triggersHotDice
        )
        val updatedLog = appendHistory(state.rollHistoryLog, bankHistoryEntry)

        val hasWon = if (rules.exactFinishRequired) {
            finalScore == rules.targetScore
        } else {
            finalScore >= rules.targetScore
        }

        if (hasWon) {
            val winTurn = turn.copy(
                bankedInTurn = totalTurnPoints,
                selectedPoints = 0,
                statusMessageFr = "${player.name} remporte la partie avec $finalScore pts !"
            )
            return EngineResult.Success(
                state.copy(
                    players = updatedPlayers,
                    turn = winTurn,
                    winnerPlayerId = player.id,
                    isInstantWin = false,
                    rollHistoryLog = updatedLog
                )
            )
        }

        val nextPiggybackOffer = if (rules.piggybackEnabled && remainingFreeDiceCount in 1 until rules.diceCount) {
            PiggybackOffer(
                inheritedPoints = totalTurnPoints,
                remainingDiceCount = remainingFreeDiceCount,
                fromPlayerName = player.name
            )
        } else {
            null
        }

        val nextState = advanceToNextPlayer(
            state = state.copy(players = updatedPlayers, rollHistoryLog = updatedLog),
            piggybackOffer = nextPiggybackOffer,
            transitionNote = "${player.name} banque +$totalTurnPoints pts$bumpSuffix."
        )
        return EngineResult.Success(nextState)
    }

    /**
     * Accuse réception d'un Coup nul (`BUSTED` ou `OVERFLOW_BUSTED`), ajoute une encoche (`✕`) sur le palier courant,
     * applique l'éventuelle pénalité de 3 encoches (`threeStrikesMode`), et passe la main au joueur suivant.
     */
    fun acknowledgeBustAndEndTurn(state: MatchState): EngineResult {
        if (state.isFinished) {
            return EngineResult.Rejected(state, MoveValidationError.MatchAlreadyFinished)
        }
        val turn = state.turn
        if (turn.phase != TurnPhase.BUSTED && turn.phase != TurnPhase.OVERFLOW_BUSTED) {
            return EngineResult.Rejected(state, MoveValidationError.InvalidPhaseForAction)
        }

        val rules = state.rules
        val player = state.activePlayer
        val currentStack = player.normalizedCheckpoints()
        val newBustCount = player.consecutiveBusts + 1
        val triggersThreeStrikes = rules.threeStrikesMode != ThreeStrikesMode.DISABLED && newBustCount >= 3

        val updatedPlayer: PlayerState
        val note: String

        if (!triggersThreeStrikes) {
            val updatedStack = if (currentStack.isEmpty()) {
                listOf(ScoreCheckpoint(score = player.score, strikes = newBustCount))
            } else {
                currentStack.dropLast(1) + currentStack.last().copy(strikes = newBustCount)
            }
            updatedPlayer = player.copy(
                consecutiveBusts = newBustCount,
                checkpoints = updatedStack
            )
            val strikesBadge = if (rules.threeStrikesMode != ThreeStrikesMode.DISABLED) " (Encoche $newBustCount/3)" else ""
            note = "${player.name} termine son tour à 0 pt$strikesBadge."
        } else {
            when (rules.threeStrikesMode) {
                ThreeStrikesMode.DISABLED -> {
                    updatedPlayer = player.copy(consecutiveBusts = newBustCount)
                    note = "${player.name} termine son tour à 0 pt."
                }
                ThreeStrikesMode.MINUS_500, ThreeStrikesMode.MINUS_1000 -> {
                    val penalty = if (rules.threeStrikesMode == ThreeStrikesMode.MINUS_1000) 1000 else 500
                    val penalizedScore = (player.score - penalty).coerceAtLeast(0)
                    val updatedStack = if (currentStack.isEmpty()) {
                        listOf(ScoreCheckpoint(score = penalizedScore, strikes = 0))
                    } else {
                        currentStack.dropLast(1) + ScoreCheckpoint(score = penalizedScore, strikes = 0)
                    }
                    val prevScore = updatedStack.dropLast(1).lastOrNull()?.score ?: 0
                    updatedPlayer = player.copy(
                        score = penalizedScore,
                        previousScore = prevScore,
                        consecutiveBusts = 0,
                        isOpened = if (penalizedScore == 0 && rules.openingThreshold > 0) false else player.isOpened,
                        checkpoints = updatedStack
                    )
                    note = "${player.name} cumule 3 encoches (-$penalty pts → $penalizedScore pts) !"
                }
                ThreeStrikesMode.REVERT_TO_PREVIOUS_CLEAR_STRIKES -> {
                    val poppedStack = currentStack.dropLast(1)
                    if (poppedStack.isEmpty()) {
                        updatedPlayer = player.copy(
                            score = 0,
                            previousScore = 0,
                            consecutiveBusts = 0,
                            isOpened = rules.openingThreshold == 0,
                            checkpoints = emptyList()
                        )
                        note = "${player.name} cumule 3 encoches (retour à 0 pt) !"
                    } else {
                        val restoredTop = poppedStack.last().copy(strikes = 0)
                        val updatedStack = poppedStack.dropLast(1) + restoredTop
                        val prevScore = updatedStack.dropLast(1).lastOrNull()?.score ?: 0
                        updatedPlayer = player.copy(
                            score = restoredTop.score,
                            previousScore = prevScore,
                            consecutiveBusts = 0,
                            isOpened = if (restoredTop.score == 0 && rules.openingThreshold > 0) false else player.isOpened,
                            checkpoints = updatedStack
                        )
                        note = "${player.name} cumule 3 encoches (recul au palier ${restoredTop.score} pts, 0 encoche) !"
                    }
                }
                ThreeStrikesMode.REVERT_TO_PREVIOUS_PERSIST_STRIKES -> {
                    val poppedStack = currentStack.dropLast(1)
                    if (poppedStack.isEmpty()) {
                        updatedPlayer = player.copy(
                            score = 0,
                            previousScore = 0,
                            consecutiveBusts = 0,
                            isOpened = rules.openingThreshold == 0,
                            checkpoints = emptyList()
                        )
                        note = "${player.name} cumule 3 encoches (retour à 0 pt) !"
                    } else {
                        val restoredTop = poppedStack.last()
                        val prevScore = poppedStack.dropLast(1).lastOrNull()?.score ?: 0
                        updatedPlayer = player.copy(
                            score = restoredTop.score,
                            previousScore = prevScore,
                            consecutiveBusts = restoredTop.strikes,
                            isOpened = if (restoredTop.score == 0 && rules.openingThreshold > 0) false else player.isOpened,
                            checkpoints = poppedStack
                        )
                        note = "${player.name} cumule 3 encoches (recul au palier ${restoredTop.score} pts avec ${restoredTop.strikes} encoche(s)) !"
                    }
                }
            }
        }

        val updatedPlayers = state.players.mapIndexed { idx, p ->
            if (idx == turn.activePlayerIndex) updatedPlayer else p
        }

        val updatedHistory = if (state.rollHistoryLog.isNotEmpty() && state.rollHistoryLog.last().isBust) {
            val last = state.rollHistoryLog.last()
            state.rollHistoryLog.dropLast(1) + last.copy(
                summaryFr = "${last.summaryFr} • $note",
                playerScoreAfter = updatedPlayer.score,
                playerStrikesAfter = updatedPlayer.consecutiveBusts
            )
        } else {
            state.rollHistoryLog
        }

        return EngineResult.Success(
            advanceToNextPlayer(
                state = state.copy(players = updatedPlayers, rollHistoryLog = updatedHistory),
                piggybackOffer = null,
                transitionNote = note
            )
        )
    }

    private fun applyRollToFreeDice(
        state: MatchState,
        baseDice: List<DieState>,
        freeDiceIndices: List<Int>,
        activeRollFaces: List<Int>,
        bankedBeforeRoll: Int,
        hotDiceBeforeRoll: Int,
        usedPiggyback: Boolean,
        entropySnapshot: EntropySnapshot?
    ): EngineResult {
        val rules = state.rules
        val player = state.activePlayer
        val nextRollIndex = state.turn.rollIndexInTurn + 1
        val nextTotalRolls = state.totalRollsInMatch + 1

        val scoringFreePositions = DiceScorer.findScoringDieIndices(activeRollFaces, rules)
        val freePosMap = freeDiceIndices.withIndex().associate { (pos, dieIdx) -> dieIdx to pos }

        val updatedDice = baseDice.mapIndexed { dieIdx, die ->
            val freePos = freePosMap[dieIdx]
            if (freePos == null) {
                die.copy(isSelected = false, canScoreOnCurrentRoll = false)
            } else {
                val face = activeRollFaces[freePos]
                val canScore = freePos in scoringFreePositions
                die.copy(
                    face = face,
                    isLocked = false,
                    isSelected = false,
                    canScoreOnCurrentRoll = canScore
                )
            }
        }

        // 1. Cas Coup nul (aucun dé libre ne score)
        if (scoringFreePositions.isEmpty()) {
            val projectedStrikes = (player.consecutiveBusts + 1).coerceAtMost(3)
            val bustEntry = RollHistoryEntry(
                turnNumber = state.turn.turnNumber,
                rollNumber = nextRollIndex,
                playerId = player.id,
                playerName = player.name,
                rolledFaces = activeRollFaces,
                summaryFr = "GLITCH (0 pt) — Pot de $bankedBeforeRoll pts perdu",
                playerScoreAfter = player.score,
                playerStrikesAfter = projectedStrikes,
                isBust = true
            )
            val bustedTurn = state.turn.copy(
                phase = TurnPhase.BUSTED,
                dice = updatedDice,
                bankedInTurn = bankedBeforeRoll,
                selectedPoints = 0,
                selectedCombinations = emptyList(),
                isSelectionValid = false,
                rollIndexInTurn = nextRollIndex,
                hotDiceCountInTurn = hotDiceBeforeRoll,
                piggybackOffer = null,
                usedPiggyback = usedPiggyback,
                lastEntropySnapshot = entropySnapshot,
                statusMessageFr = "GLITCH ! Aucun dé scorant — ${player.name} perd le pot du tour."
            )
            return EngineResult.Success(
                state.copy(
                    turn = bustedTurn,
                    totalRollsInMatch = nextTotalRolls,
                    rollHistoryLog = appendHistory(state.rollHistoryLog, bustEntry)
                )
            )
        }

        // 2. Vérifier la victoire instantanée par 5 As en un seul lancer (si activée)
        val maxEval = DiceScorer.evaluateMaxScoringSubset(activeRollFaces, rules)
        if (maxEval.triggersInstantWin) {
            val winningScore = player.score.coerceAtLeast(rules.targetScore)
            val updatedPlayers = state.players.mapIndexed { idx, p ->
                if (idx == state.turn.activePlayerIndex) {
                    p.copy(previousScore = p.score, score = winningScore, isOpened = true, consecutiveBusts = 0)
                } else {
                    p
                }
            }
            val winDice = updatedDice.map { die ->
                if (!die.isLocked && die.canScoreOnCurrentRoll) die.copy(isSelected = true) else die
            }
            val winEntry = RollHistoryEntry(
                turnNumber = state.turn.turnNumber,
                rollNumber = nextRollIndex,
                playerId = player.id,
                playerName = player.name,
                rolledFaces = activeRollFaces,
                summaryFr = "5 AS ! VICTOIRE INSTANTANÉE ($winningScore pts)",
                playerScoreAfter = winningScore,
                playerStrikesAfter = 0,
                isBank = true,
                isHotDice = true
            )
            val winTurn = state.turn.copy(
                phase = TurnPhase.SELECTING_DICE,
                dice = winDice,
                bankedInTurn = bankedBeforeRoll,
                selectedPoints = maxEval.totalPoints,
                selectedCombinations = maxEval.combinations,
                isSelectionValid = true,
                rollIndexInTurn = nextRollIndex,
                hotDiceCountInTurn = hotDiceBeforeRoll,
                piggybackOffer = null,
                usedPiggyback = usedPiggyback,
                lastEntropySnapshot = entropySnapshot,
                statusMessageFr = "QUINTE D'AS ! Victoire instantanée de ${player.name} !"
            )
            return EngineResult.Success(
                state.copy(
                    players = updatedPlayers,
                    turn = winTurn,
                    winnerPlayerId = player.id,
                    isInstantWin = true,
                    totalRollsInMatch = nextTotalRolls,
                    rollHistoryLog = appendHistory(state.rollHistoryLog, winEntry)
                )
            )
        }

        // 3. Vérifier si même le plus petit score sélectionnable sur ce lancer fait dépasser targetScore (avec exactFinishRequired)
        val minPossibleScoreOnRoll = computeMinValidScoreOnRoll(activeRollFaces, rules)
        if (rules.exactFinishRequired && player.score + bankedBeforeRoll + minPossibleScoreOnRoll > rules.targetScore) {
            val projectedStrikes = (player.consecutiveBusts + 1).coerceAtMost(3)
            val overflowEntry = RollHistoryEntry(
                turnNumber = state.turn.turnNumber,
                rollNumber = nextRollIndex,
                playerId = player.id,
                playerName = player.name,
                rolledFaces = activeRollFaces,
                summaryFr = "DÉPASSEMENT (> ${rules.targetScore} pts) — Tour annulé",
                playerScoreAfter = player.score,
                playerStrikesAfter = projectedStrikes,
                isBust = true
            )
            val overflowTurn = state.turn.copy(
                phase = TurnPhase.OVERFLOW_BUSTED,
                dice = updatedDice,
                bankedInTurn = bankedBeforeRoll,
                selectedPoints = 0,
                selectedCombinations = emptyList(),
                isSelectionValid = false,
                rollIndexInTurn = nextRollIndex,
                hotDiceCountInTurn = hotDiceBeforeRoll,
                piggybackOffer = null,
                usedPiggyback = usedPiggyback,
                lastEntropySnapshot = entropySnapshot,
                statusMessageFr = "DÉPASSEMENT ! Score cible (${rules.targetScore}) dépassé — Tour annulé."
            )
            return EngineResult.Success(
                state.copy(
                    turn = overflowTurn,
                    totalRollsInMatch = nextTotalRolls,
                    rollHistoryLog = appendHistory(state.rollHistoryLog, overflowEntry)
                )
            )
        }

        // 4. Lancer normal avec au moins une sélection valide possible
        val isHotRoll = scoringFreePositions.size == activeRollFaces.size
        val combosSummary = maxEval.combinations.joinToString(" + ") { it.labelFr }
        val rollEntry = RollHistoryEntry(
            turnNumber = state.turn.turnNumber,
            rollNumber = nextRollIndex,
            playerId = player.id,
            playerName = player.name,
            rolledFaces = activeRollFaces,
            summaryFr = "$combosSummary (max +${maxEval.totalPoints} pts${if (isHotRoll) " ★ Main pleine" else ""})",
            playerScoreAfter = player.score,
            playerStrikesAfter = player.consecutiveBusts,
            isHotDice = isHotRoll
        )

        val selectingTurn = state.turn.copy(
            phase = TurnPhase.SELECTING_DICE,
            dice = updatedDice,
            bankedInTurn = bankedBeforeRoll,
            selectedPoints = 0,
            selectedCombinations = emptyList(),
            isSelectionValid = false,
            rollIndexInTurn = nextRollIndex,
            hotDiceCountInTurn = hotDiceBeforeRoll,
            piggybackOffer = null,
            usedPiggyback = usedPiggyback,
            lastEntropySnapshot = entropySnapshot,
            statusMessageFr = "Sélectionnez vos dés scorants (max +${maxEval.totalPoints} pts)"
        )
        return EngineResult.Success(
            state.copy(
                turn = selectingTurn,
                totalRollsInMatch = nextTotalRolls,
                rollHistoryLog = appendHistory(state.rollHistoryLog, rollEntry)
            )
        )
    }

    private fun appendHistory(
        current: List<RollHistoryEntry>,
        entry: RollHistoryEntry
    ): List<RollHistoryEntry> {
        val next = current + entry
        return if (next.size > MAX_HISTORY_ENTRIES) {
            next.takeLast(MAX_HISTORY_ENTRIES)
        } else {
            next
        }
    }

    private fun computeMinValidScoreOnRoll(
        activeRollFaces: List<Int>,
        rules: GameRulesConfig
    ): Int {
        if (5 in activeRollFaces) return 50
        if (1 in activeRollFaces) return 100

        val counts = IntArray(7)
        for (f in activeRollFaces) counts[f]++
        val triples = (2..6).filter { counts[it] >= 3 }.map { DiceScorer.tripleBasePoints(it) }
        if (triples.isNotEmpty()) return triples.min()

        return DiceScorer.evaluateMaxScoringSubset(activeRollFaces, rules).totalPoints
    }

    private fun recalculateSelectionState(
        state: MatchState,
        updatedDice: List<DieState>
    ): MatchState {
        val selectedFaces = updatedDice.filter { !it.isLocked && it.isSelected }.map { it.face }
        val eval = DiceScorer.evaluateSelection(selectedFaces, state.rules)

        val message = when {
            selectedFaces.isEmpty() -> "Sélectionnez au moins un dé scorant."
            !eval.isValid -> "Sélection invalide : retirez les dés qui ne forment pas une combinaison complète."
            else -> {
                val combosText = eval.combinations.joinToString(" + ") { it.labelFr }
                "$combosText (+${eval.totalPoints} pts)"
            }
        }

        val updatedTurn = state.turn.copy(
            dice = updatedDice,
            selectedPoints = eval.totalPoints,
            selectedCombinations = if (eval.isValid) eval.combinations else emptyList(),
            isSelectionValid = eval.isValid,
            statusMessageFr = message
        )
        return state.copy(turn = updatedTurn)
    }

    private fun advanceToNextPlayer(
        state: MatchState,
        piggybackOffer: PiggybackOffer?,
        transitionNote: String
    ): MatchState {
        val nextPlayerIndex = (state.turn.activePlayerIndex + 1) % state.players.size
        val nextPlayer = state.players[nextPlayerIndex]
        val freshDice = List(state.rules.diceCount) { idx ->
            DieState(index = idx, face = 0, isLocked = false, isSelected = false, canScoreOnCurrentRoll = false)
        }
        val canPiggyback = piggybackOffer != null && state.rules.piggybackEnabled && nextPlayer.isOpened
        val effectiveOffer = if (canPiggyback) piggybackOffer else null

        val nextTurn = TurnState(
            turnNumber = state.turn.turnNumber + 1,
            activePlayerIndex = nextPlayerIndex,
            phase = TurnPhase.AWAITING_INITIAL_ROLL,
            dice = freshDice,
            piggybackOffer = effectiveOffer,
            lastEntropySnapshot = state.turn.lastEntropySnapshot,
            statusMessageFr = if (effectiveOffer != null) {
                "$transitionNote Tour de ${nextPlayer.name} (Repiquage possible : ${effectiveOffer.inheritedPoints} pts / ${effectiveOffer.remainingDiceCount} dés)"
            } else {
                "$transitionNote Tour de ${nextPlayer.name}"
            }
        )
        return state.copy(turn = nextTurn)
    }
}

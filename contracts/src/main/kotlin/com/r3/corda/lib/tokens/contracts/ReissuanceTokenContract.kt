package com.r3.corda.lib.tokens.contracts

import com.r3.corda.lib.tokens.contracts.states.NonceState
import com.r3.corda.lib.tokens.contracts.states.ProofOfBurn
import com.r3.corda.lib.tokens.contracts.states.ReissuanceToken
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey


class ReissuanceTokenContract : Contract {

    interface ReissuanceTokenCommands : CommandData

    class IssueReissuanceTokenCommand : ReissuanceTokenCommands

    class MoveReissuanceTokenCommand : ReissuanceTokenCommands

    class UseReissuanceTokenCommand : ReissuanceTokenCommands

    override fun verify(tx: LedgerTransaction) {
        // One command only for now. Should make more flexible later
        val command = tx.commands.requireSingleCommand<ReissuanceTokenCommands>()
        val setOfSigners = command.signers.toSet()
        when (command.value) {
            is IssueReissuanceTokenCommand -> verifyIssue(tx)
            is MoveReissuanceTokenCommand -> verifyMove(tx, setOfSigners)
            is UseReissuanceTokenCommand -> verifyUse(tx, setOfSigners)
            else -> throw IllegalArgumentException("Unrecognised command.")
        }

    }

    /**
     * A reissuance token can only be created in a transaction which consumes a nonce state as input.
     * The reissuKey on the reissuance token output must be set to the StateRef of the consumed nonce state.
     *
     * There are no required signers on issuance as the owner of the new reissuance token can simply ignore the
     * transaction if she decides she does not want the asset that has been given her.
     *
     * For simple example purposes, can only create one reissuance token at a time for now
     **/
    private fun verifyIssue(tx: LedgerTransaction) = requireThat {
        "No reissuance token inputs are consumed when issuing a new reissuance token" using
                (tx.inputsOfType<ReissuanceToken>().isEmpty())

        "The single reissuance token reissueKey equals the StateRef of the single consumed nonce state" using (
                tx.outRefsOfType<ReissuanceToken>().single().state.data.reissueKey == tx.inRefsOfType<NonceState>().single().ref)
    }

    /**
     * For now, only one ReissuanceToken can move at a time and the entire amount must be transferred
     * However, the outputs can be split to better prove out the overall concept
     * */
    private fun verifyMove(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {

        val reissuanceInput = tx.inputsOfType<ReissuanceToken>().single()

        "Current owner on single ReissuanceToken input is in list of signers" using
                (reissuanceInput.owner.owningKey in signers)

        val reissuanceOutputs = tx.outputsOfType<ReissuanceToken>()

        "Combined amount on ReissuanceToken outputs is less than or equal to the ReissuanceToken input" using
                (reissuanceOutputs.map { it.amount }.sum() <= reissuanceInput.amount)
    }

    /**
     * The reissuance contract doesn't care whether the ReissuanceToken is actually being used for anything
     * As long as the reissuance token is exited from ledger, this contract is happy
     * The logic about whether exiting this token gives the spending party the right to issue other tokens will be handled
     * in the contract of the relevant burned state (normally a token state)
     *
     * Again, only for purposes of example, we assume for now that only one reissuance is used at once. However, there
     * can be change returned if the amount burned is less than the amount on the ReissuanceToken
     */
    private fun verifyUse(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        val reissuanceInput = tx.inputsOfType<ReissuanceToken>().single()

        "One reissuance token is consumed as input" using (tx.inputsOfType<ReissuanceToken>().size == 1)
        "One reissuance token is created on output" using (tx.outputsOfType<ReissuanceToken>().size <= 1)

        "Owner of reissuance input is on the list of signers" using
                (reissuanceInput.owner.owningKey in signers)

        "The reissuance output lists the referenced ProofOfBurn as one of the used burns" using
                (tx.referenceInputRefsOfType<ProofOfBurn>().single().state in tx.outputsOfType<ReissuanceToken>().first().usedProofs)


    }
}
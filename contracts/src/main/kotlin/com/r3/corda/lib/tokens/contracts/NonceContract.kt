package com.r3.corda.lib.tokens.contracts

import com.r3.corda.lib.tokens.contracts.states.NonceState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

/**
 * This class is one of a few ways to leverage implicit trust in the notary to ensure uniqueness of state identifiers.
 * In cases where the uniqueness of a certain field on a state is paramount, issuance transaction will include a nonce
 * state as input and use its consumed [StateRef] as an ID such that the notary would reject any attempt to use the same
 * ID multiple times.
 */

class NonceContract : Contract {

    companion object {
        @JvmStatic
        val ID: ContractClassName = this::class.java.enclosingClass.canonicalName
    }

    interface NonceStateCommands : CommandData

    class IssueNonceStateCommand : NonceStateCommands

    class UseNonceStateCommand : NonceStateCommands

    /**
     * Simple function to ensure no nonce states are used during issuance and none are issued while using others
     *
     * Neither command contains any required signers as the only attestation we care about is that of the notary.
     */
    override fun verify(tx: LedgerTransaction) {

        val command = tx.commands.requireSingleCommand<NonceStateCommands>()
        when (command.value) {
            is IssueNonceStateCommand -> verifyIssue(tx)
            is UseNonceStateCommand -> verifyUse(tx)
            else -> throw IllegalArgumentException("Unrecognised command.")
        }

    }

    private fun verifyIssue(tx: LedgerTransaction) = requireThat {
        "No nonce state inputs should be consumed when issuing new nonce states" using
                (tx.inputsOfType<NonceState>().isEmpty())
    }

    private fun verifyUse(tx: LedgerTransaction) = requireThat {
        "At least one nonce state should be consumed" using
                (tx.inputsOfType<NonceState>().isNotEmpty())

        "No nonce state outputs should be created when a nonce state is being used as an input" using
                (tx.outputsOfType<NonceState>().isEmpty())
    }
}
package com.r3.corda.lib.tokens.contracts

import com.r3.corda.lib.tokens.contracts.states.ProofOfBurn
import com.r3.corda.lib.tokens.contracts.states.ReissuableState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

/**
 * This class is the [ProofOfBurn] contract. It has only one command as [ProofOfBurn] states, once issued, can never
 * be spent subsequently in order to effectively burn the states that are encumbered by it.
 * The contract's main job is to ensure that the data on the newly issued [ProofOfBurn] matches the relevant fields
 * on the burned input transaction states (state data, attachment constraint, notary). This matching needs to be enforced
 * so that reissuers cannot create states that look at all different to the ones that were burned
 */

class ProofOfBurnContract : Contract {

    companion object {
        @JvmStatic
        val contractId = ProofOfBurnContract::class.qualifiedName!!
    }

    class IssueBurnCommand : TypeOnlyCommandData()

    override fun verify(tx: LedgerTransaction) {

        tx.commands.requireSingleCommand<IssueBurnCommand>()

        require(tx.inputsOfType<ProofOfBurn>().isEmpty()) { "Proof of burn states are never consumed" }
        // For now, can only create one ProofOfBurn in a given transaction. This may need to be more flexible long term
        // i.e. we should group them by their stated purpose (burnPurpose: StateRef)
        val proofOfBurnOutputRef = tx.outRefsOfType<ProofOfBurn>().single()

        require(tx.outRef<ReissuableState>(proofOfBurnOutputRef.state.data.burnedState.second).state.encumbrance
                == proofOfBurnOutputRef.ref.index) { "The output ProofOfBurn claims was burned is encumbered by ProofOfBurn" }
    }
}
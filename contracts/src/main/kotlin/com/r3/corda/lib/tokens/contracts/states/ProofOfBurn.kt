package com.r3.corda.lib.tokens.contracts.states

import com.r3.corda.lib.tokens.contracts.ProofOfBurnContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.identity.AbstractParty

/**
 * This class represents the fact that some other [ContractState] has been burned in a transaction. That other state is
 * considered validly burned if its output is encumbered by this [ProofOfBurn] output (which, as a state that can only
 * ever be issued, locks any encumbered states forever).
 *
 * @property burnedStateIndex the index of the [ContractState] in the transaction by which it was burned.
 * @property purposeOfBurn the identifier for the reason the party is burning some state (i.e. to settle an obligation)
 */

@BelongsToContract(ProofOfBurnContract::class)
data class ProofOfBurn(val burnedStateIndex: Int, val purposeOfBurn: StateRef) : ContractState {
    override val participants: List<AbstractParty> get() = emptyList()
}
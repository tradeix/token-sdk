package com.r3.corda.lib.tokens.contracts.states

import com.r3.corda.lib.tokens.contracts.NonceStateContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import java.util.*

/**
 * This state is a simple way to leverage the notary as a mechanism to ensure uniqueness of state identifiers.
 * The state can only ever be issued and exited from ledger; there is no other evolution required or allowed.
 * This state may be better placed outside the tokens SDK, but is required for a secure burn-and-reissuance pattern so
 * I have included it here.
 *
 * @property nonce a UUID used only to ensure the StateRef is unique. We don't mind if an issuing party wants to be
 * malicious and force in a UUID that has been used before as the notary will refuse to sign a state with the same
 * StateRef when it is used in a subsequent transaction
 */
@BelongsToContract(NonceStateContract::class)
data class NonceState(val nonce: UUID = UUID.randomUUID()) : ContractState {
    override val participants: List<AbstractParty> get() = emptyList()
}
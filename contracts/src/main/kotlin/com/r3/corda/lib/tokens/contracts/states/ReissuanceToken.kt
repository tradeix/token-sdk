package com.r3.corda.lib.tokens.contracts.states

import com.r3.corda.lib.tokens.contracts.ReissuanceTokenContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty

/**
 * This class represents the right of a party to reissue some burned tokens (of some type) by consuming this state in a
 * transaction which uses a [ProofOfBurn] state as a reference input. The stated "purpose of burn" on the
 * referenced [ProofOfBurn] must be equivalent to the reissue key on this state or the tx is invalid.
 * Owner can only reissue a designated portion of the overall tokens which were burned by the referenced [ProofOfBurn].
 *
 * The trick regarding that designated portion is that in order to allow these [ReissuanceToken]s to split to multiple
 * owners (as is one of the core reasons for implementing a burn-and-reissuance pattern to begin with), we need to ensure
 * that the designated portion for each party is absolutely unambiguous (i.e. a simple numerical amount on each
 * [ReissuanceToken] is not sufficient). If two [ReissuanceToken]s believe that both of their respective owners have
 * legitimate right to collect to the same burned funds, money will be minted out of thin air.
 * As an example, if a certain [ReissuanceToken] which starts off with a value of $1000 then splits into ten outputs
 * each with a value of $100, we run into an issue if the paying party burns in some tx only, say, $200. Without additional
 * logic regarding who receives which founds, all ten parties will think they can reissue $100 and $800 is magically printed.
 *
 * To solve this, we can either declare explicitly the priority of collection (i.e. PartyA gets dollars 1-10, 100-110,
 * 200-210, etc.; PartyB 10-20, 110-120, etc.) or allow each party to take a percentage of any incoming funds. Although
 * we may want to support both (sometimes payout definitely is dependent on whether the party at the front of the queue
 * has been paid in full yet) I have chosen here to let each [ReissuanceToken] contain a percentage amount as I think it
 * is more intuitive for most cases.
 *
 * @property owner the party who has the right to spend this state in a token reissuance transaction
 * @property reissueKey a pointer to the [NonceState] consumed as part of the issuance transaction which links the
 * [ReissuanceToken] to one or more [ProofOfBurn] states as attested to by the notary
 * @property amount the percentage of incoming funds this party has the right to collect
 * @property usedProofs the list of [ProofOfBurn] states that have already be used and, therefore, can't be used again
 */

@BelongsToContract(ReissuanceTokenContract::class)
data class ReissuanceToken(
        val owner: AbstractParty,
        val reissueKey: StateRef,
        // Until split, the owner will have the right to reissue 100% of funds burned with the correct burn purpose
        val amount: Long = 100,
        // No burns have been used when the state is first issued
        val usedProofs: List<TransactionState<ProofOfBurn>> = listOf(),

        /** Although normally only the owner, a reissuance token can have an arbitrary set of participants to allow,
         * as an example, for the case where the party burning tokens needs to know who has the right to reissue them */
        override val participants: List<AbstractParty> = listOf()
) : ContractState {

    fun withNewOwner(newOwner: AbstractParty, amount: Long, additionalParticipants: List<AbstractParty> = listOf()): ReissuanceToken {
        return ReissuanceToken(
                newOwner,
                reissueKey,
                amount = amount,
                usedProofs = usedProofs,
                participants = additionalParticipants + newOwner)
    }
}
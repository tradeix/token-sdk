package com.r3.corda.lib.tokens.contracts.states

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty

interface Reissuable: ContractState {

    /**
     * For determining what the owner of the ReissuanceToken to be consumed is allowed to reissue
     * A flow creating a reissuance transaction doesn't need to know what type of token was burned in order to create
     * the reissued output. They can call this function on any Reissuable ContractState
     */
    fun getAllowableReissuanceOutput (
            reissuanceTokenInputs: List<ReissuanceToken>,
            newOwner: AbstractParty
    ): List<Pair<CommandData, Reissuable>>
}
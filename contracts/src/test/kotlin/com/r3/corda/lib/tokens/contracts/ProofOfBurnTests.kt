package com.r3.corda.lib.tokens.contracts

import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.states.ProofOfBurn
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.GBP
import com.r3.corda.lib.tokens.money.USD
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.SecureHash
import net.corda.finance.`issued by`
import net.corda.finance.issuedBy
import org.jgroups.demos.wb.UserInfoDialog
import org.junit.Test

class ProofOfBurnTests : ContractTestCommon() {

    @Test
    fun `issue ProofOfBurn tests where the state ProofOfBurn claims was burned was not`() {

        val token = USD issuedBy ISSUER.party
        val otherToken = GBP issuedBy ISSUER.party
        transaction {
            // start with a ProofOfBurn and a fungible token that is encumbered by the ProofOfBurn
            output(
                    contractClassName = ProofOfBurnContract.ID,
                    encumbrance = 1,
                    contractState = ProofOfBurn(
                            // state and index of the state which we will add later
                            burnedTxState = TransactionState(
                                    data = 800 of token heldBy ALICE.party,
                                    contract = FungibleTokenContract.contractId,
                                    notary = NOTARY.party),
                            // index of the above state (not the one we add next)
                            burnedStateIndex = 2,
                            purposeOfBurn = StateRef(txhash = SecureHash.zeroHash, index = 0)))
            input(
                    contractClassName = FungibleTokenContract.contractId,
                    state = 500 of otherToken heldBy ALICE.party)
            output(
                    contractClassName = FungibleTokenContract.contractId,
                    // encumbered by the ProofOfBurn (which was added first so has index 0)
                    encumbrance = 0,
                    contractState = 500 of otherToken heldBy ALICE.party
            )
            command(ALICE.publicKey, MoveTokenCommand(otherToken, listOf(0), listOf(1)))

            // no command of ProofOfBurn type fails.
            tweak {
                this `fails with` "Required com.r3.corda.lib.tokens.contracts.ProofOfBurnContract.IssueBurnCommand command"
            }
            // Even if other states are encumbered by proof of burn (as is the case with the first token added above),
            // contract fails if the one that ProofOfBurn says is burned is actually not
            tweak {
                input(FungibleTokenContract.contractId, 800 of token heldBy ALICE.party)
                // Note: no encumbrance on this one. This is the state ProofOfBurn claims was burned so this is bad
                // The data is correct but no encumbrance means it wasn't burned and ProofOfBurn is wrong to claim it was
                output(FungibleTokenContract.contractId, 800 of token heldBy ALICE.party)

                command(ALICE.publicKey, MoveTokenCommand(token, listOf(1), listOf(2)))
                command(ALICE.publicKey, ProofOfBurnContract.IssueBurnCommand())
                this `fails with` "The output ProofOfBurn claims was burned is encumbered by ProofOfBurn"
            }
        }
    }
}
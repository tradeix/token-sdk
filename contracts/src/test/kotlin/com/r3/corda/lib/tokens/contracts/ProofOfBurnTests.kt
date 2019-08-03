package com.r3.corda.lib.tokens.contracts

import com.r3.corda.lib.tokens.contracts.states.ProofOfBurn
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.money.USD
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import org.junit.Test

class ProofOfBurnTests : ContractTestCommon() {

    @Test
    fun `issue ProofOfBurn tests`() {

        val burnedToken = USD issuedBy ISSUER.party
        transaction {
            output(ProofOfBurnContract.contractId, ProofOfBurn(
                    burnedState = 0,
                    purposeOfBurn = StateRef(txhash = SecureHash.zeroHash, index = 0)))

            tweak {
                command(ALICE.publicKey, ProofOfBurnContract.IssueBurnCommand())
                this `fails with` "The output ProofOfBurn claims was burned is encumbered by ProofOfBurn"
            }
        }
    }
}
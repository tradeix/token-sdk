package com.r3.corda.lib.tokens.contracts

import com.r3.corda.lib.tokens.contracts.states.NonceState
import com.r3.corda.lib.tokens.contracts.states.ProofOfBurn
import com.r3.corda.lib.tokens.contracts.states.ReissuanceToken
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.USD
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.SecureHash
import org.junit.Test

class ReissuanceTokenTests : ContractTestCommon() {

    val burnedToken = USD issuedBy ISSUER.party

    val ref = StateRef(SecureHash.randomSHA256(), 0)

    val pob = ProofOfBurn(
            burnedTxState = TransactionState(
                    800 of burnedToken heldBy ALICE.party,
                    FungibleTokenContract.contractId,
                    NOTARY.party),
            burnedStateIndex = 0,
            purposeOfBurn =  ref)

    val pobTx = TransactionState(pob, ProofOfBurnContract.ID, NOTARY.party)

    val token = ReissuanceToken(
            owner = ALICE.party,
            reissueKey = ref,
            percentageAmount = 100,
            usedProofs = listOf(pobTx)
    )

    val splitTokens = token.spend(mapOf(ALICE.party to 50L))

    val nonce = NonceState()

    @Test
    fun `verify issue - No reissuance token inputs are consumed when issuing a new reissuance token`() {
        transaction {
            input(ReissuanceTokenContract.ID, token)
            output(ReissuanceTokenContract.ID, token)
            command(ALICE.publicKey, ReissuanceTokenContract.IssueReissuanceTokenCommand())
            `fails with`("No reissuance token inputs are consumed when issuing a new reissuance token")
        }
    }

    @Test
    fun `verify issue - The single reissuance token reissueKey equals the StateRef of the single consumed nonce state`() {
        transaction {
            input(NonceContract.ID, nonce)
            output(ReissuanceTokenContract.ID, token)
            command(ALICE.publicKey, NonceContract.UseNonceStateCommand())
            command(ALICE.publicKey, ReissuanceTokenContract.IssueReissuanceTokenCommand())
            `fails with`("The single reissuance token reissueKey equals the StateRef of the single consumed nonce state")
        }
    }

    @Test
    fun `verify move - Current owner on single ReissuanceToken input is in list of signers`() {
        transaction {
            //input(NonceContract.ID, nonce)
            input(ReissuanceTokenContract.ID, token)
            splitTokens.forEach { output(ReissuanceTokenContract.ID, it) }
            command(BOB.publicKey, NonceContract.UseNonceStateCommand())
            command(BOB.publicKey, ReissuanceTokenContract.MoveReissuanceTokenCommand())
            `fails with`("Current owner on single ReissuanceToken input is in list of signers")
        }
    }

    @Test
    fun `verify move - Combined amount on ReissuanceToken outputs is less than or equal to the ReissuanceToken input`() {
        transaction {
            input(ReissuanceTokenContract.ID, token)
            splitTokens.forEach { output(ReissuanceTokenContract.ID, it) }
            output(ReissuanceTokenContract.ID, token)
            command(ALICE.publicKey, NonceContract.UseNonceStateCommand())
            command(ALICE.publicKey, ReissuanceTokenContract.MoveReissuanceTokenCommand())
            `fails with`("Combined percentageAmount on ReissuanceToken outputs is less than or equal to the ReissuanceToken input")
        }
    }

    @Test
    fun `verify use - One reissuance token is consumed as input`() {
        transaction {
            output(ReissuanceTokenContract.ID, token)
            reference(ProofOfBurnContract.ID, pob)
            command(ALICE.publicKey, ReissuanceTokenContract.UseReissuanceTokenCommand())
            `fails with`("One reissuance token must be consumed.")
        }
    }

    @Test
    fun `verify use - One reissuance token is created as output`() {
        transaction {
            input(ReissuanceTokenContract.ID, token)
            reference(ProofOfBurnContract.ID, pob)
            command(ALICE.publicKey, ReissuanceTokenContract.UseReissuanceTokenCommand())
            `fails with`("One reissuance token must be created.")
        }
    }

    @Test
    fun `verify use - One proof-of-burn state must be referenced`() {
        transaction {
            input(ReissuanceTokenContract.ID, token)
            output(ReissuanceTokenContract.ID, token)
            command(ALICE.publicKey, ReissuanceTokenContract.UseReissuanceTokenCommand())
            `fails with`("One proof-of-burn state must be referenced.")
        }
    }

    @Test
    fun `verify use - Owner of reissuance input is on the list of signers`() {
        transaction {
            input(ReissuanceTokenContract.ID, token)
            output(ReissuanceTokenContract.ID, token)
            reference(ProofOfBurnContract.ID, pob)
            command(BOB.publicKey, ReissuanceTokenContract.UseReissuanceTokenCommand())
            `fails with`("Owner of reissuance input is on the list of signers")
        }
    }

    @Test
    fun `verify use - The reissuance output lists the referenced ProofOfBurn as one of the used burns`() {
        transaction {
            input(ReissuanceTokenContract.ID, token)
            output(ReissuanceTokenContract.ID, token.copy(usedProofs = emptyList()))
            reference(ProofOfBurnContract.ID, pob)
            command(ALICE.publicKey, ReissuanceTokenContract.UseReissuanceTokenCommand())
            `fails with`("The reissuance output lists the referenced ProofOfBurn as one of the used burns")
        }
    }
}
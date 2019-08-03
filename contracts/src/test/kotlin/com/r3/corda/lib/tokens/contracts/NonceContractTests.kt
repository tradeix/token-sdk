package com.r3.corda.lib.tokens.contracts

import com.r3.corda.lib.tokens.contracts.states.NonceState
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.LedgerTransaction
import org.junit.Ignore
import org.junit.Test

class FTSContract : Contract {
    companion object {
        @JvmStatic
        val ID: ContractClassName = FTSContract::class.qualifiedName!!
    }
    override fun verify(tx: LedgerTransaction) = Unit
}

@BelongsToContract(FTSContract::class)
data class FTSState(val fubar: String = "") : ContractState {
    override val participants: List<AbstractParty> get() = emptyList()
}

class NonceContractTests : ContractTestCommon() {

    @Test
    fun `issue nonce state test`() {
        transaction {
            input(NonceContract.ID, NonceState())
            command(ALICE.publicKey, NonceContract.IssueNonceStateCommand())
            `fails with`("No nonce state inputs should be consumed when issuing new nonce states")
        }
    }

    @Ignore("tx reports having a nonce state input...weird!")
    fun `use nonce state test - must consume at least one nonce state input`() {
        transaction {
            input(FTSContract.ID, FTSState())
            command(ALICE.publicKey, NonceContract.UseNonceStateCommand())
            `fails with`("At least one nonce state should be consumed")
        }
    }

    @Test
    fun `use nonce state test - must not create any nonce state outputs`() {
        transaction {
            input(NonceContract.ID, NonceState())
            output(NonceContract.ID, NonceState())
            command(ALICE.publicKey, NonceContract.UseNonceStateCommand())
            `fails with`("No nonce state outputs should be created when a nonce state is being used as an input")
        }
    }
}
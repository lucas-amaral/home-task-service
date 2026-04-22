package com.amaral.hometask.service

import com.amaral.hometask.model.FamilyConfig
import com.amaral.hometask.repository.FamilyConfigRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Optional

class FamilyConfigServiceTest {

    private val familyConfigRepo: FamilyConfigRepository = mock()
    private val service = FamilyConfigService(familyConfigRepo)

    @Test
    fun `getFamilyConfig returns names from repository`() {
        whenever(familyConfigRepo.findById(1L)).thenReturn(
            Optional.of(FamilyConfig(child1Name = "TestChild1", child2Name = "TestChild2"))
        )

        val cfg = service.getFamilyConfig()

        assertEquals("TestChild1", cfg.child1Name)
        assertEquals("TestChild2", cfg.child2Name)
    }

    @Test
    fun `getFamilyConfig returns defaults when no row exists`() {
        whenever(familyConfigRepo.findById(1L)).thenReturn(Optional.empty())

        val cfg = service.getFamilyConfig()

        assertEquals("Child 1", cfg.child1Name)
        assertEquals("Child 2", cfg.child2Name)
    }
}

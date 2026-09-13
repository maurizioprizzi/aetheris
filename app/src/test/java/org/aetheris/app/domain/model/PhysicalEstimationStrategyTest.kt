package org.aetheris.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Verifica a semântica das estratégias físicas previstas
 * pelo ADR-018.
 *
 * Artifact revision: day18-v1.
 */
class PhysicalEstimationStrategyTest {

    @Test
    fun `entries preserve the official implementation order`() {
        assertThat(
            PhysicalEstimationStrategy.entries
        ).containsExactly(
            PhysicalEstimationStrategy.HOMOGENEOUS_SOLID,
            PhysicalEstimationStrategy.OCCUPANCY_ADJUSTED,
            PhysicalEstimationStrategy.PANEL_ASSEMBLY,
            PhysicalEstimationStrategy.SHELL_OR_CONTAINER,
            PhysicalEstimationStrategy.COMPONENT_COMPOSITION,
            PhysicalEstimationStrategy.EMPIRICAL_REFERENCE
        ).inOrder()
    }

    @Test
    fun `homogeneous solid preserves the current mass calculation`() {
        val strategy =
            PhysicalEstimationStrategy.HOMOGENEOUS_SOLID

        assertThat(strategy.usesFullExternalVolume)
            .isTrue()

        assertThat(strategy.requiresAdditionalModelParameters)
            .isFalse()

        assertThat(strategy.usesAnalyticalGeometry)
            .isTrue()

        assertThat(strategy.requiresOccupancyFraction)
            .isFalse()

        assertThat(strategy.requiresPanelGeometry)
            .isFalse()

        assertThat(strategy.requiresInternalGeometry)
            .isFalse()

        assertThat(strategy.requiresComponentDefinitions)
            .isFalse()

        assertThat(strategy.requiresEmpiricalReference)
            .isFalse()

        assertThat(strategy.supportsCurrentMassCalculation)
            .isTrue()
    }

    @Test
    fun `occupancy adjusted requires only an occupancy fraction`() {
        val strategy =
            PhysicalEstimationStrategy.OCCUPANCY_ADJUSTED

        assertThat(strategy.usesFullExternalVolume)
            .isFalse()

        assertThat(strategy.requiresAdditionalModelParameters)
            .isTrue()

        assertThat(strategy.usesAnalyticalGeometry)
            .isTrue()

        assertThat(strategy.requiresOccupancyFraction)
            .isTrue()

        assertThat(strategy.requiresPanelGeometry)
            .isFalse()

        assertThat(strategy.requiresInternalGeometry)
            .isFalse()

        assertThat(strategy.requiresComponentDefinitions)
            .isFalse()

        assertThat(strategy.requiresEmpiricalReference)
            .isFalse()

        assertThat(strategy.supportsCurrentMassCalculation)
            .isFalse()
    }

    @Test
    fun `panel assembly requires only panel geometry`() {
        val strategy =
            PhysicalEstimationStrategy.PANEL_ASSEMBLY

        assertThat(strategy.usesFullExternalVolume)
            .isFalse()

        assertThat(strategy.requiresAdditionalModelParameters)
            .isTrue()

        assertThat(strategy.usesAnalyticalGeometry)
            .isTrue()

        assertThat(strategy.requiresOccupancyFraction)
            .isFalse()

        assertThat(strategy.requiresPanelGeometry)
            .isTrue()

        assertThat(strategy.requiresInternalGeometry)
            .isFalse()

        assertThat(strategy.requiresComponentDefinitions)
            .isFalse()

        assertThat(strategy.requiresEmpiricalReference)
            .isFalse()

        assertThat(strategy.supportsCurrentMassCalculation)
            .isFalse()
    }

    @Test
    fun `shell or container requires only internal geometry`() {
        val strategy =
            PhysicalEstimationStrategy.SHELL_OR_CONTAINER

        assertThat(strategy.usesFullExternalVolume)
            .isFalse()

        assertThat(strategy.requiresAdditionalModelParameters)
            .isTrue()

        assertThat(strategy.usesAnalyticalGeometry)
            .isTrue()

        assertThat(strategy.requiresOccupancyFraction)
            .isFalse()

        assertThat(strategy.requiresPanelGeometry)
            .isFalse()

        assertThat(strategy.requiresInternalGeometry)
            .isTrue()

        assertThat(strategy.requiresComponentDefinitions)
            .isFalse()

        assertThat(strategy.requiresEmpiricalReference)
            .isFalse()

        assertThat(strategy.supportsCurrentMassCalculation)
            .isFalse()
    }

    @Test
    fun `component composition requires only component definitions`() {
        val strategy =
            PhysicalEstimationStrategy.COMPONENT_COMPOSITION

        assertThat(strategy.usesFullExternalVolume)
            .isFalse()

        assertThat(strategy.requiresAdditionalModelParameters)
            .isTrue()

        assertThat(strategy.usesAnalyticalGeometry)
            .isTrue()

        assertThat(strategy.requiresOccupancyFraction)
            .isFalse()

        assertThat(strategy.requiresPanelGeometry)
            .isFalse()

        assertThat(strategy.requiresInternalGeometry)
            .isFalse()

        assertThat(strategy.requiresComponentDefinitions)
            .isTrue()

        assertThat(strategy.requiresEmpiricalReference)
            .isFalse()

        assertThat(strategy.supportsCurrentMassCalculation)
            .isFalse()
    }

    @Test
    fun `empirical strategy requires only a traceable reference`() {
        val strategy =
            PhysicalEstimationStrategy.EMPIRICAL_REFERENCE

        assertThat(strategy.usesFullExternalVolume)
            .isFalse()

        assertThat(strategy.requiresAdditionalModelParameters)
            .isTrue()

        assertThat(strategy.usesAnalyticalGeometry)
            .isFalse()

        assertThat(strategy.requiresOccupancyFraction)
            .isFalse()

        assertThat(strategy.requiresPanelGeometry)
            .isFalse()

        assertThat(strategy.requiresInternalGeometry)
            .isFalse()

        assertThat(strategy.requiresComponentDefinitions)
            .isFalse()

        assertThat(strategy.requiresEmpiricalReference)
            .isTrue()

        assertThat(strategy.supportsCurrentMassCalculation)
            .isFalse()
    }

    @Test
    fun `only homogeneous solid uses the complete external volume`() {
        val strategiesUsingFullExternalVolume =
            PhysicalEstimationStrategy.entries.filter { strategy ->
                strategy.usesFullExternalVolume
            }

        assertThat(strategiesUsingFullExternalVolume)
            .containsExactly(
                PhysicalEstimationStrategy.HOMOGENEOUS_SOLID
            )
    }

    @Test
    fun `only homogeneous solid supports the current use case`() {
        val compatibleStrategies =
            PhysicalEstimationStrategy.entries.filter { strategy ->
                strategy.supportsCurrentMassCalculation
            }

        assertThat(compatibleStrategies)
            .containsExactly(
                PhysicalEstimationStrategy.HOMOGENEOUS_SOLID
            )
    }

    @Test
    fun `every advanced strategy requires additional parameters`() {
        val advancedStrategies =
            PhysicalEstimationStrategy.entries.filterNot { strategy ->
                strategy ==
                        PhysicalEstimationStrategy.HOMOGENEOUS_SOLID
            }

        assertThat(advancedStrategies)
            .isNotEmpty()

        advancedStrategies.forEach { strategy ->
            assertThat(
                strategy.requiresAdditionalModelParameters
            ).isTrue()
        }
    }

    @Test
    fun `each advanced strategy declares one principal requirement`() {
        val advancedStrategies =
            PhysicalEstimationStrategy.entries.filterNot { strategy ->
                strategy ==
                        PhysicalEstimationStrategy.HOMOGENEOUS_SOLID
            }

        advancedStrategies.forEach { strategy ->
            val principalRequirementCount =
                listOf(
                    strategy.requiresOccupancyFraction,
                    strategy.requiresPanelGeometry,
                    strategy.requiresInternalGeometry,
                    strategy.requiresComponentDefinitions,
                    strategy.requiresEmpiricalReference
                ).count { requirement ->
                    requirement
                }

            assertThat(principalRequirementCount)
                .isEqualTo(1)
        }
    }
}

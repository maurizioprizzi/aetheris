package org.aetheris.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Verifica a integridade e o comportamento de consulta do
 * catálogo manual de perfis de objetos.
 *
 * Artifact revision: day18-v1.
 */
class ObjectProfileCatalogTest {

    @Test
    fun `all preserves the official manual selection order`() {
        assertThat(
            ObjectProfileCatalog.all
        ).containsExactly(
            ObjectProfileCatalog
                .GENERIC_HOMOGENEOUS_SOLID,
            ObjectProfileCatalog.BOOK,
            ObjectProfileCatalog.CONTAINER,
            ObjectProfileCatalog.PANEL_FURNITURE
        ).inOrder()
    }

    @Test
    fun `catalog profile identifiers are unique`() {
        val identifiers =
            ObjectProfileCatalog.all.map { profile ->
                profile.profileId
            }

        assertThat(identifiers)
            .containsNoDuplicates()
    }

    @Test
    fun `catalog display names are unique ignoring case`() {
        val normalizedNames =
            ObjectProfileCatalog.all.map { profile ->
                profile.displayName.lowercase()
            }

        assertThat(normalizedNames)
            .containsNoDuplicates()
    }

    @Test
    fun `generic homogeneous solid is the default profile`() {
        assertThat(ObjectProfileCatalog.defaultProfile)
            .isSameInstanceAs(
                ObjectProfileCatalog
                    .GENERIC_HOMOGENEOUS_SOLID
            )
    }

    @Test
    fun `generic solid uses current homogeneous calculation`() {
        val profile =
            ObjectProfileCatalog
                .GENERIC_HOMOGENEOUS_SOLID

        assertThat(profile.recommendedStrategy)
            .isEqualTo(
                PhysicalEstimationStrategy
                    .HOMOGENEOUS_SOLID
            )

        assertThat(profile.supportsCurrentMassCalculation)
            .isTrue()

        assertThat(profile.requiresAdditionalModelParameters)
            .isFalse()

        assertThat(profile.suggestedMaterials)
            .containsExactly(
                MaterialDensityCatalog.GENERIC_WOOD,
                MaterialDensityCatalog.POLYPROPYLENE,
                MaterialDensityCatalog.ALUMINUM,
                MaterialDensityCatalog.STRUCTURAL_STEEL,
                MaterialDensityCatalog.SOLID_GLASS,
                MaterialDensityCatalog.PORTLAND_CONCRETE
            )
            .inOrder()

        assertThat(profile.suggestedMaterials)
            .doesNotContain(
                MaterialDensityCatalog
                    .WATER_AT_ROOM_TEMPERATURE
            )
    }

    @Test
    fun `book uses component composition without provisional materials`() {
        val profile =
            ObjectProfileCatalog.BOOK

        assertThat(profile.recommendedStrategy)
            .isEqualTo(
                PhysicalEstimationStrategy
                    .COMPONENT_COMPOSITION
            )

        assertThat(profile.requiresAdditionalModelParameters)
            .isTrue()

        assertThat(profile.hasSuggestedMaterials)
            .isFalse()
    }

    @Test
    fun `container uses shell model with suitable material suggestions`() {
        val profile =
            ObjectProfileCatalog.CONTAINER

        assertThat(profile.recommendedStrategy)
            .isEqualTo(
                PhysicalEstimationStrategy
                    .SHELL_OR_CONTAINER
            )

        assertThat(profile.suggestedMaterials)
            .containsExactly(
                MaterialDensityCatalog.POLYPROPYLENE,
                MaterialDensityCatalog.ALUMINUM,
                MaterialDensityCatalog.SOLID_GLASS
            )
            .inOrder()
    }

    @Test
    fun `panel furniture uses panel assembly with generic wood suggestion`() {
        val profile =
            ObjectProfileCatalog.PANEL_FURNITURE

        assertThat(profile.recommendedStrategy)
            .isEqualTo(
                PhysicalEstimationStrategy
                    .PANEL_ASSEMBLY
            )

        assertThat(profile.suggestedMaterials)
            .containsExactly(
                MaterialDensityCatalog.GENERIC_WOOD
            )
    }

    @Test
    fun `findById ignores case and outer spaces`() {
        assertThat(
            ObjectProfileCatalog.findById(
                "  BoOk  "
            )
        ).isSameInstanceAs(
            ObjectProfileCatalog.BOOK
        )

        assertThat(
            ObjectProfileCatalog.findById(
                "PANEL-FURNITURE"
            )
        ).isSameInstanceAs(
            ObjectProfileCatalog.PANEL_FURNITURE
        )
    }

    @Test
    fun `findById returns null for absent or blank identifier`() {
        assertThat(
            ObjectProfileCatalog.findById(
                "unknown-profile"
            )
        ).isNull()

        assertThat(
            ObjectProfileCatalog.findById("   ")
        ).isNull()
    }

    @Test
    fun `findByDisplayName ignores case and outer spaces`() {
        assertThat(
            ObjectProfileCatalog.findByDisplayName(
                "  rEcIpIeNtE  "
            )
        ).isSameInstanceAs(
            ObjectProfileCatalog.CONTAINER
        )

        assertThat(
            ObjectProfileCatalog.findByDisplayName(
                "MÓVEL DE PAINÉIS"
            )
        ).isSameInstanceAs(
            ObjectProfileCatalog.PANEL_FURNITURE
        )
    }

    @Test
    fun `findByDisplayName returns null for absent or blank name`() {
        assertThat(
            ObjectProfileCatalog.findByDisplayName(
                "Objeto desconhecido"
            )
        ).isNull()

        assertThat(
            ObjectProfileCatalog.findByDisplayName("\t")
        ).isNull()
    }

    @Test
    fun `profilesUsing returns matching profiles in catalog order`() {
        assertThat(
            ObjectProfileCatalog.profilesUsing(
                PhysicalEstimationStrategy
                    .HOMOGENEOUS_SOLID
            )
        ).containsExactly(
            ObjectProfileCatalog
                .GENERIC_HOMOGENEOUS_SOLID
        )

        assertThat(
            ObjectProfileCatalog.profilesUsing(
                PhysicalEstimationStrategy
                    .COMPONENT_COMPOSITION
            )
        ).containsExactly(
            ObjectProfileCatalog.BOOK
        )

        assertThat(
            ObjectProfileCatalog.profilesUsing(
                PhysicalEstimationStrategy
                    .SHELL_OR_CONTAINER
            )
        ).containsExactly(
            ObjectProfileCatalog.CONTAINER
        )

        assertThat(
            ObjectProfileCatalog.profilesUsing(
                PhysicalEstimationStrategy
                    .PANEL_ASSEMBLY
            )
        ).containsExactly(
            ObjectProfileCatalog.PANEL_FURNITURE
        )
    }

    @Test
    fun `profilesUsing returns empty list for unrepresented strategies`() {
        assertThat(
            ObjectProfileCatalog.profilesUsing(
                PhysicalEstimationStrategy
                    .OCCUPANCY_ADJUSTED
            )
        ).isEmpty()

        assertThat(
            ObjectProfileCatalog.profilesUsing(
                PhysicalEstimationStrategy
                    .EMPIRICAL_REFERENCE
            )
        ).isEmpty()
    }

    @Test
    fun `only default catalog profile supports current mass calculation`() {
        val compatibleProfiles =
            ObjectProfileCatalog.all.filter { profile ->
                profile.supportsCurrentMassCalculation
            }

        assertThat(compatibleProfiles)
            .containsExactly(
                ObjectProfileCatalog
                    .GENERIC_HOMOGENEOUS_SOLID
            )
    }

    @Test
    fun `advanced catalog profiles require additional model parameters`() {
        val advancedProfiles =
            ObjectProfileCatalog.all.filterNot { profile ->
                profile.supportsCurrentMassCalculation
            }

        assertThat(advancedProfiles)
            .containsExactly(
                ObjectProfileCatalog.BOOK,
                ObjectProfileCatalog.CONTAINER,
                ObjectProfileCatalog.PANEL_FURNITURE
            )
            .inOrder()

        advancedProfiles.forEach { profile ->
            assertThat(
                profile.requiresAdditionalModelParameters
            ).isTrue()
        }
    }
}

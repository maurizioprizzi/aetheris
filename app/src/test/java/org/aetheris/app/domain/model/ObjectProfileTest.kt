package org.aetheris.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Verifica os invariantes e as propriedades derivadas de
 * [ObjectProfile].
 *
 * Artifact revision: day18-v1.
 */
class ObjectProfileTest {

    private val wood =
        MaterialDensity(
            materialName = "Madeira",
            kilogramsPerCubicMeter = 650f,
            uncertaintyKilogramsPerCubicMeter = 75f
        )

    private val aluminum =
        MaterialDensity(
            materialName = "Alumínio",
            kilogramsPerCubicMeter = 2_710f,
            uncertaintyKilogramsPerCubicMeter = 100f
        )

    @Test
    fun `valid profile preserves its declared values`() {
        val profile =
            createProfile(
                profileId = "solid-object",
                displayName = "Objeto sólido",
                description =
                    "Objeto predominantemente homogêneo.",
                recommendedStrategy =
                    PhysicalEstimationStrategy
                        .HOMOGENEOUS_SOLID,
                suggestedMaterials =
                    listOf(
                        wood,
                        aluminum
                    )
            )

        assertThat(profile.profileId)
            .isEqualTo("solid-object")

        assertThat(profile.displayName)
            .isEqualTo("Objeto sólido")

        assertThat(profile.description)
            .isEqualTo(
                "Objeto predominantemente homogêneo."
            )

        assertThat(profile.recommendedStrategy)
            .isEqualTo(
                PhysicalEstimationStrategy
                    .HOMOGENEOUS_SOLID
            )

        assertThat(profile.suggestedMaterials)
            .containsExactly(
                wood,
                aluminum
            )
            .inOrder()
    }

    @Test
    fun `profile exposes suggested material summary`() {
        val profile =
            createProfile(
                suggestedMaterials =
                    listOf(
                        wood,
                        aluminum
                    )
            )

        assertThat(profile.suggestedMaterialCount)
            .isEqualTo(2)

        assertThat(profile.hasSuggestedMaterials)
            .isTrue()

        assertThat(profile.primarySuggestedMaterial)
            .isEqualTo(wood)
    }

    @Test
    fun `profile supports an empty material suggestion list`() {
        val profile =
            createProfile(
                suggestedMaterials = emptyList()
            )

        assertThat(profile.suggestedMaterialCount)
            .isEqualTo(0)

        assertThat(profile.hasSuggestedMaterials)
            .isFalse()

        assertThat(profile.primarySuggestedMaterial)
            .isNull()
    }

    @Test
    fun `homogeneous profile delegates current calculation support`() {
        val profile =
            createProfile(
                recommendedStrategy =
                    PhysicalEstimationStrategy
                        .HOMOGENEOUS_SOLID
            )

        assertThat(profile.requiresAdditionalModelParameters)
            .isFalse()

        assertThat(profile.supportsCurrentMassCalculation)
            .isTrue()

        assertThat(profile.requiresEmpiricalReference)
            .isFalse()
    }

    @Test
    fun `advanced profile delegates additional parameter requirement`() {
        val profile =
            createProfile(
                recommendedStrategy =
                    PhysicalEstimationStrategy
                        .PANEL_ASSEMBLY
            )

        assertThat(profile.requiresAdditionalModelParameters)
            .isTrue()

        assertThat(profile.supportsCurrentMassCalculation)
            .isFalse()

        assertThat(profile.requiresEmpiricalReference)
            .isFalse()
    }

    @Test
    fun `empirical profile exposes reference requirement`() {
        val profile =
            createProfile(
                recommendedStrategy =
                    PhysicalEstimationStrategy
                        .EMPIRICAL_REFERENCE
            )

        assertThat(profile.requiresAdditionalModelParameters)
            .isTrue()

        assertThat(profile.supportsCurrentMassCalculation)
            .isFalse()

        assertThat(profile.requiresEmpiricalReference)
            .isTrue()
    }

    @Test
    fun `suggestsMaterial uses complete structural equality`() {
        val profile =
            createProfile(
                suggestedMaterials = listOf(wood)
            )

        val equivalentWood =
            wood.copy()

        val differentWoodDensity =
            wood.copy(
                kilogramsPerCubicMeter = 700f
            )

        assertThat(
            profile.suggestsMaterial(wood)
        ).isTrue()

        assertThat(
            profile.suggestsMaterial(equivalentWood)
        ).isTrue()

        assertThat(
            profile.suggestsMaterial(differentWoodDensity)
        ).isFalse()

        assertThat(
            profile.suggestsMaterial(aluminum)
        ).isFalse()
    }

    @Test
    fun `findSuggestedMaterialByName ignores case and outer spaces`() {
        val profile =
            createProfile(
                suggestedMaterials =
                    listOf(
                        wood,
                        aluminum
                    )
            )

        assertThat(
            profile.findSuggestedMaterialByName(
                "  mAdEiRa  "
            )
        ).isEqualTo(wood)

        assertThat(
            profile.findSuggestedMaterialByName(
                "ALUMÍNIO"
            )
        ).isEqualTo(aluminum)
    }

    @Test
    fun `findSuggestedMaterialByName returns null for absent or blank name`() {
        val profile =
            createProfile(
                suggestedMaterials = listOf(wood)
            )

        assertThat(
            profile.findSuggestedMaterialByName("Vidro")
        ).isNull()

        assertThat(
            profile.findSuggestedMaterialByName("   ")
        ).isNull()
    }

    @Test
    fun `profile accepts canonical identifiers at supported boundaries`() {
        val validIdentifiers =
            listOf(
                "a",
                "book",
                "panel-shelf",
                "generic_object_2",
                "a" + "1".repeat(63)
            )

        validIdentifiers.forEach { profileId ->
            val profile =
                createProfile(
                    profileId = profileId
                )

            assertThat(profile.profileId)
                .isEqualTo(profileId)
        }
    }

    @Test
    fun `profile rejects noncanonical identifiers`() {
        val invalidIdentifiers =
            listOf(
                "",
                "Book",
                "2book",
                "book profile",
                "book.profile",
                "book/profile",
                "área",
                "a" + "1".repeat(64)
            )

        invalidIdentifiers.forEach { profileId ->
            val exception =
                assertThrows(
                    IllegalArgumentException::class.java
                ) {
                    createProfile(
                        profileId = profileId
                    )
                }

            assertThat(exception)
                .hasMessageThat()
                .contains(
                    "O identificador do perfil deve"
                )
        }
    }

    @Test
    fun `profile rejects blank display name`() {
        val exception =
            assertThrows(
                IllegalArgumentException::class.java
            ) {
                createProfile(
                    displayName = "   "
                )
            }

        assertThat(exception)
            .hasMessageThat()
            .contains(
                "O nome do perfil de objeto não pode estar vazio."
            )
    }

    @Test
    fun `profile rejects blank description`() {
        val exception =
            assertThrows(
                IllegalArgumentException::class.java
            ) {
                createProfile(
                    description = "\t\n"
                )
            }

        assertThat(exception)
            .hasMessageThat()
            .contains(
                "A descrição do perfil de objeto não pode estar vazia."
            )
    }

    @Test
    fun `profile rejects duplicate material names ignoring case and spaces`() {
        val duplicateWood =
            MaterialDensity(
                materialName = "  MADEIRA  ",
                kilogramsPerCubicMeter = 720f,
                uncertaintyKilogramsPerCubicMeter = 90f
            )

        val exception =
            assertThrows(
                IllegalArgumentException::class.java
            ) {
                createProfile(
                    suggestedMaterials =
                        listOf(
                            wood,
                            duplicateWood
                        )
                )
            }

        assertThat(exception)
            .hasMessageThat()
            .contains(
                "Os materiais sugeridos não podem possuir " +
                        "nomes duplicados."
            )
    }

    private fun createProfile(
        profileId: String = "generic-object",
        displayName: String = "Objeto genérico",
        description: String =
            "Perfil utilizado pelos testes de domínio.",
        recommendedStrategy: PhysicalEstimationStrategy =
            PhysicalEstimationStrategy.HOMOGENEOUS_SOLID,
        suggestedMaterials: List<MaterialDensity> =
            emptyList()
    ): ObjectProfile {
        return ObjectProfile(
            profileId = profileId,
            displayName = displayName,
            description = description,
            recommendedStrategy = recommendedStrategy,
            suggestedMaterials = suggestedMaterials
        )
    }
}

package org.aetheris.app.domain.model

import java.util.Locale

/**
 * Descreve um arquétipo de objeto utilizado para orientar
 * uma estimativa física.
 *
 * Um perfil não representa uma instância observada nem confirma
 * automaticamente sua classificação. Ele reúne uma estratégia
 * física recomendada e materiais que podem ser apresentados como
 * sugestões ao usuário.
 *
 * A confirmação do tipo de objeto, do material e dos parâmetros
 * estruturais pertence à sessão de estimativa. Esses dados não
 * devem ser incorporados silenciosamente ao perfil reutilizável.
 *
 * @property profileId Identificador estável para persistência,
 * exportação e integração futura com classificadores visuais.
 * @property displayName Nome apresentado ao usuário.
 * @property description Explicação resumida do perfil e de suas
 * limitações físicas.
 * @property recommendedStrategy Estratégia física inicialmente
 * recomendada para objetos desse arquétipo.
 * @property suggestedMaterials Materiais plausíveis que poderão
 * ser sugeridos, mas que ainda exigem confirmação do usuário.
 *
 * Artifact revision: day18-v1.
 */
data class ObjectProfile(
    val profileId: String,
    val displayName: String,
    val description: String,
    val recommendedStrategy: PhysicalEstimationStrategy,
    val suggestedMaterials: List<MaterialDensity> =
        emptyList()
) {

    init {
        require(
            PROFILE_ID_PATTERN.matches(profileId)
        ) {
            "O identificador do perfil deve começar com uma " +
                    "letra minúscula e conter somente letras " +
                    "minúsculas, números, hífens ou sublinhados."
        }

        require(displayName.isNotBlank()) {
            "O nome do perfil de objeto não pode estar vazio."
        }

        require(description.isNotBlank()) {
            "A descrição do perfil de objeto não pode estar vazia."
        }

        val normalizedMaterialNames =
            suggestedMaterials.map { material ->
                material.materialName
                    .trim()
                    .lowercase(Locale.ROOT)
            }

        require(
            normalizedMaterialNames.distinct().size ==
                    normalizedMaterialNames.size
        ) {
            "Os materiais sugeridos não podem possuir " +
                    "nomes duplicados."
        }
    }

    /**
     * Quantidade de materiais sugeridos pelo perfil.
     */
    val suggestedMaterialCount: Int
        get() = suggestedMaterials.size

    /**
     * Indica que o perfil possui ao menos uma sugestão de material.
     */
    val hasSuggestedMaterials: Boolean
        get() = suggestedMaterials.isNotEmpty()

    /**
     * Primeira sugestão de material, quando disponível.
     *
     * A posição na lista representa somente a ordem de apresentação.
     * Ela não constitui confirmação nem evidência de composição.
     */
    val primarySuggestedMaterial: MaterialDensity?
        get() = suggestedMaterials.firstOrNull()

    /**
     * Indica que a estratégia recomendada exige parâmetros físicos
     * adicionais além do volume externo e de uma densidade nominal.
     */
    val requiresAdditionalModelParameters: Boolean
        get() = recommendedStrategy
            .requiresAdditionalModelParameters

    /**
     * Indica que o perfil pode utilizar diretamente o caso de uso
     * atual de volume multiplicado pela densidade.
     */
    val supportsCurrentMassCalculation: Boolean
        get() = recommendedStrategy
            .supportsCurrentMassCalculation

    /**
     * Indica que o perfil depende de referência experimental ou
     * documental em vez de uma geometria analítica completa.
     */
    val requiresEmpiricalReference: Boolean
        get() = recommendedStrategy
            .requiresEmpiricalReference

    /**
     * Verifica se o material completo informado está presente entre
     * as sugestões do perfil.
     *
     * A comparação considera nome, densidade e incerteza por meio da
     * igualdade estrutural de [MaterialDensity].
     */
    fun suggestsMaterial(
        materialDensity: MaterialDensity
    ): Boolean {
        return materialDensity in suggestedMaterials
    }

    /**
     * Procura uma sugestão de material pelo nome apresentado.
     *
     * Espaços externos e diferenças entre letras maiúsculas e
     * minúsculas são ignorados. Um texto vazio não produz resultado.
     */
    fun findSuggestedMaterialByName(
        materialName: String
    ): MaterialDensity? {
        val normalizedName =
            materialName.trim()

        if (normalizedName.isEmpty()) {
            return null
        }

        return suggestedMaterials.firstOrNull { material ->
            material.materialName.equals(
                other = normalizedName,
                ignoreCase = true
            )
        }
    }

    private companion object {
        val PROFILE_ID_PATTERN =
            Regex("^[a-z][a-z0-9_-]{0,63}$")
    }
}

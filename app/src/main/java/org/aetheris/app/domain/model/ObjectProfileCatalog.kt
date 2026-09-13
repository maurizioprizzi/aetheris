package org.aetheris.app.domain.model

/**
 * Catálogo inicial de perfis de objetos para seleção manual.
 *
 * Os perfis orientam a escolha de um modelo físico. Eles não
 * classificam automaticamente o objeto observado e não confirmam
 * materiais ou parâmetros estruturais em nome do usuário.
 *
 * O catálogo começa deliberadamente pequeno. Novos perfis somente
 * devem ser adicionados quando sua estratégia, suas hipóteses e sua
 * forma de validação estiverem claramente documentadas.
 *
 * Artifact revision: day18-v1.
 */
object ObjectProfileCatalog {

    /**
     * Objeto predominantemente maciço e homogêneo.
     *
     * É o único perfil inicial compatível diretamente com o cálculo
     * atual de volume externo multiplicado pela densidade.
     */
    val GENERIC_HOMOGENEOUS_SOLID =
        ObjectProfile(
            profileId = "generic-homogeneous-solid",
            displayName = "Sólido homogêneo genérico",
            description =
                "Objeto predominantemente maciço, homogêneo " +
                        "e representável pelo volume externo.",
            recommendedStrategy =
                PhysicalEstimationStrategy
                    .HOMOGENEOUS_SOLID,
            suggestedMaterials =
                listOf(
                    MaterialDensityCatalog.GENERIC_WOOD,
                    MaterialDensityCatalog.POLYPROPYLENE,
                    MaterialDensityCatalog.ALUMINUM,
                    MaterialDensityCatalog.STRUCTURAL_STEEL,
                    MaterialDensityCatalog.SOLID_GLASS,
                    MaterialDensityCatalog.PORTLAND_CONCRETE
                )
        )

    /**
     * Livro composto por miolo, capas, adesivos e espaços residuais.
     *
     * Nenhum material é sugerido nesta etapa porque papel, papelão,
     * revestimentos e adesivos ainda não possuem referências próprias
     * no catálogo de densidades.
     */
    val BOOK =
        ObjectProfile(
            profileId = "book",
            displayName = "Livro",
            description =
                "Objeto composto por miolo, capas, adesivos " +
                        "e pequenos espaços internos.",
            recommendedStrategy =
                PhysicalEstimationStrategy
                    .COMPONENT_COMPOSITION
        )

    /**
     * Recipiente cuja massa estrutural depende das geometrias
     * externa e interna.
     *
     * O conteúdo, quando existente, deverá ser tratado como um
     * componente separado e explicitamente confirmado.
     */
    val CONTAINER =
        ObjectProfile(
            profileId = "container",
            displayName = "Recipiente",
            description =
                "Objeto oco cuja estrutura depende da espessura " +
                        "das paredes e de sua geometria interna.",
            recommendedStrategy =
                PhysicalEstimationStrategy
                    .SHELL_OR_CONTAINER,
            suggestedMaterials =
                listOf(
                    MaterialDensityCatalog.POLYPROPYLENE,
                    MaterialDensityCatalog.ALUMINUM,
                    MaterialDensityCatalog.SOLID_GLASS
                )
        )

    /**
     * Móvel formado predominantemente por painéis.
     *
     * Madeira genérica é mantida apenas como sugestão inicial. MDF,
     * compensado e outros painéis serão incluídos quando possuírem
     * densidades e incertezas documentadas no catálogo de materiais.
     */
    val PANEL_FURNITURE =
        ObjectProfile(
            profileId = "panel-furniture",
            displayName = "Móvel de painéis",
            description =
                "Móvel construído principalmente com painéis, " +
                        "como estantes, armários e prateleiras.",
            recommendedStrategy =
                PhysicalEstimationStrategy
                    .PANEL_ASSEMBLY,
            suggestedMaterials =
                listOf(
                    MaterialDensityCatalog.GENERIC_WOOD
                )
        )

    /**
     * Perfis apresentados na seleção manual, em ordem estável.
     */
    val all: List<ObjectProfile> =
        listOf(
            GENERIC_HOMOGENEOUS_SOLID,
            BOOK,
            CONTAINER,
            PANEL_FURNITURE
        )

    /**
     * Perfil inicial da seleção manual.
     *
     * A existência de um perfil inicial não representa confirmação
     * automática de que o objeto observado seja um sólido homogêneo.
     */
    val defaultProfile: ObjectProfile
        get() = GENERIC_HOMOGENEOUS_SOLID

    /**
     * Procura um perfil pelo identificador estável.
     *
     * Espaços externos e diferenças entre letras maiúsculas e
     * minúsculas são ignorados. Um texto vazio não produz resultado.
     */
    fun findById(
        profileId: String
    ): ObjectProfile? {
        val normalizedId =
            profileId.trim()

        if (normalizedId.isEmpty()) {
            return null
        }

        return all.firstOrNull { profile ->
            profile.profileId.equals(
                other = normalizedId,
                ignoreCase = true
            )
        }
    }

    /**
     * Procura um perfil pelo nome apresentado ao usuário.
     */
    fun findByDisplayName(
        displayName: String
    ): ObjectProfile? {
        val normalizedName =
            displayName.trim()

        if (normalizedName.isEmpty()) {
            return null
        }

        return all.firstOrNull { profile ->
            profile.displayName.equals(
                other = normalizedName,
                ignoreCase = true
            )
        }
    }

    /**
     * Retorna os perfis que recomendam a estratégia informada,
     * preservando a ordem oficial do catálogo.
     */
    fun profilesUsing(
        strategy: PhysicalEstimationStrategy
    ): List<ObjectProfile> {
        return all.filter { profile ->
            profile.recommendedStrategy == strategy
        }
    }
}

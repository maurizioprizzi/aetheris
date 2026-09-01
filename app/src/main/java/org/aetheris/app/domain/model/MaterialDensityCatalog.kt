package org.aetheris.app.domain.model

/**
 * Catálogo inicial de densidades para estimativa de massa.
 *
 * Os valores representam referências aproximadas para materiais
 * sólidos e predominantemente homogêneos.
 *
 * A margem declarada considera variações práticas de composição,
 * liga, espécie, umidade, porosidade e fabricação. Ela não transforma
 * a estimativa em uma pesagem certificada.
 *
 * Objetos ocos, espumas, recipientes, móveis montados e estruturas
 * compostas não podem ser estimados corretamente apenas pela
 * multiplicação do volume externo pela densidade do material.
 */
object MaterialDensityCatalog {

    val WATER_AT_ROOM_TEMPERATURE =
        MaterialDensity(
            materialName = "Água em temperatura ambiente",
            kilogramsPerCubicMeter = 998f,
            uncertaintyKilogramsPerCubicMeter = 5f
        )

    val ALUMINUM =
        MaterialDensity(
            materialName = "Alumínio sólido",
            kilogramsPerCubicMeter = 2_710f,
            uncertaintyKilogramsPerCubicMeter = 100f
        )

    val STRUCTURAL_STEEL =
        MaterialDensity(
            materialName = "Aço estrutural sólido",
            kilogramsPerCubicMeter = 7_850f,
            uncertaintyKilogramsPerCubicMeter = 250f
        )

    val SOLID_GLASS =
        MaterialDensity(
            materialName = "Vidro sólido",
            kilogramsPerCubicMeter = 2_450f,
            uncertaintyKilogramsPerCubicMeter = 150f
        )

    val PORTLAND_CONCRETE =
        MaterialDensity(
            materialName = "Concreto Portland",
            kilogramsPerCubicMeter = 2_300f,
            uncertaintyKilogramsPerCubicMeter = 300f
        )

    val POLYPROPYLENE =
        MaterialDensity(
            materialName = "Polipropileno sólido",
            kilogramsPerCubicMeter = 900f,
            uncertaintyKilogramsPerCubicMeter = 100f
        )

    /**
     * A madeira possui grande variação entre espécies,
     * teor de umidade e direção do corte.
     *
     * Este item deve ser utilizado apenas quando a espécie
     * da madeira não for conhecida.
     */
    val GENERIC_WOOD =
        MaterialDensity(
            materialName = "Madeira genérica",
            kilogramsPerCubicMeter = 600f,
            uncertaintyKilogramsPerCubicMeter = 300f
        )

    /**
     * Materiais apresentados inicialmente na interface.
     *
     * A lista é somente leitura e mantém uma ordem adequada
     * para seleção pelo usuário.
     */
    val all: List<MaterialDensity> =
        listOf(
            GENERIC_WOOD,
            POLYPROPYLENE,
            ALUMINUM,
            STRUCTURAL_STEEL,
            SOLID_GLASS,
            PORTLAND_CONCRETE,
            WATER_AT_ROOM_TEMPERATURE
        )

    /**
     * Procura um material pelo nome, ignorando diferenças
     * entre letras maiúsculas e minúsculas e espaços externos.
     */
    fun findByName(
        materialName: String
    ): MaterialDensity? {
        val normalizedName =
            materialName.trim()

        if (normalizedName.isEmpty()) {
            return null
        }

        return all.firstOrNull { material ->
            material.materialName.equals(
                other = normalizedName,
                ignoreCase = true
            )
        }
    }
}
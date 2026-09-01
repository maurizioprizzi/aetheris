package org.aetheris.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.aetheris.app.domain.model.MaterialDensity
import java.util.Locale

/**
 * Seletor de densidade utilizado na estimativa de massa.
 *
 * O componente recebe os materiais externamente para permanecer
 * independente do catálogo e facilitar testes e reutilização.
 */
@Composable
fun MaterialDensitySelector(
    materials: List<MaterialDensity>,
    selectedMaterial: MaterialDensity?,
    onMaterialSelected: (MaterialDensity) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var isMenuExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(enabled) {
        if (!enabled) {
            isMenuExpanded = false
        }
    }

    val hasAvailableMaterials =
        materials.isNotEmpty()

    val isSelectionEnabled =
        enabled && hasAvailableMaterials

    Column(
        modifier = modifier,
        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Material do objeto",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "A densidade selecionada será combinada " +
                    "com o volume medido para estimar a massa.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = {
                    isMenuExpanded = true
                },
                enabled = isSelectionEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 2.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = selectionTitle(
                            selectedMaterial =
                                selectedMaterial,
                            hasAvailableMaterials =
                                hasAvailableMaterials
                        ),
                        style =
                            MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    selectedMaterial?.let { material ->
                        Text(
                            text = material
                                .densityDescription(),
                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = {
                    isMenuExpanded = false
                }
            ) {
                materials.forEach { material ->
                    val isSelected =
                        material == selectedMaterial

                    DropdownMenuItem(
                        text = {
                            Column(
                                verticalArrangement =
                                    Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text =
                                        material.materialName,
                                    fontWeight =
                                        if (isSelected) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        },
                                    color =
                                        if (isSelected) {
                                            MaterialTheme
                                                .colorScheme
                                                .primary
                                        } else {
                                            MaterialTheme
                                                .colorScheme
                                                .onSurface
                                        }
                                )

                                Text(
                                    text = material
                                        .densityDescription(),
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            isMenuExpanded = false
                            onMaterialSelected(material)
                        }
                    )
                }
            }
        }

        if (selectedMaterial != null) {
            Text(
                text = "Estimativa válida principalmente para " +
                        "objetos sólidos e homogêneos.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun selectionTitle(
    selectedMaterial: MaterialDensity?,
    hasAvailableMaterials: Boolean
): String {
    return when {
        selectedMaterial != null -> {
            selectedMaterial.materialName
        }

        hasAvailableMaterials -> {
            "Selecionar material"
        }

        else -> {
            "Nenhum material disponível"
        }
    }
}

private fun MaterialDensity.densityDescription(): String {
    return String.format(
        Locale.getDefault(),
        "%.0f ± %.0f kg/m³",
        kilogramsPerCubicMeter,
        uncertaintyKilogramsPerCubicMeter
    )
}
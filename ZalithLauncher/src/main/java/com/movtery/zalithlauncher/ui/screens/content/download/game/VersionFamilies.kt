/*
 * Tokito Launcher - unofficial modified version of Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors (Zalith Launcher 2)
 * Modifications Copyright (C) 2026 Tokito Launcher contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.screens.content.download.game
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.installed.Version

internal const val VERSION_FAMILY_SNAPSHOTS = "@snapshots"
internal const val VERSION_FAMILY_BETA = "@beta"
internal const val VERSION_FAMILY_ALPHA = "@alpha"
internal const val VERSION_FAMILY_CLASSIC = "@classic"
internal const val VERSION_FAMILY_OTHER = "@other"

/** "1.8.9" -> 1.8 | "26.1.2" -> 26.1 (a numeração nova por ano) */
private val NUMERIC_FAMILY = Regex("^(\\d{1,2})\\.(\\d{1,2})(?!\\d)")

/** "Fabric-1.20.1" -> 1.20 (o número solto, sem letras ou pontos colados antes) */
private val ANYWHERE_FAMILY = Regex("(?<![\\d.])(\\d{1,2})\\.(\\d{1,2})(?!\\d)")

/** "24w14a", "15w14a" e parecidos */
private val WEEKLY_SNAPSHOT = Regex("^\\d{2}w\\d{2}[a-z~]")

private val SPECIAL_ORDER = listOf(
    VERSION_FAMILY_SNAPSHOTS,
    VERSION_FAMILY_BETA,
    VERSION_FAMILY_ALPHA,
    VERSION_FAMILY_CLASSIC,
    VERSION_FAMILY_OTHER
)

/**
 * Família de versões do Minecraft (por exemplo a família "1.8" = 1.8, 1.8.1, 1.8.2 ... 1.8.9).
 * [key] é "1.8" ou uma das chaves especiais que começam com "@".
 */
internal data class VersionFamily(val key: String, val count: Int) {
    val isNumeric: Boolean get() = !key.startsWith("@")
    val major: Int get() = if (isNumeric) key.substringBefore('.').toIntOrNull() ?: 0 else 0
    val minor: Int get() = if (isNumeric) key.substringAfter('.').toIntOrNull() ?: 0 else 0
}

/** Descobre a família a partir do ID da versão (só olha o texto, não precisa de rede). */
internal fun String.versionFamilyKey(): String {
    val id = trim()
    NUMERIC_FAMILY.find(id)?.let { return it.groupValues[1] + "." + it.groupValues[2] }
    return when {
        WEEKLY_SNAPSHOT.containsMatchIn(id) -> VERSION_FAMILY_SNAPSHOTS
        id.startsWith("b") && id.getOrNull(1)?.isDigit() == true -> VERSION_FAMILY_BETA
        id.startsWith("a") && id.getOrNull(1)?.isDigit() == true -> VERSION_FAMILY_ALPHA
        id.startsWith("rd-") || id.startsWith("c0.") || id.startsWith("inf-") || id.startsWith("in-") -> VERSION_FAMILY_CLASSIC
        else -> VERSION_FAMILY_OTHER
    }
}

/** Igual a [versionFamilyKey], mas aceita nomes como "Fabric-1.20.1" ou "1.8.9-OptiFine" (acha o número em qualquer lugar). */
internal fun String.versionFamilyKeyLoose(): String {
    val strict = versionFamilyKey()
    if (strict != VERSION_FAMILY_OTHER) return strict
    return ANYWHERE_FAMILY.find(this)?.let { it.groupValues[1] + "." + it.groupValues[2] } ?: VERSION_FAMILY_OTHER
}

/** Família de uma versão instalada: usa a versão do Minecraft dela, ou o nome da pasta. */
internal fun Version.installedFamilyKey(): String {
    val mc = getVersionInfo()?.minecraftVersion
    return (if (!mc.isNullOrBlank()) mc else getVersionName()).versionFamilyKeyLoose()
}

/** Nome da família para mostrar nos chips: "1.8.x", "Snapshots", "Beta"... */
@Composable
internal fun versionFamilyLabel(family: VersionFamily): String = when (family.key) {
    VERSION_FAMILY_SNAPSHOTS -> stringResource(R.string.bd_family_snapshots)
    VERSION_FAMILY_BETA -> stringResource(R.string.bd_family_beta)
    VERSION_FAMILY_ALPHA -> stringResource(R.string.bd_family_alpha)
    VERSION_FAMILY_CLASSIC -> stringResource(R.string.bd_family_classic)
    VERSION_FAMILY_OTHER -> stringResource(R.string.bd_family_other)
    else -> family.key + ".x"
}

/** Conta as versões por família a partir dos IDs. */
internal fun List<String>.toVersionFamilies(): List<VersionFamily> = map { it.versionFamilyKey() }.countFamilyKeys()

/** Conta as versões por família: as numéricas primeiro (mais novas em cima), depois as especiais. */
internal fun List<String>.countFamilyKeys(): List<VersionFamily> {
    val counts = LinkedHashMap<String, Int>()
    for (key in this) {
        counts[key] = (counts[key] ?: 0) + 1
    }
    return counts.map { VersionFamily(it.key, it.value) }.sortedWith(
        compareBy<VersionFamily>(
            { if (it.isNumeric) 0 else 1 },
            { if (it.isNumeric) 0 else SPECIAL_ORDER.indexOf(it.key) }
        ).thenByDescending { it.major }.thenByDescending { it.minor }
    )
}

package com.adamfoerster.syncthing.core.presentation

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

sealed interface UiText {
    data class DynamicString(val value: String) : UiText

    class Resource(val id: StringResource, vararg val args: Any) : UiText

    class Plural(val id: PluralStringResource, val quantity: Int, vararg val args: Any) : UiText

    @Composable
    fun asString(): String = when (this) {
        is DynamicString -> value
        is Resource -> stringResource(id, *args.resolve())
        is Plural -> pluralStringResource(id, quantity, *args.resolve())
    }

    suspend fun asStringAsync(): String = when (this) {
        is DynamicString -> value
        is Resource -> getString(id, *args.resolveAsync())
        is Plural -> getPluralString(id, quantity, *args.resolveAsync())
    }
}

/** Args may themselves be UiText (e.g. a device name that might be "Este celular"). */
@Composable
private fun Array<out Any>.resolve(): Array<Any> = map { if (it is UiText) it.asString() else it }.toTypedArray()

private suspend fun Array<out Any>.resolveAsync(): Array<Any> =
    map { if (it is UiText) it.asStringAsync() else it }.toTypedArray()

package br.com.rotinas.etiquetas

import android.content.Context
import androidx.core.content.edit

/**
 * Lista de cidades usada para separar o nome do cliente do local do pedido.
 * Junta a lista que vem com o aplicativo (res/raw/cidades.txt) com as cidades
 * que a usuária cadastrar na tela de configuração.
 */
class PlaceCatalog(private val context: Context) {

    fun all(): List<String> = bundled() + custom()

    fun custom(): List<String> =
        prefs().getStringSet(KEY_CUSTOM, emptySet())!!.sorted()

    fun saveCustom(places: Collection<String>) {
        val cleaned = places.map { TextTools.squeeze(it) }.filter { it.isNotBlank() }.toSet()
        prefs().edit { putStringSet(KEY_CUSTOM, cleaned) }
    }

    private fun bundled(): List<String> = cachedBundled ?: loadBundled().also { cachedBundled = it }

    private fun loadBundled(): List<String> =
        context.resources.openRawResource(R.raw.cidades).bufferedReader().useLines { lines ->
            lines.map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .toList()
        }

    private fun prefs() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private companion object {
        const val PREFS = "etiquetas"
        const val KEY_CUSTOM = "cidades_personalizadas"

        @Volatile
        var cachedBundled: List<String>? = null
    }
}

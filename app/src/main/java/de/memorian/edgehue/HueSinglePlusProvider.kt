package de.memorian.edgehue

import android.content.Context
import android.widget.RemoteViews
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailProvider

/**
 * Implementation of a [SlookCocktailProvider] that offers actions for Philips Hue in Edge Single Plus style.
 */
class HueSinglePlusProvider : SlookCocktailProvider() {

    override fun onUpdate(context: Context, cocktailManager: SlookCocktailManager, cocktailIds: IntArray) {
        panelUpdate(context, cocktailManager, cocktailIds)
    }

    private fun panelUpdate(context: Context, manager: SlookCocktailManager, cocktailIds: IntArray) {
        val remoteViews = RemoteViews(context.packageName, R.layout.provider_hue_single_plus)
        for (id in cocktailIds) {
            manager.updateCocktail(id, remoteViews)
        }
    }
}

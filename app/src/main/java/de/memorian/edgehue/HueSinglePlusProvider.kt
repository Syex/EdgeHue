package de.memorian.edgehue

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailProvider
import de.memorian.edgehue.bridge.hueConnectActivity
import timber.log.Timber

private const val CLICK_SCAN_BRIDGES = "scanBridges"

/**
 * Implementation of a [SlookCocktailProvider] that offers actions for Philips Hue in Edge Single Plus style.
 */
class HueSinglePlusProvider : SlookCocktailProvider() {

    private lateinit var hueController: HueController

    override fun onUpdate(context: Context, cocktailManager: SlookCocktailManager, cocktailIds: IntArray) {
        hueController = HueController(context)
        val layoutID = if (hueController.hasBridgeIp) R.layout.provider_hue_single_plus else R.layout.view_no_bridge_connected
        val remoteViews = RemoteViews(context.packageName, layoutID)
        remoteViews.setOnClickPendingIntent(R.id.btn_startBridgeScan,
                getPendingSelfIntent(context, CLICK_SCAN_BRIDGES))
        for (id in cocktailIds) {
            cocktailManager.updateCocktail(id, remoteViews)
        }
    }

    override fun onEnabled(context: Context) {
        Timber.i("Edge Hue Panel enabled")
    }

    override fun onDisabled(context: Context) {
        Timber.i("Edge Hue Panel disabled")
    }

    override fun onVisibilityChanged(context: Context, cocktailId: Int, visibility: Int) {
        Timber.i("Visibility of $cocktailId changed to $visibility")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            CLICK_SCAN_BRIDGES -> context.startActivity(context.hueConnectActivity())
        }
    }

    private fun getPendingSelfIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, javaClass).apply { this.action = action }
        return PendingIntent.getBroadcast(context, 0, intent, 0)
    }
}

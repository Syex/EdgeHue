package de.memorian.edgehue

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.support.v4.content.LocalBroadcastManager
import android.widget.RemoteViews
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager.COCKTAIL_VISIBILITY_SHOW
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailProvider
import de.memorian.edgehue.bridge.hueConnectActivity
import timber.log.Timber
import timber.log.Timber.DebugTree

const val ACTION_BRIDGE_ADDED = "bridgeAdded"
private const val CLICK_SCAN_BRIDGES = "scanBridges"

/**
 * Implementation of a [SlookCocktailProvider] that offers actions for Philips Hue in Edge Single Plus style.
 */
class HueSinglePlusProvider : SlookCocktailProvider(), HueController.Callback {

    override fun onUpdate(context: Context, cocktailManager: SlookCocktailManager, cocktailIds: IntArray) {
        refreshEdgePanelViews(context)
    }

    override fun onEnabled(context: Context) {
        if (BuildConfig.DEBUG) Timber.plant(DebugTree())
        Timber.i("Edge Hue Panel enabled")
        LocalBroadcastManager.getInstance(context).registerReceiver(this, IntentFilter())
    }

    override fun onDisabled(context: Context) {
        Timber.i("Edge Hue Panel disabled")
        LocalBroadcastManager.getInstance(context).unregisterReceiver(this)
        // stop heartbeats and disconnect from bridges
    }

    override fun onVisibilityChanged(context: Context, cocktailId: Int, visibility: Int) {
        Timber.i("Visibility of $cocktailId changed to $visibility")
        if (visibility == COCKTAIL_VISIBILITY_SHOW) {
            hueController(context).connectToBridges()
            refreshEdgePanelViews(context)
        }
    }

    private fun hueController(context: Context): HueController = HueController.instance(context, this)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            CLICK_SCAN_BRIDGES -> context.startActivity(context.hueConnectActivity())
            ACTION_BRIDGE_ADDED -> hueController(context).connectToBridges()
        }
    }

    private fun refreshEdgePanelViews(context: Context) {
        val manager = SlookCocktailManager.getInstance(context)
        val thisAppWidget = ComponentName(context.packageName, HueSinglePlusProvider::class.java.name)
        val cocktailIds = manager.getCocktailIds(thisAppWidget)

        for (id in cocktailIds) {
            val edgePanel = getEdgePanelView(context, id)
            manager.updateCocktail(id, edgePanel)
        }
    }

    private fun getEdgePanelView(context: Context, cocktailId: Int): RemoteViews {
        val remoteViews: RemoteViews
        if (hueController(context).hasConnectedBridge) {
            remoteViews = RemoteViews(context.packageName, R.layout.provider_hue_single_plus)
            val intent = Intent(context, RoomAdapterWidgetService::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, cocktailId)
            intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))
            remoteViews.setRemoteAdapter(R.id.room_list, getUpdateIntent(context, cocktailId))
        } else {
            remoteViews = RemoteViews(context.packageName, R.layout.view_no_bridge_connected).apply {
                setOnClickPendingIntent(R.id.btn_startBridgeScan,
                        getPendingSelfIntent(context, CLICK_SCAN_BRIDGES))
            }
        }

        return remoteViews
    }

    private fun getUpdateIntent(context: Context, cocktailId: Int): Intent {
        val updateIntent = Intent(context, RoomAdapterWidgetService::class.java)
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, cocktailId)
        updateIntent.putExtra("random", Math.random())
        // Somehow if we pass same intent every time system thinks nothing change so it didn't start the
        // service after sometime but if we add any random number in the intent then it thinks we passes a new intent every time and our service start again
        // when we require it, for reference see below link.
        // http://stackoverflow.com/questions/13199904/android-home-screen-widget-remoteviews-setremoteadapter-method-not-working
        updateIntent.data = Uri.parse(updateIntent.toUri(Intent.URI_INTENT_SCHEME))
        return updateIntent
    }

    private fun getPendingSelfIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, javaClass).apply { this.action = action }
        return PendingIntent.getBroadcast(context, 0, intent, 0)
    }

    override fun bridgesUpdated(context: Context) {
        refreshEdgePanelViews(context)
    }
}

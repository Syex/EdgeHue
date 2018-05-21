package de.memorian.edgehue

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import de.memorian.edgehue.model.Light
import de.memorian.edgehue.model.Room
import timber.log.Timber

class RoomAdapterWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsService.RemoteViewsFactory {
        return RoomRemoteViewsFactory(this.applicationContext)
    }
}

class RoomRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory,
        HueController.Callback {

    private val hueController by lazy { HueController.instance(context, this) }
    private var values: List<Any> = emptyList()

    override fun bridgesUpdated(context: Context) {
        onDataSetChanged()
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun onDataSetChanged() {
        values = hueController.getDisplayGroups().map { listOf(it) + it.lights }.flatten()
    }

    override fun hasStableIds(): Boolean = false

    override fun onDestroy() {
    }

    override fun onCreate() {
    }

    override fun getViewAt(position: Int): RemoteViews {
        Timber.i("Creating view for position $position and object ${values[position]}")
        val layoutId = if (values[position] is Room) R.layout.item_room else R.layout.item_light
        val remoteViews = RemoteViews(context.packageName, layoutId)
        when (values[position]) {
            is Room -> {
                val room = values[position] as Room
                Timber.i("View is room ${room.name}")
                remoteViews.setTextViewText(R.id.item_room_name, room.name)
                remoteViews.setTextViewText(R.id.item_room_enabled, if (room.enabled) "ON" else "OFF")
            }
            is Light -> {
                val light = values[position] as Light
                Timber.i("View is light ${light.name}")
                remoteViews.setTextViewText(R.id.item_light_name, light.name)
                remoteViews.setTextViewText(R.id.item_light_enabled, if (light.enabled) "ON" else "OFF")
            }
        }

        return remoteViews
    }

    override fun getCount(): Int {
        val size = values.size
        Timber.i("$size rooms and lights to render")
        return size
    }

    override fun getViewTypeCount(): Int = 2

    override fun getLoadingView(): RemoteViews? = null
}
package de.memorian.edgehue

import android.annotation.SuppressLint
import android.content.Context
import com.philips.lighting.hue.sdk.wrapper.HueLog
import com.philips.lighting.hue.sdk.wrapper.Persistence
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnection
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionCallback
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionType
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateCacheType
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateUpdatedCallback
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateUpdatedEvent
import com.philips.lighting.hue.sdk.wrapper.connection.ConnectionEvent
import com.philips.lighting.hue.sdk.wrapper.domain.Bridge
import com.philips.lighting.hue.sdk.wrapper.domain.BridgeBuilder
import com.philips.lighting.hue.sdk.wrapper.domain.HueError
import com.philips.lighting.hue.sdk.wrapper.domain.resource.GroupType
import com.philips.lighting.hue.sdk.wrapper.knownbridges.KnownBridge
import com.philips.lighting.hue.sdk.wrapper.knownbridges.KnownBridges
import de.memorian.edgehue.model.Light
import de.memorian.edgehue.model.Room
import timber.log.Timber

private const val HUE_SDK_NAME = "huesdk"
private const val STORAGE_DIR_NAME = "EdgeHue"
private const val HEARTBEAT_PERIOD = 10_000 //ms

/**
 * Wrapper around the Hue SDK.
 */
class HueController private constructor(
        private val context: Context,
        private val callback: Callback
) {

    init {
        System.loadLibrary(HUE_SDK_NAME)
        Persistence.setStorageLocation(context.filesDir.absolutePath, STORAGE_DIR_NAME)
        if (BuildConfig.DEBUG) HueLog.setConsoleLogLevel(HueLog.LogLevel.DEBUG)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var hueController: HueController? = null

        fun instance(context: Context, callback: Callback): HueController {
            val hueController = hueController ?: HueController(context, callback)
            this.hueController = hueController
            return hueController
        }
    }

    val hasConnectedBridge: Boolean get() = connectedBridges.isNotEmpty()
    private val knownBridges: List<KnownBridge>
        get() = KnownBridges.getAll()
    private val connectedBridges = mutableListOf<Bridge>()

    fun connectToBridges() {
        for (bridge in knownBridges) {
            if (bridge.uniqueId in connectedBridges.map { it.identifier }) continue

            try {
                val newBridge = BridgeBuilder(context.getString(R.string.app_name), android.os.Build.MODEL)
                        .setConnectionType(BridgeConnectionType.LOCAL)
                        .setIpAddress(bridge.ipAddress)
                        .setBridgeId(bridge.uniqueId)
                        .addBridgeStateUpdatedCallback(BridgeUpdateCallback())
                        .setBridgeConnectionCallback(BridgeConnectCallback())
                        .build()
                newBridge.getBridgeConnection(BridgeConnectionType.LOCAL).apply {
                    connectionOptions.enableFastConnectionMode(newBridge.identifier)
                    heartbeatManager?.startHeartbeat(BridgeStateCacheType.LIGHTS_AND_GROUPS, HEARTBEAT_PERIOD)
                    connect()
                }
                connectedBridges.add(newBridge)
            } catch (e: Exception) {
                Timber.e(e, "Couldn't connect to Bridge $bridge")
            }
        }

        callback.bridgesUpdated(context)
    }

    fun getDisplayGroups(): List<Room> {
        val rooms = mutableListOf<Room>()
        for (bridge in connectedBridges) {
            for (group in bridge.bridgeState.groups.filter { it.groupType == GroupType.ROOM }) {
                val roomName = group.name
                val roomEnabled = group.groupState.isAllOn
                Timber.i("Found room $roomName")
                val lights = group.lightIds
                        .map { bridge.bridgeState.getLight(it) }
                        .map {
                            Timber.i("Found light ${it.name}")
                            Light(
                                    name = it.name,
                                    enabled = it.lightState.isOn,
                                    reachable = it.lightState.isReachable
                            )
                        }
                rooms.add(Room(roomName, roomEnabled, lights))
            }
        }

        return rooms
    }

    private inner class BridgeUpdateCallback : BridgeStateUpdatedCallback() {

        override fun onBridgeStateUpdated(bridge: Bridge, event: BridgeStateUpdatedEvent) {
            when (event) {
                BridgeStateUpdatedEvent.LIGHTS_AND_GROUPS -> {
                    Timber.i("Lights and groups have been updated")
                    callback.bridgesUpdated(context)
                }
                BridgeStateUpdatedEvent.UNKNOWN -> Timber.i("Received update event $event")
                BridgeStateUpdatedEvent.INITIALIZED -> {
                    Timber.i("Bridge $bridge has been initialized, starting heartbeat")
                    bridge.getBridgeConnection(BridgeConnectionType.LOCAL)
                            .heartbeatManager?.startHeartbeat(BridgeStateCacheType.LIGHTS_AND_GROUPS,
                            HEARTBEAT_PERIOD)
                }
                BridgeStateUpdatedEvent.FULL_CONFIG -> Timber.i("Received update event $event")
                BridgeStateUpdatedEvent.BRIDGE_CONFIG -> Timber.i("Received update event $event")
                BridgeStateUpdatedEvent.SCENES -> Timber.i("Received update event $event")
                BridgeStateUpdatedEvent.SENSORS_AND_SWITCHES -> Timber.i("Received update event $event")
                BridgeStateUpdatedEvent.RULES -> Timber.i("Received update event $event")
                BridgeStateUpdatedEvent.SCHEDULES_AND_TIMERS -> Timber.i("Received update event $event")
                BridgeStateUpdatedEvent.RESOURCE_LINKS -> Timber.i("Received update event $event")
                BridgeStateUpdatedEvent.DEVICE_SEARCH_STATUS -> Timber.i("Received update event $event")
            }
        }
    }

    private inner class BridgeConnectCallback : BridgeConnectionCallback() {

        override fun onConnectionEvent(bridgeConnection: BridgeConnection?, connectionEvent: ConnectionEvent?) {
            when (connectionEvent) {
                ConnectionEvent.NO_VALUE -> Timber.i("Received connection event $connectionEvent")
                ConnectionEvent.NONE -> Timber.i("Received connection event $connectionEvent")
                ConnectionEvent.COULD_NOT_CONNECT -> Timber.i("Received connection event $connectionEvent")
                ConnectionEvent.CONNECTED -> Timber.i("Received connection event $connectionEvent")
                ConnectionEvent.NOT_AUTHENTICATED -> Timber.i("Received connection event $connectionEvent")
                ConnectionEvent.CONNECTION_LOST -> Timber.i("Received connection event $connectionEvent")
                ConnectionEvent.CONNECTION_RESTORED -> Timber.i("Received connection event $connectionEvent")
                ConnectionEvent.DISCONNECTED -> Timber.i("Received connection event $connectionEvent")
                ConnectionEvent.AUTHENTICATED -> Timber.i("Received connection event $connectionEvent")
                ConnectionEvent.LINK_BUTTON_NOT_PRESSED -> Timber.i("Received connection event $connectionEvent")
                ConnectionEvent.LOGIN_REQUIRED -> Timber.i("Received connection event $connectionEvent")
                ConnectionEvent.TOKEN_EXPIRED -> Timber.i("Received connection event $connectionEvent")
                ConnectionEvent.NO_BRIDGE_FOR_PORTAL_ACCOUNT -> Timber.i("Received connection event $connectionEvent")
                ConnectionEvent.BRIDGE_UNIQUE_ID_MISMATCH -> Timber.i("Received connection event $connectionEvent")
                ConnectionEvent.RATE_LIMIT_QUOTA_VIOLATION -> Timber.i("Received connection event $connectionEvent")
                ConnectionEvent.TOKEN_UNKNOWN -> Timber.i("Received connection event $connectionEvent")
                ConnectionEvent.TOKEN_BRIDGE_MISMATCH -> Timber.i("Received connection event $connectionEvent")
                null -> Timber.i("Received connection event $connectionEvent")
            }
        }

        override fun onConnectionError(bridgeConnection: BridgeConnection?, errors: MutableList<HueError>?) {
            errors?.forEach { Timber.e("Bridge connection error: $it") }
        }
    }

    interface Callback {

        fun bridgesUpdated(context: Context)
    }
}

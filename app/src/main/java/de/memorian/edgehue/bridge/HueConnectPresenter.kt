package de.memorian.edgehue.bridge

import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnection
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionCallback
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionType
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateUpdatedCallback
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateUpdatedEvent
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateUpdatedEvent.INITIALIZED
import com.philips.lighting.hue.sdk.wrapper.connection.ConnectionEvent
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscovery
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryCallback
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryResult
import com.philips.lighting.hue.sdk.wrapper.domain.Bridge
import com.philips.lighting.hue.sdk.wrapper.domain.BridgeBuilder
import com.philips.lighting.hue.sdk.wrapper.domain.HueError
import com.philips.lighting.hue.sdk.wrapper.domain.ReturnCode
import net.grandcentrix.thirtyinch.TiPresenter
import net.grandcentrix.thirtyinch.TiView
import timber.log.Timber

class HueConnectPresenter
    : TiPresenter<HueConnectView>() {

    private val bridgeSearchCallback = BridgeSearchCallback()

    init {
        BridgeDiscovery().search(bridgeSearchCallback)
    }

    private fun onBridgeSearchFinished(results: MutableList<BridgeDiscoveryResult>, returnCode: ReturnCode) {
        Timber.i("Bridge search returned code $returnCode and results are $results")
        if (returnCode != ReturnCode.SUCCESS || results.isEmpty()) {
            sendToView { it.showNoBridgesFoundError() }
            return
        }

        sendToView {
            it.hideError()
            it.setBridgeResults(results)
        }
    }

    fun connectToBridge(bridgeDiscoveryResult: BridgeDiscoveryResult) {
        BridgeBuilder("app name", "device name")
                .setIpAddress(bridgeDiscoveryResult.ip)
                .setConnectionType(BridgeConnectionType.LOCAL)
                .addBridgeStateUpdatedCallback(BridgeUpdateCallback())
                .setBridgeConnectionCallback(BridgeConnectCallback())
                .build()
                .connect()
    }

    private inner class BridgeSearchCallback : BridgeDiscoveryCallback() {

        override fun onFinished(results: MutableList<BridgeDiscoveryResult>, returnCode: ReturnCode) {
            onBridgeSearchFinished(results, returnCode)
        }
    }

    private inner class BridgeConnectCallback : BridgeConnectionCallback() {

        override fun onConnectionEvent(connection: BridgeConnection, event: ConnectionEvent) {
            Timber.i("Receives connection event: $event")
        }

        override fun onConnectionError(connection: BridgeConnection, errors: MutableList<HueError>) {
            errors.forEach { Timber.i("Error while connection to bridge: $it") }
        }
    }

    private inner class BridgeUpdateCallback : BridgeStateUpdatedCallback() {
        override fun onBridgeStateUpdated(bridge: Bridge, event: BridgeStateUpdatedEvent) {
            if (event == INITIALIZED) {
                Timber.i("Successfully connected to bridge $bridge")
            }
        }
    }
}

interface HueConnectView : TiView {

    fun setBridgeResults(results: List<BridgeDiscoveryResult>)

    fun showNoBridgesFoundError()

    fun hideError()

    fun setProgressVisibility(visible: Boolean)

    fun showConnectingToBridgeUi()
}
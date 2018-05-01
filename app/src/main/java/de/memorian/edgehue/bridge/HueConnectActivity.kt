package de.memorian.edgehue.bridge

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryResult
import de.memorian.edgehue.ACTION_BRIDGE_ADDED
import de.memorian.edgehue.R
import net.grandcentrix.thirtyinch.TiActivity

fun Context.hueConnectActivity(): Intent {
    return Intent(this, HueConnectActivity::class.java)
}

class HueConnectActivity : TiActivity<HueConnectPresenter, HueConnectView>(), HueConnectView {

    private val progressBar by lazy { findViewById<ProgressBar>(R.id.pb_scanning_for_bridges) }
    private val bridgeError by lazy { findViewById<TextView>(R.id.tv_bridge_connect_error) }
    private val bridgeResults by lazy { findViewById<RecyclerView>(R.id.rv_bridges) }
    private val bridgeLinkView by lazy { findViewById<BridgeLinkView>(R.id.view_bridge_link) }

    override fun providePresenter(): HueConnectPresenter = HueConnectPresenter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hue_connect)

        setSupportActionBar(findViewById(R.id.tb_hue_connect))
        supportActionBar?.apply {
            title = getString(R.string.activity_hue_connect)
            setHomeAsUpIndicator(R.drawable.ic_close)
            setDisplayHomeAsUpEnabled(true)
        }

        bridgeResults.layoutManager = LinearLayoutManager(this)
    }

    override fun setProgressVisible(visible: Boolean) {
        progressBar.isVisible = visible
    }

    override fun setBridgeResults(results: List<BridgeDiscoveryResult>) {
        bridgeResults.adapter = BridgesAdapter().apply {
            items = results
            listener = { presenter.connectToBridge(it) }
        }
    }

    override fun showNoBridgesFoundError() {
        bridgeError.isVisible = true
        bridgeError.text = getString(R.string.no_bridges_found_error)
    }

    override fun hideError() {
        bridgeError.isVisible = false
    }

    override fun showWaitingForLinkPush() {
        hideError()
        setProgressVisible(false)
        bridgeResults.isVisible = false
        bridgeLinkView.isVisible = true
    }

    override fun finish() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ACTION_BRIDGE_ADDED))
        super.finish()
    }
}

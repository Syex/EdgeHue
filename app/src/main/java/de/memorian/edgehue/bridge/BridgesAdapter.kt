package de.memorian.edgehue.bridge

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryResult
import de.memorian.edgehue.R
import de.memorian.edgehue.bridge.BridgesAdapter.ViewHolder

class BridgesAdapter : RecyclerView.Adapter<ViewHolder>() {

    var items: List<BridgeDiscoveryResult> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var listener: ((BridgeDiscoveryResult) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_bridge_result, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.id.text = item.uniqueID
        holder.ip.text = item.ip
        holder.itemView.setOnClickListener { listener?.invoke(item) }
    }

    class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {

        val id: TextView = rootView.findViewById(R.id.tv_bridge_result_id)
        val ip: TextView = rootView.findViewById(R.id.tv_bridge_result_ip)
    }
}
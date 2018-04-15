package de.memorian.edgehue.bridge

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import de.memorian.edgehue.R

class BridgeLinkView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {

    init {
        inflate(context, R.layout.view_bridge_link, this)
    }
}
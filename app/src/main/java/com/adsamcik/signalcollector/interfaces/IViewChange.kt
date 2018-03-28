package com.adsamcik.signalcollector.interfaces

import android.view.View

/**
 * Interface that requires recycler to implement onViewChangedListener
 */
interface IViewChange {
    /**
     * Invoked when view in recycler is changed
     */
    var onViewChangedListener: ((View) -> Unit)?
}
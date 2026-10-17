package com.jiitkiosk.app.widget

import android.content.Context

/**
 * The Today tile (4x2): next class, attendance ring, pending count in one card.
 * Upgraded in place from the app's original single widget, so home-screen
 * instances survive the redesign under the same component name.
 */
class KioskWidget : WidgetProvider() {

    override val kind = WidgetKind.Today

    companion object {
        /** repaint all six widgets from the caches — app open, classroom sync, alarms */
        fun refresh(context: Context) = WidgetRender.refresh(context)
    }
}

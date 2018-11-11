package com.theevilroot.uadaquoter.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.theevilroot.uadaquoter.R
import java.util.*

class RandomQuoteWidgetProvider: AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val remoteView = RemoteViews(context.packageName, R.layout.widget_random_quote)
        remoteView.setTextViewText(R.id.rqw_quote_id, "#${Random().nextInt()}")
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
    }

}
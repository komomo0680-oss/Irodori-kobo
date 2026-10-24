package com.example.konishike

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/** ホーム画面に置く「家族ニュース」のウィジェット */
class NewsWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        refreshInBackground(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH) {
            refreshInBackground(context)
        } else {
            super.onReceive(context, intent)
        }
    }

    private fun refreshInBackground(context: Context) {
        val pending = goAsync()
        val app = context.applicationContext
        thread {
            try {
                update(app)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.example.konishike.REFRESH"
        private const val PROJECT = "family-app-438a1"
        private const val API_KEY = "AIzaSyDXpUCPANQpTppk1dhOY3B4sENz-7bw08k"
        const val APP_URL = "https://komomo0680-oss.github.io/family-app/family-app.html"
        private const val PREFS = "news"

        fun savedCode(context: Context): String =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("code", "") ?: ""

        fun saveCode(context: Context, code: String) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString("code", code.trim()).apply()
        }

        /** ニュースを読み込んで、ウィジェットを更新する。結果のメッセージを返す。 */
        fun update(context: Context): String {
            val code = savedCode(context)
            var body = ""
            var updated = ""
            var message = "更新しました"

            if (code.isEmpty()) {
                body = "アプリを開いて、ウィジェット用のコードを入れてください"
                message = "コードが入っていません"
            } else {
                try {
                    val result = fetch(code)
                    body = if (result.first.isEmpty()) "まだニュースがありません" else result.first.joinToString("\n")
                    updated = result.second
                } catch (e: Exception) {
                    body = "読み込めませんでした\n" + (e.message ?: "")
                    message = "読み込めませんでした：" + (e.message ?: "")
                }
            }

            val views = RemoteViews(context.packageName, R.layout.widget_news)
            views.setTextViewText(R.id.body, body)
            views.setTextViewText(R.id.updated, if (updated.isNotEmpty()) "更新 $updated" else "")

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(APP_URL))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val open = PendingIntent.getActivity(context, 0, openIntent, flags)
            val refreshIntent = Intent(context, NewsWidget::class.java).setAction(ACTION_REFRESH)
            val refresh = PendingIntent.getBroadcast(context, 1, refreshIntent, flags)
            views.setOnClickPendingIntent(R.id.body, open)
            views.setOnClickPendingIntent(R.id.title, open)
            views.setOnClickPendingIntent(R.id.refresh, refresh)

            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, NewsWidget::class.java))
            if (ids.isNotEmpty()) manager.updateAppWidget(ids, views)
            return message
        }

        private fun fetch(code: String): Pair<List<String>, String> {
            val url = URL(
                "https://firestore.googleapis.com/v1/projects/$PROJECT" +
                    "/databases/(default)/documents/widgets/$code?key=$API_KEY"
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            try {
                val status = conn.responseCode
                if (status == 404) throw Exception("コードが見つかりません。コードを確かめるか、数分待ってください")
                if (status == 403) throw Exception("読み取りが許可されていません（Firebaseのルール）")
                if (status != 200) throw Exception("エラー $status")
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val fields = JSONObject(text).getJSONObject("fields")
                val values = fields.getJSONObject("lines")
                    .getJSONObject("arrayValue").optJSONArray("values")
                val lines = ArrayList<String>()
                if (values != null) {
                    for (i in 0 until values.length()) {
                        lines.add(values.getJSONObject(i).getString("stringValue"))
                    }
                }
                val updated = fields.optJSONObject("updated")?.optString("stringValue") ?: ""
                return Pair(lines, updated)
            } finally {
                conn.disconnect()
            }
        }
    }
}

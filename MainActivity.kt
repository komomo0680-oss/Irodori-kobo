package com.example.konishike

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

/** ウィジェット用のコードを入れる画面 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val density = resources.displayMetrics.density
        val pad = (20 * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
            setBackgroundColor(Color.parseColor("#FFF8F0"))
        }

        fun label(s: String, size: Float, bold: Boolean = false): TextView {
            val tv = TextView(this)
            tv.text = s
            tv.textSize = size
            tv.setTextColor(Color.parseColor("#2B2520"))
            if (bold) tv.setTypeface(tv.typeface, Typeface.BOLD)
            tv.setPadding(0, 0, 0, pad / 2)
            return tv
        }

        root.addView(label("小西家 ｜ ウィジェット設定", 20f, true))
        root.addView(
            label(
                "1. 下のボタンで、ブラウザの小西家アプリを開く\n" +
                    "2. 「メモ」タブの「ウィジェット用のコード」で、コードを作ってコピーする\n" +
                    "3. ここにコードを貼って、「保存して更新」を押す\n" +
                    "4. ホーム画面を長押し →「ウィジェット」→「小西家」を置く",
                14f
            )
        )

        val input = EditText(this)
        input.hint = "ウィジェット用のコード"
        input.setSingleLine()
        input.setText(NewsWidget.savedCode(this))
        root.addView(input)

        val status = label("", 14f)

        val save = Button(this)
        save.text = "保存して更新"
        save.setOnClickListener {
            NewsWidget.saveCode(this, input.text.toString())
            status.text = "更新しています…"
            val app = applicationContext
            thread {
                val message = NewsWidget.update(app)
                this@MainActivity.runOnUiThread { status.text = message }
            }
        }
        root.addView(save)

        val open = Button(this)
        open.text = "小西家アプリを開く（ブラウザ）"
        open.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(NewsWidget.APP_URL)))
        }
        root.addView(open)
        root.addView(status)

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }
}

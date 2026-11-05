package net.kaki87.youprice

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

object Palette {
    const val BG = 0xFFF4F1FA.toInt()
    const val CARD = Color.WHITE
    const val ACCENT = 0xFF5B2EBF.toInt()
    const val TEXT = 0xFF1E1B2E.toInt()
    const val MUTED = 0xFF6B6880.toInt()
}

fun Context.dp(v: Int) = (v * resources.displayMetrics.density).toInt()

fun Context.text(s: String, size: Float = 14f, color: Int = Palette.TEXT, bold: Boolean = false) = TextView(this).apply {
    text = s
    textSize = size
    setTextColor(color)
    if (bold) setTypeface(typeface, Typeface.BOLD)
}

fun Context.column(padding: Int = 0) = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
}

fun Context.card(title: String, content: LinearLayout.() -> Unit) = column(16).apply {
    background = GradientDrawable().apply {
        setColor(Palette.CARD)
        cornerRadius = dp(12).toFloat()
    }
    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        .apply { setMargins(dp(12), dp(6), dp(12), dp(6)) }
    addView(text(title, 18f, Palette.ACCENT, bold = true))
    content()
}

/** A label/value pair on one line. Empty values are skipped, as the web page hides empty fields. */
fun LinearLayout.row(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    addView(LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, context.dp(4), 0, 0)
        addView(context.text(label, 13f, Palette.MUTED).apply { layoutParams = LinearLayout.LayoutParams(0, -2, 0.4f) })
        addView(context.text(value, 14f).apply {
            setTextIsSelectable(true)
            layoutParams = LinearLayout.LayoutParams(0, -2, 0.6f)
        })
    })
}

fun LinearLayout.subtitle(s: String) {
    addView(context.text(s, 15f, bold = true).apply { setPadding(0, context.dp(12), 0, context.dp(2)) })
}

fun LinearLayout.note(s: String) {
    addView(context.text(s, 13f, Palette.MUTED).apply { setPadding(0, context.dp(6), 0, 0) })
}

fun LinearLayout.divider() {
    addView(View(context).apply {
        setBackgroundColor(0x1F000000)
        layoutParams = LinearLayout.LayoutParams(-1, 1).apply { topMargin = context.dp(10) }
    })
}

fun date(iso: String): String =
    Regex("""(\d{4})-(\d{2})-(\d{2})""").find(iso)?.destructured?.let { (y, m, d) -> "$d/$m/$y" } ?: iso

fun euro(v: Double) = String.format(Locale.FRANCE, "%.2f €", v)

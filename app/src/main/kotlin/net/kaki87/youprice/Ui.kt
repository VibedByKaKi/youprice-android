package net.kaki87.youprice

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.widget.ImageButton
import android.widget.Toast
import android.view.WindowInsets
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

object Palette {
    var BG = 0
    var CARD = 0
    var ACCENT = 0
    var TEXT = 0
    var MUTED = 0
    var ERROR = 0
    var DIVIDER = 0

    /** Reads the light or dark colors that match the current configuration. */
    fun init(c: Context) {
        BG = c.getColor(R.color.bg)
        CARD = c.getColor(R.color.card)
        ACCENT = c.getColor(R.color.accent)
        TEXT = c.getColor(R.color.text)
        MUTED = c.getColor(R.color.muted)
        ERROR = c.getColor(R.color.error)
        DIVIDER = c.getColor(R.color.divider)
    }
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
        // Without a label the value takes the full width instead of leaving an empty column.
        if (label.isNotEmpty()) addView(context.text(label, 13f, Palette.MUTED).apply { layoutParams = LinearLayout.LayoutParams(0, -2, 0.4f) })
        addView(context.text(value, 14f).apply {
            setTextIsSelectable(true)
            layoutParams = LinearLayout.LayoutParams(0, -2, if (label.isNotEmpty()) 0.6f else 1f)
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
        setBackgroundColor(Palette.DIVIDER)
        layoutParams = LinearLayout.LayoutParams(-1, 1).apply { topMargin = context.dp(10) }
    })
}

fun date(iso: String): String =
    Regex("""(\d{4})-(\d{2})-(\d{2})""").find(iso)?.destructured?.let { (y, m, d) -> "$d/$m/$y" } ?: iso

fun euro(v: Double) = String.format(Locale.FRANCE, "%.2f €", v)

fun Context.iconButton(icon: Int, description: String) = ImageButton(this).apply {
    setImageResource(icon)
    contentDescription = description
    imageTintList = ColorStateList.valueOf(Palette.ACCENT)
    val ripple = android.util.TypedValue()
    theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, ripple, true)
    setBackgroundResource(ripple.resourceId)
    layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
}

/** Pads the view by the status bar, navigation bar and keyboard so content is not drawn under them. */
fun View.fitSystemBars() {
    setOnApplyWindowInsetsListener { v, insets ->
        if (Build.VERSION.SDK_INT >= 30) {
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
        } else {
            @Suppress("DEPRECATION")
            v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
        }
        insets
    }
}

/** An email address that opens the mail app on tap and is copied on long press. */
fun Context.emailLink(address: String) = text(address, 14f, Palette.ACCENT).apply {
    paint.isUnderlineText = true
    setPadding(0, dp(8), 0, dp(8))
    setOnClickListener {
        try {
            startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address")))
        } catch (e: ActivityNotFoundException) {
            copy(address)
        }
    }
    setOnLongClickListener { copy(address); true }
}

private fun Context.copy(value: String) {
    getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("email", value))
    // Android 13 and later show their own confirmation.
    if (Build.VERSION.SDK_INT < 33) Toast.makeText(this, "Adresse copiée", Toast.LENGTH_SHORT).show()
}

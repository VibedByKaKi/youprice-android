package net.kaki87.youprice

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var session: Session
    private lateinit var api: Api

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Palette.init(this)
        session = Session(this)
        api = Api(session)
        if (session.token != null) showDashboard() else showLogin()
    }

    private fun background(thread: () -> Unit) = Thread(thread).start()

    private fun showLogin(error: String? = null) {
        val email = field("Email", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS).apply { setText(session.email) }
        val password = field("Mot de passe", InputType.TYPE_TEXT_VARIATION_PASSWORD).apply { setText(session.savedPassword) }
        val remember = CheckBox(this).apply {
            text = "Se souvenir du mot de passe"
            setTextColor(Palette.MUTED)
            isChecked = session.savedPassword != null
        }
        val code = field("Code de confirmation", InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS).apply { visibility = View.GONE }
        val message = text(error ?: "", 13f, Palette.ERROR)
        val submit = Button(this).apply { text = "Se connecter" }

        submit.setOnClickListener {
            submit.isEnabled = false
            message.text = ""
            val needCode = code.visibility == View.VISIBLE
            val (e, p, c) = Triple(email.text.toString().trim(), password.text.toString(), code.text.toString().trim())
            val keep = remember.isChecked
            background {
                try {
                    val signedIn = if (needCode) { api.loginWithCode(e, p, c); true } else api.login(e, p)
                    session.savedPassword = if (keep) p else null
                    runOnUiThread {
                        if (signedIn) {
                            showDashboard()
                        } else {
                            code.visibility = View.VISIBLE
                            message.text = "Un code de confirmation vous a été envoyé."
                            submit.isEnabled = true
                        }
                    }
                } catch (ex: Exception) {
                    runOnUiThread { message.text = ex.message ?: "Erreur réseau"; submit.isEnabled = true }
                }
            }
        }

        setContentView(ScrollView(this).apply {
            setBackgroundColor(Palette.BG)
            fitSystemBars()
            addView(column(24).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                addView(text("YouPrice", 28f, Palette.ACCENT, bold = true))
                addView(text("Identifiez-vous pour accéder à votre espace client.", 14f, Palette.MUTED))
                addView(email); addView(password); addView(remember); addView(code); addView(message); addView(submit)
            })
        })
    }

    private fun showDashboard() {
        val content = column()
        val status = text("Chargement…", 14f, Palette.MUTED).apply { setPadding(dp(16), dp(16), dp(16), 0) }
        val refresh = iconButton(R.drawable.ic_refresh, "Actualiser")
        val logout = iconButton(R.drawable.ic_logout, "Déconnexion")
        refresh.setOnClickListener { load(content, status) }
        logout.setOnClickListener { session.clearToken(); showLogin() }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Palette.BG)
            fitSystemBars()
            addView(LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(8), dp(8), dp(8))
                addView(text("YouPrice", 22f, Palette.ACCENT, bold = true).apply { layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
                addView(refresh); addView(logout)
            })
            addView(ScrollView(context).apply { addView(column().apply { addView(status); addView(content) }) })
        })
        load(content, status)
    }

    private fun load(content: LinearLayout, status: TextView) {
        status.text = "Chargement…"
        status.visibility = View.VISIBLE
        background {
            try {
                val dashboard = loadDashboard(api)
                runOnUiThread {
                    content.removeAllViews()
                    content.addView(dashboardView(dashboard))
                    status.visibility = View.GONE
                }
            } catch (ex: ApiException) {
                runOnUiThread {
                    if (ex.status == 401) { session.clearToken(); showLogin("Session expirée, reconnectez-vous.") }
                    else status.text = ex.message
                }
            } catch (ex: Exception) {
                runOnUiThread { status.text = ex.message ?: "Erreur réseau" }
            }
        }
    }

    private fun field(hint: String, type: Int) = EditText(this).apply {
        this.hint = hint
        // Single-line must be set before the input type, or it replaces the password masking.
        isSingleLine = true
        inputType = InputType.TYPE_CLASS_TEXT or type
    }
}

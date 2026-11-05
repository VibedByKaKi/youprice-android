package net.kaki87.youprice

import android.content.Context
import android.widget.LinearLayout
import org.json.JSONObject

/** Builds the single-page dashboard: profile, orders and lines, consumption, invoices, support tickets. */
fun Context.dashboardView(d: Dashboard) = column().apply {
    val c = d.customer
    addView(card("Mes informations") {
        row("Nom", "${c.str("firstName")} ${c.str("lastName")}".trim())
        row("Email", c.str("email"))
        row("Téléphone", c.str("phoneNumber"))
        row("Naissance", date(c.str("birthDate")))
        row("Titulaire du compte", c.str("accountHolder"))
        row("Banque", c.str("bankName"))
        row("IBAN", c.str("iban"))
        c.obj("billingAddress")?.let { subtitle("Adresse de facturation"); address(it) }
        c.obj("deliveryAddress")?.let { subtitle("Adresse de livraison"); address(it) }
    })

    addView(card("Mon forfait") {
        if (d.orders.length() == 0) note("Aucune commande.")
        d.orders.forEachObject { order ->
            subtitle("Commande ${order.str("reference")}")
            row("Statut", order.str("status"))
            row("Date", date(order.str("createDate")))
            row("Offre", order.str("offres"))
            row("Total HT", euro(order.optDouble("totaleHTPrice", 0.0)))
            order.arr("items").forEachObject { item ->
                divider()
                orderItem(item, d.lines.find { it.phone == item.str("phoneNumber") })
            }
        }
    })

    addView(card("Mon suivi conso") {
        note("Votre forfait se réinitialise le 1er de chaque mois.")
        if (d.lines.isEmpty()) note("Aucune ligne active.")
        d.lines.forEach { line ->
            subtitle(line.phone)
            val categories = line.conso?.arr("categories")
            if (categories == null || categories.length() == 0) note("Aucune consommation disponible.")
            categories?.forEachObject { cat ->
                addView(text(cat.str("libelle"), 14f, Palette.ACCENT, bold = true))
                cat.arr("sousCategories").forEachObject { sub ->
                    addView(text(sub.str("libelle"), 13f, Palette.MUTED))
                    sub.arr("detais").forEachObject { det ->
                        val ref = det.str("valeurRef")
                        row("", if (ref.isNotEmpty()) "${det.str("valeur")} / $ref utilisés" else "${det.str("valeur")} ${det.str("libelle")}")
                    }
                }
            }
        }
    })

    addView(card("Mes factures") {
        invoices(d.monthlyInvoices)
        if (d.otherInvoices.length() > 0) {
            subtitle("Mes factures diverses")
            invoices(d.otherInvoices)
        }
    })

    addView(card("Assistance") {
        if (d.tickets.length() == 0) note("Aucun ticket.")
        d.tickets.forEachObject { t ->
            divider()
            row("Ticket", "#${t.str("id")}")
            row("Raison", t.str("raison"))
            row("Sujet", t.str("sujet"))
            row("Création", t.str("dtCreation"))
            row("État", t.str("etat"))
        }
        note("Contact : support@youprice.fr")
    })
}

private fun LinearLayout.address(a: JSONObject) {
    row("", "${a.str("firstName")} ${a.str("lastName")}".trim())
    row("", a.str("street"))
    row("", a.str("addressComplement"))
    row("", "${a.str("zipCode")} ${a.str("city")}".trim())
}

private fun LinearLayout.invoices(list: org.json.JSONArray) {
    if (list.length() == 0) note("Aucune facture.")
    list.forEachObject { inv ->
        val partial = inv.str("invoiceStatus").contains("partiellement", ignoreCase = true)
        subtitle("${inv.str("invoiceName")} du ${date(inv.str("invoiceDate"))}")
        row("Statut", inv.str("invoiceStatus"))
        row("Montant TTC", inv.str("montantTTC") + " €")
        if (partial) row("Reste à payer", inv.str("montantRestant") + " €")
    }
}

private fun LinearLayout.orderItem(item: JSONObject, line: Line?) {
    val info = line?.info
    val st = line?.status
    addView(context.text("${item.str("productName")} · ${item.str("operateur")}", 16f, bold = true))
    row("Numéro", item.str("phoneNumber"))
    row("Commande", item.str("status"))
    row("État de la ligne", info?.str("etatLigne") ?: item.str("etatLigne"))
    row("En cours", st?.let(::pendingOperations))
    row("Type de SIM", item.str("typeSim"))
    row("Livraison", item.str("typeLivraison"))
    row("Activation", when (item.str("actvationtype")) { "NP" -> "Portabilité"; "NN" -> "Nouveau numéro"; else -> item.str("actvationtype") })
    row("Portabilité", date(item.str("portabilityDate")).takeIf { item.str("portabilityDate").isNotEmpty() })
    row("Statut portage", item.str("portageStatus"))
    row("Prix HT", euro(item.optDouble("priceHT", 0.0)))
    if (item.optBoolean("isOption5G")) row("Option 5G TTC", euro(item.optDouble("price5GTTC", 0.0)))
    row("Frais d'activation HT", item.optDouble("activationPriceHT", 0.0).takeIf { it > 0 }?.let(::euro))
    row("Frais de livraison HT", item.optDouble("shippingPriceHT", 0.0).takeIf { it > 0 }?.let(::euro))
    row("5G", st?.let { if (it.optBoolean("is5G")) "Activée" else "Désactivée" })
    row("Appels spéciaux", line?.special?.obj("data")?.let { if (it.optBoolean("appel")) "Activés" else "Désactivés" })
    row("SMS spéciaux", line?.special?.obj("data")?.let { if (it.optBoolean("sms")) "Activés" else "Désactivés" })
    row("Numéro de SIM", item.str("simsn"))
    row("Code RIO", line?.rio)
    row("Code PUK", line?.puk ?: item.str("puk"))
    row("Résiliation", info?.str("dateResiliation")?.takeIf { it.isNotEmpty() }?.let(::date))
    row("Changement de forfait", st?.str("newForfait"))
}

private fun pendingOperations(st: JSONObject): String? {
    val labels = mapOf(
        "isResumEnCour" to "réactivation", "isSuspendEnCour" to "suspension", "isResilEnCour" to "résiliation",
        "is5GActivateEnCour" to "activation 5G", "is5GDeactivateEnCour" to "désactivation 5G",
        "isChangementForfaitEnCour" to "changement de forfait", "isInProv" to "provisioning",
    )
    return labels.filter { st.optBoolean(it.key) }.values.joinToString(", ").ifEmpty { null }
}

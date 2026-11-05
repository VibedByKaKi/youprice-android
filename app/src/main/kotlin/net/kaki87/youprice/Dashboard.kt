package net.kaki87.youprice

import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class Line(
    val phone: String,
    val info: JSONObject?,
    val status: JSONObject?,
    val puk: String?,
    val rio: String?,
    val special: JSONObject?,
    val conso: JSONObject?,
)

class Dashboard(
    val customer: JSONObject,
    val orders: JSONArray,
    val lines: List<Line>,
    val monthlyInvoices: JSONArray,
    val otherInvoices: JSONArray,
    val tickets: JSONArray,
)

/** Fetches everything the web dashboard shows. Only a failed customer call, or an expired session, aborts the load. */
fun loadDashboard(api: Api): Dashboard {
    val pool = Executors.newFixedThreadPool(6)
    try {
        fun <T> async(block: () -> T?) = pool.submit(Callable {
            try {
                block()
            } catch (e: ApiException) {
                if (e.status == 401) throw e else null
            } catch (e: Exception) {
                null
            }
        })

        val customer = async { (api.get("/Customer/GetCustomerBrefInfoById") as JSONObject).getJSONObject("result") }
        val orders = async { (api.get("/Order/GetCustomerOrderInfo") as JSONObject).getJSONArray("result") }
        val phones = async { api.get("/msisdn/getActiveNumero") as? JSONArray }
        val monthly = async { api.get("/invoice/getMonthlyInvoices") as? JSONArray }
        val other = async { api.get("/invoice/getDiversInvoices") as? JSONArray }
        val tickets = async { api.get("/Ticketing/GetTickets") as? JSONArray }

        val lines = (phones.unwrap() ?: JSONArray()).let { arr -> (0 until arr.length()).map { arr.getString(it) } }.map { p ->
            val info = async { api.get("/msisdn/getLigneGsm?phoneNumber=$p") as? JSONObject }
            val status = async { api.postPhone("/msisdn/getStatusLigneGsm", p) as? JSONObject }
            val puk = async { (api.postPhone("/msisdn/getCodePuk", p) as? JSONObject)?.optString("codePuk") }
            val rio = async { (api.postPhone("/msisdn/getCodeRio", p) as? JSONObject)?.optString("codeRio") }
            val special = async { api.get("/msisdn/getStatusAppelSmsSpecials?phoneNumber=$p") as? JSONObject }
            val conso = async { api.postPhone("/invoice/getSuiviConsoInfo", p) as? JSONObject }
            Line(p, info.unwrap(), status.unwrap(), puk.unwrap(), rio.unwrap(), special.unwrap(), conso.unwrap())
        }

        return Dashboard(
            customer = customer.unwrap() ?: throw ApiException(0, "Impossible de charger le profil"),
            orders = orders.unwrap() ?: JSONArray(),
            lines = lines,
            monthlyInvoices = monthly.unwrap().sortedByDate(),
            otherInvoices = other.unwrap().sortedByDate(),
            tickets = tickets.unwrap() ?: JSONArray(),
        )
    } finally {
        pool.shutdown()
    }
}

private fun <T> java.util.concurrent.Future<T?>.unwrap(): T? =
    try {
        get()
    } catch (e: java.util.concurrent.ExecutionException) {
        throw e.cause ?: e
    }

private fun JSONArray?.sortedByDate(): JSONArray {
    val items = this?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) } } ?: return JSONArray()
    return JSONArray(items.sortedByDescending { it.str("invoiceDate") })
}

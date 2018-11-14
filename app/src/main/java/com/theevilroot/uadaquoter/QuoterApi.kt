package com.theevilroot.uadaquoter

import android.content.Context
import android.content.Intent
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.theevilroot.uadaquoter.objects.Quote
import com.theevilroot.uadaquoter.utils.contains
import com.theevilroot.uadaquoter.utils.getOrDef
import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.call.receive
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.client.response.readBytes
import io.ktor.client.response.readText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.encodeURLPath
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL

object QuoterApi {

    var quotes = ArrayList<Quote>()

    private val parser = JsonParser()
    private val response_errors = mapOf(
            "KEY_NOT_VALID" to "Invalid key",
            "QUOTE_NOT_FOUND" to "Quote not found",
            "INVALID_ID" to "Invalid quote id",
            "INVALID_MODE" to "Invalid mode",
            "MODE_NOT_SET" to "Mode not set",
            "TASK_NOT_SET" to "Task not set",
            "INVALID_TASK" to "Invalid task")

    private const val backendUrl = "http://52.48.142.75:8888/backend/quoter"

    fun req(reqUrl: String, args: Map<String, String>, onLoad: (JsonObject) -> Unit, onError: (Throwable?) -> Unit) =  GlobalScope.async {
        try {
            val client = HttpClient()
            val res = client.call(URL(reqUrl)) {
                method = HttpMethod.Post
                body = FormDataContent(Parameters.build {
                    args.forEach { t, u ->
                        append(t, u)
                    }
                })
            }
            when(res.response.status.value) {
                200 -> onLoad(parser.parse(res.response.readText()).asJsonObject)
                404 -> onError(APIException("Backend does not exists"))
                500 -> onError(APIException("Server is busy"))
                403 -> onError(APIException("Server permission error"))
                418 -> onError(APIException("Server is a teapot"))
                else -> onError(APIException("Server error: ${res.response.status.value}"))
            }
        }catch (e: Throwable) {
            onError(e)
        }
    }

    fun getPos(pos: Int, onLoad: (Quote) -> Unit, onError: (Throwable?) -> Unit) = req(backendUrl, mapOf("task" to "GET", "mode" to "pos", "pos" to pos.toString()), { json ->
        if(arrayOf("error", "message", "data") !in json)
            return@req onError(APIException("Malformed response"))
        if(json["error"].asBoolean)
            return@req onError(APIException("Error: ${response_errors.getOrDef(json["message"].asString, json["message"].asString)}"))
        val data = json["data"].asJsonObject
        if(arrayOf("id", "author", "adder", "author", "quote", "edited_by", "edited_at") !in data)
            return@req onError(APIException("Malformed response data"))
        onLoad(Quote.fromJson(data))
    }, onError)

    fun getFromTo(from: Int, to: Int, onLoad: (List<Quote>) -> Unit, onError: (Throwable?) -> Unit) = req(backendUrl, mapOf("task" to "GET", "mode" to "fromto", "from" to from.toString(), "to" to to.toString()), { json ->
        if(arrayOf("error", "message", "data") !in json)
            return@req onError(APIException("Malformed response"))
        if(json["error"].asBoolean)
            return@req onError(APIException("Error: ${response_errors.getOrDef(json["message"].asString, json["message"].asString)}"))
        val data = json["data"].asJsonObject
        if("quotes" !in data)
            return@req onError(APIException("Malformed response data"))
        val quotes = data["quotes"].asJsonArray
        onLoad(quotes.map { it.asJsonObject }.map { Quote.fromJson(it) })
    }, onError)


    fun getRandom(onLoad: (Quote) -> Unit, onError: (Throwable?) -> Unit) = req(backendUrl, mapOf("task" to "GET", "mode" to "rand"), { json ->
        if(arrayOf("error", "message", "data") !in json)
            return@req onError(APIException("Malformed response"))
        if(json["error"].asBoolean)
            return@req onError(APIException("Error: ${response_errors.getOrDef(json["message"].asString, json["message"].asString)}"))
        val data = json["data"].asJsonObject
        if(arrayOf("id", "author", "adder", "author", "quote", "edited_by", "edited_at") !in data)
            return@req onError(APIException("Malformed response data"))
        onLoad(Quote.fromJson(data))
    }, onError)

    fun getTotal(onLoad: (Int) -> Unit, onError: (Throwable?) -> Unit) = req(backendUrl, mapOf("task" to "GET", "mode" to "total"), { json ->
        if(arrayOf("error", "message", "data") !in json)
            return@req onError(APIException("Malformed response"))
        if(json["error"].asBoolean)
            return@req onError(APIException("Error: ${response_errors.getOrDef(json["message"].asString, json["message"].asString)}"))
        val data = json["data"].asJsonObject
        if("count" !in data)
            return@req onError(APIException("Malformed response data"))
        onLoad(data["count"].asInt)
    }, onError)

    fun add(adder: String, author: String,quote: String ,key: String, onLoad: () -> Unit, onError: (Throwable?) -> Unit) = req(backendUrl, mapOf("task" to "ADD", "key" to key, "author" to author, "adder" to adder, "quote" to quote), { json ->
        if(arrayOf("error", "message") !in json)
            return@req onError(APIException("Malformed response"))
        if(json["error"].asBoolean)
            return@req onError(APIException("Error: ${response_errors.getOrDef(json["message"].asString, json["message"].asString)}"))
        onLoad()
    }, onError)

    fun edit(id: Int, edited_by: String, quote: String, key: String, onLoad: () -> Unit, onError: (Throwable?) -> Unit) = req(backendUrl, mapOf("task" to "EDIT", "key" to key, "id" to id.toString(), "edited_by" to edited_by, "new_text" to quote), { json ->
        if(arrayOf("error", "message") !in json)
            return@req onError(APIException("Malformed response"))
        if(json["error"].asBoolean)
            return@req onError(APIException("Error: ${response_errors.getOrDef(json["message"].asString, json["message"].asString)}"))
        onLoad()
    }, onError)

    fun saveCache(filesDir: File, onSuccess: () -> Unit, onError: (Throwable?) -> Unit) = GlobalScope.async {
        try {
            val file = File(filesDir, "cache.json")
            if (!file.exists())
                file.createNewFile()
            val json = JsonObject()
            val arr = JsonArray()
            quotes.forEach { arr.add(it.toJson()) }
            json.add("quotes", arr)
            file.writeText(GsonBuilder().setPrettyPrinting().create().toJson(json))
            onSuccess()
        } catch (e: Throwable) {
            onError(e)
        }
    }
    fun loadCache(filesDir: File, onLoad: (List<Quote>) -> Unit, onError: (Throwable?) -> Unit) = GlobalScope.async {
        try {
            val file = File(filesDir, "cache.json")
            if (!file.exists())
                return@async onError(FileNotFoundException())
            val json = JsonParser().parse(file.readText()).asJsonObject
            onLoad(json.get("quotes").asJsonArray.map { it.asJsonObject }.map { Quote.fromJson(it) }.map { it.cached = true ; it})
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun getAdderName(context: Context): String {
        val preferences = context.getSharedPreferences("UADAQuoter", Context.MODE_PRIVATE)
        return preferences.getString("adderName", "") ?: ""
    }

    fun setAdderName(context: Context, name: String): String {
        val preferences = context.getSharedPreferences("UADAQuoter", Context.MODE_PRIVATE)
        preferences.edit().putString("adderName", name).apply()
        return getAdderName(context)
    }

    fun shareQuote(context: Context, quote: Quote) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, "${quote.author}:\n${quote.text}\n(c) Цитатник UADAF")
        intent.type = "text/plain"
        context.startActivity(intent)
    }

    class APIException(msg: String): IOException(msg)

}
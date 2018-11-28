package com.theevilroot.uadaquoter

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.gson.*
import com.jakewharton.rxbinding3.widget.textChanges
import com.theevilroot.uadaquoter.objects.Quote
import com.theevilroot.uadaquoter.objects.TextWatcherWrapper
import com.theevilroot.uadaquoter.utils.DialogCanceledException
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

object RxQuoterApi {

    private const val backendUrl = "http://192.168.0.100:6741/api/quote/"
    private const val defaultHeader = "Connection: close"

    private val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(backendUrl)
            .build()

    private var service: QuoterService? = null

    private fun quoter(mocked: Boolean = false): QuoterService {
        if (service == null) {
            service = retrofit.create(QuoterService::class.java)
            if (!mocked)
                Log.i(this::class.java.simpleName, "QuoterService singleton is null. Reinitializing...")
        }
        return service!!
    }

    class QuoterServiceApi(context: Context, private val mocked: Boolean = false) {

        private val sharedPreferences: SharedPreferences = context.getSharedPreferences("UADAQuoter", Context.MODE_PRIVATE)!!
        private val filesDir: File = context.filesDir
        private val gson: Gson = GsonBuilder().setPrettyPrinting().create()!!
        private val parser: JsonParser = JsonParser()

        /**
         *  @param pos - Position of requseting quote
         *  @return Single of requesing quote
         */
        fun getPos(pos: Int): Single<Quote> = Single.create<Quote> {
            try {
                val result = quoter(mocked).pos(pos)
                val response = result.execute()
                if (!response.isSuccessful)
                    it.onError(IOException())
                val quote = response.body()!!
                it.onSuccess(quote)
            } catch (e: Exception) {
                it.onError(e)
            }
        }

        /**
         *  @param from - Position of first quote (Include)
         *  @param to - Position of last quote. Might be out of range (Exclude)
         *  @return Observable of quotes from range.
         */
        fun getRange(from: Int, to: Int): Observable<Quote> = Observable.create<Quote> {
            try {
                val result = quoter(mocked).range(from, to)
                val response = result.execute()
                if (!response.isSuccessful)
                    it.onError(IOException())
                val quotes = response.body()!!
                quotes.forEach(it::onNext)
                it.onComplete()
            } catch (e: Exception) {
                it.onError(e)
            }
        }

        /**
         * @param count - Count of random quotes. 1 by default
         * @return Observable of quotes.
         */
        fun getRandom(count: Int = 1): Observable<Quote> = Observable.create<Quote> {
            try {
                val result = quoter(mocked).random(count)
                val response = result.execute()
                if (!response.isSuccessful)
                    it.onError(IOException())
                val quotes = response.body()!!
                quotes.forEach(it::onNext)
                it.onComplete()
            } catch (e: Exception) {
                it.onError(e)
            }
        }

        /**
         *  @return Observable of all quotes.
         */
        fun getAll(): Observable<Quote> = Observable.create<Quote> {
            try {
                val result = quoter(mocked).all()
                val response = result.execute()
                if (!response.isSuccessful)
                    it.onError(IOException())
                val quotes = response.body()!!
                quotes.forEach(it::onNext)
                it.onComplete()
            } catch (e: Exception) {
                it.onError(e)
            }
        }

        /**
         *  @return Count of quotes in database
         */
        fun getTotal(): Single<Int> = Single.create<Int> {
            try {
                val result = quoter(mocked).total()
                val response = result.execute()
                if (!response.isSuccessful)
                    it.onError(IOException())
                val count = response.body()!!
                it.onSuccess(count)
            } catch (e: Exception) {
                it.onError(e)
            }
        }

        /**
         *  @param key - Sucurity key for backend
         *  @param adder - Adder name
         *  @param author - Author name
         *  @param quote - quote's text
         *  @returm Completable. Throws error if operation is failed.
         */
        fun add(key: String, adder: String, author: String, quote: String): Completable = Completable.create {
            try {
                val result = quoter().add(key, adder, author, quote)
                val response = result.execute()
                if (!response.isSuccessful)
                    it.onError(IOException())
                it.onComplete()
            } catch (e: Exception) {
                it.onError(e)
            }
        }

        /**
         *  @param key - Sucurity key for backend
         *  @param id - Position of quote you want to edit
         *  @param editedBy - Editor's name
         *  @param newText - Text that will replace current quote
         *  @returm Completable. Throws error if operation is failed.
         */
        fun edit(key: String, id: Int, editedBy: String, newText: String): Completable = Completable.create {
            try {
                val result = quoter().edit(key, id, editedBy, newText)
                val response = result.execute()
                if (!response.isSuccessful)
                    it.onError(IOException())
                it.onComplete()
            } catch (e: Exception) {
                it.onError(e)
            }
        }

        /**
         *  @param context - Context to show intent
         *  @param quote - Quote to share
         */
        fun share(context: Context, quote: Quote) {
            val intent = Intent(Intent.ACTION_SEND)
            intent.putExtra(Intent.EXTRA_TEXT, "${quote.author}:\n${quote.quote}\n(c) Цитатник UADAF")
            intent.type = "quote/plain"
            context.startActivity(intent)
        }

        /**
         *  @return Current username if specified. If not - null
         */
        fun username(): String? =
                sharedPreferences.getString("adderName", "") ?: ""

        /**
         *  @param newName - Name you want to set
         *  @return New username
         */
        fun username(newName: String) =
                sharedPreferences.edit().putString("adderName", newName).apply()

        /**
         * @return Observable of quoutes loaded from cache file. Throws {@link java.io.FileNotFoundException} if doesn't exists.
         */
        fun getCache(): Observable<Quote> = Observable.create<Quote> {
            try {
                val file = File(filesDir, "cache.json")
                if (file.exists()) {
                    val fileText = file.readText()
                    val jsonRoot = parser.parse(fileText).asJsonObject
                    val jsonArray = jsonRoot.get("quotes").asJsonArray
                    val quotes = jsonArray.map(JsonElement::getAsJsonObject)
                            .map(Quote.Companion::fromJson)
                            .map(Quote::makeCached)
                    for(quote in quotes)
                        it.onNext(quote)
                    it.onComplete()
                } else {
                    it.onError(FileNotFoundException())
                }
            } catch (e: Exception) {
                it.onError(e)
            }
        }

        /**
         * @param quotes - List of quotes that will be saved into cache file
         * @return Completable. Throws exception if operation is failed (Including permission exception)
         */
        fun setCache(quotes: List<Quote>): Completable = Completable.create {
            try {
                val file = File(filesDir, "cache.json")
                if (!file.exists() && !file.createNewFile())
                    return@create it.onError(IOException("Cannot create cache file"))
                val array = JsonArray()
                val root = JsonObject()
                quotes.map(Quote::toJson).forEach(array::add)
                root.add("quotes", array)
                val text = gson.toJson(root)
                file.writeText(text)
                it.onComplete()
            }catch (e: Exception) {
                it.onError(e)
            }
        }

        /**
         * @param context - Context to show dialog
         * @param defaultValue - String that will be showed in dialog on show
         * @param cancelable - Can dialog be canceled
         * @return Single of String with entered username. Throws {@link com.theevilroot.uadaquoter.utils.DialogCanceledException} of user clicked cancel button or canceled dialog.
         */
        fun requestUserdata(context: Context, defaultValue: String, cancelable: Boolean = true): Single<String> = Single.create<String> { emitter ->
            val view = LayoutInflater.from(context).inflate(R.layout.personal_data, null, false)
            val dialog = AlertDialog.Builder(context).setView(view).setCancelable(cancelable).create()
            with(view) {
                val label = findViewById<TextView>(R.id.personal_data_label)
                val adderNameField = findViewById<EditText>(R.id.personal_data_adder_name_field)
                val saveBtn = findViewById<ImageButton>(R.id.personal_data_save)
                val cancelBtn= findViewById<ImageButton>(R.id.personal_data_cancel)

                adderNameField.setText(defaultValue)

                if(!cancelable)
                    cancelBtn.visibility = View.VISIBLE

                saveBtn.setOnClickListener { emitter.onSuccess(adderNameField.text.toString()) }
                cancelBtn.setOnClickListener { emitter.onError(DialogCanceledException()) }
            }
            dialog.show()
        }

        /**
         *  @param context - Context to show dialog
         */
        fun showSecurityDialog(context: Context): Single<String> = Single.create <String> {
            val view = LayoutInflater.from(context).inflate(R.layout.security_code_dialog_layout, null, false)
            val dialog = AlertDialog.Builder(context).setView(view).create()
            val pinView = view.findViewById<EditText>(R.id.security_code_field)

            val disposable = pinView.textChanges()
                    .map(CharSequence::toString)
                    .filter(App.instance.references::isKeyValid)
                    .subscribe { key ->
                        it.onSuccess(key)
                        dialog.dismiss()
                    }
            dialog.setOnCancelListener { _ ->
                disposable.dispose()
                it.onError(DialogCanceledException())
            }
            dialog.setOnDismissListener { _ ->
                disposable.dispose()
            }
            dialog.show()
        }

        fun isServiceAvailable(): Completable = Completable.create {
            try {
                quoter().available()
                it.onComplete()
            } catch (t: Throwable) {
                it.onError(t)
            }
        }

    }


    private interface QuoterService {

        @GET("pos/{pos}")
        @Headers(defaultHeader)
        fun pos(@Path("pos") pos: Int): Call<Quote>

        @GET("range/{from}/{to}")
        @Headers(defaultHeader)
        fun range(
                @Path("from") from: Int,
                @Path("to") to: Int): Call<List<Quote>>

        @Headers(defaultHeader)
        @GET("random/{count}")
        fun random(@Path("count") count: Int = 1): Call<List<Quote>>

        @Headers(defaultHeader)
        @GET("all")
        fun all(): Call<List<Quote>>

        @Headers(defaultHeader)
        @GET("total")
        fun total(): Call<Int>

        @Headers(defaultHeader)
        @FormUrlEncoded
        @POST("add")
        fun add(
                @Field("key") key: String,
                @Field("adder") adder: String,
                @Field("author") author: String,
                @Field("quote") quote: String
        ): Call<ResponseBody>

        @Headers(defaultHeader)
        @FormUrlEncoded
        @POST("edit")
        fun edit(
                @Field("key") key: String,
                @Field("id") id: Int,
                @Field("edited_by") editedBy: String,
                @Field("new_text") newText: String
        ): Call<ResponseBody>

        @Headers(defaultHeader)
        @GET("/")
        fun available(): Call<ResponseBody>
    }

}
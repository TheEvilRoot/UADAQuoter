package com.theevilroot.uadaquoter.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.theevilroot.uadaquoter.*
import com.theevilroot.uadaquoter.adapters.QuotesAdapter
import org.jsoup.Jsoup
import java.io.File
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

    lateinit var app: App

    lateinit var toolbar: Toolbar
    lateinit var quotesList: ListView
    lateinit var loadingProcess: ProgressBar
    lateinit var rootLayout: ConstraintLayout
    lateinit var searchStatus: TextView
    lateinit var searchLayout: ConstraintLayout
    lateinit var searchClose: ImageButton
    lateinit var searchIgnorCase: IgnoreCaseButton
    lateinit var searchField: EditText

    lateinit var imm: InputMethodManager

    var ignoreLocal: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        app = application as App
        quotesList = findViewById(R.id.quotes_list)
        loadingProcess = findViewById(R.id.progressBar)
        toolbar = findViewById(R.id.toolbar)
        rootLayout = findViewById(R.id.root_layout)
        searchStatus = findViewById(R.id.search_status)
        searchLayout = findViewById(R.id.search_layout)
        searchClose = findViewById(R.id.search_close)
        searchIgnorCase = findViewById(R.id.search_ignorcase)
        searchField = findViewById(R.id.search_field)
        imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        setSupportActionBar(toolbar)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.window_close)
        supportActionBar!!.title = "Цитаты"
        quotesList.setOnItemLongClickListener { _, _, position, _ ->
            val intent = Intent(this, EditQuoteActivity::class.java)
            intent.putExtra("quote", GsonBuilder().create().toJson((quotesList.adapter.getItem(position) as Quote).toJson()))
            startActivity(intent)
            true
        }
        searchClose.setOnClickListener { closeSearch() }
        searchIgnorCase.setOnClickListener {
            searchIgnorCase.turnIgnoreCase()
            onSearch(searchField.text.toString(), searchIgnorCase)
        }
        searchField.addTextChangedListener(TextWatcherWrapper(onChange = {str, _,_,_ -> onSearch(str, searchIgnorCase)}))
        searchField.setOnKeyListener { _, keyCode, event ->
            if(event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                closeSearch()
                imm.hideSoftInputFromWindow(searchField.windowToken, 0)
            }
            false
        }
        load()
        loadUserdata()
    }

    private fun loadUserdata() {
        thread(true) {
            val file = File(filesDir, "user.json")
            try {
                app.adderName = if (file.exists()) {
                    JsonParser().parse(file.readText()).asJsonObject["adderName"].asString
                } else {
                    ""
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showLoading() {
        quotesList.visibility = View.GONE
        loadingProcess.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        loadingProcess.visibility = View.GONE
    }

    private fun showList() {
        quotesList.visibility = View.VISIBLE
        loadingProcess.visibility = View.GONE
    }

    private fun updateUI() {
        runOnUiThread {
            supportActionBar!!.title = getString(R.string.app_name)
            supportActionBar!!.subtitle = "Загружено ${app.quotes.size} цитат ${if (app.quotes.isNotEmpty() &&  app.quotes[0].cached) "из кэша" else ""}"
            quotesList.adapter = QuotesAdapter(this, app.quotes.toTypedArray())
        }
    }

    private fun loadRemote(): Boolean {
        try {
            val allQuotes = Jsoup.connect("http://52.48.142.75:8888/backend/quoter").
                    ignoreContentType(true).
                    data("task", "GET").
                    data("mode", "fromto").
                    data("from", "0").
                    data("to", Integer.MAX_VALUE.toString()).post()
            var allJson = JsonParser().parse(allQuotes.text()).asJsonObject
            if(allJson.has("error") && allJson["error"].asBoolean) {
                Log.e("ERROR", "Server error:${allJson["message"]}")
                return false
            }
            allJson = allJson["data"].asJsonObject
            app.quotes.clear()
            Log.d("JSON", allJson.toString())
            app.quotes.addAll(allJson.get("quotes").asJsonArray.map { it.asJsonObject }.map {
                Quote(it.get("id").asString.toInt(), it.get("adder").asString, it.get("author").asString, it.get("quote").asString, false, if(it["edited_by"].isJsonNull) null else it["edited_by"].asString, if(it["edited_at"].isJsonNull) -1 else it["edited_at"].asLong)
            })
            saveToCache(allJson.get("quotes").asJsonArray)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun saveToCache(array: JsonArray) {
        try {
            val file = File(filesDir, "cache.json")
            if (!file.exists())
                file.createNewFile()
            val json = JsonObject()
            json.add("quotes", array)
            file.writeText(GsonBuilder().setPrettyPrinting().create().toJson(json))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadLocal(): Boolean {
        try {
            val file = File(filesDir, "cache.json")
            if (!file.exists())
                return false
            val json = JsonParser().parse(file.readText()).asJsonObject
            app.quotes.clear()
            app.quotes.addAll(json.get("quotes").asJsonArray.map { it.asJsonObject }.map {
                Quote(it.get("id").asString.toInt(), it.get("adder").asString, it.get("author").asString, it.get("quote").asString, true, it["editedBy"].asString, it["editedAt"].asLong)
            })
            app.quotes.sortBy { it.id }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                buildAlert(this, "Похоже с кешом что-то не так!", "Желаете очистить его?", "Да", "Нет!") { event ->
                    if(event) {
                        File(filesDir, "cache.json").delete()
                        load()
                        true
                    }else{
                        ignoreLocal = true
                        load()
                        true
                    }
                }
            }
            return false
        }
    }

    private fun load() {
        showLoading()
        searchStatus.visibility = View.GONE
        thread(true) {
            if (loadRemote()) {
                runOnUiThread { showList() }
                updateUI()
            } else {
                if (!ignoreLocal && loadLocal()) {
                    runOnUiThread { showList() }
                    updateUI()
                }else {
                    runOnUiThread {
                        showStatus("Похоже отсутствует соединение с интернетом. Проверьте подключение и повторите попытку!")
                        hideLoading()
                    }
                }
            }
        }
    }

    private fun showStatus(msg: String) {
        searchStatus.text = msg
        searchStatus.visibility = View.VISIBLE

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if(!isSearchShowed())
            menuInflater.inflate(R.menu.toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.tb_reload -> load()
            R.id.tb_search -> {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                showSearch()
            }
            R.id.tb_add -> {
                startActivity(Intent(this, NewQuoteActivity::class.java))
            }
            android.R.id.home -> {
                closeSearch()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (isSearchShowed()) {
            closeSearch()
        } else {
            super.onBackPressed()
        }
    }

    private fun showSearch() {
        searchLayout.animate().alphaBy(0F).alpha(1F).setDuration(100).setUpdateListener {
            if(it.animatedValue == 0F) {
                searchLayout.visibility = View.VISIBLE
                invalidateOptionsMenu()
            }
            if(it.animatedValue == 1F) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(searchField, 0)
            }
        }.start()
    }

    private fun isSearchShowed(): Boolean =
            searchLayout.visibility == View.VISIBLE

    private fun closeSearch() {
        with(currentFocus) {
            if (this != null)
                imm.hideSoftInputFromWindow(searchField.windowToken, 0)
        }
        searchLayout.animate().alphaBy(1F).alpha(0F).setDuration(100).setUpdateListener {
            if(it.animatedValue == 1F) {
                searchField.setText("")
                searchStatus.visibility = View.GONE
                searchLayout.visibility = View.GONE
                invalidateOptionsMenu()
                updateUI()
            }
        }.start()
    }

    private fun onSearch(searchValue: String, ignoreCaseButton: IgnoreCaseButton) {
        val str = if (!ignoreCaseButton.value)
            searchValue.toLowerCase()
        else
            searchValue
        val list: List<Quote> = if (searchValue.isNotBlank()) {
            app.quotes.filter {
                var (id, adder, author, text) = it
                if (ignoreCaseButton.value) {
                    text = text.toLowerCase()
                    adder = adder.toLowerCase()
                    author = author.toLowerCase()
                }
                text.contains(str) || adder.contains(str) || author.contains(str) || id.toString() == str
            }
        } else {
            emptyList()
        }
        quotesList.adapter = QuotesAdapter(this, list.toTypedArray())
        if (list.isEmpty()) {
            showStatus("По данному запросу не нашлось цитат. Введите чё-нить другое")
        } else {
            searchStatus.visibility = View.GONE
        }
    }
}
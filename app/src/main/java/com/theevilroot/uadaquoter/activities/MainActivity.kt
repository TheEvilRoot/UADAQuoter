package com.theevilroot.uadaquoter.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.theevilroot.uadaquoter.adapters.QuotesAdapter
import org.jsoup.Jsoup
import java.io.File
import kotlin.concurrent.thread
import android.graphics.drawable.BitmapDrawable
import android.renderscript.ScriptIntrinsicBlur
import android.renderscript.Allocation
import android.renderscript.RenderScript
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.provider.MediaStore
import android.support.constraint.ConstraintLayout
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.theevilroot.uadaquoter.*
import kotlinx.android.synthetic.main.main_activity.*
import java.net.URLEncoder


class MainActivity: AppCompatActivity() {

    lateinit var app: App

    lateinit var toolbar: Toolbar
    lateinit var quotesList: ListView
    lateinit var loadingProcess: ProgressBar
    lateinit var rootLayout: ConstraintLayout
    lateinit var searchStatus: TextView

    var quotes: ArrayList<Quote> = ArrayList()
    var searchMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        app = application as App
        quotesList = findViewById(R.id.quotes_list)
        loadingProcess = findViewById(R.id.progressBar)
        toolbar = findViewById(R.id.toolbar)
        rootLayout = findViewById(R.id.root_layout)
        searchStatus = findViewById(R.id.search_status)
        setSupportActionBar(toolbar)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.window_close)
        supportActionBar!!.title = "Цитаты"
        load()
        loadUserdata()
    }

    private fun loadUserdata() {
        thread(true)  {
            val file = File(filesDir, "user.json")
            try {
                app.adderName = if (file.exists()) {
                    JsonParser().parse(file.readText()).asJsonObject["adderName"].asString
                } else {
                    ""
                }
            }catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showLoading() {
        quotesList.visibility = View.GONE
        loadingProcess.visibility = View.VISIBLE
    }

    private fun showList() {
        quotesList.visibility = View.VISIBLE
        loadingProcess.visibility = View.GONE
    }

    private fun updateUI() {
        runOnUiThread {
            supportActionBar!!.title = getString(R.string.app_name)
            supportActionBar!!.subtitle = "Загружено ${quotes.size} цитат ${if(quotes.isNotEmpty() && quotes[0].cached) "из кэша" else ""} "
            quotesList.adapter = QuotesAdapter(this, quotes.toTypedArray())
        }
    }

    private fun loadRemote(): Boolean {
        try {
            val allQuotes = Jsoup.connect("http://52.48.142.75/backend/Quoter.php").data("task", "GET").data("mode", "fromto").data("from", "0").data("to", Integer.MAX_VALUE.toString()).post()
            val allJson = JsonParser().parse(allQuotes.text()).asJsonObject
            if(allJson.get("error").asBoolean)
                return false
            quotes.clear()
            quotes.addAll(allJson.get("quotes").asJsonArray.map { it.asJsonObject }.map {
                Quote(it.get("id").asString.toInt(), it.get("adder").asString, it.get("author").asString, it.get("quote").asString)
            })
            saveToCache(allJson.get("quotes").asJsonArray)
            return true
        }catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun saveToCache(array: JsonArray) {
        try {
            val file = File(filesDir, "cache.json")
            if(!file.exists())
                file.createNewFile()
            val json = JsonObject()
            json.add("quotes", array)
            file.writeText(GsonBuilder().setPrettyPrinting().create().toJson(json))
        }catch (e: Exception) { e.printStackTrace() }
    }

    private fun loadLocal(): Boolean {
        try{
            val file = File(filesDir, "cache.json")
            if(!file.exists())
                return false
            val json = JsonParser().parse(file.readText()).asJsonObject
            quotes.clear()
            quotes.addAll(json.get("quotes").asJsonArray.map { it.asJsonObject }.map {
                Quote(it.get("id").asString.toInt(), it.get("adder").asString, it.get("author").asString, it.get("quote").asString, true)
            })
            return true
        }catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun load() {
        showLoading()
        thread(true) {
            if(loadRemote()) {
                runOnUiThread { showList() }
                updateUI()
            }else{
                if(loadLocal()) {
                    runOnUiThread { showList() }
                    updateUI()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar, menu)
        if(searchMode) {
            for(i in 0 until menu.size()) {
                if(menu.getItem(i).itemId != R.id.tb_search)
                    menu.getItem(i).isVisible = false
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.tb_reload -> load()
            R.id.tb_search -> {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                val view = findViewById<View>(R.id.search_overlay)
                val searchField = view.findViewById<EditText>(R.id.search_overlay_field)
                val ignoreCaseButton = view.findViewById<IgnoreCaseButton>(R.id.search_overlay_ignore_case)
                val closeSearchButton = view.findViewById<ImageButton>(R.id.search_overlay_close)
                searchField.addTextChangedListener(TextWatcherWrapper(onChange = { s, _,_,_ -> onSearch(s, ignoreCaseButton) }))
                searchField.setOnKeyListener { v, keyCode, event ->
                    if(event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                        view.visibility = View.GONE
                        imm.hideSoftInputFromWindow(view.windowToken, 0)
                    }
                    false
                }
                ignoreCaseButton.setOnClickListener { ignoreCaseButton.turnIgnoreCase() ; onSearch(searchField.text.toString(), ignoreCaseButton) }
                closeSearchButton.setOnClickListener { view.visibility = View.GONE ; imm.hideSoftInputFromWindow(view.windowToken, 0);}
                view.visibility = View.VISIBLE
                searchField.requestFocus()
                imm.showSoftInput(searchField, 0)
            }

            R.id.tb_add -> {
                startActivity(Intent(this, NewQuoteActivity::class.java))
            }

            android.R.id.home -> {
                closeSearch(findViewById<View>(R.id.search_overlay))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val view = findViewById<View>(R.id.search_overlay)
        if(searchMode) {
            closeSearch(findViewById<View>(R.id.search_overlay))
        }else{
            if(view.visibility != View.GONE)
                view.visibility = View.GONE
            super.onBackPressed()
        }
    }

    private fun closeSearch(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        with(currentFocus) {
            if(this != null)
                imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
        view.findViewById<EditText>(R.id.search_overlay_field).setText("")
        view.visibility = View.GONE
        searchStatus.visibility = View.GONE
        updateUI()
        supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        searchMode = false
        invalidateOptionsMenu()
    }

    private fun onSearch(searchValue: String, ignoreCaseButton: IgnoreCaseButton) {
        val str = if(ignoreCaseButton.value)
            searchValue.toLowerCase()
        else
            searchValue
        val list: List<Quote> = if(searchValue.isNotBlank()) {
            quotes.filter {
                var (id, adder, author, text) = it
                if (ignoreCaseButton.value) {
                    text = text.toLowerCase()
                    adder = adder.toLowerCase()
                    author = author.toLowerCase()
                }
                text.contains(str) || adder.contains(str) || author.contains(str) || id.toString() == str
            }
        }else{
            emptyList()
        }
        quotesList.adapter = QuotesAdapter(this, list.toTypedArray())
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        searchMode = true
        invalidateOptionsMenu()
        supportActionBar!!.title = "Результаты поиска"
        supportActionBar!!.subtitle = str
        searchStatus.visibility = if(list.isEmpty()) { View.VISIBLE }else{ View.GONE }
    }
}
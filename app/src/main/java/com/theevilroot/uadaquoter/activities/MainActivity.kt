package com.theevilroot.uadaquoter.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.google.gson.JsonParser
import com.theevilroot.uadaquoter.*
import com.theevilroot.uadaquoter.adapters.QuotesAdapter
import java.io.File
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

    private lateinit var app: App
    private lateinit var imm: InputMethodManager
    private lateinit var adapter: QuotesAdapter

    val toolbar by bind<Toolbar>(R.id.toolbar)
    private val quotesView by bind<RecyclerView>(R.id.quotes_view)
    private val loadingProcess by bind<ProgressBar>(R.id.progressBar)
    private val searchStatus by bind<TextView>(R.id.search_status)
    private val searchLayout by bind<ConstraintLayout>(R.id.search_layout)
    private val searchClose by bind<ImageButton>(R.id.search_close)
    private val searchIgnoreCase by bind<IgnoreCaseButton>(R.id.search_ignorcase)
    private val searchField by bind<EditText>(R.id.search_field)

    var ignoreLocal: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        setSupportActionBar(toolbar)
        app = application as App
        imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        adapter = QuotesAdapter()
        quotesView.layoutManager = LinearLayoutManager(this)
        quotesView.adapter = adapter
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.window_close)
        supportActionBar!!.title = "Цитаты"
        searchClose.setOnClickListener { closeSearch() }
        searchIgnoreCase.setOnClickListener {
            searchIgnoreCase.turnIgnoreCase()
            onSearch(searchField.text.toString(), searchIgnoreCase)
        }
        searchField.addTextChangedListener(TextWatcherWrapper(onChange = {str, _,_,_ -> onSearch(str, searchIgnoreCase)}))
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
        quotesView.visibility = View.GONE
        loadingProcess.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        loadingProcess.visibility = View.GONE
    }

    private fun showList() {
        quotesView.visibility = View.VISIBLE
        loadingProcess.visibility = View.GONE
    }

    private fun updateUI() {
        runOnUiThread {
            supportActionBar!!.title = getString(R.string.app_name)
            supportActionBar!!.subtitle = "Загружено ${QuoterAPI.quotes.size} цитат ${if (QuoterAPI.quotes.isNotEmpty() &&  QuoterAPI.quotes[0].cached) "из кэша" else ""}"
            adapter.notifyDataSetChanged()
        }
    }

    private fun loadRemote() {
        QuoterAPI.getTotal({ count ->
            QuoterAPI.getFromTo(1, count, { quotes ->
                QuoterAPI.quotes.clear()
                QuoterAPI.quotes.addAll(quotes)
                QuoterAPI.saveCache(filesDir, {  }) {  }
                runOnUiThread { showList() }
                updateUI()
            }, {
                onRemoteError(it)
            })
        }) {
            onRemoteError(it)
        }
    }

    private fun onRemoteError(e: Throwable?) {
        if(!ignoreLocal) {
            QuoterAPI.loadCache(filesDir, { quotes ->
                QuoterAPI.quotes.clear()
                QuoterAPI.quotes.addAll(quotes)
                QuoterAPI.quotes.sortBy { it.id }
                runOnUiThread { showList() }
                updateUI()
            }) {
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
            }
        } else {
            showStatus("Похожу на проблему с сервером, не находите? Вот, почитайте, что он мне сказал: ${e?.localizedMessage}")
            hideLoading()
        }
    }

    private fun load() {
        showLoading()
        searchStatus.visibility = View.GONE
        loadRemote()
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
                // TODO: Refactor: showSearch()
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
            QuoterAPI.quotes.filter {
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
       //  quotesView.adapter = QuotesAdapter(this, list.toTypedArray())
        if (list.isEmpty()) {
            showStatus("По данному запросу не нашлось цитат. Введите чё-нить другое")
        } else {
            searchStatus.visibility = View.GONE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        try {
            adapter.saveStates(outState)
        }catch (e: Exception) { }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        try {
            adapter.restoreStates(savedInstanceState)
        }catch (e: Exception) { }
    }

}
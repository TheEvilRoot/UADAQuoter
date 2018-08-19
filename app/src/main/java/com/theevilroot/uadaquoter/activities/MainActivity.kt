package com.theevilroot.uadaquoter.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AlertDialog
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
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.theevilroot.uadaquoter.*
import com.theevilroot.uadaquoter.adapters.QuotesAdapter
import com.theevilroot.uadaquoter.adapters.SearchResultAdapter
import java.io.File
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

    private lateinit var app: App
    private lateinit var imm: InputMethodManager
    private lateinit var quotesAdapter: QuotesAdapter
    private lateinit var searchAdapter: SearchResultAdapter

    val toolbar by bind<Toolbar>(R.id.toolbar)
    private val quotesView by bind<RecyclerView>(R.id.quotes_view)
    private val loadingProcess by bind<ProgressBar>(R.id.progressBar)
    private val searchStatus by bind<TextView>(R.id.search_status)
    private val searchLayout by bind<ConstraintLayout>(R.id.search_layout)
    private val searchOverlayLayout by bind<ConstraintLayout>(R.id.search_overlay_layout)
    private val searchClose by bind<ImageButton>(R.id.search_close)
    private val searchIgnoreCase by bind<IgnoreCaseButton>(R.id.search_ignorcase)
    private val searchField by bind<EditText>(R.id.search_field)
    private val searchQuotesView by bind<RecyclerView>(R.id.search_list_view)

    var ignoreLocal: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        setSupportActionBar(toolbar)
        app = application as App
        imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        quotesAdapter = QuotesAdapter()
        searchAdapter = SearchResultAdapter { quote ->
            quotesView.scrollToPosition(QuoterAPI.quotes.indexOfFirst { it.id == quote.id })
            closeSearch()
        }
        quotesView.layoutManager = LinearLayoutManager(this)
        searchQuotesView.layoutManager = LinearLayoutManager(this)
        quotesView.adapter = quotesAdapter
        searchQuotesView.adapter = searchAdapter
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.window_close)
        supportActionBar!!.title = "Цитаты"
        searchClose.setOnClickListener { closeSearch() }
        searchIgnoreCase.setOnClickListener {
            searchIgnoreCase.turnIgnoreCase()
            onSearch(searchField.text.toString(), searchIgnoreCase.value)
        }
        searchOverlayLayout.setOnClickListener { closeSearch() }
        searchField.addTextChangedListener(TextWatcherWrapper(onChange = {str, _,_,_ -> onSearch(str, searchIgnoreCase.value)}))
        load()
        loadUserdata()
    }

    private fun onSearch(str: String, value: Boolean) {
        if(str.isNotBlank()) {
            if (str.startsWith("#")) {
                searchAdapter.setQuotes(QuoterAPI.quotes.filter { "#${it.id}" == str })
            } else {
                searchAdapter.setQuotes(QuoterAPI.quotes.filter {
                    if (value) {
                        it.text.toLowerCase().contains(str.toLowerCase()) ||
                                it.adder.toLowerCase().contains(str.toLowerCase()) ||
                                it.author.toLowerCase().contains(str.toLowerCase())
                    } else {
                        it.text.contains(str) ||
                                it.adder.contains(str) ||
                                it.author.contains(str)
                    }
                })
            }
        } else {
            searchAdapter.setQuotes(emptyList())
        }
    }

    private fun loadUserdata() {
        if (QuoterAPI.getAdderName(this).isBlank()) {
            val view = layoutInflater.inflate(R.layout.personal_data_layout, null)
            val dialog = AlertDialog.Builder(this, R.style.AppTheme_Dialog).setView(view).create()
            val adderNameView = view.findViewById<EditText>(R.id.personal_data_adder_name_field)
            val saveButton = view.findViewById<Button>(R.id.personal_data_save)
            saveButton.setOnClickListener {
                QuoterAPI.setAdderName(this, adderNameView.text.toString())
                dialog.dismiss()
            }
            dialog.show()
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
            quotesAdapter.notifyDataSetChanged()
        }
    }

    private fun loadRemote() {
        QuoterAPI.getTotal({ count ->
            QuoterAPI.getFromTo(0, count, { quotes ->
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
                    it?.printStackTrace()
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
            runOnUiThread {
                showStatus("Похожу на проблему с сервером, не находите? Вот, почитайте, что он мне сказал: ${e?.localizedMessage}")
                hideLoading()
            }
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
                searchOverlayLayout.visibility = View.VISIBLE
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
                searchOverlayLayout.visibility = View.GONE
                invalidateOptionsMenu()
            }
        }.start()
    }

}
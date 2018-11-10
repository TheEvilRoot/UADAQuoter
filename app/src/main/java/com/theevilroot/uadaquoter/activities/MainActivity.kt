package com.theevilroot.uadaquoter.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.content.PermissionChecker
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.theevilroot.uadaquoter.*
import com.theevilroot.uadaquoter.adapters.QuotesAdapter
import com.theevilroot.uadaquoter.adapters.SearchResultAdapter
import com.theevilroot.uadaquoter.objects.IgnoreCaseButton
import com.theevilroot.uadaquoter.objects.TextWatcherWrapper
import com.theevilroot.uadaquoter.utils.bind
import com.theevilroot.uadaquoter.utils.showAdderNameDialog

class MainActivity : AppCompatActivity() {

    private lateinit var app: App
    private lateinit var imm: InputMethodManager
    private lateinit var quotesAdapter: QuotesAdapter
    private lateinit var searchAdapter: SearchResultAdapter

    private val toolbar by bind<Toolbar>(R.id.toolbar)
    private val quotesView by bind<RecyclerView>(R.id.quotes_view)
    private val loadingProcess by bind<ProgressBar>(R.id.progressBar)
    private val searchStatus by bind<TextView>(R.id.search_status)
    private val searchLayout by bind<ConstraintLayout>(R.id.search_layout)
    private val searchOverlayLayout by bind<ConstraintLayout>(R.id.search_overlay_layout)
    private val searchClose by bind<ImageButton>(R.id.search_close)
    private val searchIgnoreCase by bind<IgnoreCaseButton>(R.id.search_ignorcase)
    private val searchField by bind<EditText>(R.id.search_field)
    private val searchQuotesView by bind<RecyclerView>(R.id.search_list_view)

    private var localLoadingError: Boolean = false
    private var permissionGranted: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        setSupportActionBar(toolbar)
        app = application as App
        imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        quotesAdapter = QuotesAdapter()
        searchAdapter = SearchResultAdapter { quote ->
            val id = QuoterAPI.quotes.indexOfFirst { it.id == quote.id }
            quotesView.scrollToPosition(id)
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
        searchField.addTextChangedListener(TextWatcherWrapper(onChange = { str, _, _, _ -> onSearch(str, searchIgnoreCase.value) }))
        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED ||
                PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED) {
            if(Build.VERSION.SDK_INT >= 23)
                return requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 6741)
        }
        onPermissionGranted()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            6741 -> if (grantResults.all { it == PermissionChecker.PERMISSION_GRANTED }) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }
    }

    private fun onSearch(str: String, value: Boolean) {
        if(str.isNotBlank()) {
            when {
                str.startsWith("#") ->
                    searchAdapter.setQuotes(QuoterAPI.quotes.filter { "#${it.id}" == str })
                str.startsWith("+") -> {
                    val q = str.substring(1).apply { if (!value) toLowerCase()  }
                    searchAdapter.setQuotes(QuoterAPI.quotes.filter {
                        if (!value)
                            it.adder.toLowerCase().contains(q)
                        else it.adder.contains(q)
                    })
                }
                str.startsWith("-") -> {
                    val q = str.substring(1).apply { if (!value) toLowerCase()  }
                    searchAdapter.setQuotes(QuoterAPI.quotes.filter {
                        if (!value)
                            it.author.toLowerCase().contains(q)
                        else it.author.contains(q)
                    })
                }
                else -> searchAdapter.setQuotes(QuoterAPI.quotes.filter {
                    if (!value)
                        it.text.toLowerCase().contains(str.toLowerCase())
                     else it.text.contains(str)
                })
            }
        } else {
            searchAdapter.setQuotes(emptyList())
        }
    }

    private fun loadUserdata() {
        if (QuoterAPI.getAdderName(this).isBlank()) {
            showAdderNameDialog(this, "", { editText, textView, alertDialog ->
                if (editText.text.toString().isBlank()) {
                    textView.text = "Введите что-нибудь, кроме ничего"
                    return@showAdderNameDialog textView.setTextColor(resources.getColor(android.R.color.holo_red_light))
                }
                QuoterAPI.setAdderName(this, editText.text.toString())
                alertDialog.dismiss()
            }, false)
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
        if(!localLoadingError) {
            QuoterAPI.loadCache(filesDir, { quotes ->
                QuoterAPI.quotes.clear()
                QuoterAPI.quotes.addAll(quotes)
                QuoterAPI.quotes.sortBy { it.id }
                runOnUiThread { showList() }
                updateUI()
            }) {
                it?.printStackTrace()
                localLoadingError = true
            }
        } else {
            runOnUiThread {
                showStatus("Похоже на проблему с подключением, не находите? Вот: ${e?.localizedMessage}")
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
            R.id.tb_reload -> if (permissionGranted) {
                load()
            } else {
                if (Build.VERSION.SDK_INT >= 23) {
                    requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 6741)
                    return true
                }
                val result = arrayOf(PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        PermissionChecker.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE))
                if (result.all { it == PermissionChecker.PERMISSION_GRANTED }) {
                   onPermissionGranted()
                } else {
                    onPermissionDenied()
                }
            }
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
                imm.showSoftInput(searchField, InputMethodManager.SHOW_FORCED)
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

    private fun onPermissionGranted() {
        load()
        loadUserdata()
        permissionGranted = true
    }

    private fun onPermissionDenied() {
        hideLoading()
        permissionGranted = false
        showStatus("У приложения нет доступа к хранилищу на устройстве что-бы сохранять кэш и данные пользователя. Нажмите на кнопку 'Обновить' сверху экрана, если это не поможет, то необходимо дать данные права через настройки устройства!")
    }

}
package com.theevilroot.uadaquoter.activities

import android.Manifest
import android.accounts.NetworkErrorException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import cn.nekocode.badge.BadgeDrawable
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jakewharton.rxbinding3.appcompat.navigationClicks
import com.jetradar.permissions.PermissionsDeniedException
import com.theevilroot.uadaquoter.App
import com.theevilroot.uadaquoter.R
import com.theevilroot.uadaquoter.adapters.MessagesAdapter
import com.theevilroot.uadaquoter.adapters.QuotesAdapter
import com.theevilroot.uadaquoter.objects.Message
import com.theevilroot.uadaquoter.objects.MessageAction
import com.theevilroot.uadaquoter.objects.MessageEvent
import com.theevilroot.uadaquoter.objects.Quote
import com.theevilroot.uadaquoter.utils.DialogCanceledException
import com.theevilroot.uadaquoter.utils.bind
import com.theevilroot.uadaquoter.utils.log
import daio.io.dresscode.dressCodeStyleId
import daio.io.dresscode.matchDressCode
import io.reactivex.Completable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.FileNotFoundException

class MainActivity : AppCompatActivity(), Observer<MessageEvent> {
    private lateinit var imm: InputMethodManager

    private lateinit var quotesAdapter: QuotesAdapter
    private lateinit var messagesAdapter: MessagesAdapter
    private val appbar by bind<BottomAppBar>(R.id.app_bar)

    private val quotesView by bind<RecyclerView>(R.id.quotes_view)
    private val appbarButton by bind<FloatingActionButton>(R.id.app_bar_button)
    private val messagesView by bind<RecyclerView>(R.id.messages_view)
    private val loadingView by bind<View>(R.id.loading_view)
    private val permissionsDelegate = App.instance.permissionsActivityDelegate

    private val api = App.instance.api
    private val compositeDisposable = CompositeDisposable()
    private val mIdUserdataNotSpecified = 1

    private var isInitialized = false
    private var isFirstMessage = true

    private val mIdPermissionDenied = 2
    private val mIdNetworkError = 3
    private val mIdFirstRun = 4
    private val mIdQuotesLoaded = 5
    private val mIdServiceUnavailable = 6
    private val mIdCacheError = 7

    override fun onCreate(savedInstanceState: Bundle?) {
        matchDressCode()
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null)
            isInitialized = savedInstanceState.getBoolean("isInitialized")
        setContentView(R.layout.activity_main)
        setSupportActionBar(appbar)
        permissionsDelegate.attach(this)
        imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        setupQuotesView()
        setupMessagesView()
        subscribeMessages()
        log(isInitialized.toString())
        if (isInitialized)  {
            hideLoading()
            log("isInitialized = true. notifying adapters")
            quotesAdapter.notifyDataSetChanged()
            messagesAdapter.notifyDataSetChanged()
        } else {
            log("isInitialized = false. starting loading")
            checkPermissions()
        }
    }

    /** Main logic stuff **/

    private fun checkPermissions() =
            compositeDisposable.add(App.instance.butler
                    .require(true,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        requestUserdata()
                        init()
                    }) {
                        if (it is PermissionsDeniedException) {
                            addPermissionDeniedMessage()
                            init()
                        } else addErrorMessage(it)
                    })

    private fun requestUserdata() {
        if (api.username() == null)
            compositeDisposable.add(api.requestUserdata(this, "")
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe({ api.username(it) }, {
                        if (it !is DialogCanceledException)
                            addErrorMessage(it)
                        addUserdataNotSpecifiedMessage()
                    }))
    }

    private fun init() {
        showLoading()
        compositeDisposable.add(checkConnection()
                .subscribe(this::checkService) {
                    if (it is NetworkErrorException)
                        addNetworkErrorMessage()
                    else addErrorMessage(it)
                    loadFromCache()
                })
    }

    private fun checkService() {
        compositeDisposable.add(api.isServiceAvailable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::loadFromServer) {
                    addServiceUnavailableMessage()
                    loadFromCache()
                    it.printStackTrace()
                })
    }

    private fun loadFromCache() {
        clearQuotesDatabase(true)
        compositeDisposable.add(api.getCache()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    addQuote(it)
                }, {
                    hideLoading()
                    if (it is FileNotFoundException) {
                        addFirstRunMessage()
                    } else addErrorMessage(it)
                }) {
                    addQuotesLoadedMessage()
                    hideLoading()
                    isInitialized = true
                })
    }

    private fun loadFromServer() {
        clearQuotesDatabase(true)
        compositeDisposable.add(api.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    addQuote(it)
                }, {
                    addErrorMessage(it)
                    loadFromCache()
                }) {
                    hideLoading()
                    addQuotesLoadedMessage()
                    isInitialized = true
                    api.setCache(App.instance.quotes)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({
                                log("Cache saved")
                            }, this::addCacheErrorMessage)
                })
    }

    private fun checkConnection() = Completable.create {
        val service = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = service.activeNetworkInfo
        if (activeNetwork.isConnected)
            it.onComplete()
        else it.onError(NetworkErrorException())
    }

    private fun addQuote(quote: Quote) {
        App.instance.quotes.add(quote)
        quotesAdapter.notifyItemInserted(App.instance.quotes.size - 1)
    }

    private fun clearQuotesDatabase(notifyAdapter: Boolean) {
        if (App.instance.quotes.isNotEmpty()) {
            App.instance.quotes.clear()
            if (notifyAdapter)
                quotesAdapter.notifyDataSetChanged()
        }
    }

    /** Messages stuff **/

    private fun subscribeMessages() {
        api.messagesService()
                .subscribe(this)
    }

    private fun addUserdataNotSpecifiedMessage() {
        addMessage("UserdataNotSpecified",
                "UserdataNotSpecifiedMessage",
                android.R.color.holo_green_dark,
                R.drawable.ic_trash_can,
                id = mIdUserdataNotSpecified)
    }

    private fun addErrorMessage(t: Throwable) {
        t.printStackTrace()
        addMessage("Error", t::class.java.simpleName, android.R.color.holo_red_dark, R.drawable.ic_trash_can)
    }

    private fun addPermissionDeniedMessage() {
        addMessage("PermissionDenied",
                "PermissionDeniedMessage",
                android.R.color.holo_red_dark,
                R.drawable.ic_trash_can,
                id = mIdPermissionDenied)
    }

    private fun addNetworkErrorMessage() {
        addMessage("NetworkError",
                "NetworkErrorMessage",
                android.R.color.holo_red_dark,
                R.drawable.ic_trash_can,
                id = mIdNetworkError)
    }

    private fun addFirstRunMessage() {
        addMessage("FirstRun",
                "FirstRunMessage",
                android.R.color.holo_green_dark,
                R.drawable.ic_trash_can,
                id = mIdFirstRun)
    }

    private fun addQuotesLoadedMessage() {
        addMessage("QuotesLoaded",
                App.instance.quotes.count().toString(),
                android.R.color.holo_green_dark,
                R.drawable.ic_trash_can,
                id = mIdQuotesLoaded)
    }

    private fun addServiceUnavailableMessage() {
        addMessage("ServiceUnavailable",
                "ServiceUnavailableMessage",
                android.R.color.holo_red_dark,
                R.drawable.ic_trash_can,
                id = mIdServiceUnavailable)
    }

    private fun addCacheErrorMessage(t: Throwable) {
        addMessage("CacheError",
                "CacheErrorMessage\n${t.localizedMessage}",
                android.R.color.holo_red_dark,
                R.drawable.ic_trash_can,
                id = mIdCacheError)
    }

    private fun addMessage(title: String,
                           message: String,
                           @ColorRes color: Int,
                           @DrawableRes icon: Int,
                           actions: List<MessageAction> = listOf(MessageAction()),
                           id: Int? = null) {
        api.messagesService().onNext(MessageEvent(MessageEvent.EventType.MESSAGE_INSERT, message = Message(title, message, color, icon, actions, id)))
    }

    private fun dismissMessage(msg: Message) {
        api.messagesService().onNext(MessageEvent(MessageEvent.EventType.MESSAGE_DELETE,message = msg))
    }

    private fun dismissMessages(withId: Int) {
        api.messagesService().onNext(MessageEvent(MessageEvent.EventType.MESSAGE_DELETE, messageId = withId))
    }

    /** UI setting up stuff **/

    private fun toggleMessagesViewState() {
        if (messagesView.isShown)
            messagesView.visibility = View.GONE
        else messagesView.visibility = View.VISIBLE
    }

    private fun setupMessagesView() {
        messagesAdapter = MessagesAdapter()
        messagesView.layoutManager = GridLayoutManager(this, 1)
        messagesView.adapter = messagesAdapter
        ItemTouchHelper(object: ItemTouchHelper.Callback() {
            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int =
                    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean =
                    true

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val message = App.instance.messages.getOrNull(viewHolder.adapterPosition)
                if (message != null)
                    dismissMessage(message)
            }
        }).attachToRecyclerView(messagesView)
        appbar.setNavigationOnClickListener {
            log("OnNavigationClick")
            toggleMessagesViewState()
        }
    }


    private fun setupQuotesView() {
        quotesAdapter = QuotesAdapter()
        quotesView.layoutManager = GridLayoutManager(this, 1)
        quotesView.adapter = quotesAdapter
    }

    private fun showLoading() {
        loadingView.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        loadingView.visibility = View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.tb_reload -> checkPermissions()
            R.id.tb_add -> {
                startActivity(Intent(this, NewQuoteActivity::class.java))
            }
            R.id.tb_test_light -> {
                dressCodeStyleId = R.style.AppTheme_UADAFLight
                matchDressCode()
            }
            R.id.tb_test_dark -> {
                dressCodeStyleId = R.style.AppTheme_UADAFDark
                matchDressCode()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateMessagesBadge(count: Int?) {
        if (count == null) {
            appbar.navigationIcon = null
        } else {
            appbar.navigationIcon = BadgeDrawable.Builder()
                    .type(BadgeDrawable.TYPE_NUMBER)
                    .badgeColor(resources.getColor(android.R.color.holo_red_dark))
                    .number(count)
                    .build()
        }
    }

    /** System stuff **/

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionsDelegate.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        if (!compositeDisposable.isDisposed)
            compositeDisposable.dispose()
        permissionsDelegate.detach()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar_main, menu)
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("isInitialized", isInitialized)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        isInitialized = savedInstanceState.getBoolean("isInitialized")
    }

    override fun onComplete() {

    }

    override fun onSubscribe(d: Disposable) {
        compositeDisposable.add(d)
        updateMessagesBadge(messagesAdapter.messagesCount())
    }

    override fun onNext(event: MessageEvent) {
        when (event.eventType) {
            MessageEvent.EventType.MESSAGE_INSERT -> {
                if (event.message == null)
                    return log("Attempt to add null message")

                App.instance.messages.add(event.message)
                messagesAdapter.notifyItemInserted(App.instance.messages.count() - 1)
                updateMessagesBadge(messagesAdapter.messagesCount())
                if (isFirstMessage) {
                    if (!messagesView.isShown)
                        toggleMessagesViewState()
                    isFirstMessage = false
                }
            }
            MessageEvent.EventType.MESSAGE_UPDATE -> {
                val position = messagesAdapter.indexBy(event.message ?: event.messageId ?: return log("Message update event have no message or messageId"))
                        ?: return log("Attempt to update missing message")

                if (event.newMessage == null)
                    return log("Unable to update message on position $position because new message is null")

                App.instance.messages[position] = event.newMessage
                messagesAdapter.notifyItemChanged(position)
            }
            MessageEvent.EventType.MESSAGE_DELETE -> {
                val position = messagesAdapter.indexBy(event.message ?: event.messageId ?: return log("Message delete event have no message or messageId"))
                        ?: return log("Attempt to delete missing message")

                App.instance.messages.removeAt(position)
                messagesAdapter.notifyItemRemoved(position)

                val newCount = messagesAdapter.messagesCount()
                updateMessagesBadge(
                        if (newCount == 0)
                            null
                        else newCount
                )
            }
        }
    }

    override fun onError(e: Throwable) {
        log("Message error: ")
        e.printStackTrace()
    }

}

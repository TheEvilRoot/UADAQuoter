package com.theevilroot.uadaquoter

import android.util.Log
import com.theevilroot.uadaquoter.objects.Quote
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

object RxQuoterApi {

    private const val backendUrl = "http://127.0.0.1:6741/api/quote/"
    private const val defaultHeader = "Connection: close"

    private val retrofit = Retrofit.Builder()
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
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

    class QuoterServiceApi(val mocked: Boolean = false) {

        fun getPos(pos: Int): Single<Quote> =
                quoter(mocked).pos(pos)

        fun getRange(from: Int, to: Int): Observable<Quote> =
                quoter(mocked).range(from, to)
                        .map(List<Quote>::toTypedArray)
                        .flatMapObservable { Observable.fromArray(*it) }

        fun getRandom(count: Int = 1): Observable<Quote> =
                quoter(mocked).random(count)
                        .map(List<Quote>::toTypedArray)
                        .flatMapObservable { Observable.fromArray(*it) }

        fun getAll(): Observable<Quote> =
                quoter(mocked).all()
                        .map(List<Quote>::toTypedArray)
                        .flatMapObservable { Observable.fromArray(*it) }

        fun getTotal(): Single<Int> =
                quoter(mocked).total()

        fun add(key: String, adder: String, author: String, quote: String): Completable =
                quoter(mocked).add(key, adder, author, quote)

        fun edit(key: String, id: Int, editedBy: String, newText: String): Completable =
                quoter(mocked).edit(key, id, editedBy, newText)

    }

    private interface QuoterService {

        @GET("pos/{pos}")
        @Headers(defaultHeader)
        fun pos(@Path("pos") pos: Int): Single<Quote>

        @GET("range/{from}/{to}")
        @Headers(defaultHeader)
        fun range(
                @Path("from") from: Int,
                @Path("to") to: Int): Single<List<Quote>>

        @Headers(defaultHeader)
        @GET("random/{count}")
        fun random(@Path("count") count: Int = 1): Single<List<Quote>>

        @Headers(defaultHeader)
        @GET("all")
        fun all(): Single<List<Quote>>

        @Headers(defaultHeader)
        @GET("total")
        fun total(): Single<Int>

        @Headers(defaultHeader)
        @FormUrlEncoded
        @POST("add")
        fun add(
                @Field("key") key: String,
                @Field("adder") adder: String,
                @Field("author") author: String,
                @Field("quote") quote: String
        ): Completable

        @Headers(defaultHeader)
        @FormUrlEncoded
        @POST("edit")
        fun edit(
                @Field("key") key: String,
                @Field("id") id: Int,
                @Field("edited_by") editedBy: String,
                @Field("new_text") newText: String
        ): Completable

    }

}
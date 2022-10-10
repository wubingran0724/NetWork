package com.example.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 *Create by hey on 2022/10/9
 *$ 协程网络请求库
 */

object NetWork {
    private var retrofit: Retrofit? = null

    fun initRetrofit(
        context: Context,
        debug: Boolean,
        baseUrl: String,
        connectTimeout_secs: Long,
        readTimeout_secs: Long,
        writeTimeout_secs: Long,
    ) {
        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(
                initOkhttpClient(
                    context,
                    debug,
                    connectTimeout_secs,
                    readTimeout_secs,
                    writeTimeout_secs
                )
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * 初始OkhttpClient
     *
     * @param context
     * @param debug
     * @param connectTimeout_secs
     * @param readTimeout_secs
     * @param writeTimeout_secs
     * @return
     */
    private fun initOkhttpClient(
        context: Context,
        debug: Boolean,
        connectTimeout_secs: Long,
        readTimeout_secs: Long,
        writeTimeout_secs: Long
    ): OkHttpClient {
        val builder =
            OkHttpClient.Builder().connectTimeout(connectTimeout_secs, TimeUnit.SECONDS)
                .readTimeout(readTimeout_secs, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout_secs, TimeUnit.SECONDS)
                .followRedirects(true)
        if (debug) {
            builder.addInterceptor(initLogInterceptor())
        }
        return builder.build()
    }

    /**
     *
     *
     * @return 日志拦截器
     */
    private fun initLogInterceptor(): HttpLoggingInterceptor {
        val interceptor =
            HttpLoggingInterceptor { message -> Log.w("Retrofit", message) }
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        return interceptor
    }

    /*
     * 具体服务实例化
     */
    fun <T> getService(service: Class<T>): T {
        return retrofit!!.create(service)
    }
}

suspend inline fun <T> apiCall(crossinline call: suspend CoroutineScope.() -> Response<T>): Response<T> {
    return withContext(Dispatchers.IO) {
        val res: Response<T>
        try {
            res = call()
        } catch (e: Throwable) {
            // 请求出错，将状态码封装为Response
            return@withContext ApiException.toResponse<T>()
        }
        if (res.code == ApiException.SESSION_INVALID_STATUS) {
            //登陆信息失效
            Log.e("Retrofit", "SESSION_INVALID_STATUS")
            //取消协程
            cancel()
        }
        return@withContext res
    }
}


// 网络、数据解析错误处理
class ApiException(
    override val cause: Throwable? = null
) : RuntimeException(cause) {
    companion object {
        //网络连接失败
        private const val NETWORK_BROKEN_STATUS = 1000

        //登陆信息失效
        const val SESSION_INVALID_STATUS = 401

        fun <T> toResponse(): Response<T> {
            return Response(NETWORK_BROKEN_STATUS)
        }
    }
}

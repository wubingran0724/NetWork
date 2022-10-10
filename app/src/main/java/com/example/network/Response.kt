package com.example.network

/**
 *Create by hey on 2022/10/9
 *$ 网络请求回调
 */
data class Response<T>(
    val code: Int,
    val errorMsg: String? = null,
    val data: T? = null
)
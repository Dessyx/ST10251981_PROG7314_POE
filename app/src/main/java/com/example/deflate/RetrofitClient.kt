package com.example.deflate

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {  // Code in this class was guided by AI (ChatGPT, 2025)
    private const val BASE_URL = "https://favqs.com/api/"

    val instance: FavQsApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(FavQsApi::class.java)
    }
}

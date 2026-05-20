package com.example.data.api

import com.example.data.model.MaccmsResponse
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.http.GET
import retrofit2.http.QueryMap
import retrofit2.http.Url

interface MaccmsService {
    @GET
    suspend fun getVodData(
        @Url url: String,
        @QueryMap options: Map<String, String>
    ): MaccmsResponse

    companion object {
        private val moshi = Moshi.Builder()
            .add(Int::class.java, object : com.squareup.moshi.JsonAdapter<Int>() {
                override fun fromJson(reader: com.squareup.moshi.JsonReader): Int? {
                    return when (reader.peek()) {
                        com.squareup.moshi.JsonReader.Token.NUMBER -> reader.nextInt()
                        com.squareup.moshi.JsonReader.Token.STRING -> {
                            val str = reader.nextString()
                            str.toIntOrNull() ?: str.toDoubleOrNull()?.toInt()
                        }
                        com.squareup.moshi.JsonReader.Token.NULL -> {
                            reader.nextNull<Any>()
                            null
                        }
                        else -> {
                            reader.skipValue()
                            null
                        }
                    }
                }
                override fun toJson(writer: com.squareup.moshi.JsonWriter, value: Int?) {
                    writer.value(value)
                }
            }.nullSafe())
            .addLast(KotlinJsonAdapterFactory())
            .build()

        fun create(): MaccmsService {
            return Retrofit.Builder()
                // Placeholder base URL; @Url parameter overrides it completely.
                .baseUrl("https://placeholder.api/")
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(MaccmsService::class.java)
        }
    }
}

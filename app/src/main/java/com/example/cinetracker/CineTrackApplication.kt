package com.example.cinetracker

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.example.cinetracker.data.remote.NetworkModule
import com.example.cinetracker.di.ServiceLocator

/**
 * Application subclass that owns the process-wide [ServiceLocator] and the Coil
 * [SingletonImageLoader]. Sharing OkHttp between Retrofit and Coil keeps a single
 * connection pool and dispatcher across the app.
 */
class CineTrackApplication : Application(), SingletonImageLoader.Factory {

    val serviceLocator: ServiceLocator by lazy { ServiceLocator(applicationContext) }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { NetworkModule.okHttpClient }))
            }
            .build()
}

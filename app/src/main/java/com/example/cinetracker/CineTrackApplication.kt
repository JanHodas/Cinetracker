package com.example.cinetracker

import android.app.Application
import com.example.cinetracker.di.ServiceLocator

/**
 * Application subclass that owns the process-wide [ServiceLocator]. ViewModel factories
 * resolve dependencies through `(context.applicationContext as CineTrackApplication).serviceLocator`.
 */
class CineTrackApplication : Application() {
    val serviceLocator: ServiceLocator by lazy { ServiceLocator(applicationContext) }
}

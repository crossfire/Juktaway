package net.slashOmega.juktaway

import android.content.Context
import android.graphics.Typeface
import android.os.StrictMode
import android.support.multidex.MultiDex
import android.support.multidex.MultiDexApplication
import net.slashOmega.juktaway.model.UserIconManager
import net.slashOmega.juktaway.settings.BasicSettings

class JuktawayApplication: MultiDexApplication() {
    companion object {
        lateinit var app: JuktawayApplication
        //TODO private set
        lateinit var font: Typeface
        //TODO private set
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        app = this
        if (!BuildConfig.DEBUG)
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())


        // load setting files
        BasicSettings.init()
        UserIconManager.warmUpUserIconMap()


        font = Typeface.createFromAsset(assets, "fontello.ttf")

        if (BuildConfig.DEBUG)
            Thread.setDefaultUncaughtExceptionHandler(MyUncaughtExceptionHandler(app))
    }
}
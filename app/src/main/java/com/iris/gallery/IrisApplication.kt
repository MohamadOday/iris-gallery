package com.iris.gallery

import android.app.Application
import android.os.Build
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.decode.StaticImageDecoder
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import com.iris.gallery.decoder.AvifCoilDecoder

class IrisApplication : Application(), SingletonImageLoader.Factory {
    companion object {
        lateinit var instance: IrisApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(AvifCoilDecoder.Factory())
                add(coil3.svg.SvgDecoder.Factory())
                if (Build.VERSION.SDK_INT >= 28) {
                    add(AnimatedImageDecoder.Factory())
                    add(StaticImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
}

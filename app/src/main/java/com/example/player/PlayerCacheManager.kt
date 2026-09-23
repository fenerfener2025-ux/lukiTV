package com.example.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

/**
 * Singleton cache and pre-buffering manager for PlayerEngine.
 * Provides:
 * 1. Persistent LRU disk cache for HLS manifests, video segments and keys (up to 100MB).
 * 2. Pre-buffering mechanism that pre-fetches initial stream segments before or during channel switches
 *    to minimize freezes, eliminate black screen flickers, and achieve sub-second zapping.
 */
object PlayerCacheManager {
    private const val TAG = "PlayerCacheManager"
    private const val CACHE_SIZE = 100 * 1024 * 1024L // 100 MB LRU disk cache
    private const val PREFETCH_BUFFER_BYTES = 256 * 1024L // 256 KB initial segment buffer

    @Volatile
    private var simpleCache: SimpleCache? = null
    private var databaseProvider: StandaloneDatabaseProvider? = null

    private val prefetchScope = CoroutineScope(Dispatchers.IO)
    private var activePrefetchJob: Job? = null

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.cacheDir, "iptv_media_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val evictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE)
            val dbProvider = StandaloneDatabaseProvider(context.applicationContext)
            databaseProvider = dbProvider
            simpleCache = SimpleCache(cacheDir, evictor, dbProvider)
            Log.d(TAG, "Initialized IPTV media cache at: ${cacheDir.absolutePath}")
        }
        return simpleCache!!
    }

    /**
     * Builds a caching DataSource.Factory that intercepts and caches video segments.
     */
    fun createCacheDataSourceFactory(
        context: Context,
        upstreamFactory: DefaultHttpDataSource.Factory
    ): DataSource.Factory {
        val cache = getCache(context)
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /**
     * Pre-buffers the initial chunks of a target stream URL into the cache asynchronously.
     * When the user switches to this channel, the connection handshake, manifest, and first
     * audio/video segments are already cached, leading to instant playback without stutter.
     */
    fun prebufferStream(context: Context, url: String, onBufferReady: (() -> Unit)? = null) {
        if (url.isBlank()) return

        activePrefetchJob?.cancel()
        activePrefetchJob = prefetchScope.launch {
            try {
                Log.d(TAG, "Pre-buffering stream head: $url")
                val cache = getCache(context)
                val upstreamFactory = DefaultHttpDataSource.Factory()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(4000)
                    .setReadTimeoutMs(4000)

                val cacheDataSource = CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(upstreamFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                    .createDataSource()

                val uri = Uri.parse(url)
                val dataSpec = DataSpec.Builder()
                    .setUri(uri)
                    .setPosition(0)
                    .setLength(PREFETCH_BUFFER_BYTES)
                    .build()

                try {
                    cacheDataSource.open(dataSpec)
                    val buffer = ByteArray(16 * 1024)
                    var totalRead = 0L
                    while (totalRead < PREFETCH_BUFFER_BYTES) {
                        val read = cacheDataSource.read(buffer, 0, buffer.size)
                        if (read <= 0) break
                        totalRead += read
                    }
                    Log.d(TAG, "Pre-buffered $totalRead bytes for: $url")
                } finally {
                    try {
                        cacheDataSource.close()
                    } catch (_: Throwable) {}
                }
                onBufferReady?.invoke()
            } catch (e: Exception) {
                // Pre-buffering is a best-effort performance enhancement; non-fatal if it fails
                Log.w(TAG, "Pre-buffer non-fatal notice for $url: ${e.message}")
            }
        }
    }

    /**
     * Release pre-buffering resources when app or player is destroyed
     */
    fun cancelPrefetch() {
        activePrefetchJob?.cancel()
        activePrefetchJob = null
    }

    @Synchronized
    fun releaseCache() {
        cancelPrefetch()
        try {
            simpleCache?.release()
            simpleCache = null
            databaseProvider = null
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing cache: ${e.message}")
        }
    }

    @Synchronized
    fun clearCache(context: Context) {
        cancelPrefetch()
        try {
            simpleCache?.keys?.forEach { key ->
                try {
                    simpleCache?.removeResource(key)
                } catch (_: Exception) {}
            }
            releaseCache()
            val cacheDir = File(context.cacheDir, "iptv_media_cache")
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
            Log.d(TAG, "Cleared media cache directory.")
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing cache: ${e.message}")
        }
    }
}

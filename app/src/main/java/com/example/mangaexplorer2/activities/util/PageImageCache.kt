package com.example.mangaexplorer2.activities.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Disk cache that holds only the images the page reader downloads.
 *
 * Glide keeps one shared disk cache for the whole app. That cache can just be emptied as a whole
 * with `Glide.clearDiskCache()`, single entries can not be removed and a second Glide instance
 * with its own cache directory is not reachable from application code. Since the reader produces
 * much bigger images than the rest of the app and has to get rid of them quickly, the page images
 * are kept in their own directory here, where size limit, eviction and cleanup are under our control.
 *
 * Every method is safe to call from a background thread, only [get] does blocking work.
 */
class PageImageCache private constructor(
    private val cacheDir: File,
    private val maxCacheBytes: Long
) {
    private val downloadLocks = ConcurrentHashMap<String, Any>()
    private val trimLock = Any()

    /**
     * Returns a file containing the image [url] points to, downloading it if it is not cached yet.
     *
     * @return the local copy of the image or null when it could not be downloaded
     */
    fun get(url: String, headers: Map<String, String> = emptyMap()): File? {
        val key = cacheKey(url)
        val target = File(cacheDir, key)
        synchronized(downloadLocks.computeIfAbsent(key) { Any() }) {
            if (target.isFile && target.length() > 0) {
                target.setLastModified(System.currentTimeMillis()) //mark it as the most recently used entry
                return target
            }
            return try {
                download(url, headers, target)
            } catch (e: Exception) {
                Log.e(TAG, "Could not download the page image '$url'", e)
                null
            } finally {
                trim()
            }
        }
    }

    /**
     * Drops the cached image of [url]. Called when a page buffer is thrown away, so the images of a
     * chapter do not pile up on the disk.
     */
    fun remove(url: String) {
        val key = cacheKey(url)
        synchronized(downloadLocks.computeIfAbsent(key) { Any() }) {
            File(cacheDir, key).delete()
            deleteTemporaryFiles(key)
        }
    }

    /** Deletes every image this cache created. Glide's cache is never touched. */
    fun clear() {
        val entries = cacheDir.listFiles() ?: return
        for (entry in entries) {
            entry.delete()
        }
        Log.d(TAG, "Cleared the page image cache")
    }

    /** Deletes the least recently used images until the cache fits into [maxCacheBytes] again. */
    fun trim() {
        synchronized(trimLock) {
            val entries = cacheDir.listFiles { file -> !file.name.endsWith(TMP_SUFFIX) } ?: return
            var usedBytes = entries.sumOf { it.length() }
            if (usedBytes <= maxCacheBytes) {
                return
            }
            for (entry in entries.sortedBy { it.lastModified() }) {
                val entrySize = entry.length()
                if (entry.delete()) {
                    usedBytes -= entrySize
                }
                if (usedBytes <= maxCacheBytes) {
                    return
                }
            }
        }
    }

    private fun download(url: String, headers: Map<String, String>, target: File): File? {
        if (!cacheDir.isDirectory && !cacheDir.mkdirs()) {
            Log.e(TAG, "Could not create the cache directory '${cacheDir.path}'")
            return null
        }

        val temporaryFile = File(cacheDir, "${target.name}.${Thread.currentThread().id}$TMP_SUFFIX")
        try {
            val connection = openConnection(url, headers) ?: return null
            try {
                val declaredSize = connection.contentLengthLong
                if (declaredSize > maxCacheBytes) {
                    Log.e(TAG, "The page image '$url' is bigger than the whole cache")
                    return null
                }
                connection.inputStream.use { input ->
                    temporaryFile.outputStream().use { output -> input.copyTo(output) }
                }
            } finally {
                connection.disconnect()
            }

            if (temporaryFile.length() == 0L) {
                Log.e(TAG, "The page image '$url' is empty")
                return null
            }
            if (!temporaryFile.renameTo(target)) {
                Log.e(TAG, "Could not move the downloaded image to '${target.path}'")
                return null
            }
            target.setLastModified(System.currentTimeMillis())
            return target
        } catch (e: IOException) {
            Log.e(TAG, "Could not download the page image '$url'", e)
            return null
        } finally {
            temporaryFile.delete()
        }
    }

    /** Follows redirects by hand, because HttpURLConnection does not follow redirects between http and https. */
    private fun openConnection(url: String, headers: Map<String, String>): HttpURLConnection? {
        var currentUrl = URL(url)
        var redirectCount = 0
        while (true) {
            val connection = currentUrl.openConnection() as? HttpURLConnection ?: return null
            connection.connectTimeout = TIMEOUT
            connection.readTimeout = TIMEOUT
            connection.instanceFollowRedirects = false
            for ((name, value) in headers) {
                connection.setRequestProperty(name, value)
            }
            try {
                connection.connect()
            } catch (e: IOException) {
                connection.disconnect()
                throw e
            }

            val status = connection.responseCode
            if (status in 300..399) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                if (location == null || redirectCount >= MAX_REDIRECTS) {
                    Log.e(TAG, "The page image '$url' redirects too often or nowhere")
                    return null
                }
                redirectCount++
                currentUrl = URL(currentUrl, location)
                continue
            }
            if (status !in 200..299) {
                connection.disconnect()
                Log.e(TAG, "The page image '$url' responded with the status $status")
                return null
            }
            return connection
        }
    }

    private fun deleteTemporaryFiles(key: String) {
        val entries = cacheDir.listFiles { file -> file.name.startsWith(key) && file.name.endsWith(TMP_SUFFIX) }
        entries?.forEach { it.delete() }
    }

    private fun cacheKey(url: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val TAG = "PageImageCache"
        private const val CACHE_FOLDER = "page_images"
        private const val TMP_SUFFIX = ".tmp"
        private const val TIMEOUT = 20 * 1000
        private const val MAX_REDIRECTS = 5
        private const val MAX_CACHE_BYTES = 40L * 1024 * 1024

        @Volatile
        private var instance: PageImageCache? = null

        fun get(context: Context): PageImageCache {
            return instance ?: synchronized(this) {
                instance ?: PageImageCache(
                    File(context.applicationContext.cacheDir, CACHE_FOLDER),
                    MAX_CACHE_BYTES
                ).also { instance = it }
            }
        }
    }
}

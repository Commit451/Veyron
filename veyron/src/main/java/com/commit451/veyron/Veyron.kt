@file:Suppress("unused")

package com.commit451.veyron

import android.util.Log
import com.commit451.okyo.Okyo
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.squareup.moshi.Moshi
import io.reactivex.Completable
import io.reactivex.Single
import java.util.*
import io.reactivex.schedulers.Schedulers
import io.reactivex.Flowable

/**
 * Save and fetch files in JSON or raw format from Google Drive in a REST-like way.
 * Derived partially from [this sample](https://github.com/gsuitedevs/android-samples/blob/master/drive/deprecation/app/src/main/java/com/google/android/gms/drive/sample/driveapimigration/DriveServiceHelper.java)
 */
@Suppress("MemberVisibilityCanBePrivate")
class Veyron private constructor(builder: Builder) {

    companion object {
        private const val SPACE_APP_DATA = "appDataFolder"
        private const val PAGE_SIZE = 1000
    }

    private var moshi: Moshi = builder.moshi ?: Moshi.Builder().build()
    private val verbose = builder.verbose
    private val drive: Drive = builder.drive
    // We just support one space for now
    private val space = SPACE_APP_DATA
    private val fields = "id,name,modifiedTime,size,mimeType"
    private val filesFields = "nextPageToken, files($fields)"
    private val foldersCache = if (builder.cacheFolders) FoldersCache() else NoOpFoldersCache()
    private val lock = Any()

    /**
     * Get the file at the path. Creates intermediate folders and the actual file if they do not exist
     */
    fun file(path: String): Single<File> {
        return file(path, true)
                .map {
                    //Never null, there will always be a result
                    it.result!!
                }
    }

    /**
     * Search for files at the given path. Creates intermediate folders if they do not exist.
     * Example: A path of "dogs/favorites" with a query of "name = 'spike'" will query the dogs/favorites
     * folder for all files with a title equal to "spike".
     * See [Search for files](https://developers.google.com/drive/api/v3/search-parameters) for
     * documentation on how to create the query.
     */
    fun search(path: String, query: String): Single<List<File>> {
        return Single.defer {
            synchronized(lock) {
                log { "Searching with query $query" }
                val folderResult = file(path, false)
                    .blockingGet()
                // If folder doesn't exist, return early.
                val folder = folderResult.result ?: return@defer Single.just(listOf<File>())
                var finalQuery = "'${folder.id}' in parents"
                finalQuery += if (query.isBlank()) "" else " and $query"
                //empty first, since that will not break the loop
                var nextPageToken: String? = ""
                val results = mutableListOf<File>()
                while (nextPageToken != null) {
                    val result = drive.files()
                        .list()
                        .apply {
                            spaces = space
                            q = finalQuery
                            fields = filesFields
                            pageSize = PAGE_SIZE
                            if (!nextPageToken.isNullOrEmpty()) {
                                pageToken = nextPageToken
                            }
                        }
                        .execute()
                    log { "Adding ${result.files.size} files" }
                    results.addAll(result.files)
                    nextPageToken = result.nextPageToken
                    if (nextPageToken != null) {
                        log { "Loading next page with token $nextPageToken" }
                    }
                }
                return@defer Single.just(results)
            }
        }
    }

    /**
     * List all files at the given path. Creates intermediate folders and the actual file if they do not exist. Example:
     * dogs/favorites with a query of "spike" will query that result.
     */
    fun files(path: String): Single<List<File>> {
        return Single.defer {
            search(path, "")
        }
    }

    /**
     * Get the document at a given path
     */
    fun <T> document(path: String, type: Class<T>): Single<VeyronResult<T>> {
        return file(path, false)
                .flatMap {
                    if (it.result != null) {
                        log { "Attempting to turn ${it.result.identify()} into a document" }
                        val document = fileToDocument(it.result, type)
                        Single.just(VeyronResult(document))
                    } else {
                        Single.just(VeyronResult.EMPTY)
                    }
                }
    }

    /**
     * Get all files and convert them to documents at a given path (with optional query)
     */
    fun <T> documents(path: String, type: Class<T>, query: String = ""): Single<List<T>> {
        return search(path, query)
                .map {
                    it.map { file -> fileToDocument(file, type)!! }
                }
    }

    /**
     * Gets the file's media as a string at a given path
     */
    fun string(path: String): Single<VeyronResult<String>> {
        return file(path, false)
                .flatMap {
                    if (it.result == null) {
                        return@flatMap Single.just(VeyronResult.EMPTY)
                    } else {
                        drive.files().get(it.result.id)
                                .asInputStream()
                                .map { inputStream ->
                                    VeyronResult(Okyo.readInputStreamAsString(inputStream))
                                }
                    }
                }
    }

    /**
     * Save the data within the [SaveRequest] at the given path.
     * Note that file names are considered unique and any file with the same title will be overwritten.
     */
    fun save(path: String, request: SaveRequest): Completable {
        return Completable.defer {
            when (request) {
                is SaveRequest.ByteArrayContent -> save(path, request.title, request.content)
                is SaveRequest.String -> save(path, request.title, request.content)
                is SaveRequest.Document<*> -> save(path, request)
            }
        }
    }

    /**
     * Save all of the files concurrently. Please be aware of rate limits and adjust [maxConcurrency] accordingly
     */
    fun save(path: String, requests: List<SaveRequest>, maxConcurrency: Int = 4): Completable {
        // https://stackoverflow.com/a/48965035
        return Completable.defer {
            Flowable.range(0, requests.size)
                    .concatMapEager<Any>({ index ->
                        save(path, requests[index])
                                .subscribeOn(Schedulers.io())
                                .toFlowable()
                    }, maxConcurrency, 1)
                    .toList()
                    .flatMapCompletable { Completable.complete() }
        }
    }

    /**
     * Deletes the file or folder at the given path.
     */
    fun delete(path: String): Completable {
        return Completable.defer {
            val fileId = file(path)
                    .blockingGet()
                    .id
            drive.files().delete(fileId)
                    .execute()
            foldersCache.remove(path)
            Completable.complete()
        }
    }

    /**
     * Typically you won't need to worry about this, but if you have an application where the user
     * can change, you will want to clear the folder cache when switching users.
     */
    fun clearCache() {
        foldersCache.clear()
    }

    private fun file(path: String, alwaysCreate: Boolean): Single<VeyronResult<File>> {
        return Single.defer {
            synchronized(lock) {
                val segments = segments(path)
                val startFolder = startFolder()
                var runnerFolder: File = startFolder

                log { "Attempting to access file at $path" }
                segments.forEachIndexed { index, path ->
                    val cached = foldersCache.get(path)
                    if (cached != null) {
                        log { "Cache hit on $path" }
                        runnerFolder = cached
                        return@forEachIndexed
                    }
                    val result = drive.files()
                        .list()
                        .setSpaces(space)
                        .setQ("'${runnerFolder.id}' in parents and name = '$path'")
                        .setFields(filesFields)
                        .setPageSize(1)
                        .execute()
                    if (result != null && result.files.count() > 0) {
                        log { "File at path $path found" }
                        runnerFolder = result.files.first()
                    } else {
                        val file = if (index == segments.lastIndex) {
                            if (alwaysCreate) {
                                log { "File at path $path not found, creating file" }
                                fileMetadata(path, runnerFolder.id)
                            } else {
                                log { "File at path $path not found, returning empty" }
                                return@defer Single.just(VeyronResult.EMPTY)
                            }
                        } else {
                            log { "File at path $path not found, creating folder" }
                            folderMetadata(path, runnerFolder.id)
                        }
                        runnerFolder = drive.files().create(file)
                            .execute()
                    }
                    if (runnerFolder.isFolder()) {
                        log { "Caching folder at path $path" }
                        foldersCache.put(path, runnerFolder)
                    }
                }
                log { "Returning result for file ${runnerFolder.identify()} at $path" }
                return@defer Single.just(VeyronResult(runnerFolder))
            }
        }
    }

    private fun <T> save(path: String, request: SaveRequest.Document<T>): Completable {
        return Completable.defer {
            val json = moshi.adapter<T>(request.type)
                    .toJson(request.item)
            save(path, request.title, json)
        }
    }

    private fun save(path: String, title: String, content: String): Completable {
        return Completable.defer {
            val contentStream = ByteArrayContent.fromString(MIME_TYPE_JSON, content)
            save(path, title, contentStream)
        }
    }

    private fun save(path: String, title: String, content: ByteArrayContent): Completable {
        return Completable.defer {
            val savePath = "$path/$title"
            log { "Saving to $savePath" }
            val file = file(savePath)
                    .blockingGet()

            val savedFile = drive.files().update(file.id, null, content)
                    .execute()

            log { "File $savePath saved to file ${savedFile.identify()}" }
            Completable.complete()
        }
    }

    private fun startFolder(): File {
        val path = SPACE_APP_DATA
        return foldersCache.get(path) ?: drive.files().get(path)
                .toSingle()
                .doOnSuccess { foldersCache.put(path, it) }
                .blockingGet()
    }

    private fun folderMetadata(folderName: String, parentId: String): File {
        return File()
                .setParents(Collections.singletonList(parentId))
                .setMimeType(MIME_TYPE_FOLDER)
                .setName(folderName)
    }

    private fun fileMetadata(folderName: String, parentId: String): File {
        return File()
                .setParents(Collections.singletonList(parentId))
                .setMimeType(MIME_TYPE_JSON)
                .setName(folderName)
    }

    private fun <T> fileToDocument(file: File?, type: Class<T>): T? {
        if (file == null) {
            log { "File is null, so no document" }
            return null
        }
        return try {
            val getRequest = drive.files()
                    .get(file.id)
            val inputStream = getRequest.executeMediaAsInputStream()
            val content = Okyo.readInputStreamAsString(inputStream)
            if (content.isEmpty()) {
                return null
            }
            val adapter = moshi.adapter<T>(type)
            adapter.fromJson(content)!!
        } catch (exception: Exception) {
            // If the file is found to be not downloadable, that means it was just created and has no content
            if (exception is GoogleJsonResponseException && exception.details.errors.any { it.reason == "fileNotDownloadable" }) {
                null
            } else {
                throw exception
            }
        }
    }

    private fun segments(path: String): List<String> {
        return path.split("/")
    }

    private fun log(messageBlock: () -> String) {
        if (verbose) {
            val message = messageBlock.invoke()
            Log.d("Veyron", message)
        }
    }

    /**
     * Use the builder class to create an instance of [Veyron]
     */
    class Builder(internal val drive: Drive) {

        internal var moshi: Moshi? = null
        internal var verbose = false
        internal var cacheFolders = true

        /**
         * Use a custom Mosh instance to serialize and deserialize. Needed if you are going to save
         * special objects such as Dates
         */
        fun moshi(moshi: Moshi): Builder {
            this.moshi = moshi
            return this
        }

        /**
         * If you would like a bunch of logs
         */
        fun verbose(verbose: Boolean): Builder {
            this.verbose = verbose
            return this
        }

        /**
         * Set if you would like to have folders be cached. Enabled by default, which may lead
         * to unusual behavior if it is common for the user to modify/delete files outside of
         * your current app.
         */
        fun cacheFolders(enabled: Boolean): Builder {
            this.cacheFolders = enabled
            return this
        }

        /**
         * Build the instance
         */
        fun build(): Veyron {
            return Veyron(this)
        }
    }
}

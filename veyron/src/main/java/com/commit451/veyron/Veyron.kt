package com.commit451.veyron

import android.net.Uri
import android.util.Log
import com.commit451.okyo.Okyo
import com.commit451.tisk.toCompletable
import com.commit451.tisk.toSingle
import com.google.android.gms.drive.*
import com.google.android.gms.drive.query.Filters
import com.google.android.gms.drive.query.Query
import com.google.android.gms.drive.query.SearchableField
import com.squareup.moshi.Moshi
import io.reactivex.Completable
import io.reactivex.Single

/**
 * Save and fetch JSON from Google Drive easily
 */
class Veyron private constructor(builder: Builder) {

    companion object {
        /**
         * The root of the user's Google Drive, which they can see
         */
        const val SCHEME_ROOT = "root"

        /**
         * The root of the app folder, which only the current app can see and modify
         */
        const val SCHEME_APP = "app"
    }

    private var driveClient: DriveResourceClient = builder.driveClient
    private var moshi: Moshi = builder.moshi ?: Moshi.Builder().build()
    private val verbose = builder.verbose

    /**
     * Get the folder at the URI. Creates intermediate folders and the actual folder if they do not exist
     */
    fun folder(uri: String): Single<DriveFolder> {
        return Single.defer {
            val url = Uri.parse(uri)
            val segments = segments(uri)
            val startFolder = startFolder(url)
            var runnerFolder: DriveFolder = startFolder
            for (path in segments) {
                val folderQuery = Query.Builder()
                        .addFilter(Filters.eq(SearchableField.MIME_TYPE, DriveFolder.MIME_TYPE))
                        .addFilter(Filters.eq(SearchableField.TITLE, path))
                        .build()
                val buffer = driveClient.queryChildren(runnerFolder, folderQuery)
                        .toSingle()
                        .blockingGet()
                if (buffer != null && buffer.count > 0) {
                    runnerFolder = buffer.get(0).driveId.asDriveFolder()
                } else {
                    val changeSet = folderMetadataChangeSet(path)
                    runnerFolder = driveClient.createFolder(runnerFolder, changeSet)
                            .toSingle()
                            .blockingGet()
                }
                buffer.release()
            }
            Single.just(runnerFolder)
        }
    }

    /**
     * Get the driveId at the given URI.
     */
    fun driveId(uri: String): Single<VeyronResult<DriveId>> {
        log { "Fetching driveId for path $uri" }
        return Single.defer {
            val url = Uri.parse(uri)
            val segments = segments(uri)
            val startFolder = startFolder(url)
            var runnerFolder = startFolder
            var driveId: DriveId? = null
            for (path in segments) {
                val folderQuery = Query.Builder()
                        .addFilter(Filters.eq(SearchableField.TITLE, path))
                        .build()
                val buffer = driveClient.queryChildren(runnerFolder, folderQuery)
                        .toSingle()
                        .blockingGet()
                var found = false
                if (buffer.count > 0) {
                    found = true
                    log { "Found result for path $path" }
                    val thing = buffer.get(0)
                    if (thing.isFolder) {
                        runnerFolder = thing.driveId.asDriveFolder()
                    }
                    driveId = thing.driveId
                } else {
                    log { "Found no result for path $path" }
                }
                buffer.release()
                if (!found) {
                    return@defer Single.just(VeyronResult(null))
                }
            }
            Single.just(VeyronResult(driveId))
        }
    }

    /**
     * Get the [DriveFile] at the given URI
     */
    fun file(uri: String): Single<VeyronResult<DriveFile>> {
        return driveId(uri)
                .flatMap {
                    Single.just(VeyronResult(it.result?.asDriveFile()))
                }
    }

    /**
     * Get the document at a given path
     */
    fun <T> document(uri: String, type: Class<T>): Single<VeyronResult<T>> {
        return file(uri)
                .flatMap {
                    log { "Attempting to turn ${it.result?.driveId} into a document" }
                    val document = fileToDocument(it.result, type)
                    Single.just(VeyronResult(document))
                }
    }

    /**
     * Save the data within the [SaveRequest] at the given uri. Note that a new file will be created
     * base on the [SaveRequest.metadataChangeSet] and the file name should not be included in the uri.
     * Note that file names are considered unique and any file with the same title will be overwritten.
     */
    fun <T> save(uri: String, request: SaveRequest<T>): Completable {
        if (request.metadataChangeSet.title == null) {
            throw IllegalStateException("A title is required for the MetadataChangeSet of this SaveRequest")
        }
        return Completable.defer {
            val folder = folder(uri)
                    .blockingGet()
            //Check if file already exists
            val query = Query.Builder()
                    .addFilter(Filters.eq(SearchableField.TITLE, request.metadataChangeSet.title!!))
                    .build()
            val buffer = driveClient.queryChildren(folder, query)
                    .toSingle()
                    .blockingGet()
            if (buffer.isEmpty()) {
                log { "File did not already exist, creating new file" }
                saveToNewFile(folder, request)
            } else {
                log { "File existed already, overwriting" }
                saveToExistingFile(buffer.get(0).driveId.asDriveFile(), request)
            }
            buffer.release()

            Completable.complete()
        }
    }

    private fun <T> saveToNewFile(folder: DriveFolder, request: SaveRequest<T>) {
        val json = moshi.adapter<T>(request.type)
                .toJson(request.item)
        val driveContents = driveClient.createContents()
                .toSingle()
                .blockingGet()
        Okyo.writeByteArrayToOutputStream(json.toByteArray(), driveContents.outputStream)
        driveClient.createFile(folder, request.metadataChangeSet, driveContents)
    }

    private fun <T> saveToExistingFile(file: DriveFile, request: SaveRequest<T>) {
        val json = moshi.adapter<T>(request.type)
                .toJson(request.item)
        val driveContents = driveClient.openFile(file, DriveFile.MODE_WRITE_ONLY)
                .toSingle()
                .blockingGet()
        Okyo.writeByteArrayToOutputStream(json.toByteArray(), driveContents.outputStream)
        driveClient.commitContents(driveContents, request.metadataChangeSet)
    }

    /**
     * Deletes the file or folder at the given uri. If not found, throws a [DriveIdNotFoundException]
     */
    fun delete(uri: String): Completable {
        return Completable.defer {
            val driveId = driveId(uri)
                    .blockingGet()
            if (driveId.result == null) {
                throw DriveIdNotFoundException()
            }
            driveClient.delete(driveId.result.asDriveResource())
                    .toCompletable()

        }
    }

    private fun startFolder(uri: Uri): DriveFolder {
        return when (uri.scheme) {
            SCHEME_ROOT -> driveClient.rootFolder
                    .toSingle()
                    .blockingGet()
            SCHEME_APP -> driveClient.appFolder
                    .toSingle()
                    .blockingGet()
            else -> throw IllegalArgumentException("The scheme must be one of `root` or `app`")
        }
    }

    private fun folderMetadataChangeSet(folderName: String): MetadataChangeSet {
        return MetadataChangeSet.Builder()
                .setTitle(folderName)
                .setMimeType(DriveFolder.MIME_TYPE)
                .build()
    }

    private fun <T> fileToDocument(file: DriveFile?, type: Class<T>): T? {
        if (file == null) {
            log { "File is null, so no document" }
            return null
        }
        val contents = driveClient.openFile(file, DriveFile.MODE_READ_ONLY)
                .toSingle()
                .blockingGet()
        val inputStream = contents.inputStream
        val content = Okyo.readInputStreamAsString(inputStream)
        val adapter = moshi.adapter<T>(type)
        return adapter.fromJson(content)!!
    }

    private fun segments(uri: String): List<String> {
        return uri.substringAfter("://").split("/")
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
    class Builder(internal val driveClient: DriveResourceClient) {

        internal var moshi: Moshi? = null
        internal var verbose = false

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
         * Build the instance
         */
        fun build(): Veyron {
            return Veyron(this)
        }
    }
}
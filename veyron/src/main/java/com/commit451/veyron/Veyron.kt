package com.commit451.veyron

import android.net.Uri
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
import java.io.FileNotFoundException


/**
 * Save and fetch things from Google Drive easily
 */
class Veyron private constructor(builder: Builder) {

    companion object {
        const val SCHEME_ROOT = "root"
        const val SCHEME_APP = "app"
    }

    private var driveClient: DriveResourceClient = builder.driveClient
    private var moshi: Moshi = builder.moshi ?: Moshi.Builder().build()

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
                    buffer.release()
                } else {
                    val changeSet = folderMetadataChangeSet(path)
                    runnerFolder = driveClient.createFolder(runnerFolder, changeSet)
                            .toSingle()
                            .blockingGet()
                }
            }
            Single.just(runnerFolder)
        }
    }

    /**
     * Get the driveId at the given URI. Throws [FileNotFoundException] if the file does not yet exist
     */
    fun driveId(uri: String): Single<VeyronResult<DriveId>> {
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
                if (buffer.count > 0) {
                    val thing = buffer.get(0)
                    if (thing.isFolder) {
                        runnerFolder = thing.driveId.asDriveFolder()
                    }
                    driveId = thing.driveId
                } else {
                    return@defer Single.just(VeyronResult(driveId))
                }
                buffer.release()
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
                    val document = fileToDocument(it.result, type)
                    Single.just(VeyronResult(document))
                }
    }

    /**
     * Get the collection of docs at the given path
     */
    fun <T> collection(uri: String, type: Class<T>): Single<List<T>> {
        return Single.defer {
            val folder = folder(uri)
                    .blockingGet()
            val buffer = driveClient.listChildren(folder)
                    .toSingle()
                    .blockingGet()
            val collection = mutableListOf<T>()
            buffer.forEach {
                if (!it.isFolder) {
                    val file = it.driveId.asDriveFile()
                    val document = fileToDocument(file, type)
                    if (document != null) {
                        collection.add(document)
                    }
                }
            }
            buffer.release()
            Single.just(collection)
        }
    }

    fun <T> save(uri: String, collection: Collection<SaveRequest<T>>): Completable {
        return Completable.defer {
            val folder = folder(uri)
                    .blockingGet()
            collection.forEach {
                save(folder, it)
            }
            Completable.complete()
        }
    }

    fun <T> save(uri: String, thing: SaveRequest<T>): Completable {
        return Completable.defer {
            val folder = folder(uri)
                    .blockingGet()
            save(folder, thing)
            Completable.complete()
        }
    }

    private fun <T> save(folder: DriveFolder, request: SaveRequest<T>) {
        val json = moshi.adapter<T>(request.type)
                .toJson(request.item)
        val driveContents = driveClient.createContents()
                .toSingle()
                .blockingGet()
        Okyo.writeByteArrayToOutputStream(json.toByteArray(), driveContents.outputStream)
        driveClient.createFile(folder, request.metadataChangeSet, driveContents)
    }

    fun delete(uri: String): Completable {
        return Completable.defer {
            val driveId = driveId(uri)
                    .blockingGet()
            if (driveId.result == null) {
                throw NotFoundException()
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

    /**
     * Use the builder class to create an instance of [Veyron]
     */
    class Builder(internal val driveClient: DriveResourceClient) {

        internal var moshi: Moshi? = null

        /**
         * Use a custom Mosh instance to serialize and deserialize
         */
        fun moshi(moshi: Moshi): Builder {
            this.moshi = moshi
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
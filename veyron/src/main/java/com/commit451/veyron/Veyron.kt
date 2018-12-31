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

/**
 * Save and fetch files in JSON or raw format from Google Drive in a REST-like way.
 * Derived partially from [this sample](https://github.com/gsuitedevs/android-samples/blob/master/drive/deprecation/app/src/main/java/com/google/android/gms/drive/sample/driveapimigration/DriveServiceHelper.java)
 */
class Veyron private constructor(builder: Builder) {

    companion object {
        private const val SPACE_APP_DATA = "appDataFolder"
    }

    private var moshi: Moshi = builder.moshi ?: Moshi.Builder().build()
    private val verbose = builder.verbose
    private val drive: Drive = builder.drive
    // We just support one space for now
    private val space = SPACE_APP_DATA
    private val fields = "id,name,modifiedTime,size,mimeType"
    private val filesFields = "files($fields)"

    /**
     * Get the file at the URI. Creates intermediate folders and the actual file if they do not exist
     */
    fun file(uri: String): Single<File> {
        return Single.defer {
            val segments = segments(uri)
            val startFolder = startFolder()
            var runnerFolder: File = startFolder

            log { "Attempting to access file at $uri" }
            segments.forEachIndexed { index, path ->
                val result = drive.files()
                        .list()
                        .setSpaces(space)
                        .setQ("'${runnerFolder.id}' in parents and name = '$path'")
                        .setFields(filesFields)
                        .setPageSize(1000)
                        .execute()
                if (result != null && result.files.count() > 0) {
                    log { "File at path $path found" }
                    runnerFolder = result.files.first()
                } else {
                    log { "File at path $path not found, creating" }
                    val file = if (index == segments.lastIndex) {
                        fileMetadata(path, runnerFolder.id)
                    } else {
                        folderMetadata(path, runnerFolder.id)
                    }
                    runnerFolder = drive.files().create(file)
                            .execute()
                }
            }
            log { "Returning result for file ${runnerFolder.identify()} at $uri" }
            Single.just(runnerFolder)
        }
    }

    /**
     * Search for files at the given URI. Creates intermediate folders and the actual file if they do not exist. Example:
     * app://dogs with a query of "spike" will query that result.
     * See [Search for files](https://developers.google.com/drive/api/v3/search-parameters) for
     * documentation on how to create the query
     */
    fun search(uri: String, query: String): Single<List<File>> {
        return Single.defer {
            log { "Searching with query $query" }
            val folder = file(uri)
                    .blockingGet()
            val result = drive.files()
                    .list()
                    .setSpaces(space)
                    .setQ("'${folder.id}' in parents and $query")
                    .setFields(filesFields)
                    .setPageSize(1000)
                    .execute()
            Single.just(result.files ?: emptyList())
        }
    }

    /**
     * Get the document at a given path
     */
    fun <T> document(uri: String, type: Class<T>): Single<VeyronResult<T>> {
        return file(uri)
                .flatMap {
                    log { "Attempting to turn ${it.identify()} into a document" }
                    val document = fileToDocument(it, type)
                    Single.just(VeyronResult(document))
                }
    }

    /**
     * Gets the file's media as a string at a given path
     */
    fun string(uri: String): Single<String> {
        return file(uri)
                .flatMap {
                    drive.files().get(it.id)
                            .asInputStream()
                }
                .map {
                    Okyo.readInputStreamAsString(it)
                }
    }

    /**
     * Save the data within the [SaveRequest] at the given uri. Note that a new file will be created
     * base on the [SaveRequest]. The file name should not be included in the uri.
     * Note that file names are considered unique and any file with the same title will be overwritten.
     */
    fun <T> save(uri: String, request: SaveRequest<T>): Completable {
        return Completable.defer {
            val saveUri = "$uri/${request.title}"
            log { "Saving to $saveUri" }
            val file = file(saveUri)
                    .blockingGet()

            val json = moshi.adapter<T>(request.type)
                    .toJson(request.item)
            val contentStream = ByteArrayContent.fromString(MIME_TYPE_JSON, json)

            val savedFile = drive.files().update(file.id, null, contentStream)
                    .execute()

            log { "File $saveUri saved to file ${savedFile.identify()}" }
            Completable.complete()
        }
    }

    /**
     * Deletes the file or folder at the given uri.
     */
    fun delete(uri: String): Completable {
        return Completable.defer {
            val fileId = file(uri)
                    .blockingGet()
                    .id
            drive.files().delete(fileId)
                    .execute()
            Completable.complete()

        }
    }

    private fun startFolder(): File {
        return drive.files().get(SPACE_APP_DATA)
                .toSingle()
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

    private fun segments(uri: String): List<String> {
        return uri.split("/")
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
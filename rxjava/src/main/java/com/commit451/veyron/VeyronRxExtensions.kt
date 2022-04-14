package com.commit451.veyron

import com.google.api.services.drive.model.File
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers

fun Veyron.fileSingle(path: String): Single<File> {
    return Single.defer {
        Single.just(file(path))
    }
}

fun Veyron.searchSingle(path: String, query: String): Single<List<File>> {
    return Single.defer {
        Single.just(search(path, query))
    }
}

fun Veyron.filesSingle(path: String): Single<List<File>> {
    return Single.defer {
        Single.just(files(path))
    }
}

fun <T> Veyron.documentSingle(path: String, type: Class<T>): Single<VeyronResult<T>> {
    return Single.defer {
        Single.just(document(path, type))
    }
}

fun <T> Veyron.documentsSingle(path: String, type: Class<T>, query: String = ""): Single<List<T>> {
    return Single.defer {
        Single.just(documents(path, type, query))
    }
}

fun Veyron.stringSingle(path: String): Single<VeyronResult<String>> {
    return Single.defer {
        Single.just(string(path))
    }
}

fun Veyron.saveCompletable(path: String, request: SaveRequest): Completable {
    return Completable.defer {
        save(path, request)
        Completable.complete()
    }
}

/**
 * Save all of the files concurrently. Please be aware of rate limits and adjust [maxConcurrency] accordingly
 */
fun Veyron.saveCompletable(
    path: String,
    requests: List<SaveRequest>,
    maxConcurrency: Int = 4
): Completable {
    // https://stackoverflow.com/a/48965035
    return Completable.defer {
        Flowable.range(0, requests.size)
            .concatMapEager<Any>({ index ->
                saveCompletable(path, requests[index])
                    .subscribeOn(Schedulers.io())
                    .toFlowable()
            }, maxConcurrency, 1)
            .toList()
            .flatMapCompletable { Completable.complete() }
    }
}

fun Veyron.deleteCompletable(path: String): Completable {
    return Completable.defer {
        delete(path)
        Completable.complete()
    }
}
package com.commit451.veyron

import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveRequest
import io.reactivex.Completable
import io.reactivex.Single
import java.io.InputStream

fun <T> DriveRequest<T>.toSingle(): Single<T> {
    return Single.defer {
        val result = this.execute()
        Single.just(result)
    }
}

internal fun Drive.Files.Get.asInputStream(): Single<InputStream> {
    return Single.defer {
        Single.just(this.executeMediaAsInputStream())
    }
}

fun DriveRequest<Void>.toCompletable(): Completable {
    return Completable.defer {
        this.execute()
        Completable.complete()
    }
}
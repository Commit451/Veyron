# Veyron

[![Build Status](https://travis-ci.org/Commit451/Veyron.svg?branch=master)](https://travis-ci.org/Commit451/Veyron) [![](https://jitpack.io/v/Commit451/Veyron.svg)](https://jitpack.io/#Commit451/Veyron)

Easily store and fetch JSON in Google Drive.

## Usage
First, follow the [Android Google Drive SDK quick start sample](https://github.com/gsuitedevs/android-samples/tree/master/drive/quickstart) to get the project set up. Then, you will be able to use Veyron:
```kotlin
//when you have a valid DriveResourceClient
val veyron = Veyron.Builder(driveResourceClient)
    //optional configuration for Moshi
    //.moshi(moshi)
    .build()
```

### Create
```kotlin
val dog = Dog()
val metadataChangeSet = MetadataChangeSet.Builder()
        .setTitle("dog.json")
        .build()
val saveRequest = SaveRequest(Dog::class.java, thing, metadataChangeSet)

val url = "${Veyron.SCHEME_APP}://info"
//RxJava 2 Single is returned
veyron.save(url, saveRequest)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({
            snackbar("Saved")
        }, {
            it.printStackTrace()
        })
```

### Fetch
```kotlin
val url = "${Veyron.SCHEME_APP}://info/dog.json"
veyron.document(url, Dog::class.java)
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe({
        snackbar("Dog found")
    }, {
        it.printStackTrace()
    })
```

### Delete
```kotlin
val url = "${Veyron.SCHEME_APP}://info/dog.json"
veyron.delete(url)
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe({
        snackbar("Dog deleted")
    }, {
        it.printStackTrace()
    })
```

### Other Operations
Other operations you can perform:
- veyron.file(url)
- veyron.folder(url)
- veyron.driveId(url)

License
--------

    Copyright 2018 Commit 451

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

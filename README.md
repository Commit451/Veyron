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
val saveRequest = SaveDocumentRequest(Dog::class.java, "dog.json", thing)

val path = "info"
//RxJava Single is returned
veyron.save(path, saveRequest)
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
val path = "info/dog.json"
veyron.document(path, Dog::class.java)
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
val path = "info/dog.json"
veyron.delete(path)
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe({
        snackbar("Dogs deleted")
    }, {
        it.printStackTrace()
    })
```

### Other Operations
Other operations you can perform:
- veyron.file(path)
- veyron.files(path)
- veyron.search(path, query)

## Build Locally
You will need to set up a Google Cloud project with the same package name as the sample, and create a keystore that works with the details within the app's `build.gradle` file. Then, create a new `gradle.properties` file that looks like so:
```
KEYSTORE_PASSWORD=keystorepasswordhere
```

## Note
A few things to note about this library:
- It has dependencies. Please check them and understand what they do.
- Each query is limited to 1,000 results. This is something that will hopefully change one day, but it is the limit for now.

License
--------

    Copyright 2019 Commit 451

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

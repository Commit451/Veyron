# Veyron
[![Build](https://github.com/Commit451/Veyron/actions/workflows/ci.yml/badge.svg)](https://github.com/Commit451/Veyron/actions/workflows/ci.yml) [![Maven Central](https://img.shields.io/maven-central/v/com.commit451.veyron/veyron.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=g:com.commit451.veyron)

Easily store and fetch JSON, Files, and Strings in Google Drive on Android.

## Dependency
```kotlin
implementation("com.commit451.veyron:veyron:latest.version.here")
// And for RxJava methods:
implementation("com.commit451.veyron:rxjava:latest.version.here")
```
Compile with Java 17:
```kotlin
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

## Usage
You will first need to do the setup for the [Google Drive API](https://developers.google.com/drive). See the sample project for a complete example.
```kotlin
// when you have a valid Drive class:
val veyron = Veyron.Builder(drive)
    // optional configuration for Moshi, if you are saving classes instead of Strings/Files
    // .moshi(moshi)
    .build()
```

### Create
```kotlin
val dog = Dog()
val saveRequest = SaveRequest.Document("dog.json", Dog::class.java, thing)

val path = "info"
// RxJava Completable is returned
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
- veyron.documents(path)
- veyron.search(path, query)
- veyron.string(path)
- veyron.save(path, listOfSaveRequests())

## Build Locally
```bash
./gradlew :veyron:build :rxjava:build
```

## Note
A few things to note about this library:
- Assumes unique file and folder names. Unlike a normal file system, Google Drive allows you to create folders and files at the same level with the same name. We will always fetch/return the first result we find if there are duplicates.
- It has dependencies. Please check them and understand what they do.
- Each query is limited to 1,000 results, but we fetch all results. We do not currently support pagination or "infinite loading"

# R8/Proguard
If you are using R8/Proguard, you will need to include the rules for [Moshi](https://github.com/square/moshi#r8--proguard) and [okio](https://github.com/square/okio#r8--proguard). Rules for the [Google HTTP Client Library](https://developers.google.com/api-client-library/java/google-http-java-client/setup#proguard) are included as rules for this library.

License
--------

    Copyright 2023 Commit 451

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

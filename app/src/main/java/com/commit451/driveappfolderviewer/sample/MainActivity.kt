package com.commit451.driveappfolderviewer.sample

import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.commit451.aloy.AloyAdapter
import com.commit451.driveappfolderviewer.DriveAppFolderViewer
import com.commit451.driveappfolderviewer.DriveAppViewerBaseActivity
import com.commit451.veyron.SaveRequest
import com.commit451.veyron.Veyron
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.material.snackbar.Snackbar
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : DriveAppViewerBaseActivity() {

    companion object {
        private const val PATH_DOGS = "just-dogs"
        private const val PATH_FAVORITE_DOGS = "favorite-dogs"
        private const val FILE_DOGS = "dogs"
    }

    private lateinit var veyron: Veyron

    private var currentDogsResponse: DogsResponse? = null

    private lateinit var adapter: AloyAdapter<Dog, DogViewHolder>
    private lateinit var adapterFavorites: AloyAdapter<Dog, DogViewHolder>

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar.title = "Veyron"
        toolbar.inflateMenu(R.menu.debug)
        toolbar.inflateMenu(R.menu.clear_cache)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_debug -> {
                    val intent = DriveAppFolderViewer.intent(this)
                    startActivity(intent)
                    return@setOnMenuItemClickListener true
                }
                R.id.action_clear_cache -> {
                    veyron.clearCache()
                    snackbar("Cache cleared")
                    return@setOnMenuItemClickListener true
                }
            }
            false
        }

        buttonNewDog.setOnClickListener {

            val dog = Dog()
            dog.name = UUID.randomUUID().toString()
            dog.created = Date()

            val response = currentDogsResponse ?: DogsResponse()
            this.currentDogsResponse = response
            val dogs = response.dogs ?: mutableListOf()
            //in the case where we created a new list
            response.dogs = dogs
            dogs.add(dog)
            val saveRequest = SaveRequest.Document(FILE_DOGS, DogsResponse::class.java, response)

            veyron.save(PATH_DOGS, saveRequest)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        snackbar("Created dog")
                        loadDogs()
                    }, { throwable ->
                        error(throwable)
                    })
        }

        buttonDeleteAll.setOnClickListener {
            //we also have to delete the local
            currentDogsResponse?.dogs?.clear()
            veyron.delete(PATH_DOGS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        snackbar("All dogs deleted")
                        loadDogs()
                    }, { throwable ->
                        error(throwable)
                    })
        }

        buttonNewFavoriteDog.setOnClickListener {
            val dog = Dog()
            dog.name = UUID.randomUUID().toString()
            dog.created = Date()

            val saveRequest = SaveRequest.String(dog.name, "asdf")

            veyron.save(PATH_FAVORITE_DOGS, saveRequest)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        snackbar("Created favorite dog")
                        loadFavorites()
                    }, { throwable ->
                        error(throwable)
                    })
        }

        buttonManyFavoriteDog.setOnClickListener {
            val dogSaveRequests = mutableListOf<SaveRequest.String>()
            for (i in 0..9) {
                val dog = Dog()
                dog.name = UUID.randomUUID().toString()
                dog.created = Date()

                val saveRequest = SaveRequest.String(dog.name, "asdf")

                dogSaveRequests.add(saveRequest)
            }
            veyron.save(PATH_FAVORITE_DOGS, dogSaveRequests)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        snackbar("Created ${dogSaveRequests.size} favorite dogs")
                        loadFavorites()
                    }, { throwable ->
                        error(throwable)
                    })
        }

        buttonDeleteAllFavorite.setOnClickListener {
            adapterFavorites.clear()
            veyron.delete(PATH_FAVORITE_DOGS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        snackbar("All favorite dogs deleted")
                        load()
                    }, { throwable ->
                        error(throwable)
                    })
        }

        adapter = AloyAdapter({ parent, _ ->
            DogViewHolder.inflate(parent)
        }, { viewHolder, _, item ->
            viewHolder.bind(item)
        })

        adapterFavorites = AloyAdapter({ parent, _ ->
            DogViewHolder.inflate(parent)
        }, { viewHolder, _, item ->
            viewHolder.bind(item)
        })

        listDogs.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        listDogs.adapter = adapter

        listFavoriteDogs.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        listFavoriteDogs.adapter = adapterFavorites

        swipeRefreshLayout.setOnRefreshListener { load() }
    }

    override fun onSignedIn(googleSignInAccount: GoogleSignInAccount) {
        super.onSignedIn(googleSignInAccount)
        val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .add(Date::class.java, Rfc3339DateJsonAdapter())
                //etc
                .build()
        veyron = Veyron.Builder(drive!!)
                .moshi(moshi)
                .verbose(true)
                .build()
        load()
    }

    override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }

    private fun load() {
        loadDogs()
        loadFavorites()
    }

    private fun loadDogs() {
        swipeRefreshLayout.isRefreshing = true
        disposables.add(
                veyron.document("$PATH_DOGS/$FILE_DOGS", DogsResponse::class.java)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            swipeRefreshLayout.isRefreshing = false
                            if (it.result != null) {
                                currentDogsResponse = it.result
                                it.result?.dogs?.let { dogs ->
                                    adapter.set(dogs)
                                }
                            }
                        }, { throwable ->
                            error(throwable)
                        })
        )
    }

    private fun loadFavorites() {
        swipeRefreshLayout.isRefreshing = true
        disposables.add(
                veyron.files(PATH_FAVORITE_DOGS)
                        .map {
                            it.map { file ->
                                val dog = Dog()
                                dog.name = file.name
                                //dog.created = Date(file.createdTime.value)
                                dog
                            }
                        }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            swipeRefreshLayout.isRefreshing = false
                            adapterFavorites.set(it)
                        }, {
                            error(it)
                        })
        )
    }

    private fun snackbar(message: String) {
        Snackbar.make(root, message, Snackbar.LENGTH_SHORT)
                .show()
    }

    private fun log(message: String) {
        Log.d("Sample", message)
    }

    private fun error(t: Throwable) {
        swipeRefreshLayout.isRefreshing = false
        t.printStackTrace()
        snackbar("Something bad happened. Check the logs")
    }
}

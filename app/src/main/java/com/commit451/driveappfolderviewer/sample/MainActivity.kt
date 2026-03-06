package com.commit451.driveappfolderviewer.sample

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.commit451.aloy.AloyAdapter
import com.commit451.driveappfolderviewer.DriveAppFolderViewer
import com.commit451.driveappfolderviewer.DriveAppViewerBaseActivity
import com.commit451.driveappfolderviewer.sample.databinding.ActivityMainBinding
import com.commit451.veyron.SaveRequest
import com.commit451.veyron.Veyron
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.material.snackbar.Snackbar
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

class MainActivity : DriveAppViewerBaseActivity() {

    companion object {
        private const val PATH_DOGS = "just-dogs"
        private const val FILE_DOGS = "dogs"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var veyron: Veyron

    private var currentDogsResponse: DogsResponse? = null

    private lateinit var adapter: AloyAdapter<Dog, DogViewHolder>
    private lateinit var adapterFavorites: AloyAdapter<Dog, DogViewHolder>

    private lateinit var viewModel: MainActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainActivityViewModel::class.java]
        viewModel.uiState.observe(this) {
            binding.swipeRefreshLayout.isRefreshing = false
            adapterFavorites.set(it.favorites)
        }
        binding.toolbar.title = "Veyron"
        binding.toolbar.inflateMenu(R.menu.debug)
        binding.toolbar.inflateMenu(R.menu.clear_cache)
        binding.toolbar.setOnMenuItemClickListener {
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

        binding.buttonNewDog.setOnClickListener {
            lifecycleScope.launch {
                val dog = Dog().apply {
                    name = UUID.randomUUID().toString()
                    created = Date()
                }

                val response = currentDogsResponse ?: DogsResponse()
                currentDogsResponse = response
                val dogs = response.dogs ?: mutableListOf()
                response.dogs = dogs
                dogs.add(dog)
                val saveRequest = SaveRequest.Document(FILE_DOGS, DogsResponse::class.java, response)

                runCatching {
                    veyron.save(PATH_DOGS, saveRequest)
                }.onSuccess {
                    snackbar("Created dog")
                    loadDogs()
                }.onFailure { throwable ->
                    error(throwable)
                }
            }
        }

        binding.buttonDeleteAll.setOnClickListener {
            lifecycleScope.launch {
                currentDogsResponse?.dogs?.clear()
                runCatching {
                    veyron.delete(PATH_DOGS)
                }.onSuccess {
                    snackbar("All dogs deleted")
                    loadDogs()
                }.onFailure { throwable ->
                    error(throwable)
                }
            }
        }

        binding.buttonNewFavoriteDog.setOnClickListener {
            lifecycleScope.launch {
                val dog = Dog().apply {
                    name = UUID.randomUUID().toString()
                    created = Date()
                }
                val saveRequest = SaveRequest.String(dog.name, "asdf")

                runCatching {
                    veyron.save(MainActivityViewModel.PATH_FAVORITE_DOGS, saveRequest)
                }.onSuccess {
                    snackbar("Created favorite dog")
                    loadFavorites()
                }.onFailure { throwable ->
                    error(throwable)
                }
            }
        }

        binding.buttonManyFavoriteDog.setOnClickListener {
            lifecycleScope.launch {
                val dogSaveRequests = mutableListOf<SaveRequest.String>()
                repeat(10) {
                    val dog = Dog().apply {
                        name = UUID.randomUUID().toString()
                        created = Date()
                    }
                    dogSaveRequests += SaveRequest.String(dog.name, "asdf")
                }

                runCatching {
                    veyron.save(MainActivityViewModel.PATH_FAVORITE_DOGS, dogSaveRequests)
                }.onSuccess {
                    snackbar("Created ${dogSaveRequests.size} favorite dogs")
                    loadFavorites()
                }.onFailure { throwable ->
                    error(throwable)
                }
            }
        }

        binding.buttonDeleteAllFavorite.setOnClickListener {
            lifecycleScope.launch {
                adapterFavorites.clear()
                runCatching {
                    veyron.delete(MainActivityViewModel.PATH_FAVORITE_DOGS)
                }.onSuccess {
                    snackbar("All favorite dogs deleted")
                    load()
                }.onFailure { throwable ->
                    error(throwable)
                }
            }
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

        binding.listDogs.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.listDogs.adapter = adapter

        binding.listFavoriteDogs.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.listFavoriteDogs.adapter = adapterFavorites

        binding.swipeRefreshLayout.setOnRefreshListener { load() }
    }

    override fun onSignedIn(googleSignInAccount: GoogleSignInAccount) {
        super.onSignedIn(googleSignInAccount)
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .add(Date::class.java, Rfc3339DateJsonAdapter())
            // etc
            .build()
        veyron = Veyron.Builder(drive!!)
            .moshi(moshi)
            .verbose(true)
            .build()
        load()
    }

    private fun load() {
        loadDogs()
        loadFavorites()
    }

    private fun loadDogs() {
        binding.swipeRefreshLayout.isRefreshing = true
        lifecycleScope.launch {
            runCatching {
                veyron.document("$PATH_DOGS/$FILE_DOGS", DogsResponse::class.java)
            }.onSuccess {
                binding.swipeRefreshLayout.isRefreshing = false
                if (it.result != null) {
                    currentDogsResponse = it.result
                    it.result?.dogs?.let { dogs ->
                        adapter.set(dogs)
                    }
                }
            }.onFailure { throwable ->
                error(throwable)
            }
        }
    }

    private fun loadFavorites() {
        binding.swipeRefreshLayout.isRefreshing = true
        viewModel.loadFavorites(veyron)
    }

    private fun snackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .show()
    }

    private fun log(message: String) {
        Log.d("Sample", message)
    }

    private fun error(t: Throwable) {
        binding.swipeRefreshLayout.isRefreshing = false
        t.printStackTrace()
        snackbar("Something bad happened. Check the logs")
    }
}

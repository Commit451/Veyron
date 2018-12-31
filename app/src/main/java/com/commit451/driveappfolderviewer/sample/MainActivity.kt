package com.commit451.driveappfolderviewer.sample

import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.commit451.adapterlayout.AdapterLayout
import com.commit451.aloy.AloyAdapter
import com.commit451.driveappfolderviewer.DriveAppFolderViewer
import com.commit451.driveappfolderviewer.DriveAppViewerBaseActivity
import com.commit451.veyron.SaveRequest
import com.commit451.veyron.Veyron
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.material.snackbar.Snackbar
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : DriveAppViewerBaseActivity() {

    companion object {
        private const val URL_DOGS = "favorite-dogs"
        private const val FILE_DOGS = "dogs"
    }

    private lateinit var veyron: Veyron

    private var currentDogsResponse: DogsResponse? = null

    private lateinit var adapter: AloyAdapter<Dog, DogViewHolder>

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar.title = "Veyron"
        toolbar.inflateMenu(R.menu.debug)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_debug -> {
                    val intent = DriveAppFolderViewer.intent(this)
                    startActivity(intent)
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
            val saveRequest = SaveRequest(DogsResponse::class.java, response, FILE_DOGS)

            veyron.save(URL_DOGS, saveRequest)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        snackbar("Saved")
                        load()
                    }, { throwable ->
                        error(throwable)
                    })

        }

        buttonDeleteAll.setOnClickListener {
            //we also have to delete the local
            currentDogsResponse?.dogs?.clear()
            veyron.delete(URL_DOGS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        snackbar("All dogs deleted")
                        load()
                    }, { throwable ->
                        error(throwable)
                    })
        }

        //later, in onCreate for example:
        adapter = AloyAdapter({ parent, _ ->
            val holder = DogViewHolder.inflate(parent)
            holder.itemView.setOnClickListener {
                val cheese = adapter.items[AdapterLayout.getAdapterPosition(holder)]
                Snackbar.make(root, "${cheese.name} clicked", Snackbar.LENGTH_SHORT)
                        .show()
            }
            holder
        }, { viewHolder, _, item ->
            viewHolder.bind(item)
        })

        listDogs.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener {
            load()
        }
    }

    override fun onSignedIn(googleSignInAccount: GoogleSignInAccount) {
        super.onSignedIn(googleSignInAccount)
        val moshi = Moshi.Builder()
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
        disposables.add(
                veyron.document("$URL_DOGS/$FILE_DOGS", DogsResponse::class.java)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            swipeRefreshLayout.isRefreshing = false
                            if (it.result != null) {
                                currentDogsResponse = it.result
                                it.result?.dogs?.let { dogs ->
                                    adapter.set(dogs)
                                }
                            } else {
                                Log.d("TAG", "No dogs created yet")
                                adapter.clear()
                                snackbar("No dogs found")
                            }
                        }, { throwable ->
                            swipeRefreshLayout.isRefreshing = false
                            error(throwable)
                        })
        )
    }

    private fun snackbar(message: String) {
        Snackbar.make(root, message, Snackbar.LENGTH_SHORT)
                .show()
    }

    private fun error(t: Throwable) {
        t.printStackTrace()
        snackbar("Something bad happened. Check the logs")
    }

    class DogViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        companion object {

            fun inflate(parent: ViewGroup): DogViewHolder {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_dog, parent, false)
                return DogViewHolder(view)
            }
        }

        val text: TextView = view.findViewById(R.id.text)
        val textCreated: TextView = view.findViewById(R.id.textCreated)

        fun bind(dog: Dog) {
            text.text = dog.name
            textCreated.text = "${DateFormat.getLongDateFormat(text.context).format(dog.created)} at ${DateFormat.getTimeFormat(text.context).format(dog.created)}"
        }
    }
}

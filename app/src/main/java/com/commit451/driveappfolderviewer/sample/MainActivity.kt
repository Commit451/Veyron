package com.commit451.driveappfolderviewer.sample

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.recyclerview.widget.RecyclerView
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.commit451.adapterlayout.AdapterLayout
import com.commit451.aloy.AloyAdapter
import com.commit451.driveappfolderviewer.DriveAppFolderViewerActivity
import com.commit451.driveappfolderviewer.DriveAppViewerBaseActivity
import com.commit451.driveappfolderviewer.sample.R.id.*
import com.commit451.veyron.SaveRequest
import com.commit451.veyron.Veyron
import com.google.android.gms.drive.MetadataChangeSet
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import com.squareup.moshi.Rfc3339DateJsonAdapter
import com.squareup.moshi.Moshi

class MainActivity : DriveAppViewerBaseActivity() {

    companion object {
        const val URL_DOGS = "${Veyron.SCHEME_APP}://dogs"
        const val FILE_DOGS = "dogs.json"
    }

    lateinit var veyron: Veyron

    var currentDogsResponse: DogsResponse? = null

    //top of file
    lateinit var adapter: AloyAdapter<Dog, DogViewHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar.title = "Veyron"
        toolbar.inflateMenu(R.menu.debug)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_debug -> {
                    val intent = DriveAppFolderViewerActivity.newIntent(this)
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

            val metadataChangeSet = MetadataChangeSet.Builder()
                    .setTitle(FILE_DOGS)
                    .build()
            val response = currentDogsResponse ?: DogsResponse()
            this.currentDogsResponse = response
            val dogs = response.dogs ?: mutableListOf()
            //in the case where we created a new list
            response.dogs = dogs
            dogs.add(dog)
            val saveRequest = SaveRequest(DogsResponse::class.java, response, metadataChangeSet)

            veyron.save(URL_DOGS, saveRequest)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        snackbar("Saved")
                        load()
                    }, {
                        error(it)
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
                    }, {
                        error(it)
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
    }

    override fun onSignedIn() {
        super.onSignedIn()
        val moshi = Moshi.Builder()
                .add(Date::class.java, Rfc3339DateJsonAdapter())
                //etc
                .build()
        veyron = Veyron.Builder(driveResourceClient)
                .moshi(moshi)
                .verbose(true)
                .build()
        load()
    }

    fun load() {
        veyron.document("$URL_DOGS/$FILE_DOGS", DogsResponse::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it.result != null) {
                        currentDogsResponse = it.result
                        it.result?.dogs?.let {
                            adapter.set(it)
                        }
                    } else {
                        Log.d("TAG", "No dogs created yet")
                        adapter.clear()
                    }
                }, {
                    error(it)
                })
    }

    fun snackbar(message: String) {
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

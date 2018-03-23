package com.commit451.driveappfolderviewer.sample

import android.os.Bundle
import android.support.design.widget.Snackbar
import com.commit451.driveappfolderviewer.DriveAppFolderViewerActivity
import com.commit451.driveappfolderviewer.DriveAppViewerBaseActivity
import com.commit451.veyron.SaveRequest
import com.commit451.veyron.Veyron
import com.google.android.gms.drive.MetadataChangeSet
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : DriveAppViewerBaseActivity() {

    companion object {
        const val url = "${Veyron.SCHEME_APP}/stuff"
        const val file = "thing"
    }

    lateinit var veyron: Veyron

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

        buttonCreate.setOnClickListener {

            val thing = Thing()
            thing.stuff = System.currentTimeMillis().toString()
            val metadataChangeSet = MetadataChangeSet.Builder()
                    .setTitle(file)
                    .build()
            val saveRequest = SaveRequest(Thing::class.java, thing, metadataChangeSet)

            veyron.save(url, saveRequest)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        snackbar("Saved")
                    }, {
                        it.printStackTrace()
                    })

        }
    }

    override fun onSignedIn() {
        super.onSignedIn()
        veyron = Veyron.Builder(driveResourceClient)
                .build()
        veyron.document(url, Thing::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    textThing.text = it.result?.stuff
                }, {
                    it.printStackTrace()
                })
    }

    fun snackbar(message: String) {
        Snackbar.make(root, message, Snackbar.LENGTH_SHORT)
                .show()
    }
}

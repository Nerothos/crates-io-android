package com.bmco.cratesiounofficial.activities

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bmco.cratesiounofficial.Networking
import com.bmco.cratesiounofficial.R
import com.bmco.cratesiounofficial.Utility
import com.bmco.cratesiounofficial.interfaces.OnDependencyDownloadListener
import com.bmco.cratesiounofficial.models.Alert
import com.bmco.cratesiounofficial.models.Crate
import com.bmco.cratesiounofficial.models.Dependency
import com.google.gson.reflect.TypeToken
import com.mukesh.MarkdownView
import com.varunest.sparkbutton.SparkButton
import ru.dimorinny.floatingtextbutton.FloatingTextButton
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class CrateActivity : AppCompatActivity() {

    internal var crate: Crate? = null

    internal lateinit var downloads: TextView
    private lateinit var maxVersion: TextView
    private lateinit var createdAt: TextView
    private lateinit var description: TextView
    private lateinit var readme: MarkdownView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crate)

        downloads = findViewById(R.id.crate_downloads)
        maxVersion = findViewById(R.id.crate_max_version)
        createdAt = findViewById(R.id.crate_created_at)
        description = findViewById(R.id.crate_description)
        readme = findViewById(R.id.readme)

        val actionBar = supportActionBar
        actionBar!!.setHomeButtonEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        val intent = intent
        val data = intent.data
        if (data != null) {
            val path = data.toString().split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val id = path[4]
            downloadCrateInfo(id)
        } else {
            crate = getIntent().getSerializableExtra("crate") as Crate
            init()
            downloadCrateInfo(crate!!.id)
        }
    }

    private fun downloadCrateInfo(id: String?) {
        Networking.getCrateById(id!!, { crate ->
            if (crate != null) {
                this.crate = crate
                downloads.post { this.init() }
            }
            null
        }, { error -> null })
    }

    private fun init() {
        title = crate!!.name

        val alertButton = findViewById<SparkButton>(R.id.alert_button)

        val homepage = findViewById<FloatingTextButton>(R.id.home_link)
        val repo = findViewById<FloatingTextButton>(R.id.rep_link)
        val docs = findViewById<FloatingTextButton>(R.id.doc_link)

        if (crate!!.homepage != null) {
            homepage.setBackgroundColor(resources.getColor(R.color.colorPrimary))
            homepage.setOnClickListener { v ->
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(crate!!.homepage.toString())
                startActivity(intent)
            }
        }

        if (crate!!.documentation != null) {
            docs.setBackgroundColor(resources.getColor(R.color.colorPrimary))
            docs.setOnClickListener { v ->
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(crate!!.documentation.toString())
                startActivity(intent)
            }
        }

        if (crate!!.repository != null) {
            repo.setBackgroundColor(resources.getColor(R.color.colorPrimary))
            repo.setOnClickListener { v ->
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(crate!!.repository.toString())
                startActivity(intent)
            }
        }

        val df = DecimalFormat("#,##0")
        downloads.text = df.format(java.lang.Long.valueOf(crate!!.downloads.toLong()))
        maxVersion.text = crate!!.maxVersion

        val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss", Locale.getDefault())
        try {
            val date = formatter.parse(crate!!.createdAt!!.substring(0, 18))
            createdAt.text = format.format(date!!)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        description.text = crate!!.description

        val markdownView = findViewById<MarkdownView>(R.id.readme)

        if (crate!!.versionList != null) {
            val markdown = crate!!.versionList!![0].readme
            markdownView.setMarkDownText(markdown)
        } else {
            markdownView.setMarkDownText("Loading...")
        }

        val dependencies = findViewById<LinearLayout>(R.id.dependency_group)

        crate!!.getDependencies(object: OnDependencyDownloadListener {
            override fun onDependenciesReady(dependency: List<Dependency>) {
                for (d in dependency) {
                    val button = LayoutInflater.from(dependencies.context).inflate(R.layout.link_button, null) as FloatingTextButton
                    button.setTitle(d.crateId)
                    button.setOnClickListener { v ->
                        val dialog = AlertDialog.Builder(dependencies.context).create()
                        dialog.setTitle("Loading")
                        dialog.setCancelable(false)
                        dialog.setView(LayoutInflater.from(dependencies.context).inflate(R.layout.load_alert, null))
                        dialog.show()

                        Networking.getCrateById(d.crateId!!, { crate ->
                            dependencies.post {
                                val intent = Intent(dependencies.context, CrateActivity::class.java)
                                intent.putExtra("crate", crate)
                                dialog.dismiss()
                                startActivity(intent)
                            }
                        }, {  })
                    }
                    dependencies.post { dependencies.addView(button) }
                }
            }
        })

        val context = this

        var alerts: MutableList<Alert>?
        val listType = object : TypeToken<ArrayList<Alert>>() {

        }.type
        alerts = Utility.loadData<MutableList<Alert>>("alerts", listType)
        if (alerts == null) {
            alerts = ArrayList()
        }

        for (alert in alerts) {
            if (alert.crate!!.name == crate!!.name) {
                alertButton.isChecked = alert.isDownloads || alert.isVersion
                break
            }
        }

        alertButton.setOnClickListener { v ->
            val dialog = AlertDialog.Builder(context).create()
            val view = LayoutInflater.from(context).inflate(R.layout.alert_activate_dialog, null)
            val downloads = view.findViewById<CheckBox>(R.id.downloads)
            val version = view.findViewById<CheckBox>(R.id.version)
            dialog.setView(view)
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Save") { dialog1, which ->
                for (alert in alerts) {
                    if (alert.crate!!.name == crate!!.name) {
                        alert.isDownloads = downloads.isChecked
                        alert.isVersion = version.isChecked
                        Utility.saveData("alerts", alerts)
                        if (alert.isDownloads || alert.isVersion) {
                            alertButton.isChecked = true
                            alertButton.playAnimation()
                        } else {
                            alertButton.isChecked = false
                        }
                    }
                }
                val alert = Alert(version.isChecked, downloads.isChecked, crate)
                alerts.add(alert)
                Utility.saveData("alerts", alerts)
                if (alert.isDownloads || alert.isVersion) {
                    alertButton.isChecked = true
                    alertButton.playAnimation()
                }
            }
            for (alert in alerts) {
                if (alert.crate!!.name == crate!!.name) {
                    downloads.isChecked = alert.isDownloads
                    version.isChecked = alert.isVersion
                    break
                }
            }
            dialog.show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.crate_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_share -> {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_SUBJECT, "Check out this awesome crate:")
                intent.putExtra(Intent.EXTRA_TEXT, "https://crates.io/crates/" + crate!!.id!!)
                startActivity(Intent.createChooser(intent, "Share crate with..."))
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

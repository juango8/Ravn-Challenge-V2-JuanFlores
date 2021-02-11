package com.example.strarwarsguide.ui.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.coroutines.await
import com.apollographql.apollo.exception.ApolloException
import com.example.strarwarsguide.R
import com.example.strarwarsguide.server.apolloClient
import com.example.strarwarsguide.ui.adapter.VehiclesAdapter
import com.example.swapi.PersonInformationQuery
import kotlinx.android.synthetic.main.activity_detail_character.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.*

class DetailCharacter : AppCompatActivity() {

    companion object {
        private const val TAG = "DetailCharacter"
    }

    private lateinit var actionBar: ActionBar

    private lateinit var mKeyValueLayout: LinearLayout
    private lateinit var mVehiclesRecyclerView: RecyclerView
    private lateinit var mVehiclesListAdapter: VehiclesAdapter
    private lateinit var mApolloClient: ApolloClient

    private var mVehiclesList = mutableListOf<PersonInformationQuery.Vehicle>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_character)

        mApolloClient = apolloClient
        actionBar = supportActionBar!!
        mKeyValueLayout = findViewById(R.id.ll_information)
        mVehiclesRecyclerView = findViewById(R.id.rv_vehicles)
        mVehiclesListAdapter = VehiclesAdapter(this, mVehiclesList)
        mVehiclesRecyclerView.adapter = mVehiclesListAdapter
        mVehiclesRecyclerView.layoutManager = LinearLayoutManager(this)

        actionBar.setDisplayHomeAsUpEnabled(true)

        val id = intent.getStringExtra("id")
        if (id != null) {
            lifecycleScope.launch {
                getInformation(id)
            }
        } else {
            ll_loading.visibility = View.GONE
            ll_failed.visibility = View.VISIBLE
        }

    }

    private suspend fun getInformation(id: String): Unit = coroutineScope {
        val response = try {
            mApolloClient.query(PersonInformationQuery(id = id)).await()
        } catch (e: ApolloException) {
            runOnUiThread {
                ll_loading.visibility = View.GONE
                ll_failed.visibility = View.VISIBLE
            }
            return@coroutineScope
        }

        val person = response.data?.person
        if (person == null || response.hasErrors()) {
            runOnUiThread {
                ll_loading.visibility = View.GONE
                ll_failed.visibility = View.VISIBLE
            }
            return@coroutineScope
        }

        Log.d(TAG, "getPersonInformation: $person")

        mVehiclesList
            .addAll(person.vehicleConnection?.vehicles?.filterNotNull() ?: emptyList())

        runOnUiThread {
            // Add data to "General Information" layout
            ll_detail.visibility = View.VISIBLE
            actionBar.title = person.name ?: "Unknown"
            addItemDataView(getString(R.string.eye_color), person.eyeColor ?: "Unknown")
            addItemDataView(getString(R.string.hair_color), person.hairColor ?: "Unknown")
            addItemDataView(getString(R.string.skin_color), person.skinColor ?: "Unknown")
            addItemDataView(getString(R.string.birth_year), person.birthYear ?: "Unknown")
            // Notify changes to the vehicles adapter
            mVehiclesListAdapter.notifyDataSetChanged()
            ll_loading.visibility = View.GONE
        }
    }

    private fun addItemDataView(key: String, value: String) {
        val view = layoutInflater.inflate(R.layout.item_information, mKeyValueLayout, false)
        view.findViewById<TextView>(R.id.dataL).text = key
        view.findViewById<TextView>(R.id.dataR).text = value
        mKeyValueLayout.addView(view)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

}
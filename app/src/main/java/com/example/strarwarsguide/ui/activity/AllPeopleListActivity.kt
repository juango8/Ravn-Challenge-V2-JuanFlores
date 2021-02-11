package com.example.strarwarsguide.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.coroutines.await
import com.apollographql.apollo.exception.ApolloException
import com.example.strarwarsguide.R
import com.example.strarwarsguide.server.apolloClient
import com.example.strarwarsguide.ui.adapter.AllPeopleListAdapter
import com.example.swapi.AllPeoplePaginatedQuery
import kotlinx.android.synthetic.main.activity_all_people_list.*
import kotlinx.coroutines.*
import kotlin.time.ExperimentalTime


class AllPeopleListActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AllPeopleListActivity"
    }

    private var mApolloClient = apolloClient
    private lateinit var mPeopleListAdapter: AllPeopleListAdapter

    private var mPeopleList = mutableListOf<AllPeoplePaginatedQuery.Person>()
    private var mCurrEndCursor: String? = null


    @ExperimentalTime
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_people_list)

        mPeopleListAdapter = AllPeopleListAdapter(this, mPeopleList)
        mPeopleListAdapter.onItemClickListener { person ->
            getDetailOf(person.id)
            Log.i(TAG, "click on item")
        }

        rv_all_people.adapter = mPeopleListAdapter
        rv_all_people.layoutManager = LinearLayoutManager(this)


        lifecycleScope.launch {
            while (true) {
                val result = try {
                    getDataServer()
                } catch (e: Exception) {
                    Log.d(TAG, "error $e")
                    break
                }

                when (result) {
                    LoadResult.Empty -> {
                        ll_loading.visibility = View.GONE
                        break
                    }
                    LoadResult.Successful -> {
                        rv_all_people.visibility = View.VISIBLE
                        runOnUiThread { ll_loading.visibility = View.VISIBLE }
                    }
                }
            }
        }
    }

    private suspend fun getDataServer(): LoadResult = coroutineScope {
        val response = try {
            mApolloClient
                .query(AllPeoplePaginatedQuery(get = 5, Input.optional(mCurrEndCursor)))
                .await()
        } catch (e: ApolloException) {
            runOnUiThread { showLoadError() }
            throw e
        }

        val allPeople = response.data?.allPeople
        if (allPeople == null || response.hasErrors()) {
            runOnUiThread { showLoadError() }
            throw Exception("Failed to get data from endpoint")
        }

        if (allPeople.people?.isEmpty() != false) {
            runOnUiThread { ll_loading.visibility = View.GONE }
            return@coroutineScope LoadResult.Empty
        }

        val lastIndex = mPeopleList.size
        mPeopleList.addAll(allPeople.people.filterNotNull())

        runOnUiThread {
            mPeopleListAdapter.notifyItemRangeInserted(lastIndex, 5)
        }
        if (allPeople.pageInfo.endCursor == null)
            throw Exception("Get a null cursor")

        mCurrEndCursor = allPeople.pageInfo.endCursor
        return@coroutineScope LoadResult.Successful
    }

    private fun showLoadError() {
        ll_loading.visibility = View.GONE
        rv_all_people.visibility = View.GONE
        ll_failed.visibility = View.VISIBLE
    }

    private enum class LoadResult {
        Empty,
        Successful
    }

    private fun getDetailOf(id: String) {
        val intent = Intent(this,  DetailCharacter::class.java)
        intent.putExtra("id", id)
        startActivity(intent)
    }
}
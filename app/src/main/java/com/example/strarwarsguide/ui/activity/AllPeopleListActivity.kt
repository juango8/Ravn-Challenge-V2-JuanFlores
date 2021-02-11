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
    private lateinit var mAllPeopleListAdapter: AllPeopleListAdapter

    private var mPeopleList = mutableListOf<AllPeoplePaginatedQuery.Person>()
    private var mCurrEndCursor: String? = null


    @ExperimentalTime
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_people_list)

        mAllPeopleListAdapter = AllPeopleListAdapter(this, mPeopleList)
        mAllPeopleListAdapter.onItemClickListener { person ->
            Log.i(TAG, "click on item of ${person.name}")
            getDetailOf(person.id)
        }

        rv_all_people.adapter = mAllPeopleListAdapter
        rv_all_people.layoutManager = LinearLayoutManager(this)


        /**
         * Courutine Scope to work in another thread
         */
        lifecycleScope.launch {
            while (true) {
                val result = try {
                    getDataServer()
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                    break
                }

                when (result) {
                    LoadResult.Empty -> {
                        hideLoad()
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

    /**
     * Get list of character of the server SWAPI from server, check if response has any errors
     * or is empty, else get another five characters and save CurrEndCursor to the next query
     * @return LoadResult.Empty if it is not more data of characters
     * @return LoadResult.Successful if is more data of characters
     */
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
            runOnUiThread { hideLoad() }
            return@coroutineScope LoadResult.Empty
        }

        val lastIndex = mPeopleList.size
        mPeopleList.addAll(allPeople.people.filterNotNull())

        runOnUiThread {
            mAllPeopleListAdapter.notifyItemRangeInserted(lastIndex, 5)
        }
        if (allPeople.pageInfo.endCursor == null)
            throw Exception("Get a null cursor")

        mCurrEndCursor = allPeople.pageInfo.endCursor
        return@coroutineScope LoadResult.Successful
    }

    /**
     * show a failed message in the view
     */
    private fun showLoadError() {
        ll_loading.visibility = View.GONE
        rv_all_people.visibility = View.GONE
        ll_failed.visibility = View.VISIBLE
    }

    /**
     * hide the loading message in the view
     */
    private fun hideLoad(){
        ll_loading.visibility = View.GONE
    }

    private enum class LoadResult {
        Empty,
        Successful
    }

    /**
     * add a extra to the intent that is the id of the character clicked
     */
    private fun getDetailOf(id: String) {
        val intent = Intent(this,  DetailCharacter::class.java)
        intent.putExtra("id", id)
        startActivity(intent)
    }
}
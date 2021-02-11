package com.example.strarwarsguide.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.strarwarsguide.R
import com.example.swapi.AllPeoplePaginatedQuery

class AllPeopleListAdapter(
    context: Context,
    peopleList: MutableList<AllPeoplePaginatedQuery.Person>
) : RecyclerView.Adapter<AllPeopleListAdapter.PersonViewHolder>() {
    class PersonViewHolder(itemView: View, adapter: AllPeopleListAdapter) :
        RecyclerView.ViewHolder(itemView) {
        private val mAdapter = adapter
        val personName: TextView = itemView.findViewById(R.id.personName)
        val personFooter: TextView = itemView.findViewById(R.id.personFooter)

        init {
            itemView.setOnClickListener {
                mAdapter.onItemClick(layoutPosition)
            }
        }
    }

    private val mPeopleList = peopleList
    private val mInflater = LayoutInflater.from(context)

    private var mOnItemClickListener: ((AllPeoplePaginatedQuery.Person) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonViewHolder {
        val mItemView = mInflater.inflate(R.layout.item_person, parent, false)
        return PersonViewHolder(mItemView, this)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
        val person = mPeopleList[position]
        holder.personName.text = person.name

        val species = person.species?.name ?: "unknown"
        val homeWorld = person.homeworld?.name ?: "far, far away"
        holder.personFooter.text = "$species from $homeWorld"
    }

    override fun getItemCount(): Int {
        return mPeopleList.size
    }

    fun onItemClickListener(listener: (AllPeoplePaginatedQuery.Person) -> Unit) {
        mOnItemClickListener = listener
    }

    private fun onItemClick(position: Int) {
        mOnItemClickListener?.invoke(mPeopleList[position])
    }
}
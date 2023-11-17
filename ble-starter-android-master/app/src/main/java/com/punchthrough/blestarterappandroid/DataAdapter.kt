/*
 * Copyright 2021 Punch Through Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.punchthrough.blestarterappandroid

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import kotlinx.android.synthetic.main.data_table.view.tvCount
import kotlinx.android.synthetic.main.data_table.view.tvDatetime
import kotlinx.android.synthetic.main.data_table.view.tvHumid
import kotlinx.android.synthetic.main.data_table.view.tvTemp

class DataAdapter (
    var list: MutableList<DataFrame>
) : RecyclerView.Adapter<DataAdapter.ListViewHolder>(){

    inner class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder{
        val view = LayoutInflater.from(parent.context).inflate(R.layout.data_table, parent, false)
        return ListViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }
    override fun onBindViewHolder(holder: ListViewHolder, position: Int){
        val curElement = list[position]
        holder.itemView.apply{
            tvCount.text = curElement.count.toString()
            tvDatetime.text = curElement.datetime
            tvTemp.text = curElement.temperature.toString()
            tvHumid.text = curElement.humidity.toString()
            //setOnClickListener { onItemClick?.invoke(curElement.to_do) }
        }
    }
    fun addNewElement(new_CheckList: DataFrame){
        list.add(new_CheckList)
        notifyItemInserted(list.size - 1)
    }
    fun RemoveItems(){
        list.clear()
        notifyDataSetChanged()
    }
}
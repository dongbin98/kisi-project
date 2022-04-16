package com.dbsh.calendarapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.RadioButton
import android.widget.TextView

// 커스텀 어댑터
class ListAdapter(context: Context, listviewDataList: ArrayList<String>, listviewTimeList: ArrayList<String>, listviewImportanceList: ArrayList<String>) : BaseAdapter() {
    var context: Context = context
    var listviewDataList: ArrayList<String> = listviewDataList
    var listviewTimeList: ArrayList<String> = listviewTimeList
    var listviewImportanceList: ArrayList<String> = listviewImportanceList

    override fun getCount(): Int {
        return listviewDataList.size
    }
    override fun getItem(p0: Int): Any {
        return listviewDataList[p0]
    }
    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }
    override fun getView(position: Int, p1: View?, p2: ViewGroup?): View {
        val view: View = LayoutInflater.from(context).inflate(R.layout.listview_itemlist, null)
        val radioButton = view.findViewById(R.id.radiobutton) as RadioButton
        val listView = view.findViewById(R.id.list) as TextView
        val timeView = view.findViewById(R.id.time) as TextView
        val impView = view.findViewById(R.id.imp) as TextView

        listView.text = listviewDataList[position]
        timeView.text = listviewTimeList[position]
        impView.text = listviewImportanceList[position]
        radioButton.isChecked = (position == selectedItem)

        return view
    }
}
package com.ex.serialport.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.ex.serialport.R

class SpAdapter(var mContext: Context) : BaseAdapter() {
    var datas: Array<String>? = null
    fun setDatasLk(datas: Array<String>?) {
        this.datas = datas
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return if (datas == null) 0 else datas!!.size
    }

    override fun getItem(position: Int): Any {
        return (if (datas == null) null else datas!![position])!!
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        var hodler: ViewHodler? = null
        if (convertView == null) {
            hodler = ViewHodler()
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_layout, null)
            hodler.mTextView = convertView as TextView
            convertView.setTag(hodler)
        } else {
            hodler = convertView.tag as ViewHodler
        }
        hodler.mTextView!!.text = datas!![position]
        return convertView
    }

    private class ViewHodler {
        var mTextView: TextView? = null
    }
}
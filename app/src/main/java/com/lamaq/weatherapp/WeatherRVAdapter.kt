package com.lamaq.weatherapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat

class WeatherRVAdapter(private val context: Context, private val weatherRVModelArrayList: ArrayList<WeatherRVModel>): RecyclerView.Adapter<WeatherRVAdapter.ViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view:View = LayoutInflater.from(parent.context).inflate(R.layout.weather_rv_item,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model: WeatherRVModel = weatherRVModelArrayList[position]
        holder.windTV!!.text = model.windSpeed + "km/h"
        holder.tempTV!!.text = model.temperature + "°C"
        holder.timeTV!!.text = model.time
        Glide.with(context).load("https://${model.icon}").into(holder.conditionIV!!)
    }

    override fun getItemCount(): Int {
        return weatherRVModelArrayList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
         var windTV: TextView? = null
         var tempTV: TextView? = null
         var timeTV: TextView? = null
        var conditionIV: ImageView? = null
       init {
            windTV = itemView.findViewById(R.id.idTVWindSpeed)
            tempTV = itemView.findViewById(R.id.idTVTemperature)
            timeTV = itemView.findViewById(R.id.idTVTime)
            conditionIV = itemView.findViewById(R.id.idTVCondition)
        }
    }
}
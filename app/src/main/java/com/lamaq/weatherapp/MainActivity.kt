package com.lamaq.weatherapp

import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.JsonObject
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    private var homeRL: RelativeLayout? = null
    private var loadingPB: ProgressBar? = null
    private var cityNameTV: TextView? = null
    private var temperatureTV: TextView? = null
    private var conditionTV: TextView? = null
    private var weatherRV: RecyclerView? = null
    private var cityEdt: TextInputEditText? = null
    private var backIV: ImageView? = null
    private var iconIV: ImageView? = null
    private var searchIV: ImageView? = null

    private var weatherRVModelArrayList: ArrayList<WeatherRVModel>? = null
    private var weatherRVAdapter: WeatherRVAdapter? = null

    private var PERMISSION_CODE = 1
    private var cityName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        homeRL = findViewById(R.id.idRLHome)
        loadingPB = findViewById(R.id.idPBLoading)
        cityNameTV = findViewById(R.id.idTVCityName)
        temperatureTV = findViewById(R.id.idTVTemperature)
        conditionTV = findViewById(R.id.idTVCondition)
        weatherRV = findViewById(R.id.idRVWeather)
        cityEdt = findViewById(R.id.idEdtCity)
        backIV = findViewById(R.id.idIVBack)
        iconIV = findViewById(R.id.idIVIcon)
        searchIV = findViewById(R.id.idIVSearch)

        weatherRVModelArrayList = ArrayList()
        weatherRVAdapter = WeatherRVAdapter(this, weatherRVModelArrayList!!)
        weatherRV!!.adapter = weatherRVAdapter

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_CODE
            )
            return
        }
        val location : Location? = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (location != null) {
            cityName = getCityName(location.latitude, location.longitude)
        }
        cityName?.let { getWeatherDetails(it) }

        searchIV!!.setOnClickListener {
            cityName = cityEdt!!.text.toString()
            if (cityName!!.isNotEmpty()) {
                cityNameTV!!.text = cityName
                getWeatherDetails(cityName!!)
            }else{
                Toast.makeText(this, "Please enter city name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun getCityName (latitude: Double, longitude: Double): String? {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses: List<Address> = geocoder.getFromLocation(latitude, longitude, 10)
            for (address in addresses) {
                if (address.locality != null && address.locality.isNotEmpty()) {
                    cityName = address.locality
                }else{
                    Toast.makeText(this, "City not found", Toast.LENGTH_SHORT).show()
                }
            }
        }catch (e: Exception) {
            e.printStackTrace()
        }
        return cityName
    }

    private fun getWeatherDetails(cityName: String) {
        val url =
            "https://api.weatherapi.com/v1/forecast.json?key=179c6873c8c546b895c152355221407&q=${cityName}&days=1&aqi=no&alerts=no"
        cityNameTV!!.text = cityName

        val requestQueue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->

                loadingPB!!.visibility = View.GONE
                homeRL!!.visibility = View.VISIBLE
                val current = response.getJSONObject("current")
                val condition = current.getJSONObject("condition")
                val tempC = current.getString("temp_c")
                val icon = condition.getString("icon")
                val text = condition.getString("text")

                temperatureTV!!.text = "$tempC°c"
                conditionTV!!.text = text
                iconIV!!.setImageURI(Uri.parse("https:$icon"))

                val forecast = response.getJSONObject("forecast")
                val forecastday = forecast.getJSONArray("forecastday")
                val day = forecastday.getJSONObject(0)
                val dayArray = day.getJSONArray("hour")
                weatherRVModelArrayList!!.clear()
                for (i in 0 until dayArray.length()) {
                    val hour = dayArray.getJSONObject(i)
                    val time = hour.getString("time")
                    val tempC = hour.getString("temp_c")
                    val tempF = hour.getString("temp_f")
                    val windkph = hour.getString("wind_kph")
                    val condition = hour.getJSONObject("condition")
                    val icon = condition.getString("icon")
                    val text = condition.getString("text")

                    val weatherRVModel = WeatherRVModel(cityName, tempC, windkph, icon, time)
                    weatherRVModelArrayList!!.add(weatherRVModel)
                }
                weatherRVAdapter!!.notifyDataSetChanged()
                loadingPB!!.visibility = View.GONE
                homeRL!!.visibility = View.VISIBLE
            },
            { _ ->
                Toast.makeText(this, "Please enter valid city name..", Toast.LENGTH_SHORT).show()
            })
    }
}
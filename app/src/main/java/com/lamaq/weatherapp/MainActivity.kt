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
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
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

        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_layout)

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
        println(location)
        if (location != null) {
            cityName = getCityName(location.latitude, location.longitude)
        }else{
            cityName = "Mumbai"
        }
        cityName?.let { getWeatherDetails(it) }

        searchIV!!.setOnClickListener {
            cityName = cityEdt!!.text.toString()
            if (cityName!!.isNotEmpty()) {
                getWeatherDetails(cityName!!)
            }else{
                Toast.makeText(this, "Please enter city name", Toast.LENGTH_SHORT).show()
            }
        }

        cityEdt!!.setOnEditorActionListener { v, actionId, event ->
            cityName = cityEdt!!.text.toString()
            if(actionId == EditorInfo.IME_ACTION_DONE){
                if (cityName!!.isNotEmpty()) {
                    //hide keyboard
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                    getWeatherDetails(cityName!!)
                }else{
                    Toast.makeText(this, "Please enter city name", Toast.LENGTH_SHORT).show()
                }
            }
            true
        }

        swipeRefreshLayout.setOnRefreshListener {
            cityName?.let { it1 -> getWeatherDetails(it1) }
            swipeRefreshLayout.isRefreshing = false
        }

        weatherRV!!.addOnLayoutChangeListener(object : View.OnLayoutChangeListener{
            override fun onLayoutChange(
                v: View?,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val startingHour = 0
                val position = currentHour - startingHour
                weatherRV!!.post {
                    weatherRV!!.scrollToPosition(position)
                }
                weatherRV!!.removeOnLayoutChangeListener(this)
            }
        })

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
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
                    break
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

        //get weather details
        val queue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val location = response.getJSONObject("location")
                    val name = location.getString("name")
                    val region = location.getString("region")
                    val country = location.getString("country")
                    cityNameTV!!.text = "$name, $region, $country"
                    val current = response.getJSONObject("current")
                    val condition = current.getJSONObject("condition")
                    val tempC = current.getString("temp_c")
                    val icon = condition.getString("icon")
                    val text = condition.getString("text")

                    temperatureTV!!.text = "$tempC°C"
                    conditionTV!!.text = text
                    Glide.with(this).load("https://${icon}").into(iconIV!!)

                    val forecast = response.getJSONObject("forecast")
                    val forecastday = forecast.getJSONArray("forecastday")
                    val day = forecastday.getJSONObject(0)
                    val hour = day.getJSONArray("hour")

                    weatherRVModelArrayList!!.clear()
                    for (i in 0 until hour.length()) {
                        val jsonObject = hour.getJSONObject(i)
                        var time = jsonObject.getString("time")
                        time = time.substring(11, 16)
                        val tempC = jsonObject.getString("temp_c")
                        val condition = jsonObject.getJSONObject("condition")
                        val icon = condition.getString("icon")
                        val text = condition.getString("text")
                        val windKph = jsonObject.getString("wind_kph")

                        val weatherRVModel = WeatherRVModel(cityName, tempC, windKph, icon, time)
                        weatherRVModelArrayList!!.add(weatherRVModel)
                    }

                    weatherRVAdapter!!.notifyDataSetChanged()

                    homeRL!!.visibility = View.VISIBLE
                    loadingPB!!.visibility = View.GONE
                }catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error ->
                Toast.makeText(this, error.message, Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(jsonObjectRequest)
    }
}
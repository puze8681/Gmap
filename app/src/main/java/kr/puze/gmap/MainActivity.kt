package kr.puze.gmap

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.*
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_info_window.view.*
import java.net.URL

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object{
        lateinit var gMap: MapView
        lateinit var mMap: GoogleMap
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        gMap = map
        gMap.onCreate(savedInstanceState)
        gMap.getMapAsync(this)
        getData()
    }

    private fun getData(){
        val db: FirebaseFirestore = FirebaseFirestore.getInstance()
        db.collection("map")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        Log.d("LOGTAG",document.id + " => " + document.data)
                        var image = document.data["image"].toString()
                        var number = document.data["number"].toString()
                        var title = document.data["title"].toString()
                        var lng = document.data["lng"] as Double
                        var lat = document.data["lat"] as Double

                        setPin(image, number, title, lat, lng)
                    }
                } else {
                    Log.w("LOGTAG", "Error getting documents.", task.exception)
                }
            }
    }

    private fun setPin(path: String, number: String, title: String, lat: Double, lng: Double){
        val storage = FirebaseStorage.getInstance("gs://gmap-49df0.appspot.com")
        val storageRef = storage.reference
        val pathReference = storageRef.child(path)
        pathReference.downloadUrl
            .addOnSuccessListener {
                val downloadUri: Uri = it
                var url: String = downloadUri.toString()
                Log.d("LOGTAG", url)
                setMarker(lat, lng, title, number, url)
            }.addOnFailureListener {
                Log.d("LOGTAG", "addOnFailureListener")
            }.let {
                Log.d("LOGTAG", "let")
            }
    }

    //현재 위치 찍기
    private fun setMarker(lat: Double, lng: Double, title: String, number: String, image: String) {
        val url = URL(image)
        if(url != null){
            val bmp = getResponse(url)
            if(bmp != null){
                var smallBitmap = Bitmap.createScaledBitmap(bmp, 200, 180, false)
                val makerOptions = MarkerOptions()
                makerOptions
                    .position(LatLng(lat, lng))
                    .title(title)
                    .snippet(number)
                    .icon(BitmapDescriptorFactory.defaultMarker( BitmapDescriptorFactory.HUE_BLUE))

                val m: Marker = mMap.addMarker(makerOptions)
                m.tag = smallBitmap
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 15f))
            }else{
                Log.d("LOGTAG", "setMarker bmp is null")
            }
        }else{
            Log.d("LOGTAG", "setMarker url is null")
        }
    }

    private fun getResponse(url: URL): Bitmap? {
        return try {
            val asyncTask: AsyncTask<URL, Void?, Bitmap?> =
                object : AsyncTask<URL, Void?, Bitmap?>() {
                    override fun doInBackground(vararg params: URL): Bitmap? {
                        return BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    }
                }

            return asyncTask.execute(url).get()
        } catch (e: Exception) {
            null
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        MapsInitializer.initialize(this@MainActivity)
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = false
        mMap.uiSettings.isCompassEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = false
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15F))
        mMap.setOnMapClickListener {
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(37.52487, 126.92723)))

        mMap.setInfoWindowAdapter(object : InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): View? {
                return null
            }

            override fun getInfoContents(marker: Marker): View? {
                var view: View? = null
                try {
                    view = layoutInflater.inflate(R.layout.layout_info_window, null)
                    Log.d("LOGTAG", "marker = ${marker.title} , ${marker.snippet} , ${marker.tag}")
                    view.text_name.text = marker.title
                    view.text_number.text = marker.snippet
                    view.image.setImageBitmap(marker.tag as Bitmap?)
//                    Glide.with(this@MainActivity).load(marker.tag).into(view.image)
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                        view.image.clipToOutline = true
//                    }
                    Log.d("LOGTAG", "text_name = ${view.text_name.text}")
                    Log.d("LOGTAG", "text_number = ${view.text_number.text}")
                    Log.d("LOGTAG", "image = ${view.image.drawable}")
                } catch (e: Exception) {
                    print(e.message)
                }
                return view
            }
        })
        Log.d("LOGTAG", "onMapReady")
    }

    override fun onResume() {
        super.onResume()
        gMap.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        gMap.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        gMap.onLowMemory()
    }
}

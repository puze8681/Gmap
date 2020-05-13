package kr.puze.gmap

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_main.*
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
                        var number = document.data["number"] as Long
                        var lng = document.data["lng"] as Double
                        var lat = document.data["lat"] as Double

                        setPin(image, number, lat, lng)
                    }
                } else {
                    Log.w("LOGTAG", "Error getting documents.", task.exception)
                }
            }
    }

    private fun setPin(path: String, number: Long, lat: Double, lng: Double){
        val storage = FirebaseStorage.getInstance("gs://gmap-49df0.appspot.com")
        val storageRef = storage.reference
        val pathReference = storageRef.child(path)
        pathReference.downloadUrl
            .addOnSuccessListener {
                val downloadUri: Uri = it
                var url: String = downloadUri.toString()
                Log.d("LOGTAG", url)
                setMarker(lat, lng, number.toString(), url)
            }.addOnFailureListener {
                Log.d("LOGTAG", "addOnFailureListener")
            }.let {
                Log.d("LOGTAG", "let")
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

    //현재 위치 찍기
    private fun setMarker(lat: Double, lng: Double, placeName: String, image: String) {
        val url = URL(image)
        if(url != null){
            val bmp = getResponse(url)
            if(bmp != null){
                var smallBitmap = Bitmap.createScaledBitmap(bmp, 200, 180, false)
                val zoomLevel = 16.0f //This goes up to 21
                val makerOptions = MarkerOptions()
                makerOptions
                    .position(LatLng(lat, lng))
                    .title(placeName)
                    .icon(BitmapDescriptorFactory.fromBitmap(smallBitmap))
                mMap.addMarker(makerOptions)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), zoomLevel))
            }else{
                Log.d("LOGTAG", "setMarker bmp is null")
            }
        }else{
            Log.d("LOGTAG", "setMarker url is null")
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        MapsInitializer.initialize(this@MainActivity)
        mMap = googleMap

        mMap.uiSettings.isMyLocationButtonEnabled = false
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15F))
        mMap.setOnMapClickListener {
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(37.52487, 126.92723)))
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

package com.example.womensafety

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Example Hotspots (Replace with real data)
        val hotspotLocations = listOf(
            LatLng(28.6139, 77.2090),  // Example: Delhi
            LatLng(19.0760, 72.8777),  // Example: Mumbai
            LatLng(12.9716, 77.5946)   // Example: Bangalore
        )

        for (location in hotspotLocations) {
            mMap.addMarker(MarkerOptions().position(location).title("High-Risk Area"))
        }

        // Move camera to first hotspot
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hotspotLocations[0], 12f))
    }
}

package com.example.labtesttourplanner

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class DestinationFragment : Fragment(R.layout.fragment_destination) {

    companion object {
        private const val TAG = "DestinationFragment"
        private const val ARG_CATEGORY = "category"

        fun newInstance(category: String): DestinationFragment {
            val fragment = DestinationFragment()
            fragment.arguments = Bundle().apply { putString(ARG_CATEGORY, category) }
            return fragment
        }
    }

    interface OnAddToTripListener {
        fun onAddToTrip(category: String, activities: String)
    }

    private data class DestinationContent(
        val imageRes: Int,
        val accentColorRes: Int,
        val activities: String,
        val description: String
    )

    private var listener: OnAddToTripListener? = null
    private val category: String
        get() = arguments?.getString(ARG_CATEGORY) ?: "Adventure"

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(TAG, "onAttach")
        listener = context as? OnAddToTripListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: category=$category")

        val content = when (category) {
            "Heritage" -> DestinationContent(
                R.drawable.img_heritage,
                R.color.heritage_color,
                "Guided fort tours, heritage walks, museum visits",
                "Explore centuries-old forts, temples, and monuments with a local guide."
            )
            "Relaxation" -> DestinationContent(
                R.drawable.img_relaxation,
                R.color.relaxation_color,
                "Beach lounging, spa sessions, sunset cruises",
                "Unwind on quiet beaches and resorts with spa and wellness experiences."
            )
            else -> DestinationContent(
                R.drawable.img_adventure,
                R.color.adventure_color,
                "Trekking, white-water rafting, zip-lining",
                "Get your adrenaline pumping with treks, rafting, and outdoor adventure sports."
            )
        }
        val (imageRes, accentColorRes, activities, description) = content

        view.findViewById<ImageView>(R.id.ivDestinationImage).setImageResource(imageRes)
        view.findViewById<TextView>(R.id.tvCategoryLabel).text = category
        view.findViewById<TextView>(R.id.tvActivities).text = activities
        view.findViewById<TextView>(R.id.tvDescription).text = description

        val addButton = view.findViewById<Button>(R.id.btnAddToTrip)
        addButton.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), accentColorRes)
        addButton.setOnClickListener {
            listener?.onAddToTrip(category, activities)
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(TAG, "onDetach")
        listener = null
    }
}

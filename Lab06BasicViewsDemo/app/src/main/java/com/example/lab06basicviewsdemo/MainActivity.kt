package com.example.lab06basicviewsdemo

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etName = findViewById<EditText>(R.id.etName)
        val rgGender = findViewById<RadioGroup>(R.id.rgGender)
        val spCourse = findViewById<Spinner>(R.id.spCourse)
        val cbNewsletter = findViewById<CheckBox>(R.id.cbNewsletter)
        val sbExperience = findViewById<SeekBar>(R.id.sbExperience)
        val tvExperienceLabel = findViewById<TextView>(R.id.tvExperienceLabel)
        val tvResult = findViewById<TextView>(R.id.tvResult)

        // Populate the Spinner with the course list defined in strings.xml
        spCourse.adapter = ArrayAdapter.createFromResource(
            this,
            R.array.course_options,
            android.R.layout.simple_spinner_dropdown_item
        )
        spCourse.setSelection(1) // default to "MCA"

        sbExperience.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                tvExperienceLabel.text = "Experience: $progress yrs"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            val name = etName.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                tvResult.visibility = TextView.GONE
                return@setOnClickListener
            }

            val genderId = rgGender.checkedRadioButtonId
            val gender = findViewById<android.widget.RadioButton>(genderId).text.toString()
            val course = spCourse.selectedItem.toString()
            val subscribed = if (cbNewsletter.isChecked) "Yes" else "No"
            val experience = sbExperience.progress

            tvResult.text = "Name: $name\n" +
                "Gender: $gender\n" +
                "Course: $course\n" +
                "Newsletter: $subscribed\n" +
                "Experience: $experience yrs"
            tvResult.visibility = TextView.VISIBLE
        }
    }
}

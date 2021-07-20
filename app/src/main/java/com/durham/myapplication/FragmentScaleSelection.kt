package com.durham.myapplication

import android.content.res.ColorStateList
import android.database.CursorIndexOutOfBoundsException
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources.getColorStateList
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColor
import com.google.android.material.chip.Chip




class FragmentScaleSelection(cs:CurrentScale) : Fragment() {

    private lateinit var jsonSerializer: JSONSerializer

    val scaleNotes:ArrayList<Chip> = ArrayList()
    val scaleTypes:ArrayList<Chip> = ArrayList<Chip>()

    var currentScale:CurrentScale = cs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
//called after onCreate
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_scale_selection, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scaleTypes.add(view.findViewById<Chip>(R.id.chipIonian))
        scaleTypes.add(view.findViewById(R.id.chipAeolian))
        scaleTypes.add(view.findViewById<Chip>(R.id.chipPhrygian))
        scaleTypes.add(view.findViewById(R.id.chipDorian))
        scaleTypes.add(view.findViewById<Chip>(R.id.chipLydian))
        scaleTypes.add(view.findViewById(R.id.chipMixolydian))
        scaleTypes.add(view.findViewById<Chip>(R.id.chipLocrian))

        scaleNotes.add(view.findViewById(R.id.chipD))
        scaleNotes.add(view.findViewById(R.id.chipA))
        scaleNotes.add(view.findViewById(R.id.chipC))
        scaleNotes.add(view.findViewById(R.id.chipF))
        scaleNotes.add(view.findViewById(R.id.chipFSharp))
        scaleNotes.add(view.findViewById(R.id.chipAFlat))
        scaleNotes.add(view.findViewById(R.id.chipEFlat))
        scaleNotes.add(view.findViewById(R.id.chipB))
        scaleNotes.add(view.findViewById(R.id.chipBFlat))
        scaleNotes.add(view.findViewById(R.id.chipG))
        scaleNotes.add(view.findViewById(R.id.chipCSharp))
        scaleNotes.add(view.findViewById(R.id.chipE))


        for (note in scaleNotes){
            note.setOnClickListener {
                for (n in scaleNotes) {
                    if (n.text.toString() == note.text.toString()) {
                        currentScale.note = note.text.toString()
                        n.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#DC650A"))
                    }
                    else {
                        n.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#FAE5A6"))
                    }
                }
            }
            if (note.text.toString() == currentScale.note){
                note.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#DC650A"))
            } else{
                note.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#FAE5A6"))
            }
        }
        for (type in scaleTypes){
            type.setOnClickListener {
                for (t in scaleTypes) {
                    if (t.text.toString() == type.text.toString()) {
                        currentScale.type = type.text.toString()
                        t.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                    }
                    else {
                        t.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#91D8BC"))
                    }
                }
            }
            if (type.text.toString() == currentScale.type) {
                type.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
            } else {
                type.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#91D8BC"))
            }
        }

    }


}
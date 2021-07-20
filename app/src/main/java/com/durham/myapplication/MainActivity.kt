package com.durham.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.marginTop
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import org.w3c.dom.Text
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {

    val notes: ArrayList<MusicNote> = ArrayList<MusicNote>()

    var position:Int = 0
    var correct:Int = 0
    var startingNote:Int = 0
    val pattern:ArrayList<MusicNote> = ArrayList<MusicNote>()

    private lateinit var jsonSerializer: JSONSerializer
    private lateinit var currentScale:CurrentScale

    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                setContentView(R.layout.main_menu)
            } else {
                this@MainActivity.finish()
                exitProcess(0)
            }
        }

    //lateinit var tv1:TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        checkPermissions()
        getNotes()

        jsonSerializer = JSONSerializer("SCALEpro.json", applicationContext)

        try {
            currentScale = jsonSerializer.load()
        } catch (e:Exception){
            currentScale = CurrentScale()
            Log.e("Error loading previous scale: ", "", e)
        }




    }



    //Button Functions

    fun onAcceptClick(v:View){ //accept permissions button only shows if Record Audio Permissions are still required
       requestPermissions()
    }

    fun showTuner(v:View) {
        setContentView(R.layout.tuner)
        activateTuner()
    }

    fun showMenu(v:View) {
        setContentView(R.layout.main_menu)
    }

    fun showScaleTrainer(v:View) {
        setContentView(R.layout.scale_trainer_layout)
        val scaleSelect = FragmentScaleSelection(currentScale)
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment, scaleSelect)
            addToBackStack(null)
            commit()
        }
    }

    fun startScaleTrainer(v:View) {
        setContentView(R.layout.scale_trainer_layout)
        val scaleTrainActive = FragmentScaleActive(currentScale)
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment, scaleTrainActive)
            commit()
        }
        activatePitchChecker(scaleTrainActive)
    }

    fun exit(v:View) {
        this@MainActivity.finish()
        exitProcess(0)
    }


    //launches the permission request for the specific record_audio permission required
    fun requestPermissions() {

        when {
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                setContentView(R.layout.main_menu)

            }
            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.RECORD_AUDIO)
                if ( ContextCompat.checkSelfPermission(this,
                        Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED){
                }
            }
        }
    }


    //function that sets the content View ot main menu if the Record Audio Permissions are already granted
    fun checkPermissions() {
        when {
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                setContentView(R.layout.main_menu)

            }
            else -> {

            }
        }
    }





    //this function controls the audio functions for the instrument tuner any other pitch detection
    //will occur in independent fragments

    fun activateTuner() {
        val tvHz = findViewById<TextView>(R.id.tvHz)
        val ivHand = findViewById<ImageView>(R.id.ivHand)
        val tvNote = findViewById<TextView>(R.id.tvNote)
        var dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0)
        var pdh = PitchDetectionHandler { result, e ->
            val pitchInHz = result.pitch
            runOnUiThread {
                if (pitchInHz != -1.0f){
                    tvHz.text = "" + pitchInHz
                    if (ivHand.imageAlpha <= 230){
                        ivHand.imageAlpha += 25
                    }else ivHand.imageAlpha += 255 - ivHand.imageAlpha


                    for (note in notes) {
                        val variance:Float = note.pitch*.04f
                        val acceptedTuning:Float = note.pitch*.008f
                        println("variance = " + variance.toString())
                        if (pitchInHz >= note.pitch - variance && pitchInHz <= note.pitch +
                            variance){
                            tvNote.text = note.name
                            val difference = pitchInHz - note.pitch
                            ivHand.rotation = difference/variance * 50
                        }
                    }
                }else {
                    tvHz.text = ""
                    if (ivHand.imageAlpha >= 10){
                        ivHand.imageAlpha -= 10
                    }
                    else {
                        ivHand.imageAlpha -= ivHand.imageAlpha
                        if (ivHand.imageAlpha == 0) tvNote.text = ""
                    }
                }


            }
        }
        var p: AudioProcessor =
            PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050f, 1024, pdh)
        dispatcher.addAudioProcessor(p)
        Thread(dispatcher,"Audio Dispatcher").start()
    }

    fun setScale() {
        when (currentScale.note) {
            "E" ->
                startingNote = 52
            "B" ->
                startingNote = 47
            "F♯/G♭" ->
                startingNote = 42
            "C♯/D♭" ->
                startingNote = 49
            "C" ->
                startingNote = 48
            "G" ->
                startingNote = 43
            "D" ->
                startingNote = 50
            "A" ->
                startingNote = 45
            "A♭/G♯" ->
                startingNote = 44
            "E♭/D♯" ->
                startingNote = 51
            "B♭/A♯" ->
                startingNote = 46
        }
        when (currentScale.type) {
            "Ionian" ->
            {
                pattern.clear()
                for (i in 0..1){
                    startingNote = startingNote + 12 * i
                    pattern.add(notes[startingNote])
                    pattern.add(notes[startingNote+2])
                    pattern.add(notes[startingNote+4])
                    pattern.add(notes[startingNote+5])
                    pattern.add(notes[startingNote+7])
                    pattern.add(notes[startingNote+9])
                    pattern.add(notes[startingNote+11])
                }
                startingNote = startingNote + 12
                for (i in 0..1){
                    startingNote = startingNote - 12 * i
                    pattern.add(notes[startingNote])
                    pattern.add(notes[startingNote-1])
                    pattern.add(notes[startingNote-3])
                    pattern.add(notes[startingNote-5])
                    pattern.add(notes[startingNote-7])
                    pattern.add(notes[startingNote-8])
                    pattern.add(notes[startingNote-10])
                }
                startingNote = startingNote - 12

            }
            "Dorian" -> {
                pattern.clear()
                for(i in 0..1){
                    startingNote = startingNote + 12 * i
                    pattern.add(notes[startingNote])
                    pattern.add(notes[startingNote + 2])
                    pattern.add(notes[startingNote + 3])
                    pattern.add(notes[startingNote + 5])
                    pattern.add(notes[startingNote + 7])
                    pattern.add(notes[startingNote + 9])
                    pattern.add(notes[startingNote + 10])
                }
                startingNote += 12
                for(i in 0..1){
                    startingNote = startingNote - 12 * i
                    pattern.add(notes[startingNote])
                    pattern.add(notes[startingNote-2])
                    pattern.add(notes[startingNote-3])
                    pattern.add(notes[startingNote-5])
                    pattern.add(notes[startingNote-7])
                    pattern.add(notes[startingNote-9])
                    pattern.add(notes[startingNote-10])
                }
                startingNote -= 12
            }
            "Phrygian" -> {
                pattern.clear()
                for (i in 0..1){
                    startingNote = startingNote + 12 * i
                    pattern.add(notes[startingNote])
                    pattern.add(notes[startingNote+1])
                    pattern.add(notes[startingNote+2])
                    pattern.add(notes[startingNote+4])
                    pattern.add(notes[startingNote+6])
                    pattern.add(notes[startingNote+7])
                    pattern.add(notes[startingNote+9])
                }
                startingNote += 12
                for (i in 0..1){
                    startingNote = startingNote - 12 * i
                    pattern.add(notes[startingNote])
                    pattern.add(notes[startingNote-3])
                    pattern.add(notes[startingNote-5])
                    pattern.add(notes[startingNote-6])
                    pattern.add(notes[startingNote-8])
                    pattern.add(notes[startingNote-10])
                    pattern.add(notes[startingNote-11])
                }
                startingNote -= 12
            }
            "Lydian" -> {
                pattern.clear()
                for (i in 0..1){
                    startingNote = startingNote + 12 * i
                    pattern.add(notes[startingNote])
                    pattern.add(notes[startingNote+2])
                    pattern.add(notes[startingNote+3])
                    pattern.add(notes[startingNote+5])
                    pattern.add(notes[startingNote+6])
                    pattern.add(notes[startingNote+8])
                    pattern.add(notes[startingNote+10])
                }
                startingNote += 12
                for (i in 0..1){
                    startingNote = startingNote - 12 * i
                    pattern.add(notes[startingNote])
                    pattern.add(notes[startingNote-2])
                    pattern.add(notes[startingNote-4])
                    pattern.add(notes[startingNote-6])
                    pattern.add(notes[startingNote-7])
                    pattern.add(notes[startingNote-9])
                    pattern.add(notes[startingNote-10])
                }
                startingNote -= 12
            }
            "Mixolydian" -> {
                pattern.clear()
                for (i in 0..1){
                    startingNote = startingNote + 12 * i
                    pattern.add(notes[startingNote])
                    pattern.add(notes[startingNote+2])
                    pattern.add(notes[startingNote+4])
                    pattern.add(notes[startingNote+5])
                    pattern.add(notes[startingNote+7])
                    pattern.add(notes[startingNote+9])
                    pattern.add(notes[startingNote+10])
                }
                startingNote += 12
                for (i in 0..1){
                    startingNote = startingNote - 12 * i
                    pattern.add(notes[startingNote])
                    pattern.add(notes[startingNote-2])
                    pattern.add(notes[startingNote-3])
                    pattern.add(notes[startingNote-5])
                    pattern.add(notes[startingNote-7])
                    pattern.add(notes[startingNote-8])
                    pattern.add(notes[startingNote-10])
                }
                startingNote -= 12
            }
            "Aeolian" -> {
                pattern.clear()
                for (i in 0..1){
                    startingNote = startingNote + 12 * i
                    pattern.add(notes[startingNote])
                    pattern.add(notes[startingNote+2])
                    pattern.add(notes[startingNote+3])
                    pattern.add(notes[startingNote+5])
                    pattern.add(notes[startingNote+7])
                    pattern.add(notes[startingNote+8])
                    pattern.add(notes[startingNote+10])
                }
                startingNote += 12
                for (i in 0..1){
                    startingNote = startingNote - 12 * i
                    pattern.add(notes[startingNote])
                    pattern.add(notes[startingNote-2])
                    pattern.add(notes[startingNote-4])
                    pattern.add(notes[startingNote-5])
                    pattern.add(notes[startingNote-7])
                    pattern.add(notes[startingNote-9])
                    pattern.add(notes[startingNote-10])
                }
                startingNote -= 12
            }
            "Locrian" -> {
                pattern.clear()
                for (i in 0..1){
                    startingNote = startingNote + 12 * i
                    pattern.add(notes[startingNote])
                    pattern.add(notes[startingNote+1])
                    pattern.add(notes[startingNote+3])
                    pattern.add(notes[startingNote+5])
                    pattern.add(notes[startingNote+6])
                    pattern.add(notes[startingNote+8])
                    pattern.add(notes[startingNote+10])
                }
                startingNote += 12
                for (i in 0..1){
                    startingNote = startingNote - 12 * i
                    pattern.add(notes[startingNote])
                    pattern.add(notes[startingNote-2])
                    pattern.add(notes[startingNote-4])
                    pattern.add(notes[startingNote-6])
                    pattern.add(notes[startingNote-7])
                    pattern.add(notes[startingNote-9])
                    pattern.add(notes[startingNote-11])
                }
                startingNote -= 12
            }

        }
    }
    fun activatePitchChecker(sta:FragmentScaleActive) {
        position = 0
        correct = 0
        setScale()


        var dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0)

        var pdh = PitchDetectionHandler { result, e ->
            var tvBar: TextView
            if (sta.view?.findViewById<TextView>(R.id.tvScaleTrainerBar) != null) {
                tvBar = sta.requireView().findViewById<TextView>(R.id.tvScaleTrainerBar)
                val pitchInHz = result.pitch
                val acceptedTuning: Float = pattern[position].pitch * .008f
                val variance: Float = pattern[position].pitch * .04f
                runOnUiThread {
                    if (pattern.count() > 0) sta.view?.findViewById<TextView>(R.id.tvNextNote)?.text =
                        pattern[position].name
                    if (pitchInHz != -1.0f) {

                        if (position > 0) {
                            val previousVariance = pattern[position - 1].pitch * .04f
                            if (pitchInHz >= pattern[position - 1].pitch - previousVariance &&
                                pitchInHz <= pattern[position - 1].pitch + previousVariance) {
                                tvBar.translationY =
                                    98 + (pitchInHz - pattern[position-1].pitch)/previousVariance*98
                                if (tvBar.alpha <= .9f
                                ) {
                                    tvBar.alpha += .1f
                                } else if (tvBar.alpha < 1f
                                ) {
                                    tvBar.alpha = 1.0f
                                }
                            } else if (pitchInHz >= pattern[position].pitch - variance &&
                                       pitchInHz <= pattern[position].pitch + variance) {
                                tvBar.translationY =
                                    98 + (pitchInHz - pattern[position].pitch)/variance*98
                                if (tvBar.alpha <= .9f) {
                                    tvBar.alpha += .1f
                                } else if (tvBar.alpha < 1f) {
                                    tvBar.alpha = 1.0f
                                }
                                if (pitchInHz >= pattern[position].pitch - acceptedTuning &&
                                    pitchInHz <= pattern[position].pitch + acceptedTuning){
                                    position++
                                    correct++
                                    if (position == pattern.count()) position = 0
                                }
                            }
                        } else {
                            val previousVariance = pattern.last().pitch * .04f
                            if (pitchInHz >= pattern.last().pitch - previousVariance &&
                                pitchInHz <= pattern.last().pitch + previousVariance) {

                            } else if (pitchInHz >= pattern[position].pitch - variance && pitchInHz
                                <= pattern[position].pitch + variance) {
                                tvBar.translationY =
                                    98 + (pitchInHz - pattern[position].pitch)/variance*98
                                if (tvBar.alpha <= .9f) {
                                    tvBar.alpha += .1f
                                } else if (tvBar.alpha < 1f) {
                                    tvBar.alpha = 1.0f
                                }
                                if (pitchInHz >= pattern[position].pitch - acceptedTuning &&
                                    pitchInHz <= pattern[position].pitch + acceptedTuning){
                                    position++
                                    correct++
                                    if (position == pattern.count()) position = 0
                                }

                            }

                        }


                    } else {
                        tvBar.translationY = 196f
                        if (tvBar.alpha >= .05f) {
                            tvBar.alpha -= .05f
                        } else if (tvBar.alpha > 0f) {
                            tvBar.alpha = 0f
                        }
                    }
                }
            } else {
                Thread("Audio Dispatcher").interrupt()
            }
        }



        var p: AudioProcessor =
            PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050f, 1024, pdh)
        dispatcher.addAudioProcessor(p)
        Thread(dispatcher,"Audio Dispatcher").start()
    }

    //populate the list of music notes and their corresponding pitches

    fun getNotes(){
        val noteText: InputStream = this.resources.openRawResource(R.raw.notes)
        var parts = listOf<String>("","","")
        val reader = BufferedReader(InputStreamReader(noteText))
        var myLine: String = ""
        try {
            reader.forEachLine {
                parts = it.split('\t')
                notes.add(MusicNote(parts[0], parts[1].toFloat()))
            }
        }catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        saveCurrentScale(currentScale)
    }

    fun saveCurrentScale(cs:CurrentScale) {
        try {
            jsonSerializer.save(cs)
        }catch (e:Exception){
            Log.e("Error Saving Scale", "", e)
        }
    }

    //    fun activatePitchDetect() {
//        var dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0)
//        var pdh = PitchDetectionHandler { result, e ->
//            val pitchInHz = result.pitch
//            runOnUiThread {
//
//                tv1.text = "" + pitchInHz
//            }
//        }
//        var p: AudioProcessor =
//            PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050f, 1024, pdh)
//        dispatcher.addAudioProcessor(p)
//        Thread(dispatcher,"Audio Dispatcher").start()
//    }

}
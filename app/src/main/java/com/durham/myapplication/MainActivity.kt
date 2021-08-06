package com.durham.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
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
    var total:Int = 0
    var startingNote:Int = 0
    val pattern:ArrayList<MusicNote> = ArrayList<MusicNote>()

    val samples:ArrayList<Float> = ArrayList<Float>()
    val playedList:ArrayList<Float> = ArrayList<Float>()


    var checkNote:Boolean = false
    var checkCorrect:Boolean = false

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
                if (pitchInHz != -1.0f && result.probability >= 0.91){
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
        var instrumentStart = -1 //-1 for guitar 0 for violin
        when (currentScale.note) {


            "F♯/G♭" ->
                startingNote = 42
            "G" ->
                startingNote = 43
            "A♭/G♯" ->
                startingNote = 44
            "A" ->
                startingNote = 45
            "B♭/A♯" ->
                startingNote = 46
            "B" ->
                startingNote = 47
            "C" ->
                startingNote = 48
            "C♯/D♭" ->
                startingNote = 49
            "D" ->
                startingNote = 50
            "E♭/D♯" ->
                startingNote = 51
            "E" ->{
                if (instrumentStart == -1) startingNote = 40

                else startingNote = 52
            }

            "F" ->
                if (instrumentStart == -1) startingNote = 41

                else startingNote = 53
        }

        when (currentScale.type) {
            "Ionian" ->
            {
                pattern.clear()
                for (i in instrumentStart..instrumentStart + 1){
                    val stN:Int = startingNote + 12 * i
                    pattern.add(notes[stN])
                    pattern.add(notes[stN+2])
                    pattern.add(notes[stN+4])
                    pattern.add(notes[stN+5])
                    pattern.add(notes[stN+7])
                    pattern.add(notes[stN+9])
                    pattern.add(notes[stN+11])
                }
                for (i in instrumentStart..instrumentStart + 1){
                    val stN = startingNote - 12 * i
                    pattern.add(notes[stN])
                    pattern.add(notes[stN-1])
                    pattern.add(notes[stN-3])
                    pattern.add(notes[stN-5])
                    pattern.add(notes[stN-7])
                    pattern.add(notes[stN-8])
                    pattern.add(notes[stN-10])
                }

            }
            "Dorian" -> {
                pattern.clear()
                for(i in instrumentStart..instrumentStart + 1){
                    val stN = startingNote + 12 * i
                    pattern.add(notes[stN])
                    pattern.add(notes[stN + 2])
                    pattern.add(notes[stN + 3])
                    pattern.add(notes[stN + 5])
                    pattern.add(notes[stN + 7])
                    pattern.add(notes[stN + 9])
                    pattern.add(notes[stN + 10])
                }

                for(i in instrumentStart..instrumentStart + 1){
                    val stN = startingNote - 12 * i
                    pattern.add(notes[stN])
                    pattern.add(notes[stN-2])
                    pattern.add(notes[stN-3])
                    pattern.add(notes[stN-5])
                    pattern.add(notes[stN-7])
                    pattern.add(notes[stN-9])
                    pattern.add(notes[stN-10])
                }

            }
            "Phrygian" -> {
                pattern.clear()
                for (i in instrumentStart..instrumentStart + 1){
                    val stN = startingNote + 12 * i
                    pattern.add(notes[stN])
                    pattern.add(notes[stN+1])
                    pattern.add(notes[stN+3])
                    pattern.add(notes[stN+5])
                    pattern.add(notes[stN+7])
                    pattern.add(notes[stN+8])
                    pattern.add(notes[stN+10])
                }
                for (i in instrumentStart..instrumentStart + 1){
                    val stN = startingNote - 12 * i
                    pattern.add(notes[stN])
                    pattern.add(notes[stN-2])
                    pattern.add(notes[stN-4])
                    pattern.add(notes[stN-5])
                    pattern.add(notes[stN-7])
                    pattern.add(notes[stN-9])
                    pattern.add(notes[stN-11])
                }
            }
            "Lydian" -> {
                pattern.clear()
                for (i in instrumentStart..instrumentStart + 1){
                    val stN = startingNote + 12 * i
                    pattern.add(notes[stN])
                    pattern.add(notes[stN+2])
                    pattern.add(notes[stN+4])
                    pattern.add(notes[stN+6])
                    pattern.add(notes[stN+7])
                    pattern.add(notes[stN+9])
                    pattern.add(notes[stN+11])
                }
                for (i in instrumentStart..instrumentStart + 1) {
                    val stN = startingNote - 12 * i
                    pattern.add(notes[stN])
                    pattern.add(notes[stN - 1])
                    pattern.add(notes[stN - 3])
                    pattern.add(notes[stN - 5])
                    pattern.add(notes[stN - 6])
                    pattern.add(notes[stN - 8])
                    pattern.add(notes[stN - 10])
                }
            }
            "Mixolydian" -> {
                pattern.clear()
                for (i in instrumentStart..instrumentStart + 1){
                    val stN = startingNote + 12 * i
                    pattern.add(notes[stN])
                    pattern.add(notes[stN+2])
                    pattern.add(notes[stN+4])
                    pattern.add(notes[stN+5])
                    pattern.add(notes[stN+7])
                    pattern.add(notes[stN+9])
                    pattern.add(notes[stN+10])
                }
                for (i in instrumentStart..instrumentStart + 1){
                    val stN = startingNote - 12 * i
                    pattern.add(notes[stN])
                    pattern.add(notes[stN-2])
                    pattern.add(notes[stN-3])
                    pattern.add(notes[stN-5])
                    pattern.add(notes[stN-7])
                    pattern.add(notes[stN-8])
                    pattern.add(notes[stN-10])
                }
            }
            "Aeolian" -> {
                pattern.clear()
                for (i in instrumentStart..instrumentStart + 1){
                    val stN = startingNote + 12 * i
                    pattern.add(notes[stN])
                    pattern.add(notes[stN+2])
                    pattern.add(notes[stN+3])
                    pattern.add(notes[stN+5])
                    pattern.add(notes[stN+7])
                    pattern.add(notes[stN+8])
                    pattern.add(notes[stN+10])
                }
                for (i in instrumentStart..instrumentStart + 1){
                    val stN = startingNote - 12 * i
                    pattern.add(notes[stN])
                    pattern.add(notes[stN-2])
                    pattern.add(notes[stN-4])
                    pattern.add(notes[stN-5])
                    pattern.add(notes[stN-7])
                    pattern.add(notes[stN-9])
                    pattern.add(notes[stN-10])
                }
            }
            "Locrian" -> {
                pattern.clear()
                for (i in instrumentStart..instrumentStart + 1){
                    val stN = startingNote + 12 * i
                    pattern.add(notes[stN])
                    pattern.add(notes[stN+1])
                    pattern.add(notes[stN+3])
                    pattern.add(notes[stN+5])
                    pattern.add(notes[stN+6])
                    pattern.add(notes[stN+8])
                    pattern.add(notes[stN+10])
                }
                for (i in instrumentStart..instrumentStart + 1){
                    val stN = startingNote - 12 * i
                    pattern.add(notes[stN])
                    pattern.add(notes[stN-2])
                    pattern.add(notes[stN-4])
                    pattern.add(notes[stN-6])
                    pattern.add(notes[stN-7])
                    pattern.add(notes[stN-9])
                    pattern.add(notes[stN-11])
                }
            }

        }
    }
    //
    fun activatePitchChecker(sta:FragmentScaleActive) {
        position = 0
        correct = 0
        total = 0
        samples.clear()
        setScale()


        var dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0)

        var pdh = PitchDetectionHandler { result, e ->

            if (sta.view?.findViewById<TextView>(R.id.tvScaleTrainerBar) != null) {
                val tvBar = sta.requireView().findViewById<TextView>(R.id.tvScaleTrainerBar)
                val pitchInHz = result.pitch

                val acceptedTuning: Float = pattern[position].pitch * .008f
                val variance: Float = pattern[position].pitch * .04f
                runOnUiThread {
                    sta.changeScore(total, correct)
                    if (pattern.count() > 0) sta.view?.findViewById<TextView>(R.id.tvNextNote)?.text =
                        pattern[position].name
                    if (pitchInHz != -1.0f) {

                        //if not first position in pattern then check to see if previous position
                        // is playing
                        if (position > 0) {
                            val previousVariance = pattern[position - 1].pitch * .04f //formula for acceptable variance
                            if (pitchInHz >= pattern[position - 1].pitch - previousVariance &&
                                pitchInHz <= pattern[position - 1].pitch + previousVariance) {
                                tvBar.translationY =
                                     (pitchInHz - pattern[position-1].pitch)/previousVariance*-98
                                tvBar.background = getDrawable(R.drawable.gradient_tuner_yellow)
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
                                     (pitchInHz - pattern[position].pitch)/variance*-98
                                tvBar.background = getDrawable(R.drawable.gradient_tuner_blue)
                                if (tvBar.alpha <= .9f) {
                                    tvBar.alpha += .1f
                                } else if (tvBar.alpha < 1f) {
                                    tvBar.alpha = 1.0f
                                }
                                if (pitchInHz >= pattern[position].pitch - acceptedTuning &&
                                    pitchInHz <= pattern[position].pitch + acceptedTuning){
                                    tvBar.background = getDrawable(R.drawable.gradient_tuner_green)

                                    if (position == pattern.count()) position = 0
                                }
                            }else if (pitchInHz < pattern[position].pitch - variance) {
                                if (tvBar.alpha <= .9f) {
                                    tvBar.alpha += .1f
                                } else if (tvBar.alpha < 1f) {
                                    tvBar.alpha = 1.0f
                                }
                                tvBar.background = getDrawable(R.drawable.gradient_tuner)
                                tvBar.translationY = 98f
                            }else if (pitchInHz > pattern[position].pitch + variance) {
                                if (tvBar.alpha <= .9f) {
                                    tvBar.alpha += .1f
                                } else if (tvBar.alpha < 1f) {
                                    tvBar.alpha = 1.0f
                                }
                                tvBar.background = getDrawable(R.drawable.gradient_tuner)
                                tvBar.translationY = -98f
                            }
                        } else { //if this is the first pitch in the pattern then check the last pitch
                            val previousVariance = pattern.last().pitch * .04f
                            if (pitchInHz >= pattern.last().pitch - previousVariance &&
                                pitchInHz <= pattern.last().pitch + previousVariance) {
                                tvBar.translationY =
                                    (pitchInHz - pattern.last().pitch)/previousVariance*-98
                                if (checkNote && checkCorrect) {

                                }else {
                                    tvBar.background = getDrawable(R.drawable.gradient_tuner_yellow)
                                }

                            } else if (pitchInHz >= pattern[position].pitch - variance && pitchInHz
                                <= pattern[position].pitch + variance) {
                                tvBar.translationY =
                                    (pitchInHz - pattern[position].pitch)/variance*-98
                                if (checkNote && checkCorrect) {

                                }else {
                                    tvBar.background = getDrawable(R.drawable.gradient_tuner_blue)
                                }

                                if (tvBar.alpha <= .9f) {
                                    tvBar.alpha += .1f
                                } else if (tvBar.alpha < 1f) {
                                    tvBar.alpha = 1.0f
                                }
                                if (pitchInHz >= pattern[position].pitch - acceptedTuning &&
                                    pitchInHz <= pattern[position].pitch + acceptedTuning){
                                    tvBar.background = getDrawable(R.drawable.gradient_tuner_green)

                                    checkCorrect = true

                                }

                            } else if (pitchInHz < pattern[position].pitch - variance) {
                                if (tvBar.alpha <= .9f) {
                                    tvBar.alpha += .1f
                                } else if (tvBar.alpha < 1f) {
                                    tvBar.alpha = 1.0f
                                }
                                tvBar.background = getDrawable(R.drawable.gradient_tuner)
                                tvBar.translationY = 98f
                            }else if (pitchInHz > pattern[position].pitch + variance) {
                                if (tvBar.alpha <= .9f) {
                                    tvBar.alpha += .1f
                                } else if (tvBar.alpha < 1f) {
                                    tvBar.alpha = 1.0f
                                }
                                tvBar.background = getDrawable(R.drawable.gradient_tuner)
                                tvBar.translationY = -98f
                            }

                        }
                        if (checkNote != true){
                            checkNote = true

                            object : CountDownTimer(300, 50) {

                                override fun onTick(millisUntilFinished: Long) {

                                }

                                override fun onFinish() {

                                    checkNote = false
                                    checkCorrect = false

                                    if (samples.count() > 10){
                                        if (position != 0){
                                            if (samples.average() >= pattern[position - 1].pitch
                                                - pattern[position - 1].pitch * .04

                                                && samples.average() <= pattern[position - 1].pitch
                                                + pattern[position - 1].pitch * .04){

                                            }else if (samples.average().toFloat() >= playedList.last() - playedList.last() * .04
                                                && samples.average().toFloat() <= playedList.last() + playedList.last() * .04 ){

                                            }
                                            else{
                                                playedList.add(samples.average().toFloat())
                                                total++
                                            }
                                        } else {
                                            if (samples.average() >= pattern.last().pitch
                                                - pattern.last().pitch * .04

                                                && samples.average() <= pattern.last().pitch
                                                + pattern.last().pitch * .04){

                                            }else if (samples.average().toFloat() >= playedList.last() - playedList.last() * .04
                                                && samples.average().toFloat() <= playedList.last() + playedList.last() * .04 ){

                                            }
                                            else {
                                                playedList.add(samples.average().toFloat())
                                                total++
                                            }
                                        }
                                        if (samples.average() >= pattern[position].pitch
                                            - acceptedTuning

                                            && samples.average() <= pattern[position].pitch
                                            + acceptedTuning) {
                                            position++
                                            correct++
                                            if (position == pattern.count()) position = 0
                                        }

                                    }
                                    samples.clear()
                                }
                            }.start()
                        }else {
                            if (samples.count() <= 12){
                                samples.add(pitchInHz)
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
                runOnUiThread{

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
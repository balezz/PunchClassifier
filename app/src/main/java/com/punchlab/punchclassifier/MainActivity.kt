package com.punchlab.punchclassifier

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    lateinit var mTextView: TextView
    lateinit var mDecoder: DecoderAsync
    var uiHandler: Handler = Handler(Looper.getMainLooper())

    private var resultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK){
                    val data = result.data?.data
                    Log.d(TAG, "Load data: $data")
                    mTextView.text = "Video $data is loading."
                    mDecoder.processVideo(data!!)
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        mDecoder = DecoderAsync(this, uiHandler) { onVideoProcessFinish() }

        mTextView = findViewById(R.id.textview)
        mTextView.setOnClickListener{
            if (mDecoder.personList.isNotEmpty()){
                mTextView.setText("Done!")
                Log.d(TAG, "List size: ${mDecoder.personList.size}")
            }
        }

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 3)
            resultLauncher.launch(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onVideoProcessFinish() {
        mTextView.text = "Punches: ${mDecoder.punchIdxList.joinToString(separator = ", ")}"
    }
}
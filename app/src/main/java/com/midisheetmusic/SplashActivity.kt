package com.midisheetmusic

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.midisheetmusic.sheets.ClefSymbol

/**
 * An activity to be shown when starting the app.
 * It handles checking for the required permissions and preloading the images.
 */
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadImages()
        startActivity()
    }

    /** Check for required permissions and start ChooseSongActivity  */
    private fun startActivity() {
        // Check if we have WRITE_EXTERNAL_STORAGE permission
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE_EXT_STORAGE_)
            return
        }
        val intent = Intent(this, ChooseSongActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE_EXT_STORAGE_ -> {
                if (grantResults.size > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted
                    startActivity()
                } else {
                    // permission denied
                    Snackbar.make(findViewById(android.R.id.content),
                            R.string.msg_permission_denied, Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.msg_permission_denied_retry) { view -> startActivity() }
                            .show()
                }
            }
        }
    }

    /** Load all the resource images  */
    private fun loadImages() {
        ClefSymbol.LoadImages(this)
        TimeSigSymbol.LoadImages(this)
    }

    /** Always use landscape mode for this activity.  */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE_EXT_STORAGE_ = 724
    }
}
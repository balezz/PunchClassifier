package com.punchlab.punchclassifier.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.punchlab.punchclassifier.R
import java.lang.IllegalStateException

class VideoProcessingDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder
                .setMessage(R.string.processing_message)
                .setPositiveButton(R.string.cancel_work,
                    DialogInterface.OnClickListener{
                        dialog, which ->

                    })
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
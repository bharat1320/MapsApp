package com.project.mapsapp.ui.util

import android.app.AlertDialog


fun AlertDialog.Builder.customExtensionDialog(title :String,
                                              positiveText :String = "Yes",
                                              negativeText :String = "No",
                                              acceptBlock: () -> Unit,
                                              deniedBlock: (() -> Unit)? = null,
): AlertDialog.Builder{
    val builder: AlertDialog.Builder = AlertDialog.Builder(context)
    builder.setMessage(title)
    builder.setCancelable(true)
    builder.setPositiveButton(positiveText) { dialog, id ->
        acceptBlock()
        dialog.cancel()
    }
    builder.setNegativeButton(negativeText) { dialog, id ->
        if(deniedBlock != null) deniedBlock()
        dialog.cancel()
    }
    val alert: AlertDialog = builder.create()
    alert.show()
    return this
}



package com.rork.ghostdetectorspiritbox.ui.text

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val REPORT_DATE = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.US)

/** Masthead date of a field report, in the instrument's own uppercase style. */
fun formatRecordDate(epochMillis: Long): String =
    REPORT_DATE.format(Date(epochMillis)).uppercase(Locale.US)

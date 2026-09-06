package com.example.vehiclemaintenance.backup

import java.time.LocalDate

/** The name offered to the system picker; the user is free to change it. */
fun backupFileName(today: LocalDate): String = "vehicle-maintenance-$today.json"

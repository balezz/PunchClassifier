package com.punchlab.punchclassifier

import android.app.Application
import com.punchlab.punchclassifier.database.PersonPunchDatabase

class PunchApplication : Application() {
    val database: PersonPunchDatabase by lazy { PersonPunchDatabase.getDatabase(this) }
}
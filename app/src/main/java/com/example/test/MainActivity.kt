package com.example.test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.example.test.data.repository.MoodleRepository
import com.example.test.ui.MoodleApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = MoodleRepository(applicationContext)
        setContent {
            MaterialTheme {
                MoodleApp(repository)
            }
        }
    }
}

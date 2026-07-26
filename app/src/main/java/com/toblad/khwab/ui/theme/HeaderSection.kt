package com.toblad.khwab.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HeaderSection() {

    var currentTime by remember {
        mutableStateOf(Date())
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000)
        }
    }

    val calendar = Calendar.getInstance().apply {
        time = currentTime
    }

    val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }

    val date = SimpleDateFormat(
        "dd MMMM yyyy",
        Locale.getDefault()
    ).format(currentTime)

    val time = SimpleDateFormat(
        "hh:mm:ss a",
        Locale.getDefault()
    ).format(currentTime)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {

        Text(
            text = "$greeting, Mr. Bhushan",
            color = KhwabWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Column(
            horizontalAlignment = Alignment.End
        ) {

            Text(
                text = date,
                color = KhwabWhite,
                fontSize = 14.sp
            )

            Text(
                text = time,
                color = KhwabWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

        }

    }
}
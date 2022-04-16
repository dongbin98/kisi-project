package com.dbsh.calendarapp

import android.graphics.Color
import android.text.style.ForegroundColorSpan
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import java.util.*

// 토요일 색상표시
class SaturdayDecorator : DayViewDecorator {
    private val calendar: Calendar = Calendar.getInstance()

    override fun shouldDecorate(day: CalendarDay): Boolean {
        day.copyTo(calendar)
        var weekDay: Int = calendar.get(Calendar.DAY_OF_WEEK)
        return weekDay == Calendar.SATURDAY
    }
    override fun decorate(view: DayViewFacade) {
        view.addSpan(ForegroundColorSpan(Color.BLUE))
    }
}
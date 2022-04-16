package com.dbsh.calendarapp

import android.graphics.Color
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import java.util.HashSet

// 일정있는 날 점 표시
class TododayDecorator(dates: Collection<CalendarDay>) : DayViewDecorator {
    var dates: HashSet<CalendarDay> = HashSet(dates)

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return dates.contains(day)
    }
    override fun decorate(view: DayViewFacade) {
        view.addSpan(DotSpan(5f, Color.rgb(0, 127, 0)))
    }
}
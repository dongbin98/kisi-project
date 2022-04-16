package com.dbsh.calendarapp

import android.content.DialogInterface
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.database.*
import com.prolificinteractive.materialcalendarview.*
import com.prolificinteractive.materialcalendarview.format.TitleFormatter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

// 전역변수 선언
// 리스트뷰와 라디오박스를 연결해줄 변수(순서번호)
var selectedItem: Int = 0
// 파이어베이스 참조 위치
private val database by lazy { FirebaseDatabase.getInstance() }
private val todoRef = database.getReference("user_data").child("todo_list")
private val noticeRef = database.getReference("user_data").child("notice")
// 일정 있는 날 담을 리스트
lateinit var datelist: ArrayList<CalendarDay>
// 일정 변경 notification 변수
var change = false

class MainActivity : AppCompatActivity() {
    // 커스텀 리스트뷰 아이템
    // 위젯 늦은 초기화 선언
    lateinit var calendar: MaterialCalendarView
    lateinit var todolist: TextView
    lateinit var noticeBtn: Button
    lateinit var updateBtn: Button
    lateinit var deleteBtn: Button
    // 날짜정보를 문자열로 받음
    var date: String = ""
    // 리스트뷰 객체 (커스텀)
    lateinit var datalist: ArrayList<String>
    lateinit var timelist: ArrayList<String>
    lateinit var importancelist: ArrayList<String>
    lateinit var adapter: ListAdapter

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // DB 실시간성 보장
        todoRef.keepSynced(true)
        noticeRef.keepSynced(true)
        // 리스트뷰 위젯 할당
        datalist = ArrayList<String>()
        timelist = ArrayList<String>()
        importancelist = ArrayList<String>()
        adapter = ListAdapter(this@MainActivity, datalist, timelist, importancelist)
        datelist = ArrayList<CalendarDay>()
        // 위젯 할당
        noticeBtn = findViewById<Button>(R.id.notice)
        calendar = findViewById<MaterialCalendarView>(R.id.calendar)
        todolist = findViewById<TextView>(R.id.todolist)
        updateBtn = findViewById<Button>(R.id.updateBtn)
        deleteBtn = findViewById<Button>(R.id.deleteAllBtn)
        // todolist 에디트텍스트 비활성화
        todolist.isEnabled = false
        // 강제로 라이트모드 설정
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        // 어플 시작 시 오늘 날짜를 받아오기
        var today = CalendarDay.today()
        date = String.format(
            "%04d-%02d-%02d",
            today.year,
            today.month + 1,
            today.day
        )
        updateTodo()
        // 데이터베이스 변경 시 정보갱신 리스너할당
        todoRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Handler().postDelayed({
                    todoRef.get().addOnSuccessListener {
                        var isInclude = false
                        // 일정 있는 날 표시 (모든 데코레이터를 지우고, 새로 데코)
                        calendar.removeDecorators()
                        calendar.addDecorator(SaturdayDecorator())
                        calendar.addDecorator(SundayDecorator())
                        updateSpan()
                        // 리스트뷰를 초기화
                        datalist.clear()
                        timelist.clear()
                        importancelist.clear()
                        adapter.notifyDataSetChanged()
                        if (it.hasChild(date)) {
                            // 리스트뷰와 에딧텍스트를 갱신함
                            var editStr: String? = null
                            for (data in it.child(date).children) {
                                datalist.add(data.child("todo").value.toString())
                                timelist.add(data.child("start").value.toString())
                                importancelist.add(data.child("importance").value.toString())
                            }
                            sort()
                            adapter.notifyDataSetChanged()
                            for (i in timelist.indices) {
                                if (editStr.isNullOrEmpty()) {
                                    editStr =
                                        "[" + importancelist[i] + "] " + timelist[i] + " - " + datalist[i]
                                } else
                                    editStr += "\n" + "[" + importancelist[i] + "] " + timelist[i] + " - " + datalist[i]
                            }
                            todolist.text = editStr
                            updateBtn.text = "수정"

                        } else {
                            todolist.text = ""
                            updateBtn.text = "추가"
                        }
                    }
                }, 100L)
            }
            override fun onCancelled(error: DatabaseError) {
                //Log.w("Error", "Failed to read value", error.toException())
            }
        })
        // 캘린더 기본 설정
        calendar.selectedDate = CalendarDay.today()

        calendar.state().edit()
            .isCacheCalendarPositionEnabled(false)
            .setCalendarDisplayMode(CalendarMode.MONTHS)
            .commit()
        calendar.isDynamicHeightEnabled = true
        calendar.addDecorator(SaturdayDecorator())
        calendar.addDecorator(SundayDecorator())
        // 년 - 월 순으로 나오게 하기
        calendar.setTitleFormatter(object: TitleFormatter {
            override fun format(day: CalendarDay?): CharSequence {
                val YYYY_M: SimpleDateFormat = SimpleDateFormat("YYYY년 M월")
                return YYYY_M.format(day!!.date)
            }
        })
        // 일정 있는 날 닷 표시
        updateSpan()
        // 날짜 선택 시 해당 날짜를 받아 파이어베이스로 할 일 목록 확인 리스너할당
        calendar.setOnDateChangedListener { widget: MaterialCalendarView, day: CalendarDay, selected: Boolean ->
            date = String.format(
                "%04d-%02d-%02d",
                widget.selectedDate.year,
                widget.selectedDate.month + 1,
                widget.selectedDate.day
            )
            updateTodo()
        }
        // 공지사항 확인 버튼
        noticeBtn.setOnClickListener{
            showNoticeDialog()
        }

        // 상황에 따라 추가, 수정 또는 저장 버튼으로 가변성을 갖는 좌측 버튼
        updateBtn.setOnClickListener {
            showTodoDialog()
        }

        // 할 일 삭제 버튼
        deleteBtn.setOnClickListener {
            deleteAll()
        }
    }

    fun showNoticeDialog() {
        val view: View = layoutInflater.inflate(R.layout.notice_dialog, null)
        var noticeEditText: EditText = view.findViewById<EditText>(R.id.noticeEdit)
        noticeEditText.isEnabled = false
        var noticeEditBtn: Button = view.findViewById<Button>(R.id.noticeEditBtn)

        noticeRef.get().addOnSuccessListener {
            noticeEditText.setText(it.value.toString())
        }

        var dialog: AlertDialog = AlertDialog.Builder(this@MainActivity)
            .setTitle("공지사항")
            .setPositiveButton("확인") { d, which ->
                d.dismiss()
            }
            .create()
        dialog.setView(view)
        dialog.show()

        noticeEditBtn.setOnClickListener {
            if(noticeEditBtn.text == "공지사항 편집") {
                noticeEditText.isEnabled = true
                noticeEditBtn.text = "공지사항 저장"
            }
            else {
                noticeEditText.isEnabled = false
                noticeRef.setValue(noticeEditText.text.toString())
                noticeEditBtn.text = "공지사항 편집"
            }
        }
    }

    fun showTodoDialog() {
        // 처음 열면 리스트 초기화 후 해당 위치의 데이터 가져옴
        datalist.clear()
        timelist.clear()
        importancelist.clear()
        adapter.notifyDataSetChanged()
        todoRef.get().addOnSuccessListener {
            var count: Int = 0
            for(data in it.child(date).children) {
                datalist.add(data.child("todo").value.toString())
                timelist.add(data.child("start").value.toString())
                importancelist.add(data.child("importance").value.toString())
            }
            sort()
            adapter.notifyDataSetChanged()
        }
        // 다이얼로그 생성
        var dialog: AlertDialog = AlertDialog.Builder(this@MainActivity)
            .setTitle(date + " 일정")
            .setPositiveButton("확인") { d, which ->
                d.dismiss()
            }
            .create()
        val view: View = layoutInflater.inflate(R.layout.listview_dialog, null)
        dialog.setView(view)
        dialog.show()
        // 리스트뷰 레이아웃 설정
        var listview: ListView = view.findViewById(R.id.listview1)
        // 리스트뷰에 어댑터 지정
        listview.adapter = adapter
        listview.choiceMode = ListView.CHOICE_MODE_SINGLE
        listview.setItemChecked(0, true)
        // 이벤트 처리
        listview.setOnItemClickListener { adapterView, view, i, l ->
            selectedItem = i
            adapter.notifyDataSetChanged()
        }

        var add: Button? = dialog.findViewById<Button>(R.id.add)
        var edit: Button? = dialog.findViewById<Button>(R.id.edit)
        var delete: Button? = dialog.findViewById<Button>(R.id.delete)

        add?.setOnClickListener {
            // 다이얼로그 새로 띄우고 거기서 내용 저장하면 그게 리스트뷰에 추가되도록
            showTodoEditor(1)
        }
        edit?.setOnClickListener {
            // 다이얼로그 새로 띄우고 (여긴 기존내용 가져옴) 내용 저장하면 그게 원래 리스트뷰에 저장되도록
            if(datalist.isEmpty() || !isChecked(listview)) {
                Toast.makeText(applicationContext, "편집할 대상이 없습니다", Toast.LENGTH_SHORT).show()
            } else {
                showTodoEditor(2)
            }
        }
        delete?.setOnClickListener {
            // 해당 날짜의 할 일을 선택하여 삭제
            if(datalist.isEmpty() || !isChecked(listview))
                Toast.makeText(applicationContext, "삭제할 대상이 없습니다", Toast.LENGTH_SHORT).show()
            else {
                // 리스트에서 해당 내용을 tmp에 담음
                var editStr: String = datalist[selectedItem].toString()
                // 이후 DB에서 해당되는 튜플을 찾아 해당 key를 tmp2에 담고
                todoRef.get().addOnSuccessListener {
                    for(data in it.child(date).children) {
                        if (data.child("todo").value.toString() == editStr) {
                            var key: String = data.key.toString()
                            // DB에서 삭제
                            todoRef.child(date).child(key).removeValue()
                            break
                        }
                    }
                }
            }
        }
    }
    // 할 일 편집기 (추가, 편집에 해당)
    fun showTodoEditor(requestCode: Int) {
        // 편집 다이얼로그 생성
        val view = layoutInflater.inflate(R.layout.editor_dialog, null)
        var dialog = AlertDialog.Builder(this@MainActivity).setView(view)
        var editor: EditText = view.findViewById<EditText>(R.id.todoeditor)
        // 중요도 선택
        var selectedSpinner: String? = null
        var spinner: Spinner = view.findViewById<Spinner>(R.id.spinner)
        var spinnerItem = arrayOf("중요", "일반", "미정")
        var spinnerAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, spinnerItem)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item)
        spinner.adapter = spinnerAdapter
        spinner.setSelection(0)
        selectedSpinner = spinnerItem[0]
        spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                selectedSpinner = spinnerItem[position]
            }
        }
        // 오전 오후가 아닌 24시간제로 타임피커 표시
        var timepicker: TimePicker = view.findViewById<TimePicker>(R.id.timepicker)
        timepicker.setIs24HourView(true)
        var todoStr: String = ""
        var impStr: String = ""
        // 추가의 경우 타이틀이 날짜 + 일정추가
        if (requestCode == 1) {
            dialog.setTitle(date + " 일정 추가")
        }
        // 편집의 경우 타이틀이 날짜 + 일정편집, 기존 내용을 담아서 EditText에 넣어둠
        else {
            dialog.setTitle(date + " 일정 편집")
            // 할 일 담아오기
            todoStr = datalist[selectedItem]
            editor.setText(todoStr)
            // 중요도 담아오기
            impStr = importancelist[selectedItem]
            if(impStr == "중요") {
                selectedSpinner = spinnerItem[0]
                spinner.setSelection(0)
            }
            else if(impStr == "일반") {
                selectedSpinner = spinnerItem[1]
                spinner.setSelection(1)
            }
            else {
                selectedSpinner = spinnerItem[2]
                spinner.setSelection(2)
            }
            // 시간 담아오기
            var timeStr: String = timelist[selectedItem]
            var timeStr_Arr = timeStr.split(":")
            for (i in timeStr_Arr.indices)
            {
                when (i) {
                    0 -> timepicker.hour = timeStr_Arr.get(i).toInt()
                    1 -> timepicker.minute = timeStr_Arr.get(i).toInt()
                }
            }
        }
        var listener = DialogInterface.OnClickListener{ p0, p1 ->
            var alert = p0 as AlertDialog
            when(p1) {
                DialogInterface.BUTTON_POSITIVE -> {
                    // 할 일 추가
                    if(requestCode == 1) {
                        // 새로 DB에 내용을 추가함
                        // 업무 추가
                        var key: String? = todoRef.child(date).push().key
                        todoRef.child(date).child(key.toString()).child("todo").setValue(editor.text.toString())
                        // 해당 시작 시간 추가
                        todoRef.child(date).child(key.toString()).child("start").setValue(String.format("%02d:%02d", timepicker.hour, timepicker.minute))
                        // 중요도 추가
                        todoRef.child(date).child(key.toString()).child("importance").setValue(selectedSpinner)
                    }
                    // 할 일 편집
                    else {
                        // DB에서 해당되는 튜플을 찾아 해당 key를 tmp2에 담고
                        todoRef.get().addOnSuccessListener {
                            for(data in it.child(date).children) {
                                if (data.child("todo").value.toString().equals(todoStr)) {
                                    var key: String = data.key.toString()
                                    // DB에서 내용을 수정함
                                    todoRef.child(date).child(key).child("todo").setValue(editor.text.toString())
                                    todoRef.child(date).child(key).child("start").setValue(String.format("%02d:%02d", timepicker.hour, timepicker.minute))
                                    todoRef.child(date).child(key).child("importance").setValue(selectedSpinner)
                                    break
                                }
                            }
                        }
                    }
                    // 이후 종료
                    alert.dismiss()
                }
                DialogInterface.BUTTON_NEGATIVE -> {
                    alert.dismiss()
                }
            }
        }
        dialog.setPositiveButton("저장", listener)
        dialog.setNegativeButton("취소", listener)
        dialog.show()
    }
    // 전체 삭제
    fun deleteAll() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("삭제 확인")
        builder.setMessage("정말로 삭제하시겠어요?")
        val listener = DialogInterface.OnClickListener { p0, p1 ->
            when (p1) {
                DialogInterface.BUTTON_POSITIVE -> {
                    todoRef.child(date).removeValue()
                    todolist.text = ""
                    updateBtn.text = "추가"
                }
                DialogInterface.BUTTON_NEGATIVE -> p0.dismiss()
            }
        }
        builder.setPositiveButton("네", listener)
        builder.setNegativeButton("아니오", listener)
        builder.show()
    }

    // 캘린더에 일정있는 날 점 찍기
    fun updateSpan() {
        var year: Int
        var month: Int
        var day: Int
        var str: String
        todoRef.get().addOnSuccessListener {
            if (it.hasChildren()) {
                for (key in it.children) {
                    str = key.key.toString()
                    var strArr = str.split("-")
                    year = strArr[0].toInt()
                    month = strArr[1].toInt()
                    day = strArr[2].toInt()
                    calendar.addDecorator(TododayDecorator(Collections.singleton(CalendarDay(year, month-1, day))))
                }
            }
        }
        calendar.invalidateDecorators()
    }

    // 메인화면의 에딧텍스트 갱신
    fun updateTodo() {
        todoRef.get().addOnSuccessListener {
            datalist.clear()
            timelist.clear()
            importancelist.clear()
            adapter.notifyDataSetChanged()
            if (it.hasChild(date)) {
                // 리스트뷰와 에딧텍스트를 갱신함
                var editStr: String? = null
                // 파이어베이스에 데이터를 받아옴
                for (data in it.child(date).children) {
                    datalist.add(data.child("todo").value.toString())
                    timelist.add(data.child("start").value.toString())
                    importancelist.add(data.child("importance").value.toString())
                }
                sort()
                adapter.notifyDataSetChanged()
                for (i in timelist.indices) {
                    if (editStr.isNullOrEmpty()) {
                        editStr =
                            "[" + importancelist[i] + "] " + timelist[i] + " - " + datalist[i]
                    } else
                        editStr += "\n" + "[" + importancelist[i] + "] " + timelist[i] + " - " + datalist[i]
                }
                todolist.text = editStr
                updateBtn.text = "수정"

            } else {
                todolist.text = ""
                updateBtn.text = "추가"
            }
        }
    }

    // 체크여부 확인함수 (리스트뷰의 라디오버튼 체크를 위함)
    fun isChecked(listview: ListView): Boolean {
        // 체크 여부 확인
        var size: Int = 0
        for (list in datalist) {
            if (listview.isItemChecked(size)) {
                selectedItem = size
                return true
            }
            size++
        }
        return false
    }

    // 선택정렬 함수
    fun sort(){
        // 1개 이하의 데이터에선 처리X
        if(timelist.isEmpty() || timelist.size == 1)
            return
        // : 제거
        for(i in 0 until timelist.size) {
            timelist[i] = timelist[i].replace(":", "")
        }
        // 정렬 전 임시리스트 3개 생성
        // 임시 시간리스트는 기존 것을 백업
        var timelistTmp = ArrayList<String>()
        for(i in 0 until timelist.size) {
            timelistTmp.add(timelist[i])
        }
        // 임시 할일, 임시 중요도 리스트는 빈 리스트로 생성
        var datalistTmp = ArrayList<String>()
        var importancelistTmp = ArrayList<String>()
        // 타임리스트를 자체적으로 정렬하고
        timelist.sort()
//        println("기존 데이터리스트 " + datalist)
//        println("기존 중요도리스트 " + importancelist)
//        println("기존 시간리스트" + timelistTmp)
//        println("정렬된 시간리스트" + timelist)
        // 정렬된 순서대로 임시 할일, 임시 중요도 리스트의 값을넣음
        for(i in 0 until timelist.size) {
            for(j in 0 until timelist.size) {
                if(timelist[i] == timelistTmp[j]) {
                    datalistTmp.add(datalist[j])
//                    println("데이터 " + i.toString() + "번째에 " + datalist[j] + "삽입")
                    importancelistTmp.add(importancelist[j])
//                    println("중요도 " + i.toString() + "번째에 " + importancelist[j] + "삽입")
                }
            }
        }
        // 이후 임시리스트의 값을 기존 리스트에 넣음
        for(i in 0 until datalist.size) {
            datalist[i] = datalistTmp[i]
        }
        for(i in 0 until importancelist.size) {
            importancelist[i] = importancelistTmp[i]
        }
//        println("변경된 데이터리스트 " + datalist)
//        println("변경된 중요도리스트 " + importancelist)
        // : 추가
        for (i in timelist.indices) {
            timelist[i] = timelist[i].slice(IntRange(0, 1)) + ":" + timelist[i].slice(IntRange(2, 3))
        }
    }
}

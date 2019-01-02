package com.mohsenshahini.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.support.annotation.IntDef
import android.support.v4.view.GestureDetectorCompat
import android.support.v4.view.ViewCompat
import android.support.v4.view.animation.FastOutLinearInInterpolator
import android.text.*
import android.text.format.DateFormat
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.OverScroller
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class CalendarEmployeeView : View {

    internal interface WeekViewLoader {
        /**
         * Convert a date into a double that will be used to reference when you're loading data.
         *
         * All periods that have the same integer part, define one period. Dates that are later in time
         * should have a greater return value.
         *
         * @param instance the date
         * @return The period index in which the date falls (floating point number).
         */
        fun toWeekViewPeriodIndex(instance: Calendar): Double

        /**
         * Load the events within the period
         * @param periodIndex the period to load
         * @return A list with the events of this period
         */
        fun onLoad(periodIndex: Int): List<WeekViewEvent>
    }


    interface ScrollListener {
        /**
         * Called when the first visible day has changed.
         *
         * (this will also be called during the first draw of the weekview)
         * @param newFirstVisibleDay The new first visible day
         * @param oldFirstVisibleDay The old first visible day (is null on the first call).
         */
        fun onFirstVisibleDayChanged(newFirstVisibleDay: Calendar, oldFirstVisibleDay: Calendar)
    }

    interface EmptySpaceClickListener {
        /**
         * Triggered when the users clicks on a empty space of the [CalendarEmployeeView].
         * @param time: [Calendar] object set with the date and time of the clicked position on the view.
         */
        fun onEmptySpaceClicked(time: Calendar, staff: String)
    }

    interface MonthChangeListener {
        /**
         * Very important interface, it's the base to load events in the calendar.
         * This method is called three times: once to load the previous month, once to load the next month and once to load the current month.<br></br>
         * **That's why you can have three times the same event at the same place if you mess up with the configuration**
         * @param newYear : year of the events required by the view.
         * @param newMonth : month of the events required by the view <br></br>**1 based (not like JAVA API) --> January = 1 and December = 12**.
         * @return a list of the events happening **during the specified month**.
         */
        fun onMonthChange(newYear: Int, newMonth: Int): List<WeekViewEvent>
    }

    interface EventClickListener {
        /**
         * Triggered when clicked on one existing event
         * @param event: event clicked.
         * @param eventRect: view containing the clicked event.
         */
        fun onEventClick(event: WeekViewEvent, eventRect: RectF)
    }

    companion object {
        private const val LENGTH_SHORT = 1
        private const val LENGTH_LONG = 2

        @IntDef(NONE, LEFT, RIGHT, VERTICAL)
        @Retention(AnnotationRetention.SOURCE)
        annotation class DIRECTION

        private const val NONE = 0
        private const val LEFT = 1
        private const val RIGHT = 2
        private const val VERTICAL = 3

    }

    @DIRECTION
    private var currentFlingDirection = NONE
    @DIRECTION
    private var currentScrollDirection = NONE

    /** Days */
    private var firstDayOfWeek = Calendar.MONDAY

    /** Sizes */
    private var hourHeight = 50
    private var maxHourHeight = 300
    private var minHourHeight = 0
    private var effectiveMinHourHeight = 0
    private var newHourHeight = -1
    private var eventTextSize = 12
    private var textSize = 12
    private var headerTextHeight = 0f
    private var timeTextHeight = 0f
    private var hourSeparatorHeight = 2
    private var dayNameLength = LENGTH_LONG
    private var timeTextWidth = 0f
    private var headerColumnWidth = 0f
    private var widthPerDay = 0f
    /*** Margins and Padding */
    private var eventPadding = 8
    private var headerMarginBottom = 0f
    private var headerColumnPadding = 10
    private var headerRowPadding = 10
    private var eventMarginVertical = 0
    /*** Measure values */
    private var minimuflingVelocity = 1
    private var scaledTouchSlop = 0
    private var scrollToHour = 1F
    //Fixme This must change for number of staffs
    private var numberOfStaff = 7
    private var numberOfVisibleDays = 3
    private var nowLineThickness = 1
    private var columnGap = 10
    private var overlappingEventGap = 0
    private var xScrollingSpeed = 2f
    private var yScrollingSpeed = 2f
    private var eventCornerRadius = 1
    private var fetchedPeriod = (-1).toDouble() // the middle period the calendar has fetched.

    /** Colors */
    private var defaultEventColor = 0
    private var futureWeekendBackgroundColor = 0
    private var pastWeekendBackgroundColor = 0
    private var nowLineColor = Color.RED//Color.rgb(102, 102, 102)
    private var eventTextColor = Color.BLACK
    private var headerColumnTextColor = Color.BLACK
    private var headerColumnBackgroundColor = Color.WHITE
    private var headerRowBackgroundColor = Color.WHITE
    private var dayBackgroundColor = Color.rgb(245, 245, 245)
    private var todayHeaderTextColor = Color.rgb(39, 137, 228)
    private var futureBackgroundColor = Color.rgb(245, 245, 245)
    private var pastBackgroundColor = Color.rgb(227, 227, 227)
    private var hourSeparatorColor = Color.rgb(230, 230, 230)
    private var todayBackgroundColor = Color.rgb(239, 247, 254)


    /** Flags */
    private var isZooming = false
    private var showDistinctWeekendColor = false
    private var showDistinctPastFutureColor = false
    private var showNowLine = false
    private var horizontalFlingEnabled = false
    private var verticalFlingEnabled = false
    private var areDimensionsInvalid = true
    private var refreshEvents = false
    private var isFirstDraw = true

    /** Detects scaling transformation gestures */
    private lateinit var scaleDetector: ScaleGestureDetector

    /** Paints */
    private lateinit var dayBackgroundPaint: Paint
    private lateinit var eventBackgroundPaint: Paint
    private lateinit var futureBackgroundPaint: Paint
    private lateinit var futureWeekendBackgroundPaint: Paint
    private lateinit var headerBackgroundPaint: Paint
    private lateinit var headerColumnBackgroundPaint: Paint
    private lateinit var hourSeparatorPaint: Paint
    private lateinit var nowLinePaint: Paint
    private lateinit var pastBackgroundPaint: Paint
//    private lateinit var pastWeekendBackgroundPaint: Paint
    private lateinit var timeTextPaint: Paint
    private lateinit var todayBackgroundPaint: Paint
    /*** TextPaints */
    private lateinit var eventTextPaint: TextPaint
    private lateinit var headerTextPaint: Paint
    private lateinit var todayHeaderTextPaint: Paint

    /** Points */
    private var currentOrigin = PointF(0f, 0f)

    private lateinit var scroller: OverScroller
    private lateinit var gestureDetector: GestureDetectorCompat
    private var scrollToDay: Calendar? = null
    private var firstVisibleDay: Calendar? = null
    private var eventRects = ArrayList<EventRect>()
    private var previousPeriodEvents: List<WeekViewEvent>? = arrayListOf()
    private var currentPeriodEvents: List<WeekViewEvent>? = arrayListOf()
    private var nextPeriodEvents: List<WeekViewEvent>? = arrayListOf()


    /*** Listeners */
    private var scrollListener: ScrollListener = ScrollListenerImpl()
    private var emptySpaceClickListener: EmptySpaceClickListener = EmptySpaceClickListenerImpl()
    private var eventClickListener: EventClickListener = EventClickListenerImpl()
    private var dateTimeInterpreter = DateTimeInterpreterImpl()
    //    private lateinit var monthChangeListener: MonthChangeListener
    private lateinit var weekViewLoader: WeekViewLoader


    private var bitmap: Bitmap? = null


    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        // Get the attribute values (if any).
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.CalendarEmployeeView, 0, 0)
        try {
            firstDayOfWeek = a.getInteger(R.styleable.CalendarEmployeeView_firstDayOfWeek, firstDayOfWeek)
            hourHeight = a.getDimensionPixelSize(R.styleable.CalendarEmployeeView_hourHeight, hourHeight)
            minHourHeight = a.getDimensionPixelSize(R.styleable.CalendarEmployeeView_minHourHeight, minHourHeight)
            effectiveMinHourHeight = minHourHeight
            maxHourHeight = a.getDimensionPixelSize(R.styleable.CalendarEmployeeView_maxHourHeight, maxHourHeight)
            textSize = a.getDimensionPixelSize(R.styleable.CalendarEmployeeView_textSize, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSize.toFloat(), context.resources.displayMetrics).toInt())
            headerColumnPadding = a.getDimensionPixelSize(R.styleable.CalendarEmployeeView_headerColumnPadding, headerColumnPadding)
            columnGap = a.getDimensionPixelSize(R.styleable.CalendarEmployeeView_columnGap, columnGap)
            headerColumnTextColor = a.getColor(R.styleable.CalendarEmployeeView_headerColumnTextColor, headerColumnTextColor)
            numberOfVisibleDays = a.getInteger(R.styleable.CalendarEmployeeView_noOfVisibleDays, numberOfVisibleDays)
            headerRowPadding = a.getDimensionPixelSize(R.styleable.CalendarEmployeeView_headerRowPadding, headerRowPadding)
            headerRowBackgroundColor = a.getColor(R.styleable.CalendarEmployeeView_headerRowBackgroundColor, headerRowBackgroundColor)
            dayBackgroundColor = a.getColor(R.styleable.CalendarEmployeeView_dayBackgroundColor, dayBackgroundColor)
            futureBackgroundColor = a.getColor(R.styleable.CalendarEmployeeView_futureBackgroundColor, futureBackgroundColor)
            pastBackgroundColor = a.getColor(R.styleable.CalendarEmployeeView_pastBackgroundColor, pastBackgroundColor)
            futureWeekendBackgroundColor = a.getColor(R.styleable.CalendarEmployeeView_futureWeekendBackgroundColor, futureBackgroundColor) // If not set, use the same color as in the week
            pastWeekendBackgroundColor = a.getColor(R.styleable.CalendarEmployeeView_pastWeekendBackgroundColor, pastBackgroundColor)
            nowLineColor = a.getColor(R.styleable.CalendarEmployeeView_nowLineColor, nowLineColor)
            nowLineThickness = a.getDimensionPixelSize(R.styleable.CalendarEmployeeView_nowLineThickness, nowLineThickness)
            hourSeparatorColor = a.getColor(R.styleable.CalendarEmployeeView_hourSeparatorColor, hourSeparatorColor)
            todayBackgroundColor = a.getColor(R.styleable.CalendarEmployeeView_todayBackgroundColor, todayBackgroundColor)
            hourSeparatorHeight = a.getDimensionPixelSize(R.styleable.CalendarEmployeeView_hourSeparatorHeight, hourSeparatorHeight)
            todayHeaderTextColor = a.getColor(R.styleable.CalendarEmployeeView_todayHeaderTextColor, todayHeaderTextColor)
            eventTextSize = a.getDimensionPixelSize(R.styleable.CalendarEmployeeView_eventTextSize, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, eventTextSize.toFloat(), context.resources.displayMetrics).toInt())
            eventTextColor = a.getColor(R.styleable.CalendarEmployeeView_eventTextColor, eventTextColor)
            eventPadding = a.getDimensionPixelSize(R.styleable.CalendarEmployeeView_eventPadding, eventPadding)
            headerColumnBackgroundColor = a.getColor(R.styleable.CalendarEmployeeView_headerColumnBackground, headerColumnBackgroundColor)
            dayNameLength = a.getInteger(R.styleable.CalendarEmployeeView_dayNameLength, dayNameLength)
            overlappingEventGap = a.getDimensionPixelSize(R.styleable.CalendarEmployeeView_overlappingEventGap, overlappingEventGap)
            eventMarginVertical = a.getDimensionPixelSize(R.styleable.CalendarEmployeeView_eventMarginVertical, eventMarginVertical)
            xScrollingSpeed = a.getFloat(R.styleable.CalendarEmployeeView_xScrollingSpeed, xScrollingSpeed)
            eventCornerRadius = a.getDimensionPixelSize(R.styleable.CalendarEmployeeView_eventCornerRadius, eventCornerRadius)
            showDistinctPastFutureColor = a.getBoolean(R.styleable.CalendarEmployeeView_showDistinctPastFutureColor, showDistinctPastFutureColor)
            showDistinctWeekendColor = a.getBoolean(R.styleable.CalendarEmployeeView_showDistinctWeekendColor, showDistinctWeekendColor)
            showNowLine = a.getBoolean(R.styleable.CalendarEmployeeView_showNowLine, showNowLine)
            horizontalFlingEnabled = a.getBoolean(R.styleable.CalendarEmployeeView_horizontalFlingEnabled, horizontalFlingEnabled)
            verticalFlingEnabled = a.getBoolean(R.styleable.CalendarEmployeeView_verticalFlingEnabled, verticalFlingEnabled)
        } finally {
            a.recycle()
        }

        init()
    }

    fun setBitmap(bitmap: Bitmap?) {
        this.bitmap = bitmap
        requestLayout()
    }

    @Suppress("unused")
    fun setOnScrollListener(listener: ScrollListener) {
        scrollListener = listener
    }

    fun setOnEventClickListener(listener: EventClickListener) {
        eventClickListener = listener
    }

    fun setOnEmptySpaceListener(listener: EmptySpaceClickListener) {
        emptySpaceClickListener = listener
    }

    @Suppress("unused")
    fun getMonthChangeListener(): MonthChangeListener? = if (weekViewLoader is MonthLoader) {
        (weekViewLoader as MonthLoader).getOnMonthChangeListener()
    } else {
        null
    }


    fun setMonthChangeListener(listener: MonthChangeListener) {
        weekViewLoader = MonthLoader(listener)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        val touchEvent = gestureDetector.onTouchEvent(event)

        // Check after call of mGestureDetector, so mCurrentFlingDirection and mCurrentScrollDirection are set.
        if (event.action == MotionEvent.ACTION_UP && !isZooming && currentFlingDirection == NONE) {
            if (currentScrollDirection == RIGHT || currentScrollDirection == LEFT) {
                goToNearestOrigin()
            }
            currentScrollDirection = NONE
        }

        return touchEvent
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let {
            // Hide everything in the first cell (top left corner).
            it.drawRect(
                    0f,
                    0f,
                    timeTextWidth + headerColumnPadding * 2,
                    headerTextHeight + headerRowPadding * 2,
                    headerBackgroundPaint
            )

            // Draw the header row.
//            drawHeaderRowAndEvents(it)
            drawHourLine(it)

            // Draw the time column and all the axes/separators. 
            drawTimeColumnAndAxes(it)
        }
    }

    private fun init() {
        // Scrolling initialization.
        gestureDetector = GestureDetectorCompat(context, SimpleOnGestureListenerImpl())
        scroller = OverScroller(context, FastOutLinearInInterpolator())

        minimuflingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
        scaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop

        // Measure settings for time column.
        timeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        timeTextPaint.textAlign = Paint.Align.RIGHT
        timeTextPaint.textSize = textSize.toFloat()
        timeTextPaint.color = headerColumnTextColor
        val rect = Rect()
        timeTextPaint.getTextBounds("00 PM", 0, "00 PM".length, rect)
        timeTextHeight = rect.height().toFloat()
        headerMarginBottom = timeTextHeight / 2
        initTextTimeWidth()

        // Measure settings for header row.
        headerTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        headerTextPaint.color = headerColumnTextColor
        headerTextPaint.textAlign = Paint.Align.CENTER
        headerTextPaint.textSize = textSize.toFloat()
        headerTextPaint.getTextBounds("00 PM", 0, "00 PM".length, rect)
        headerTextHeight = rect.height().toFloat()
        headerTextPaint.typeface = Typeface.DEFAULT_BOLD

        // Prepare header background paint.
        headerBackgroundPaint = Paint()
        headerBackgroundPaint.color = headerRowBackgroundColor

        // Prepare day background color paint.
        dayBackgroundPaint = Paint()
        dayBackgroundPaint.color = dayBackgroundColor
        futureBackgroundPaint = Paint()
        futureBackgroundPaint.color = futureBackgroundColor
        pastBackgroundPaint = Paint()
        pastBackgroundPaint.color = pastBackgroundColor
        futureWeekendBackgroundPaint = Paint()
        futureWeekendBackgroundPaint.color = futureWeekendBackgroundColor
//        pastWeekendBackgroundPaint = Paint()
//        pastWeekendBackgroundPaint.color = pastWeekendBackgroundColor

        // Prepare hour separator color paint.
        hourSeparatorPaint = Paint()
        hourSeparatorPaint.style = Paint.Style.STROKE
        hourSeparatorPaint.strokeWidth = hourSeparatorHeight.toFloat()
        hourSeparatorPaint.color = hourSeparatorColor

        // Prepare the "now" line color paint
        nowLinePaint = Paint()
        nowLinePaint.strokeWidth = nowLineThickness.toFloat()
        nowLinePaint.color = nowLineColor

        // Prepare today background color paint.
        todayBackgroundPaint = Paint()
        todayBackgroundPaint.color = todayBackgroundColor

        // Prepare today header text color paint.
        todayHeaderTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        todayHeaderTextPaint.textAlign = Paint.Align.CENTER
        todayHeaderTextPaint.textSize = textSize.toFloat()
        todayHeaderTextPaint.typeface = Typeface.DEFAULT_BOLD
        todayHeaderTextPaint.color = todayHeaderTextColor

        // Prepare event background color.
        eventBackgroundPaint = Paint()
        eventBackgroundPaint.color = Color.rgb(174, 208, 238)

        // Prepare header column background color.
        headerColumnBackgroundPaint = Paint()
        headerColumnBackgroundPaint.color = headerColumnBackgroundColor

        // Prepare event text size and color.
        eventTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.LINEAR_TEXT_FLAG)
        eventTextPaint.style = Paint.Style.FILL
        eventTextPaint.color = eventTextColor
        eventTextPaint.textSize = eventTextSize.toFloat()

        // Set default event color.
        defaultEventColor = Color.parseColor("#9fc6e7")

        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.OnScaleGestureListener {
            override fun onScaleEnd(detector: ScaleGestureDetector) {
                isZooming = false
            }

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                isZooming = true
//                goToNearestOrigin()
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
//                newHourHeight = Math.round(hourHeight * detector.scaleFactor)
//                invalidate()
                return true
            }
        })
    }

    /**
     * Checks if two times are on the same day.
     * @param dayOne The first day.
     * @param dayTwo The second day.
     * @return Whether the times are on the same day.
     */
    private fun isSameDay(dayOne: Calendar?, dayTwo: Calendar?) = dayOne?.get(Calendar.YEAR) == dayTwo?.get(Calendar.YEAR) && dayOne?.get(Calendar.DAY_OF_YEAR) == dayTwo?.get(Calendar.DAY_OF_YEAR)

    private fun goToNearestOrigin() {
        var leftDays = (currentOrigin.x / (widthPerDay + columnGap)).toDouble()

        leftDays = when {
            currentFlingDirection != NONE -> // snap to nearest day
                Math.round(leftDays).toDouble()
            currentScrollDirection == LEFT -> // snap to last day
                Math.floor(leftDays)
            currentScrollDirection == RIGHT -> // snap to next day
                Math.ceil(leftDays)
            else -> // snap to nearest day
                Math.round(leftDays).toDouble()
        }

        val nearestOrigin = (currentOrigin.x - leftDays * (widthPerDay + columnGap)).toInt()

        if (nearestOrigin != 0) {
            // Stop current animation.
            scroller.forceFinished(true)
            // Snap to date.
            scroller.startScroll(currentOrigin.x.toInt(), currentOrigin.y.toInt(), -nearestOrigin, 0, (Math.abs(nearestOrigin) / widthPerDay * 500).toInt())
            ViewCompat.postInvalidateOnAnimation(this@CalendarEmployeeView)
        }
        // Reset scrolling and fling direction.
        currentFlingDirection = NONE
        currentScrollDirection = currentFlingDirection
    }

    private fun initTextTimeWidth() {
        timeTextWidth = 0f
        for (i in 0..23) {
            // Measure time string and get max width.
            //TODO maybe here is needed to throw an exception if time will be null
            // ?: throw IllegalStateException("A DateTimeInterpreter must not return null time")
            val time = dateTimeInterpreter.interpretTime(i)
            timeTextWidth = Math.max(timeTextWidth, timeTextPaint.measureText(time))
        }
    }

    private fun goToHour(hour: Float) {

        //TODO Needed implementation
    }

    private fun goToDate(date: Calendar) {
        return
        scroller.forceFinished(true)
        currentFlingDirection = NONE

        date.set(Calendar.HOUR_OF_DAY, 0)
        date.set(Calendar.MINUTE, 0)
        date.set(Calendar.SECOND, 0)
        date.set(Calendar.MILLISECOND, 0)

        if (areDimensionsInvalid) {
            scrollToDay = date
            return
        }

        refreshEvents = true

        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val day = 1000L * 60L * 60L * 24L
        val dateInMillis = date.timeInMillis + date.timeZone.getOffset(date.timeInMillis)
        val todayInMillis = today.timeInMillis + today.timeZone.getOffset(today.timeInMillis)
        val dateDifference = dateInMillis / day - todayInMillis / day
        currentOrigin.x = -dateDifference * (widthPerDay + columnGap)
        invalidate()
    }

    private fun today(): Calendar {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        return today
    }

    /**
     * Draw all the events of a particular day.
     * @param date The day.
     * @param startFropixel The left position of the day area. The events will never go any left from this value.
     * @param canvas The canvas to draw upon.
     */
    private fun drawEvents(date: Calendar, startFropixel: Float, canvas: Canvas) {
        if (eventRects.isNullOrEmpty()) {
            return
        }
        for (i in eventRects.indices) {
            if (isSameDay(eventRects[i].event.startTime, date)) {

                // Calculate top.
                val top = hourHeight.toFloat() * 24f * eventRects[i].top / 1440 +
                        currentOrigin.y + headerTextHeight + (headerRowPadding * 2).toFloat() +
                        headerMarginBottom + timeTextHeight / 2 + eventMarginVertical.toFloat()

                // Calculate bottom.
                var bottom = eventRects[i].bottom
                bottom = hourHeight.toFloat() * 24f * bottom / 1440 + currentOrigin.y +
                        headerTextHeight + (headerRowPadding * 2).toFloat() +
                        headerMarginBottom + timeTextHeight / 2 - eventMarginVertical

                // Calculate left and right.
                var left = startFropixel + eventRects[i].left * widthPerDay
                if (left < startFropixel)
                    left += overlappingEventGap.toFloat()
                var right = left + eventRects[i].width * widthPerDay
                if (right < startFropixel + widthPerDay)
                    right -= overlappingEventGap.toFloat()

                // Draw the event and the event name on top of it.
                if (left < right && left < width && top < height && right > headerColumnWidth &&
                        bottom > headerTextHeight + (headerRowPadding * 2).toFloat() + timeTextHeight / 2 + headerMarginBottom) {
                    eventRects[i].rectF = RectF(left, top, right, bottom)
                    eventBackgroundPaint.color = if (eventRects[i].event.color == 0) defaultEventColor else eventRects[i].event.color
                    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                    canvas.drawRoundRect(
                            eventRects[i].rectF,
                            eventCornerRadius.toFloat(),
                            eventCornerRadius.toFloat(),
                            eventBackgroundPaint
                    )
                    drawEventTitle(eventRects[i].event, eventRects[i].rectF, canvas, top, left)
                } else
                    eventRects[i].rectF = null
            }
        }
    }

    /**
     * Draw the name of the event on top of the event rectangle.
     * @param event The event of which the title (and location) should be drawn.
     * @param rect The rectangle on which the text is to be drawn.
     * @param canvas The canvas to draw upon.
     * @param originalTop The original top position of the rectangle. The rectangle may have some of its portion outside of the visible area.
     * @param originalLeft The original left position of the rectangle. The rectangle may have some of its portion outside of the visible area.
     */
    private fun drawEventTitle(event: WeekViewEvent, rect: RectF?, canvas: Canvas, originalTop: Float, originalLeft: Float) {
        if (rect == null) return
        if (rect.right - rect.left - (eventPadding * 2).toFloat() < 0) return
        if (rect.bottom - rect.top - (eventPadding * 2).toFloat() < 0) return

        val simpleDateFormat = SimpleDateFormat("h:mm", Locale.getDefault())

        val startTime = simpleDateFormat.format(event.startTime?.time).toUpperCase()
        val endTime = simpleDateFormat.format(event.endTime?.time).toUpperCase()
        // Prepare the name of the event.
        val spannableStringBuilder = SpannableStringBuilder()
        spannableStringBuilder.append(startTime)
        spannableStringBuilder.append(" - ")
        spannableStringBuilder.append(endTime)
        spannableStringBuilder.setSpan(StyleSpan(Typeface.BOLD), 0, spannableStringBuilder.length, 0)
        spannableStringBuilder.append("\n")
        spannableStringBuilder.append(event.name)

        // Prepare the location of the event.
        spannableStringBuilder.append(event.location)

        val availableHeight = (rect.bottom - originalTop - (eventPadding * 2).toFloat()).toInt()
        val availableWidth = (rect.right - originalLeft - (eventPadding * 2).toFloat()).toInt()

        // Get text dimensions.
        var textLayout = StaticLayout(spannableStringBuilder, eventTextPaint, availableWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)

        val lineHeight = textLayout.height / textLayout.lineCount

        if (availableHeight >= lineHeight) {
            // Calculate available number of line counts.
            var availableLineCount = availableHeight / lineHeight
            do {
                // Ellipsize text to fit into event rect.
                textLayout = StaticLayout(TextUtils.ellipsize(spannableStringBuilder, eventTextPaint, (availableLineCount * availableWidth).toFloat(), TextUtils.TruncateAt.END), eventTextPaint, (rect.right - originalLeft - (eventPadding * 2).toFloat()).toInt(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)

                // Reduce line count.
                availableLineCount--

                // Repeat until text is short enough.
            } while (textLayout.height > availableHeight)

            // Draw text.
            canvas.save()
            canvas.translate(originalLeft + eventPadding, originalTop + eventPadding)
            textLayout.draw(canvas)
            canvas.restore()
        }
    }

    /**
     * Get the time and date where the user clicked on.
     * @param x The x position of the touch event.
     * @param y The y position of the touch event.
     * @return The time and date at the clicked position.
     */
    private fun getTimeForPoint(x: Float, y: Float): Calendar? {
        val leftDaysWithGaps = (-Math.ceil((currentOrigin.x / (widthPerDay + columnGap)).toDouble())).toInt()
        var startPixel = currentOrigin.x + (widthPerDay + columnGap) * leftDaysWithGaps +
                headerColumnWidth
        for (dayNumber in leftDaysWithGaps + 1..leftDaysWithGaps + numberOfStaff + 1) {
            val start = if (startPixel < headerColumnWidth) headerColumnWidth else startPixel
            if (widthPerDay + startPixel - start > 0 && x > start && x < startPixel + widthPerDay) {
                val day = today()
                day.add(Calendar.DATE, dayNumber - 1)
                val pixelsFromZero = (y - currentOrigin.y - headerTextHeight
                        - (headerRowPadding * 2).toFloat() - timeTextHeight / 2 - headerMarginBottom)
                val hour = (pixelsFromZero / hourHeight).toInt()
                val minute = (60 * (pixelsFromZero - hour * hourHeight) / hourHeight).toInt()
                day.add(Calendar.HOUR, hour)
                day.set(Calendar.MINUTE, minute)
                return day
            }
            startPixel += widthPerDay + columnGap
        }
        return null
    }

    //FIXME this method in progress. Now, return a string of number of staff but it must return a staff object
    /**
     * Get the employee where the user clicked on.
     * @param x The x position of the touch event.
     * @param y The y position of the touch event.
     * @return The employee at the clicked position.
     */
    private fun getEmployeeForPoint(x: Float, y: Float): String {
        val leftEmployeesWithGaps = (-Math.ceil((currentOrigin.x / (widthPerDay + columnGap)).toDouble())).toInt()
        var startPixel = currentOrigin.x + (widthPerDay + columnGap) * leftEmployeesWithGaps +
                headerColumnWidth

        for (staffNumber in leftEmployeesWithGaps + 1..leftEmployeesWithGaps + numberOfStaff + 1) {
            val start = if (startPixel < headerColumnWidth) {
                headerColumnWidth
            } else {
                startPixel
            }
            if (widthPerDay + startPixel - start > 0 && x > start && x < startPixel + widthPerDay) {
                val pixelsFromZero = (y - currentOrigin.y - headerTextHeight
                        - (headerRowPadding * 2).toFloat() - timeTextHeight / 2 - headerMarginBottom)
                return staffNumber.toString()
            }
            startPixel += widthPerDay + columnGap
        }
        return "Not Found"
    }


    /**
     * Gets more events of one/more month(s) if necessary. This method is called when the user is
     * scrolling the week view. The week view stores the events of three months: the visible month,
     * the previous month, the next month.
     * @param day The day where the user is currently is.
     */
    private fun getMoreEvents(day: Calendar) {
        // Get more events if the month is changed.
        if (eventRects == null)
            eventRects = ArrayList<EventRect>()
        if (weekViewLoader == null && !isInEditMode)
            throw IllegalStateException("You must provide a MonthChangeListener")

        // If a refresh was requested then reset some variables.
        if (refreshEvents) {
            eventRects.clear()
            previousPeriodEvents = null
            currentPeriodEvents = null
            nextPeriodEvents = null
            fetchedPeriod = (-1).toDouble()
        }

        if (weekViewLoader != null) {
            val periodToFetch = weekViewLoader.toWeekViewPeriodIndex(day)
            if (!isInEditMode && (fetchedPeriod < 0 || fetchedPeriod != periodToFetch || refreshEvents)) {
                var previousPeriodEvents: List<WeekViewEvent>? = null
                var currentPeriodEvents: List<WeekViewEvent>? = null
                var nextPeriodEvents: List<WeekViewEvent>? = null

                if (previousPeriodEvents != null && currentPeriodEvents != null && nextPeriodEvents != null) {
                    when (periodToFetch) {
                        fetchedPeriod - 1 -> {
                            currentPeriodEvents = previousPeriodEvents
                            nextPeriodEvents = currentPeriodEvents
                        }
                        fetchedPeriod -> {
                            previousPeriodEvents = previousPeriodEvents
                            currentPeriodEvents = currentPeriodEvents
                            nextPeriodEvents = nextPeriodEvents
                        }
                        fetchedPeriod + 1 -> {
                            previousPeriodEvents = currentPeriodEvents
                            currentPeriodEvents = nextPeriodEvents
                        }
                    }
                }
                if (currentPeriodEvents == null)
                    currentPeriodEvents = weekViewLoader.onLoad(periodToFetch.toInt())
                if (previousPeriodEvents == null)
                    previousPeriodEvents = weekViewLoader.onLoad(periodToFetch.toInt() - 1)
                if (nextPeriodEvents == null)
                    nextPeriodEvents = weekViewLoader.onLoad(periodToFetch.toInt() + 1)


                // Clear events.
                eventRects.clear()
                sortAndCacheEvents(previousPeriodEvents)
                sortAndCacheEvents(currentPeriodEvents)
                sortAndCacheEvents(nextPeriodEvents)

//                previousPeriodEvents = previousPeriodEvents
//                currentPeriodEvents = currentPeriodEvents
//                nextPeriodEvents = nextPeriodEvents
                fetchedPeriod = periodToFetch
            }
        }

        // Prepare to calculate positions of each events.
        val tempEvents = eventRects
        eventRects = ArrayList<EventRect>()

        // Iterate through each day with events to calculate the position of the events.
        while (tempEvents.size > 0) {
            val eventRects = ArrayList<EventRect>(tempEvents.size)

            // Get first event for a day.
            val eventRect1 = tempEvents.removeAt(0)
            eventRects.add(eventRect1)

            var i = 0
            while (i < tempEvents.size) {
                // Collect all other events for same day.
                val eventRect2 = tempEvents[i]
                if (isSameDay(eventRect1.event.startTime, eventRect2.event.startTime)) {
                    tempEvents.removeAt(i)
                    eventRects.add(eventRect2)
                } else {
                    i++
                }
            }
            computePositionOfEvents(eventRects)
        }
    }

    /**
     * Checks if two events overlap.
     * @param event1 The first event.
     * @param event2 The second event.
     * @return true if the events overlap.
     */
    private fun isEventsCollide(event1: WeekViewEvent, event2: WeekViewEvent): Boolean {
        val start1 = event1.startTime?.timeInMillis ?: 0L
        val end1 = event1.endTime?.timeInMillis ?: 0L
        val start2 = event2.startTime?.timeInMillis ?: 0L
        val end2 = event2.endTime?.timeInMillis ?: 0L
        return !(start1 >= end2 || end1 <= start2)
    }

    /**
     * Calculates the left and right positions of each events. This comes handy specially if events
     * are overlapping.
     * @param eventRects The events along with their wrapper class.
     */
    private fun computePositionOfEvents(eventRects: List<EventRect>) {
        // Make "collision groups" for all events that collide with others.
        //Fixme List<List>
        val collisionGroups = ArrayList<ArrayList<EventRect>>()
        for (eventRect in eventRects) {
            var isPlaced = false
            outerLoop@ for (collisionGroup in collisionGroups) {
                for (groupEvent in collisionGroup) {
                    if (isEventsCollide(groupEvent.event, eventRect.event)) {
                        collisionGroup.add(eventRect)
                        isPlaced = true
                        break@outerLoop
                    }
                }
            }
            if (!isPlaced) {
                val newGroup = java.util.ArrayList<EventRect>()
                newGroup.add(eventRect)
                collisionGroups.add(newGroup)
            }
        }

        for (collisionGroup in collisionGroups) {
            expandEventsToMaxWidth(collisionGroup)
        }
    }

    /**
     * Expands all the events to maximum possible width. The events will try to occupy maximum
     * space available horizontally.
     * @param collisionGroup The group of events which overlap with each other.
     */
    private fun expandEventsToMaxWidth(collisionGroup: List<EventRect>) {
        // Expand the events to maximum possible width.
        //Fixme List<List>
        val columns = ArrayList<ArrayList<EventRect>>()
        columns.add(java.util.ArrayList())
        for (eventRect in collisionGroup) {
            var isPlaced = false
            for (column in columns) {
                if (column.size == 0) {
                    column.add(eventRect)
                    isPlaced = true
                } else if (!isEventsCollide(eventRect.event, column[column.size - 1].event)) {
                    column.add(eventRect)
                    isPlaced = true
                    break
                }
            }
            if (!isPlaced) {
                val newColumn = java.util.ArrayList<EventRect>()
                newColumn.add(eventRect)
                columns.add(newColumn)
            }
        }


        // Calculate left and right position for all the events.
        // Get the maxRowCount by looking in all columns.
        var maxRowCount = 0
        for (column in columns) {
            maxRowCount = Math.max(maxRowCount, column.size)
        }
        for (i in 0 until maxRowCount) {
            // Set the left and right values of the event.
            var j = 0f
            for (column in columns) {
                if (column.size >= i + 1) {
                    val eventRect = column[i]
                    eventRect.width = 1f / columns.size
                    eventRect.left = j / columns.size
                    //Fixme !!
                    eventRect.top = eventRect.event.startTime!!.get(Calendar.HOUR_OF_DAY) * 60 + eventRect.event.startTime!!.get(Calendar.MINUTE).toFloat()
                    eventRect.bottom = eventRect.event.endTime!!.get(Calendar.HOUR_OF_DAY) * 60 + eventRect.event.endTime!!.get(Calendar.MINUTE).toFloat()
                    eventRects.add(eventRect)
                }
                j++
            }
        }
    }

    private fun drawHourLine(canvas: Canvas) {
        // Calculate the available width for each day.
        headerColumnWidth = timeTextWidth + headerColumnPadding * 2
        widthPerDay = width - headerColumnWidth - columnGap * (numberOfVisibleDays - 1)
        widthPerDay /= numberOfVisibleDays

        val today: Calendar = today()

        if (areDimensionsInvalid) {
            effectiveMinHourHeight = Math.max(minHourHeight, ((height - headerTextHeight - headerRowPadding * 2 - headerMarginBottom) / 24).toInt())

            areDimensionsInvalid = false
            scrollToDay?.let {
                goToDate(it)
            }

            areDimensionsInvalid = false
            if (scrollToHour >= 0)
                goToHour(scrollToHour)

            scrollToDay = null
            scrollToHour = -1f
            areDimensionsInvalid = false
        }
        if (isFirstDraw) {
            isFirstDraw = false

            // If the week view is being drawn for the first time, then consider the first day of the week.
            if (numberOfStaff >= 7 && today.get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) {
                val difference = 7 + (today.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek)
                currentOrigin.x += (widthPerDay + columnGap) * difference
            }
        }

        // Calculate the new height due to the zooming.
        if (newHourHeight > 0) {
            if (newHourHeight < effectiveMinHourHeight)
                newHourHeight = effectiveMinHourHeight
            else if (newHourHeight > maxHourHeight)
                newHourHeight = maxHourHeight

            currentOrigin.y = (currentOrigin.y / hourHeight) * newHourHeight
            hourHeight = newHourHeight
            newHourHeight = -1
        }

        // If the new currentOrigin.y is invalid, make it valid.
        val originHeight = height - hourHeight * 24 - headerTextHeight - headerRowPadding * 2 - headerMarginBottom - timeTextHeight / 2
        if (currentOrigin.y < originHeight) {
            currentOrigin.y = originHeight
        }

        // Don't put an "else if" because it will trigger a glitch when completely zoomed out and
        // scrolling vertically.
        if (currentOrigin.y > 0) {
            currentOrigin.y = 0f
        }

        //test
        if (currentOrigin.x > 0) {
            currentOrigin.x = 0f
        }
        val originWidth = -widthPerDay * numberOfStaff
        if (currentOrigin.x < originWidth) {
            currentOrigin.x = originWidth
        }

        // Consider scroll offset.
        val leftDaysWithGaps = -(Math.ceil(currentOrigin.x.toDouble() / (widthPerDay + columnGap))).toInt()
        val startFropixel = currentOrigin.x + (widthPerDay + columnGap) * leftDaysWithGaps + headerColumnWidth
        var startPixel = startFropixel

        // Prepare to iterate for each day.
        val day = today.clone()
        (day as Calendar).add(Calendar.HOUR, 6)

        // Prepare to iterate for each hour to draw the hour lines.
        var lineCount = ((height - headerTextHeight - headerRowPadding * 2 - headerMarginBottom) / hourHeight) + 1
        lineCount *= (numberOfStaff + 1)
        val hourLines = FloatArray(lineCount.toInt() * 4)

        // Clear the cache for event rectangles.
        eventRects?.let {
            for (eventRect: EventRect in it) {
                eventRect.rectF = null
            }
        }

        // Clip to paint events only.
        canvas.clipRect(
                headerColumnWidth,
                headerTextHeight + headerRowPadding * 2 + headerMarginBottom + timeTextHeight / 2,
                width.toFloat(),
                height.toFloat(),
                Region.Op.REPLACE
        )

        // Iterate through each day.
        val oldFirstVisibleDay = firstVisibleDay
        firstVisibleDay = today.clone() as Calendar
        firstVisibleDay?.add(Calendar.DATE, -(Math.round(currentOrigin.x / (widthPerDay + columnGap))))
        firstVisibleDay?.let {
            if (it != oldFirstVisibleDay) {
                oldFirstVisibleDay?.let { it1 -> scrollListener.onFirstVisibleDayChanged(it, it1) }
            }
        }

        val begin = leftDaysWithGaps + 1
        val end = leftDaysWithGaps + numberOfStaff + 1
        for (dayNumber in begin..end) {
            // Check if the day is today.
            val day = today.clone()
            val lastVisibleDay = (day as Calendar).clone()
            day.add(Calendar.DATE, dayNumber - 1)
            (lastVisibleDay as Calendar).add(Calendar.DATE, dayNumber - 2)
            val sameDay = isSameDay(day, today)

            // Get more events if necessary. We want to store the events 3 months beforehand. Get
            // events only when it is the first iteration of the loop.
            if (eventRects == null || refreshEvents ||
                    (dayNumber == leftDaysWithGaps + 1 && fetchedPeriod != weekViewLoader.toWeekViewPeriodIndex(day) &&
                            Math.abs(fetchedPeriod - weekViewLoader.toWeekViewPeriodIndex(day)) > 0.5)) {
                getMoreEvents(day)
                refreshEvents = false
            }

            //Fixme
            // Draw background color for each day.
            val start = if (startPixel < headerColumnWidth) {
                headerColumnWidth
            } else {
                startPixel
            }
            if (widthPerDay + startPixel - start > 0) {
                if (showDistinctPastFutureColor) {
                    val isWeekend = day.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || day.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
//                    val pastPaint: Paint = if (isWeekend && showDistinctWeekendColor) {
//                        pastWeekendBackgroundPaint
//                    } else {
//                        pastBackgroundPaint
//                    }
                    val futurePaint: Paint = if (isWeekend && showDistinctWeekendColor) {
                        futureWeekendBackgroundPaint
                    } else {
                        futureBackgroundPaint
                    }
                    val startY = headerTextHeight + headerRowPadding * 2 + timeTextHeight / 2 + headerMarginBottom + currentOrigin.y

                    when {
                        sameDay -> {
                            val now = Calendar.getInstance()
                            val beforeNow = (now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE)/60.0f) * hourHeight
//                            canvas.drawRect(start, startY, startPixel + widthPerDay, startY+beforeNow, pastPaint)
                            canvas.drawRect(start, startY+beforeNow, startPixel + widthPerDay, height.toFloat(), futurePaint)
                        }
//                        day.before(today) -> canvas.drawRect(start, startY, startPixel + widthPerDay, height.toFloat(), pastPaint)
                        else -> canvas.drawRect(start, startY, startPixel + widthPerDay, height.toFloat(), futurePaint)
                    }
                }
                else {
                    val backgroundPaint = dayBackgroundPaint //if (sameDay) {
//                        todayBackgroundPaint
//                    } else {
//                        dayBackgroundPaint
//                    }
                    //OK
                    if (!day.before(today)) {
                        canvas.drawRect(start, headerTextHeight + headerRowPadding * 2 + timeTextHeight / 2 + headerMarginBottom, startPixel + widthPerDay, height.toFloat(), backgroundPaint)
                    }
                }
            }

            // Prepare the separator lines for hours.
            var i = 0
            for (hourNumber in 0..23) {
                val top = headerTextHeight + headerRowPadding * 2 + currentOrigin.y + hourHeight * hourNumber + timeTextHeight / 2 + headerMarginBottom
                if (top > headerTextHeight + headerRowPadding * 2 + timeTextHeight / 2 + headerMarginBottom - hourSeparatorHeight && top < height && startPixel + widthPerDay - start > 0) {
                    hourLines[i * 4] = start
                    hourLines[i * 4 + 1] = top
                    hourLines[i * 4 + 2] = startPixel + widthPerDay
                    hourLines[i * 4 + 3] = top
                    i++
                }
            }

            if (!day.before(today)) {
                // Draw the lines for hours.
                canvas.drawLines(hourLines, hourSeparatorPaint)
                // Draw the events.
                drawEvents(day, startPixel, canvas)
            }

            // Draw the line at the current time.
            if (showNowLine && sameDay) {
                val startY = headerTextHeight + headerRowPadding * 2 + timeTextHeight / 2 + headerMarginBottom + currentOrigin.y
                val now = Calendar.getInstance()
                val beforeNow = (now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE) / 60.0f) * hourHeight
                canvas.drawLine(start, startY + beforeNow, startPixel + widthPerDay, startY + beforeNow, nowLinePaint)
            }

            // In the next iteration, start from the next day.
            startPixel += widthPerDay + columnGap

        }


        // Clip to paint header row only.
        canvas.clipRect(headerColumnWidth, 0f, width.toFloat(), headerTextHeight + headerRowPadding * 2, Region.Op.REPLACE)

        // Draw the header background.
        canvas.drawRect(0f, 0f, width.toFloat(), headerTextHeight + headerRowPadding * 2, headerBackgroundPaint)






        // TODO this header must move up than staff header. Also, must implement day header corresponding design
        /*** Draw the header row texts. ***/
        startPixel = startFropixel
        for (employee in begin..end) {
            /*** Draw the day labels. ***/
            val headerPaint = headerTextPaint
            bitmap?.let {
                canvas.drawBitmap(bitmap, startPixel + widthPerDay / 2, headerTextHeight + headerRowPadding, headerPaint)
            }
            canvas.drawText("STAFF $employee", startPixel + widthPerDay / 2, headerTextHeight + headerRowPadding, headerPaint)
            startPixel += widthPerDay + columnGap
            /*** *** ***/
        }
        /*** *** ***/



//        // TODO this header must move up than staff header. Also, must implement day header corresponding design
//        /*** Draw the header row texts. ***/
//        startPixel = startFropixel
//        for (dayNumber in begin..end) {
//            // Check if the day is today.
//            val day = today.clone()
//            (day as Calendar).add(Calendar.DATE, dayNumber - 1)
//            val sameDay = isSameDay(day, today)
//            /*** Draw the day labels. ***/
//            val dayLabel = dateTimeInterpreter.interpretDate(day) ?: throw IllegalStateException("A DateTimeInterpreter must not return null date")
//            val headerPaint = if (sameDay) {
//                todayHeaderTextPaint
//            } else {
//                headerTextPaint
//            }
//            bitmap?.let {
//                canvas.drawBitmap(bitmap, startPixel + widthPerDay / 2, headerTextHeight + headerRowPadding, headerPaint)
//            }
//            canvas.drawText(dayLabel, startPixel + widthPerDay / 2, headerTextHeight + headerRowPadding, headerPaint)
//            startPixel += widthPerDay + columnGap
//            /*** *** ***/
//        }
//        /*** *** ***/
    }

    private fun drawTimeColumnAndAxes(canvas: Canvas) {
        // Draw the background color for the header column.
        canvas.drawRect(
                0f,
                headerTextHeight + headerRowPadding * 2,
                headerColumnWidth,
                height.toFloat(),
                headerColumnBackgroundPaint
        )

        // Clip to paint in left column only.
        canvas.clipRect(
                0f,
                headerTextHeight + headerRowPadding * 2,
                headerColumnWidth,
                height.toFloat(),
                Region.Op.REPLACE
        )

        for (i in 0..23) {
            val top = headerTextHeight + (headerRowPadding * 2).toFloat() + currentOrigin.y + (hourHeight * i).toFloat() + headerMarginBottom

            // Draw the text if its y position is not outside of the visible area. The pivot point of the text is the point at the bottom-right corner.
            val time = dateTimeInterpreter.interpretTime(i)
            if (top < height) canvas.drawText(time, timeTextWidth + headerColumnPadding, top + timeTextHeight, timeTextPaint)
        }
    }

    /**
     * Sort and cache events.
     * @param events The events to be sorted and cached.
     */
    private fun sortAndCacheEvents(events: List<WeekViewEvent>) {
        sortEvents(events)
        for (event in events) {
            cacheEvent(event)
        }
    }

    /**
     * Cache the event for smooth scrolling functionality.
     * @param event The event to cache.
     */
    private fun cacheEvent(event: WeekViewEvent) {
        //Fixme !!
        if (event.startTime?.compareTo(event.endTime)!! >= 0)
            return
        if (!isSameDay(event.startTime, event.endTime)) {
            // Add first day.
            val endTime = event.startTime?.clone() as Calendar
            endTime.set(Calendar.HOUR_OF_DAY, 23)
            endTime.set(Calendar.MINUTE, 59)
            val event1 = WeekViewEvent(event.id, event.name, event.startTime, endTime, event.location)
            event1.color = event.color
            eventRects.add(EventRect(event1, event, null))

            // Add other days.
            val otherDay = event.startTime?.clone() as Calendar
            otherDay.add(Calendar.DATE, 1)
            while (!isSameDay(otherDay, event.endTime)) {
                val overDay = otherDay.clone() as Calendar
                overDay.set(Calendar.HOUR_OF_DAY, 0)
                overDay.set(Calendar.MINUTE, 0)
                val endOfOverDay = overDay.clone() as Calendar
                endOfOverDay.set(Calendar.HOUR_OF_DAY, 23)
                endOfOverDay.set(Calendar.MINUTE, 59)
                val eventMore = WeekViewEvent(event.id, event.name, overDay, endOfOverDay)
                eventMore.color = event.color
                eventRects.add(EventRect(eventMore, event, null))

                // Add next day.
                otherDay.add(Calendar.DATE, 1)
            }

            // Add last day.
            val startTime = event.endTime?.clone() as Calendar
            startTime.set(Calendar.HOUR_OF_DAY, 0)
            startTime.set(Calendar.MINUTE, 0)
            val event2 = WeekViewEvent(event.id, event.name, startTime, event.endTime, event.location)
            event2.color = event.color
            eventRects.add(EventRect(event2, event, null))
        } else {
            eventRects.add(EventRect(event, event, null))
        }
    }

    /**
     * Sorts the events in ascending order.
     * @param events The events to be sorted.
     */
    private fun sortEvents(events: List<WeekViewEvent>) {
        Collections.sort(events) { event1, event2 ->
            val start1 = event1.startTime?.timeInMillis ?: 0L
            val start2 = event2.startTime?.timeInMillis ?: 0L
            var comparator = if (start1 > start2) 1 else if (start1 < start2) -1 else 0
            if (comparator == 0) {
                val end1 = event1.endTime?.timeInMillis ?: 0L
                val end2 = event2.endTime?.timeInMillis ?: 0L
                comparator = if (end1 > end2) 1 else if (end1 < end2) -1 else 0
            }
            comparator
        }
    }

    /**
     * A class to hold reference to the events and their visual representation. An EventRect is
     * actually the rectangle that is drawn on the calendar for a given event. There may be more
     * than one rectangle for a single event (an event that expands more than one day). In that
     * case two instances of the EventRect will be used for a single event. The given event will be
     * stored in "originalEvent". But the event that corresponds to rectangle the rectangle
     * instance will be stored in "event".
     */
    private inner class EventRect
    /**
     * Create a new instance of event rect. An EventRect is actually the rectangle that is drawn
     * on the calendar for a given event. There may be more than one rectangle for a single
     * event (an event that expands more than one day). In that case two instances of the
     * EventRect will be used for a single event. The given event will be stored in
     * "originalEvent". But the event that corresponds to rectangle the rectangle instance will
     * be stored in "event".
     * @param event Represents the event which this instance of rectangle represents.
     * @param originalEvent The original event that was passed by the user.
     * @param rectF The rectangle.
     */
    (var event: WeekViewEvent, var originalEvent: WeekViewEvent, var rectF: RectF?) {
        var left: Float = 0.toFloat()
        var width: Float = 0.toFloat()
        var top: Float = 0.toFloat()
        var bottom: Float = 0.toFloat()
    }

    private inner class DateTimeInterpreterImpl {
        //: DateTimeInterpreter {
        //OK
        fun interpretTime(hour: Int): String {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, 0)

            return try {
                val sdf = if (DateFormat.is24HourFormat(context)) {
                    SimpleDateFormat("HH:mm", Locale.getDefault())
                } else {
                    SimpleDateFormat("hh a", Locale.getDefault())
                }
                sdf.format(calendar.time)
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }

        }

        //OK BUT
        fun interpretDate(date: Calendar?): String {
            return try {
                val sdf = if (dayNameLength == LENGTH_SHORT) {
                    SimpleDateFormat("EEEEE M/dd", Locale.getDefault())
                } else {
                    SimpleDateFormat("M/dd EEE ", Locale.getDefault())
                }
                sdf.format(date?.time).toUpperCase()
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }

        }
    }

    inner class ScrollListenerImpl : ScrollListener {
        override fun onFirstVisibleDayChanged(newFirstVisibleDay: Calendar, oldFirstVisibleDay: Calendar) {
            Log.d("@@@@@@@@@@@@@@", "onFirstVisibleDayChanged ")
        }
    }

    inner class EventClickListenerImpl : EventClickListener {
        override fun onEventClick(event: WeekViewEvent, eventRect: RectF) {
            Log.d("@@@@@@@@@@@@@@", "Clicked " + event.name)
        }
    }

    inner class EmptySpaceClickListenerImpl : EmptySpaceClickListener {
        override fun onEmptySpaceClicked(time: Calendar, staff: String) {
            //TODO To change body of created functions use File | Settings | File Templates.
        }
    }

    private inner class SimpleOnGestureListenerImpl : GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapConfirmed(motionEvent: MotionEvent?): Boolean {
            //super.onSingleTapConfirmed(e)
            if (motionEvent == null) {
                return false
            }
            // If the tap was on an event then trigger the callback.
            val reversedEventRects = eventRects
            reversedEventRects.reverse()
            val x = motionEvent.x
            val y = motionEvent.y
            for (event in reversedEventRects) {
                if (event.rectF != null && x > (event.rectF as RectF).left && x < (event.rectF as RectF).right && y > (event.rectF as RectF).top && y < (event.rectF as RectF).bottom) {
                    eventClickListener.onEventClick(event.originalEvent, (event.rectF as RectF))
                    playSoundEffect(SoundEffectConstants.CLICK)
                    return true//super.onSingleTapConfirmed(e)
                }
            }
//            if (eventRects != null && eventClickListener != null) {
//            }

            // If the tap was on in an empty space, then trigger the callback.
            if (x > headerColumnWidth && y > headerTextHeight + (headerRowPadding * 2).toFloat() + headerMarginBottom) {
                val selectedTime = getTimeForPoint(x, y)
                val selectedStaff = getEmployeeForPoint(x, y)
                if (selectedTime != null) {
                    playSoundEffect(SoundEffectConstants.CLICK)
                    emptySpaceClickListener.onEmptySpaceClicked(time = selectedTime, staff = selectedStaff)
                }
            }

            return super.onSingleTapConfirmed(motionEvent)
        }

        override fun onShowPress(e: MotionEvent?) {
            //TODO To change body of created functions use File | Settings | File Templates.
        }

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            //TODO To change body of created functions use File | Settings | File Templates.
            Log.d("@@@@@@@@@@@@@@", "onSingleTapUp")
            return true
        }

        override fun onDown(e: MotionEvent?): Boolean {
//            goToNearestOrigin()
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
//            if (isZooming)
//                return true
//
//            if (currentFlingDirection == LEFT && !horizontalFlingEnabled ||
//                    currentFlingDirection == RIGHT && !horizontalFlingEnabled ||
//                    currentFlingDirection == VERTICAL && !verticalFlingEnabled) {
//                return true
//            }
//
//            scroller.forceFinished(true)
//
//            currentFlingDirection = currentScrollDirection
//            when (currentFlingDirection) {
//                LEFT, RIGHT -> scroller.fling(currentOrigin.x.toInt(), currentOrigin.y.toInt(), (velocityX * xScrollingSpeed).toInt(), 0, Integer.MIN_VALUE, Integer.MAX_VALUE, (-((hourHeight * 24).toFloat() + headerTextHeight + (headerRowPadding * 2).toFloat() + headerMarginBottom + timeTextHeight / 2 - height)).toInt(), 0)
//                VERTICAL -> scroller.fling(currentOrigin.x.toInt(), currentOrigin.y.toInt(), 0, velocityY.toInt(), Integer.MIN_VALUE, Integer.MAX_VALUE, (-((hourHeight * 24).toFloat() + headerTextHeight + (headerRowPadding * 2).toFloat() + headerMarginBottom + timeTextHeight / 2 - height)).toInt(), 0)
//            }
//
//            ViewCompat.postInvalidateOnAnimation(this@CalendarEmployeeView)
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            // Check if view is zoomed.
            if (isZooming)
                return true

            when (currentScrollDirection) {
                NONE -> {
                    // Allow scrolling only in one direction.
                    currentScrollDirection = if (Math.abs(distanceX) > Math.abs(distanceY)) {
                        if (distanceX > 0) {
                            LEFT
                        } else {
                            RIGHT
                        }
                    } else {
                        VERTICAL
                    }
                }
//                LEFT -> {
//                    // Change direction if there was enough change.
//                    if (Math.abs(distanceX) > Math.abs(distanceY) && distanceX < -scaledTouchSlop) {
//                        currentScrollDirection = RIGHT
//                    }
//                }
//                RIGHT -> {
//                    // Change direction if there was enough change.
//                    if (Math.abs(distanceX) > Math.abs(distanceY) && distanceX > scaledTouchSlop) {
//                        currentScrollDirection = LEFT
//                    }
//                }
            }

            // Calculate the new origin after scroll.
            when (currentScrollDirection) {
                LEFT, RIGHT -> {
                    currentOrigin.x -= distanceX * xScrollingSpeed
                    ViewCompat.postInvalidateOnAnimation(this@CalendarEmployeeView)
                }
                VERTICAL -> {
                    currentOrigin.y -= distanceY * yScrollingSpeed
                    ViewCompat.postInvalidateOnAnimation(this@CalendarEmployeeView)
                }
            }
            return true
        }

        override fun onLongPress(e: MotionEvent?) {
            //TODO To change body of created functions use File | Settings | File Templates.
        }
    }
}
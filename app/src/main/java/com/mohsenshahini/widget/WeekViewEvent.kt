package com.mohsenshahini.widget

import java.util.*

data class WeekViewEvent(
        var id: Long = 0,
        var name: String = "",
        var startTime: Calendar? = null,
        var endTime: Calendar? = null) {

    var color: Int = 0
    var location: String = ""

    constructor(
            id: Long = 0,
            name: String = "",
            startTime: Calendar? = null,
            endTime: Calendar? = null,
            location: String = "",
            color: Int = 0) : this (id, name, startTime, endTime)
}



    /**
     * Initializes the event for week view.
     * @param id The id of the event.
     * @param name Name of the event.
     * @param startYear Year when the event starts.
     * @param startMonth Month when the event starts.
     * @param startDay Day when the event starts.
     * @param startHour Hour (in 24-hour format) when the event starts.
     * @param startMinute Minute when the event starts.
     * @param endYear Year when the event ends.
     * @param endMonth Month when the event ends.
     * @param endDay Day when the event ends.
     * @param endHour Hour (in 24-hour format) when the event ends.
     * @param endMinute Minute when the event ends.
     */
//    constructor(id: Long, name: String, startYear: Int, startMonth: Int, startDay: Int, startHour: Int, startMinute: Int, endYear: Int, endMonth: Int, endDay: Int, endHour: Int, endMinute: Int) {
//        this.mId = id
//
//        this.mStartTime = Calendar.getInstance()
//        this.mStartTime!!.set(Calendar.YEAR, startYear)
//        this.mStartTime!!.set(Calendar.MONTH, startMonth - 1)
//        this.mStartTime!!.set(Calendar.DAY_OF_MONTH, startDay)
//        this.mStartTime!!.set(Calendar.HOUR_OF_DAY, startHour)
//        this.mStartTime!!.set(Calendar.MINUTE, startMinute)
//
//        this.mEndTime = Calendar.getInstance()
//        this.mEndTime!!.set(Calendar.YEAR, endYear)
//        this.mEndTime!!.set(Calendar.MONTH, endMonth - 1)
//        this.mEndTime!!.set(Calendar.DAY_OF_MONTH, endDay)
//        this.mEndTime!!.set(Calendar.HOUR_OF_DAY, endHour)
//        this.mEndTime!!.set(Calendar.MINUTE, endMinute)
//
//        this.mName = name
//    }

    /**
     * Initializes the event for week view.
     * @param id The id of the event.
     * @param name Name of the event.
     * @param location The location of the event.
     * @param startTime The time when the event starts.
     * @param endTime The time when the event ends.
     */
//    fun WeekViewEvent(id: Long, name: String, location: String?, startTime: Calendar, endTime: Calendar): ??? {
//        this.mId = id
//        this.mName = name
//        this.mLocation = location
//        this.mStartTime = startTime
//        this.mEndTime = endTime
//    }

    /**
     * Initializes the event for week view.
     * @param id The id of the event.
     * @param name Name of the event.
     * @param startTime The time when the event starts.
     * @param endTime The time when the event ends.
     */
//    fun WeekViewEvent(id: Long, name: String, startTime: Calendar, endTime: Calendar): ??? {
//        this(id, name, null, startTime, endTime)
//    }


//    fun getStartTime(): Calendar? {
//        return startTime
//    }
//
//    fun setStartTime(startTime: Calendar) {
//        this.mStartTime = startTime
//    }
//
//    fun getEndTime(): Calendar? {
//        return mEndTime
//    }
//
//    fun setEndTime(endTime: Calendar) {
//        this.mEndTime = endTime
//    }
//
//    fun getName(): String? {
//        return mName
//    }
//
//    fun setName(name: String) {
//        this.mName = name
//    }
//
//    fun getLocation(): String? {
//        return mLocation
//    }
//
//    fun setLocation(location: String) {
//        this.mLocation = location
//    }
//
//    fun getColor(): Int {
//        return mColor
//    }
//
//    fun setColor(color: Int) {
//        this.mColor = color
//    }
//
//    fun getId(): Long {
//        return mId
//    }
//
//    fun setId(id: Long) {
//        this.mId = id
//    }
//
//    override fun equals(o: Any?): Boolean {
//        if (this === o) return true
//        if (o == null || javaClass != o.javaClass) return false
//
//        val that = o as WeekViewEvent?
//
//        return mId == that!!.mId
//
//    }
//
//    override fun hashCode(): Int {
//        return (mId xor mId.ushr(32)).toInt()
//    }

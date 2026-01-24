package java.util;

/**
 * GWT polyfill for java.util.GregorianCalendar
 * Uses JavaScript Date object
 */
public class GregorianCalendar extends Calendar {
    private static final long serialVersionUID = 1L;
    
    private transient native JSDate jsDate;
    
    public GregorianCalendar() {
        this(TimeZone.getDefault(), Locale.getDefault());
    }
    
    public GregorianCalendar(TimeZone zone) {
        this(zone, Locale.getDefault());
    }
    
    public GregorianCalendar(Locale aLocale) {
        this(TimeZone.getDefault(), aLocale);
    }
    
    public GregorianCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
        this.jsDate = createJSDate();
        computeTime();
        computeFields();
    }
    
    public GregorianCalendar(int year, int month, int dayOfMonth) {
        this(year, month, dayOfMonth, 0, 0, 0);
    }
    
    public GregorianCalendar(int year, int month, int dayOfMonth, int hourOfDay, int minute) {
        this(year, month, dayOfMonth, hourOfDay, minute, 0);
    }
    
    public GregorianCalendar(int year, int month, int dayOfMonth, int hourOfDay, int minute, int second) {
        super(TimeZone.getDefault(), Locale.getDefault());
        this.jsDate = createJSDate(year, month, dayOfMonth, hourOfDay, minute, second);
        computeTime();
        computeFields();
    }
    
    private native JSDate createJSDate() /*-{
        return new Date();
    }-*/;
    
    private native JSDate createJSDate(int year, int month, int day, int hour, int minute, int second) /*-{
        return new Date(year, month, day, hour, minute, second);
    }-*/;
    
    @Override
    protected void computeTime() {
        // Time is computed from fields
    }
    
    @Override
    protected void computeFields() {
        // Fields are computed from time
        internalSet(YEAR, getNativeYear());
        internalSet(MONTH, getNativeMonth());
        internalSet(DAY_OF_MONTH, getNativeDay());
        internalSet(HOUR_OF_DAY, getNativeHours());
        internalSet(MINUTE, getNativeMinutes());
        internalSet(SECOND, getNativeSeconds());
        internalSet(MILLISECOND, getNativeMilliseconds());
    }
    
    private native int getNativeYear() /*-{
        return this.@java.util.GregorianCalendar::jsDate.getFullYear();
    }-*/;
    
    private native int getNativeMonth() /*-{
        return this.@java.util.GregorianCalendar::jsDate.getMonth();
    }-*/;
    
    private native int getNativeDay() /*-{
        return this.@java.util.GregorianCalendar::jsDate.getDate();
    }-*/;
    
    private native int getNativeHours() /*-{
        return this.@java.util.GregorianCalendar::jsDate.getHours();
    }-*/;
    
    private native int getNativeMinutes() /*-{
        return this.@java.util.GregorianCalendar::jsDate.getMinutes();
    }-*/;
    
    private native int getNativeSeconds() /*-{
        return this.@java.util.GregorianCalendar::jsDate.getSeconds();
    }-*/;
    
    private native int getNativeMilliseconds() /*-{
        return this.@java.util.GregorianCalendar::jsDate.getMilliseconds();
    }-*/;
    
    /**
     * Check if the year is a leap year
     */
    public boolean isLeapYear(int year) {
        if (year % 4 != 0) {
            return false;
        } else if (year % 100 != 0) {
            return true;
        } else {
            return year % 400 == 0;
        }
    }
    
    /**
     * Get the maximum value for a field
     */
    public int getActualMaximum(int field) {
        switch (field) {
            case DAY_OF_MONTH:
                int month = get(MONTH);
                int year = get(YEAR);
                if (month == Calendar.FEBRUARY) {
                    return isLeapYear(year) ? 29 : 28;
                } else if (month == Calendar.APRIL || month == Calendar.JUNE ||
                           month == Calendar.SEPTEMBER || month == Calendar.NOVEMBER) {
                    return 30;
                } else {
                    return 31;
                }
            default:
                return getMaximum(field);
        }
    }
    
    /**
     * Add to a field
     */
    public void add(int field, int amount) {
        switch (field) {
            case YEAR:
                set(YEAR, get(YEAR) + amount);
                break;
            case MONTH:
                set(MONTH, get(MONTH) + amount);
                break;
            case DAY_OF_MONTH:
            case DAY_OF_YEAR:
                set(DAY_OF_MONTH, get(DAY_OF_MONTH) + amount);
                break;
            case HOUR:
            case HOUR_OF_DAY:
                set(HOUR_OF_DAY, get(HOUR_OF_DAY) + amount);
                break;
            case MINUTE:
                set(MINUTE, get(MINUTE) + amount);
                break;
            case SECOND:
                set(SECOND, get(SECOND) + amount);
                break;
            case MILLISECOND:
                set(MILLISECOND, get(MILLISECOND) + amount);
                break;
        }
        computeTime();
        computeFields();
    }
    
    /**
     * Roll a field
     */
    public void roll(int field, boolean up) {
        roll(field, up ? 1 : -1);
    }
    
    /**
     * Roll a field by amount
     */
    public void roll(int field, int amount) {
        int max = getMaximum(field);
        int min = getMinimum(field);
        int value = get(field);
        
        value += amount;
        while (value > max) {
            value -= (max - min + 1);
        }
        while (value < min) {
            value += (max - min + 1);
        }
        
        set(field, value);
        computeTime();
        computeFields();
    }
    
    /**
     * Set the date
     */
    public void set(int year, int month, int date) {
        set(YEAR, year);
        set(MONTH, month);
        set(DAY_OF_MONTH, date);
        computeTime();
        computeFields();
    }
    
    /**
     * Set the date and time
     */
    public void set(int year, int month, int date, int hourOfDay, int minute) {
        set(year, month, date, hourOfDay, minute, 0);
    }
    
    /**
     * Set the date and time
     */
    public void set(int year, int month, int date, int hourOfDay, int minute, int second) {
        set(YEAR, year);
        set(MONTH, month);
        set(DAY_OF_MONTH, date);
        set(HOUR_OF_DAY, hourOfDay);
        set(MINUTE, minute);
        set(SECOND, second);
        computeTime();
        computeFields();
    }
    
    /**
     * Get time in milliseconds
     */
    public long getTimeInMillis() {
        return getNativeTime();
    }
    
    private native long getNativeTime() /*-{
        return this.@java.util.GregorianCalendar::jsDate.getTime();
    }-*/;
    
    /**
     * Set time in milliseconds
     */
    public void setTimeInMillis(long millis) {
        setNativeTime(millis);
        computeFields();
    }
    
    private native void setNativeTime(long millis) /*-{
        this.@java.util.GregorianCalendar::jsDate.setTime(millis);
    }-*/;
    
    /**
     * Clone this calendar
     */
    @Override
    public Object clone() {
        GregorianCalendar other = (GregorianCalendar) super.clone();
        other.jsDate = cloneJSDate();
        return other;
    }
    
    private native JSDate cloneJSDate() /*-{
        return new Date(this.@java.util.GregorianCalendar::jsDate.getTime());
    }-*/;
    
    /**
     * Get timezone offset
     */
    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds) {
        return getNativeTimezoneOffset();
    }
    
    private native int getNativeTimezoneOffset() /*-{
        return -this.@java.util.GregorianCalendar::jsDate.getTimezoneOffset() * 60000;
    }-*/;
}
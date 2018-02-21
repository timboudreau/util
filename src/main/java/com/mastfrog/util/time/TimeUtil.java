/*
 * The MIT License
 *
 * Copyright 2017 Tim Boudreau.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.mastfrog.util.time;

import com.mastfrog.util.Checks;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A few utility methods to reduce the boilerplate involved in using JDK 8's
 * time API, mostly for dealing with ISO 2822, the format used in HTTP headers,
 * e.g. <code>Mon, 29 May 2017 03:33:57 -04:00</code>.
 *
 * @author Tim Boudreau
 */
public class TimeUtil {

    public static final DateTimeFormatter ISO_INSTANT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendInstant()
            .toFormatter(Locale.US);

    public static final ZoneId GMT = ZoneId.of("GMT");

    public static final ZonedDateTime EPOCH
            = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), GMT);

    private TimeUtil() {
    }

    public static final DateTimeFormatter ISO2822DateFormat = new DateTimeFormatterBuilder()
            .appendText(ChronoField.DAY_OF_WEEK, TextStyle.SHORT_STANDALONE).appendLiteral(", ")
            .appendText(ChronoField.DAY_OF_MONTH, TextStyle.FULL).appendLiteral(" ")
            .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT).appendLiteral(" ")
            .appendText(ChronoField.YEAR, TextStyle.FULL).appendLiteral(" ")
            .appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral(":")
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral(":")
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2).appendLiteral(" ")
            .appendOffsetId().toFormatter();

    private static final DateTimeFormatter SORTABLE_STRING_FORMAT = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4).appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2).appendLiteral('.')
            .appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral('-')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral('-')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2).appendLiteral('-')
            .appendValue(ChronoField.NANO_OF_SECOND, 9)
            .toFormatter();

    private static final Pattern SORTABLE = Pattern.compile("^([\\d]{4}-[\\-\\d\\.]*\\.)(\\d+).*?");

    /**
     * Generates dates such as "2012-12-25.00-24-01-00000000" which can be used
     * for file names and sorted.
     *
     * @param zdt
     * @return
     */
    public static String toSortableStringFormat(ZonedDateTime zdt) {
        return zdt.format(SORTABLE_STRING_FORMAT);
    }

    /**
     * Gets a sortable date format from a string. Allows trailing content, so
     * file names can be directly converted.
     *
     * @param s The input string
     * @return A zoned date time
     */
    public static ZonedDateTime fromSortableDateFormat(String s) {
        Matcher m = SORTABLE.matcher(s);
        if (m.matches()) {
            s = m.group(1) + m.group(2) + m.group(3);
        }
        return ZonedDateTime.parse(s, SORTABLE_STRING_FORMAT);
    }

    public static String toIsoFormat(ZonedDateTime zdt) {
        return zdt.format(ISO_INSTANT);
    }

    public static String toIsoFormat(LocalDateTime ldt) {
        ZoneOffset off = ZoneOffset.systemDefault().getRules().getOffset(ldt);
        Instant inst = ldt.toInstant(off);
        return toIsoFormat(inst);
    }

    public static String toIsoFormat(long unixTimestamp) {
        return toIsoFormat(fromUnixTimestamp(unixTimestamp));
    }

    public static String toIsoFormat(OffsetDateTime odt) {
        return toIsoFormat(fromUnixTimestamp(toUnixTimestamp(odt)));
    }

    public static String toIsoFormat(Instant inst) {
        return toIsoFormat(fromUnixTimestamp(inst.toEpochMilli()));
    }

    public static String toIsoFormat(Date date) {
        return toIsoFormat(toZonedDateTime(date));
    }

    public static ZonedDateTime fromIsoFormat(String fmt) {
        return ZonedDateTime.parse(fmt);
    }

    public static long timestampFromIsoFormat(String fmt) {
        ZonedDateTime zdt = fromIsoFormat(fmt);
        return toUnixTimestamp(zdt);
    }

    public static OffsetDateTime offsetFromIsoFormat(String fmt) {
        ZonedDateTime zdt = fromIsoFormat(fmt);
        return zdt.toOffsetDateTime();
    }

    public static LocalDateTime localFromIsoFormat(String fmt) {
        ZonedDateTime zdt = fromIsoFormat(fmt);
        return LocalDateTime.ofInstant(zdt.toInstant(), ZoneOffset.systemDefault().getRules().getOffset(zdt.toInstant()));
    }

    public static LocalDateTime localFromIsoFormatGMT(String fmt) {
        ZonedDateTime zdt = fromIsoFormat(fmt);
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(toUnixTimestamp(zdt)), GMT);
    }

    public static Instant instantFromIsoFormat(String fmt) {
        return fromIsoFormat(fmt).toInstant();
    }

    public static boolean equals(ZonedDateTime a, ZonedDateTime b) {
        if (a == b) {
            return true;
        }
        if ((a == null) && (b == null)) {
            return true;
        }
        if ((a == null) != (b == null)) {
            return false;
        }
        return toUnixTimestamp(a) == toUnixTimestamp(b);
    }

    public static boolean equals(OffsetDateTime a, OffsetDateTime b) {
        if (a == b) {
            return true;
        }
        if ((a == null) && (b == null)) {
            return true;
        }
        if ((a == null) != (b == null)) {
            return false;
        }
        return toUnixTimestamp(a) == toUnixTimestamp(b);
    }

    public static boolean equals(LocalDateTime a, LocalDateTime b) {
        if (a == b) {
            return true;
        }
        if ((a == null) && (b == null)) {
            return true;
        }
        if ((a == null) != (b == null)) {
            return false;
        }
        return toUnixTimestampGMT(a) == toUnixTimestampGMT(b);
    }

    public static boolean equals(OffsetDateTime a, ZonedDateTime b) {
        if ((a == null) && (b == null)) {
            return true;
        }
        if ((a == null) != (b == null)) {
            return false;
        }
        return toUnixTimestamp(a) == toUnixTimestamp(b);
    }

    public static boolean equals(LocalDateTime a, ZonedDateTime b) {
        if ((a == null) && (b == null)) {
            return true;
        }
        if ((a == null) != (b == null)) {
            return false;
        }
        return toUnixTimestampSystemDefault(a) == toUnixTimestamp(b);
    }

    public static boolean equals(LocalDateTime a, OffsetDateTime b) {
        if ((a == null) && (b == null)) {
            return true;
        }
        if ((a == null) != (b == null)) {
            return false;
        }
        return toUnixTimestampSystemDefault(a) == toUnixTimestamp(b);
    }

    public static ZonedDateTime toZonedDateTime(Date date) {
        return fromUnixTimestamp(date.getTime());
    }

    public static Date toDate(ZonedDateTime time) {
        return new Date(time.toInstant().toEpochMilli());
    }

    public static Date toDate(OffsetDateTime odt) {
        return new Date(odt.toInstant().toEpochMilli());
    }

    public static Date toDate(LocalDateTime ldt) {
        ZoneOffset offset = ZoneOffset.systemDefault().getRules().getOffset(ldt);
        return new Date(ldt.toInstant(offset).toEpochMilli());
    }

    public static ZonedDateTime nowGMT() {
        return ZonedDateTime.now().withZoneSameInstant(GMT);
    }

    /**
     * Get a ZonedDateTime from ISO 2822 HTTP header formatted date.
     *
     * @param iso2822dateTime The date
     * @return A zoned date time
     */
    public static ZonedDateTime fromHttpHeaderFormat(String iso2822dateTime) {
        Checks.notNull("iso2822dateTime", iso2822dateTime);
        return ZonedDateTime.parse(iso2822dateTime, ISO2822DateFormat);
    }

    /**
     * Convert a unix timestamp to an ISO 2822 HTTP header format date.
     *
     * @param timestamp
     * @return A formatted timestamp
     */
    public static String toHttpHeaderFormat(long timestamp) {
        return TimeUtil.toHttpHeaderFormat(fromUnixTimestamp(timestamp));
    }

    /**
     * Get a unix timestamp from an ISO 2822 formatted timestamp.
     *
     * @param iso2822dateTime The timestamp
     * @return a timestamp as epoch millis
     */
    public static long timestampFromHttpHeaderFormat(String iso2822dateTime) {
        Checks.notNull("iso2822dateTime", iso2822dateTime);
        return toUnixTimestamp(fromHttpHeaderFormat(iso2822dateTime));
    }

    /**
     * Convert a ZonedDateTime to epoch millis.
     *
     * @param dateTime A zoned date time
     * @return epoch millis
     */
    public static long toUnixTimestamp(ZonedDateTime dateTime) {
        Checks.notNull("dateTime", dateTime);
        return dateTime.toInstant().toEpochMilli();
    }

    /**
     * Convert a LocalDateTime to epoch millis, assuming time zone GMT.
     *
     * @param dateTime A zoned date time
     * @return epoch millis
     */
    public static long toUnixTimestampGMT(LocalDateTime dateTime) {
        Checks.notNull("dateTime", dateTime);
        return dateTime.toInstant(GMT.getRules().getOffset(dateTime)).toEpochMilli();
    }

    /**
     * Convert a LocalDateTime to epoch millis, assuming time zone GMT.
     *
     * @param dateTime A zoned date time
     * @return epoch millis
     */
    public static long toUnixTimestampSystemDefault(LocalDateTime dateTime) {
        Checks.notNull("dateTime", dateTime);
        return dateTime.toInstant(ZoneId.systemDefault().getRules().getOffset(dateTime)).toEpochMilli();
    }

    /**
     * Convert an OffsetDateTime to an epoch millis unix timestamp.
     *
     * @param dt An OffsetDateTime
     * @return a long representing millis since 1/1/1970
     */
    public static long toUnixTimestamp(OffsetDateTime dt) {
        return dt.toInstant().toEpochMilli();
    }

    /**
     * Convert a ZonedDateTime to ISO 2822 format.
     *
     * @param dateTime A date time
     * @return a formatted date time
     */
    public static String toHttpHeaderFormat(ZonedDateTime dateTime) {
        Checks.notNull("dateTime", dateTime);
        return ISO2822DateFormat.format(dateTime);
    }

    /**
     * Convert a unix timestamp (epoch milliseconds) to a ZonedDateTime using
     * the current system time zone.
     *
     * @param timestamp A unix timestamp
     * @return A zoned date time
     */
    public static ZonedDateTime fromUnixTimestamp(long timestamp) {
        ZonedDateTime ldt = fromUnixTimestamp(timestamp, ZoneId.systemDefault());
        return ldt;
    }

    /**
     * Convert a unix timestamp (epoch milliseconds) to a LocalDateTime using
     * the current system time zone.
     *
     * @param timestamp A unix timestamp
     * @return A zoned date time
     */
    public static LocalDateTime localFromUnixTimestamp(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

    /**
     * Convert a unix timestamp (epoch milliseconds) to a LocalDateTime using
     * the current system time zone.
     *
     * @param timestamp A unix timestamp
     * @return A zoned date time
     */
    public static LocalDateTime localFromUnixTimestampGMT(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), GMT);
    }

    /**
     * Convert a unix timestamp (epoch milliseconds) to a LocalDateTime using
     * the current system time zone.
     *
     * @param timestamp A unix timestamp
     * @return A zoned date time
     */
    public static OffsetDateTime offsetFromUnixTimestamp(long timestamp) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

    /**
     * Convert a unix timestamp (epoch milliseconds) to a ZonedDateTime using
     * the passed time zone id.
     *
     * @param timestamp A unix timestamp
     * @param timeZoneId A time zone id
     * @return a zoned date time
     */
    public static ZonedDateTime fromUnixTimestamp(long timestamp, ZoneId timeZoneId) {
        Checks.notNull("timeZoneId", timeZoneId);
        ZonedDateTime ldt = Instant.ofEpochMilli(timestamp).atZone(timeZoneId);
        return ldt;
    }

    public static Duration minutes(int minutes) {
        Checks.nonNegative("minutes", minutes);
        return Duration.ofMinutes(minutes);
    }

    public static Duration minutes(long minutes) {
        Checks.nonNegative("minutes", minutes);
        return Duration.ofMinutes(minutes);
    }

    public static Duration seconds(int seconds) {
        Checks.nonNegative("seconds", seconds);
        return Duration.ofSeconds(seconds);
    }

    public static Duration seconds(long seconds) {
        Checks.nonNegative("seconds", seconds);
        return Duration.ofSeconds(seconds);
    }

    public static Duration millis(int milliseconds) {
        Checks.nonNegative("milliseconds", milliseconds);
        return Duration.of(milliseconds, ChronoUnit.MILLIS);
    }

    public static Duration millis(long milliseconds) {
        Checks.nonNegative("milliseconds", milliseconds);
        return Duration.of(milliseconds, ChronoUnit.MILLIS);
    }

    public static Duration days(int days) {
        Checks.nonNegative("days", days);
        return Duration.ofDays(days);
    }

    public static Duration days(long days) {
        Checks.nonNegative("days", days);
        return Duration.ofDays(days);
    }

    public static Duration years(long years) {
        Checks.nonNegative("years", years);
        return Duration.ofDays(365 * years);
    }

    public static Duration years(int years) {
        Checks.nonNegative("years", years);
        return Duration.ofDays(365 * years);
    }

    public static long millis(Duration duration) {
        Checks.notNull("duration", duration);
        return duration.toMillis();
    }

    public static long seconds(Duration duration) {
        Checks.notNull("duration", duration);
        return duration.toMillis() / 1_000;
    }

    private static final NumberFormat TWO_DIGITS = new DecimalFormat("00");
    private static final NumberFormat THREE_DIGITS = new DecimalFormat("000");
    private static final NumberFormat MANY_DIGITS = new DecimalFormat("######00");

    public static String format(Duration dur) {
        return format(dur, false);
    }

    public static String format(Duration dur, boolean includeAllFields) {
        long days = dur.toDays();
        long hours = dur.toHours() % 24;
        long minutes = dur.toMinutes() % 60;
        long seconds = 0;
        long millis = 0;
        try {
            seconds = (dur.toMillis() / 1_000) % 60;
            millis = dur.toMillis() % 1_000;
        } catch (Exception e) {
            seconds = 0;
        }

        StringBuilder sb = new StringBuilder();
        appendComponent(sb, days, ':', MANY_DIGITS, includeAllFields ? ChronoUnit.MILLIS : ChronoUnit.DAYS);
        appendComponent(sb, hours, ':', TWO_DIGITS, includeAllFields ? ChronoUnit.MILLIS : ChronoUnit.HOURS);
        appendComponent(sb, minutes, ':', TWO_DIGITS, includeAllFields ? ChronoUnit.MILLIS : ChronoUnit.MINUTES);
        appendComponent(sb, seconds, ':', TWO_DIGITS, ChronoUnit.SECONDS);
        appendComponent(sb, millis, '.', THREE_DIGITS, ChronoUnit.MILLIS);
        return sb.toString();
    }

    public static Duration parse(String val) {
        long[] vals = new long[5];
        char[] chars = val.toCharArray();
        int ix = vals.length - 1;
        long position = 1;
        for (int i = chars.length - 1; i >= 0; i--) {
            if (ix < 0) {
                throw new IllegalArgumentException("Too many fields in '" + val + "'");
            }
            char c = chars[i];
            switch (c) {
                case ':':
                case '.':
                    ix--;
                    position = 1;
                    continue;
                default:
                    vals[ix] += position * (c - '0');
                    position *= 10;
                    if (i == 0) {
                        continue;
                    }
            }
            if (i == 0) {
                vals[ix] += position * (c - '0');
            }
        }
        if (vals[0] > 106_751_991_167_299L) {
            throw new IllegalArgumentException("Duration.ofDays() cannot handle values larger "
                    + "than 106,751,991,167,299 days and still contain hours/minutes/seconds/millis");
        }
        Duration result = Duration.ofDays(vals[0]);
        for (int i = 1; i < 5; i++) {
            if (vals[i] == 0L) {
                continue;
            }
            switch (i) {
                case 1:
                    result = result.plus(Duration.ofHours(vals[i]));
                    break;
                case 2:
                    result = result.plus(Duration.ofMinutes(vals[i]));
                    break;
                case 3:
                    result = result.plus(Duration.ofSeconds(vals[i]));
                    break;
                case 4:
                    result = result.plus(Duration.ofMillis(vals[i]));
                    break;
                default:
                    throw new AssertionError(i);
            }
        }
        return result;
    }

    private static void appendComponent(StringBuilder sb, long val, char delim, NumberFormat fmt, ChronoUnit unit) {
        boolean use = ChronoUnit.SECONDS == unit || ChronoUnit.MILLIS == unit || val > 0 || sb.length() > 0;
        if (use) {
            if (sb.length() != 0) {
                sb.append(delim);
            }
            sb.append(fmt.format(val));
        }
    }

    public static boolean isLonger(Duration test, Duration other) {
        return test.toMillis() > other.toMillis();
    }

    public static boolean isShorter(Duration test, Duration other) {
        return test.toMillis() < other.toMillis();
    }

    public static boolean isAfterEqualOrNullSecondsResolution(ZonedDateTime when, ZonedDateTime test) {
        if (test == null) {
            return true;
        }
        when = when.withSecond(0);
        test = test.withSecond(0);
        return test.isAfter(when) || test.toInstant().equals(when.toInstant());
    }

    public static boolean isBeforeEqualOrNullSecondsResolution(ZonedDateTime when, ZonedDateTime test) {
        if (test == null) {
            return true;
        }
        when = when.withSecond(0);
        test = test.withSecond(0);
        return test.isBefore(when) || test.toInstant().equals(when.toInstant());
    }

    /**
     * The maximum Duration possible before arithmetic overflow.
     */
    public static final Duration MAX_DURATION = Duration.ofDays(106751991167300L);
    /**
     * The maximum Duration less ten years, for cases where it will be added to.
     */
    public static final Duration MAX_SAFE_DURATION = Duration.ofDays(106751991167300L).minus(Duration.ofDays(365 * 10));

}

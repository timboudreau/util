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
package com.mastfrog.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import static java.time.ZonedDateTime.now;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;

/**
 * A few utility methods to reduce the boilerplate involved in using JDK 8's
 * time API, mostly for dealing with ISO 2822, the format used in HTTP headers,
 * e.g. <code>Mon, 29 May 2017 03:33:57 -04:00</code>.
 *
 * @author Tim Boudreau
 */
public class TimeUtil {

    private TimeUtil() {
    }

    private static final DateTimeFormatter ISO2822DateFormat = new DateTimeFormatterBuilder()
            .appendText(ChronoField.DAY_OF_WEEK, TextStyle.SHORT_STANDALONE).appendLiteral(", ")
            .appendText(ChronoField.DAY_OF_MONTH, TextStyle.FULL).appendLiteral(" ")
            .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT).appendLiteral(" ")
            .appendText(ChronoField.YEAR, TextStyle.FULL).appendLiteral(" ")
            .appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral(":")
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral(":")
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2).appendLiteral(" ")
            .appendOffsetId().toFormatter();

    /**
     * Get a ZonedDateTime from ISO 2822 HTTP header formatted date.
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
     * Convert a unix timestamp (epoch milliseconds) to a ZonedDateTime
     * using the current system time zone.
     * 
     * @param timestamp A unix timestamp
     * @return A zoned date time
     */
    public static ZonedDateTime fromUnixTimestamp(long timestamp) {
        ZonedDateTime ldt = fromUnixTimestamp(timestamp, ZoneId.systemDefault());
        return ldt;
    }
    /**
     * Convert a unix timestamp (epoch milliseconds) to a ZonedDateTime
     * using the passed time zone id.
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
}

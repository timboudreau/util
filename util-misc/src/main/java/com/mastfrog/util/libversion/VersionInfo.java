/*
 * The MIT License
 *
 * Copyright 2018 Tim Boudreau.
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
package com.mastfrog.util.libversion;

import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.function.IntSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tim Boudreau
 */
public class VersionInfo implements Comparable<VersionInfo> {

    static final DateTimeFormatter ISO_INSTANT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendInstant()
            //            .parseLenient()
            .toFormatter(Locale.US);

    private static final DateTimeFormatter GIT_LOG_FORMAT = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4).appendLiteral("-")
            .appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral("-")
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendLiteral(' ')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .appendLiteral(' ')
            .appendOffset("+HHMM", "+0000")
            .parseLenient()
            .toFormatter();

    static final ZoneId GMT = ZoneId.of("GMT");

    static final ZonedDateTime EPOCH
            = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), GMT);

    public final String version;
    public final String shortCommitHash;
    public final String longCommitHash;
    public final String artifactId;
    public final boolean foundGitMetadata;
    public final boolean foundMavenMetadata;
    public final boolean dirty;
    public final String groupId;
    public final ZonedDateTime commitDate;

    VersionInfo(Class<?> clazz, String groupId, String artifactId, /* for tests */ boolean ignoreVersionsProps) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        // The antrun portion of the build script writes a file for each jar with
        // version information, git commit id and whether the repo was dirty.  We
        // retrieve it here for use in the server header.
        String className = clazz.getSimpleName() + ".class";
        String classPath = clazz.getResource(className).toString();
        String versionPropertiesPath;
        // Root around in the classpath for META-INF/services
        if (!classPath.startsWith("jar")) {
            // Class not from JAR - we are runnning from Maven most likely
            versionPropertiesPath = classPath.substring(0, classPath.length() - (clazz.getName().length()
                    + ".class".length()))
                    + "/META-INF/" + groupId + "." + artifactId + ".versions.properties";
        } else {
            // Find it within the JAR URL
            versionPropertiesPath = classPath.substring(0, classPath.lastIndexOf("!") + 1)
                    + "/META-INF/" + groupId + "." + artifactId + ".versions.properties";
        }
        boolean tryMavenMetadata = false;
        String version = "unknown";
        String shortCommitHash = "-";
        String longCommitHash = "-";
        ZonedDateTime commitDate = EPOCH;
        boolean foundGitMetadata = false;
        boolean foundMavenMetadata = false;
        boolean dirty = true;
        if (ignoreVersionsProps) {
            tryMavenMetadata = true;
        } else {
            try {
                URL url = new URL(versionPropertiesPath);
                // Load the properties file and extract a few properties from it.
                // Hmm, maybe the file should be injectable?
                try (InputStream in = url.openStream()) {
                    if (in == null) {
                        // Non existent
                        dirty = false;
                        tryMavenMetadata = true;
                    } else {
                        Properties props = new Properties();
                        props.load(in);
                        System.out.println("LOADED PROPERTIES " + props);

                        version = props.getProperty(artifactId + ".version", props.getProperty("version"));
                        shortCommitHash = props.getProperty(artifactId + ".shortCommitHash", props.getProperty("shortCommitHash"));
                        longCommitHash = props.getProperty(artifactId + ".longCommitHash", props.getProperty("longCommitHash"));
                        dirty = "dirty".equals(props.getProperty(artifactId + ".repoStatus", props.getProperty("repoStatus")));
                        String isoDate = props.getProperty(artifactId + ".commitDateISO", props.getProperty("commitDateISO"));
                        String date = isoDate != null ? isoDate : props.getProperty(artifactId + ".commitDate", props.getProperty("commitDate"));
                        System.out.println("DATE: " + date);
                        Exception first = null;
                        try {
                            if (date != null) {
                                try {
                                    System.out.println("PARSE " + date);
                                    OffsetDateTime odt = OffsetDateTime.parse(date, ISO_INSTANT);
                                    commitDate = ZonedDateTime.from(odt).withZoneSameInstant(GMT);
                                } catch (DateTimeParseException ex) {
                                    first = ex;
                                    try {
                                        commitDate = ZonedDateTime.parse(date, GIT_LOG_FORMAT);
                                    } catch (DateTimeParseException ex1) {
                                        if (first != null) {
                                            first.addSuppressed(ex1);
                                        } else {
                                            first = ex1;
                                        }
                                        commitDate = ZonedDateTime.ofInstant(
                                                Instant.parse(date), ZoneId.systemDefault());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            if (first != null) {
                                e.addSuppressed(first);
                            }
                            Logger.getLogger(VersionInfo.class.getName()).log(Level.INFO,
                                    "Could not parse " + date + " from classpath", e);
                        }
                        foundGitMetadata = true;
                        foundMavenMetadata = true;
                    }
                }
            } catch (Exception ioe) {
                Logger.getLogger(VersionInfo.class.getName()).log(Level.INFO,
                        "Could not load " + versionPropertiesPath + " from classpath", ioe);
                tryMavenMetadata = true;
                foundGitMetadata = false;
            }
        }
        if (tryMavenMetadata) {
            String mavenMetadataPath;
            if (!classPath.startsWith("jar")) {
                // Class not from JAR - we are runnning from Maven most likely
                mavenMetadataPath = classPath.substring(0, classPath.length() - (clazz.getName().length()
                        + ".class".length()))
                        + "/META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
            } else {
                // Find it within the JAR URL
                mavenMetadataPath = classPath.substring(0, classPath.lastIndexOf("!") + 1)
                        + "/META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
            }
            try {
                URL url = new URL(mavenMetadataPath);
                try (InputStream in = url.openStream()) {
                    Properties props = new Properties();
                    props.load(in);
                    version = props.getProperty("version");
                    foundMavenMetadata = true;
                }
            } catch (Exception ioe) {
                Logger.getLogger(VersionInfo.class.getName()).log(Level.INFO,
                        "Could not load " + versionPropertiesPath + " from classpath", ioe);
                foundMavenMetadata = false;
            }
        }
        this.version = version;
        this.foundGitMetadata = foundGitMetadata;
        this.foundMavenMetadata = foundMavenMetadata;
        this.shortCommitHash = shortCommitHash;
        this.longCommitHash = longCommitHash;
        this.dirty = dirty;
        this.commitDate = commitDate;
    }

    public static VersionInfo find(Class<?> type, String groupId, String artifactId) {
        return new VersionInfo(type, groupId, artifactId, false);
    }

    public String deweyDecimalVersion() {
        StringBuilder sb = new StringBuilder();
        int max = version.length();
        boolean lastWasDot = true;
        for (int i = 0; i < max; i++) {
            char c = version.charAt(i);
            if (lastWasDot && !Character.isDigit(c)) {
                break;
            }
            lastWasDot = c == '.';
            if (!Character.isDigit(c) && c == '.') {
                break;
            }
            sb.append(c);
        }
        if (sb.length() == 0) {
            return "0.0";
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version + ":" + shortCommitHash
                + ":" + commitDate.format(ISO_INSTANT);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.version);
        hash = 59 * hash + Objects.hashCode(this.longCommitHash);
        hash = 59 * hash + Objects.hashCode(this.artifactId);
        hash = 59 * hash + (this.dirty ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final VersionInfo other = (VersionInfo) obj;
        if (this.dirty != other.dirty) {
            return false;
        }
        if (!Objects.equals(this.version, other.version)) {
            return false;
        }
        if (!Objects.equals(this.longCommitHash, other.longCommitHash)) {
            return false;
        }
        if (!Objects.equals(this.artifactId, other.artifactId)) {
            return false;
        }
        if (!Objects.equals(this.groupId, other.groupId)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(VersionInfo o) {
        return compare(groupId, o.groupId, () -> {
            return compare(artifactId, o.artifactId, () -> {
                return compare(version, o.version, () -> {
                    return compare(commitDate.toInstant(), o.commitDate.toInstant(), null);
                });
            });
        });
    }

    private static <T extends Comparable<T>> int compare(T a, T b, IntSupplier next) {
        int result = a.compareTo(b);
        if (result == 0 && next != null) {
            result = next.getAsInt();
        }
        return result;
    }
}

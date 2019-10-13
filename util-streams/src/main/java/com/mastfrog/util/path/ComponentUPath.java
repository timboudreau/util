/*
 * The MIT License
 *
 * Copyright 2019 Mastfrog Technologies.
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
package com.mastfrog.util.path;

import static com.mastfrog.util.preconditions.Checks.notNull;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author Tim Boudreau
 */
public class ComponentUPath implements UnixPath {

    private static final String[] EMPTY = new String[0];
    static final ComponentUPath EMPTY_PATH = new ComponentUPath(false);
    static final ComponentUPath EMPTY_PATH_ABS = new ComponentUPath(true);
    private final String[] components;
    private final boolean absolute;
    private String stringValue;
    private int hashCode;

    String infoString() {
        StringBuilder sb = new StringBuilder()
                .append("ComponentUPath{").append(System.identityHashCode(this))
                .append(" absolute ").append(absolute)
                .append(" components " + components.length)
                .append(" {");
        for (int i = 0; i < components.length; i++) {
            String c = components[i];
            sb.append('\'').append(c).append('\'');
            if (i != components.length - 1) {
                sb.append(", ");
            }
        }
        return sb.append(')').toString();
    }

    ComponentUPath() {
        this(EMPTY, false);
    }

    ComponentUPath(boolean abs) {
        this(EMPTY, abs);
    }

    ComponentUPath(ComponentUPath orig, boolean abs) {
        this.components = orig.components;
        this.absolute = abs;
    }

    ComponentUPath(String[] components, boolean absolute) {
        this.components = components;
        this.absolute = absolute;
        sanityCheck(components);
    }

    ComponentUPath(String single) {
        this(new String[]{single}, false);
    }

    ComponentUPath(String path, char sep) {
        this(toComponents(new String[]{path}, sep),
                path.length() > 0 && path.charAt(0) == sep);
    }

    ComponentUPath(String first, char sep, String... more) {
        String[] all;
        if (more.length == 0) {
            all = new String[]{first};
        } else {
            all = new String[more.length + 1];
            all[0] = first;
            System.arraycopy(more, 0, all, 1, more.length);
        }
        this.components = toComponents(all, sep);
        sanityCheck(components);
        absolute = all.length > 0 && all[0].length() > 0 && all[0].charAt(0) == sep;
    }

    ComponentUPath(Path path) {
        this.components = toComponents(path);
        absolute = path.isAbsolute();
        sanityCheck(components);
    }

    ComponentUPath(Path a, Path... more) {
        if (more.length == 0) {
            this.components = toComponents(a);
        } else {
            this.components = toComponents(a, more);
        }
        absolute = a.isAbsolute();
    }

    static void sanityCheck(String[] s) {
        boolean asserts = true;
//        assert asserts = true;
        if (asserts) {
            for (int i = 0; i < s.length; i++) {
                if (s[i] == null) {
                    throw new IllegalArgumentException("Null element at " + i + " in "
                            + Arrays.toString(s));
                }
                for (int j = 0; j < s[i].length(); j++) {
                    char c = s[i].charAt(j);
                    if (c == File.separatorChar) {
                        throw new IllegalArgumentException("Component at " + i + " contains " + File.separatorChar + ": " + s[i]);
                    }
                    if (c == 0) {
                        throw new IllegalArgumentException("Component at " + i + " is nul (0) char");
                    }
                }
            }
        }
    }

    static ComponentUPath of(Path path) {
        return path.getNameCount() == 0 ? EMPTY_PATH : new ComponentUPath(path);
    }

    static ComponentUPath of(Path path, Path... more) {
        return path.getNameCount() == 0 ? EMPTY_PATH : new ComponentUPath(path, more);
    }

    static ComponentUPath of(String path) {
        return notNull("path", path).isEmpty()
                ? EMPTY_PATH
                : File.separator.equals(path)
                ? EMPTY_PATH_ABS
                : new ComponentUPath(path, File.separatorChar);
    }

    static ComponentUPath of(String path, String... more) {
        if (more.length == 0) {
            return of(path);
        }
        return new ComponentUPath(path, File.separatorChar, more);
    }

    static ComponentUPath ofUnixPaths(String path, String... more) {
        if (more.length == 0) {
            return of(path);
        }
        return new ComponentUPath(path, '/', more);
    }

    static ComponentUPath ofUnixPath(String path) {
        if (path.isEmpty()) {
            return EMPTY_PATH;
        } else if ("/".equals(path)) {
            return EMPTY_PATH_ABS;
        }
        return new ComponentUPath(path, '/');
    }

    static String[] toComponents(Path first, Path[] more) {
        List<String> result = new ArrayList<>(first.getNameCount()
                + more.length + 16);
        for (Path sub : first) {
            result.add(sub.toString());
        }
        for (Path p : more) {
            for (Path sub : p) {
                result.add(sub.toString());
            }
        }
        return result.toArray(new String[result.size()]);
    }

    static String[] toComponents(Path path) {
        if (path instanceof ComponentUPath) {
            return ((ComponentUPath) path).components;
        }
        List<String> all = new ArrayList<>(path.getNameCount());
        for (Path p : path) {
            all.add(p.toString());
        }
        return all.toArray(new String[all.size()]);
    }

    static String[] toComponents(String[] all, char sep) {
        if (all.length == 0) {
            return EMPTY;
        } else if (all.length == 1) {
            if (all[0].indexOf(sep) < 0) {
                return all;
            }
        }
        List<String> result = new ArrayList<>(Math.max(all.length * 2, 30));
        StringBuilder scratch = new StringBuilder(72);
        for (String curr : all) {
            int len = curr.length();
            for (int i = 0; i < len; i++) {
                char c = curr.charAt(i);
                if (c == sep) {
                    if (scratch.length() > 0) {
                        String comp = scratch.toString();
                        scratch.setLength(0);
                        result.add(comp);
                    }
                } else {
                    scratch.append(c);
                }
            }
            if (scratch.length() > 0) {
                String comp = scratch.toString();
                scratch.setLength(0);
                result.add(comp);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    public ComponentUPath toRelativePath() {
        if (absolute) {
            return new ComponentUPath(components, false);
        } else {
            return this;
        }
    }

    @Override
    public String toString() {
        if (stringValue == null) {
            stringValue = toString('/');
        }
        return stringValue;
    }

    @Override
    public Iterator<Path> iterator() {
        if (!absolute && components.length == 0) {
            return Collections.<Path>singleton(EMPTY_PATH).iterator();
        }
        return new It(components);
    }

    static class It implements Iterator<Path> {

        private final String[] items;
        private int pos = -1;

        public It(String[] items) {
            this.items = items;
        }

        @Override
        public boolean hasNext() {
            return pos + 1 < items.length;
        }

        @Override
        public Path next() {
            String item = items[++pos];
            return new ComponentUPath(item);
        }
    }

    @Override
    public String toString(char sep) {
        StringBuilder sb = new StringBuilder();
        if (absolute) {
            sb.append(sep);
        }
        for (int i = 0; i < components.length; i++) {
            sb.append(components[i]);
            if (i != components.length - 1) {
                sb.append(sep);
            }
        }
        return sb.toString();
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        return Paths.get(toString()).toRealPath(options);
    }

    @Override
    public ComponentUPath toAbsolutePath() {
        if (absolute) {
            return this;
        } else {
            Path p = Paths.get(".").toAbsolutePath().normalize();
            return ComponentUPath.of(p, this);
        }
    }

    private static boolean equalPaths(ComponentUPath query, Path other) {
        if (other instanceof ComponentUPath) {
            ComponentUPath o = (ComponentUPath) other;
            return o.absolute == query.absolute && Arrays.equals(query.components, o.components);
        } else {
            boolean abs = other.isAbsolute();
            if (abs == query.absolute) {
                String[] comps = toComponents(other);
                return Arrays.equals(query.components, comps);
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return components.length == 0 || components.length == 1 && components[0].isEmpty();
    }

    @Override
    public ComponentUPath relativize(Path other) {
        if (equalPaths(this, other)) {
            return EMPTY_PATH;
        }
        if (isEmpty()) {
            return of(other);
        }
        // can only relativize paths of the same type
        if (this.isAbsolute() != other.isAbsolute()) {
            throw new IllegalArgumentException("'other' is different type of Path");
        }
        // XXX this is wrong
        String[] comps = other instanceof ComponentUPath
                ? ((ComponentUPath) other).components
                : toComponents(other);

        int lastCommonComponent = -1;
        for (int i = 0; i < Math.min(comps.length, components.length); i++) {
            if (!components[i].equals(comps[i])) {
                break;
            }
            lastCommonComponent = i;
        }
//        System.out.println("\n" + this + " relativize " + other
//                + " lastCommon " + lastCommonComponent + " mylen " + components.length + " otherlen " + comps.length);
        if (lastCommonComponent == components.length - 1) {
//            System.out.println("B.");
            // The passed path starts with this one
            int newSize = comps.length - (lastCommonComponent + 1);
            String[] items = new String[newSize];
            System.arraycopy(comps, comps.length - newSize, items, 0, newSize);
            return forItems(items, false);
        } else if (lastCommonComponent == comps.length - 1) {
//            System.out.println("C.");
            String[] items = new String[components.length - (lastCommonComponent + 1)];
            Arrays.fill(items, "..");
            return forItems(items, false);
        } else if (lastCommonComponent >= 0) {
//            System.out.println("D.");
            if (true || components.length >= comps.length) {
                int dotDots = components.length - (lastCommonComponent + 1);
                int addedComponents = comps.length - (lastCommonComponent + 1);
//                System.out.println("dotDots " + dotDots + " addedComponents " + addedComponents
//                        + " myLen " + components.length + " olen " + comps.length);
                String[] items = new String[dotDots + addedComponents];
                Arrays.fill(items, "..");
                int srcPos = comps.length - dotDots;
                if (comps.length > components.length) {
                    srcPos -= (comps.length - components.length);
                }
//                System.out.println("mylen " + components.length + " olen " + comps.length);
//                int destLength = (comps.length - dotDots) + 1;
                int destLength = (items.length - dotDots); //(comps.length - dotDots) + 1;
                try {
                    System.arraycopy(comps, srcPos, items, dotDots, destLength);
                } catch (ArrayIndexOutOfBoundsException ex) {
                    ArrayIndexOutOfBoundsException ex2 = new ArrayIndexOutOfBoundsException(
                            "Copying from array of " + comps.length + " into array of " + items.length
                            + " srcPos " + srcPos + " destPos " + dotDots + " destLength " + destLength
                            + " lastCommonComponent " + lastCommonComponent
                            + " addedComponents " + addedComponents
                            + " relativizing " + other + " into " + this
                    );
                    ex2.initCause(ex);
                    throw (ex2);
                }
                return forItems(items, false);
            } else {
                return null;
            }
        } else {
//            System.out.println("E. ");
            String[] items = new String[components.length + comps.length];
            Arrays.fill(items, 0, components.length, "..");
            System.arraycopy(comps, 0, items, components.length, items.length - components.length);
            return forItems(items, false);
        }
    }

    static ComponentUPath forItems(String[] items, boolean abs) {
        return items.length == 0 || items.length == 1 && "".equals(items[0])
                ? abs
                        ? EMPTY_PATH_ABS
                        : EMPTY_PATH
                : new ComponentUPath(trimTrailingEmpty(items), abs);
    }

    static String[] trimTrailingEmpty(String[] items) {
        while (items.length > 0 && items[items.length - 1].isEmpty()) {
            items = Arrays.copyOf(items, items.length - 1);
        }
        return items;
    }

    @Override
    public Path toNativePath() {
        return this.toSystemPath();
    }

    @Override
    public boolean isNormalized() {
        for (String comp : components) {
            if (".".equals(comp) || "..".equals(comp)) {
                return false;
            }
        }
        return true;
    }

    private String lastRealNameComponent() {
        int ct = components.length;
        if (ct == 0) {
            return "";
        }
        String nm = components[ct - 1];
        if (".".equals(nm) || "..".equals(nm)) {
            ComponentUPath up = normalize();
            ct = up.components.length;
            if (ct == 0) {
                return "";
            }
            nm = up.components[ct - 1];
        }
        return nm;
    }

    @Override
    public String extension() {
        String nm = lastRealNameComponent();
        int ix = nm.lastIndexOf('.');
        if (ix < 1 || ix == nm.length() - 1) {
            return "";
        }
        return nm.substring(ix + 1);
    }

    @Override
    public String rawName() {
        String nm = lastRealNameComponent();
        int ix = nm.lastIndexOf('.');
        if (ix < 1 || ix == nm.length() - 1) {
            return nm;
        }
        return nm.substring(0, ix);
    }

    public boolean isExtension(String ext) {
        if (notNull("ext", ext).length() == 0 || components.length == 0) {
            return false;
        }
        String nm = lastRealNameComponent();
        if (ext.charAt(0) == '.') {
            return nm.endsWith(ext);
        }
        int ix = nm.lastIndexOf('.');
        if (ix < 1 || ix == nm.length() - 1) {
            return false;
        }
        if (nm.length() - ix < ext.length()) {
            return false;
        }
        return ext.equals(nm.substring(ix + 1));
    }

    @Override
    public ComponentUPath resolveSibling(String other) {
        if (notNull("other", other).isEmpty()) {
            return getParent();
        }
        int ix = other.indexOf('/');
        if (ix < 0) {
            String[] newComps = Arrays.copyOf(components, components.length);
            newComps[newComps.length - 1] = other;
            sanityCheck(newComps);
            return new ComponentUPath(newComps, absolute);
        } else {
            String[] otherComps = toComponents(new String[]{other}, '/');
            sanityCheck(otherComps);
            String[] newComps = concatLessTail(components, otherComps);
            sanityCheck(newComps);
            return new ComponentUPath(newComps, absolute);
        }
    }

    @Override
    public ComponentUPath resolve(String other) {
        if (notNull("other", other).isEmpty()) {
            return this;
        }
        int ix = other.indexOf('/');
        if (ix < 0) {
            String[] newComps = Arrays.copyOf(components, components.length + 1);
            newComps[newComps.length - 1] = other;
            return new ComponentUPath(newComps, absolute);
        } else {
            String[] otherComps = toComponents(new String[]{other}, '/');
            String[] newComps = concat(components, otherComps);
            return new ComponentUPath(newComps, absolute);
        }
    }

    @Override
    public UnixPath resolve(Path other) {
        if (other.isAbsolute()) {
            if (other instanceof UnixPath) {
                return (UnixPath) other;
            } else {
                return new ComponentUPath(other);
            }
        }
        String[] comps = other instanceof ComponentUPath
                ? ((ComponentUPath) other).components
                : toComponents(other);
        return new ComponentUPath(concat(components, comps), absolute);
    }

    private boolean normChecked;
    private boolean norm;

    @Override
    public ComponentUPath normalize() {
        if (components.length == 0) {
            return this;
        }
        if (normChecked && norm) {
            return this;
        }
        normChecked = true;
        norm = true;
        for (String s : components) {
            if (".".equals(s) || "..".equals(s) || s.isEmpty()) {
                norm = false;
                break;
            }
        }
        if (norm) {
            return this;
        }
        String[] nulledRemovals = Arrays.copyOf(components, components.length);
        int removedCount = 0;
        for (int i = nulledRemovals.length - 1; i >= 0; i--) {
            String c = nulledRemovals[i];
            if (c != null && ".".equals(c)) {
                nulledRemovals[i] = null;
                removedCount++;
            } else if (c != null && "..".equals(c)) {
                int firstPrecedingNonDotDot = -1;
                for (int j = i - 1; j >= 0; j--) {
                    String c1 = nulledRemovals[j];
                    if (c1 != null && !"..".equals(c1) && !".".equals(c1)) {
                        firstPrecedingNonDotDot = j;
                        break;
                    }
                }
                if (firstPrecedingNonDotDot >= 0) {
                    nulledRemovals[i] = null;
                    nulledRemovals[firstPrecedingNonDotDot] = null;
                    removedCount += 2;
                } else {
                    break;
                }
            }
        }
        if (removedCount == 0) {
            norm = true;
            return this;
        }
        assert nulledRemovals.length - removedCount >= 0 :
                "Weird result norm " + this + " len "
                + nulledRemovals.length + " removed " + removedCount;

        String[] newComponents = new String[nulledRemovals.length - removedCount];
        int cursor = 0;
        for (int i = 0; i < nulledRemovals.length; i++) {
            if (nulledRemovals[i] != null) {
                assert cursor < newComponents.length : "out of range "
                        + cursor + " origLen " + nulledRemovals.length
                        + " removed " + removedCount
                        + " in " + Arrays.toString(nulledRemovals)
                        + " with " + Arrays.toString(newComponents) + " at " + i
                        + " for '" + toString() + "'";
                newComponents[cursor++] = nulledRemovals[i];
            }
        }
        return new ComponentUPath(newComponents, absolute);
    }

    @Override
    public ComponentUPath resolveSibling(Path other) {
        String[] comps = toComponents(other);
        return new ComponentUPath(concatLessTail(components, comps), absolute);
    }

    static String[] concat(String[] a, String[] b) {
        if (a.length == 0) {
            return b;
        } else if (b.length == 0) {
            return a;
        }
        String[] result = new String[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    static String[] concatLessTail(String[] a, String[] b) {
        if (a.length == 0) {
            return b;
        } else if (b.length == 0 && a.length != 0) {
            return Arrays.copyOf(a, a.length - 1);
        } else if (a.length == 0 && b.length == 0) {
            return EMPTY;
        }
        String[] result = new String[a.length + b.length - 1];
        System.arraycopy(a, 0, result, 0, a.length - 1);
        System.arraycopy(b, 0, result, a.length - 1, b.length);
        return result;
    }

    @Override
    public ComponentUPath subpath(int beginIndex, int endIndex) {
        if (beginIndex == endIndex) {
            return EMPTY_PATH;
        }
        String[] sub = new String[endIndex - beginIndex];
        System.arraycopy(components, beginIndex, sub, 0, endIndex - beginIndex);
        return new ComponentUPath(sub, beginIndex == 0 ? absolute : false);
    }

    @Override
    public UnixPath getName(int index) {
        if (components.length == 0 && !absolute && index == 0) {
            return EMPTY_PATH;
        }
        return new ComponentUPath(components[index]);
    }

    @Override
    public ComponentUPath getParent() {
        switch (components.length) {
            case 1:
                return !absolute ? null : EMPTY_PATH_ABS;
            case 0:
                return null;
            default:
                String[] comps = Arrays.copyOf(components, components.length - 1);
                return new ComponentUPath(comps, absolute);
        }
    }

    @Override
    public UnixPath getRoot() {
        if (absolute) {
            return null;
        } else {
            return EMPTY_PATH;
        }
    }

    @Override
    public FileSystem getFileSystem() {
        return Paths.get(toString(File.separatorChar)).getFileSystem();
    }

    @Override
    public boolean isAbsolute() {
        return absolute;
    }

    @Override
    public UnixPath getFileName() {
        return components.length == 0
                ? absolute
                        ? null
                        : EMPTY_PATH
                : new ComponentUPath(components[components.length - 1]);
    }

    @Override
    public int getNameCount() {
        return components.length == 0 && !absolute ? 1 : components.length;
    }

    @Override
    public boolean startsWith(Path other) {
        if (other == this) {
            return true;
        } else if (other instanceof ComponentUPath) {
            String[] otherComponents = ((ComponentUPath) other).components;
            if (otherComponents.length > components.length) {
                return false;
            }
            for (int i = 0; i < otherComponents.length; i++) {
                if (components[i] != otherComponents[i] && !components[i].equals(otherComponents[i])) {
                    return false;
                }
            }
            return true;
        } else {
            int otherCount = other.getNameCount();
            if (otherCount > components.length) {
                return false;
            }
            for (int i = 0; i < otherCount; i++) {
                Path sub = other.getName(i);
                if (!components[i].equals(sub.toString())) {
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public boolean startsWith(String other) {
        int ix = other.indexOf('/');
        if (ix >= 0) {
            ComponentUPath up = new ComponentUPath(other, '/');
            return startsWith(up);
        } else {
            return components.length > 0 && components[0].equals(other);
        }
    }

    @Override
    public boolean endsWith(Path other) {
        if (other instanceof ComponentUPath) {
            String[] comps = ((ComponentUPath) other).components;
            if (comps.length > components.length) {
                return false;
            } else if (comps.length == components.length) {
                return Arrays.equals(comps, components);
            } else {
                for (int myCounter = components.length - 1, otherCounter = comps.length - 1; otherCounter >= 0; otherCounter--, myCounter--) {
                    if (!comps[otherCounter].equals(components[myCounter])) {
                        return false;
                    }
                }
                return true;
            }
        } else {
            int nc = other.getNameCount();
            if (nc > components.length) {
                return false;
            }
            for (int myCounter = components.length - 1, otherCounter = nc - 1; otherCounter >= 0; otherCounter--, myCounter--) {
                String nm = other.getName(nc).toString();
                if (!nm.equals(components[myCounter])) {
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public boolean endsWith(String other) {
        int ix = other.indexOf('/');
        if (ix < 0) {
            return components.length == 0 ? false : other.equals(components[components.length - 1]);
        } else {
            return endsWith(new ComponentUPath(other, '/'));
        }
    }

    @Override
    public URI toUri() {
        return Paths.get(toString()).toUri();
    }

    @Override
    public File toFile() {
        return new File(toUri());
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        return toSystemPath().register(watcher, events, modifiers);
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
        return toSystemPath().register(watcher, events);
    }

    Path toSystemPath() {
        return Paths.get(toString(File.separatorChar));
    }

    @Override
    public int visitNames(Consumer<String> consumer) {
        for (String comp : components) {
            consumer.accept(comp);
        }
        return components.length;
    }

    @Override
    public int compareTo(Path other) {
//        if (true) {
        return toString().compareTo(other.toString());
//        }
//        String[] comps;
//        if (other instanceof ComponentUPath) {
//            comps = ((ComponentUPath) other).components;
//        } else {
//            comps = toComponents(other);
//        }
//        int len1 = components.length;
//        int len2 = comps.length;
//        int max = Math.min(len1, len2);
//        for (int i = 0; i < max; i++) {
//            String a = components[i];
//            String b = comps[i];
//            int res = a.compareTo(b);
//            if (res != 0) {
//                return res;
//            }
//        }
//        return len1 - len2;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (obj instanceof Path) {
            return equalPaths(this, (Path) obj);
        }
        return false;
    }

    @Override
    public synchronized int hashCode() {
        if (hashCode != 0) {
            return hashCode;
        }
        return hashCode = Arrays.hashCode(components) * (absolute ? 1 : 17);
    }
}

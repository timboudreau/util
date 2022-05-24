/*
 * The MIT License
 *
 * Copyright 2022 Mastfrog Technologies.
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
package com.mastfrog.mtest1;

import com.mastfrog.metainf.MetaInfLoader;
import java.io.InputStream;

/**
 *
 * @author Tim Boudreau
 */
public class M1Loader extends MetaInfLoader {

    public static volatile boolean instanceCreated;

    public M1Loader() {
        super(10);
        instanceCreated = true;
        System.out.println("CREATED AN M1 LOADER");
    }

    @Override
    protected Iterable<InputStream> streamsInternal(String metaInfRelativePath) {
        System.out.println("SI " + metaInfRelativePath);
        return super.streamsInternal(metaInfRelativePath); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    protected String transformPath(String metaInfRelativePath) {
        if (true) {
//            return "/info/info.txt";
        }
        String old = metaInfRelativePath;
        String result = super.transformPath(metaInfRelativePath);
        if (!old.equals(result)) {
            System.out.println("\n" + old + " -->\n" + result + "\n");
        }
        return result;
    }

}

/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
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
package com.mastfrog.range;

import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class TestCoalesce {

    @Test
    @SuppressWarnings("unchecked")
    public void test() {
        List<IntRange<? extends IntRange<?>>> got
                = Range.coalesce((List) origs, (Coalescer) new C());
        assertEquals(expectedCoalesced, got);
    }

    static class C implements Coalescer<DRange> {

        @Override
        public DRange combine(DRange a, DRange b, int start, int size) {
            if (a.isEqual(b)) {
                return a.newRange(start, size);
            }
            return merge(a, b, start, size);
        }

        private DRange merge(DRange a, DRange b, int start, int size) {
            if (!a.isActive()) {
                return b;
            } else if (!b.isActive()) {
                return a;
            }
            int depth = a.isDepth || b.isDepth
                    ? 0 : Integer.MAX_VALUE;
            if (a.isDepth) {
                depth = a.depth();
            }
            if (b.isDepth) {
                depth = Math.max(depth, b.depth());
            }
            DRange result;
            if (depth != Integer.MAX_VALUE) {
                result = new DRange(start, start + size, depth, true, true, a.hc);
            } else {
                result = new DRange(start, start + size, 0, false, true, a.hc);
            }
            return result;
        }
    }

    private final List<IntRange<? extends IntRange<?>>> origs = Arrays.asList(
            new DRange(183, 260, 3, true, true, -757277540),
            new DRange(188, 260, 4, true, true, -999440644),
            new DRange(281, 293, 5, true, true, -906248531),
            new DRange(315, 324, 5, true, true, -906248531),
            new DRange(346, 347, 5, true, true, -906248531),
            new DRange(373, 378, 5, true, true, -906248531),
            new DRange(389, 392, 5, true, true, -906248531),
            new DRange(519, 523, 6, true, true, -130860062),
            new DRange(521, 522, 12, true, true, -130860062),
            new DRange(532, 536, 6, true, true, -130860062),
            new DRange(534, 535, 12, true, true, -130860062),
            new DRange(549, 575, 6, true, true, -130860062),
            new DRange(560, 562, 15, true, true, -130860062),
            new DRange(564, 566, 15, true, true, -130860062),
            new DRange(571, 575, 14, true, true, -130860062),
            new DRange(580, 584, 5, true, true, -130860062),
            new DRange(694, 699, 6, true, true, -130860062),
            new DRange(704, 709, 6, true, true, -130860062),
            new DRange(714, 718, 5, true, true, -130860062),
            new DRange(762, 766, 6, true, true, -130860062),
            new DRange(771, 775, 6, true, true, -130860062),
            new DRange(780, 784, 5, true, true, -130860062),
            new DRange(804, 805, 11, true, true, -906248531),
            new DRange(2080, 2089, 6, true, true, -130860062),
            new DRange(2094, 2103, 6, true, true, -130860062),
            new DRange(2108, 2112, 5, true, true, -130860062),
            new DRange(2400, 2409, 6, true, true, -130860062),
            new DRange(2414, 2423, 6, true, true, -130860062),
            new DRange(2428, 2432, 5, true, true, -130860062),
            new DRange(2625, 2631, 6, true, true, -130860062),
            new DRange(2636, 2642, 6, true, true, -130860062),
            new DRange(2647, 2651, 5, true, true, -130860062),
            new DRange(2727, 2732, 6, true, true, -130860062),
            new DRange(2737, 2742, 6, true, true, -130860062),
            new DRange(2747, 2751, 5, true, true, -130860062),
            new DRange(2840, 2852, 6, true, true, -130860062),
            new DRange(2857, 2869, 6, true, true, -130860062),
            new DRange(2874, 2878, 5, true, true, -130860062),
            new DRange(2994, 3006, 6, true, true, -130860062),
            new DRange(3011, 3023, 6, true, true, -130860062),
            new DRange(3028, 3032, 5, true, true, -130860062),
            new DRange(3441, 3459, 6, true, true, -130860062),
            new DRange(3464, 3482, 6, true, true, -130860062),
            new DRange(3487, 3491, 5, true, true, -130860062),
            new DRange(3762, 3773, 6, true, true, -130860062),
            new DRange(3778, 3789, 6, true, true, -130860062),
            new DRange(3794, 3798, 5, true, true, -130860062),
            new DRange(4110, 4126, 6, true, true, -130860062),
            new DRange(4131, 4147, 6, true, true, -130860062),
            new DRange(4152, 4156, 5, true, true, -130860062),
            new DRange(4217, 4222, 6, true, true, -130860062),
            new DRange(4227, 4232, 6, true, true, -130860062),
            new DRange(4237, 4241, 5, true, true, -130860062),
            new DRange(4262, 4263, 11, true, true, -906248531),
            new DRange(7446, 7452, 6, true, true, -130860062),
            new DRange(7457, 7463, 6, true, true, -130860062),
            new DRange(7468, 7472, 5, true, true, -130860062),
            new DRange(7666, 7670, 6, true, true, -130860062),
            new DRange(7675, 7679, 6, true, true, -130860062),
            new DRange(7684, 7688, 5, true, true, -130860062),
            new DRange(7926, 7940, 6, true, true, -130860062),
            new DRange(7934, 7939, 17, true, true, -130860062),
            new DRange(7945, 7959, 6, true, true, -130860062),
            new DRange(7953, 7958, 17, true, true, -130860062),
            new DRange(7964, 7968, 5, true, true, -130860062),
            new DRange(8040, 8046, 6, true, true, -130860062),
            new DRange(8051, 8057, 6, true, true, -130860062),
            new DRange(8062, 8066, 5, true, true, -130860062),
            new DRange(8163, 8168, 6, true, true, -130860062),
            new DRange(8173, 8178, 6, true, true, -130860062),
            new DRange(8183, 8187, 5, true, true, -130860062),
            new DRange(8208, 8209, 11, true, true, -906248531),
            new DRange(8819, 8827, 6, true, true, -130860062),
            new DRange(8825, 8826, 17, true, true, -130860062),
            new DRange(8832, 8840, 6, true, true, -130860062),
            new DRange(8838, 8839, 17, true, true, -130860062),
            new DRange(8855, 8881, 6, true, true, -130860062),
            new DRange(8866, 8868, 15, true, true, -130860062),
            new DRange(8870, 8872, 15, true, true, -130860062),
            new DRange(8877, 8881, 14, true, true, -130860062),
            new DRange(8886, 8890, 5, true, true, -130860062),
            new DRange(9076, 9085, 6, true, true, -130860062),
            new DRange(9090, 9099, 6, true, true, -130860062),
            new DRange(9104, 9108, 5, true, true, -130860062),
            new DRange(9129, 9130, 11, true, true, -906248531),
            new DRange(12304, 12320, 6, true, true, -130860062),
            new DRange(12325, 12341, 6, true, true, -130860062),
            new DRange(12346, 12350, 5, true, true, -130860062),
            new DRange(12378, 12379, 11, true, true, -906248531),
            new DRange(13066, 13080, 6, true, true, -130860062),
            new DRange(13085, 13099, 6, true, true, -130860062),
            new DRange(13104, 13108, 5, true, true, -130860062),
            new DRange(13134, 13135, 11, true, true, -906248531),
            new DRange(13828, 13836, 6, true, true, -130860062),
            new DRange(13841, 13849, 6, true, true, -130860062),
            new DRange(13854, 13858, 5, true, true, -130860062),
            new DRange(14183, 14195, 6, true, true, -130860062),
            new DRange(14200, 14212, 6, true, true, -130860062),
            new DRange(14217, 14221, 5, true, true, -130860062),
            new DRange(14245, 14246, 11, true, true, -906248531),
            new DRange(14469, 14481, 6, true, true, -130860062),
            new DRange(14486, 14498, 6, true, true, -130860062),
            new DRange(14503, 14507, 5, true, true, -130860062),
            new DRange(14755, 14761, 6, true, true, -130860062),
            new DRange(14766, 14772, 6, true, true, -130860062),
            new DRange(14777, 14781, 5, true, true, -130860062),
            new DRange(14881, 14890, 6, true, true, -130860062),
            new DRange(14895, 14904, 6, true, true, -130860062),
            new DRange(14909, 14913, 5, true, true, -130860062),
            new DRange(15188, 15197, 6, true, true, -130860062),
            new DRange(15202, 15211, 6, true, true, -130860062),
            new DRange(15216, 15220, 5, true, true, -130860062),
            new DRange(15446, 15461, 6, true, true, -130860062),
            new DRange(15466, 15481, 6, true, true, -130860062),
            new DRange(15486, 15490, 5, true, true, -130860062),
            new DRange(15517, 15518, 11, true, true, -906248531),
            new DRange(16183, 16191, 6, true, true, -130860062),
            new DRange(16196, 16204, 6, true, true, -130860062),
            new DRange(16209, 16213, 5, true, true, -130860062),
            new DRange(16318, 16330, 6, true, true, -130860062),
            new DRange(16335, 16347, 6, true, true, -130860062),
            new DRange(16352, 16356, 5, true, true, -130860062),
            new DRange(16380, 16381, 11, true, true, -906248531),
            new DRange(16648, 16659, 6, true, true, -130860062),
            new DRange(16664, 16675, 6, true, true, -130860062),
            new DRange(16680, 16684, 5, true, true, -130860062),
            new DRange(16863, 16874, 6, true, true, -130860062),
            new DRange(16879, 16890, 6, true, true, -130860062),
            new DRange(16895, 16899, 5, true, true, -130860062),
            new DRange(16926, 16927, 11, true, true, -906248531),
            new DRange(17175, 17182, 6, true, true, -130860062),
            new DRange(17187, 17194, 6, true, true, -130860062),
            new DRange(17199, 17203, 5, true, true, -130860062),
            new DRange(17525, 17533, 6, true, true, -130860062),
            new DRange(17538, 17546, 6, true, true, -130860062),
            new DRange(17551, 17555, 5, true, true, -130860062),
            new DRange(17739, 17742, 6, true, true, -130860062),
            new DRange(17747, 17750, 6, true, true, -130860062),
            new DRange(17755, 17759, 5, true, true, -130860062),
            new DRange(17778, 17779, 11, true, true, -906248531),
            new DRange(19154, 19161, 6, true, true, -130860062),
            new DRange(19166, 19173, 6, true, true, -130860062),
            new DRange(19178, 19182, 5, true, true, -130860062),
            new DRange(19201, 19202, 11, true, true, -906248531),
            new DRange(19382, 19389, 6, true, true, -130860062),
            new DRange(19394, 19401, 6, true, true, -130860062),
            new DRange(19406, 19410, 5, true, true, -130860062),
            new DRange(19515, 19528, 6, true, true, -130860062),
            new DRange(19533, 19546, 6, true, true, -130860062),
            new DRange(19551, 19555, 5, true, true, -130860062),
            new DRange(19759, 19772, 6, true, true, -130860062),
            new DRange(19777, 19790, 6, true, true, -130860062),
            new DRange(19795, 19799, 5, true, true, -130860062),
            new DRange(19828, 19829, 11, true, true, -906248531),
            new DRange(20355, 20368, 6, true, true, -130860062),
            new DRange(20373, 20386, 6, true, true, -130860062),
            new DRange(20391, 20395, 5, true, true, -130860062),
            new DRange(20420, 20421, 11, true, true, -906248531),
            new DRange(20661, 20679, 6, true, true, -130860062),
            new DRange(20684, 20702, 6, true, true, -130860062),
            new DRange(20707, 20711, 5, true, true, -130860062),
            new DRange(20745, 20746, 11, true, true, -906248531),
            new DRange(21043, 21051, 6, true, true, -130860062),
            new DRange(21056, 21064, 6, true, true, -130860062),
            new DRange(21069, 21073, 5, true, true, -130860062),
            new DRange(21160, 21170, 6, true, true, -130860062),
            new DRange(21175, 21185, 6, true, true, -130860062),
            new DRange(21190, 21194, 5, true, true, -130860062),
            new DRange(21215, 21216, 11, true, true, -906248531),
            new DRange(21490, 21498, 6, true, true, -130860062),
            new DRange(21503, 21511, 6, true, true, -130860062),
            new DRange(21516, 21520, 5, true, true, -130860062),
            new DRange(21540, 21541, 11, true, true, -906248531),
            new DRange(210, 211, 2147483647, false, true, -306751442),
            new DRange(236, 237, 2147483647, false, true, -306751442)
    );

    private final List<IntRange<? extends IntRange<?>>> expectedCoalesced = Arrays.asList(
            new DRange(183, 188, 3, true, true, -757277540),
            new DRange(188, 210, 4, true, true, -999439796),
            new DRange(210, 211, 4, true, true, -1306192563),
            new DRange(211, 236, 4, true, true, -999439796),
            new DRange(236, 237, 4, true, true, -1306192563),
            new DRange(237, 260, 4, true, true, -999439796),
            new DRange(281, 293, 5, true, true, -906248531),
            new DRange(315, 324, 5, true, true, -906248531),
            new DRange(346, 347, 5, true, true, -906248531),
            new DRange(373, 378, 5, true, true, -906248531),
            new DRange(389, 392, 5, true, true, -906248531),
            new DRange(519, 521, 6, true, true, -130860062),
            new DRange(521, 522, 6, true, true, -130860062),
            new DRange(522, 523, 6, true, true, -130860062),
            new DRange(532, 534, 6, true, true, -130860062),
            new DRange(534, 535, 6, true, true, -130860062),
            new DRange(535, 536, 6, true, true, -130860062),
            new DRange(549, 560, 6, true, true, -130860062),
            new DRange(560, 562, 6, true, true, -130860062),
            new DRange(562, 564, 6, true, true, -130860062),
            new DRange(564, 566, 6, true, true, -130860062),
            new DRange(566, 571, 6, true, true, -130860062),
            new DRange(571, 575, 6, true, true, -130860062),
            new DRange(580, 584, 5, true, true, -130860062),
            new DRange(694, 699, 6, true, true, -130860062),
            new DRange(704, 709, 6, true, true, -130860062),
            new DRange(714, 718, 5, true, true, -130860062),
            new DRange(762, 766, 6, true, true, -130860062),
            new DRange(771, 775, 6, true, true, -130860062),
            new DRange(780, 784, 5, true, true, -130860062),
            new DRange(804, 805, 11, true, true, -906248531),
            new DRange(2080, 2089, 6, true, true, -130860062),
            new DRange(2094, 2103, 6, true, true, -130860062),
            new DRange(2108, 2112, 5, true, true, -130860062),
            new DRange(2400, 2409, 6, true, true, -130860062),
            new DRange(2414, 2423, 6, true, true, -130860062),
            new DRange(2428, 2432, 5, true, true, -130860062),
            new DRange(2625, 2631, 6, true, true, -130860062),
            new DRange(2636, 2642, 6, true, true, -130860062),
            new DRange(2647, 2651, 5, true, true, -130860062),
            new DRange(2727, 2732, 6, true, true, -130860062),
            new DRange(2737, 2742, 6, true, true, -130860062),
            new DRange(2747, 2751, 5, true, true, -130860062),
            new DRange(2840, 2852, 6, true, true, -130860062),
            new DRange(2857, 2869, 6, true, true, -130860062),
            new DRange(2874, 2878, 5, true, true, -130860062),
            new DRange(2994, 3006, 6, true, true, -130860062),
            new DRange(3011, 3023, 6, true, true, -130860062),
            new DRange(3028, 3032, 5, true, true, -130860062),
            new DRange(3441, 3459, 6, true, true, -130860062),
            new DRange(3464, 3482, 6, true, true, -130860062),
            new DRange(3487, 3491, 5, true, true, -130860062),
            new DRange(3762, 3773, 6, true, true, -130860062),
            new DRange(3778, 3789, 6, true, true, -130860062),
            new DRange(3794, 3798, 5, true, true, -130860062),
            new DRange(4110, 4126, 6, true, true, -130860062),
            new DRange(4131, 4147, 6, true, true, -130860062),
            new DRange(4152, 4156, 5, true, true, -130860062),
            new DRange(4217, 4222, 6, true, true, -130860062),
            new DRange(4227, 4232, 6, true, true, -130860062),
            new DRange(4237, 4241, 5, true, true, -130860062),
            new DRange(4262, 4263, 11, true, true, -906248531),
            new DRange(7446, 7452, 6, true, true, -130860062),
            new DRange(7457, 7463, 6, true, true, -130860062),
            new DRange(7468, 7472, 5, true, true, -130860062),
            new DRange(7666, 7670, 6, true, true, -130860062),
            new DRange(7675, 7679, 6, true, true, -130860062),
            new DRange(7684, 7688, 5, true, true, -130860062),
            new DRange(7926, 7934, 6, true, true, -130860062),
            new DRange(7934, 7939, 6, true, true, -130860062),
            new DRange(7939, 7940, 6, true, true, -130860062),
            new DRange(7945, 7953, 6, true, true, -130860062),
            new DRange(7953, 7958, 6, true, true, -130860062),
            new DRange(7958, 7959, 6, true, true, -130860062),
            new DRange(7964, 7968, 5, true, true, -130860062),
            new DRange(8040, 8046, 6, true, true, -130860062),
            new DRange(8051, 8057, 6, true, true, -130860062),
            new DRange(8062, 8066, 5, true, true, -130860062),
            new DRange(8163, 8168, 6, true, true, -130860062),
            new DRange(8173, 8178, 6, true, true, -130860062),
            new DRange(8183, 8187, 5, true, true, -130860062),
            new DRange(8208, 8209, 11, true, true, -906248531),
            new DRange(8819, 8825, 6, true, true, -130860062),
            new DRange(8825, 8826, 6, true, true, -130860062),
            new DRange(8826, 8827, 6, true, true, -130860062),
            new DRange(8832, 8838, 6, true, true, -130860062),
            new DRange(8838, 8839, 6, true, true, -130860062),
            new DRange(8839, 8840, 6, true, true, -130860062),
            new DRange(8855, 8866, 6, true, true, -130860062),
            new DRange(8866, 8868, 6, true, true, -130860062),
            new DRange(8868, 8870, 6, true, true, -130860062),
            new DRange(8870, 8872, 6, true, true, -130860062),
            new DRange(8872, 8877, 6, true, true, -130860062),
            new DRange(8877, 8881, 6, true, true, -130860062),
            new DRange(8886, 8890, 5, true, true, -130860062),
            new DRange(9076, 9085, 6, true, true, -130860062),
            new DRange(9090, 9099, 6, true, true, -130860062),
            new DRange(9104, 9108, 5, true, true, -130860062),
            new DRange(9129, 9130, 11, true, true, -906248531),
            new DRange(12304, 12320, 6, true, true, -130860062),
            new DRange(12325, 12341, 6, true, true, -130860062),
            new DRange(12346, 12350, 5, true, true, -130860062),
            new DRange(12378, 12379, 11, true, true, -906248531),
            new DRange(13066, 13080, 6, true, true, -130860062),
            new DRange(13085, 13099, 6, true, true, -130860062),
            new DRange(13104, 13108, 5, true, true, -130860062),
            new DRange(13134, 13135, 11, true, true, -906248531),
            new DRange(13828, 13836, 6, true, true, -130860062),
            new DRange(13841, 13849, 6, true, true, -130860062),
            new DRange(13854, 13858, 5, true, true, -130860062),
            new DRange(14183, 14195, 6, true, true, -130860062),
            new DRange(14200, 14212, 6, true, true, -130860062),
            new DRange(14217, 14221, 5, true, true, -130860062),
            new DRange(14245, 14246, 11, true, true, -906248531),
            new DRange(14469, 14481, 6, true, true, -130860062),
            new DRange(14486, 14498, 6, true, true, -130860062),
            new DRange(14503, 14507, 5, true, true, -130860062),
            new DRange(14755, 14761, 6, true, true, -130860062),
            new DRange(14766, 14772, 6, true, true, -130860062),
            new DRange(14777, 14781, 5, true, true, -130860062),
            new DRange(14881, 14890, 6, true, true, -130860062),
            new DRange(14895, 14904, 6, true, true, -130860062),
            new DRange(14909, 14913, 5, true, true, -130860062),
            new DRange(15188, 15197, 6, true, true, -130860062),
            new DRange(15202, 15211, 6, true, true, -130860062),
            new DRange(15216, 15220, 5, true, true, -130860062),
            new DRange(15446, 15461, 6, true, true, -130860062),
            new DRange(15466, 15481, 6, true, true, -130860062),
            new DRange(15486, 15490, 5, true, true, -130860062),
            new DRange(15517, 15518, 11, true, true, -906248531),
            new DRange(16183, 16191, 6, true, true, -130860062),
            new DRange(16196, 16204, 6, true, true, -130860062),
            new DRange(16209, 16213, 5, true, true, -130860062),
            new DRange(16318, 16330, 6, true, true, -130860062),
            new DRange(16335, 16347, 6, true, true, -130860062),
            new DRange(16352, 16356, 5, true, true, -130860062),
            new DRange(16380, 16381, 11, true, true, -906248531),
            new DRange(16648, 16659, 6, true, true, -130860062),
            new DRange(16664, 16675, 6, true, true, -130860062),
            new DRange(16680, 16684, 5, true, true, -130860062),
            new DRange(16863, 16874, 6, true, true, -130860062),
            new DRange(16879, 16890, 6, true, true, -130860062),
            new DRange(16895, 16899, 5, true, true, -130860062),
            new DRange(16926, 16927, 11, true, true, -906248531),
            new DRange(17175, 17182, 6, true, true, -130860062),
            new DRange(17187, 17194, 6, true, true, -130860062),
            new DRange(17199, 17203, 5, true, true, -130860062),
            new DRange(17525, 17533, 6, true, true, -130860062),
            new DRange(17538, 17546, 6, true, true, -130860062),
            new DRange(17551, 17555, 5, true, true, -130860062),
            new DRange(17739, 17742, 6, true, true, -130860062),
            new DRange(17747, 17750, 6, true, true, -130860062),
            new DRange(17755, 17759, 5, true, true, -130860062),
            new DRange(17778, 17779, 11, true, true, -906248531),
            new DRange(19154, 19161, 6, true, true, -130860062),
            new DRange(19166, 19173, 6, true, true, -130860062),
            new DRange(19178, 19182, 5, true, true, -130860062),
            new DRange(19201, 19202, 11, true, true, -906248531),
            new DRange(19382, 19389, 6, true, true, -130860062),
            new DRange(19394, 19401, 6, true, true, -130860062),
            new DRange(19406, 19410, 5, true, true, -130860062),
            new DRange(19515, 19528, 6, true, true, -130860062),
            new DRange(19533, 19546, 6, true, true, -130860062),
            new DRange(19551, 19555, 5, true, true, -130860062),
            new DRange(19759, 19772, 6, true, true, -130860062),
            new DRange(19777, 19790, 6, true, true, -130860062),
            new DRange(19795, 19799, 5, true, true, -130860062),
            new DRange(19828, 19829, 11, true, true, -906248531),
            new DRange(20355, 20368, 6, true, true, -130860062),
            new DRange(20373, 20386, 6, true, true, -130860062),
            new DRange(20391, 20395, 5, true, true, -130860062),
            new DRange(20420, 20421, 11, true, true, -906248531),
            new DRange(20661, 20679, 6, true, true, -130860062),
            new DRange(20684, 20702, 6, true, true, -130860062),
            new DRange(20707, 20711, 5, true, true, -130860062),
            new DRange(20745, 20746, 11, true, true, -906248531),
            new DRange(21043, 21051, 6, true, true, -130860062),
            new DRange(21056, 21064, 6, true, true, -130860062),
            new DRange(21069, 21073, 5, true, true, -130860062),
            new DRange(21160, 21170, 6, true, true, -130860062),
            new DRange(21175, 21185, 6, true, true, -130860062),
            new DRange(21190, 21194, 5, true, true, -130860062),
            new DRange(21215, 21216, 11, true, true, -906248531),
            new DRange(21490, 21498, 6, true, true, -130860062),
            new DRange(21503, 21511, 6, true, true, -130860062),
            new DRange(21516, 21520, 5, true, true, -130860062),
            new DRange(21540, 21541, 11, true, true, -906248531)
    );

    static final class DRange implements IntRange<DRange> {

        private final int start;
        private final int end;
        private final int depth;
        private final boolean isDepth;
        private final boolean isActive;
        private final int hc;

        DRange(int start, int end, int depth, boolean isDepth, boolean isActive, int hc) {
            this.start = start;
            this.end = end;
            this.depth = depth;
            this.isDepth = isDepth;
            this.isActive = isActive;
            this.hc = hc;
        }

        @Override
        public int start() {
            return start;
        }

        @Override
        public int size() {
            return end - start;
        }

        @Override
        public DRange newRange(int start, int size) {
            return new DRange(start, start + size, depth, isDepth, isActive, hc);
        }

        @Override
        public DRange newRange(long start, long size) {
            return newRange((int) start, (int) size);
        }

        int depth() {
            return isDepth ? depth : Integer.MAX_VALUE;
        }

        boolean isEqual(DRange other) {
            return hc == other.hc;
        }

        boolean isActive() {
            return isActive;
        }

        boolean isDepth() {
            return isDepth;
        }

        @Override
        public String toString() {
            return depth + "(" + start + ":" + end + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o == null || !(o instanceof DRange)) {
                return false;
            }
            DRange other = (DRange) o;
            return start == other.start && end == other.end && depth == other.depth;
        }

        @Override
        public int hashCode() {
            return ((start * 71) + end * 1000001) + depth;
        }
    }
}

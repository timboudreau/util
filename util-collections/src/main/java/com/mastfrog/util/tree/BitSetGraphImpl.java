package com.mastfrog.util.tree;

import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * A highly compact representation of a tree of rules referencing other rules as
 * an array of BitSets, one per rule.
 *
 * @author Tim Boudreau
 */
final class BitSetGraphImpl implements BitSetGraph {

    private final BitSet[] ruleReferences;
    private final BitSet[] referencedBy;
    private final BitSet topLevel;
    private final BitSet bottomLevel;

    BitSetGraphImpl(BitSet[] ruleReferences, BitSet[] referencedBy) {
        this.ruleReferences = ruleReferences;
        this.referencedBy = referencedBy;
        BitSet ruleReferencesKeys = keySet(ruleReferences);
        BitSet referencedByKeys = keySet(referencedBy);
        topLevel = new BitSet(ruleReferences.length);
        bottomLevel = new BitSet(referencedBy.length);
        topLevel.or(ruleReferencesKeys);
        bottomLevel.or(referencedByKeys);
        topLevel.andNot(referencedByKeys);
        bottomLevel.andNot(ruleReferencesKeys);
    }

    @Override
    public void walk(BitSetGraphVisitor v) {
        walk(v, topLevel, new BitSet(), 0);
    }

    @Override
    public void walk(int startingWith, BitSetGraphVisitor v) {
        BitSet set = new BitSet();
        set.set(startingWith);
        walk(v, set, new BitSet(), 0);
    }

    private void walk(BitSetGraphVisitor v, BitSet traverse, BitSet seen, int depth) {
        BitSet refs = traverse;
        for (int bit = refs.nextSetBit(0); bit >= 0; bit = refs.nextSetBit(bit + 1)) {
            if (!seen.get(bit)) {
                seen.set(bit);
                v.enterRule(bit, depth);
                walk(v, ruleReferences[bit], seen, depth + 1);
                v.exitRule(bit, depth);
            }
        }
    }

    @Override
    public void walkUpwards(BitSetGraphVisitor v) {
        walkUpwards(v, topLevel, new BitSet(), 0);
    }

    @Override
    public void walkUpwards(int startingWith, BitSetGraphVisitor v) {
        BitSet set = new BitSet();
        set.set(startingWith);
        walkUpwards(v, set, new BitSet(), 0);
    }

    private void walkUpwards(BitSetGraphVisitor v, BitSet traverse, BitSet seen, int depth) {
        BitSet refs = traverse;
        for (int bit = refs.nextSetBit(0); bit >= 0; bit = refs.nextSetBit(bit + 1)) {
            if (!seen.get(bit)) {
                seen.set(bit);
                v.enterRule(bit, depth);
                walk(v, referencedBy[bit], seen, depth + 1);
                v.exitRule(bit, depth);
            }
        }
    }

    @Override
    public int size() {
        return ruleReferences.length;
    }

    @Override
    public BitSet closureDisjunction(int a, int b) {
        if (a == b) {
            return new BitSet();
        }
        BitSet ca = closureOf(a);
        BitSet cb = closureOf(b);
        ca.xor(cb);
        return ca;
    }

    @Override
    public BitSet closureUnion(int a, int b) {
        if (a == b) {
            return new BitSet();
        }
        BitSet ca = closureOf(a);
        BitSet cb = closureOf(b);
        ca.or(cb);
        return ca;
    }

    @Override
    public BitSet closureIntersection(int a, int b) {
        if (a == b) {
            return new BitSet();
        }
        BitSet ca = closureOf(a);
        BitSet cb = closureOf(b);
        ca.and(cb);
        return ca;
    }

    @Override
    public int distance(int a, int b) {
        if (a == b) {
            return 0;
        }
        int down = distanceDown(a, b, Integer.MAX_VALUE, 1);
        if (down == 1) {
            return down;
        }
        int up = distanceUp(a, b, Integer.MAX_VALUE, 1);
        if (up == Integer.MAX_VALUE && down == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (up == down) {
            return up;
        }
        if (up < down) {
            return -up;
        } else {
            return down;
        }
    }

    private int distanceDown(int from, int to, int currResult, int depth) {
        BitSet refs = ruleReferences[to];
        for (int bit = refs.nextSetBit(0); bit >= 0; bit = refs.nextSetBit(bit + 1)) {
            if (bit == to) {
                return Math.min(currResult, depth);
            }
            int found = distanceDown(from, bit, currResult, depth + 1);
            if (found < currResult) {
                currResult = found;
            }
        }
        return currResult;
    }

    private int distanceUp(int from, int to, int currResult, int depth) {
        BitSet refs = ruleReferences[to];
        for (int bit = refs.nextSetBit(0); bit >= 0; bit = refs.nextSetBit(bit + 1)) {
            if (bit == to) {
                return Math.min(currResult, depth);
            }
            int found = distanceDown(from, bit, currResult, depth + 1);
            if (found < currResult) {
                currResult = found;
            }
        }
        return currResult;
    }

    @Override
    public BitSet closureDisjunction(int... nodes) {
        BitSet result = new BitSet(size());
        for (int i = 0; i < nodes.length; i++) {
            BitSet clos = closureOf(nodes[i]);
            if (i == 0) {
                result.or(clos);
            } else {
                result.xor(clos);
            }
        }
        return result;
    }

    @Override
    public double[] eigenvectorCentrality(int maxIterations, double maxDiff,
            boolean inEdges, boolean ignoreSelfEdges, boolean l2norm) {
        int sz = size();
        double[] unnormalized = new double[sz];
        double[] centrality = new double[sz];
        Arrays.fill(centrality, 1.0 / (double) sz);
        double diff = 0.0;
        int iter = 0;
        do {
            for (int i = 0; i < sz; i++) {
                BitSet dests = inEdges ? referencedBy[i] : reachableFrom(i);
                double sum = sum(dests, centrality, ignoreSelfEdges ? i : Integer.MIN_VALUE);
                unnormalized[i] = sum;
                double s;
                if (l2norm) {
                    double l2sum = 0.0;
                    for (int j = 0; j < sz; j++) {
                        l2sum += unnormalized[j] * unnormalized[j];
                    }
                    s = (l2sum == 0.0) ? 1.0 : 1 / sqrt(l2sum);
                } else {
                    double l1sum = 0.0;
                    for (int j = 0; j < sz; j++) {
                        l1sum += unnormalized[j];
                    }
                    s = l1sum == 0.0 ? 1.0 : 1 / l1sum;
                }
                diff = 0.0;
                for (int j = 0; j < sz; j++) {
                    double val = unnormalized[j] * s;
                    diff += Math.abs(centrality[j] - val);
                    centrality[j] = val;
                }
            }
        } while (iter++ < maxIterations && diff > maxDiff);
        return centrality;
    }

    private double sum(DoubleIntFunction func) {
        double result = 0.0;
        for (int i = 0; i < size(); i++) {
            result += func.apply(i);
        }
        return result;
    }

    @Override
    public double[] pageRank(double maxError, double dampingFactor,
            int maximumIterations, boolean normalize) {
        double difference;
        int cnt = 0;
        double n = size();
        double[] result = new double[size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = 1d / n;
        }
        do {
            difference = 0.0;
            double danglingFactor = 0;
            if (normalize) {
                danglingFactor = dampingFactor / n * sum(i -> {
                    if (children(i).cardinality() == 0) {
                        return result[i];
                    }
                    return 0.0;
                });
            }
            for (int i = 0; i < size(); i++) {
                double inputSum = sum(referencedBy[i], j -> {
                    double outDegree = children(j).cardinality();
                    if (outDegree != 0) {
                        return result[j] / outDegree;
                    }
                    return 0.0;
                });
                double val = (1.0 - dampingFactor) / n
                        + dampingFactor * inputSum + danglingFactor;
                difference += Math.abs(val - result[i]);
                if (result[i] < val) {
                    result[i] = val;
                }
            }
            cnt++;
        } while ((difference > maxError) && cnt < maximumIterations);
        return result;
    }

    private double sum(BitSet set, DoubleIntFunction f) {
        double result = 0.0;
        for (int bit = set.nextSetBit(0); bit >= 0; bit = set.nextSetBit(bit + 1)) {
            result += f.apply(bit);
        }
        return result;
    }

    private double sum(BitSet set, double[] values, int ifNot) {
        double result = 0.0;
        for (int bit = set.nextSetBit(0); bit >= 0; bit = set.nextSetBit(bit + 1)) {
            if (bit != ifNot) {
                result += values[bit];
            }
        }
        return result;
    }

    private void forEach(BitSet set, IntConsumer cons) {
        for (int bit = set.nextSetBit(0); bit >= 0; bit = set.nextSetBit(bit + 1)) {
            cons.accept(bit);
        }
    }

    @Override
    public BitSet reachableFrom(int startingNode) {
        BitSet result = copyOf(referencedBy[startingNode]);
        result.or(ruleReferences[startingNode]);
        return result;
    }

    public void depthFirstSearch(int startingNode, boolean up, IntConsumer cons) {
        depthFirstSearch(startingNode, up, cons, new BitSet());
    }

    public void breadthFirstSearch(int startingNode, boolean up, IntConsumer cons) {
        breadthFirstSearch(startingNode, up, cons, new BitSet());
    }

    private void breadthFirstSearch(int startingNode, boolean up, IntConsumer cons, BitSet traversed) {
        BitSet dests = up ? referencedBy[startingNode] : ruleReferences[startingNode];
        boolean any = false;
        for (int bit = dests.nextSetBit(0); bit >= 0; bit = dests.nextSetBit(bit + 1)) {
            if (!traversed.get(bit)) {
                cons.accept(bit);
                any = true;
            }
        }
        if (!any) {
            return;
        }
        for (int bit = dests.nextSetBit(0); bit >= 0; bit = dests.nextSetBit(bit + 1)) {
            if (!traversed.get(bit)) {
                breadthFirstSearch(bit, up, cons, traversed);
                traversed.set(bit);
            }
        }
    }

    private void depthFirstSearch(int startingNode, boolean up, IntConsumer cons, BitSet traversed) {
        BitSet dests = up ? referencedBy[startingNode] : ruleReferences[startingNode];
        boolean any = false;
        for (int bit = dests.nextSetBit(0); bit >= 0; bit = dests.nextSetBit(bit + 1)) {
            if (!traversed.get(bit)) {
                depthFirstSearch(bit, up, cons, traversed);
                any = true;
            }
        }
        if (!any) {
            return;
        }
        for (int bit = dests.nextSetBit(0); bit >= 0; bit = dests.nextSetBit(bit + 1)) {
            if (!traversed.get(bit)) {
                traversed.set(bit);
                cons.accept(bit);
            }
        }
    }

    private BitSet copyOf(BitSet set) {
        BitSet nue = new BitSet(size());
        nue.or(set);
        return nue;
    }

    @Override
    public Set<Integer> disjointItems() {
        BitSet[] unions = new BitSet[size()];
        for (int i = 0; i < unions.length; i++) {
            unions[i] = new BitSet(unions.length);
        }
        Set<Integer> result = new HashSet<>();
        outer:
        for (int i = 0; i < unions.length; i++) {
            unions[i] = closureOf(i);
            unions[i].clear(i);
            for (int j = 0; j < unions.length; j++) {
                if (i != j) {
                    unions[i].andNot(unions[j]);
                }
                if (unions[i].cardinality() == 0) {
                    continue;
                }
            }
            if (!unions[i].isEmpty()) {
                result.add(i);
            }
        }
        return result;
    }

    @Override
    public boolean isRecursive(int rule) {
        BitSet closure = closureOf(rule);
        return closure.get(rule);
    }

    @Override
    public boolean isIndirectlyRecursive(int rule) {
        BitSet test = new BitSet(size());
        test.or(ruleReferences[rule]);
        test.clear(rule);
        closureOf(rule, test, 0);
        return test.get(rule);
    }

    @Override
    public int[] byClosureSize() {
        Integer[] result = new Integer[ruleReferences.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = i;
        }
        int[] cache = new int[result.length];
        Arrays.sort(result, (a, b) -> {
            int sizeA = cache[a] == -1 ? cache[a] = closureSize(a) : cache[a];
            int sizeB = cache[b] == -1 ? cache[b] = closureSize(b) : cache[b];
            return sizeA == sizeB ? 0 : sizeA > sizeB ? 1 : -1;
        });
        int[] res = new int[result.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = result[i];
        }
        return res;
    }

    @Override
    public int[] byReverseClosureSize() {
        Integer[] result = new Integer[ruleReferences.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = i;
        }
        int[] cache = new int[ruleReferences.length];
        Arrays.fill(cache, -1);
        Arrays.sort(result, (a, b) -> {
            int sizeA = cache[a] == -1 ? cache[a] = reverseClosureSize(a) : cache[a];
            int sizeB = cache[b] == -1 ? cache[b] = reverseClosureSize(b) : cache[b];
            return sizeA == sizeB ? 0 : sizeA > sizeB ? 1 : -1;
        });
        int[] res = new int[result.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = result[i];
        }
        return res;
    }

    @Override
    public List<int[]> edges() {
        List<int[]> result = new ArrayList<>();
        for (int i = 0; i < ruleReferences.length; i++) {
            BitSet refs = ruleReferences[i];
            for (int bit = refs.nextSetBit(0); bit >= 0; bit = refs.nextSetBit(bit + 1)) {
                result.add(new int[]{i, bit});
            }
        }
        return result;
    }

    @Override
    public int inboundReferenceCount(int rule) {
        return referencedBy[rule].cardinality();
    }

    @Override
    public int outboundReferenceCount(int rule) {
        return ruleReferences[rule].cardinality();
    }

    public BitSet children(int rule) {
        return ruleReferences[rule];
    }

    @Override
    public BitSet parents(int rule) {
        return referencedBy[rule];
    }

    private BitSet keySet(BitSet[] bits) {
        BitSet nue = new BitSet(bits.length);
        for (int i = 0; i < bits.length; i++) {
            if (bits[i].cardinality() > 0) {
                nue.set(i);
            }
        }
        return nue;
    }

    @Override
    public BitSet topLevelOrOrphanNodes() {
        return topLevel;
    }

    @Override
    public BitSet bottomLevelNodes() {
        return bottomLevel;
    }

    @Override
    public boolean isUnreferenced(int rule) {
        return referencedBy[rule].isEmpty();
    }

    @Override
    public int closureSize(int rule) {
        return closureOf(rule).cardinality();
    }

    @Override
    public int reverseClosureSize(int rule) {
        return reverseClosureOf(rule).cardinality();
    }

    @Override
    public BitSet closureOf(int rule) {
        BitSet result = new BitSet();
        closureOf(rule, result, 0);
        return result;
    }

    private void closureOf(int rule, BitSet into, int depth) {
        if (into.get(rule)) {
            return;
        }
        if (depth > 0) {
            into.set(rule);
        }
        BitSet refs = ruleReferences[rule];
        for (int bit = refs.nextSetBit(0); bit >= 0; bit = refs.nextSetBit(bit + 1)) {
            if (bit != rule /* && !into.get(bit) */) {
                closureOf(bit, into, depth + 1);
            }
            into.set(bit);
        }
    }

    @Override
    public BitSet reverseClosureOf(int rule) {
        BitSet result = new BitSet();
        System.out.println("reverse closure of " + rule);
        reverseClosureOf(rule, result, 0);
        return result;
    }

    private String depthString(int val) {
        char[] c = new char[val * 2];
        Arrays.fill(c, ' ');
        return new String(c);
    }

    private void reverseClosureOf(int rule, BitSet into, int depth) {
        String ind = depthString(depth);
        if (into.get(rule)) {
            return;
        }
        if (depth > 0) {
            into.set(rule);
        }
        BitSet refs = referencedBy[rule];
        for (int bit = refs.nextSetBit(0); bit >= 0; bit = refs.nextSetBit(bit + 1)) {
            if (bit != rule) {
                reverseClosureOf(bit, into, depth + 1);
            }
            into.set(bit);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        BitSet seen = new BitSet(this.ruleReferences.length);
        walk((node, depth) -> {
            sb.append(depthString(depth));
            sb.append(node);
            BitSet seenKids = copyOf(children(node));
            seenKids.and(seen);
            if (!seenKids.isEmpty()) {
                boolean started = false;
                for (int bit = seenKids.nextSetBit(0); bit >= 0; bit = seenKids.nextSetBit(bit + 1)) {
                    if (bit != node) {
                        if (!started) {
                            sb.append(" -> (");
                        } else {
                            sb.append(',');
                        }
                        sb.append(bit);
                    }
                }
                sb.append(')');
            }
            sb.append('\n');
            seen.set(node);
        });
        return sb.toString();
    }

//    public RuleTree strings(String[] ruleNames) {
//        return new StringRuleTree(this, ruleNames);
//    }
}

package com.mastfrog.util.tree;

import java.util.Arrays;
import java.util.Iterator;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class RuleTreeTest {

    private final FakeRule[] RULES_1 = new FakeRule[]{
        new FakeRule("G"),
        new FakeRule("C", "D", "E", "F"),
        new FakeRule("B", "B", "D"),
        new FakeRule("E", "G", "H"),
        new FakeRule("D"),
        new FakeRule("H", "D"),
        new FakeRule("F", "B", "D"),
        new FakeRule("A", "B", "C"),
        new FakeRule("O")
    };

    private final FakeRule[] RULES_2 = new FakeRule[] {
        new FakeRule("A", "B", "C", "Q"),
        new FakeRule("B", "B", "C", "Q"),
        new FakeRule("C", "B", "C", "Q"),
        new FakeRule("D", "E", "Q"),
        new FakeRule("E", "N", "C", "Q"),
        new FakeRule("F", "M", "G", "Q"),
        new FakeRule("G", "C", "D", "Q"),
        new FakeRule("H", "G", "D", "Q"),
        new FakeRule("I", "D", "Q"),
        new FakeRule("J", "C", "D", "Q"),
        new FakeRule("K", "D", "C", "Q"),
        new FakeRule("L", "C", "B", "Q"),
        new FakeRule("M", "B", "A", "Q"),
        new FakeRule("N", "Q"),
        new FakeRule("Q", "N", "L", "A", "I")
    };

    String[] names;
    String[] names2;
    @Before
    public void setup() {
        names = new String[RULES_1.length];
        for (int i = 0; i < RULES_1.length; i++) {
            names[i] = RULES_1[i].name;
        }
        Arrays.sort(names);
        names2 = new String[RULES_2.length];
        for (int i = 0; i < RULES_2.length; i++) {
            names2[i] = RULES_2[i].name;
        }
        Arrays.sort(names2);
    }

    @Test
    public void testCentrality() {
        ObjectGraphImpl<String> irt = (ObjectGraphImpl<String>) traverseAll(RULES_2, new BitSetGraphBuilder<String>(Arrays.asList(names2), String.class)).build().toObjectTree(Arrays.asList(names2), String.class);
        System.out.println("\n************* EIGENVECTOR ************");
        for (Score<String> score : irt.eigenvectorCentrality()) {
            System.out.println("  " + score + " closureSize " + irt.closureSize(score.node()));
        }
        System.out.println("\n************* PAGERANK ************");
        for (Score<String> score : irt.pageRank()) {
            System.out.println("  " + score);
        }
        System.out.println("disjoint: " + irt.disjointItems());

        System.out.println("\n************* DISJUNCTION ************");
        for (String disj : irt.disjunctionOfClosureOfMostCentralNodes()) {
            System.out.println("  " + disj);
        }

    }

    @Test
    public void testSomeMethod() {
        ObjectGraph<String> irt = traverseAll(RULES_1, new BitSetGraphBuilder<>(Arrays.asList(names), String.class)).build().toObjectTree(Arrays.asList(names), String.class);
        ObjectGraph<String> srt = traverseAll(RULES_1, new RuleTreeBuilder()).toRuleTree();
        Iterator<String> a = srt.edgeStrings().iterator();
        Iterator<String> b = irt.edgeStrings().iterator();
        int ix = 0;
        System.out.println("EDGES: ");
        while (a.hasNext() && b.hasNext()) {
            System.out.println(ix + "-ST: " + a.next());
            System.out.println(ix + "-IN: " + b.next());
            ix++;
        }

//        assertEquals(new HashSet<>(srt.edgeStrings()), new HashSet<>(irt.edgeStrings()));
        assertEquals(srt.topLevelOrOrphanNodes(), irt.topLevelOrOrphanNodes());
        assertEquals(srt.bottomLevelNodes(), irt.bottomLevelNodes());

        System.out.println("ST edge-strings size: " + srt.edgeStrings().size());
        System.out.println("IN edge-strings size: " + irt.edgeStrings().size());

        System.out.println("ST top: " + srt.topLevelOrOrphanNodes());
        System.out.println("IN top: " + irt.topLevelOrOrphanNodes());
        System.out.println("ST bottom: " + srt.bottomLevelNodes());
        System.out.println("IN bottom: " + irt.bottomLevelNodes());
        for (String r : names) {

            assertEquals(r, srt.parents(r), irt.parents(r));

            System.out.println("ST " + r + " parents: " + srt.parents(r));
            System.out.println("IN " + r + " parents: " + irt.parents(r));
            System.out.println("ST " + r + " kids: " + srt.children(r));
            System.out.println("IN " + r + " kids: " + irt.children(r));

            assertEquals(r, srt.children(r), irt.children(r));
            assertEquals(r, srt.parents(r), irt.parents(r));

            assertEquals(r, srt.closureOf(r), irt.closureOf(r));
            assertEquals(r, srt.reverseClosureOf(r), irt.reverseClosureOf(r));

            System.out.println("ST " + r + " closure-size: " + srt.closureSize(r));
            System.out.println("IN " + r + " closure-size: " + irt.closureSize(r));
            System.out.println("ST " + r + " rev-closure-size: " + srt.reverseClosureSize(r));
            System.out.println("IN " + r + " rev-closure-size: " + irt.reverseClosureSize(r));
            System.out.println("ST " + r + " ir-count: " + srt.inboundReferenceCount(r));
            System.out.println("IN " + r + " ir-count: " + irt.inboundReferenceCount(r));
            System.out.println("ST " + r + " or-count: " + srt.outboundReferenceCount(r));
            System.out.println("IN " + r + " or-count: " + irt.outboundReferenceCount(r));
            System.out.println("ST " + r + " closure: " + srt.closureOf(r));
            System.out.println("IN " + r + " closure: " + irt.closureOf(r));
            System.out.println("ST " + r + " rev-closure: " + srt.reverseClosureOf(r));
            System.out.println("IN " + r + " rev-closure: " + irt.reverseClosureOf(r));
            System.out.println("");
        }
        

    }

    private BitSetGraphBuilder<String> traverseAll(FakeRule[] rules, BitSetGraphBuilder<String> b) {
        for (FakeRule rule : rules) {
            rule.traverse(b);
        }
        return b;
    }

    private RuleTreeBuilder traverseAll(FakeRule[] rules, RuleTreeBuilder b) {
        for (FakeRule rule : rules) {
            rule.traverse(b);
        }
        return b;
    }

    static final class FakeRule implements Iterable<String> {

        private final String name;
        private final String[] refs;

        public FakeRule(String name, String... refs) {
            this.name = name;
            this.refs = refs;
        }

        void traverse(RuleTreeBuilder rtb) {
            rtb.enterItem(name, () -> {
                for (String ref : this) {
                    rtb.addEdge(name, ref);
                }
            });
        }

        void traverse(BitSetGraphBuilder<String> rtb) {
            rtb.enterItem(name, () -> {
                for (String ref : this) {
                    rtb.addEdge(name, ref);
                }
            });
        }

        public String name() {
            return name;
        }

        public Iterator<String> iterator() {
            return Arrays.asList(refs).iterator();
        }
    }
}

package com.mastfrog.util.tree;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Tim Boudreau
 */
class RuleTreeBuilder {

    Map<String, Set<String>> ruleReferences = new HashMap<>();
    Map<String, Set<String>> ruleReferencedBy = new HashMap<>();
    String currRule;

    void addEdge(String referencer, String referenced) {
        System.out.println("addEdge " + referencer + " -> " + referenced);
        Set<String> outbound = ruleReferences.get(referencer);
        if (outbound == null) {
            outbound = new HashSet<>(10);
            ruleReferences.put(referencer, outbound);
        }
        outbound.add(referenced);
        Set<String> inbound = ruleReferencedBy.get(referenced);
        if (inbound == null) {
            inbound = new HashSet<>(10);
            ruleReferencedBy.put(referenced, inbound);
        }
        inbound.add(referencer);
    }

    void enterItem(String item, Runnable run) {
        String old = currRule;
        currRule = item;
        try {
            run.run();
        } finally {
            currRule = old;
        }
    }

    public RuleTreeImpl toRuleTree() {
        return new RuleTreeImpl(ruleReferences, ruleReferencedBy);
    }
}

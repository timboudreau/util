package com.mastfrog.util.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The original, correct but far less efficient implementation of RuleTree,
 * retained to verify the behavior of the bitset based one.
 *
 * @author Tim Boudreau
 */
final class RuleTreeImpl implements ObjectGraph<String> {

    final Map<String, Set<String>> ruleReferences;
    final Map<String, Set<String>> ruleReferencedBy;
    final Set<String> topLevel = new HashSet<>();
    final Set<String> bottomLevel = new HashSet<>();

    RuleTreeImpl(Map<String, Set<String>> ruleReferences, Map<String, Set<String>> ruleReferencedBy) {
        this.ruleReferences = ruleReferences;
        this.ruleReferencedBy = ruleReferencedBy;
        topLevel.addAll(ruleReferences.keySet());
        topLevel.removeAll(ruleReferencedBy.keySet());
        bottomLevel.addAll(ruleReferencedBy.keySet());
        bottomLevel.removeAll(ruleReferences.keySet());
    }

    @Override
    public Set<String> parents(String rule) {
        Set<String> result = ruleReferencedBy.get(rule);
        return result == null ? Collections.emptySet() : result;
    }

    @Override
    public Set<String> children(String rule) {
        Set<String> result = ruleReferences.get(rule);
        return result == null ? Collections.emptySet() : result;
    }

    Set<String> all() {
        Set<String> result = new HashSet<>();
        result.addAll(ruleReferences.keySet());
        result.addAll(ruleReferencedBy.keySet());
        result.addAll(topLevel);
        result.addAll(bottomLevel);
        return result;
    }

    @Override
    public List<String> byClosureSize() {
        Map<String, Integer> m = new HashMap<>();
        List<String> result = new ArrayList<>(all());
        for (String name : result) {
            m.put(name, closureSize(name));
        }
        Collections.sort(result, (a, b) -> {
            return m.get(a).compareTo(m.get(b));
        });
        return result;
    }

    @Override
    public List<String> byReverseClosureSize() {
        Map<String, Integer> m = new HashMap<>();
        List<String> result = new ArrayList<>(all());
        for (String name : result) {
            m.put(name, reverseClosureSize(name));
        }
        Collections.sort(result, (a, b) -> {
            return m.get(a).compareTo(m.get(b));
        });
        return result;
    }

    public Set<String> edgeStrings() {
        Set<String> result = new HashSet<>(ruleReferences.size());
        for (Map.Entry<String, Set<String>> e : ruleReferences.entrySet()) {
            for (String s : e.getValue()) {
                result.add(e.getKey() + ":" + s);
            }
        }
        return result;
    }

    public int inboundReferenceCount(String rule) {
        Set<String> inbound = ruleReferencedBy.get(rule);
        return inbound == null ? 0 : inbound.size();
    }

    public int outboundReferenceCount(String rule) {
        Set<String> outbound = ruleReferences.get(rule);
        return outbound == null ? 0 : outbound.size();
    }

    /**
     * Rules that no other rule references - likely top level rules (or
     * orphans).
     *
     * @return
     */
    public Set<String> topLevelOrOrphanNodes() {
        return topLevel;
    }

    /**
     * Rules which do not themselves contain any references to other rules.
     *
     * @return
     */
    public Set<String> bottomLevelNodes() {
        return bottomLevel;
    }

    public boolean isUnreferenced(String rule) {
        return !ruleReferencedBy.containsKey(rule);
    }

    public int closureSize(String rule) {
        return closureOf(rule).size();
    }

    public int reverseClosureSize(String rule) {
        return reverseClosureOf(rule).size();
    }

    public Set<String> reverseClosureOf(String rule) {
        Set<String> result = new HashSet<>(ruleReferencedBy.size());
        reverseClosureOf(rule, result);
        return result;
    }

    public Set<String> closureOf(String rule) {
        Set<String> result = new HashSet<>(ruleReferencedBy.size());
        closureOf(rule, result);
        return result;
    }

    private void closureOf(String rule, Set<String> result) {
        Set<String> refs = ruleReferences.get(rule);
        if (refs != null) {
            for (String s : refs) {
                if (!result.contains(s)) {
                    result.add(s);
                    closureOf(s, result);
                }
            }
        }
    }

    private void reverseClosureOf(String rule, Set<String> result) {
        Set<String> refs = ruleReferencedBy.get(rule);
        if (refs != null) {
            for (String s : refs) {
                if (!result.contains(s)) {
                    result.add(s);
                    reverseClosureOf(s, result);
                }
            }
        }
    }

}

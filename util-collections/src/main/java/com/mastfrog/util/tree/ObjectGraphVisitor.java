package com.mastfrog.util.tree;

/**
 * Visitor interface for traversing rule trees.
 *
 * @author Tim Boudreau
 */
public interface ObjectGraphVisitor<T> {

    /**
     * Enter a rule; subsequent calls to enterRule before a call to exitRule
     * means the subsequent rules are referenced by the previous one. EnterRule
     * will be called exactly once for each rule, starting from top-level and
     * orphan rules which have no antecedents. For rules referenced by
     * descendants, the order they are passed to enterRule is
     * implementation-depenedent - they will appear nested under some rule that
     * calls them, but not more than one.
     *
     * @param rule
     * @param depth
     */
    void enterNode(T node, int depth);

    void exitNode(T node, int depth);

}

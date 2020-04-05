/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mastfrog.function.state;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Tim Boudreau
 */
final class IntWithChildrenImpl extends IntImpl implements IntWithChildren {

    private List<Int> children = null;

    IntWithChildrenImpl() {
    }

    IntWithChildrenImpl(int initial) {
        super(initial);
    }

    IntWithChildrenImpl(Int initial) {
        super(initial.getAsInt());
    }

    @Override
    public IntWithChildrenImpl child() {
        IntWithChildrenImpl result = new IntWithChildrenImpl(this);
        if (children == null) {
            children = new LinkedList<>();
        }
        children.add(result);
        return result;
    }

    @Override
    public int increment(int val) {
        int result = super.increment(val);
        if (children != null) {
            children.forEach((kid) -> {
                kid.increment(val);
            });
        }
        return result;
    }
}

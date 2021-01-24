package com.mastfrog.function.state;

/**
 *
 * @author Tim Boudreau
 */
final class FltImpl implements Flt {

    private float value;

    FltImpl() {

    }

    FltImpl(float initial) {
        this.value = initial;
    }

    @Override
    public void accept(float value) {
        this.value = value;
    }

    @Override
    public float getAsFloat() {
        return this.value;
    }

    @Override
    public String toString() {
        return Float.toString(value);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof Flt) {
            float otherVal = ((Flt) o).getAsFloat();
            int a = Float.floatToIntBits(value + 0.0F);
            int b = Float.floatToIntBits(otherVal + 0.0F);
            return a == b;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Float.floatToIntBits(value);
    }
}

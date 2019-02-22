package org.jboss.builder;

import java.util.Objects;

/**
 * The ordinal identifier for a build step.
 */
public final class StepOrdinal implements Comparable<StepOrdinal> {

    private final StepOrdinal parent;
    private final int index;
    private final int depth;

    private StepOrdinal(final StepOrdinal parent, final int index) {
        this.parent = parent;
        this.index = index;
        depth = parent == null ? 0 : parent.depth + 1;
    }

    public static StepOrdinal create(int index) {
        return new StepOrdinal(null, index);
    }

    public StepOrdinal createChild(int childIndex) {
        return new StepOrdinal(this, childIndex);
    }

    public StepOrdinal getRoot() {
        return parent == null ? this : parent.getRoot();
    }

    public int compareTo(final StepOrdinal o) {
        final int depth = this.depth;
        final int otherDepth = o.depth;
        int res;
        if (depth > otherDepth) {
            res = parent.compareTo(o);
            // we come after
            return res == 0 ? 1 : res;
        } else if (depth < otherDepth) {
            res = compareTo(o.parent);
            // we come before
            return res == 0 ? -1 : res;
        } else if (depth == 0) {
            assert otherDepth == 0;
            res = 0;
        } else {
            res = parent.compareTo(o.parent);
        }
        if (res == 0) res = Integer.signum(index - o.index);
        return res;
    }

    public StepOrdinal min(StepOrdinal other) {
        return compareTo(other) < 0 ? this : other;
    }

    public StepOrdinal max(StepOrdinal other) {
        return compareTo(other) > 0 ? this : other;
    }

    public int hashCode() {
        return Objects.hashCode(parent) * 12289 + index;
    }

    public boolean equals(final Object obj) {
        return obj instanceof StepOrdinal && equals((StepOrdinal) obj);
    }

    public boolean equals(final StepOrdinal obj) {
        return obj == this || obj != null && Objects.equals(parent, obj.parent) && index == obj.index;
    }

    private StringBuilder toString(StringBuilder b) {
        if (parent != null) {
            parent.toString(b);
            b.append('.');
        }
        b.append(index);
        return b;
    }

    public String toString() {
        return toString(new StringBuilder(depth * 3)).toString();
    }
}

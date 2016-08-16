package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A commit (SCM transaction) with the changes performed in it.
 */
public class Commit {

    private final String message;
    private final List<Change> changes;

    Commit(String message, List<Change> changes) {
        this.message = message;
        this.changes = new ArrayList<Change>(changes);
    }

    public String getMessage() {
        return this.message;
    }

    public List<Change> getChanges() {
        return Collections.unmodifiableList(this.changes);
    }

    /**
     * Creates and returns a copy of this commit where all changes contained in the given
     * set haven been changed to "irrelevant for review".
     */
    public Commit makeChangesIrrelevant(Set<? extends Change> toMakeIrrelevant) {
        final List<Change> adjustedChanges = new ArrayList<>();
        for (final Change change : this.changes) {
            if (toMakeIrrelevant.contains(change)) {
                adjustedChanges.add(change.makeIrrelevant());
            } else {
                adjustedChanges.add(change);
            }
        }
        return new Commit(this.message, adjustedChanges);
    }

}

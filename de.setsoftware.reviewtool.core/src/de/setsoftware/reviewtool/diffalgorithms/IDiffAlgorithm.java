package de.setsoftware.reviewtool.diffalgorithms;

import java.io.IOException;
import java.util.List;

import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.model.changestructure.Fragment;
import de.setsoftware.reviewtool.model.changestructure.FileInRevision;

/**
 * Interface for diff algorithms.
 */
public interface IDiffAlgorithm {

    /**
     * Determines a diff between the given file contents. The result is a list
     * of fragment positions in fileNew that contain changes (as pairs (Start position; end position).
     * Not all changes on the byte level have to result in change fragments, as an implementation can
     * choose to ignore certain changes.
     */
    public abstract List<Pair<Fragment, Fragment>> determineDiff(
            FileInRevision fileOldInfo,
            byte[] fileOldContent,
            FileInRevision fileNewInfo,
            byte[] fileNewContent,
            String charset) throws IOException;

}

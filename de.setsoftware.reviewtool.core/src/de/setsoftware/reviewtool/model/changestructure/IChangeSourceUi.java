package de.setsoftware.reviewtool.model.changestructure;

/**
 * UI-Callbacks for change sources.
 */
public interface IChangeSourceUi {

    /**
     * Is called when the current local working copy does not contain all needed
     * changes (if the change source implements a corresponding check).
     * If true is returned the user chose to update the working copy.
     * If false is returned, the change source shall go on without performing an update.
     */
    public abstract boolean handleLocalWorkingCopyOutOfDate(String detailInfo);

}

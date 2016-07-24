package de.setsoftware.reviewtool.ui.dialogs;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

/**
 * Class with helper functions for dialogs.
 */
public class DialogHelper {

    private static final String PREFIX = "dialogSizes_";

    private static IPreferenceStore preferenceStore;

    public static void setPreferenceStore(IPreferenceStore ps) {
        preferenceStore = ps;
    }

    public static void saveDialogSize(String id, int width, int height) {
        preferenceStore.setValue(determineId(id, "x"), width);
        preferenceStore.setValue(determineId(id, "y"), height);
    }

    public static void saveDialogSize(Window window) {
        final Point size = window.getShell().getSize();
        saveDialogSize(window.getClass().getName(), size.x, size.y);
    }

    /**
     * Returns the saved size for the dialog with the given id.
     */
    public static Point getDialogSize(String id) {
        final int x = getSize(id, "x");
        final int y = getSize(id, "y");
        if (x > 0 && y > 0) {
            return new Point(x, y);
        } else {
            return null;
        }

    }

    private static int getSize(String id, String coord) {
        return preferenceStore.getInt(determineId(id, coord));
    }

    private static String determineId(String id, String coord) {
        return PREFIX + id + "_" + coord;
    }

    /**
     * Sets the dialog's size to the saved values. If no saved values are available, the given default values
     * are used.
     */
    public static void restoreSavedSize(
            Shell newShell, Window dialog, int defaultWidth, int defaultHeight) {
        final Point savedSize = getDialogSize(dialog.getClass().getName());
        if (savedSize == null) {
            newShell.setSize(defaultWidth, defaultHeight);
        } else {
            newShell.setSize(savedSize);
        }
    }

    public static void saveSetting(String id, String value) {
        preferenceStore.setValue("dialogSetting_" + id, value);
    }

    public static String getSetting(String id) {
        return preferenceStore.getString("dialogSetting_" + id);
    }

}

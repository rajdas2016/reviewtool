package de.setsoftware.reviewtool.ui.popup.actions;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.ResolutionType;
import de.setsoftware.reviewtool.model.ReviewRemark;
import de.setsoftware.reviewtool.plugin.ReviewPlugin;
import de.setsoftware.reviewtool.telemetry.Telemetry;
import de.setsoftware.reviewtool.ui.dialogs.AddReplyDialog;
import de.setsoftware.reviewtool.ui.dialogs.InputDialogCallback;

public class QuestionResolution implements IMarkerResolution {

    public static final QuestionResolution INSTANCE = new QuestionResolution();

    private QuestionResolution() {
    }

    @Override
    public String getLabel() {
        return "Question";
    }

    @Override
    public void run(final IMarker marker) {
        final ReviewRemark review = ReviewRemark.getFor(ReviewPlugin.getPersistence(), marker);
        AddReplyDialog.get(review, new InputDialogCallback() {
            @Override
            public void execute(String text) {
                try {
                    review.addComment(ReviewPlugin.getUserPref(), text);
                    review.setResolution(ResolutionType.QUESTION);
                    review.save();
                    Telemetry.get().resolutionQuestion(
                            marker.getResource().toString(),
                            marker.getAttribute(IMarker.LINE_NUMBER, -1));
                } catch (final CoreException e) {
                    throw new ReviewtoolException(e);
                }
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof QuestionResolution;
    }

    @Override
    public int hashCode() {
        return 1231232;
    }

}

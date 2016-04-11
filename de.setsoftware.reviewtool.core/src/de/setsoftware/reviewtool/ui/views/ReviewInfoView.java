package de.setsoftware.reviewtool.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

import de.setsoftware.reviewtool.model.ITicketData;
import de.setsoftware.reviewtool.model.ReviewStateManager;
import de.setsoftware.reviewtool.model.TicketInfo;
import de.setsoftware.reviewtool.model.changestructure.SlicesInReview;

/**
 * A view that contains general information on the review and the ticket.
 */
public class ReviewInfoView extends ViewPart implements ReviewModeListener {

    private Composite comp;
    private Composite currentContent;

    @Override
    public void createPartControl(Composite comp) {
        this.comp = comp;

        ViewDataSource.get().registerListener(this);
    }

    private void createLabelAndText(Composite comp, String labelText, String text, int style, int fill) {
        final Label label = new Label(comp, SWT.NULL);
        label.setText(labelText);

        final Text field = new Text(comp, style | SWT.BORDER | SWT.RESIZE);
        field.setText(text);
        field.setLayoutData(new GridData(fill));
        field.setEditable(false);
    }

    @Override
    public void setFocus() {
    }

    @Override
    public void notifyReview(ReviewStateManager mgr, SlicesInReview slices) {
        this.disposeOldContent();
        this.currentContent = this.createReviewContent(mgr);
        this.comp.layout();
    }

    private ScrolledComposite createReviewContent(ReviewStateManager mgr) {
        return this.createCommonContentForReviewAndFixing(mgr, "Review aktiv für:");
    }

    private ScrolledComposite createCommonContentForReviewAndFixing(ReviewStateManager mgr, String title) {
        final ScrolledComposite scroll = new ScrolledComposite(this.comp, SWT.VERTICAL);
        scroll.setExpandHorizontal(true);
        scroll.setExpandVertical(true);
        scroll.setMinSize(300, 200);

        final Composite scrollContent = new Composite(scroll, SWT.NULL);

        final GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        scrollContent.setLayout(layout);

        final ITicketData ticketData = mgr.getCurrentTicketData();
        final TicketInfo ticketInfo = ticketData.getTicketInfo();
        this.createLabelAndText(scrollContent, title, ticketInfo.getId(),
                SWT.SINGLE, GridData.FILL_HORIZONTAL);
        this.createLabelAndText(scrollContent, "Titel:", ticketInfo.getSummary(),
                SWT.SINGLE | SWT.WRAP, GridData.FILL_HORIZONTAL);
        this.createLabelAndText(scrollContent, "Reviewanmerkungen:", ticketData.getReviewData(),
                SWT.MULTI, GridData.FILL_BOTH);

        scroll.setContent(scrollContent);
        return scroll;
    }

    @Override
    public void notifyFixing(ReviewStateManager mgr) {
        this.disposeOldContent();
        this.currentContent = this.createFixingContent(mgr);
        this.comp.layout();
    }

    private Composite createFixingContent(ReviewStateManager mgr) {
        return this.createCommonContentForReviewAndFixing(mgr, "Einpflegen aktiv für:");
    }

    @Override
    public void notifyIdle() {
        this.disposeOldContent();
        this.currentContent = this.createIdleContent();
        this.comp.layout();
    }

    private Composite createIdleContent() {
        final Composite panel = new Composite(this.comp, SWT.NULL);
        panel.setLayout(new FillLayout());
        final Label label = new Label(panel, SWT.NULL);
        label.setText("Kein Review oder Einpflegen aktiv");
        return panel;
    }

    private void disposeOldContent() {
        if (this.currentContent != null) {
            this.currentContent.dispose();
        }
    }

}
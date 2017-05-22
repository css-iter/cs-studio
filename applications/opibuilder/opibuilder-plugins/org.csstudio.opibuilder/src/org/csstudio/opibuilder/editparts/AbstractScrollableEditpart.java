package org.csstudio.opibuilder.editparts;

import org.eclipse.draw2d.ScrollPane;

public abstract class AbstractScrollableEditpart extends AbstractContainerEditpart {

    /**
     * @return The {@link ScrollPane} of this scrollable EditPart
     */
    public abstract ScrollPane getScrollPane();

}

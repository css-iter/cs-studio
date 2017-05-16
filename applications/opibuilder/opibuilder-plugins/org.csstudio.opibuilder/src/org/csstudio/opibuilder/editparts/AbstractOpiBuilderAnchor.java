package org.csstudio.opibuilder.editparts;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;

abstract public class AbstractOpiBuilderAnchor extends AbstractConnectionAnchor {

    /**
     * This enum tells in which direction a connector is connected to the widget
     * @author mvitorovic
     *
     */
    public static enum ConnectorDirection { HORIZONTAL, VERTICAL }

    public AbstractOpiBuilderAnchor(IFigure owner) {
        super(owner);
    }

    abstract public Point getSlantDifference(Point anchorPoint, Point midPoint);

    /**
     * @return The direction in which the connection is oriented at this anchor, if the Manhattan router is being selected
     */
    abstract public ConnectorDirection getDirection();
}

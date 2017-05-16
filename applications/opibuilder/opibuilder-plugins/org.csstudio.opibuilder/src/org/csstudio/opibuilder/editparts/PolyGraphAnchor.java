package org.csstudio.opibuilder.editparts;

import org.eclipse.draw2d.Polyline;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

public class PolyGraphAnchor extends AbstractOpiBuilderAnchor {
    private int pointIndex;
    private Polyline polyline;

    public PolyGraphAnchor(final Polyline owner, final int pointIndex) {
        super(owner);
        this.polyline = owner;
        this.pointIndex = pointIndex;
    }

    @Override
    public Point getLocation(Point reference) {
        Point p = polyline.getPoints().getPoint(pointIndex);
        polyline.translateToAbsolute(p);
        return p;
    }

    @Override
    public Point getReferencePoint() {
        return getLocation(null);
    }

    @Override
    public ConnectorDirection getDirection() {
        return getDirection(polyline.getPoints().getPoint(pointIndex));
    }

    private ConnectorDirection getDirection(final Point anchor) {
        // calculate the direction. The direction for now is decided like this:
        // if the connector is closest to the left or right side of the bounding box, then horizontal connection line is selected
        // if the connector is closest to the top or bottom side of the bounding box, then vertical connection line is selected
        final Rectangle bounds = getOwner().getBounds();
        // The bounds in is relative coordinates, so relative to the clipping/bounding box if in a linked container ==> translate the anchor as well
        final Point translatedAnchor = anchor.getCopy();
        // calculate the smallest absolute offset from the left and right bound
        int leftRight = Math.min(Math.abs(translatedAnchor.x - bounds.x), Math.abs(translatedAnchor.x - (bounds.x + bounds.width)));
        // calculate the smallest absolute offset from the top and bottom bound
        int topBottom = Math.min(Math.abs(translatedAnchor.y - bounds.y), Math.abs(translatedAnchor.y - (bounds.y + bounds.height)));
        // anchorPoint and midPoint are both in original coordinates
        if (leftRight < topBottom)
            return ConnectorDirection.HORIZONTAL;
        else
            return ConnectorDirection.VERTICAL;
    }

    @Override
    public Point getSlantDifference(Point anchorPoint, Point midPoint) {
        int x = 0;
        int y = 0;
        // anchorPoint and midPoint are both in original coordinates
        if (getDirection(anchorPoint) == ConnectorDirection.HORIZONTAL) {
            // closer to the left or right edge
            y = anchorPoint.y() - midPoint.y();
        } else {
            // closer to the top or bottom edge
            x = anchorPoint.x() - midPoint.x();
        }
        return new Point(x, y);
    }
}

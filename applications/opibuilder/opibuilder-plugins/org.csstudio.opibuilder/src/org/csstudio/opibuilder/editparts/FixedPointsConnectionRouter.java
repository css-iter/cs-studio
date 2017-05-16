package org.csstudio.opibuilder.editparts;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.opibuilder.editparts.AbstractOpiBuilderAnchor.ConnectorDirection;
import org.eclipse.draw2d.AbstractRouter;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.ScrollPane;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;

/**
 * The router that route a connection through fixed points
 * @author Xihui Chen
 *
 */
public class FixedPointsConnectionRouter extends AbstractRouter {

    private static final Logger LOGGER = Logger.getLogger(FixedPointsConnectionRouter.class.getCanonicalName());

    private Map<Connection, Object> constraints = new HashMap<Connection, Object>(2);

    private PolylineConnection connectionFigure;

    private ScrollPane scrollPane;

    public FixedPointsConnectionRouter() {
    }

    @Override
    public Object getConstraint(Connection connection) {
        return constraints.get(connection);
    }

    @Override
    public void remove(Connection connection) {
        constraints.remove(connection);
    }

    @Override
    public void setConstraint(Connection connection, Object constraint) {
        constraints.put(connection, constraint);
    }

    public void setConnectionFigure(PolylineConnection connectionFigure) {
        this.connectionFigure = connectionFigure;
    }

    public void setScrollPane(ScrollPane scrollPane) {
        this.scrollPane = scrollPane;
    }

    @Override
    public void route(Connection conn) {
        PointList connPoints = new PointList();
        PointList constraintPoints = (PointList) getConstraint(conn);

        // we get the absolute and relative start points of the connection
        // absolute: on screen, relative: according to parent. May be different if the OPI is being scrolled
        Point startPoint = getStartPoint(conn);
        Point startPointRel = startPoint.getCopy();
        conn.translateToRelative(startPointRel);
        LOGGER.log(Level.FINEST, "startPoint: " + startPoint + ", startPointRel: " + startPointRel);

        // we get the absolute and relative end points of the connection
        Point endPoint = getEndPoint(conn);
        Point endPointRel = endPoint.getCopy();
        conn.translateToRelative(endPointRel);
        LOGGER.log(Level.FINEST, "endPoint: " + endPoint + ", endPointRel: " + endPointRel);

        connPoints.addPoint(startPointRel);

        AbstractOpiBuilderAnchor anchor = (AbstractOpiBuilderAnchor)conn.getSourceAnchor();
        final ConnectorDirection startDirection = anchor.getDirection();
        LOGGER.log(Level.FINEST, "startDirection: " + startDirection.toString());

        anchor = (AbstractOpiBuilderAnchor)conn.getTargetAnchor();
        final ConnectorDirection endDirection = anchor.getDirection();
        LOGGER.log(Level.FINEST, "endDirection: " + endDirection.toString());

        connPoints.addAll(adjustRouteEndsToAnchors(constraintPoints.getCopy(), startDirection, endDirection, startPointRel, endPointRel));

        connPoints.addPoint(endPointRel);

        LOGGER.log(Level.FINEST, buildPointDebug("final connection", connPoints));
        conn.setPoints(connPoints);
    }

    private PointList adjustRouteEndsToAnchors(PointList oldPoints, ConnectorDirection startDirection, ConnectorDirection endDirection, Point startPointRel, Point endPointRel) {
        simpleMove(oldPoints, startDirection, endDirection, startPointRel, endPointRel);

        return oldPoints;
    }

    //
    //--------------------------------------------------------------- One or two point connections -------------------------------
    //
    private void simpleMove(PointList translatedPoints, ConnectorDirection startDirection, ConnectorDirection endDirection, Point startPointRel, Point endPointRel) {
        // Handle the start point
        final Point firstPoint = translatedPoints.getFirstPoint();
        onePointMove(firstPoint, startDirection, startPointRel);
        translatedPoints.setPoint(firstPoint, 0);

        // Handle the end point
        final int lastIndex = translatedPoints.size() - 1;
        final Point lastPoint = translatedPoints.getPoint(lastIndex);
        onePointMove(lastPoint, endDirection, endPointRel);
        translatedPoints.setPoint(lastPoint, lastIndex);
    }

    private void onePointMove(Point pointToMove, ConnectorDirection connectorDirection, Point anchor) {
        if (connectorDirection == ConnectorDirection.VERTICAL) {
            // Only horizontal move affects the connection
            pointToMove.setX(anchor.x());
        } else {
            // Only vertical move affects the connection
            pointToMove.setY(anchor.y());
        }
    }

    //
    //--------------------------------------------------------------- Other methods -------------------------------
    //

    private String buildPointDebug(String name, PointList points) {
        final StringBuilder sb = new StringBuilder(points.size() * 8 + 32);
        sb.append(name).append(": [");
        for(int i = 0; i < points.size(); ++i) {
            final Point p = points.getPoint(i);
            sb.append(p.toString()).append(i>=points.size()-1?"":", ");
        }
        return sb.append(']').toString();
    }
}

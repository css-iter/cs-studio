/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.editparts;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.csstudio.opibuilder.commands.ConnectionDeleteCommand;
import org.csstudio.opibuilder.datadefinition.WidgetIgnorableUITask;
import org.csstudio.opibuilder.editpolicies.ManhattanBendpointEditPolicy;
import org.csstudio.opibuilder.model.ConnectionModel;
import org.csstudio.opibuilder.model.ConnectionModel.LineJumpAdd;
import org.csstudio.opibuilder.model.DisplayModel;
import org.csstudio.opibuilder.util.GUIRefreshThread;
import org.csstudio.opibuilder.util.OPIColor;
import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ManhattanConnectionRouter;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.Polyline;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.PolylineDecoration;
import org.eclipse.draw2d.RotatableDecoration;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editpolicies.ConnectionEditPolicy;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.gef.requests.ReconnectRequest;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionFilter;

/**
 * Editpart for connections between widgets.
 *
 * @author Xihui Chen
 *
 */
public class WidgetConnectionEditPart extends AbstractConnectionEditPart {

    private ExecutionMode executionMode;

    private RotatableDecoration targetDecoration, sourceDecoration;

    private HashMap<Point, PointList> intersectionMap;

    /**
     * The factor to calculate x from arrow length
     */
    private static double X_FACTOR = 1 / Math.sqrt(1 + Math.pow(
            Math.tan(ConnectionModel.ARROW_ANGLE), 2));
    private static double Y_FACTOR = Math.tan(ConnectionModel.ARROW_ANGLE)
            * X_FACTOR;

    @Override
    public void activate() {
        if (!isActive()) {
            super.activate();
            getWidgetModel().getProperty(ConnectionModel.PROP_LINE_COLOR)
                    .addPropertyChangeListener(new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            getConnectionFigure().setForegroundColor(
                                    ((OPIColor) evt.getNewValue())
                                            .getSWTColor());
                        }
                    });

            getWidgetModel().getProperty(ConnectionModel.PROP_LINE_STYLE)
                    .addPropertyChangeListener(new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            getConnectionFigure().setLineStyle(
                                    getWidgetModel().getLineStyle());
                        }
                    });
            getWidgetModel().getProperty(ConnectionModel.PROP_LINE_WIDTH)
                    .addPropertyChangeListener(new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            getConnectionFigure().setLineWidth(
                                    getWidgetModel().getLineWidth());
                        }
                    });
            getWidgetModel().getProperty(ConnectionModel.PROP_ROUTER)
                    .addPropertyChangeListener(new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            updateRouter(getConnectionFigure());
                        }
                    });

            getWidgetModel().getProperty(ConnectionModel.PROP_POINTS)
                    .addPropertyChangeListener(new PropertyChangeListener() {
                        @Override
                        public void propertyChange(final PropertyChangeEvent evt) {
                            if (getViewer() == null || getViewer().getControl() == null) {
                                return;
                            }
                            Runnable runnable = new Runnable() {

                                @Override
                                public void run() {
                                    if(((PointList)evt.getOldValue()).size() !=
                                            ((PointList)evt.getNewValue()).size())
                                        updateRouter(getConnectionFigure());
                                    else
                                        refreshBendpoints(getConnectionFigure());
                                }
                            };
                            //It should update at the same rate as other widget at run time
                            if(getExecutionMode() == ExecutionMode.RUN_MODE){
                                Display display = getViewer().getControl().getDisplay();
                                WidgetIgnorableUITask task = new WidgetIgnorableUITask(
                                        getWidgetModel().getProperty(ConnectionModel.PROP_POINTS),
                                        runnable, display);

                                GUIRefreshThread.getInstance(
                                        getExecutionMode() == ExecutionMode.RUN_MODE)
                                        .addIgnorableTask(task);
                            }else
                                runnable.run();
                        }
                    });

            getWidgetModel().getProperty(ConnectionModel.PROP_ARROW_LENGTH)
                    .addPropertyChangeListener(new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            updateArrowLength(getConnectionFigure());
                        }
                    });

            getWidgetModel().getProperty(ConnectionModel.PROP_ARROW_TYPE)
                    .addPropertyChangeListener(new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            updateDecoration(getConnectionFigure());
                            updateArrowLength(getConnectionFigure());
                        }
                    });

            getWidgetModel().getProperty(ConnectionModel.PROP_FILL_ARROW)
                    .addPropertyChangeListener(new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            updateDecoration(getConnectionFigure());
                            updateArrowLength(getConnectionFigure());
                        }
                    });
            getWidgetModel().getProperty(ConnectionModel.PROP_ANTIALIAS)
                    .addPropertyChangeListener(new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            getConnectionFigure().setAntialias(
                                    getWidgetModel().isAntiAlias()? SWT.ON : SWT.OFF);
                            for(Object obj : getConnectionFigure().getChildren()){
                                if(obj instanceof Shape)
                                    ((Shape)obj).setAntialias(
                                            getWidgetModel().isAntiAlias()? SWT.ON : SWT.OFF);
                            }
                        }
                    });
            getWidgetModel().getProperty(ConnectionModel.PROP_LINE_JUMP_ADD)
                    .addPropertyChangeListener(new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            getConnectionFigure().setLineJumpAdd(
                                    getWidgetModel().getLineJumpAdd());
                        }
                    });
            getWidgetModel().getProperty(ConnectionModel.PROP_LINE_JUMP_SIZE)
                    .addPropertyChangeListener(new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            getConnectionFigure().setLineJumpSize(
                                    getWidgetModel().getLineJumpSize());
                        }
                    });
            getWidgetModel().getProperty(ConnectionModel.PROP_LINE_JUMP_STYLE)
                    .addPropertyChangeListener(new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            getConnectionFigure().setLineJumpStyle(
                                    getWidgetModel().getLineJumpStyle());
                        }
                    });
            getWidgetModel().getProperty(ConnectionModel.PROP_IS_LOADED_FROM_LINKING_CONTAINER)
            .addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    updateRouter(getConnectionFigure());
                }
            });
        }
    }

    @Override
    protected void createEditPolicies() {
        if (getExecutionMode() == ExecutionMode.EDIT_MODE && !getWidgetModel().isLoadedFromLinkedOpi()) {
            // Selection handle edit policy.
            // Makes the connection show a feedback, when selected by the user.
            installEditPolicy(EditPolicy.CONNECTION_ENDPOINTS_ROLE,
                    new ConnectionEndpointEditPolicy() {
                        private ConnectionRouter originalRouter = null;
                        private Object originalConstraint = null;

                        @Override
                        protected void showConnectionMoveFeedback(
                                ReconnectRequest request) {
                            EditPolicy connectionHandlesEditpolicy = getEditPolicy(EditPolicy.CONNECTION_BENDPOINTS_ROLE);
                            if(connectionHandlesEditpolicy !=null && connectionHandlesEditpolicy instanceof ManhattanBendpointEditPolicy){
                                ((ManhattanBendpointEditPolicy)connectionHandlesEditpolicy).removeSelectionHandles();
                            }
                            if (getConnection().getConnectionRouter() instanceof FixedPointsConnectionRouter) {
                                originalRouter = getConnection()
                                        .getConnectionRouter();
                                originalConstraint = originalRouter.getConstraint(getConnection());

                                getConnection().setConnectionRouter(
                                        new ManhattanConnectionRouter());
                            }
                            super.showConnectionMoveFeedback(request);
                        }

                        @Override
                        protected void eraseConnectionMoveFeedback(
                                ReconnectRequest request) {
                            if(originalRouter != null){
                                originalRouter.setConstraint(getConnection(), originalConstraint);
                                getConnection().setConnectionRouter(originalRouter);
                            }
                            super.eraseConnectionMoveFeedback(request);
                        }
                    });
            // Allows the removal of the connection model element
            installEditPolicy(EditPolicy.CONNECTION_ROLE,
                    new ConnectionEditPolicy() {
                        @Override
                        protected Command getDeleteCommand(GroupRequest request) {
                            return new ConnectionDeleteCommand(getWidgetModel());
                        }
                    });
        }
    }

    @Override
    protected IFigure createFigure() {
        PolylineJumpConnection connection = new PolylineJumpConnection(this);
        connection.setLineStyle(getWidgetModel().getLineStyle());
        connection.setLineWidth(getWidgetModel().getLineWidth());
        connection.setForegroundColor(getWidgetModel().getLineColor()
                .getSWTColor());
        updateDecoration(connection);
        updateArrowLength(connection);
        updateRouter(connection);
        connection.setAntialias(getWidgetModel().isAntiAlias() ? SWT.ON
                : SWT.OFF);
        updateLineJumpProperties(connection);
        return connection;
    }

    private void updateLineJumpProperties(PolylineJumpConnection connection) {
        connection.setLineJumpSize(getWidgetModel().getLineJumpSize());
        connection.setLineJumpStyle(getWidgetModel().getLineJumpStyle());
        connection.setLineJumpAdd(getWidgetModel().getLineJumpAdd());
    }

    public static double angleOf(Point p1, Point p2) {
        final double deltaY = (p1.y - p2.y);
        final double deltaX = (p2.x - p1.x);
        final double result = Math.toDegrees(Math.atan2(deltaY, deltaX));
        return (result < 0) ? (360d + result) : result;
    }

    PointList getIntersectionPoints(PolylineJumpConnection connection) {
        intersectionMap = new HashMap<Point, PointList>();
        PointList pointsInConnection = connection.getPoints();
        ConnectionModel widgetModel = getWidgetModel();

        // Skip calculating intersections if line_jump_add is set to none for both lines
        if ((widgetModel.getLineJumpAdd() == LineJumpAdd.NONE)
                && (connection.getWidgetConnectionEditPart().getWidgetModel().getLineJumpAdd() == LineJumpAdd.NONE)) {
            return pointsInConnection;
        }

        PointList intersections = new PointList();
        int lineJumpSize = connection.getLineJumpSize();

        DisplayModel rootDisplayModel = widgetModel.getRootDisplayModel();
        while (rootDisplayModel.getParentDisplayModel() != null) {
           rootDisplayModel = rootDisplayModel.getParentDisplayModel();
        }
        List<ConnectionModel> connectionList = rootDisplayModel.getConnectionList();

        for (int i=0; (i+1)<pointsInConnection.size();) {
            Point x1y1 = pointsInConnection.getPoint(i);
            Point x2y2 = pointsInConnection.getPoint(i+1);
            /* The Manhattan connection in CS-Studio always has at least 3 segments, even if
             * they are invisible to the user, because he sees a straight line. But if these
             * invisible segments fall exactly where this connection intersects another
             * connection, this may confuse the logic into thinking that the line jump
             * should not be drawn because of the limitation that the line jump cannot be
             * drawn too close to the bend in the connection.
             * To overcome this problem, we do not simply follow the line segments, but
             * check whether subsequent line segments are actually in the same line
             * (vertically or horizontally). If this is the case, we join such segments
             * until we reach an actual bend or the end of the connection.
             * To achieve this we need to manipulate the index to skip the joined
             * segments.
             */
            i++;   // increase 'i' once for the next point (simple case)
            for (int j = (i+1); j < pointsInConnection.size(); j++) {
                // we may increase 'i' some more if we detect joined segments
                Point p = pointsInConnection.getPoint(j);
                if ((p.x() == x1y1.x()) || (p.y() == x1y1.y())) {
                    x2y2 = p;
                    i = j;
                } else {
                    break;
                }
            }
            int x1 = x1y1.x;
            int y1 = x1y1.y;
            int x2 = x2y2.x;
            int y2 = x2y2.y;

            intersections.addPoint(x1y1);
            List<Point> intersectionPointsList = new ArrayList<Point>();


            for (ConnectionModel connModel : connectionList) {
                if (connModel != getModel()) {

                    if (y1-y2 == 0) {
                        // line segment is horizontal, but neither connection has the property set to horizontal
                        if ((widgetModel.getLineJumpAdd() != LineJumpAdd.HORIZONTAL_LINES) && (connModel.getLineJumpAdd() != LineJumpAdd.HORIZONTAL_LINES)) {
                            continue;
                        }
                    } else if (x1-x2 == 0) {
                        // line segment is vertical, but neither connection has the property set to vertical
                        if ((widgetModel.getLineJumpAdd() != LineJumpAdd.VERTICAL_LINES) && (connModel.getLineJumpAdd() != LineJumpAdd.VERTICAL_LINES)) {
                            continue;
                        }
                    } else {
                        // skip for slanting lines
                        continue;
                    }

                    // this is some other connection, not this one
                    WidgetConnectionEditPart widgetConnectionEditPart = (WidgetConnectionEditPart) getViewer().getEditPartRegistry().get(connModel);
                    if (widgetConnectionEditPart == null) {
                        continue;
                    }
                    PolylineJumpConnection connectionFigure = widgetConnectionEditPart.getConnectionFigure();
                    PointList pointListOfConnection = connectionFigure.getPoints();

                    for (int j=0; (j+1)<pointListOfConnection.size(); j++) {
                        Point x3y3 = pointListOfConnection.getPoint(j);
                        Point x4y4 = pointListOfConnection.getPoint(j+1);

                        int x3 = x3y3.x;
                        int y3 = x3y3.y;
                        int x4 = x4y4.x;
                        int y4 = x4y4.y;

                        // Edge Case: Check if lines are parallel
                        if((x1 -x2)*(y3 - y4)-(y1 - y2)*(x3 - x4) != 0) {
                            // Calculate intersection point https://en.wikipedia.org/wiki/Line%E2%80%93line_intersection
                            // This calculates an intersection point between 2 lines that carry the (x1y1,x2y2) and
                            // (x3y3, x4y4) segments. But the intersection may be somewhere outside those two segments.
                            double itx = (x1*y2 - y1*x2)*(x3 - x4) - (x1-x2)*(x3*y4 - y3*x4);
                            itx = itx/( ((x1-x2)*(y3-y4)) - ((y1-y2)*(x3-x4)) );

                            double ity = (x1*y2 - y1*x2)*(y3 - y4) - (y1-y2)*(x3*y4 - y3*x4);
                            ity = ity/( ((x1-x2)*(y3-y4)) - ((y1-y2)*(x3-x4)) );

                            Point intersectionPoint = new Point( (int)itx, (int)ity);

                            // Edge case: intersection is very near to end point
                            // Ignore
                            if ((intersectionPoint.getDistance(x1y1) < lineJumpSize) || (intersectionPoint.getDistance(x2y2) < lineJumpSize)) {
                                continue;
                            }

                            // Check if intersection point is in both line segments
                            Polyline line1 = new Polyline();
                            line1.addPoint(new Point(x1, y1));
                            line1.addPoint(new Point(x2, y2));

                            Polyline line2 = new Polyline();
                            line2.addPoint(new Point(x3, y3));
                            line2.addPoint(new Point(x4, y4));
                            if (line1.containsPoint((int)itx, (int)ity) && line2.containsPoint((int)itx, (int)ity)) {
                                // Line segments intersect.
                                // Store intersection point and points x unit far away from intersection point

                                if (lineJumpSize > 0) {
                                    Point intersectionPoint1 = null;
                                    // Get point between intersection point and start point
                                    double d = x1y1.getDistance(intersectionPoint);
                                    double dt = lineJumpSize;

                                    // Edge Case: Intersection point is start point
                                    if (d < 0.9d) {
                                        intersectionPoint1 = x1y1;
                                    } else {
                                        double t = dt/d;

                                        double xit1 = (((1-t)*intersectionPoint.x) + t*x1);
                                        double yit1 = (((1-t)*intersectionPoint.y) + t*y1);

                                        intersectionPoint1 = new Point( (int)xit1, (int)yit1);
                                    }

                                    // Get point between intersection point and end point
                                    Point intersectionPoint2 = null;
                                    d = intersectionPoint.getDistance(x2y2);

                                    // Edge Case: Intersection point is end point
                                    if ( d < 0.9d) {
                                        intersectionPoint2 = x2y2;
                                    } else {
                                        double t = dt/d;

                                        double xit2 = (((1-t)*intersectionPoint.x) + t*x2);
                                        double yit2 = (((1-t)*intersectionPoint.y) + t*y2);

                                        intersectionPoint2 = new Point( (int)xit2, (int)yit2);
                                    }

                                    // Edge Case: It may happen that this points are out of bounds.
                                    // This will happen when line intersection is very near to end points.
                                    // If so correct it.
                                    Polyline line3 = new Polyline();
                                    line3.addPoint(x1y1);
                                    line3.addPoint(x2y2);
                                    if(!line3.containsPoint(intersectionPoint1)){
                                        intersectionPoint1 = x1y1;
                                    }
                                    if(!line3.containsPoint(intersectionPoint2)){
                                        intersectionPoint2 = x2y2;
                                    }

                                    intersectionPoint1 = normalizePoint(intersectionPoint1, x1y1, x2y2);
                                    intersectionPoint2 = normalizePoint(intersectionPoint2, x1y1, x2y2);

                                    intersectionPointsList.add(intersectionPoint1);
                                    intersectionPointsList.add(intersectionPoint2);

                                    PointList currentIntersectionPoints = new PointList();
                                    currentIntersectionPoints.addPoint(intersectionPoint1);
                                    currentIntersectionPoints.addPoint(intersectionPoint2);

                                    intersectionMap.put(intersectionPoint, currentIntersectionPoints);
                                }
                            }
                        }
                    }
                }
            }

            // Edge Case: While calculating intersection points, iterating on connections does
            // not guarantee order. Sort so that points are in order.
            Collections.sort(intersectionPointsList, new Comparator<Point>() {
                @Override
                public int compare(Point x1_, Point x2_) {
                    int result = Double.compare(x1_.getDistance(x1y1), x2_.getDistance(x1y1));
                    return result;
                }
            });

            for (Point p : intersectionPointsList) {
                intersections.addPoint(p);
            }
        }
        if (pointsInConnection.size() > 0) {
            intersections.addPoint(pointsInConnection.getLastPoint());
        }
        return intersections;
    }

    /**
     * Due to rounding errors, the point may not be exactly on the line.
     *
     * @param point the point to normalize
     * @param lineStart line start
     * @param lineEnd line end
     * @return normalized {@link Point} if the line is horizontal or vertical, unchanged otherwise
     */
    private Point normalizePoint(Point point, Point lineStart, Point lineEnd) {
        Point normalized = point.getCopy();
        if (lineStart.x() - lineEnd.x() == 0) {
            // vertical
            normalized.setX(lineStart.x());
        } else if (lineStart.y() - lineEnd.y() == 0) {
            // horizontal
            normalized.setY(lineStart.y());
        }
        return normalized;
    }

    private void updateDecoration(PolylineConnection connection) {
        switch (getWidgetModel().getArrowType()) {
        case None:
            // if(targetDecoration != null)
            // connection.remove(targetDecoration);
            targetDecoration = null;
            // if(sourceDecoration != null)
            // connection.remove(sourceDecoration);
            sourceDecoration = null;
            break;
        case From:
            // if(targetDecoration != null)
            // connection.remove(targetDecoration);
            targetDecoration = null;
            if (getWidgetModel().isFillArrow())
                sourceDecoration = new PolygonDecoration();
            else
                sourceDecoration = new PolylineDecoration();
            break;
        case To:
            // if(sourceDecoration != null)
            // connection.remove(sourceDecoration);
            sourceDecoration = null;
            if (getWidgetModel().isFillArrow())
                targetDecoration = new PolygonDecoration();
            else
                targetDecoration = new PolylineDecoration();
            break;
        case Both:
            if (getWidgetModel().isFillArrow()) {
                sourceDecoration = new PolygonDecoration();
                targetDecoration = new PolygonDecoration();
            } else {
                sourceDecoration = new PolylineDecoration();
                targetDecoration = new PolylineDecoration();
            }
            break;
        default:
            break;
        }
        if(targetDecoration != null)
            ((Shape)targetDecoration).setAntialias(
                    getWidgetModel().isAntiAlias()? SWT.ON : SWT.OFF);
        if(sourceDecoration != null)
            ((Shape)sourceDecoration).setAntialias(
                    getWidgetModel().isAntiAlias()? SWT.ON : SWT.OFF);
        connection.setTargetDecoration(targetDecoration);
        connection.setSourceDecoration(sourceDecoration);
    }

    private void updateArrowLength(PolylineConnection connection) {
        int l = getWidgetModel().getArrowLength();
        if (sourceDecoration != null) {
            if (sourceDecoration instanceof PolygonDecoration)
                ((PolygonDecoration) sourceDecoration).setScale(X_FACTOR * l,
                        Y_FACTOR * l);
            else
                ((PolylineDecoration) sourceDecoration).setScale(X_FACTOR * l,
                        Y_FACTOR * l);
            sourceDecoration.repaint();
        }
        if (targetDecoration != null) {
            if (targetDecoration instanceof PolygonDecoration)
                ((PolygonDecoration) targetDecoration).setScale(X_FACTOR * l,
                        Y_FACTOR * l);
            else
                ((PolylineDecoration) targetDecoration).setScale(X_FACTOR * l,
                        Y_FACTOR * l);
            targetDecoration.repaint();
        }
        connection.revalidate();
    }

    private void updateRouter(PolylineConnection connection) {
        ConnectionRouter router = ConnectionRouter.NULL;
        switch (getWidgetModel().getRouterType()) {
        case MANHATTAN:
            //Allow move bendpoint
            if(getExecutionMode()==ExecutionMode.EDIT_MODE)
                installEditPolicy(EditPolicy.CONNECTION_BENDPOINTS_ROLE,
                    new ManhattanBendpointEditPolicy());
            //no points, use manhattan
            if(getWidgetModel().getPoints().size() == 0){
                router = new ManhattanConnectionRouter();
                break;
            }
            //has points, use points for routing
            router = new FixedPointsConnectionRouter();
            ((FixedPointsConnectionRouter) router).setConnectionModel(getWidgetModel());
            connection.setConnectionRouter(router);
            refreshBendpoints(connection);
            return;
        case STRAIGHT_LINE:
            //no bendpoint
            if(getExecutionMode()==ExecutionMode.EDIT_MODE)
                installEditPolicy(EditPolicy.CONNECTION_BENDPOINTS_ROLE,
                    null);
            router = ConnectionRouter.NULL;
        default:
            break;
        }
        connection.setConnectionRouter(router);
    }

    /**
     * Updates the bendpoints, based on the model.
     * @param connection
     */
    protected void refreshBendpoints(PolylineConnection connection) {
        //Only work for manhattan router
        if (!(connection.getConnectionRouter() instanceof FixedPointsConnectionRouter))
            return;
        PointList points = getWidgetModel().getPoints().getCopy();
        if(points.size() ==0){
            points = connection.getPoints().getCopy();
            points.removePoint(0);
            points.removePoint(points.size()-1);
            getWidgetModel().setPoints(points);
        }
        connection.setRoutingConstraint(points);
    }

    public ConnectionModel getWidgetModel() {
        return (ConnectionModel) getModel();
    }

    @Override
    public PolylineJumpConnection getConnectionFigure() {
        return (PolylineJumpConnection) getFigure();
    }

    /**
     * @return the executionMode
     */
    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    /**
     * Set execution mode, this should be set before widget is activated.
     *
     * @param executionMode
     *            the executionMode to set
     */
    public void setExecutionMode(ExecutionMode executionMode) {
        this.executionMode = executionMode;
        getWidgetModel().setExecutionMode(executionMode);
    }

    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class key) {
        if (key == IActionFilter.class)
            return new IActionFilter() {
                @Override
                public boolean testAttribute(Object target, String name,
                        String value) {
                    if (name.equals("executionMode") && //$NON-NLS-1$
                            value.equals("EDIT_MODE") && //$NON-NLS-1$
                            getExecutionMode() == ExecutionMode.EDIT_MODE)
                        return true;
                    if (name.equals("executionMode") && //$NON-NLS-1$
                            value.equals("RUN_MODE") && //$NON-NLS-1$
                            getExecutionMode() == ExecutionMode.RUN_MODE)
                        return true;
                    return false;
                }

            };
        return super.getAdapter(key);
    }

    public void setPropertyValue(String propID, Object value){
        getWidgetModel().setPropertyValue(propID, value);
    }

    public HashMap<Point, PointList> getIntersectionMap() {
        return intersectionMap;
    }

}

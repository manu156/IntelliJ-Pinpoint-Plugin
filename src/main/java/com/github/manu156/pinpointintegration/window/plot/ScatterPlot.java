package com.github.manu156.pinpointintegration.window.plot;

import com.github.manu156.pinpointintegration.window.dto.TxnMetaDto;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class ScatterPlot extends JPanel {
    private static final Color SUCCESS_COLOR = new JBColor(new Color(85, 150, 50, 203), new Color(85, 150, 50, 203));
    private static final Color SUCCESS_COLOR_FADE = new JBColor(new Color(85, 150, 50, 111), new Color(85, 150, 50, 111));

    private static final Color FAIL_COLOR = new JBColor(new Color(150, 50, 77, 205), new Color(150, 50, 77, 205));
    private static final Color FAIL_COLOR_FADE = new JBColor(new Color(150, 50, 77, 111), new Color(150, 50, 77, 111));

    private static final Color SELECTOR_COLOR = new JBColor(new Color(217, 227, 255, 180), new Color(217, 227, 255, 180));
    private static final int POINT_RADIUS = 6;
    private static final int POINT_DIAMETER = POINT_RADIUS * 2;
    private static final int X_MARK_COUNT = 10;
    private static final int Y_MARK_COUNT = 5;
    private static final int X_MARGIN = 10 + 25; //normal margin + x-axis slider margin
    private static final int Y_MARGIN = 10 + 20; //normal margin + y-axis slider margin
    private static final int MARK_SIZE = 7;
    private final List<Long> xData;
    private final List<Long> yData;
    private final List<Boolean> success;
    private final List<Long> countData;
    private final List<String> tid;
    private double x1Selector;
    private double x2Selector;
    private double y1Selector;
    private double y2Selector;


    public ScatterPlot() {
        this.xData = new ArrayList<>();
        this.yData = new ArrayList<>();
        this.success = new ArrayList<>();
        this.countData = new ArrayList<>();
        this.tid = new ArrayList<>();
        setSelector(0D, 1D, 0D, 100D);
    }

    public void MoveSelector(Double xSelector1, Double xSelector2, Double ySelector1, Double ySelector2) {
        setSelector(xSelector1, xSelector2, ySelector1, ySelector2);
        this.revalidate();
        this.repaint();
    }

    public void setSelector(Double x1Selector, Double x2Selector, Double y1Selector, Double y2Selector) {
        if (null != x1Selector)
            this.y1Selector = x1Selector;
        if (null != x2Selector)
            this.y2Selector = x2Selector;
        if (null != y1Selector)
            this.x1Selector = y1Selector;
        if (null != y2Selector)
            this.x2Selector = y2Selector;
    }

    private int getYS(double y) {
        return Math.toIntExact(Math.round((getHeight() - Y_MARGIN) * (1 - y)));
    }

    private int getXS(double x) {
        return Math.toIntExact(Math.round(x * (getWidth() - X_MARGIN))) + X_MARGIN;
    }

    public void populateNewData(GraphData graphData) {
        this.yData.clear();
        this.xData.clear();
        this.success.clear();
        this.countData.clear();
        this.tid.clear();
        _addData(graphData.xData, graphData.yData, graphData.success, graphData.count, graphData.tid);
        this.repaint();
        this.revalidate();
    }

    public void addData(List<Long> xData, List<Long> yData, List<Boolean> success, List<Long> count, List<String> tid) {
        _addData(xData, yData, success, count, tid);
        this.revalidate();
        this.repaint();
    }

    private void _addData(List<Long> xData, List<Long> yData, List<Boolean> success, List<Long> count, List<String> tid) {
        this.xData.addAll(xData);
        this.yData.addAll(yData);
        this.success.addAll(success);
        this.countData.addAll(count);
        this.tid.addAll(tid);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        Color mainColor = g2.getColor();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double maxX = xData.stream().max(Comparator.naturalOrder()).stream().findFirst().orElse(1L);
        double minX = xData.stream().min(Comparator.naturalOrder()).stream().findFirst().orElse(0L);
        double maxY = 1.1 * yData.stream().max(Comparator.naturalOrder()).stream().findFirst().orElse(1L);
        double minY = yData.stream().min(Comparator.naturalOrder()).stream().findFirst().orElse(0L);
        double xDelta = maxX - minX;
        double yDelta = maxY - minY;
        double xScale = (getWidth() - X_MARGIN) / xDelta;
        double yScale = (getHeight() - Y_MARGIN) / yDelta;

        double maxCount = countData.stream().max(Comparator.naturalOrder()).stream().findFirst().orElse(1L);
        double minCount = countData.stream().min(Comparator.naturalOrder()).stream().findFirst().orElse(0L);
        if (minCount == maxCount) {
            maxCount += minCount;
        }


        List<Point> graphPoints = new ArrayList<>();
        for (int i = 0; i < xData.size(); i++) {
            int x1 = (int) ((xData.get(i) - minX) * xScale + X_MARGIN);
            int y1 = (int) ((maxY - yData.get(i)) * yScale);
            graphPoints.add(new Point(x1, y1));
        }


        // create hatch marks for y axis.
        int fontSize = g2.getFont().getSize();
        for (int i = 0; i < Y_MARK_COUNT; i++) {
            int x0 = X_MARGIN;
            int x1 = X_MARGIN - MARK_SIZE;
            int y0 = getHeight() - Y_MARGIN - (((i + 1) * (getHeight() - Y_MARGIN)) / Y_MARK_COUNT);
            int y1 = y0;
            if (!yData.isEmpty()) {
                g2.drawLine(x0, y0, x1, y1);
                if (i != Y_MARK_COUNT - 1) {
                    Double timeY1 = (getHeight() - Y_MARGIN - y1) / yScale / 1000D;
                    g2.drawString(timeY1.toString().substring(0, 4), 0, y0 + fontSize / 2);
                } else {
                    Color currentColor = g2.getColor();
                    g2.setColor(SUCCESS_COLOR_FADE);
                    g2.drawString("s", 0, y0 + fontSize);
                    g2.setColor(currentColor);
                }
            }
        }

        // and for x axis
        for (int i = 0; i < X_MARK_COUNT; i++) {
            int x0 = (i + 1) * (getWidth() - X_MARGIN) / (X_MARK_COUNT) + X_MARGIN;
            int x1 = x0;
            int y0 = getHeight() - Y_MARGIN;
            int y1 = y0 + MARK_SIZE;
            if (!xData.isEmpty()) {
                g2.drawLine(x0, y0, x1, y1);
                if (i != X_MARK_COUNT - 1) {
                    DateTimeFormatter tsFormat = DateTimeFormatter.ofPattern("hh:mm:ss");
                    long timeX = (long) (x0 / xScale + minX);
                    LocalDateTime ts =
                            Instant.ofEpochMilli(timeX).atZone(ZoneId.systemDefault()).toLocalDateTime();
                    g2.drawString(ts.format(tsFormat), x0 - 2 * fontSize, y1 + 15);
                } else {

                }
            }
        }

        Stroke oldStroke = g2.getStroke();
        renderSelectors(g2);

        g2.setStroke(oldStroke);
        for (int i = 0; i < graphPoints.size(); i++) {
            int x = graphPoints.get(i).x;
            int y = graphPoints.get(i).y;
            if (Boolean.TRUE.equals(success.get(i))) {
                if (((y < getYS(y1Selector)) && y < getYS(y2Selector)) || (y > getYS(y1Selector) && y > getYS(y2Selector)) ||
                        ((x < getXS(x1Selector)) && x < getXS(x2Selector)) || (x > getXS(x1Selector) && x > getXS(x2Selector)))
                    g2.setColor(SUCCESS_COLOR_FADE);
                else
                    g2.setColor(SUCCESS_COLOR);
            } else {
                if (((y < getYS(y1Selector)) && y < getYS(y2Selector)) || (y > getYS(y1Selector) && y > getYS(y2Selector)) ||
                        ((x < getXS(x1Selector)) && x < getXS(x2Selector)) || (x > getXS(x1Selector) && x > getXS(x2Selector)))
                    g2.setColor(FAIL_COLOR_FADE);
                else
                    g2.setColor(FAIL_COLOR);
            }

            double minRadius;
            double variableRadius;
            if (graphPoints.size() < 100) {
                minRadius = POINT_DIAMETER * 0.7;
                variableRadius = POINT_DIAMETER * 0.3 * countData.get(i) / (maxCount - minCount);
            } else {
                minRadius = POINT_DIAMETER * 0.3;
                variableRadius = POINT_DIAMETER * 0.7 * countData.get(i) / (maxCount - minCount);
            }
            double radius = minRadius + variableRadius;
            double diameter = 2 * radius;
            if (x < X_MARGIN + radius && y > getHeight() - Y_MARGIN - radius) {
                double thetaRad1 = Math.asin(Math.min((double) getHeight() - Y_MARGIN - y, radius) / radius);
                double startAngle = -1 * (180 / Math.PI) * thetaRad1;
                double thetaRad2 = Math.asin(Math.min(x - (double) X_MARGIN, radius) / radius);
                double arcAngle = -startAngle + 90 + (180 / Math.PI) * thetaRad2;
                Arc2D.Double point = new Arc2D.Double(x - radius, y - radius, diameter, diameter, startAngle, arcAngle, Arc2D.PIE);
                g2.fill(point);
                Path2D triangleX = new Path2D.Double();
                triangleX.moveTo(X_MARGIN, getHeight() - (double) Y_MARGIN);
                triangleX.lineTo(x + radius * Math.cos(thetaRad1), (double) getHeight() - Y_MARGIN);
                triangleX.lineTo(x, y);
                triangleX.closePath();
                g2.fill(triangleX);
                Path2D triangleY = new Path2D.Double();
                triangleY.moveTo(X_MARGIN, (double) getHeight() - Y_MARGIN);
                triangleY.lineTo(X_MARGIN, y - radius * Math.cos(thetaRad2));
                triangleY.lineTo(x, y);
                triangleY.closePath();
                g2.fill(triangleY);
            } else if (x < X_MARGIN + radius) {
                double thetaRad = Math.asin(Math.min(x - (double) X_MARGIN, radius) / radius);
                double startAngle = -90 - 1 * (180 / Math.PI) * thetaRad;
                double arcAngle = 2 * (180 / Math.PI) * thetaRad + 180;
                Arc2D.Double point = new Arc2D.Double(x - radius, y - radius, diameter, diameter, startAngle, arcAngle, Arc2D.PIE);
                g2.fill(point);
                Path2D triangle = new Path2D.Double();
                triangle.moveTo(X_MARGIN, y - radius * Math.cos(thetaRad));
                triangle.lineTo(X_MARGIN, y + radius * Math.cos(thetaRad));
                triangle.lineTo(x, y);
                triangle.closePath();
                g2.fill(triangle);
            } else if (y > getHeight() - Y_MARGIN - radius) {
                double thetaRad = Math.asin(Math.min((double) getHeight() - Y_MARGIN - y, radius) / (radius));
                double startAngle = -1 * (180 / Math.PI) * thetaRad;
                double arcAngle = 2 * (180 / Math.PI) * thetaRad + 180;
                Arc2D.Double point = new Arc2D.Double(x - radius, y - radius, diameter, diameter, startAngle, arcAngle, Arc2D.PIE);
                g2.fill(point);
                Path2D triangle = new Path2D.Double();
                triangle.moveTo(x - radius * Math.cos(thetaRad), getHeight() - (double) Y_MARGIN);
                triangle.lineTo(x + radius * Math.cos(thetaRad), getHeight() - (double) Y_MARGIN);
                triangle.lineTo(x, y);
                triangle.closePath();
                g2.fill(triangle);
            } else {
                Ellipse2D.Double ellipse = new Ellipse2D.Double(x - radius, y - radius, radius, radius);
                g2.fill(ellipse);
            }
        }
        g2.setColor(mainColor);
        g2.drawLine(X_MARGIN, getHeight() - Y_MARGIN, X_MARGIN, 0);
        g2.drawLine(X_MARGIN, getHeight() - Y_MARGIN, getWidth(), getHeight() - Y_MARGIN);
    }

    private void renderSelectors(Graphics2D g) {
        g.setColor(SELECTOR_COLOR);
//        g.setStroke(new BasicStroke(SELECTOR_COLOR));
        if (getYS(y1Selector) != 0 & getYS(y1Selector) != getHeight() - Y_MARGIN)
            g.drawLine(X_MARGIN, getYS(y1Selector), getWidth(), getYS(y1Selector));
        if (getYS(y2Selector) != 0 & getYS(y2Selector) != getHeight() - Y_MARGIN)
            g.drawLine(X_MARGIN, getYS(y2Selector), getWidth(), getYS(y2Selector));
        if (getXS(x1Selector) != X_MARGIN & getXS(x1Selector) != getWidth())
            g.drawLine(getXS(x1Selector), getHeight() - Y_MARGIN, getXS(x1Selector), 0);
        if (getXS(x2Selector) != X_MARGIN & getXS(x2Selector) != getWidth())
            g.drawLine(getXS(x2Selector), getHeight() - Y_MARGIN, getXS(x2Selector), 0);
    }


    public List<TxnMetaDto> getAllTxn() {
        List<TxnMetaDto> res = new ArrayList<>(xData.size());
        for (int i = 0; i < xData.size(); i++) {
            res.add(new TxnMetaDto(xData.get(i), tid.get(i), yData.get(i)));
        }
        return res;
    }

    public List<TxnMetaDto> getSelectedTxn() {
        List<TxnMetaDto> allTxn = getAllTxn();
        List<TxnMetaDto> res = new ArrayList<>();
        double maxX = xData.stream().max(Comparator.naturalOrder()).stream().findFirst().orElse(1L);
        double minX = xData.stream().min(Comparator.naturalOrder()).stream().findFirst().orElse(0L);
        double maxY = 1.1 * yData.stream().max(Comparator.naturalOrder()).stream().findFirst().orElse(1L);
        double minY = yData.stream().min(Comparator.naturalOrder()).stream().findFirst().orElse(0L);
        double xDelta = maxX - minX;
        double yDelta = maxY - minY;
        double xScale = (getWidth() - X_MARGIN) / xDelta;
        double yScale = (getHeight() - Y_MARGIN) / yDelta;
        for (TxnMetaDto txn : allTxn) {
            int x = (int) ((txn.t - minX) * xScale + X_MARGIN);
            int y = (int) ((maxY - txn.r) * yScale);
            if (((y < getYS(y1Selector)) && y < getYS(y2Selector)) || (y > getYS(y1Selector) && y > getYS(y2Selector)) ||
                    ((x < getXS(x1Selector)) && x < getXS(x2Selector)) || (x > getXS(x1Selector) && x > getXS(x2Selector))) {
                // ignore
            } else
                res.add(txn);

        }
        return res;
    }
}

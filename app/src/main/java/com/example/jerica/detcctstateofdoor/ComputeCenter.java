package com.example.jerica.detcctstateofdoor;

import org.opencv.core.Point;

import java.util.LinkedList;

/**
 * Created by Think on 2017/12/12.
 */
public class ComputeCenter {
    public static Point getCenterPoint(LinkedList<Point> list){
        Point p1 = new Point(list.get(0).x, list.get(0).y);
        Point p3 = new Point(list.get(2).x, list.get(2).y);
        double x = 0.5 * (p1.x + p3.x);
        double y = 0.5 * (p1.y + p3.y);
        Point p = new Point(x, y);
        return p;
    }
}

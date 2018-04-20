package com.example.jerica.detcctstateofdoor;

import org.opencv.core.Point;

import java.util.LinkedList;

/**
 * Created by Think on 2017/12/12.
 */
public class ComputeArea {
    public static double getArea(LinkedList<Point> list){
        double area = 0.0;
        Point p1 = new Point(list.get(0).x, list.get(0).y);
        Point p2 = new Point(list.get(1).x, list.get(1).y);
        Point p3 = new Point(list.get(2).x, list.get(2).y);
        Point p4 = new Point(list.get(3).x, list.get(3).y);
        double d12 = getLineDistance(p1, p2);
        double d23 = getLineDistance(p2, p3);
        double d34 = getLineDistance(p3, p4);
        double d41 = getLineDistance(p4, p1);
        double d24 = getLineDistance(p2, p4);
        double length1 = 0.5 * (d12 + d24 + d41);
        double length2 = 0.5 * (d23 + d24 + d34);
        double s1 = Math.sqrt(length1*(length1-d12)*(length1-d24)*(length1-d41));
        double s2 = Math.sqrt(length2*(length2-d23)*(length2-d24)*(length2-d34));
        area = s1 + s2;

        return area;
    }

    public static double getLineDistance(Point p1, Point p2){
        double d = Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
        return d;
    }
}

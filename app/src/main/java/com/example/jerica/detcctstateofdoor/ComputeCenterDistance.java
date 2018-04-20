package com.example.jerica.detcctstateofdoor;

import org.opencv.core.Point;

/**
 * Created by dream on 2018/4/11.
 */

public class ComputeCenterDistance {
    private static final String TAG = ComputeCenterDistance.class.getName();

    public static double getCenterDistance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }
}

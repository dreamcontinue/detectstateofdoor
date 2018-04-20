package com.example.jerica.detcctstateofdoor;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.Point;

/**
 * Created by Think on 2017/12/19.
 */
public class Judge {
    private static final String TAG = Judge.class.getName();

    public enum DOOR_STATE {
        OPEN, CLOSE, UNKNOWN;
    }

    public static DOOR_STATE getFlag(double area1, Point p1, double area2, Point p2) {
//        DOOR_STATE state = DOOR_STATE.CLOSE;
        //judge
        /*
         *judge the coordinates of center points first,
		 *if there is a big gap between them, then return ture,
		 *or continue to judge the area
		*/
//        double tolerance_dist = 0;
//        double tolerance_area = 0.95;
        double diff_y = Math.abs(p1.y - p2.y);
        double per_area = Math.abs(area1 - area2) / area1;
        double distance = ComputeCenterDistance.getCenterDistance(p1, p2);
        if (distance < 20 && diff_y < 28 && per_area < 0.15)
            return DOOR_STATE.CLOSE;
        else if (distance >= 20 || diff_y >= 28) return DOOR_STATE.UNKNOWN;
        else return DOOR_STATE.OPEN;
//        MainForm.distance.setText(String.format("%.2f", distance));
//        Log.d(TAG, "distance = " + distance);
//        return distance >= 5 ? (diff_y >=/*18*/28 ? DOOR_STATE.UNKNOWN : DOOR_STATE.OPEN) : DOOR_STATE.CLOSE;
//        if (distance >= 5) {
//            state=diff_y>=18?DOOR_STATE.UNKNOWN:DOOR_STATE.OPEN;
//        }
//        return state;
    }

}

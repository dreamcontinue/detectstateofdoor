package com.example.jerica.detcctstateofdoor;

import android.media.FaceDetector;
import android.util.Log;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FastFeatureDetector;
import org.opencv.features2d.FeatureDetector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by Think on 2017/12/19.
 */


public class SIFT {
    private static final String TAG = "SIFT";

    /**
     * @param img_object 模板图像
     * @param img_scene  待匹配图像
     * @return list             模板图像的四个顶点坐标在待匹配图像中坐标
     */
    public static LinkedList<Point> getPointList(Mat img_object, Mat img_scene) {
//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//        loadLibraries();
        //init detector
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.SIFT);
//        FeatureDetector detector = FeatureDetector.create(algorithm_type==1?FeatureDetector.SIFT: FeatureDetector.SURF);
//        FastFeatureDetector detector=FastFeatureDetector.create(1,true,FeatureDetector.SIFT);
        //keypoint detection for both images (keypoints_object for img_object, keypoints_scene for img_scene)
        MatOfKeyPoint keypoints_object = new MatOfKeyPoint();
        MatOfKeyPoint keypoints_scene = new MatOfKeyPoint();
        detector.detect(img_object, keypoints_object);

        detector.detect(img_scene, keypoints_scene);
//        System.out.println("模板图像特征点个数: " + keypoints_object.total());
//        System.out.println("待匹配图像特征点个数: " + keypoints_scene.total());

        //init extractor
//        DescriptorExtractor extractor = DescriptorExtractor.create(algorithm_type==1?FeatureDetector.SIFT: FeatureDetector.SURF);
        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.SIFT);

        Mat descriptor_object = new Mat();
        Mat descriptor_scene = new Mat();

        //Calculate descriptors
        extractor.compute(img_object, keypoints_object, descriptor_object);
        extractor.compute(img_scene, keypoints_scene, descriptor_scene);

        //init matcher
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_L1);
        matcher.clear();
        List<MatOfDMatch> matches = new ArrayList<MatOfDMatch>();

        //match both descriptors
        matcher.knnMatch(descriptor_object, descriptor_scene, matches, 2);


        //filter good matches, ratio test
        LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
        for (Iterator<MatOfDMatch> iterator = matches.iterator(); iterator.hasNext(); ) {
            MatOfDMatch matOfDMatch = (MatOfDMatch) iterator.next();
            try {
                if (matOfDMatch.toArray()[0].distance / matOfDMatch.toArray()[1].distance < 0.85)
                    good_matches.add(matOfDMatch.toArray()[0]);
            } catch (Exception e) {
                return null;
            }
        }


        // get keypoint coordinates of good matches to find homography and remove outliers using ransac
        LinkedList<Point> objList = new LinkedList<Point>();
        LinkedList<Point> sceneList = new LinkedList<Point>();

        List<KeyPoint> keypoints_objectList = keypoints_object.toList();
        List<KeyPoint> keypoints_sceneList = keypoints_scene.toList();

        for (int i = 0; i < good_matches.size(); i++) {
            objList.addLast(keypoints_objectList.get(good_matches.get(i).queryIdx).pt);
            sceneList.addLast(keypoints_sceneList.get(good_matches.get(i).trainIdx).pt);
        }

        MatOfPoint2f obj = new MatOfPoint2f();
        obj.fromList(objList);

        MatOfPoint2f scene = new MatOfPoint2f();
        scene.fromList(sceneList);

        Mat outputMask = new Mat();
        // Find homography - here just used to perform match filtering with RANSAC, but could be used to e.g. stitch images
        // the smaller the allowed reprojection error (here 15), the more matches are filtered
//添加  2d3d的exception
        if (obj.empty() || scene.empty()) return null;
//        maxIters 最大迭代次数  confidence 置信度
        Mat hg = Calib3d.findHomography(obj, scene, Calib3d.RANSAC, 10, outputMask, 2000, 0.995);
//添加  空异常
        if (hg.empty()) return null;

//        Mat hg = Calib3d.findHomography(obj, scene, Calib3d.RANSAC, 10, outputMask);

        // outputMask contains zeros and ones indicating which matches are filtered
        LinkedList<DMatch> better_matches = new LinkedList<DMatch>();
        for (int i = 0; i < good_matches.size(); i++) {
            if (outputMask.get(i, 0)[0] != 0.0) {
                better_matches.add(good_matches.get(i));
            }
        }

        // this will draw all matches, works fine
        MatOfDMatch better_matches_mat = new MatOfDMatch();
        better_matches_mat.fromList(better_matches);


        //init corners
        Mat obj_corners = new Mat(4, 1, CvType.CV_32FC2);
        Mat scene_corners = new Mat(4, 1, CvType.CV_32FC2);

        obj_corners.put(0, 0, new double[]{0, 0});
        obj_corners.put(1, 0, new double[]{img_object.cols(), 0});
        obj_corners.put(2, 0, new double[]{img_object.cols(), img_object.rows()});
        obj_corners.put(3, 0, new double[]{0, img_object.rows()});

        //transform obj corners to scene_img (stored in scene_corners)
        Core.perspectiveTransform(obj_corners, scene_corners, hg);

        //move points for img_obg width to the right to fit the matching image
//        Point p1 = new Point(scene_corners.get(0, 0)[0] + img_object.cols(), scene_corners.get(0, 0)[1]);
//        Point p2 = new Point(scene_corners.get(1, 0)[0] + img_object.cols(), scene_corners.get(1, 0)[1]);
//        Point p3 = new Point(scene_corners.get(2, 0)[0] + img_object.cols(), scene_corners.get(2, 0)[1]);
//        Point p4 = new Point(scene_corners.get(3, 0)[0] + img_object.cols(), scene_corners.get(3, 0)[1]);
        Point p1 = new Point(scene_corners.get(0, 0)[0], scene_corners.get(0, 0)[1]);
        Point p2 = new Point(scene_corners.get(1, 0)[0], scene_corners.get(1, 0)[1]);
        Point p3 = new Point(scene_corners.get(2, 0)[0], scene_corners.get(2, 0)[1]);
        Point p4 = new Point(scene_corners.get(3, 0)[0], scene_corners.get(3, 0)[1]);

        //store the four points in a list
        LinkedList<Point> list = new LinkedList<Point>();
        list.addLast(p1);
        list.addLast(p2);
        list.addLast(p3);
        list.addLast(p4);

        //create the matching image
        Mat img_matches = new Mat();    //mat for resulting image
//        Features2d.drawMatches(img_object, keypoints_object, img_scene, keypoints_scene, better_matches_mat, img_matches);

//        //draw lines to the matching image
//        Core.line(img_matches, p1, p2, new Scalar(0, 255, 0), 4);
//        Core.line(img_matches, p2, p3, new Scalar(0, 255, 0), 4);
//        Core.line(img_matches, p3, p4, new Scalar(0, 255, 0), 4);
//        Core.line(img_matches, p4, p1, new Scalar(0, 255, 0), 4);

        //save img_matches
//        Highgui.imwrite("F:/EclipseProject/DetectStateofDoor/knnmatchResult/scene2_1-pattern1/img_matches" + count + ".png", img_matches);
        return list;
    }

//    private static void loadLibraries() {
//
//        try {
//            InputStream in = null;
//            File fileOut = null;
//            String osName = System.getProperty("os.name");
//            String opencvpath = System.getProperty("user.dir");
//            if(osName.startsWith("Windows")) {
//                int bitness = Integer.parseInt(System.getProperty("sun.arch.data.model"));
//                if(bitness == 32) {
//                    opencvpath=opencvpath+"\\opencv\\x86\\";
//                }
//                else if (bitness == 64) {
//                    opencvpath=opencvpath+"\\opencv\\x64\\";
//                } else {
//                    opencvpath=opencvpath+"\\opencv\\x86\\";
//                }
//            }
//            else if(osName.equals("Mac OS X")){
//                opencvpath = opencvpath+"Your path to .dylib";
//            }
//            System.out.println(opencvpath);
//            System.load(opencvpath + Core.NATIVE_LIBRARY_NAME + ".dll");
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to load opencv native library", e);
//        }

    /**
     * @param img_object 模板图像
     * @param img_scene  待匹配图像
     * @return list             模板图像的四个顶点坐标在待匹配图像中坐标
     */
    public static LinkedList<Point> getPointList(Mat img_object, Mat img_scene, int type) {
//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//        loadLibraries();
        //init detector
        FeatureDetector detector = FeatureDetector.create(type == 1 ? FeatureDetector.SIFT : FeatureDetector.SURF);
//        FeatureDetector detector = FeatureDetector.create(algorithm_type==1?FeatureDetector.SIFT: FeatureDetector.SURF);
//        FastFeatureDetector detector=FastFeatureDetector.create(1,true,FeatureDetector.SIFT);
        //keypoint detection for both images (keypoints_object for img_object, keypoints_scene for img_scene)
        MatOfKeyPoint keypoints_object = new MatOfKeyPoint();
        MatOfKeyPoint keypoints_scene = new MatOfKeyPoint();
        detector.detect(img_object, keypoints_object);

        detector.detect(img_scene, keypoints_scene);
//        System.out.println("模板图像特征点个数: " + keypoints_object.total());
//        System.out.println("待匹配图像特征点个数: " + keypoints_scene.total());

        //init extractor
//        DescriptorExtractor extractor = DescriptorExtractor.create(algorithm_type==1?FeatureDetector.SIFT: FeatureDetector.SURF);
        DescriptorExtractor extractor = DescriptorExtractor.create(type == 1 ? DescriptorExtractor.SIFT : DescriptorExtractor.SURF);

        Mat descriptor_object = new Mat();
        Mat descriptor_scene = new Mat();

        //Calculate descriptors
        extractor.compute(img_object, keypoints_object, descriptor_object);
        extractor.compute(img_scene, keypoints_scene, descriptor_scene);

        //init matcher
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_L1);
        matcher.clear();
        List<MatOfDMatch> matches = new ArrayList<MatOfDMatch>();

        //match both descriptors
        matcher.knnMatch(descriptor_object, descriptor_scene, matches, 2);


        //filter good matches, ratio test
        LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
        for (Iterator<MatOfDMatch> iterator = matches.iterator(); iterator.hasNext(); ) {
            MatOfDMatch matOfDMatch = (MatOfDMatch) iterator.next();
            try {
                if (matOfDMatch.toArray()[0].distance / matOfDMatch.toArray()[1].distance < 0.85)
                    good_matches.add(matOfDMatch.toArray()[0]);
            } catch (Exception e) {
                return null;
            }
        }


        // get keypoint coordinates of good matches to find homography and remove outliers using ransac
        LinkedList<Point> objList = new LinkedList<Point>();
        LinkedList<Point> sceneList = new LinkedList<Point>();

        List<KeyPoint> keypoints_objectList = keypoints_object.toList();
        List<KeyPoint> keypoints_sceneList = keypoints_scene.toList();

        for (int i = 0; i < good_matches.size(); i++) {
            objList.addLast(keypoints_objectList.get(good_matches.get(i).queryIdx).pt);
            sceneList.addLast(keypoints_sceneList.get(good_matches.get(i).trainIdx).pt);
        }

        MatOfPoint2f obj = new MatOfPoint2f();
        obj.fromList(objList);

        MatOfPoint2f scene = new MatOfPoint2f();
        scene.fromList(sceneList);

        Mat outputMask = new Mat();
        // Find homography - here just used to perform match filtering with RANSAC, but could be used to e.g. stitch images
        // the smaller the allowed reprojection error (here 15), the more matches are filtered
//添加  2d3d的exception
        if (obj.empty() || scene.empty()) return null;
//        maxIters 最大迭代次数  confidence 置信度
        Mat hg = Calib3d.findHomography(obj, scene, Calib3d.RANSAC, 10, outputMask, 2000, 0.995);
//添加  空异常
        if (hg.empty()) return null;

//        Mat hg = Calib3d.findHomography(obj, scene, Calib3d.RANSAC, 10, outputMask);

        // outputMask contains zeros and ones indicating which matches are filtered
        LinkedList<DMatch> better_matches = new LinkedList<DMatch>();
        for (int i = 0; i < good_matches.size(); i++) {
            if (outputMask.get(i, 0)[0] != 0.0) {
                better_matches.add(good_matches.get(i));
            }
        }

        // this will draw all matches, works fine
        MatOfDMatch better_matches_mat = new MatOfDMatch();
        better_matches_mat.fromList(better_matches);


        //init corners
        Mat obj_corners = new Mat(4, 1, CvType.CV_32FC2);
        Mat scene_corners = new Mat(4, 1, CvType.CV_32FC2);

        obj_corners.put(0, 0, new double[]{0, 0});
        obj_corners.put(1, 0, new double[]{img_object.cols(), 0});
        obj_corners.put(2, 0, new double[]{img_object.cols(), img_object.rows()});
        obj_corners.put(3, 0, new double[]{0, img_object.rows()});

        //transform obj corners to scene_img (stored in scene_corners)
        Core.perspectiveTransform(obj_corners, scene_corners, hg);

        Point p1 = new Point(scene_corners.get(0, 0)[0], scene_corners.get(0, 0)[1]);
        Point p2 = new Point(scene_corners.get(1, 0)[0], scene_corners.get(1, 0)[1]);
        Point p3 = new Point(scene_corners.get(2, 0)[0], scene_corners.get(2, 0)[1]);
        Point p4 = new Point(scene_corners.get(3, 0)[0], scene_corners.get(3, 0)[1]);

        //store the four points in a list
        LinkedList<Point> list = new LinkedList<Point>();
        list.addLast(p1);
        list.addLast(p2);
        list.addLast(p3);
        list.addLast(p4);

        //create the matching image
        Mat img_matches = new Mat();    //mat for resulting image
        return list;
    }
}

package biensotest;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class TestBienSo {
	public static void main(String[] args) {
		System.load("C:\\Opencv\\build\\java\\x64\\opencv_java430.dll");

		try {
			testBienso();

		} catch (Exception e) {
		}
		System.exit(0);
	}

	// ==================================================================================================================
	// do thu bien so
	public static void testBienso() {

		Mat src1 = Imgcodecs.imread("D:/BienSo/bien-so-xe-hoi.jpg");
		Mat src2 = src1.clone(); // copy anh
		Mat src3=src1.clone();
		Mat gray = new Mat(), binary = new Mat();
		Imgproc.cvtColor(src1, gray, Imgproc.COLOR_RGB2GRAY);
		Imgproc.threshold(gray, binary, 100, 255, Imgproc.THRESH_BINARY);
		HighGui.imshow("Anh nhi phan goc", binary);
		Mat morpho = new Mat();
		Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(3, 3), new Point(1, 1));
		Imgproc.erode(binary, morpho, element, new Point(-1, -1), 3);

		HighGui.imshow("Anh sau khi thuc hien phep gian no", morpho);
		List<MatOfPoint> contours1 = new ArrayList<>();
		Imgproc.findContours(binary, contours1, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

		for (int i = 0; i < contours1.size(); i++) {
			Rect r = Imgproc.boundingRect(contours1.get(i));
			if (r.width / (double) r.height > 3.5f && r.width / (double) r.height < 4.5f)
				Imgproc.rectangle(src1, r, new Scalar(0, 0, 255), 2, 8, 0);
			else
				Imgproc.rectangle(src1, r, new Scalar(0, 255, 0), 1, 8, 0);
		}
		HighGui.imshow("Ket qua phat hien truoc phep gian no", src1);
		List<MatOfPoint> contours2 = new ArrayList<>();
		Imgproc.findContours(morpho, contours2, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
		for (int i = 0; i < contours2.size(); i++) {
			Rect r = Imgproc.boundingRect(contours2.get(i));
			if (r.width / (double) r.height > 3.5f && r.width / (double) r.height < 4.5f)
				Imgproc.rectangle(src2, r, new Scalar(0, 0, 255), 2, 8, 0);
			else
				Imgproc.rectangle(src2, r, new Scalar(0, 255, 0), 1, 8, 0);
		}
		HighGui.imshow("Ket qua phat hien sau khi phep gian no", src2);
		HighGui.waitKey(0);
	}


}

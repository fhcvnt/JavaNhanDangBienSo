package biensotest;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class TestFloodfill2 {
	public static void main(String[] args) {
		System.load("C:\\Opencv\\build\\java\\x64\\opencv_java430.dll");

		Mat image = Imgcodecs.imread("D:/Bienso/bs5.jpg", Imgcodecs.IMREAD_GRAYSCALE);
		Mat src = new Mat();
		Mat threshold_image = new Mat();

		// Danh sách các hình chữ nhật bao quanh các ký tự, để cắt ra ký tự
		List<Rect> listrect = new ArrayList<>();

		// Remove noise giảm noise và tăng edge(làm egde thêm sắc nh�?n edges sharp)
		Mat noise_removal = new Mat();
		Imgproc.bilateralFilter(image, noise_removal, 5, 75, 75);

		image = noise_removal.clone();
		// resize image cho chi�?u ngang ảnh bằng 300
		Imgproc.resize(image, image, new Size(300, image.height() * 300 / image.width()));
		src = image.clone();

		threshold_image = new Mat();
		Imgproc.adaptiveThreshold(image, threshold_image, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
				Imgproc.THRESH_BINARY_INV, 35, 5);
		Mat anhnhiphan = threshold_image.clone();

		Rect rectij = new Rect();

		Imgproc.floodFill(anhnhiphan, new Mat(), new Point(200, 140), new Scalar(180), rectij, new Scalar(0));

		HighGui.imshow("anh nhi phan floodfill", anhnhiphan);
		Mat matfill = new Mat();
		matfill = new Mat(anhnhiphan, rectij);
		HighGui.imshow("anh cat floodfill", matfill);

		Mat kytuchuan = matfill.clone();

		for (int j = 0; j < matfill.size().height; j++) {
			for (int i = 0; i < matfill.size().width; i++) {
				double[] dataij = matfill.get(j, i);
				try {
					if (dataij[0] == 255) {
						kytuchuan.put(j, i, dataij);
					} else if (dataij[0] == 0) {
						dataij[0] = 255;
						kytuchuan.put(j, i, dataij);
					} else {
						dataij[0] = 0;
						kytuchuan.put(j, i, dataij);
					}
				} catch (NullPointerException ne) {
					ne.printStackTrace();
				}
			}
		}

		HighGui.imshow("anh cat chuan", kytuchuan);
		HighGui.waitKey(0);
		System.exit(0);
	}
}

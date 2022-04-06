package nhandangbienso;

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

public class TestFloodfill {
	public static void main(String[] args) {
		System.load("C:\\Opencv\\build\\java\\x64\\opencv_java430.dll");

		Mat image = Imgcodecs.imread("D:/Bienso/floodfill.jpg", Imgcodecs.IMREAD_GRAYSCALE);
		Mat src = new Mat();
		Mat threshold_image = new Mat();

		// Danh sách ảnh ký tự đã được cắt ra và nhị phân luôn
		List<Mat> danhsachkytu = new ArrayList<>();

		// tính tỷ lệ chiều ngang, chiều cao ảnh, nếu chiều ngang/chiều cao >1.5 thì ta
		// sẽ reszie ảnh về tỷ lệ 1.3
		if (image.width() / (float) image.height() > 1.5) {
			Imgproc.resize(image, image, new Size(image.height() * 1.3, image.height()));

			// Remove noise giảm noise và tăng edge(làm egde thêm sắc nhọn edges sharp)
			Mat noise_removal = new Mat();
			Imgproc.bilateralFilter(image, noise_removal, 5, 75, 75);

			image = noise_removal.clone();
			// resize image cho chiều ngang ảnh bằng 300
			Imgproc.resize(image, image, new Size(300, image.height() * 300 / image.width()));
			src = image.clone();

			threshold_image = new Mat();
			Imgproc.adaptiveThreshold(image, threshold_image, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
					Imgproc.THRESH_BINARY_INV, 35, 5);
			danhsachkytu.clear();
			Mat image_nhiphan = threshold_image.clone();
			for (int y = 0; y < threshold_image.size().height; y++) {
				for (int x = 0; x < threshold_image.size().width; x++) {
					double[] data = threshold_image.get(y, x);
					try {
						if (data[0] != 255) {
							continue;
						}
					} catch (NullPointerException ne) {
						ne.printStackTrace();
					}

					Rect rectxy = new Rect();
					// Tách ký tự bằng floodfill
					Imgproc.floodFill(threshold_image, new Mat(), new Point(x, y), new Scalar(180), rectxy,
							new Scalar(0));
					if (rectxy.width < src.width() / 4 && rectxy.width > src.width() / 17
							&& (float) rectxy.height < src.height() / 1.8 && rectxy.height > src.height() / 5) {

						// Cắt ký tự khỏi biển số và loại bỏ nền thừa chỉ còn ký tự màu đen nền trắng
						Mat anhnhiphan = image_nhiphan.clone();
						Rect rectij = new Rect();

						Imgproc.floodFill(anhnhiphan, new Mat(), new Point(x, y), new Scalar(180), rectij,
								new Scalar(0));

						Mat matfill = new Mat();
						matfill = new Mat(anhnhiphan, rectij);

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
						danhsachkytu.add(kytuchuan);
					}
				}
			}
		} else {
			// Remove noise giảm noise và tăng edge(làm egde thêm sắc nhọn edges sharp)
			Mat noise_removal = new Mat();
			Imgproc.bilateralFilter(image, noise_removal, 5, 75, 75);

			image = noise_removal.clone();
			// resize image cho chiều ngang ảnh bằng 300
			Imgproc.resize(image, image, new Size(300, image.height() * 300 / image.width()));
			src = image.clone();

			threshold_image = new Mat();
			Imgproc.adaptiveThreshold(image, threshold_image, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
					Imgproc.THRESH_BINARY_INV, 35, 5);

			danhsachkytu.clear();
			Mat image_nhiphan = threshold_image.clone();
			for (int y = 0; y < threshold_image.size().height; y++) {
				for (int x = 0; x < threshold_image.size().width; x++) {
					double[] data = threshold_image.get(y, x);
					try {
						if (data[0] != 255) {
							continue;
						}
					} catch (NullPointerException ne) {
						ne.printStackTrace();
					}

					Rect rectxy = new Rect();
					// Tách ký tự bằng floodfill
					Imgproc.floodFill(threshold_image, new Mat(), new Point(x, y), new Scalar(180), rectxy,
							new Scalar(0));
					if (rectxy.width < src.width() / 5 && rectxy.width > src.width() / 17
							&& rectxy.height < src.height() / 2 && rectxy.height > src.height() / 4) {

						// Cắt ký tự khỏi biển số và loại bỏ nền thừa chỉ còn ký tự màu đen nền trắng
						Mat anhnhiphan = image_nhiphan.clone();
						Rect rectij = new Rect();

						Imgproc.floodFill(anhnhiphan, new Mat(), new Point(x, y), new Scalar(180), rectij,
								new Scalar(0));

						Mat matfill = new Mat();
						matfill = new Mat(anhnhiphan, rectij);

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
						danhsachkytu.add(kytuchuan);
					}
				}
			}
		}

		try {
			int count = 0;
			for (Mat kytu : danhsachkytu) {
				HighGui.imshow("ky tu " + count, kytu);
				// Lưu ảnh vào D:/Bienso/Save
				// Imgcodecs.imwrite("D:/Bienso/Save/image"+count, kytu);
				count++;
			}
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		HighGui.imshow("anh nhi phan floodfill", threshold_image);
		HighGui.waitKey(0);
		System.exit(0);
	}
}

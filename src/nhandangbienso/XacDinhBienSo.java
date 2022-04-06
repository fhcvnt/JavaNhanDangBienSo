package nhandangbienso;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class XacDinhBienSo {
	private String filename = "";
	private Mat anhgoc = new Mat();
	private Rect rectvitribienso;

	public XacDinhBienSo(String tenfile) {
		try {
			Mat im = Imgcodecs.imread(tenfile); // ảnh gốc
			anhgoc = im.clone();
			// Chuyển ảnh xám
			Mat im_gray = new Mat();
			Imgproc.cvtColor(im, im_gray, Imgproc.COLOR_BGR2GRAY);

			// Remove noise giảm noise và tăng edge(làm egde thêm sắc nhọn edges sharp)
			Mat noise_removal = new Mat();
			Imgproc.bilateralFilter(im_gray, noise_removal, 9, 75, 75);

			// Làm mờ ảnh
			// Imgproc.GaussianBlur(im_gray, im_gray, new Size(9, 9), 2);

			// Cân bằng lại histogram làm cho ảnh không quá sáng hoặc tối
			Mat equal_histogram = new Mat();
			Imgproc.equalizeHist(noise_removal, equal_histogram);

			// Morphogoly open mục đích là giảm egde nhiễu , egde thật thêm sắc nhọn bằng
			// cv2.morphologyEx sử dụng kerel 5x5
			Mat kernel = new Mat();
			Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
			Mat morph_image = new Mat();
			Imgproc.morphologyEx(equal_histogram, morph_image, Imgproc.MORPH_OPEN, kernel);

			// nhị phân ảnh động (ảnh trắng đen)
			Mat thresh_image = new Mat();
			Imgproc.adaptiveThreshold(morph_image, thresh_image, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
					Imgproc.THRESH_BINARY, 35, 5);

			// tìm biên ảnh
			Mat canny_image = new Mat();
			Imgproc.Canny(thresh_image, canny_image, 250, 255);
			kernel = Mat.ones(new Size(2, 2), CvType.CV_32F); // Size(2, 2) là tìm theo hình chữ nhật có cạnh

			// dilate để tăng sharp cho egde
			Mat dilated_image = new Mat();
			Imgproc.dilate(canny_image, dilated_image, kernel);

			// tìm các contours là các hình chữ nhật bao quanh đối tượng thỏa điều kiện tìm
			// của giải thuật canny
			List<MatOfPoint> contours = new ArrayList<>();
			Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

			// vẽ hình chữ nhật quanh biển số
			// Biển số xe mô tô Chiều cao 140 mm, chiều dài 190 mm
			/*
			 * kiểm tra xem đâu là biển số trong danh sách các đường biên có được nếu là
			 * biển số thì phải chứa ít nhất 7 khung hình chữ nhật chứa các ký tự
			 */
			int dem = 0;// Đếm số ký tự trong biển số
			int vitribienso = -1; // vị trí biển số trong contours
			List<Integer> listchar = new ArrayList<>();
			for (int i = 0; i < contours.size(); i++) {
				Rect r = Imgproc.boundingRect(contours.get(i));
				if (r.width / (double) r.height > 1.0 && r.width / (double) r.height < 1.5) {
					dem = 0;// Đếm số ký tự trong biển số
					// Danh sách vị trí ký tự trong contours
					listchar.clear(); // làm rỗng danh sách
					boolean duplicate = false; // kiểm tra ký tự bị nhân đôi
					for (int check = 0; check < contours.size(); check++) {
						if (check != i) {
							Rect rcheck = Imgproc.boundingRect(contours.get(check));
							if (!(rcheck.width / (double) rcheck.height > 1.0
									&& rcheck.width / (double) rcheck.height < 1.5)) {
								// Tìm ký tự nằm trong biển số
								if ((rcheck.x > r.x && rcheck.x < r.x + r.width) && rcheck.width < rcheck.height
										&& (rcheck.y > r.y && rcheck.y < r.y + r.height)
										&& (rcheck.height > (float) r.height / 3
												&& rcheck.height < (float) r.height / 2)) {
									duplicate = false;
									// loại bỏ những ký tự bị nhân đôi
									for (int k = 0; k < listchar.size(); k++) {
										try {
											Rect rcheckduplicate = Imgproc.boundingRect(contours.get(listchar.get(k)));
											if (listchar.get(k) != check) {
												if (((rcheck.x + rcheck.width / 2 > rcheckduplicate.x)
														&& (rcheck.x + rcheck.width / 2) < (rcheckduplicate.x
																+ rcheckduplicate.width)
														&& (rcheck.y + rcheck.height / 2) > rcheckduplicate.y
														&& (rcheck.y + rcheck.height / 2) < (rcheckduplicate.y
																+ rcheckduplicate.height))) {
													duplicate = true;
												}
											}
										} catch (Exception exc) {
										}
									}
									if (!duplicate) {
										dem++;
										listchar.add(check);
										Imgproc.rectangle(im, rcheck, new Scalar(255, 0, 0), 1, 8, 0);
									}
								}
							}
						}
					}
				}
				if (dem > 3) {
					vitribienso = i;
				}
				// Kiểm tra nếu tìm được biển số thì thoát khỏi vòng lặp
				if (vitribienso >= 0) {
					break;
				}
			}
		} catch (Exception ex) {

		}
	}
}

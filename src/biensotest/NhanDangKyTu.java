package biensotest;

import java.io.File;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.TermCriteria;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.Ml;
import org.opencv.ml.SVM;

public class NhanDangKyTu {
	public static void main(String[] args) {
		System.load("C:\\Opencv\\build\\java\\x64\\opencv_java430.dll");

		int height = 0;
		int width = 0; // ảnh 22*40

		String DATABASE = "D:\\Bienso\\Data";
		// tính height cao nhất, width cao nhất
		File[] directories = new File(DATABASE).listFiles();
		for (int i = 0; i < directories.length; i++) {
			File[] files = directories[i].listFiles();
			for (int j = 0; j < files.length; j++) {
				Mat image2 = Imgcodecs.imread(files[j].getAbsolutePath());
				if (width < image2.size().width * image2.size().height) {
					width = (int) image2.size().width * (int) image2.size().height;
				}
				height++;
			}
		}

		// Danh sách ký tự trong biển số xe
		String[] danhsachkytu = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G",
				"H", "K", "L", "M", "N", "P", "S", "T", "U", "V", "X", "Y", "Z" };

		// nhãn cho ảnh trainning ---------------------------
		int[] labels = new int[height];
		Mat labelsMat = new Mat(height, 1, CvType.CV_32SC1);

		Mat trainningdata = new Mat(height, width, CvType.CV_32FC1);
		double[] trainningmatdata = new double[height * width];

		// Load trainning ---------------------------------
		int vitri = 0;
		int dem = 0;
		Mat image = new Mat();
		for (int i = 0; i < directories.length; i++) {
			File[] files = directories[i].listFiles();
			for (int j = 0; j < files.length; j++) {
				image = Imgcodecs.imread(files[j].getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
				Imgproc.adaptiveThreshold(image, image, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY,
						35, 5);
				for (int k = 0; k < image.size().height; k++) {
					for (int m = 0; m < image.size().width; m++) {
						double[] data = image.get(k, m);
						trainningmatdata[vitri] = data[0];
						vitri++;
					}
				}
				// Lấy giá trị cho nhãn trainning
				labels[dem] = i;
				dem++;
			}
		}
		trainningdata.put(0, 0, trainningmatdata); // gán giá trị cho trainning
		labelsMat.put(0, 0, labels); // gán nhãn trainning

		// Train the SVM -------------------------------------
		SVM svm = SVM.create();
		svm.setType(SVM.C_SVC);
		svm.setKernel(SVM.LINEAR);
		svm.setTermCriteria(new TermCriteria(TermCriteria.MAX_ITER, 100, 1e-6));
		svm.train(trainningdata, Ml.ROW_SAMPLE, labelsMat);
//		svm.save("D:/Bienso/trainning.xml");
//		SVM svm = SVM.load("D:/Bienso/trainning.xml");


//		SVM svm = SVM.create();
//		TermCriteria criteria = new TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 100, 0.1);
//		svm.setKernel(SVM.LINEAR);
//		svm.setType(SVM.C_SVC);
//		svm.setGamma(0.5);
//		svm.setNu(0.5);
//		svm.setC(1);
//		svm.setTermCriteria(criteria);
//		svm.train(trainningdata, Ml.ROW_SAMPLE, labelsMat);

		// dữ liệu test phải là ảnh 22*40 -----------------------------------------
		Mat testtranningdata = new Mat(1, width, CvType.CV_32FC1);
		Mat testdata = Imgcodecs.imread("D:\\Bienso\\63.jpg", Imgcodecs.IMREAD_GRAYSCALE);
		double[] datatestmat = new double[width];
		vitri = 0;
		// ảnh nhị phân
		Imgproc.adaptiveThreshold(testdata, testdata, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY,
				35, 5);
		for (int k = 0; k < testdata.size().height; k++) {
			for (int m = 0; m < testdata.size().width; m++) {
				double[] data = testdata.get(k, m);
				datatestmat[vitri] = data[0];
				vitri++;
			}
		}
		testtranningdata.put(0, 0, datatestmat);
		
		float number = svm.predict(testtranningdata);
		System.out.println("Result = " + number + " , Ky tu: " + danhsachkytu[(int) number]);
		System.exit(0);
	}
}

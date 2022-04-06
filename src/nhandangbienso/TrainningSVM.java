package nhandangbienso;

import java.io.File;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.Ml;
import org.opencv.ml.SVM;

public class TrainningSVM {
	public static void main(String[] args) {
		System.load("C:\\Opencv\\build\\java\\x64\\opencv_java430.dll");
		// Nhận dạng ký tự
		String database = "D:\\Bienso\\Data";
		// tính height cao nhất
		int height = 0;
		File[] directories = new File(database).listFiles();
		for (int i = 0; i < directories.length; i++) {
			File[] files = directories[i].listFiles();
			for (int j = 0; j < files.length; j++) {
				height++;
			}
		}

		// Danh sách ký tự trong biển số xe
//		String[] danhsachkytu = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G",
//				"H", "K", "L", "M", "N", "P", "S", "T", "U", "V", "X", "Y", "Z" };

		// nhãn cho ảnh trainning ---------------------------
		int[] labels = new int[height];
		Mat labelsMat = new Mat(height, 1, CvType.CV_32SC1);

		// ảnh trainning có kích thước 22x40
		Mat trainningdata = new Mat(height, 880, CvType.CV_32FC1);
		double[] trainningmatdata = new double[height * 880];

		// Load trainning ---------------------------------
		int vitri = 0;
		int demvitri = 0;
		Mat image = new Mat();
		for (int i = 0; i < directories.length; i++) {
			File[] files = directories[i].listFiles();
			for (int j = 0; j < files.length; j++) {
				image = Imgcodecs.imread(files[j].getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
				Imgproc.adaptiveThreshold(image, image, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY,
						35, 5);
				Imgproc.resize(image, image, new Size(22, 40));
				for (int k = 0; k < image.size().height; k++) {
					for (int m = 0; m < image.size().width; m++) {
						double[] data = image.get(k, m);
						trainningmatdata[vitri] = data[0];
						vitri++;
					}
				}
				// Lấy giá trị cho nhãn trainning
				labels[demvitri] = i;
				demvitri++;
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
			svm.save("D:/Bienso/SVM/trainningsvm.xml");
//			SVM svm = SVM.load("D:/Bienso/SVM/trainningsvm.xml");
	}
}

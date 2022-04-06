package biensotest;

import java.io.File;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.TermCriteria;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.Ml;
import org.opencv.ml.SVM;

public class TestSVM5 {
	public static void main(String[] args) {
		System.load("C:\\Opencv\\build\\java\\x64\\opencv_java430.dll");

		int[] labels = new int[300];
		for (int i = 0; i < 300; i++) {
			labels[i] = i/10 ;
		}
		Mat labelsMat = new Mat(300, 1, CvType.CV_32SC1);
		labelsMat.put(0, 0, labels);

		int height = 300;
		int width = 880;

		Mat tranningdata = new Mat(height, width, CvType.CV_32FC1);
		double[] trainingData = new double[264000];

		Mat image = new Mat();
		int vitri = 0;

		String DATABASE = "D:/Bienso/Data";
		File[] files = new File(DATABASE).listFiles();
		for (int j = 0; j < files.length; j++) {
			image = Imgcodecs.imread(files[j].getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
			Imgproc.adaptiveThreshold(image, image, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 35,
					5);
			for (int k = 0; k < image.size().height; k++) {
				for (int m = 0; m < image.size().width; m++) {
					double[] data = image.get(k, m);
					trainingData[vitri] = data[0];
					vitri++;
				}
			}
		}
		tranningdata.put(0, 0, trainingData);

		// Train the SVM
		SVM svm = SVM.create();
		svm.setType(SVM.C_SVC);
		svm.setKernel(SVM.LINEAR);
		svm.setTermCriteria(new TermCriteria(TermCriteria.MAX_ITER, 100, 1e-6));

//		SVM svm = SVM.create();
//		TermCriteria criteria = new TermCriteria(TermCriteria.MAX_ITER, 100, 1e-6);
//		svm.setKernel(SVM.LINEAR);
//		svm.setType(SVM.C_SVC);
//		svm.setGamma(0.5);
//		svm.setNu(0.5);
//		svm.setC(1);
//		svm.setTermCriteria(criteria);

		svm.train(tranningdata, Ml.ROW_SAMPLE, labelsMat);

		// du lieu test
		int vitritest = 0;
		Mat testdata2 = new Mat(1, 880, CvType.CV_32FC1);
		double[] datatest = new double[880];
		Mat testdata = Imgcodecs.imread("D:/Bienso/18.jpg", Imgcodecs.IMREAD_GRAYSCALE);
		Imgproc.adaptiveThreshold(testdata, testdata, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY,
				35, 5);
		for (int k = 0; k < testdata.size().height; k++) {
			for (int m = 0; m < testdata.size().width; m++) {
				double[] data = testdata.get(k, m);
				datatest[vitritest] = data[0];
				vitritest++;
			}
		}
		testdata2.put(0, 0, datatest);

		float a = svm.predict(testdata2);
		System.out.println("Result = " + a);
		for (int j = 0; j < 880; j++) {
			System.out.print((int) datatest[j] + " ");
		}
		System.exit(0);
	}
}

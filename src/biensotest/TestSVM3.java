package biensotest;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.ml.Ml;
import org.opencv.ml.SVM;

public class TestSVM3 {
	public static void main(String[] args) {
		System.load("C:\\Opencv\\build\\java\\x64\\opencv_java430.dll");
		Mat labels = new Mat(4,1, CvType.CV_32SC1);
		labels.put(0, 0, 8);
		labels.put(1, 0, 2);
		labels.put(2, 0, 6);
		labels.put(3, 0, 0);

		Mat data = new Mat(new Size(1, 4), CvType.CV_32FC1);
		data.put(0, 0, 5);
		data.put(1, 0, 2);
		data.put(2, 0, 3);
		data.put(3, 0, 9);

		Mat testSamples = new Mat(new Size(1, 1), CvType.CV_32FC1);
		testSamples.put(0, 0, 4);

		SVM svm = SVM.create();
		TermCriteria criteria = new TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 100, 0.1);
		svm.setKernel(SVM.LINEAR);
		svm.setType(SVM.C_SVC);
		svm.setGamma(0.5);
		svm.setNu(0.5);
		svm.setC(2);
		svm.setTermCriteria(criteria);

		// data is N x 64 trained data Mat , labels is N x 1 label Mat with integer
		// values;
		svm.train(data, Ml.ROW_SAMPLE, labels);

		//int predictedClass = (int) svm.predict(testSamples, results, 0);
		int predictedClass = (int) svm.predict(testSamples);
		System.out.println("Ket qua = "+predictedClass);
	}
}

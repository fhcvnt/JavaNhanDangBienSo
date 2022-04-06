package biensotest;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.TermCriteria;
import org.opencv.ml.Ml;
import org.opencv.ml.SVM;

public class TestSVM4 {
	public static void main(String[] args) {
		System.load("C:\\Opencv\\build\\java\\x64\\opencv_java430.dll");
		// Set up training data
		int[] labels = { 1, 2, 3,5, -1 };
		float[] trainingData = { 501, 10, 255, 10, 501, 255, 10, 501, 10, 10 };
		Mat trainingDataMat = new Mat(5, 2, CvType.CV_32FC1);
		trainingDataMat.put(0, 0, trainingData);

		Mat labelsMat = new Mat(5, 1, CvType.CV_32SC1);
		labelsMat.put(0, 0, labels);

		// Train the SVM
		SVM svm = SVM.create();
		svm.setType(SVM.C_SVC);
		svm.setKernel(SVM.LINEAR);
		svm.setTermCriteria(new TermCriteria(TermCriteria.MAX_ITER, 100, 1e-6));
		svm.train(trainingDataMat, Ml.ROW_SAMPLE, labelsMat);

		
		Mat sampleMat = new Mat(1, 2, CvType.CV_32F);
		float[] dd= {10,10};
		sampleMat.put(0, 0, dd);
		float response = svm.predict(sampleMat);
	
		System.out.println("Ket qua = "+response);
		System.exit(0);
	}
}

package biensotest;

import java.io.File;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class TestTesseract {
	public static void main(String[] args) {
		Tesseract tesseract = new Tesseract();
		try {

			tesseract.setDatapath("C:/Tess4J/tessdata");

			// the path of your tess data folder
			// inside the extracted file
			String text = tesseract.doOCR(new File("G:/30a.jpg"));

			// path of your image file
			System.out.print(text);
		} catch (TesseractException e) {
			e.printStackTrace();
		}
	}
}

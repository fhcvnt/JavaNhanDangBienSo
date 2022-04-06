package biensotest;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wb.swt.SWTResourceManager;
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
import org.opencv.photo.Photo;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class CuaSoChinh {

	protected Shell shell;
	private String filename = "";

	public static void main(String[] args) {
		System.load("C:\\Opencv\\build\\java\\x64\\opencv_java430.dll");
		try {
			CuaSoChinh window = new CuaSoChinh();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

	/**
	 * Open the window.
	 */
	public void open() {
		Display display = Display.getDefault();
		createContents();
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shell = new Shell();
		shell.setSize(680, 411);
		shell.setText("SWT Application");
		shell.setLayout(new FillLayout(SWT.HORIZONTAL));

		Composite composite = new Composite(shell, SWT.NONE);

		CLabel lbImagedata = new CLabel(composite, SWT.NONE);
		lbImagedata.setAlignment(SWT.CENTER);
		lbImagedata.setBackground(SWTResourceManager.getColor(SWT.COLOR_GRAY));
		lbImagedata.setBounds(33, 23, 307, 225);
		lbImagedata.setText("Image Data");

		Button btnLoadImage = new Button(composite, SWT.NONE);
		btnLoadImage.setBounds(368, 49, 85, 40);
		btnLoadImage.setText("Load Image");

		CLabel lbBienso = new CLabel(composite, SWT.NONE);
		lbBienso.setBounds(368, 131, 155, 69);
		lbBienso.setText("Bienso");

		Button btnStart = new Button(composite, SWT.NONE);
		btnStart.setBounds(33, 268, 105, 52);
		btnStart.setText("Start");

		Button getBienSo = new Button(composite, SWT.NONE);
		getBienSo.setBounds(185, 268, 117, 52);
		getBienSo.setText("Get Bien So");

		Button btnGet = new Button(composite, SWT.NONE);
		btnGet.setBounds(350, 268, 130, 52);
		btnGet.setText("Get");

		// =============================================================================================================================
		// Load Image
		btnLoadImage.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String[] FILTER_NAMES = { "JPG (*.jpg)", "PNG (*.png)", "All Files (*.*)" };
				// đuôi file có thể mở
				String[] FILTER_EXTS = { "*.jpg", "*.png", "*.*" };

				FileDialog dlg = new FileDialog(shell, SWT.OPEN);
				dlg.setFilterNames(FILTER_NAMES);
				dlg.setFilterExtensions(FILTER_EXTS);
				filename = dlg.open();
				if (filename != null) {
					Path path = Paths.get(filename);
					try {
						Image image = new Image(Display.getDefault(), path.toString());
						lbImagedata.setBackground(image);
						lbImagedata.setText("");
					} catch (Exception ex) {
						System.out.println("Not the picture! - " + filename.toString());
					}
				}
			}
		});

		// =============================================================================================================================
		// Tim bien so
		btnStart.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					Mat src1 = Imgcodecs.imread(filename); // anh goc
					// Khu nhieu anh
					Mat denoisedImg = new Mat();
					Photo.fastNlMeansDenoisingColored(src1, src1, 5, 5, 7, 21);

					
					// ***********************************************
					//Mat src1 = Imgcodecs.imread("G:/BienSo.jpg");
					Mat src2 = src1.clone(); // copy anh
					Mat gray = new Mat(), binary = new Mat();
					Imgproc.cvtColor(src1, gray, Imgproc.COLOR_RGB2GRAY);
					//Imgproc.threshold(gray, binary, 100, 255, Imgproc.THRESH_BINARY);
					//Imgproc.threshold(gray, binary, 90, 255, Imgproc.THRESH_BINARY);
					//Imgproc.threshold(gray, binary, 200, 255, Imgproc.THRESH_BINARY);
					//Imgproc.threshold(gray, binary, 150, 255, Imgproc.THRESH_BINARY);
					Imgproc.adaptiveThreshold(gray, binary, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,Imgproc.THRESH_BINARY, 35, 5);
					
					HighGui.imshow("Anh nhi phan goc", binary);
					Mat morpho = new Mat();
					Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(3, 3), new Point(1, 1));
					Imgproc.erode(binary, morpho, element, new Point(-1, -1), 3);

					//HighGui.imshow("Anh sau khi thuc hien phep gian no", morpho);
					List<MatOfPoint> contours1 = new ArrayList<>();
					Imgproc.findContours(morpho, contours1, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

					for (int i = 0; i < contours1.size(); i++) {
						Rect r = Imgproc.boundingRect(contours1.get(i));
						Imgproc.rectangle(src1, r, new Scalar(255, 0, 0), 1, 8, 0);
						if (r.width / (double) r.height > 3.5f && r.width / (double) r.height < 4.5f) {
							//Imgproc.rectangle(src1, r, new Scalar(0, 0, 255), 2, 8, 0);
						}
						else
							Imgproc.rectangle(src1, r, new Scalar(0, 255, 0), 1, 8, 0);
					}
					HighGui.imshow("Ket qua phat hien truoc phep gian no", src1);
//					List<MatOfPoint> contours2 = new ArrayList<>();
//					Imgproc.findContours(morpho, contours2, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
//					for (int i = 0; i < contours2.size(); i++) {
//						Rect r = Imgproc.boundingRect(contours2.get(i));
//						if (r.width / (double) r.height > 3.5f && r.width / (double) r.height < 4.5f)
//							Imgproc.rectangle(src2, r, new Scalar(0, 0, 255), 2, 8, 0);
//						else
//							Imgproc.rectangle(src2, r, new Scalar(0, 255, 0), 1, 8, 0);
//					}
//					HighGui.imshow("Ket qua phat hien sau khi phep gian no", src2);
									
					HighGui.waitKey(0);
				} catch (Exception ex) {

				}
			}
		});

		// =============================================================================================================================
		// Tim bien so
		getBienSo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Mat im = Imgcodecs.imread(filename); // ảnh gốc
				Mat im_gray = new Mat();
				Imgproc.cvtColor(im, im_gray, Imgproc.COLOR_BGR2GRAY);
				Mat noise_removal = new Mat();
				Imgproc.bilateralFilter(im_gray, noise_removal, 9, 75, 75);

				// Làm m�? ảnh
				// Imgproc.GaussianBlur(im_gray, im_gray, new Size(9, 9), 2);

				Mat equal_histogram = new Mat();
				Imgproc.equalizeHist(noise_removal, equal_histogram);
				Mat kernel = new Mat();
				Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));

				Mat morph_image = new Mat();
				Imgproc.morphologyEx(equal_histogram, morph_image, Imgproc.MORPH_OPEN, kernel);

				// xóa phông (background) không cần thiết
				Mat sub_morp_image = new Mat();
				// Core.subtract(equal_histogram, morph_image, sub_morp_image);
				sub_morp_image = morph_image.clone();

				Mat thresh_image = new Mat(); // anh nhi phan (trang den)
				Imgproc.adaptiveThreshold(sub_morp_image, thresh_image, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
						Imgproc.THRESH_BINARY, 35, 5);
				Mat canny_image = new Mat(); // tim bien anh
				Imgproc.Canny(thresh_image, canny_image, 250, 255);
				kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);
				// kernel = Mat.ones(new Size(3, 3), CvType.CV_8UC1);
				Mat dilated_image = new Mat();
				Imgproc.dilate(canny_image, dilated_image, kernel);

				// ******************************************************************
				List<MatOfPoint> contours = new ArrayList<>();
				Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
				// Imgproc.findContours(dilated_image, contours, new Mat(),
				// Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);

				// vẽ hình chữ nhật quanh biển số
				// Biển số xe mô tô Chi�?u cao 140 mm, chi�?u dài 190 mm
				/*
				 * kiểm tra xem đâu là biển số trong danh sách các đư�?ng biên có được nếu là
				 * biển số thì phải chứa ít nhất 7 khung hình chữ nhật chứa các ký tự
				 */
				int dem = 0;// �?ếm số ký tự trong biển số
				int vitribienso = -1; // vị trí biển số trong contours
				List<Integer> listchar = new ArrayList<>();
				for (int i = 0; i < contours.size(); i++) {
					Rect r = Imgproc.boundingRect(contours.get(i));
					Imgproc.rectangle(im, r, new Scalar(0, 0, 255), 1, 8, 0);
					if (r.width / (double) r.height > 1.0 && r.width / (double) r.height < 1.5) {
						dem = 0;// �?ếm số ký tự trong biển số
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
										// loại b�? những ký tự bị nhân đôi
										for (int k = 0; k < listchar.size(); k++) {
											try {
												Rect rcheckduplicate = Imgproc
														.boundingRect(contours.get(listchar.get(k)));
												if (listchar.get(k) != check) {
													if (((rcheck.x + rcheck.width / 2) > rcheckduplicate.x
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
					// Kiểm tra nếu tìm được biển số thì thoát kh�?i vòng lặp
					if (vitribienso >= 0) {
						break;
					}
				}

				// Cắt lấy biển số
				Mat catbienso = null;
				Rect rbienso = Imgproc.boundingRect(contours.get(vitribienso));
				// Imgproc.rectangle(im, rbienso, new Scalar(0, 0, 255), 2, 8, 0); // cat bien
				// so Rect
				// Rect rectCrop = new Rect(rbienso.x, rbienso.y, rbienso.width,
				// rbienso.height);
				catbienso = new Mat(thresh_image, rbienso);

				// Cắt lấy các ký tự trong biển số
				List<Mat> listcharimage = new ArrayList<Mat>();
				for (int vitri = 0; vitri < listchar.size(); vitri++) {
					Rect rkytu = Imgproc.boundingRect(contours.get(listchar.get(vitri)));
					listcharimage.add(new Mat(thresh_image, rkytu));
				}

				// trắng/đen=0.7 đến 1.4

				HighGui.imshow("Anh goc", im);
				HighGui.imshow("Bien so", catbienso);
				int a = 0;
				for (Mat imagechar : listcharimage) {
					HighGui.imshow("Ky tu - " + a, imagechar);
					a++;
				}
				HighGui.waitKey(0);

			}
		});

		// ================================================================================================================
		// tim bien so
		btnGet.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Mat im = Imgcodecs.imread(filename); // ảnh gốc
				Mat im_gray = new Mat();
				Imgproc.cvtColor(im, im_gray, Imgproc.COLOR_BGR2GRAY);
				Mat noise_removal = new Mat();
				Imgproc.bilateralFilter(im_gray, noise_removal, 9, 75, 75);

				// Làm m�? ảnh
				// Imgproc.GaussianBlur(im_gray, im_gray, new Size(9, 9), 2);

				Mat equal_histogram = new Mat();
				Imgproc.equalizeHist(noise_removal, equal_histogram);
				Mat kernel = new Mat();
				Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));

				Mat morph_image = new Mat();
				Imgproc.morphologyEx(equal_histogram, morph_image, Imgproc.MORPH_OPEN, kernel);

				// xóa phông (background) không cần thiết
				Mat sub_morp_image = new Mat();
				// Core.subtract(equal_histogram, morph_image, sub_morp_image);
				sub_morp_image = morph_image.clone();

				Mat thresh_image = new Mat(); // anh nhi phan (trang den)
				Imgproc.adaptiveThreshold(sub_morp_image, thresh_image, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
						Imgproc.THRESH_BINARY, 35, 5);
				Mat canny_image = new Mat(); // tim bien anh
				Imgproc.Canny(thresh_image, canny_image, 250, 255);
				kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);
				// kernel = Mat.ones(new Size(3, 3), CvType.CV_8UC1);
				Mat dilated_image = new Mat();
				Imgproc.dilate(canny_image, dilated_image, kernel);

				// ******************************************************************
				List<MatOfPoint> contours = new ArrayList<>();
				Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
				// Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_TREE,
				// Imgproc.CHAIN_APPROX_SIMPLE);

				// vẽ hình chữ nhật quanh biển số
				// Biển số xe mô tô Chi�?u cao 140 mm, chi�?u dài 190 mm
				/*
				 * kiểm tra xem đâu là biển số trong danh sách các đư�?ng biên có được nếu là
				 * biển số thì phải chứa ít nhất 7 khung hình chữ nhật chứa các ký tự
				 */
				int dem = 0;// �?ếm số ký tự trong biển số
				int vitribienso = -1; // vị trí biển số trong contours
				List<Integer> listchar = new ArrayList<>();
				for (int i = 0; i < contours.size(); i++) {
					Rect r = Imgproc.boundingRect(contours.get(i));
					if (r.width / (double) r.height > 1.0 && r.width / (double) r.height < 1.5) {
						dem = 0;// �?ếm số ký tự trong biển số
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
										// loại b�? những ký tự bị nhân đôi
										for (int k = 0; k < listchar.size(); k++) {
											try {
												Rect rcheckduplicate = Imgproc
														.boundingRect(contours.get(listchar.get(k)));
												if (listchar.get(k) != check) {
													if (((rcheck.x + rcheck.width / 2) > rcheckduplicate.x
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
					if (dem > 6) {
						vitribienso = i;
					}
					// Kiểm tra nếu tìm được biển số thì thoát kh�?i vòng lặp
					if (vitribienso >= 0) {
						break;
					}
				}

				// Cắt lấy biển số
				Mat catbienso = null;
				Rect rbienso = Imgproc.boundingRect(contours.get(vitribienso));
				// Imgproc.rectangle(im, rbienso, new Scalar(0, 0, 255), 2, 8, 0); // cat bien
				// so Rect
				// Rect rectCrop = new Rect(rbienso.x, rbienso.y, rbienso.width,
				// rbienso.height);
				catbienso = new Mat(thresh_image, rbienso);

				// Cắt lấy các ký tự trong biển số
				List<Mat> listcharimage = new ArrayList<Mat>();
				for (int vitri = 0; vitri < listchar.size(); vitri++) {
					Rect rkytu = Imgproc.boundingRect(contours.get(listchar.get(vitri)));
					listcharimage.add(new Mat(im, rkytu));
				}

				HighGui.imshow("Anh goc", im);
				HighGui.imshow("Bien so", catbienso);
				int a = 0;
				for (Mat imagechar : listcharimage) {
					HighGui.imshow("Ky tu - " + a, imagechar);
					a++;

					Tesseract tesseract = new Tesseract();
					try {
						tesseract.setDatapath("C:/Tess4J/tessdata");
						String text = tesseract.doOCR(Mat2BufferedImage(imagechar));
						System.out.println(text);
					} catch (TesseractException te) {
						te.printStackTrace();
					}
				}
				HighGui.waitKey(0);
			}
		});
	}

	// ****************************************************************************************************************
	// Chuyển đổi Mat => BufferedImage
	public static BufferedImage Mat2BufferedImage(Mat m) {

		int type = BufferedImage.TYPE_BYTE_GRAY;
		if (m.channels() > 1) {
			type = BufferedImage.TYPE_3BYTE_BGR;
		}
		int bufferSize = m.channels() * m.cols() * m.rows();
		byte[] b = new byte[bufferSize];
		m.get(0, 0, b); // get all the pixels
		BufferedImage img = new BufferedImage(m.cols(), m.rows(), type);
		final byte[] targetPixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
		System.arraycopy(b, 0, targetPixels, 0, b.length);
		return img;
	}
}

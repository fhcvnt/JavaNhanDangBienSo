package nhandangbienso;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
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
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.Ml;
import org.opencv.ml.SVM;
import org.eclipse.swt.widgets.Text;

public class BienSoTest {

	protected Shell shell;
	private String filename = "";
	private Text textBienso;
	private int height = 0;
	private int width = 0; // ảnh 22*40
	private String textbienso = ""; // biển số
	private List<Mat> listimagemat = new ArrayList<Mat>(); // danh sách ảnh ký tự trong biển số
	private int countchartop = 0; // số ký tự hàng trên
	private int countcharbottom = 0; // số ký tự hàng dưới

	public static void main(String[] args) {
		System.load("C:\\Opencv\\build\\java\\x64\\opencv_java430.dll");
		try {
			BienSoTest window = new BienSoTest();
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

		Button btnGet = new Button(composite, SWT.NONE);
		btnGet.setBounds(33, 254, 130, 52);
		btnGet.setText("Get");

		textBienso = new Text(composite, SWT.BORDER | SWT.CENTER | SWT.MULTI);
		textBienso.setEditable(false);
		textBienso.setText("Bien so");
		textBienso.setForeground(SWTResourceManager.getColor(255, 255, 255));
		textBienso.setFont(SWTResourceManager.getFont("Times New Roman", 27, SWT.BOLD));
		textBienso.setBackground(SWTResourceManager.getColor(SWT.COLOR_GRAY));
		textBienso.setBounds(356, 124, 187, 87);

		// =============================================================================================================================
		// Nhận dạng ký tự
		// -----------------------------------------------------------------------------------
		String DATABASE = "G:\\Bienso\\Data";
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
		int demvitri = 0;
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
//		svm.save("D:/Bienso/trainning.xml");
//		SVM svm = SVM.load("D:/Bienso/trainning.xml");

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

		// ================================================================================================================
		// tim bien so
		btnGet.addSelectionListener(new SelectionAdapter() {

			private int vitritest;

			@Override
			public void widgetSelected(SelectionEvent e) {
				Mat im = Imgcodecs.imread(filename); // ảnh gốc
				Mat im_gray = new Mat();
				Imgproc.cvtColor(im, im_gray, Imgproc.COLOR_BGR2GRAY);
				Mat noise_removal = new Mat();
				// làm mờ
				Imgproc.bilateralFilter(im_gray, noise_removal, 9, 75, 75);

				// Làm mờ ảnh
				// Imgproc.GaussianBlur(im_gray, im_gray, new Size(9, 9), 2);

				Mat equal_histogram = new Mat();
				// Cân bằng biểu đồ cải thiện độ tương phản của hình ảnh, Cân bằng histogram của
				// một biểu đồ
				Imgproc.equalizeHist(noise_removal, equal_histogram);
				Mat kernel = new Mat();
				// Mô tả sự ăn mòn của hình ảnh: Ăn mòn có thể được hiểu là giảm độ sáng của
				// vùng sáng trong ảnh
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
				Mat dilated_image = new Mat();
				Imgproc.dilate(canny_image, dilated_image, kernel);

				// ***********************************************************************************************************
				List<MatOfPoint> contours = new ArrayList<>();
				Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
				// Imgproc.findContours(dilated_image, contours, new Mat(),
				// Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);

				// vẽ hình chữ nhật quanh biển số
				// Biển số xe mô tô Chiều cao 140 mm, chiều dài 190 mm
				/*
				 * kiểm tra xem đâu là biển số trong danh sách các đường biên có được nếu là
				 * biển số thì phải chứa ít nhất 6 khung hình chữ nhật chứa các ký tự
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
					if (dem > 5) {
						vitribienso = i;
					}
					// Kiểm tra nếu tìm được biển số thì thoát khỏi vòng lặp
					if (vitribienso >= 0) {
						break;
					}
				}

				// ===========================================================================================
				// sắp xếp vị trí ký tự biển số cho đúng với thứ tự thật của biển số
				ArrayList<Integer> charsorttop = new ArrayList<>();
				ArrayList<Integer> charsortbottom = new ArrayList<>();
				int top = 0;
				for (int i = 0; i < listchar.size(); i++) {
					Rect rcheck1 = Imgproc.boundingRect(contours.get(listchar.get(i)));
					for (int j = 0; j < listchar.size(); j++) {
						Rect rcheck2 = Imgproc.boundingRect(contours.get(listchar.get(j)));
						if (rcheck1.y + rcheck1.height / 2 < rcheck2.y + rcheck2.height) {
							top++;
						}
					}
					if (top == listchar.size()) {
						charsorttop.add(listchar.get(i));
					} else {
						charsortbottom.add(listchar.get(i));
					}
					top = 0;
				}
				listchar.clear();
				// Sắp xếp thứ tự
				for (int i = 0; i < charsorttop.size(); i++) {
					Rect rectkytu = Imgproc.boundingRect(contours.get(charsorttop.get(i)));
					for (int j = i; j < charsorttop.size(); j++) {
						Rect rectkytu2 = Imgproc.boundingRect(contours.get(charsorttop.get(j)));
						if (rectkytu.x > rectkytu2.x) {
							int temp = charsorttop.get(i);
							charsorttop.set(i, charsorttop.get(j));
							charsorttop.set(j, temp);
						}
					}
				}
				for (int i = 0; i < charsorttop.size(); i++) {
					listchar.add(charsorttop.get(i));
				}

				for (int i = 0; i < charsortbottom.size(); i++) {
					Rect rectkytu = Imgproc.boundingRect(contours.get(charsortbottom.get(i)));
					for (int j = i; j < charsortbottom.size(); j++) {
						Rect rectkytu2 = Imgproc.boundingRect(contours.get(charsortbottom.get(j)));
						if (rectkytu.x > rectkytu2.x) {
							int temp = charsortbottom.get(i);
							charsortbottom.set(i, charsortbottom.get(j));
							charsortbottom.set(j, temp);
						}
					}
				}
				for (int i = 0; i < charsortbottom.size(); i++) {
					listchar.add(charsortbottom.get(i));
				}

				// Cắt lấy biển số
				// --------------------------------------------------------------------------------------------------
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

				// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
				// tách ký tự từ biển số đã cắt 'catbienso'
				tachKytuBienso(catbienso);
				// HighGui.imshow("Anh goc", im);
				HighGui.imshow("Bien so", catbienso);
				int sokytu = 0;
				for (Mat imagechar : listimagemat) {
					// HighGui.imshow("Ky tu - " + sokytu, imagechar);

					try {
						// dữ liệu test phải là ảnh 22*40 -----------------------------------------
						Mat imagetest = new Mat();
						Imgproc.resize(imagechar, imagetest, new Size(22, 40));
						// lưu ảnh ******************
						Imgcodecs.imwrite("G:/Bienso/Save/img" + sokytu + ".jpg", imagetest);
						// Imgproc.cvtColor(imagetest, imagetest, Imgproc.COLOR_BGR2GRAY);
						Mat testtranningdata = new Mat(1, width, CvType.CV_32FC1);
						double[] datatestmat = new double[width];
						vitritest = 0;
						// ảnh nhị phân
						// Imgproc.adaptiveThreshold(imagetest, imagetest, 255,
						// Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,Imgproc.THRESH_BINARY, 35, 5);
						for (int k = 0; k < imagetest.size().height; k++) {
							for (int m = 0; m < imagetest.size().width; m++) {
								double[] data = imagetest.get(k, m);
								datatestmat[vitritest] = data[0];
								vitritest++;
							}
						}
						testtranningdata.put(0, 0, datatestmat);

						float number = svm.predict(testtranningdata);
						if (sokytu == countchartop) {
							System.out.println();
							textbienso = textbienso + "\n";
						}

						if (countchartop > 2) {
							if (sokytu == 2) {
								// System.out.print("-");
								textbienso = textbienso + "-"; // chèn thêm ký tự "-"
							}
						}
						if (countcharbottom > 4) {
							if (sokytu == charsorttop.size() + 3) {
								// System.out.print(".");
								textbienso = textbienso + "."; // chèn thêm ký tự "-"
							}
						}
						// System.out.print(danhsachkytu[(int) number]);
						textbienso = textbienso + danhsachkytu[(int) number];
					} catch (Exception exc) {
						exc.printStackTrace();
					}
					sokytu++;
				}
				textBienso.setText(textbienso);
				textBienso.update();
				HighGui.waitKey(0);
			}
		});
	}

	// ******************************************************************************************************************************
	// Tách ký tự từ biển số đã cắt
	public void tachKytuBienso(Mat bienso) {
		// bienso phải là ảnh nhị phân
		Mat canny_image = new Mat(); // tim bien anh
		Imgproc.Canny(bienso, canny_image, 250, 255);
		Mat kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);
		Mat dilated_image = new Mat();
		Imgproc.dilate(canny_image, dilated_image, kernel);

		List<MatOfPoint> contours = new ArrayList<>();
		Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

		// Danh sách ký tự trong biển số
		List<Integer> listchar = new ArrayList<>();
		// Danh sách vị trí ký tự trong contours
		listchar.clear(); // làm rỗng danh sách
		boolean duplicate = false; // kiểm tra ký tự bị nhân đôi

		for (int check = 0; check < contours.size(); check++) {
			Rect rcheck = Imgproc.boundingRect(contours.get(check));
			// chểu ngang ký tự không được lớn hơn chiều cao của ký tự
			if (!(rcheck.width >= rcheck.height && rcheck.width >= rcheck.height)) {
				// Tìm ký tự nằm trong biển số
				if (rcheck.width < rcheck.height && (rcheck.height > (float) bienso.height() / 3
						&& rcheck.height < (float) bienso.height() / 2)) {
					duplicate = false;
					// loại bỏ những ký tự bị nhân đôi
					for (int k = 0; k < listchar.size(); k++) {
						try {
							Rect rcheckduplicate = Imgproc.boundingRect(contours.get(listchar.get(k)));
							if (listchar.get(k) != check) {
								if (((rcheck.x + rcheck.width / 2) > rcheckduplicate.x
										&& (rcheck.x + rcheck.width / 2) < (rcheckduplicate.x + rcheckduplicate.width)
										&& (rcheck.y + rcheck.height / 2) > rcheckduplicate.y && (rcheck.y
												+ rcheck.height / 2) < (rcheckduplicate.y + rcheckduplicate.height))) {
									duplicate = true;
								}
							}
						} catch (Exception exc) {
						}
					}
					if (!duplicate) {
						listchar.add(check);
						Imgproc.rectangle(bienso, rcheck, new Scalar(255, 0, 0), 1, 8, 0);
					}
				}
			}
		}
		// xóa đi ký tự bị sai, dư ra, ký tự nào có vị trí thấp hơn thì bị sai
		int heighty = -1;
		int vitriy = -1;
		for (int i = 0; i < listchar.size(); i++) {
			Rect rcheckngoaile = Imgproc.boundingRect(contours.get(listchar.get(i)));
			if (rcheckngoaile.y + rcheckngoaile.height > heighty) {
				heighty = rcheckngoaile.y + rcheckngoaile.height;
				vitriy = i;
			}
		}
		listchar.remove(vitriy);

		// ===========================================================================================
		// sắp xếp vị trí ký tự biển số cho đúng với thứ tự thật của biển số
		ArrayList<Integer> charsorttop = new ArrayList<>();
		ArrayList<Integer> charsortbottom = new ArrayList<>();
		int top = 0;
		for (int i = 0; i < listchar.size(); i++) {
			Rect rcheck1 = Imgproc.boundingRect(contours.get(listchar.get(i)));
			for (int j = 0; j < listchar.size(); j++) {
				Rect rcheck2 = Imgproc.boundingRect(contours.get(listchar.get(j)));
				if (rcheck1.y + rcheck1.height / 2 < rcheck2.y + rcheck2.height) {
					top++;
				}
			}
			if (top == listchar.size()) {
				charsorttop.add(listchar.get(i));
			} else {
				charsortbottom.add(listchar.get(i));
			}
			top = 0;
		}
		listchar.clear();
		// Sắp xếp thứ tự
		for (int i = 0; i < charsorttop.size(); i++) {
			Rect rectkytu = Imgproc.boundingRect(contours.get(charsorttop.get(i)));
			for (int j = i; j < charsorttop.size(); j++) {
				Rect rectkytu2 = Imgproc.boundingRect(contours.get(charsorttop.get(j)));
				if (rectkytu.x > rectkytu2.x) {
					int temp = charsorttop.get(i);
					charsorttop.set(i, charsorttop.get(j));
					charsorttop.set(j, temp);
				}
			}
		}
		for (int i = 0; i < charsorttop.size(); i++) {
			listchar.add(charsorttop.get(i));
		}

		for (int i = 0; i < charsortbottom.size(); i++) {
			Rect rectkytu = Imgproc.boundingRect(contours.get(charsortbottom.get(i)));
			for (int j = i; j < charsortbottom.size(); j++) {
				Rect rectkytu2 = Imgproc.boundingRect(contours.get(charsortbottom.get(j)));
				if (rectkytu.x > rectkytu2.x) {
					int temp = charsortbottom.get(i);
					charsortbottom.set(i, charsortbottom.get(j));
					charsortbottom.set(j, temp);
				}
			}
		}
		for (int i = 0; i < charsortbottom.size(); i++) {
			listchar.add(charsortbottom.get(i));
		}
		countchartop = charsorttop.size();
		countcharbottom = charsortbottom.size();
		listimagemat.clear();
		// Cắt lấy các ký tự trong biển số
		for (int vitri = 0; vitri < listchar.size(); vitri++) {
			Rect rkytu = Imgproc.boundingRect(contours.get(listchar.get(vitri)));
			listimagemat.add(new Mat(bienso, rkytu));
		}
	}

	// ******************************************************************************************************************************
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

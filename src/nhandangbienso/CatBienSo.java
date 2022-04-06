package nhandangbienso;

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
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class CatBienSo {

	protected Shell shellBienso;
	private String filename = "";
	private Text textFilename;

	private Mat im = new Mat(); // ảnh gốc
	private Mat image = new Mat(); // image=im.clone();
	private Mat im_gray = new Mat();
	private Mat noise_removal = new Mat();
	private Mat equal_histogram = new Mat();
	private Mat kernel = new Mat();
	private Mat morph_image = new Mat();
	private Mat thresh_image = new Mat(); // anh nhi phan (trang den)
	private Mat canny_image = new Mat();
	private Mat dilated_image = new Mat();

	private List<MatOfPoint> contours = new ArrayList<>();
	private List<Integer> listchar = new ArrayList<>();
	private int dem = 0;// Đếm số ký tự trong biển số
	private int vitribienso = -1; // vị trí biển số trong contours

	private boolean next = false; // nếu như chưa tìm được biển số thì thực hiện bước kế tìm tiếp

	// Cắt lấy biển số
	private Mat catbienso = new Mat();
	private Rect rbienso;

	private int counttitle = 0;

	public static void main(String[] args) {
		System.load("C:\\Opencv\\build\\java\\x64\\opencv_java430.dll");
		try {
			CatBienSo window = new CatBienSo();
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
		shellBienso.open();
		shellBienso.layout();
		while (!shellBienso.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shellBienso = new Shell();
		shellBienso.setSize(643, 606);
		shellBienso.setText("Nhận dạng biển số");
		shellBienso.setLayout(new FillLayout(SWT.HORIZONTAL));

		Composite composite = new Composite(shellBienso, SWT.NONE);

		CLabel lbImagedata = new CLabel(composite, SWT.NONE);
		lbImagedata.setAlignment(SWT.CENTER);
		lbImagedata.setBackground(SWTResourceManager.getColor(SWT.COLOR_GRAY));
		lbImagedata.setBounds(10, 124, 602, 433);
		lbImagedata.setText("Image Data");

		Button btnLoadImage = new Button(composite, SWT.NONE);
		btnLoadImage.setBounds(368, 49, 85, 30);
		btnLoadImage.setText("Load Image");

		Button btnGetlicenseplate = new Button(composite, SWT.NONE);
		btnGetlicenseplate.setBounds(472, 49, 130, 30);
		btnGetlicenseplate.setText("Get license plate");

		textFilename = new Text(composite, SWT.BORDER);
		textFilename.setBounds(10, 47, 350, 30);

		// =============================================================================================================================
		// Load Image
		btnLoadImage.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String[] FILTER_NAMES = { "JPG (*.jpg)", "PNG (*.png)", "All Files (*.*)" };
				// đuôi file có thể mở
				String[] FILTER_EXTS = { "*.jpg", "*.png", "*.*" };

				FileDialog dlg = new FileDialog(shellBienso, SWT.OPEN);
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
				try {
					textFilename.setText(filename);
				} catch (Exception ex) {
					textFilename.setText("");
				}
			}
		});

		// ================================================================================================================
		// tim bien so
		btnGetlicenseplate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					im = new Mat();
					im = Imgcodecs.imread(filename); // ảnh gốc
					// resize ảnh nếu chiều ngang ảnh nhỏ hơn 200 (resize 1.5 lần)
					if (im.width() < 300) {
						// Mat resizeimage = new Mat();
						Imgproc.resize(im, im, new Size(im.width() * 1.5, im.height() * 1.5));
					}

					image = im.clone();
					Imgproc.cvtColor(im, im_gray, Imgproc.COLOR_BGR2GRAY);
					// Remove noise giảm noise và tăng edge(làm egde thêm sắc nhọn edges sharp)
					Imgproc.bilateralFilter(im_gray, noise_removal, 5, 75, 75);

					// Cân bằng lại histogram làm cho ảnh không quá sáng hoặc tối
					Imgproc.equalizeHist(noise_removal, equal_histogram);

					// Morphogoly open mục đích là giảm egde nhiễu , egde thật thêm sắc nhọn bằng
					// cv2.morphologyEx sử dụng kerel 5x5
					Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
					Imgproc.morphologyEx(equal_histogram, morph_image, Imgproc.MORPH_OPEN, kernel);

					// anh nhi phan (trang den)
					Imgproc.adaptiveThreshold(morph_image, thresh_image, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
							Imgproc.THRESH_BINARY, 35, 5);
					// tim bien anh
					Imgproc.Canny(thresh_image, canny_image, 250, 255);
					kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

					// dilate để tăng sharp cho egde
					Imgproc.dilate(canny_image, dilated_image, kernel);

					contours.clear();
					Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST,
							Imgproc.CHAIN_APPROX_NONE);

					// vẽ hình chữ nhật quanh biển số
					// Biển số xe mô tô Chiều cao 140 mm, chiều dài 190 mm
					/*
					 * kiểm tra xem đâu là biển số trong danh sách các đường biên có được nếu là
					 * biển số thì phải chứa ít nhất 7 khung hình chữ nhật chứa các ký tự
					 */
					dem = 0;// Đếm số ký tự trong biển số
					vitribienso = -1; // vị trí biển số trong contours
					listchar.clear();
					for (int i = 0; i < contours.size(); i++) {
						Rect r = Imgproc.boundingRect(contours.get(i));
						// vẽ hình chữ nhật quanh đối tượng contours
						Imgproc.rectangle(im, r, new Scalar(0, 255, 0), 1, 8, 0);
						if (r.width / (double) r.height > 1.0 && r.width / (double) r.height < 2.2) {
							dem = 0;// Đếm số ký tự trong biển số
							// Danh sách vị trí ký tự trong contours
							listchar.clear(); // làm rỗng danh sách
							boolean duplicate = false; // kiểm tra ký tự bị nhân đôi
							for (int check = 0; check < contours.size(); check++) {
								if (check != i) {
									Rect rcheck = Imgproc.boundingRect(contours.get(check));
									if (!(rcheck.width / (double) rcheck.height > 1.0
											&& rcheck.width / (double) rcheck.height < 2.2)) {
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

					// HighGui.imshow("Anh goc", im);
					try {
						// Cắt lấy biển số
						catbienso = null;
						rbienso = Imgproc.boundingRect(contours.get(vitribienso));
						// Imgproc.rectangle(im, rbienso, new Scalar(0, 0, 255), 2, 8, 0); // cat bien
						// so Rect
						// Rect rectCrop = new Rect(rbienso.x, rbienso.y, rbienso.width,
						// rbienso.height);
						catbienso = new Mat(image, rbienso);

						next = false;
					} catch (Exception ae) {
						next = true;
						// ae.printStackTrace();
					}
					if (next) {
						// 100
						// --------------------------------------------------------------------------------------------------------
						Imgproc.threshold(morph_image, thresh_image, 100, 255, Imgproc.THRESH_BINARY);
						Imgproc.Canny(thresh_image, canny_image, 250, 255);
						kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

						// dilate để tăng sharp cho egde
						Imgproc.dilate(canny_image, dilated_image, kernel);

						contours.clear();
						Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST,
								Imgproc.CHAIN_APPROX_NONE);

						// vẽ hình chữ nhật quanh biển số
						// Biển số xe mô tô Chiều cao 140 mm, chiều dài 190 mm
						/*
						 * kiểm tra xem đâu là biển số trong danh sách các đường biên có được nếu là
						 * biển số thì phải chứa ít nhất 7 khung hình chữ nhật chứa các ký tự
						 */
						dem = 0;// Đếm số ký tự trong biển số
						vitribienso = -1; // vị trí biển số trong contours
						listchar.clear();
						for (int i = 0; i < contours.size(); i++) {
							Rect r = Imgproc.boundingRect(contours.get(i));
							// vẽ hình chữ nhật quanh đối tượng contours
							Imgproc.rectangle(im, r, new Scalar(0, 255, 0), 1, 8, 0);
							if (r.width / (double) r.height > 1.0 && r.width / (double) r.height < 2.2) {
								dem = 0;// Đếm số ký tự trong biển số
								// Danh sách vị trí ký tự trong contours
								listchar.clear(); // làm rỗng danh sách
								boolean duplicate = false; // kiểm tra ký tự bị nhân đôi
								for (int check = 0; check < contours.size(); check++) {
									if (check != i) {
										Rect rcheck = Imgproc.boundingRect(contours.get(check));
										if (!(rcheck.width / (double) rcheck.height > 1.0
												&& rcheck.width / (double) rcheck.height < 2.2)) {
											// Tìm ký tự nằm trong biển số
											if ((rcheck.x > r.x && rcheck.x < r.x + r.width)
													&& rcheck.width < rcheck.height
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
															if (((rcheck.x + rcheck.width / 2 > rcheckduplicate.x)
																	&& (rcheck.x
																			+ rcheck.width / 2) < (rcheckduplicate.x
																					+ rcheckduplicate.width)
																	&& (rcheck.y
																			+ rcheck.height / 2) > rcheckduplicate.y
																	&& (rcheck.y
																			+ rcheck.height / 2) < (rcheckduplicate.y
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

						try {
							// Cắt lấy biển số
							catbienso = null;
							rbienso = Imgproc.boundingRect(contours.get(vitribienso));
							// Imgproc.rectangle(im, rbienso, new Scalar(0, 0, 255), 2, 8, 0); // cat bien
							// so Rect
							// Rect rectCrop = new Rect(rbienso.x, rbienso.y, rbienso.width,
							// rbienso.height);
							catbienso = new Mat(image, rbienso);
							next = false;
						} catch (Exception ae100) {
							next = true;
							// ae100.printStackTrace();
						}
					}

					if (next) {
						// 150
						// --------------------------------------------------------------------------------------------------------
						Imgproc.threshold(morph_image, thresh_image, 150, 255, Imgproc.THRESH_BINARY);
						Imgproc.Canny(thresh_image, canny_image, 250, 255);
						kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

						// dilate để tăng sharp cho egde
						Imgproc.dilate(canny_image, dilated_image, kernel);

						contours.clear();
						Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST,
								Imgproc.CHAIN_APPROX_NONE);

						// vẽ hình chữ nhật quanh biển số
						// Biển số xe mô tô Chiều cao 140 mm, chiều dài 190 mm
						/*
						 * kiểm tra xem đâu là biển số trong danh sách các đường biên có được nếu là
						 * biển số thì phải chứa ít nhất 7 khung hình chữ nhật chứa các ký tự
						 */
						dem = 0;// Đếm số ký tự trong biển số
						vitribienso = -1; // vị trí biển số trong contours
						listchar.clear();
						for (int i = 0; i < contours.size(); i++) {
							Rect r = Imgproc.boundingRect(contours.get(i));
							// vẽ hình chữ nhật quanh đối tượng contours
							Imgproc.rectangle(im, r, new Scalar(0, 255, 0), 1, 8, 0);
							if (r.width / (double) r.height > 1.0 && r.width / (double) r.height < 2.2) {
								dem = 0;// Đếm số ký tự trong biển số
								// Danh sách vị trí ký tự trong contours
								listchar.clear(); // làm rỗng danh sách
								boolean duplicate = false; // kiểm tra ký tự bị nhân đôi
								for (int check = 0; check < contours.size(); check++) {
									if (check != i) {
										Rect rcheck = Imgproc.boundingRect(contours.get(check));
										if (!(rcheck.width / (double) rcheck.height > 1.0
												&& rcheck.width / (double) rcheck.height < 2.2)) {
											// Tìm ký tự nằm trong biển số
											if ((rcheck.x > r.x && rcheck.x < r.x + r.width)
													&& rcheck.width < rcheck.height
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
															if (((rcheck.x + rcheck.width / 2 > rcheckduplicate.x)
																	&& (rcheck.x
																			+ rcheck.width / 2) < (rcheckduplicate.x
																					+ rcheckduplicate.width)
																	&& (rcheck.y
																			+ rcheck.height / 2) > rcheckduplicate.y
																	&& (rcheck.y
																			+ rcheck.height / 2) < (rcheckduplicate.y
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

						try {
							// Cắt lấy biển số
							catbienso = null;
							rbienso = Imgproc.boundingRect(contours.get(vitribienso));
							// Imgproc.rectangle(im, rbienso, new Scalar(0, 0, 255), 2, 8, 0); // cat bien
							// so Rect
							// Rect rectCrop = new Rect(rbienso.x, rbienso.y, rbienso.width,
							// rbienso.height);
							catbienso = new Mat(image, rbienso);

							next = false;
						} catch (Exception ae150) {
							next = true;
							// ae150.printStackTrace();
						}
					}

					if (next) {
						// 200
						// --------------------------------------------------------------------------------------------------------
						Imgproc.threshold(morph_image, thresh_image, 200, 255, Imgproc.THRESH_BINARY);
						Imgproc.Canny(thresh_image, canny_image, 250, 255);
						kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

						// dilate để tăng sharp cho egde
						Imgproc.dilate(canny_image, dilated_image, kernel);

						contours.clear();
						Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST,
								Imgproc.CHAIN_APPROX_NONE);

						// vẽ hình chữ nhật quanh biển số
						// Biển số xe mô tô Chiều cao 140 mm, chiều dài 190 mm
						/*
						 * kiểm tra xem đâu là biển số trong danh sách các đường biên có được nếu là
						 * biển số thì phải chứa ít nhất 7 khung hình chữ nhật chứa các ký tự
						 */
						dem = 0;// Đếm số ký tự trong biển số
						vitribienso = -1; // vị trí biển số trong contours
						listchar.clear();
						for (int i = 0; i < contours.size(); i++) {
							Rect r = Imgproc.boundingRect(contours.get(i));
							// vẽ hình chữ nhật quanh đối tượng contours
							Imgproc.rectangle(im, r, new Scalar(0, 255, 0), 1, 8, 0);
							if (r.width / (double) r.height > 1.0 && r.width / (double) r.height < 2.2) {
								dem = 0;// Đếm số ký tự trong biển số
								// Danh sách vị trí ký tự trong contours
								listchar.clear(); // làm rỗng danh sách
								boolean duplicate = false; // kiểm tra ký tự bị nhân đôi
								for (int check = 0; check < contours.size(); check++) {
									if (check != i) {
										Rect rcheck = Imgproc.boundingRect(contours.get(check));
										if (!(rcheck.width / (double) rcheck.height > 1.0
												&& rcheck.width / (double) rcheck.height < 2.2)) {
											// Tìm ký tự nằm trong biển số
											if ((rcheck.x > r.x && rcheck.x < r.x + r.width)
													&& rcheck.width < rcheck.height
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
															if (((rcheck.x + rcheck.width / 2 > rcheckduplicate.x)
																	&& (rcheck.x
																			+ rcheck.width / 2) < (rcheckduplicate.x
																					+ rcheckduplicate.width)
																	&& (rcheck.y
																			+ rcheck.height / 2) > rcheckduplicate.y
																	&& (rcheck.y
																			+ rcheck.height / 2) < (rcheckduplicate.y
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

						try {
							// Cắt lấy biển số
							catbienso = null;
							rbienso = Imgproc.boundingRect(contours.get(vitribienso));
							// Imgproc.rectangle(im, rbienso, new Scalar(0, 0, 255), 2, 8, 0); // cat bien
							// so Rect
							// Rect rectCrop = new Rect(rbienso.x, rbienso.y, rbienso.width,
							// rbienso.height);
							catbienso = new Mat(image, rbienso);

							next = false;
						} catch (Exception ae200) {
							next = true;
							// ae200.printStackTrace();
						}
					}

					if (next) {
						// 210
						// --------------------------------------------------------------------------------------------------------
						Imgproc.threshold(morph_image, thresh_image, 210, 255, Imgproc.THRESH_BINARY);
						Imgproc.Canny(thresh_image, canny_image, 250, 255);
						kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

						// dilate để tăng sharp cho egde
						Imgproc.dilate(canny_image, dilated_image, kernel);

						contours.clear();
						Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST,
								Imgproc.CHAIN_APPROX_NONE);

						// vẽ hình chữ nhật quanh biển số
						// Biển số xe mô tô Chiều cao 140 mm, chiều dài 190 mm
						/*
						 * kiểm tra xem đâu là biển số trong danh sách các đường biên có được nếu là
						 * biển số thì phải chứa ít nhất 7 khung hình chữ nhật chứa các ký tự
						 */
						dem = 0;// Đếm số ký tự trong biển số
						vitribienso = -1; // vị trí biển số trong contours
						listchar.clear();
						for (int i = 0; i < contours.size(); i++) {
							Rect r = Imgproc.boundingRect(contours.get(i));
							// vẽ hình chữ nhật quanh đối tượng contours
							Imgproc.rectangle(im, r, new Scalar(0, 255, 0), 1, 8, 0);
							if (r.width / (double) r.height > 1.0 && r.width / (double) r.height < 2.2) {
								dem = 0;// Đếm số ký tự trong biển số
								// Danh sách vị trí ký tự trong contours
								listchar.clear(); // làm rỗng danh sách
								boolean duplicate = false; // kiểm tra ký tự bị nhân đôi
								for (int check = 0; check < contours.size(); check++) {
									if (check != i) {
										Rect rcheck = Imgproc.boundingRect(contours.get(check));
										if (!(rcheck.width / (double) rcheck.height > 1.0
												&& rcheck.width / (double) rcheck.height < 2.2)) {
											// Tìm ký tự nằm trong biển số
											if ((rcheck.x > r.x && rcheck.x < r.x + r.width)
													&& rcheck.width < rcheck.height
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
															if (((rcheck.x + rcheck.width / 2 > rcheckduplicate.x)
																	&& (rcheck.x
																			+ rcheck.width / 2) < (rcheckduplicate.x
																					+ rcheckduplicate.width)
																	&& (rcheck.y
																			+ rcheck.height / 2) > rcheckduplicate.y
																	&& (rcheck.y
																			+ rcheck.height / 2) < (rcheckduplicate.y
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
						try {
							// Cắt lấy biển số
							rbienso = Imgproc.boundingRect(contours.get(vitribienso));
							// Imgproc.rectangle(im, rbienso, new Scalar(0, 0, 255), 2, 8, 0); // cat bien
							// so Rect
							// Rect rectCrop = new Rect(rbienso.x, rbienso.y, rbienso.width,
							// rbienso.height);
							catbienso = new Mat(image, rbienso);

							next = false;
						} catch (Exception ae210) {
							next = true;
							// ae210.printStackTrace();
						}
					}

					if (next) {
						// 230
						// --------------------------------------------------------------------------------------------------------
						Imgproc.threshold(morph_image, thresh_image, 230, 255, Imgproc.THRESH_BINARY);
						Imgproc.Canny(thresh_image, canny_image, 250, 255);
						kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

						// dilate để tăng sharp cho egde
						Imgproc.dilate(canny_image, dilated_image, kernel);

						contours.clear();
						Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST,
								Imgproc.CHAIN_APPROX_NONE);

						// vẽ hình chữ nhật quanh biển số
						// Biển số xe mô tô Chiều cao 140 mm, chiều dài 190 mm
						/*
						 * kiểm tra xem đâu là biển số trong danh sách các đường biên có được nếu là
						 * biển số thì phải chứa ít nhất 7 khung hình chữ nhật chứa các ký tự
						 */
						dem = 0;// Đếm số ký tự trong biển số
						vitribienso = -1; // vị trí biển số trong contours
						listchar.clear();
						for (int i = 0; i < contours.size(); i++) {
							Rect r = Imgproc.boundingRect(contours.get(i));
							// vẽ hình chữ nhật quanh đối tượng contours
							Imgproc.rectangle(im, r, new Scalar(0, 255, 0), 1, 8, 0);
							if (r.width / (double) r.height > 1.0 && r.width / (double) r.height < 2.2) {
								dem = 0;// Đếm số ký tự trong biển số
								// Danh sách vị trí ký tự trong contours
								listchar.clear(); // làm rỗng danh sách
								boolean duplicate = false; // kiểm tra ký tự bị nhân đôi
								for (int check = 0; check < contours.size(); check++) {
									if (check != i) {
										Rect rcheck = Imgproc.boundingRect(contours.get(check));
										if (!(rcheck.width / (double) rcheck.height > 1.0
												&& rcheck.width / (double) rcheck.height < 2.2)) {
											// Tìm ký tự nằm trong biển số
											if ((rcheck.x > r.x && rcheck.x < r.x + r.width)
													&& rcheck.width < rcheck.height
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
															if (((rcheck.x + rcheck.width / 2 > rcheckduplicate.x)
																	&& (rcheck.x
																			+ rcheck.width / 2) < (rcheckduplicate.x
																					+ rcheckduplicate.width)
																	&& (rcheck.y
																			+ rcheck.height / 2) > rcheckduplicate.y
																	&& (rcheck.y
																			+ rcheck.height / 2) < (rcheckduplicate.y
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
						try {
							// Cắt lấy biển số
							catbienso = null;
							rbienso = Imgproc.boundingRect(contours.get(vitribienso));
							// Imgproc.rectangle(im, rbienso, new Scalar(0, 0, 255), 2, 8, 0); // cat bien
							// so Rect
							// Rect rectCrop = new Rect(rbienso.x, rbienso.y, rbienso.width,
							// rbienso.height);
							catbienso = new Mat(image, rbienso);

							next = false;
						} catch (Exception ae230) {
							next = true;
							ae230.printStackTrace();
						}
					}

					if (next) {
						// 250
						// --------------------------------------------------------------------------------------------------------
						Imgproc.threshold(morph_image, thresh_image, 250, 255, Imgproc.THRESH_BINARY);
						Imgproc.Canny(thresh_image, canny_image, 250, 255);
						kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

						// dilate để tăng sharp cho egde
						Imgproc.dilate(canny_image, dilated_image, kernel);

						contours.clear();
						Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST,
								Imgproc.CHAIN_APPROX_NONE);

						// vẽ hình chữ nhật quanh biển số
						// Biển số xe mô tô Chiều cao 140 mm, chiều dài 190 mm
						/*
						 * kiểm tra xem đâu là biển số trong danh sách các đường biên có được nếu là
						 * biển số thì phải chứa ít nhất 7 khung hình chữ nhật chứa các ký tự
						 */
						dem = 0;// Đếm số ký tự trong biển số
						vitribienso = -1; // vị trí biển số trong contours
						listchar.clear();
						for (int i = 0; i < contours.size(); i++) {
							Rect r = Imgproc.boundingRect(contours.get(i));
							// vẽ hình chữ nhật quanh đối tượng contours
							Imgproc.rectangle(im, r, new Scalar(0, 255, 0), 1, 8, 0);
							if (r.width / (double) r.height > 1.0 && r.width / (double) r.height < 2.2) {
								dem = 0;// Đếm số ký tự trong biển số
								// Danh sách vị trí ký tự trong contours
								listchar.clear(); // làm rỗng danh sách
								boolean duplicate = false; // kiểm tra ký tự bị nhân đôi
								for (int check = 0; check < contours.size(); check++) {
									if (check != i) {
										Rect rcheck = Imgproc.boundingRect(contours.get(check));
										if (!(rcheck.width / (double) rcheck.height > 1.0
												&& rcheck.width / (double) rcheck.height < 2.2)) {
											// Tìm ký tự nằm trong biển số
											if ((rcheck.x > r.x && rcheck.x < r.x + r.width)
													&& rcheck.width < rcheck.height
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
															if (((rcheck.x + rcheck.width / 2 > rcheckduplicate.x)
																	&& (rcheck.x
																			+ rcheck.width / 2) < (rcheckduplicate.x
																					+ rcheckduplicate.width)
																	&& (rcheck.y
																			+ rcheck.height / 2) > rcheckduplicate.y
																	&& (rcheck.y
																			+ rcheck.height / 2) < (rcheckduplicate.y
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
						try {
							// Cắt lấy biển số
							catbienso = null;
							rbienso = Imgproc.boundingRect(contours.get(vitribienso));
							// Imgproc.rectangle(im, rbienso, new Scalar(0, 0, 255), 2, 8, 0); // cat
							// bien
							// so Rect
							// Rect rectCrop = new Rect(rbienso.x, rbienso.y, rbienso.width,
							// rbienso.height);
							catbienso = new Mat(image, rbienso);
							next = false;
						} catch (Exception ae250) {
							next = true;
							// ae250.printStackTrace();
						}
					}

					if (next) {
						catbienso = null;
					}
					if (!(catbienso == null)) {
						// chú ý: không thể hiển thị nhiều ảnh cùng tiêu đề nếu không sẽ bị lỗi
						HighGui.imshow("Bien so " + counttitle, catbienso);
						counttitle++;
					}
				} catch (Exception except) {
					except.printStackTrace();
				}
				HighGui.waitKey(0);
			}
		});
	}

	// ======================================================================================================================================
	// trả về biển số là kiểu Mat, imagemat là ảnh chứa biển số
	public Mat getImageBienso(Mat imagemat) {
		try {
			im = imagemat.clone();
			image=im.clone();
			// resize ảnh nếu chiều ngang ảnh nhỏ hơn 200 (resize 1.5 lần)
			if (im.width() < 300) {
				// Mat resizeimage = new Mat();
				Imgproc.resize(im, im, new Size(im.width() * 1.5, im.height() * 1.5));
			}
			Imgproc.cvtColor(im, im_gray, Imgproc.COLOR_BGR2GRAY);
			// Remove noise giảm noise và tăng edge(làm egde thêm sắc nhọn edges sharp)
			Imgproc.bilateralFilter(im_gray, noise_removal, 5, 75, 75);

			// Cân bằng lại histogram làm cho ảnh không quá sáng hoặc tối
			Imgproc.equalizeHist(noise_removal, equal_histogram);

			// Morphogoly open mục đích là giảm egde nhiễu , egde thật thêm sắc nhọn bằng
			// cv2.morphologyEx sử dụng kerel 5x5
			Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
			Imgproc.morphologyEx(equal_histogram, morph_image, Imgproc.MORPH_OPEN, kernel);

			// anh nhi phan (trang den)
			Imgproc.adaptiveThreshold(morph_image, thresh_image, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
					Imgproc.THRESH_BINARY, 35, 5);
			// tim bien anh
			Imgproc.Canny(thresh_image, canny_image, 250, 255);
			kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

			// dilate để tăng sharp cho egde
			Imgproc.dilate(canny_image, dilated_image, kernel);

			contours.clear();
			Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

			// vẽ hình chữ nhật quanh biển số
			// Biển số xe mô tô Chiều cao 140 mm, chiều dài 190 mm
			/*
			 * kiểm tra xem đâu là biển số trong danh sách các đường biên có được nếu là
			 * biển số thì phải chứa ít nhất 7 khung hình chữ nhật chứa các ký tự
			 */
			dem = 0;// Đếm số ký tự trong biển số
			vitribienso = -1; // vị trí biển số trong contours
			listchar.clear();
			for (int i = 0; i < contours.size(); i++) {
				Rect r = Imgproc.boundingRect(contours.get(i));
				if (r.width / (double) r.height > 1.0 && r.width / (double) r.height < 2.2) {
					dem = 0;// Đếm số ký tự trong biển số
					// Danh sách vị trí ký tự trong contours
					listchar.clear(); // làm rỗng danh sách
					boolean duplicate = false; // kiểm tra ký tự bị nhân đôi
					for (int check = 0; check < contours.size(); check++) {
						if (check != i) {
							Rect rcheck = Imgproc.boundingRect(contours.get(check));
							if (!(rcheck.width / (double) rcheck.height > 1.0
									&& rcheck.width / (double) rcheck.height < 2.2)) {
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

			try {
				// Cắt lấy biển số
				catbienso = null;
				rbienso = Imgproc.boundingRect(contours.get(vitribienso));
				catbienso = new Mat(image, rbienso);
				if (!(catbienso == null)) {
					return catbienso;
				}
				next = false;
			} catch (Exception ae) {
				next = true;
			}
			if (next) {
				// 100
				// --------------------------------------------------------------------------------------------------------
				Imgproc.threshold(morph_image, thresh_image, 100, 255, Imgproc.THRESH_BINARY);
				Imgproc.Canny(thresh_image, canny_image, 250, 255);
				kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

				// dilate để tăng sharp cho egde
				Imgproc.dilate(canny_image, dilated_image, kernel);

				contours.clear();
				Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

				// vẽ hình chữ nhật quanh biển số
				dem = 0;// Đếm số ký tự trong biển số
				vitribienso = -1; // vị trí biển số trong contours
				listchar.clear();
				for (int i = 0; i < contours.size(); i++) {
					Rect r = Imgproc.boundingRect(contours.get(i));
					if (r.width / (double) r.height > 1.0 && r.width / (double) r.height < 2.2) {
						dem = 0;// Đếm số ký tự trong biển số
						// Danh sách vị trí ký tự trong contours
						listchar.clear(); // làm rỗng danh sách
						boolean duplicate = false; // kiểm tra ký tự bị nhân đôi
						for (int check = 0; check < contours.size(); check++) {
							if (check != i) {
								Rect rcheck = Imgproc.boundingRect(contours.get(check));
								if (!(rcheck.width / (double) rcheck.height > 1.0
										&& rcheck.width / (double) rcheck.height < 2.2)) {
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

				try {
					// Cắt lấy biển số
					catbienso = null;
					rbienso = Imgproc.boundingRect(contours.get(vitribienso));
					catbienso = new Mat(image, rbienso);
					if (!(catbienso == null)) {
						return catbienso;
					}
					next = false;
				} catch (Exception ae100) {
					next = true;
				}
			}

			if (next) {
				// 150
				// --------------------------------------------------------------------------------------------------------
				Imgproc.threshold(morph_image, thresh_image, 150, 255, Imgproc.THRESH_BINARY);
				Imgproc.Canny(thresh_image, canny_image, 250, 255);
				kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

				// dilate để tăng sharp cho egde
				Imgproc.dilate(canny_image, dilated_image, kernel);

				contours.clear();
				Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

				// vẽ hình chữ nhật quanh biển số
				dem = 0;// Đếm số ký tự trong biển số
				vitribienso = -1; // vị trí biển số trong contours
				listchar.clear();
				for (int i = 0; i < contours.size(); i++) {
					Rect r = Imgproc.boundingRect(contours.get(i));
					if (r.width / (double) r.height > 1.0 && r.width / (double) r.height < 2.2) {
						dem = 0;// Đếm số ký tự trong biển số
						// Danh sách vị trí ký tự trong contours
						listchar.clear(); // làm rỗng danh sách
						boolean duplicate = false; // kiểm tra ký tự bị nhân đôi
						for (int check = 0; check < contours.size(); check++) {
							if (check != i) {
								Rect rcheck = Imgproc.boundingRect(contours.get(check));
								if (!(rcheck.width / (double) rcheck.height > 1.0
										&& rcheck.width / (double) rcheck.height < 2.2)) {
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

				try {
					// Cắt lấy biển số
					catbienso = null;
					rbienso = Imgproc.boundingRect(contours.get(vitribienso));
					catbienso = new Mat(image, rbienso);
					if (!(catbienso == null)) {
						return catbienso;
					}
					next = false;
				} catch (Exception ae150) {
					next = true;
				}
			}

			if (next) {
				// 200
				// --------------------------------------------------------------------------------------------------------
				Imgproc.threshold(morph_image, thresh_image, 200, 255, Imgproc.THRESH_BINARY);
				Imgproc.Canny(thresh_image, canny_image, 250, 255);
				kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

				// dilate để tăng sharp cho egde
				Imgproc.dilate(canny_image, dilated_image, kernel);

				contours.clear();
				Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

				// vẽ hình chữ nhật quanh biển số
				dem = 0;// Đếm số ký tự trong biển số
				vitribienso = -1; // vị trí biển số trong contours
				listchar.clear();
				for (int i = 0; i < contours.size(); i++) {
					Rect r = Imgproc.boundingRect(contours.get(i));
					if (r.width / (double) r.height > 1.0 && r.width / (double) r.height < 2.2) {
						dem = 0;// Đếm số ký tự trong biển số
						// Danh sách vị trí ký tự trong contours
						listchar.clear(); // làm rỗng danh sách
						boolean duplicate = false; // kiểm tra ký tự bị nhân đôi
						for (int check = 0; check < contours.size(); check++) {
							if (check != i) {
								Rect rcheck = Imgproc.boundingRect(contours.get(check));
								if (!(rcheck.width / (double) rcheck.height > 1.0
										&& rcheck.width / (double) rcheck.height < 2.2)) {
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

				try {
					// Cắt lấy biển số
					catbienso = null;
					rbienso = Imgproc.boundingRect(contours.get(vitribienso));
					catbienso = new Mat(image, rbienso);
					if (!(catbienso == null)) {
						return catbienso;
					}
					next = false;
				} catch (Exception ae200) {
					next = true;
				}
			}

			if (next) {
				// 210
				// --------------------------------------------------------------------------------------------------------
				Imgproc.threshold(morph_image, thresh_image, 210, 255, Imgproc.THRESH_BINARY);
				Imgproc.Canny(thresh_image, canny_image, 250, 255);
				kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

				// dilate để tăng sharp cho egde
				Imgproc.dilate(canny_image, dilated_image, kernel);

				contours.clear();
				Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

				// vẽ hình chữ nhật quanh biển số
				dem = 0;// Đếm số ký tự trong biển số
				vitribienso = -1; // vị trí biển số trong contours
				listchar.clear();
				for (int i = 0; i < contours.size(); i++) {
					Rect r = Imgproc.boundingRect(contours.get(i));
					if (r.width / (double) r.height > 1.0 && r.width / (double) r.height < 2.2) {
						dem = 0;// Đếm số ký tự trong biển số
						// Danh sách vị trí ký tự trong contours
						listchar.clear(); // làm rỗng danh sách
						boolean duplicate = false; // kiểm tra ký tự bị nhân đôi
						for (int check = 0; check < contours.size(); check++) {
							if (check != i) {
								Rect rcheck = Imgproc.boundingRect(contours.get(check));
								if (!(rcheck.width / (double) rcheck.height > 1.0
										&& rcheck.width / (double) rcheck.height < 2.2)) {
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
				try {
					// Cắt lấy biển số
					rbienso = Imgproc.boundingRect(contours.get(vitribienso));
					catbienso = new Mat(image, rbienso);
					if (!(catbienso == null)) {
						return catbienso;
					}
					next = false;
				} catch (Exception ae210) {
					next = true;
				}
			}

			if (next) {
				// 230
				// --------------------------------------------------------------------------------------------------------
				Imgproc.threshold(morph_image, thresh_image, 230, 255, Imgproc.THRESH_BINARY);
				Imgproc.Canny(thresh_image, canny_image, 250, 255);
				kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

				// dilate để tăng sharp cho egde
				Imgproc.dilate(canny_image, dilated_image, kernel);

				contours.clear();
				Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

				// vẽ hình chữ nhật quanh biển số
				dem = 0;// Đếm số ký tự trong biển số
				vitribienso = -1; // vị trí biển số trong contours
				listchar.clear();
				for (int i = 0; i < contours.size(); i++) {
					Rect r = Imgproc.boundingRect(contours.get(i));
					if (r.width / (double) r.height > 1.0 && r.width / (double) r.height < 2.2) {
						dem = 0;// Đếm số ký tự trong biển số
						// Danh sách vị trí ký tự trong contours
						listchar.clear(); // làm rỗng danh sách
						boolean duplicate = false; // kiểm tra ký tự bị nhân đôi
						for (int check = 0; check < contours.size(); check++) {
							if (check != i) {
								Rect rcheck = Imgproc.boundingRect(contours.get(check));
								if (!(rcheck.width / (double) rcheck.height > 1.0
										&& rcheck.width / (double) rcheck.height < 2.2)) {
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
				try {
					// Cắt lấy biển số
					catbienso = null;
					rbienso = Imgproc.boundingRect(contours.get(vitribienso));
					catbienso = new Mat(image, rbienso);
					if (!(catbienso == null)) {
						return catbienso;
					}
					next = false;
				} catch (Exception ae230) {
					next = true;
				}
			}

			if (next) {
				// 250
				// --------------------------------------------------------------------------------------------------------
				Imgproc.threshold(morph_image, thresh_image, 250, 255, Imgproc.THRESH_BINARY);
				Imgproc.Canny(thresh_image, canny_image, 250, 255);
				kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

				// dilate để tăng sharp cho egde
				Imgproc.dilate(canny_image, dilated_image, kernel);

				contours.clear();
				Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

				// vẽ hình chữ nhật quanh biển số
				dem = 0;// Đếm số ký tự trong biển số
				vitribienso = -1; // vị trí biển số trong contours
				listchar.clear();
				for (int i = 0; i < contours.size(); i++) {
					Rect r = Imgproc.boundingRect(contours.get(i));
					if (r.width / (double) r.height > 1.0 && r.width / (double) r.height < 2.2) {
						dem = 0;// Đếm số ký tự trong biển số
						// Danh sách vị trí ký tự trong contours
						listchar.clear(); // làm rỗng danh sách
						boolean duplicate = false; // kiểm tra ký tự bị nhân đôi
						for (int check = 0; check < contours.size(); check++) {
							if (check != i) {
								Rect rcheck = Imgproc.boundingRect(contours.get(check));
								if (!(rcheck.width / (double) rcheck.height > 1.0
										&& rcheck.width / (double) rcheck.height < 2.2)) {
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
				try {
					// Cắt lấy biển số
					catbienso = null;
					rbienso = Imgproc.boundingRect(contours.get(vitribienso));
					catbienso = new Mat(image, rbienso);
					if (!(catbienso == null)) {
						return catbienso;
					}
					next = false;
				} catch (Exception ae250) {
					next = true;
				}
			}

			if (next) {
				catbienso = null;
			}
		} catch (Exception except) {
		}
		return catbienso;
	}
}

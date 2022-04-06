package nhandangbienso;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class TachKyTu {

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
	private List<Rect> listrectchar = new ArrayList<>(); // Danh sách các khung bao quanh các ký tự được nhận diện
	private List<Mat> listrectcharimage = new ArrayList<>(); // Danh sách các ảnh ký tự đã cắt
	private int countchartop = 0; // số lượng ký tự dòng trên, ví dụ: 4 (95E1)
	private int countcharbottom = 0; // số lượng ký tự dòng dưới, ví dụ: 5 (42225)

	public static void main(String[] args) {
		System.load("C:\\Opencv\\build\\java\\x64\\opencv_java430.dll");
		try {
			TachKyTu window = new TachKyTu();
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
		shellBienso.setSize(643, 287);
		shellBienso.setText("Split char");
		shellBienso.setLayout(new FillLayout(SWT.HORIZONTAL));

		Composite composite = new Composite(shellBienso, SWT.NONE);

		Button btnLoadImage = new Button(composite, SWT.NONE);
		btnLoadImage.setBounds(368, 49, 85, 30);
		btnLoadImage.setText("Load Image");

		Button btnSplitchar = new Button(composite, SWT.NONE);
		btnSplitchar.setBounds(472, 49, 130, 30);
		btnSplitchar.setText("Split char");

		textFilename = new Text(composite, SWT.BORDER);
		textFilename.setBounds(10, 47, 350, 30);

		CLabel lbCountchar = new CLabel(composite, SWT.NONE);
		lbCountchar.setBackground(SWTResourceManager.getColor(SWT.COLOR_GRAY));
		lbCountchar.setBounds(27, 121, 436, 44);
		lbCountchar.setText("Count char");

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
				try {
					textFilename.setText(filename);
				} catch (Exception ex) {
					textFilename.setText("");
				}
			}
		});

		// ================================================================================================================
		// Tách ký tự
		btnSplitchar.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {

					im = Imgcodecs.imread(filename); // ảnh gốc
					Mat biensosplit = new Mat();
					// cắt lấy biển số từ ảnh đầu vào
					// Mat biensosplit = new Mat();
					// gọi lớp cắt biển số
					CatBienSo bienso = new CatBienSo();
					biensosplit = bienso.getImageBienso(im);

					try {
						// resize ảnh nếu chiều ngang ảnh nhỏ hơn 300 (resize 1.5 lần)
						if (biensosplit.width() < 200) {
							Imgproc.resize(biensosplit, biensosplit,
									new Size(biensosplit.width() * 2, biensosplit.height() * 2));
						}

						image = biensosplit.clone();
					} catch (NullPointerException nee) {

					}
					Imgproc.cvtColor(biensosplit, im_gray, Imgproc.COLOR_BGR2GRAY);
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

					listrectchar.clear();
					boolean duplicate = false; // kiểm tra ký tự bị nhân đôi
					// hình chữ nhật bao quanh biển số
					Rect r = new Rect(0, 0, biensosplit.width(), biensosplit.height());
					for (int check = 0; check < contours.size(); check++) {
						Rect rcheck = Imgproc.boundingRect(contours.get(check));
						if (!(rcheck.width / (double) rcheck.height > 1.0
								&& rcheck.width / (double) rcheck.height < 2.2)) {
							// Tìm ký tự nằm trong biển số
							if (rcheck.width < rcheck.height
									&& (rcheck.height > (float) r.height / 3 && rcheck.height < (float) r.height / 2)) {
								duplicate = false;
								// loại bỏ những ký tự bị nhân đôi
								for (int k = 0; k < listrectchar.size(); k++) {
									try {
										Rect rcheckduplicate = listrectchar.get(k);
										if (listrectchar.get(k) != rcheck) {
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
									listrectchar.add(rcheck);
								}
							}
						}
					}

					// xóa đi ký tự bị sai, dư ra, có phần bị trùng
					int vitriy = -1;
					for (int i = 0; i < listrectchar.size(); i++) {
						Rect rcheckngoaile = listrectchar.get(i);
						for (int j = 0; j < listrectchar.size(); j++) {
							Rect rcheckngoaile2 = listrectchar.get(j);
							if (listrectchar.get(i) != listrectchar.get(j)) {
								if (rcheckngoaile.x > rcheckngoaile2.x
										&& rcheckngoaile.x < rcheckngoaile2.x + rcheckngoaile2.width
										&& rcheckngoaile.y > rcheckngoaile2.y
										&& rcheckngoaile.y < rcheckngoaile2.y + rcheckngoaile2.height) {
									vitriy = i;
								}
							}
						}
					}
					try {
						if (vitriy >= 0) {
							listrectchar.remove(vitriy);
						}

					} catch (ArrayIndexOutOfBoundsException ae) {
						// Không có ký tự cần xóa
					}

					// nhị phân với ngưỡng 100--------------------------------------------------
					if (listrectchar.size() < 9) {
						// anh nhi phan (trang den)
						Imgproc.threshold(morph_image, thresh_image, 100, 255, Imgproc.THRESH_BINARY);
						// tim bien anh
						Imgproc.Canny(thresh_image, canny_image, 250, 255);
						kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

						// dilate để tăng sharp cho egde
						Imgproc.dilate(canny_image, dilated_image, kernel);

						contours.clear();
						Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST,
								Imgproc.CHAIN_APPROX_NONE);

						duplicate = false; // kiểm tra ký tự bị nhân đôi
						// hình chữ nhật bao quanh biển số
						r = new Rect(0, 0, biensosplit.width(), biensosplit.height());
						for (int check = 0; check < contours.size(); check++) {
							Rect rcheck = Imgproc.boundingRect(contours.get(check));
							if (!(rcheck.width / (double) rcheck.height > 1.0
									&& rcheck.width / (double) rcheck.height < 2.2)) {
								// Tìm ký tự nằm trong biển số
								if (rcheck.width < rcheck.height && (rcheck.height > (float) r.height / 3
										&& rcheck.height < (float) r.height / 2)) {
									duplicate = false;
									// loại bỏ những ký tự bị nhân đôi
									for (int k = 0; k < listrectchar.size(); k++) {
										try {
											Rect rcheckduplicate = listrectchar.get(k);
											if (listrectchar.get(k) != rcheck) {
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
										listrectchar.add(rcheck);
									}
								}
							}
						}

						// xóa đi ký tự bị sai, dư ra, có phần bị trùng
						vitriy = -1;
						for (int i = 0; i < listrectchar.size(); i++) {
							Rect rcheckngoaile = listrectchar.get(i);
							for (int j = 0; j < listrectchar.size(); j++) {
								Rect rcheckngoaile2 = listrectchar.get(j);
								if (listrectchar.get(i) != listrectchar.get(j)) {
									if (rcheckngoaile.x > rcheckngoaile2.x
											&& rcheckngoaile.x < rcheckngoaile2.x + rcheckngoaile2.width
											&& rcheckngoaile.y > rcheckngoaile2.y
											&& rcheckngoaile.y < rcheckngoaile2.y + rcheckngoaile2.height) {
										vitriy = i;
									}
								}
							}
						}
						try {
							if (vitriy >= 0) {
								listrectchar.remove(vitriy);
							}

						} catch (ArrayIndexOutOfBoundsException ae) {
							// Không có ký tự cần xóa
						}
					}

					// nhị phân với ngưỡng 150--------------------------------------------------
					if (listrectchar.size() < 9) {

						// anh nhi phan (trang den)
						Imgproc.threshold(morph_image, thresh_image, 150, 255, Imgproc.THRESH_BINARY);
						// tim bien anh
						Imgproc.Canny(thresh_image, canny_image, 250, 255);
						kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

						// dilate để tăng sharp cho egde
						Imgproc.dilate(canny_image, dilated_image, kernel);

						contours.clear();
						Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST,
								Imgproc.CHAIN_APPROX_NONE);

						duplicate = false; // kiểm tra ký tự bị nhân đôi
						// hình chữ nhật bao quanh biển số
						r = new Rect(0, 0, biensosplit.width(), biensosplit.height());
						for (int check = 0; check < contours.size(); check++) {
							Rect rcheck = Imgproc.boundingRect(contours.get(check));
							if (!(rcheck.width / (double) rcheck.height > 1.0
									&& rcheck.width / (double) rcheck.height < 2.2)) {
								// Tìm ký tự nằm trong biển số
								if (rcheck.width < rcheck.height && (rcheck.height > (float) r.height / 3
										&& rcheck.height < (float) r.height / 2)) {
									duplicate = false;
									// loại bỏ những ký tự bị nhân đôi
									for (int k = 0; k < listrectchar.size(); k++) {
										try {
											Rect rcheckduplicate = listrectchar.get(k);
											if (listrectchar.get(k) != rcheck) {
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
										listrectchar.add(rcheck);
									}
								}
							}
						}

						// xóa đi ký tự bị sai, dư ra, có phần bị trùng
						vitriy = -1;
						for (int i = 0; i < listrectchar.size(); i++) {
							Rect rcheckngoaile = listrectchar.get(i);
							for (int j = 0; j < listrectchar.size(); j++) {
								Rect rcheckngoaile2 = listrectchar.get(j);
								if (listrectchar.get(i) != listrectchar.get(j)) {
									if (rcheckngoaile.x > rcheckngoaile2.x
											&& rcheckngoaile.x < rcheckngoaile2.x + rcheckngoaile2.width
											&& rcheckngoaile.y > rcheckngoaile2.y
											&& rcheckngoaile.y < rcheckngoaile2.y + rcheckngoaile2.height) {
										vitriy = i;
									}
								}
							}
						}
						try {
							if (vitriy >= 0) {
								listrectchar.remove(vitriy);
							}

						} catch (ArrayIndexOutOfBoundsException ae) {
							// Không có ký tự cần xóa
						}
					}

					// nhị phân với ngưỡng 200--------------------------------------------------
					if (listrectchar.size() < 9) {

						// anh nhi phan (trang den)
						Imgproc.threshold(morph_image, thresh_image, 200, 255, Imgproc.THRESH_BINARY);
						// tim bien anh
						Imgproc.Canny(thresh_image, canny_image, 250, 255);
						kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

						// dilate để tăng sharp cho egde
						Imgproc.dilate(canny_image, dilated_image, kernel);

						contours.clear();
						Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST,
								Imgproc.CHAIN_APPROX_NONE);

						duplicate = false; // kiểm tra ký tự bị nhân đôi
						// hình chữ nhật bao quanh biển số
						r = new Rect(0, 0, biensosplit.width(), biensosplit.height());
						for (int check = 0; check < contours.size(); check++) {
							Rect rcheck = Imgproc.boundingRect(contours.get(check));
							if (!(rcheck.width / (double) rcheck.height > 1.0
									&& rcheck.width / (double) rcheck.height < 2.2)) {
								// Tìm ký tự nằm trong biển số
								if (rcheck.width < rcheck.height && (rcheck.height > (float) r.height / 3
										&& rcheck.height < (float) r.height / 2)) {
									duplicate = false;
									// loại bỏ những ký tự bị nhân đôi
									for (int k = 0; k < listrectchar.size(); k++) {
										try {
											Rect rcheckduplicate = listrectchar.get(k);
											if (listrectchar.get(k) != rcheck) {
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
										listrectchar.add(rcheck);
									}
								}
							}
						}

						// xóa đi ký tự bị sai, dư ra, có phần bị trùng
						vitriy = -1;
						for (int i = 0; i < listrectchar.size(); i++) {
							Rect rcheckngoaile = listrectchar.get(i);
							for (int j = 0; j < listrectchar.size(); j++) {
								Rect rcheckngoaile2 = listrectchar.get(j);
								if (listrectchar.get(i) != listrectchar.get(j)) {
									if (rcheckngoaile.x > rcheckngoaile2.x
											&& rcheckngoaile.x < rcheckngoaile2.x + rcheckngoaile2.width
											&& rcheckngoaile.y > rcheckngoaile2.y
											&& rcheckngoaile.y < rcheckngoaile2.y + rcheckngoaile2.height) {
										vitriy = i;
									}
								}
							}
						}
						try {
							if (vitriy >= 0) {
								listrectchar.remove(vitriy);
							}

						} catch (ArrayIndexOutOfBoundsException ae) {
							// Không có ký tự cần xóa
						}
					}

					// nhị phân với ngưỡng 210--------------------------------------------------
					if (listrectchar.size() < 9) {

						// anh nhi phan (trang den)
						Imgproc.threshold(morph_image, thresh_image, 210, 255, Imgproc.THRESH_BINARY);
						// tim bien anh
						Imgproc.Canny(thresh_image, canny_image, 250, 255);
						kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

						// dilate để tăng sharp cho egde
						Imgproc.dilate(canny_image, dilated_image, kernel);

						contours.clear();
						Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST,
								Imgproc.CHAIN_APPROX_NONE);

						duplicate = false; // kiểm tra ký tự bị nhân đôi
						// hình chữ nhật bao quanh biển số
						r = new Rect(0, 0, biensosplit.width(), biensosplit.height());
						for (int check = 0; check < contours.size(); check++) {
							Rect rcheck = Imgproc.boundingRect(contours.get(check));
							if (!(rcheck.width / (double) rcheck.height > 1.0
									&& rcheck.width / (double) rcheck.height < 2.2)) {
								// Tìm ký tự nằm trong biển số
								if (rcheck.width < rcheck.height && (rcheck.height > (float) r.height / 3
										&& rcheck.height < (float) r.height / 2)) {
									duplicate = false;
									// loại bỏ những ký tự bị nhân đôi
									for (int k = 0; k < listrectchar.size(); k++) {
										try {
											Rect rcheckduplicate = listrectchar.get(k);
											if (listrectchar.get(k) != rcheck) {
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
										listrectchar.add(rcheck);
									}
								}
							}
						}

						// xóa đi ký tự bị sai, dư ra, có phần bị trùng
						vitriy = -1;
						for (int i = 0; i < listrectchar.size(); i++) {
							Rect rcheckngoaile = listrectchar.get(i);
							for (int j = 0; j < listrectchar.size(); j++) {
								Rect rcheckngoaile2 = listrectchar.get(j);
								if (listrectchar.get(i) != listrectchar.get(j)) {
									if (rcheckngoaile.x > rcheckngoaile2.x
											&& rcheckngoaile.x < rcheckngoaile2.x + rcheckngoaile2.width
											&& rcheckngoaile.y > rcheckngoaile2.y
											&& rcheckngoaile.y < rcheckngoaile2.y + rcheckngoaile2.height) {
										vitriy = i;
									}
								}
							}
						}
						try {
							if (vitriy >= 0) {
								listrectchar.remove(vitriy);
							}

						} catch (ArrayIndexOutOfBoundsException ae) {
							// Không có ký tự cần xóa
						}
					}

					// nhị phân với ngưỡng 230--------------------------------------------------
					if (listrectchar.size() < 9) {

						// anh nhi phan (trang den)
						Imgproc.threshold(morph_image, thresh_image, 230, 255, Imgproc.THRESH_BINARY);
						// tim bien anh
						Imgproc.Canny(thresh_image, canny_image, 250, 255);
						kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

						// dilate để tăng sharp cho egde
						Imgproc.dilate(canny_image, dilated_image, kernel);

						contours.clear();
						Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST,
								Imgproc.CHAIN_APPROX_NONE);

						duplicate = false; // kiểm tra ký tự bị nhân đôi
						// hình chữ nhật bao quanh biển số
						r = new Rect(0, 0, biensosplit.width(), biensosplit.height());
						for (int check = 0; check < contours.size(); check++) {
							Rect rcheck = Imgproc.boundingRect(contours.get(check));
							if (!(rcheck.width / (double) rcheck.height > 1.0
									&& rcheck.width / (double) rcheck.height < 2.2)) {
								// Tìm ký tự nằm trong biển số
								if (rcheck.width < rcheck.height && (rcheck.height > (float) r.height / 3
										&& rcheck.height < (float) r.height / 2)) {
									duplicate = false;
									// loại bỏ những ký tự bị nhân đôi
									for (int k = 0; k < listrectchar.size(); k++) {
										try {
											Rect rcheckduplicate = listrectchar.get(k);
											if (listrectchar.get(k) != rcheck) {
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
										listrectchar.add(rcheck);
									}
								}
							}
						}

						// xóa đi ký tự bị sai, dư ra, có phần bị trùng
						vitriy = -1;
						for (int i = 0; i < listrectchar.size(); i++) {
							Rect rcheckngoaile = listrectchar.get(i);
							for (int j = 0; j < listrectchar.size(); j++) {
								Rect rcheckngoaile2 = listrectchar.get(j);
								if (listrectchar.get(i) != listrectchar.get(j)) {
									if (rcheckngoaile.x > rcheckngoaile2.x
											&& rcheckngoaile.x < rcheckngoaile2.x + rcheckngoaile2.width
											&& rcheckngoaile.y > rcheckngoaile2.y
											&& rcheckngoaile.y < rcheckngoaile2.y + rcheckngoaile2.height) {
										vitriy = i;
									}
								}
							}
						}
						try {
							if (vitriy >= 0) {
								listrectchar.remove(vitriy);
							}

						} catch (ArrayIndexOutOfBoundsException ae) {
							// Không có ký tự cần xóa
						}
					}

					// nhị phân với ngưỡng 250--------------------------------------------------
					if (listrectchar.size() < 9) {

						// anh nhi phan (trang den)
						Imgproc.threshold(morph_image, thresh_image, 250, 255, Imgproc.THRESH_BINARY);
						// tim bien anh
						Imgproc.Canny(thresh_image, canny_image, 250, 255);
						kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

						// dilate để tăng sharp cho egde
						Imgproc.dilate(canny_image, dilated_image, kernel);

						contours.clear();
						Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST,
								Imgproc.CHAIN_APPROX_NONE);

						duplicate = false; // kiểm tra ký tự bị nhân đôi
						// hình chữ nhật bao quanh biển số
						r = new Rect(0, 0, biensosplit.width(), biensosplit.height());
						for (int check = 0; check < contours.size(); check++) {
							Rect rcheck = Imgproc.boundingRect(contours.get(check));
							if (!(rcheck.width / (double) rcheck.height > 1.0
									&& rcheck.width / (double) rcheck.height < 2.2)) {
								// Tìm ký tự nằm trong biển số
								if (rcheck.width < rcheck.height && (rcheck.height > (float) r.height / 3
										&& rcheck.height < (float) r.height / 2)) {
									duplicate = false;
									// loại bỏ những ký tự bị nhân đôi
									for (int k = 0; k < listrectchar.size(); k++) {
										try {
											Rect rcheckduplicate = listrectchar.get(k);
											if (listrectchar.get(k) != rcheck) {
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
										listrectchar.add(rcheck);
									}
								}
							}
						}

						// xóa đi ký tự bị sai, dư ra, có phần bị trùng
						vitriy = -1;
						for (int i = 0; i < listrectchar.size(); i++) {
							Rect rcheckngoaile = listrectchar.get(i);
							for (int j = 0; j < listrectchar.size(); j++) {
								Rect rcheckngoaile2 = listrectchar.get(j);
								if (listrectchar.get(i) != listrectchar.get(j)) {
									if (rcheckngoaile.x > rcheckngoaile2.x
											&& rcheckngoaile.x < rcheckngoaile2.x + rcheckngoaile2.width
											&& rcheckngoaile.y > rcheckngoaile2.y
											&& rcheckngoaile.y < rcheckngoaile2.y + rcheckngoaile2.height) {
										vitriy = i;
									}
								}
							}
						}
						try {
							if (vitriy >= 0) {
								listrectchar.remove(vitriy);
							}

						} catch (ArrayIndexOutOfBoundsException ae) {
							// Không có ký tự cần xóa
						}
					}

					// ************************************************************************************
					// sắp xếp vị trí ký tự biển số cho đúng với thứ tự thật của biển số
					ArrayList<Rect> charsorttop = new ArrayList<>();
					ArrayList<Rect> charsortbottom = new ArrayList<>();
					int top = 0;
					for (int i = 0; i < listrectchar.size(); i++) {
						Rect rcheck1 = listrectchar.get(i);
						for (int j = 0; j < listrectchar.size(); j++) {
							Rect rcheck2 = listrectchar.get(j);
							if (rcheck1.y + rcheck1.height / 2 < rcheck2.y + rcheck2.height) {
								top++;
							}
						}
						if (top == listrectchar.size()) {
							charsorttop.add(listrectchar.get(i));
						} else {
							charsortbottom.add(listrectchar.get(i));
						}
						top = 0;
					}
					countchartop = charsorttop.size();
					countcharbottom = charsortbottom.size();
					listrectchar.clear();
					// Sắp xếp thứ tự
					for (int i = 0; i < charsorttop.size(); i++) {
						Rect rectkytu = charsorttop.get(i);
						for (int j = i; j < charsorttop.size(); j++) {
							Rect rectkytu2 = charsorttop.get(j);
							if (rectkytu.x > rectkytu2.x) {
								Rect temp = charsorttop.get(i);
								charsorttop.set(i, charsorttop.get(j));
								charsorttop.set(j, temp);
							}
						}
					}
					for (int i = 0; i < charsorttop.size(); i++) {
						listrectchar.add(charsorttop.get(i));
					}

					for (int i = 0; i < charsortbottom.size(); i++) {
						Rect rectkytu = charsortbottom.get(i);
						for (int j = i; j < charsortbottom.size(); j++) {
							Rect rectkytu2 = charsortbottom.get(j);
							if (rectkytu.x > rectkytu2.x) {
								Rect temp = charsortbottom.get(i);
								charsortbottom.set(i, charsortbottom.get(j));
								charsortbottom.set(j, temp);
							}
						}
					}
					for (int i = 0; i < charsortbottom.size(); i++) {
						listrectchar.add(charsortbottom.get(i));
					}

					// Cắt lấy các ký tự trong biển số
					listrectcharimage.clear();
					for (int vitri = 0; vitri < listrectchar.size(); vitri++) {
						Rect rkytu = listrectchar.get(vitri);
						listrectcharimage.add(new Mat(image, rkytu));
					}

					HighGui.imshow("Bien so", image);
					// Imgcodecs.imwrite("D:/Bienso/Save/" + "bienso.jpg", image);
					int sokytu = 0;
					for (Mat imagechar : listrectcharimage) {
						try {
							// Imgcodecs.imwrite("D:/Bienso/Save/" + sokytu + ".jpg", imagechar);
							HighGui.imshow("Ky tu " + sokytu, imagechar);
						} catch (Exception exc) {
						}
						sokytu++;
					}

				} catch (Exception excepttion) {
					excepttion.printStackTrace();
				}

				HighGui.waitKey(0);

			}

		});
	}

	// ======================================================================================================================================
	// trả về biển số là kiểu Mat
	public List<Mat> getCharImage(Mat anhchuabienso) {
		try {
			im = anhchuabienso.clone(); // ảnh gốc
			Mat biensosplit = new Mat();
			// cắt lấy biển số từ ảnh đầu vào
			// Mat biensosplit = new Mat();
			// gọi lớp cắt biển số
			CatBienSo bienso = new CatBienSo();
			biensosplit = bienso.getImageBienso(im);

			try {
				// resize ảnh nếu chiều ngang ảnh nhỏ hơn 300 (resize 1.5 lần)
				if (biensosplit.width() < 200) {
					Imgproc.resize(biensosplit, biensosplit,
							new Size(biensosplit.width() * 2, biensosplit.height() * 2));
				}
				image = biensosplit.clone();
			} catch (NullPointerException ne) {

			}

			Imgproc.cvtColor(biensosplit, im_gray, Imgproc.COLOR_BGR2GRAY);
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

			listrectchar.clear();
			boolean duplicate = false; // kiểm tra ký tự bị nhân đôi
			// hình chữ nhật bao quanh biển số
			Rect r = new Rect(0, 0, biensosplit.width(), biensosplit.height());
			for (int check = 0; check < contours.size(); check++) {
				Rect rcheck = Imgproc.boundingRect(contours.get(check));
				if (!(rcheck.width / (double) rcheck.height > 1.0 && rcheck.width / (double) rcheck.height < 2.2)) {
					// Tìm ký tự nằm trong biển số
					if (rcheck.width < rcheck.height
							&& (rcheck.height > (float) r.height / 3 && rcheck.height < (float) r.height / 2)) {
						duplicate = false;
						// loại bỏ những ký tự bị nhân đôi
						for (int k = 0; k < listrectchar.size(); k++) {
							try {
								Rect rcheckduplicate = listrectchar.get(k);
								if (listrectchar.get(k) != rcheck) {
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
							listrectchar.add(rcheck);
						}
					}
				}
			}

			// xóa đi ký tự bị sai, dư ra, có phần bị trùng
			int vitriy = -1;
			for (int i = 0; i < listrectchar.size(); i++) {
				Rect rcheckngoaile = listrectchar.get(i);
				for (int j = 0; j < listrectchar.size(); j++) {
					Rect rcheckngoaile2 = listrectchar.get(j);
					if (listrectchar.get(i) != listrectchar.get(j)) {
						if (rcheckngoaile.x > rcheckngoaile2.x
								&& rcheckngoaile.x < rcheckngoaile2.x + rcheckngoaile2.width
								&& rcheckngoaile.y > rcheckngoaile2.y
								&& rcheckngoaile.y < rcheckngoaile2.y + rcheckngoaile2.height) {
							vitriy = i;
						}
					}
				}
			}
			try {
				if (vitriy >= 0) {
					listrectchar.remove(vitriy);
				}

			} catch (ArrayIndexOutOfBoundsException ae) {
				// Không có ký tự cần xóa
			}

			// nhị phân với ngưỡng 100--------------------------------------------------
			if (listrectchar.size() < 9) {
				// anh nhi phan (trang den)
				Imgproc.threshold(morph_image, thresh_image, 100, 255, Imgproc.THRESH_BINARY);
				// tim bien anh
				Imgproc.Canny(thresh_image, canny_image, 250, 255);
				kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

				// dilate để tăng sharp cho egde
				Imgproc.dilate(canny_image, dilated_image, kernel);

				contours.clear();
				Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

				duplicate = false; // kiểm tra ký tự bị nhân đôi
				// hình chữ nhật bao quanh biển số
				r = new Rect(0, 0, biensosplit.width(), biensosplit.height());
				for (int check = 0; check < contours.size(); check++) {
					Rect rcheck = Imgproc.boundingRect(contours.get(check));
					if (!(rcheck.width / (double) rcheck.height > 1.0 && rcheck.width / (double) rcheck.height < 2.2)) {
						// Tìm ký tự nằm trong biển số
						if (rcheck.width < rcheck.height
								&& (rcheck.height > (float) r.height / 3 && rcheck.height < (float) r.height / 2)) {
							duplicate = false;
							// loại bỏ những ký tự bị nhân đôi
							for (int k = 0; k < listrectchar.size(); k++) {
								try {
									Rect rcheckduplicate = listrectchar.get(k);
									if (listrectchar.get(k) != rcheck) {
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
								listrectchar.add(rcheck);
							}
						}
					}
				}

				// xóa đi ký tự bị sai, dư ra, có phần bị trùng
				vitriy = -1;
				for (int i = 0; i < listrectchar.size(); i++) {
					Rect rcheckngoaile = listrectchar.get(i);
					for (int j = 0; j < listrectchar.size(); j++) {
						Rect rcheckngoaile2 = listrectchar.get(j);
						if (listrectchar.get(i) != listrectchar.get(j)) {
							if (rcheckngoaile.x > rcheckngoaile2.x
									&& rcheckngoaile.x < rcheckngoaile2.x + rcheckngoaile2.width
									&& rcheckngoaile.y > rcheckngoaile2.y
									&& rcheckngoaile.y < rcheckngoaile2.y + rcheckngoaile2.height) {
								vitriy = i;
							}
						}
					}
				}
				try {
					if (vitriy >= 0) {
						listrectchar.remove(vitriy);
					}

				} catch (ArrayIndexOutOfBoundsException ae) {
					// Không có ký tự cần xóa
				}
			}

			// nhị phân với ngưỡng 150--------------------------------------------------
			if (listrectchar.size() < 9) {

				// anh nhi phan (trang den)
				Imgproc.threshold(morph_image, thresh_image, 150, 255, Imgproc.THRESH_BINARY);
				// tim bien anh
				Imgproc.Canny(thresh_image, canny_image, 250, 255);
				kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

				// dilate để tăng sharp cho egde
				Imgproc.dilate(canny_image, dilated_image, kernel);

				contours.clear();
				Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

				duplicate = false; // kiểm tra ký tự bị nhân đôi
				// hình chữ nhật bao quanh biển số
				r = new Rect(0, 0, biensosplit.width(), biensosplit.height());
				for (int check = 0; check < contours.size(); check++) {
					Rect rcheck = Imgproc.boundingRect(contours.get(check));
					if (!(rcheck.width / (double) rcheck.height > 1.0 && rcheck.width / (double) rcheck.height < 2.2)) {
						// Tìm ký tự nằm trong biển số
						if (rcheck.width < rcheck.height
								&& (rcheck.height > (float) r.height / 3 && rcheck.height < (float) r.height / 2)) {
							duplicate = false;
							// loại bỏ những ký tự bị nhân đôi
							for (int k = 0; k < listrectchar.size(); k++) {
								try {
									Rect rcheckduplicate = listrectchar.get(k);
									if (listrectchar.get(k) != rcheck) {
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
								listrectchar.add(rcheck);
							}
						}
					}
				}

				// xóa đi ký tự bị sai, dư ra, có phần bị trùng
				vitriy = -1;
				for (int i = 0; i < listrectchar.size(); i++) {
					Rect rcheckngoaile = listrectchar.get(i);
					for (int j = 0; j < listrectchar.size(); j++) {
						Rect rcheckngoaile2 = listrectchar.get(j);
						if (listrectchar.get(i) != listrectchar.get(j)) {
							if (rcheckngoaile.x > rcheckngoaile2.x
									&& rcheckngoaile.x < rcheckngoaile2.x + rcheckngoaile2.width
									&& rcheckngoaile.y > rcheckngoaile2.y
									&& rcheckngoaile.y < rcheckngoaile2.y + rcheckngoaile2.height) {
								vitriy = i;
							}
						}
					}
				}
				try {
					if (vitriy >= 0) {
						listrectchar.remove(vitriy);
					}

				} catch (ArrayIndexOutOfBoundsException ae) {
					// Không có ký tự cần xóa
				}
			}

			// nhị phân với ngưỡng 200--------------------------------------------------
			if (listrectchar.size() < 9) {

				// anh nhi phan (trang den)
				Imgproc.threshold(morph_image, thresh_image, 200, 255, Imgproc.THRESH_BINARY);
				// tim bien anh
				Imgproc.Canny(thresh_image, canny_image, 250, 255);
				kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

				// dilate để tăng sharp cho egde
				Imgproc.dilate(canny_image, dilated_image, kernel);

				contours.clear();
				Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

				duplicate = false; // kiểm tra ký tự bị nhân đôi
				// hình chữ nhật bao quanh biển số
				r = new Rect(0, 0, biensosplit.width(), biensosplit.height());
				for (int check = 0; check < contours.size(); check++) {
					Rect rcheck = Imgproc.boundingRect(contours.get(check));
					if (!(rcheck.width / (double) rcheck.height > 1.0 && rcheck.width / (double) rcheck.height < 2.2)) {
						// Tìm ký tự nằm trong biển số
						if (rcheck.width < rcheck.height
								&& (rcheck.height > (float) r.height / 3 && rcheck.height < (float) r.height / 2)) {
							duplicate = false;
							// loại bỏ những ký tự bị nhân đôi
							for (int k = 0; k < listrectchar.size(); k++) {
								try {
									Rect rcheckduplicate = listrectchar.get(k);
									if (listrectchar.get(k) != rcheck) {
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
								listrectchar.add(rcheck);
							}
						}
					}
				}

				// xóa đi ký tự bị sai, dư ra, có phần bị trùng
				vitriy = -1;
				for (int i = 0; i < listrectchar.size(); i++) {
					Rect rcheckngoaile = listrectchar.get(i);
					for (int j = 0; j < listrectchar.size(); j++) {
						Rect rcheckngoaile2 = listrectchar.get(j);
						if (listrectchar.get(i) != listrectchar.get(j)) {
							if (rcheckngoaile.x > rcheckngoaile2.x
									&& rcheckngoaile.x < rcheckngoaile2.x + rcheckngoaile2.width
									&& rcheckngoaile.y > rcheckngoaile2.y
									&& rcheckngoaile.y < rcheckngoaile2.y + rcheckngoaile2.height) {
								vitriy = i;
							}
						}
					}
				}
				try {
					if (vitriy >= 0) {
						listrectchar.remove(vitriy);
					}

				} catch (ArrayIndexOutOfBoundsException ae) {
					// Không có ký tự cần xóa
				}
			}

			// nhị phân với ngưỡng 210--------------------------------------------------
			if (listrectchar.size() < 9) {

				// anh nhi phan (trang den)
				Imgproc.threshold(morph_image, thresh_image, 210, 255, Imgproc.THRESH_BINARY);
				// tim bien anh
				Imgproc.Canny(thresh_image, canny_image, 250, 255);
				kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

				// dilate để tăng sharp cho egde
				Imgproc.dilate(canny_image, dilated_image, kernel);

				contours.clear();
				Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

				duplicate = false; // kiểm tra ký tự bị nhân đôi
				// hình chữ nhật bao quanh biển số
				r = new Rect(0, 0, biensosplit.width(), biensosplit.height());
				for (int check = 0; check < contours.size(); check++) {
					Rect rcheck = Imgproc.boundingRect(contours.get(check));
					if (!(rcheck.width / (double) rcheck.height > 1.0 && rcheck.width / (double) rcheck.height < 2.2)) {
						// Tìm ký tự nằm trong biển số
						if (rcheck.width < rcheck.height
								&& (rcheck.height > (float) r.height / 3 && rcheck.height < (float) r.height / 2)) {
							duplicate = false;
							// loại bỏ những ký tự bị nhân đôi
							for (int k = 0; k < listrectchar.size(); k++) {
								try {
									Rect rcheckduplicate = listrectchar.get(k);
									if (listrectchar.get(k) != rcheck) {
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
								listrectchar.add(rcheck);
							}
						}
					}
				}

				// xóa đi ký tự bị sai, dư ra, có phần bị trùng
				vitriy = -1;
				for (int i = 0; i < listrectchar.size(); i++) {
					Rect rcheckngoaile = listrectchar.get(i);
					for (int j = 0; j < listrectchar.size(); j++) {
						Rect rcheckngoaile2 = listrectchar.get(j);
						if (listrectchar.get(i) != listrectchar.get(j)) {
							if (rcheckngoaile.x > rcheckngoaile2.x
									&& rcheckngoaile.x < rcheckngoaile2.x + rcheckngoaile2.width
									&& rcheckngoaile.y > rcheckngoaile2.y
									&& rcheckngoaile.y < rcheckngoaile2.y + rcheckngoaile2.height) {
								vitriy = i;
							}
						}
					}
				}
				try {
					if (vitriy >= 0) {
						listrectchar.remove(vitriy);
					}

				} catch (ArrayIndexOutOfBoundsException ae) {
					// Không có ký tự cần xóa
				}
			}

			// nhị phân với ngưỡng 230--------------------------------------------------
			if (listrectchar.size() < 9) {

				// anh nhi phan (trang den)
				Imgproc.threshold(morph_image, thresh_image, 230, 255, Imgproc.THRESH_BINARY);
				// tim bien anh
				Imgproc.Canny(thresh_image, canny_image, 250, 255);
				kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

				// dilate để tăng sharp cho egde
				Imgproc.dilate(canny_image, dilated_image, kernel);

				contours.clear();
				Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

				duplicate = false; // kiểm tra ký tự bị nhân đôi
				// hình chữ nhật bao quanh biển số
				r = new Rect(0, 0, biensosplit.width(), biensosplit.height());
				for (int check = 0; check < contours.size(); check++) {
					Rect rcheck = Imgproc.boundingRect(contours.get(check));
					if (!(rcheck.width / (double) rcheck.height > 1.0 && rcheck.width / (double) rcheck.height < 2.2)) {
						// Tìm ký tự nằm trong biển số
						if (rcheck.width < rcheck.height
								&& (rcheck.height > (float) r.height / 3 && rcheck.height < (float) r.height / 2)) {
							duplicate = false;
							// loại bỏ những ký tự bị nhân đôi
							for (int k = 0; k < listrectchar.size(); k++) {
								try {
									Rect rcheckduplicate = listrectchar.get(k);
									if (listrectchar.get(k) != rcheck) {
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
								listrectchar.add(rcheck);
							}
						}
					}
				}

				// xóa đi ký tự bị sai, dư ra, có phần bị trùng
				vitriy = -1;
				for (int i = 0; i < listrectchar.size(); i++) {
					Rect rcheckngoaile = listrectchar.get(i);
					for (int j = 0; j < listrectchar.size(); j++) {
						Rect rcheckngoaile2 = listrectchar.get(j);
						if (listrectchar.get(i) != listrectchar.get(j)) {
							if (rcheckngoaile.x > rcheckngoaile2.x
									&& rcheckngoaile.x < rcheckngoaile2.x + rcheckngoaile2.width
									&& rcheckngoaile.y > rcheckngoaile2.y
									&& rcheckngoaile.y < rcheckngoaile2.y + rcheckngoaile2.height) {
								vitriy = i;
							}
						}
					}
				}
				try {
					if (vitriy >= 0) {
						listrectchar.remove(vitriy);
					}

				} catch (ArrayIndexOutOfBoundsException ae) {
					// Không có ký tự cần xóa
				}
			}

			// nhị phân với ngưỡng 250--------------------------------------------------
			if (listrectchar.size() < 9) {

				// anh nhi phan (trang den)
				Imgproc.threshold(morph_image, thresh_image, 250, 255, Imgproc.THRESH_BINARY);
				// tim bien anh
				Imgproc.Canny(thresh_image, canny_image, 250, 255);
				kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);

				// dilate để tăng sharp cho egde
				Imgproc.dilate(canny_image, dilated_image, kernel);

				contours.clear();
				Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

				duplicate = false; // kiểm tra ký tự bị nhân đôi
				// hình chữ nhật bao quanh biển số
				r = new Rect(0, 0, biensosplit.width(), biensosplit.height());
				for (int check = 0; check < contours.size(); check++) {
					Rect rcheck = Imgproc.boundingRect(contours.get(check));
					if (!(rcheck.width / (double) rcheck.height > 1.0 && rcheck.width / (double) rcheck.height < 2.2)) {
						// Tìm ký tự nằm trong biển số
						if (rcheck.width < rcheck.height
								&& (rcheck.height > (float) r.height / 3 && rcheck.height < (float) r.height / 2)) {
							duplicate = false;
							// loại bỏ những ký tự bị nhân đôi
							for (int k = 0; k < listrectchar.size(); k++) {
								try {
									Rect rcheckduplicate = listrectchar.get(k);
									if (listrectchar.get(k) != rcheck) {
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
								listrectchar.add(rcheck);
							}
						}
					}
				}

				// xóa đi ký tự bị sai, dư ra, có phần bị trùng
				vitriy = -1;
				for (int i = 0; i < listrectchar.size(); i++) {
					Rect rcheckngoaile = listrectchar.get(i);
					for (int j = 0; j < listrectchar.size(); j++) {
						Rect rcheckngoaile2 = listrectchar.get(j);
						if (listrectchar.get(i) != listrectchar.get(j)) {
							if (rcheckngoaile.x > rcheckngoaile2.x
									&& rcheckngoaile.x < rcheckngoaile2.x + rcheckngoaile2.width
									&& rcheckngoaile.y > rcheckngoaile2.y
									&& rcheckngoaile.y < rcheckngoaile2.y + rcheckngoaile2.height) {
								vitriy = i;
							}
						}
					}
				}
				try {
					if (vitriy >= 0) {
						listrectchar.remove(vitriy);
					}

				} catch (ArrayIndexOutOfBoundsException ae) {
					// Không có ký tự cần xóa
				}
			}

			// ************************************************************************************
			// sắp xếp vị trí ký tự biển số cho đúng với thứ tự thật của biển số
			ArrayList<Rect> charsorttop = new ArrayList<>();
			ArrayList<Rect> charsortbottom = new ArrayList<>();
			int top = 0;
			for (int i = 0; i < listrectchar.size(); i++) {
				Rect rcheck1 = listrectchar.get(i);
				for (int j = 0; j < listrectchar.size(); j++) {
					Rect rcheck2 = listrectchar.get(j);
					if (rcheck1.y + rcheck1.height / 2 < rcheck2.y + rcheck2.height) {
						top++;
					}
				}
				if (top == listrectchar.size()) {
					charsorttop.add(listrectchar.get(i));
				} else {
					charsortbottom.add(listrectchar.get(i));
				}
				top = 0;
			}
			countchartop = charsorttop.size();
			countcharbottom = charsortbottom.size();
			listrectchar.clear();
			// Sắp xếp thứ tự
			for (int i = 0; i < charsorttop.size(); i++) {
				Rect rectkytu = charsorttop.get(i);
				for (int j = i; j < charsorttop.size(); j++) {
					Rect rectkytu2 = charsorttop.get(j);
					if (rectkytu.x > rectkytu2.x) {
						Rect temp = charsorttop.get(i);
						charsorttop.set(i, charsorttop.get(j));
						charsorttop.set(j, temp);
					}
				}
			}
			for (int i = 0; i < charsorttop.size(); i++) {
				listrectchar.add(charsorttop.get(i));
			}

			for (int i = 0; i < charsortbottom.size(); i++) {
				Rect rectkytu = charsortbottom.get(i);
				for (int j = i; j < charsortbottom.size(); j++) {
					Rect rectkytu2 = charsortbottom.get(j);
					if (rectkytu.x > rectkytu2.x) {
						Rect temp = charsortbottom.get(i);
						charsortbottom.set(i, charsortbottom.get(j));
						charsortbottom.set(j, temp);
					}
				}
			}
			for (int i = 0; i < charsortbottom.size(); i++) {
				listrectchar.add(charsortbottom.get(i));
			}

			// Cắt lấy các ký tự trong biển số
			listrectcharimage.clear();
			for (int vitri = 0; vitri < listrectchar.size(); vitri++) {
				Rect rkytu = listrectchar.get(vitri);
				listrectcharimage.add(new Mat(image, rkytu));
			}

		} catch (Exception excepttion) {
			excepttion.printStackTrace();
		}

		return listrectcharimage;
	}

	// =============================================================================================================
	// lấy số lượng ký tự dòng trên
	public int getCountchartop() {
		return countchartop;
	}

	// =============================================================================================================
	// lấy số lượng ký tự dòng dưới
	public int getCountcharbottom() {
		return countcharbottom;
	}
}

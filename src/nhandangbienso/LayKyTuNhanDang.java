package nhandangbienso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class LayKyTuNhanDang {

	protected Shell shell;
	private Text textListfile;
	private String loadfolder = "";
	private String savefolder = "D:/Bienso/Save";
	private ArrayList<Mat> danhsachanh = new ArrayList<Mat>();

	/**
	 * Launch the application.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		System.load("C:\\Opencv\\build\\java\\x64\\opencv_java430.dll");
		try {
			LayKyTuNhanDang window = new LayKyTuNhanDang();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		shell.setSize(588, 489);
		shell.setText("\u1EA2nh c\u1EAFt ra resize 12x28");

		textListfile = new Text(shell, SWT.BORDER | SWT.MULTI);
		textListfile.setFont(SWTResourceManager.getFont("Times New Roman", 9, SWT.NORMAL));
		textListfile.setBounds(11, 67, 548, 373);

		CLabel lbFolder = new CLabel(shell, SWT.NONE);
		lbFolder.setFont(SWTResourceManager.getFont("Times New Roman", 9, SWT.NORMAL));
		lbFolder.setBackground(SWTResourceManager.getColor(SWT.COLOR_GRAY));
		lbFolder.setBounds(11, 23, 319, 25);
		lbFolder.setText("");

		Button btnLoadfolder = new Button(shell, SWT.NONE);
		btnLoadfolder.setFont(SWTResourceManager.getFont("Times New Roman", 9, SWT.NORMAL));
		btnLoadfolder.setBounds(336, 23, 46, 25);
		btnLoadfolder.setText("...");

		Button btnFoldersave = new Button(shell, SWT.NONE);
		btnFoldersave.setFont(SWTResourceManager.getFont("Times New Roman", 9, SWT.NORMAL));
		btnFoldersave.setBounds(388, 23, 93, 25);
		btnFoldersave.setText("Save directory");

		Button btnGet = new Button(shell, SWT.NONE);
		btnGet.setFont(SWTResourceManager.getFont("Times New Roman", 9, SWT.NORMAL));
		btnGet.setBounds(484, 23, 75, 25);
		btnGet.setText("Get");
		// ============================================================================================================
		// Load folder chứa ảnh cần cắt lấy ký tự rồi resize ảnh thành 12x28
		btnLoadfolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					DirectoryDialog dlg = new DirectoryDialog(shell, SWT.OPEN);
					// Đặt đường dẫn giá trị ban đầu khi mở
					dlg.setFilterPath("D:/Bienso");

					// Change the title bar text
					dlg.setText("Chọn thư mục ảnh");

					// Customizable message displayed in the dialog
					dlg.setMessage("Select a directory");

					String dir = dlg.open();
					if (dir != null) {
						lbFolder.setText(dir);
						loadfolder = dir;
					}
				} catch (Exception excep) {
				}
			}
		});

		// ============================================================================================================
		// Thư mực lưu những ảnh đã resize
		btnFoldersave.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					DirectoryDialog dlg = new DirectoryDialog(shell, SWT.OPEN);
					// Đặt đường dẫn giá trị ban đầu khi mở
					dlg.setFilterPath("D:/Bienso");

					// Change the title bar text
					dlg.setText("Chọn thư mục lưu");

					// Customizable message displayed in the dialog
					dlg.setMessage("Chọn thư mục");

					String dir = dlg.open();
					if (dir != null) {
						savefolder = dir;
					}
				} catch (Exception excep) {
				}
			}
		});

		// ============================================================================================================
		// Thực hiện cắt ký tự và resize ảnh về kích thước 12x28
		btnGet.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					// Load ảnh vào danh sách ảnh
					String DATABASE = loadfolder;
					File[] files = new File(DATABASE).listFiles();
					for (int j = 0; j < files.length; j++) {
						Mat image = Imgcodecs.imread(files[j].getAbsolutePath());
						danhsachanh.add(image);
					}

					try {
						int count = 0;
						for (Mat im : danhsachanh) {
							// Định vị biển số, cắt ký tự
							Mat im_gray = new Mat();
							Imgproc.cvtColor(im, im_gray, Imgproc.COLOR_BGR2GRAY);
							Mat noise_removal = new Mat();
							// Remove noise giảm noise và tăng edge(làm egde thêm sắc nhọn edges sharp)
							Imgproc.bilateralFilter(im_gray, noise_removal, 9, 75, 75);

							// Làm mờ ảnh
							// Imgproc.GaussianBlur(im_gray, im_gray, new Size(9, 9), 2);

							Mat equal_histogram = new Mat();
							// Cân bằng lại histogram làm cho ảnh không quá sáng hoặc tối
							Imgproc.equalizeHist(noise_removal, equal_histogram);

							// Morphogoly open mục đích là giảm egde nhiễu , egde thật thêm sắc nhọn bằng
							// cv2.morphologyEx sử dụng kerel 5x5
							Mat kernel = new Mat();
							Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
							Mat morph_image = new Mat();
							Imgproc.morphologyEx(equal_histogram, morph_image, Imgproc.MORPH_OPEN, kernel);

							// xóa phông (background) không cần thiết
							Mat sub_morp_image = new Mat();
							// Core.subtract(equal_histogram, morph_image, sub_morp_image);
							sub_morp_image = morph_image.clone();

							Mat thresh_image = new Mat(); // anh nhi phan (trang den)
							Imgproc.adaptiveThreshold(sub_morp_image, thresh_image, 255,
									Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 35, 5);
							Mat canny_image = new Mat(); // tim bien anh
							Imgproc.Canny(thresh_image, canny_image, 250, 255);
							kernel = Mat.ones(new Size(2, 2), CvType.CV_32F);
							// kernel = Mat.ones(new Size(3, 3), CvType.CV_8UC1);
							// dilate để tăng sharp cho egde
							Mat dilated_image = new Mat();
							Imgproc.dilate(canny_image, dilated_image, kernel);

							// ***********************************************************************************************************
							List<MatOfPoint> contours = new ArrayList<>();
							Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_LIST,
									Imgproc.CHAIN_APPROX_NONE);
							// Imgproc.findContours(dilated_image, contours, new Mat(), Imgproc.RETR_TREE,
							// Imgproc.CHAIN_APPROX_SIMPLE);

							// vẽ hình chữ nhật quanh biển số
							// Biển số xe máy chiều cao 140 mm, chiều dài 190 mm
							/*
							 * kiểm tra xem đâu là biển số trong danh sách các đường biên có được nếu là
							 * biển số thì phải chứa ít nhất 7 khung hình chữ nhật chứa các ký tự
							 */
							int dem = 0;// Đếm số ký tự trong biển số
							int vitribienso = -1; // vị trí biển số trong contours
							List<Integer> listchar = new ArrayList<>();
							for (int i = 0; i < contours.size(); i++) {
								Rect r = Imgproc.boundingRect(contours.get(i));
								// vẽ hình chữ nhật quanh đối tượng contours
								//Imgproc.rectangle(im, r, new Scalar(0, 255, 0), 1, 8, 0);
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
																		&& (rcheck.y + rcheck.height
																				/ 2) < (rcheckduplicate.y
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
														//Imgproc.rectangle(im, rcheck, new Scalar(255, 0, 0), 1, 8, 0);
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
							// xóa đi ký tự bị sai, dư ra, có phần bị trùng
							int vitriy = -1;
							for (int i = 0; i < listchar.size(); i++) {
								Rect rcheckngoaile = Imgproc.boundingRect(contours.get(listchar.get(i)));
								for (int j = 0; j < listchar.size(); j++) {
									Rect rcheckngoaile2 = Imgproc.boundingRect(contours.get(listchar.get(j)));
									if (listchar.get(i) != listchar.get(j)) {
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
								// listchar.remove(vitriy);
							} catch (ArrayIndexOutOfBoundsException ae) {
								// Không có ký tự cần xóa
								ae.printStackTrace();
							}

							// Cắt lấy các ký tự trong biển số
							List<Mat> listcharimage = new ArrayList<Mat>();
							for (int vitri = 0; vitri < listchar.size(); vitri++) {
								Rect rkytu = Imgproc.boundingRect(contours.get(listchar.get(vitri)));
								listcharimage.add(new Mat(im, rkytu));
							}
							for (Mat imagechar : listcharimage) {
								try {
									// dữ liệu test phải là ảnh 12*28 -----------------------------------------
									//Mat imagetest = new Mat();
									//Imgproc.resize(imagechar, imagetest, new Size(12, 28));
									// lưu ảnh
									//Imgcodecs.imwrite(savefolder + "/image" + count + ".jpg", imagetest);
									Imgcodecs.imwrite(savefolder + "/image" + count + ".jpg", imagechar);
									count++;
								} catch (Exception ee) {
									ee.printStackTrace();
								}
							}
						}
					} catch (Exception exc) {
						exc.printStackTrace();
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
	}
}

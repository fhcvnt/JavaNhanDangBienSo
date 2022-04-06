package nhandangbienso;

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
import org.eclipse.swt.graphics.GC;
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
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.SVM;

public class CuaSoChinh {

	protected Shell shellBienso;
	private String filename = "";
	private Text textBienso;
	private int width = 880; // ảnh 22*40
	private String textbienso = ""; // biển số
	private Text textFilename;
	// Danh sách ký tự trong biển số xe
	private String[] danhsachkytu = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F",
			"G", "H", "K", "L", "M", "N", "P", "S", "T", "U", "V", "X", "Y", "Z" };
	// Nhận dạng ký tự
	SVM svm = SVM.load("D:/Bienso/SVM/trainningsvm.xml");

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
		shellBienso.setSize(833, 675);
		shellBienso.setText("Nhận dạng biển số");
		shellBienso.setLayout(new FillLayout(SWT.HORIZONTAL));

		Composite composite = new Composite(shellBienso, SWT.NONE);

		CLabel lbImagedata = new CLabel(composite, SWT.NONE);
		lbImagedata.setAlignment(SWT.CENTER);
		lbImagedata.setBackground(SWTResourceManager.getColor(SWT.COLOR_GRAY));
		lbImagedata.setBounds(10, 124, 800, 500);
		lbImagedata.setText("Image Data");

		Button btnLoadImage = new Button(composite, SWT.NONE);
		btnLoadImage.setBounds(368, 49, 85, 30);
		btnLoadImage.setText("Load Image");

		Button btnGetlicenseplate = new Button(composite, SWT.NONE);
		btnGetlicenseplate.setBounds(472, 49, 130, 30);
		btnGetlicenseplate.setText("Get license plate");

		textBienso = new Text(composite, SWT.BORDER | SWT.CENTER | SWT.MULTI);
		textBienso.setEditable(false);
		textBienso.setForeground(SWTResourceManager.getColor(255, 255, 255));
		textBienso.setFont(SWTResourceManager.getFont("Times New Roman", 27, SWT.BOLD));
		textBienso.setBackground(SWTResourceManager.getColor(SWT.COLOR_GRAY));
		textBienso.setBounds(621, 10, 180, 97);

		textFilename = new Text(composite, SWT.BORDER);
		textFilename.setBounds(10, 47, 350, 30);

		// ===================================================================================
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
						if (image.getBounds().height * 800 / image.getBounds().width > 500) {
							lbImagedata.setBackground(resizeImage(image, image.getBounds().width*500/image.getBounds().height, 500));
						} else {
							lbImagedata.setBackground(resizeImage(image, 800, image.getBounds().height*800/image.getBounds().width));
						}
						lbImagedata.setText("");
					} catch (Exception ex) {
						ex.printStackTrace();
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
					Mat im = Imgcodecs.imread(filename); // ảnh gốc
					List<Mat> listcharimage = new ArrayList<Mat>();
					TachKyTu kytu = new TachKyTu();
					listcharimage = kytu.getCharImage(im);
					int sokytutren = kytu.getCountchartop();
					int sokytuduoi = kytu.getCountcharbottom();

					int sokytu = 0;
					textbienso="";
					for (Mat imagechar : listcharimage) {
						try {
							// dữ liệu test phải là ảnh 22*40 -----------------------------------------
							Mat imagetest = new Mat();
							Imgproc.resize(imagechar, imagetest, new Size(22, 40));
							// lưu ảnh ******************
							Imgcodecs.imwrite("D:/Bienso/Save/img" + sokytu + ".jpg", imagetest);
							Imgproc.cvtColor(imagetest, imagetest, Imgproc.COLOR_BGR2GRAY);
							Mat testtranningdata = new Mat(1, width, CvType.CV_32FC1);
							double[] datatestmat = new double[width];
							int vitritest = 0;
							// ảnh nhị phân
							Imgproc.adaptiveThreshold(imagetest, imagetest, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
									Imgproc.THRESH_BINARY, 35, 5);
							for (int k = 0; k < imagetest.size().height; k++) {
								for (int m = 0; m < imagetest.size().width; m++) {
									double[] data = imagetest.get(k, m);
									datatestmat[vitritest] = data[0];
									vitritest++;
								}
							}
							testtranningdata.put(0, 0, datatestmat);

							float number = svm.predict(testtranningdata);
							if (sokytu == sokytutren) {
								textbienso = textbienso + "\n";
							}

							if (sokytutren > 3) {
								if (sokytu == 2) {
									textbienso = textbienso + "-"; // chèn thêm ký tự "-"
								}
							}
							if (sokytuduoi > 4) {
								if (sokytu == sokytutren + 3) {
									textbienso = textbienso + "."; // chèn thêm ký tự "."
								}
							}
							textbienso = textbienso + danhsachkytu[(int) number];
						} catch (Exception exc) {
							exc.printStackTrace();
						}
						sokytu++;
					}
					textBienso.setText(textbienso);
					textBienso.update();

				} catch (Exception ex) {
					ex.printStackTrace();
				}
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

	// ****************************************************************************************************************
	// Resize image SWT
	public static Image resizeImage(Image image, int width, int height) {
		Image scaled = new Image(Display.getDefault(), width, height);
		GC gc = new GC(scaled);
		gc.setAntialias(SWT.ON);
		gc.setInterpolation(SWT.HIGH);
		gc.drawImage(image, 0, 0, image.getBounds().width, image.getBounds().height, 0, 0, width, height);
		gc.dispose();
		image.dispose(); // don't forget about me!
		return scaled;
	}
}

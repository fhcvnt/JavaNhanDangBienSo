package biensotest;

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

public class MainWindowBienso {

	protected Shell shell;
	private String filename = "";

	public static void main(String[] args) {
		System.load("C:\\Opencv\\build\\java\\x64\\opencv_java430.dll");
		try {
			MainWindowBienso window = new MainWindowBienso();
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
		lbBienso.setAlignment(SWT.CENTER);
		lbBienso.setBackground(SWTResourceManager.getColor(SWT.COLOR_CYAN));
		lbBienso.setBounds(368, 131, 155, 69);
		lbBienso.setText("Bienso");

		Button getBienSo = new Button(composite, SWT.NONE);
		getBienSo.setBounds(185, 268, 117, 52);
		getBienSo.setText("Get Bien So");

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
		getBienSo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Mat src1 = Imgcodecs.imread(filename); // anh goc
				Mat src2 = src1.clone();
				Mat gray = new Mat();
				Imgproc.cvtColor(src1, gray, Imgproc.COLOR_RGB2GRAY);
				
				Mat binary = new Mat();
				Imgproc.threshold(gray, binary, 100, 255, Imgproc.THRESH_BINARY);

				Mat morpho = new Mat();
				Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(3, 3), new Point(1, 1));
				Imgproc.erode(binary, morpho, element, new Point(-1, -1), 3);
				HighGui.imshow("Anh goc", src1);
				
				List<MatOfPoint> contours = new ArrayList<>();
				Imgproc.findContours(morpho, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
				for (int i = 0; i < contours.size(); i++) {
					Rect r = Imgproc.boundingRect(contours.get(i));
					if (r.width / (double) r.height > 1.1f && r.width / (double) r.height < 1.5f)
						Imgproc.rectangle(src2, r, new Scalar(0, 0, 255), 2, 8, 0);
					else
						Imgproc.rectangle(src2, r, new Scalar(0, 255, 0), 1, 8, 0);
				}
				HighGui.imshow("Ket qua phat hien sau khi phep gian no", src2);
				HighGui.waitKey(0);
			}
		});
	}
}

package nhandangbienso;

import java.io.File;
import java.util.ArrayList;

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
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class ResizeImage {

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
			ResizeImage window = new ResizeImage();
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
							try {
								// dữ liệu test phải là ảnh 12*28 -----------------------------------------
								Mat imagetest = new Mat();
								Imgproc.resize(im, imagetest, new Size(12, 28));
								// lưu ảnh
								Imgcodecs.imwrite(savefolder + "/image" + count + ".jpg", imagetest);
								count++;
							} catch (Exception ee) {
								ee.printStackTrace();
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

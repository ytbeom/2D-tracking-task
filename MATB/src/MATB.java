import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class MATB extends JFrame {
	private static final long serialVersionUID = 1L;

	private int screenHeight;
	private int screenWidth;
	private int gridSize;

	private String participantName;
	
	private int targetRadius;
	
	private Image img;
	private Graphics img_g;
	
	private int delta;
	private int xInput;
	private int yInput;
	private int movementScale;
	
	private int gridCenterX;
	private int gridCenterY;
	private int targetCenterX;
	private int targetCenterY;
	private boolean isOut;
	
	private TimerThread timerThread;
	private long timerStartTime;
	private BufferedWriter bufferedWriter;
	
	private Color MATBBackgroundColor = new Color(245, 245, 245);
	private Color MATBBlueColor = new Color(69, 118, 187);
	private Color MATBOrangeColor = new Color(248, 149, 29);
	
	private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	public MATB(String inputFileName) {
		super("Life Enhancing Technology Lab. - Multi-Attribute Task Battery");
		
		this.setUndecorated(true);
		this.setExtendedState(JFrame.MAXIMIZED_BOTH);
		this.setVisible(true);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.addKeyListener(new MainKeyListener());
		this.setFocusable(true);
		
		SettingDialog dialog = new SettingDialog(this);
		dialog.setVisible(true);
		
		screenHeight = super.getHeight();
		screenWidth = super.getWidth();
		gridSize = (int)(0.8*screenHeight);
		targetRadius = (int)(0.1*gridSize);
		gridCenterX = screenWidth/2;
		gridCenterY = screenHeight/2;
		targetCenterX = gridCenterX;
		targetCenterY = gridCenterY;
		isOut = false;

		try {
			File inputFile = new File(inputFileName);
			BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile));
			
			String outputFileName = participantName+".csv";
			File outputFile = new File(outputFileName);
			if(outputFile.exists() == false) 
				outputFile.createNewFile();
			bufferedWriter = new BufferedWriter(new FileWriter(outputFile, true));
			
			// Column Header 저장
			String line = bufferedReader.readLine();
			String columnHeaderArray[] = line.split(",");
			bufferedWriter.write("Experiment start time" + ",");
			for (int i=0; i<columnHeaderArray.length; i++)
				bufferedWriter.write(columnHeaderArray[i] + ",");
			bufferedWriter.newLine();
			bufferedWriter.flush();
			
			// Parameter Value 저장
			line = bufferedReader.readLine();
			String array[] = line.split(",");
			bufferedWriter.write(format.format(System.currentTimeMillis()) + ",");
			for (int i=0; i<array.length; i++)
				bufferedWriter.write(array[i] + ",");
			bufferedWriter.newLine();
			bufferedWriter.flush();
			
			// Parameter 세팅
			delta = 100;
			movementScale = 10;
			
			// 결과 column Header 저장
			bufferedWriter.newLine();
			bufferedWriter.write("t" + ",");
			bufferedWriter.write("y(t) / x(t)" + ",");
			bufferedWriter.write("device input" + ",");
			bufferedWriter.write("RMSD" + ",");
			bufferedWriter.write("limit excess ratio");
			bufferedWriter.newLine();
			bufferedWriter.flush();
			
			bufferedReader.close();
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		timerThread = new TimerThread();
		timerThread.setStop(false);
		timerStartTime = System.currentTimeMillis();
		timerThread.start();
	}
	
	class SettingDialog extends JDialog {
		private static final long serialVersionUID = 1L;
		
		private int width = 500;
		private int height = 150;
		private JLabel participantNameLabel = new JLabel("Participant Name: ", JLabel.CENTER);
		private JTextField participantNameTextField = new JTextField(10);
		private JButton okButton = new JButton("OK");
		private URL lineImageURL = SettingDialog.class.getClassLoader().getResource("Line.png");
		private ImageIcon lineImageIcon = new ImageIcon(lineImageURL);
		private Image lineImage = lineImageIcon.getImage().getScaledInstance(width-40, 15, java.awt.Image.SCALE_SMOOTH);
		private JLabel lineImageBox = new JLabel(new ImageIcon(lineImage));
		private URL logoImageURL = SettingDialog.class.getClassLoader().getResource("Logo.png");
		private ImageIcon logoImageIcon = new ImageIcon(logoImageURL);
		private Image logoImage = logoImageIcon.getImage().getScaledInstance(width/2, width/2*logoImageIcon.getIconHeight()/logoImageIcon.getIconWidth(), java.awt.Image.SCALE_SMOOTH);
		private JLabel logoImageBox = new JLabel(new ImageIcon(logoImage));
		
		public SettingDialog(JFrame frame) {
			super(frame, "SuRT Setting Dialog", true);
			setLayout(new FlowLayout());
			setSize(width, height);
			setLocation((MATB.super.getWidth()-width)/2, (MATB.super.getHeight()-height)/2);
			this.setFocusable(true);
			
			add(participantNameLabel, BorderLayout.CENTER);
			add(participantNameTextField);
			add(okButton);
			add(lineImageBox);
			add(logoImageBox);
			
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					SaveDialogResult();
				}
			});
			
			KeyListener enterListener = new KeyListener() {
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == 10)
						SaveDialogResult();
				}
				
				@Override
				public void keyTyped(KeyEvent e) {}

				@Override
				public void keyReleased(KeyEvent e) {}
			};
			
			this.addKeyListener(enterListener);
			participantNameTextField.addKeyListener(enterListener);
		}
		
		public void SaveDialogResult() {
			participantName = participantNameTextField.getText();
			if (participantName.equals(""))
				participantName = "NONAME";
			
			this.setVisible(false);
		}
	}
	
	public void paint(Graphics g) {
		img = createImage(super.getWidth(), super.getHeight());
		img_g = img.getGraphics();
		Graphics2D g2 = (Graphics2D)img_g;
		
		// Draw BackGround
		g2.setColor(Color.BLACK);
		g2.fillRect(0, 0, screenWidth, screenHeight);
		
		// Draw Activated Region
		g2.setColor(MATBBackgroundColor);
		g2.fillRect((screenWidth-screenHeight)/2, 0, screenHeight, screenHeight);
		
		g2.setColor(MATBBlueColor);
		// Draw Grid
		g2.setStroke(new BasicStroke(3));
		g2.drawLine((screenWidth-screenHeight)/2+(int)(0.1*screenHeight), (int)(0.1*screenHeight), (screenWidth-screenHeight)/2+(int)(0.1*screenHeight)+(int)(0.1*gridSize), (int)(0.1*screenHeight));
		g2.drawLine((screenWidth-screenHeight)/2+(int)(0.1*screenHeight)+(int)(0.45*gridSize), (int)(0.1*screenHeight), (screenWidth-screenHeight)/2+(int)(0.1*screenHeight)+(int)(0.55*gridSize), (int)(0.1*screenHeight));
		g2.drawLine(screenWidth-(screenWidth-screenHeight)/2-(int)(0.1*screenHeight), (int)(0.1*screenHeight), screenWidth-(screenWidth-screenHeight)/2-(int)(0.1*screenHeight)-(int)(0.1*gridSize), (int)(0.1*screenHeight));
		
		g2.drawLine((screenWidth-screenHeight)/2+(int)(0.1*screenHeight), screenHeight-(int)(0.1*screenHeight), (screenWidth-screenHeight)/2+(int)(0.1*screenHeight)+(int)(0.1*gridSize), screenHeight-(int)(0.1*screenHeight));
		g2.drawLine((screenWidth-screenHeight)/2+(int)(0.1*screenHeight)+(int)(0.45*gridSize), screenHeight-(int)(0.1*screenHeight), (screenWidth-screenHeight)/2+(int)(0.1*screenHeight)+(int)(0.55*gridSize), screenHeight-(int)(0.1*screenHeight));
		g2.drawLine(screenWidth-(screenWidth-screenHeight)/2-(int)(0.1*screenHeight), screenHeight-(int)(0.1*screenHeight), screenWidth-(screenWidth-screenHeight)/2-(int)(0.1*screenHeight)-(int)(0.1*gridSize), screenHeight-(int)(0.1*screenHeight));
		
		g2.drawLine((screenWidth-screenHeight)/2+(int)(0.1*screenHeight), (int)(0.1*screenHeight), (screenWidth-screenHeight)/2+(int)(0.1*screenHeight), (int)(0.1*screenHeight)+(int)(0.1*gridSize));
		g2.drawLine((screenWidth-screenHeight)/2+(int)(0.1*screenHeight), (int)(0.1*screenHeight)+(int)(0.45*gridSize), (screenWidth-screenHeight)/2+(int)(0.1*screenHeight), (int)(0.1*screenHeight)+(int)(0.55*gridSize));
		g2.drawLine((screenWidth-screenHeight)/2+(int)(0.1*screenHeight), screenHeight-(int)(0.1*screenHeight), (screenWidth-screenHeight)/2+(int)(0.1*screenHeight), screenHeight-(int)(0.1*screenHeight)-(int)(0.1*gridSize));
		
		g2.drawLine(screenWidth-(screenWidth-screenHeight)/2-(int)(0.1*screenHeight), (int)(0.1*screenHeight), screenWidth-(screenWidth-screenHeight)/2-(int)(0.1*screenHeight), (int)(0.1*screenHeight)+(int)(0.1*gridSize));
		g2.drawLine(screenWidth-(screenWidth-screenHeight)/2-(int)(0.1*screenHeight), (int)(0.1*screenHeight)+(int)(0.45*gridSize), screenWidth-(screenWidth-screenHeight)/2-(int)(0.1*screenHeight), (int)(0.1*screenHeight)+(int)(0.55*gridSize));
		g2.drawLine(screenWidth-(screenWidth-screenHeight)/2-(int)(0.1*screenHeight), screenHeight-(int)(0.1*screenHeight), screenWidth-(screenWidth-screenHeight)/2-(int)(0.1*screenHeight), screenHeight-(int)(0.1*screenHeight)-(int)(0.1*gridSize));
		
		g2.drawLine((screenWidth-screenHeight)/2+(int)(0.1*screenHeight)+(int)(0.5*gridSize), (int)(0.1*screenHeight), (screenWidth-screenHeight)/2+(int)(0.1*screenHeight)+(int)(0.5*gridSize), screenHeight-(int)(0.1*screenHeight));
		g2.drawLine((screenWidth-screenHeight)/2+(int)(0.1*screenHeight), (int)(0.1*screenHeight)+(int)(0.5*gridSize), screenWidth-(screenWidth-screenHeight)/2-(int)(0.1*screenHeight), (int)(0.1*screenHeight)+(int)(0.5*gridSize));
		
		g2.drawLine((screenWidth-screenHeight)/2+(int)(0.1*screenHeight)+(int)(0.475*gridSize), (int)(0.1*screenHeight)+(int)(0.125*gridSize), (screenWidth-screenHeight)/2+(int)(0.1*screenHeight)+(int)(0.525*gridSize), (int)(0.1*screenHeight)+(int)(0.125*gridSize));
		g2.drawLine((screenWidth-screenHeight)/2+(int)(0.1*screenHeight)+(int)(0.45*gridSize), (int)(0.1*screenHeight)+(int)(0.25*gridSize), (screenWidth-screenHeight)/2+(int)(0.1*screenHeight)+(int)(0.55*gridSize), (int)(0.1*screenHeight)+(int)(0.25*gridSize));
		g2.drawLine((screenWidth-screenHeight)/2+(int)(0.1*screenHeight)+(int)(0.45*gridSize), (int)(0.1*screenHeight)+(int)(0.75*gridSize), (screenWidth-screenHeight)/2+(int)(0.1*screenHeight)+(int)(0.55*gridSize), (int)(0.1*screenHeight)+(int)(0.75*gridSize));
		g2.drawLine((screenWidth-screenHeight)/2+(int)(0.1*screenHeight)+(int)(0.475*gridSize), (int)(0.1*screenHeight)+(int)(0.875*gridSize), (screenWidth-screenHeight)/2+(int)(0.1*screenHeight)+(int)(0.525*gridSize), (int)(0.1*screenHeight)+(int)(0.875*gridSize));
		
		g2.drawLine((screenWidth-screenHeight)/2+(int)(0.1*screenHeight)+(int)(0.125*gridSize), (int)(0.1*screenHeight)+(int)(0.475*gridSize), (screenWidth-screenHeight)/2+(int)(0.1*screenHeight)+(int)(0.125*gridSize), (int)(0.1*screenHeight)+(int)(0.525*gridSize));
		g2.drawLine((screenWidth-screenHeight)/2+(int)(0.1*screenHeight)+(int)(0.25*gridSize), (int)(0.1*screenHeight)+(int)(0.45*gridSize), (screenWidth-screenHeight)/2+(int)(0.1*screenHeight)+(int)(0.25*gridSize), (int)(0.1*screenHeight)+(int)(0.55*gridSize));
		g2.drawLine((screenWidth-screenHeight)/2+(int)(0.1*screenHeight)+(int)(0.75*gridSize), (int)(0.1*screenHeight)+(int)(0.45*gridSize), (screenWidth-screenHeight)/2+(int)(0.1*screenHeight)+(int)(0.75*gridSize), (int)(0.1*screenHeight)+(int)(0.55*gridSize));
		g2.drawLine((screenWidth-screenHeight)/2+(int)(0.1*screenHeight)+(int)(0.875*gridSize), (int)(0.1*screenHeight)+(int)(0.475*gridSize), (screenWidth-screenHeight)/2+(int)(0.1*screenHeight)+(int)(0.875*gridSize), (int)(0.1*screenHeight)+(int)(0.525*gridSize));
		
		g2.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, new float[]{20, 20}, 0));
		g2.drawRect((screenWidth-screenHeight)/2+(int)(0.1*screenHeight)+(int)(0.375*gridSize), (int)(0.1*screenHeight)+(int)(0.375*gridSize), (int)(0.25*gridSize), (int)(0.25*gridSize));
		
		// Draw Target
		g2.setStroke(new BasicStroke(10));
		g2.fillOval(targetCenterX-(int)(0.2*targetRadius), targetCenterY-(int)(0.2*targetRadius), 2*(int)(0.2*targetRadius), 2*(int)(0.2*targetRadius));
		if (isOut)
			g2.setColor(MATBOrangeColor);
		g2.drawOval(targetCenterX-targetRadius, targetCenterY-targetRadius, 2*targetRadius, 2*targetRadius);
		g2.drawLine(targetCenterX, targetCenterY-targetRadius, targetCenterX, targetCenterY-targetRadius/2);
		g2.drawLine(targetCenterX-targetRadius, targetCenterY, targetCenterX-targetRadius/2, targetCenterY);
		g2.drawLine(targetCenterX, targetCenterY+targetRadius, targetCenterX, targetCenterY+targetRadius/2);
		g2.drawLine(targetCenterX+targetRadius, targetCenterY, targetCenterX+targetRadius/2, targetCenterY);
		
		g.drawImage(img, 0, 0, null);
		
		repaint();
	}
	
	class MainKeyListener implements KeyListener {
		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == 38) {
				yInput = -1;
			}
			else if (e.getKeyCode() == 40) {
				yInput = 1;
			}
			else if (e.getKeyCode() == 39) {
				xInput = 1;
			}
			else if (e.getKeyCode() == 37) {
				xInput = -1;
			}
			else if (e.getKeyCode() == 27) {
				timerThread.setStop(true);
				try {
					bufferedWriter.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				System.exit(0);
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
			if (e.getKeyCode() == 38 || e.getKeyCode() == 40) {
				yInput = 0;
			}
			else if (e.getKeyCode() == 39 || e.getKeyCode() == 37) {
				xInput = 0;
			}
		}

		@Override
		public void keyTyped(KeyEvent e) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	class TimerThread extends Thread {
		private boolean stop;
		Random random = new Random();
		
		public void setStop(boolean stop) {
			this.stop = stop;
		}
		
		public void run() {
			while (!stop) {
				long timerTime = System.currentTimeMillis();
				if (timerTime - timerStartTime >= delta) {
					boolean tempIsOutFlag = false;
					int targetXNextStatus = random.nextInt(3) - 1;
					int targetYNextStatus = random.nextInt(3) - 1;
					targetCenterX = targetCenterX + (targetXNextStatus + xInput) * movementScale;
					targetCenterY = targetCenterY + (targetYNextStatus + yInput) * movementScale;
					
					if (targetCenterX <= (screenWidth-screenHeight)/2+(int)(0.1*screenHeight)) {
						tempIsOutFlag = true;
						targetCenterX = (screenWidth-screenHeight)/2+(int)(0.1*screenHeight);
					}
					else if (targetCenterX >= screenWidth-(screenWidth-screenHeight)/2-(int)(0.1*screenHeight)) {
						tempIsOutFlag = true;
						targetCenterX = screenWidth-(screenWidth-screenHeight)/2-(int)(0.1*screenHeight);
					}
					if (targetCenterY <= (int)(0.1*screenHeight)) {
						tempIsOutFlag = true;
						targetCenterY = (int)(0.1*screenHeight);
					}
					else if (targetCenterY >= screenHeight-(int)(0.1*screenHeight)) {
						tempIsOutFlag = true;
						targetCenterY = screenHeight-(int)(0.1*screenHeight);
					}
					
					isOut = tempIsOutFlag;
					
					timerStartTime = System.currentTimeMillis();
				}
				
			}
		}
	}

	class ControllerListenerThread extends Thread {
		private boolean stop;
		
		public void setStop(boolean stop) {
			this.stop = stop;
		}
		
		public void run() {
			while (!stop) {
				
			}
		}
	}
	
	public static void main(String[] args) {
		@SuppressWarnings("unused")
		MATB matb= new MATB("MATBsetting.csv");
	}
}

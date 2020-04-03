import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import net.java.games.input.Controller;
import net.java.games.input.Component.Identifier;
import net.java.games.input.ControllerEnvironment;

public class MATB extends JFrame {
	private static final long serialVersionUID = 1L;

	private int screenHeight;
	private int screenWidth;
	

	private String participantName;

	private Image img;
	private Graphics img_g;
	
	private int displaySize;
	private float sensitivity;
	private int delta;
	private int movementScale;
    private int gridSize;
    private int targetRadius;
	private float gridLineWidth;
	private float targetLineWidth;
	
	private boolean isControllerUsed = false;
    private Controller targetController;
    private Identifier targetComponentLeftIdentifier;
    private Identifier targetComponentRightIdentifier;
    private Identifier targetComponentUpIdentifier;
    private Identifier targetComponentDownIdentifier;
	private float leftLowerBound;
    private float leftUpperBound;
    private float rightLowerBound;
    private float rightUpperBound;
	private float upLowerBound;
    private float upUpperBound;
    private float downLowerBound;
    private float downUpperBound;
    private ControllerListenerThread controllerListenerThread;
    
	private int xInput;
	private int yInput;

	private int gridCenterX;
	private int gridCenterY;
	private int targetCenterX;
	private int targetCenterY;
	private int isOut;
	
	private TimerThread timerThread;
	private long timerStartTime;
	private BufferedWriter bufferedWriter;
	
	private int count;
    private double meanSquaredDeviation;
    
    private long experimentTime;
    private long experimentStartTime;
    private long pauseStartTime;
    private long pausedTime;
	
	private Color MATBBackgroundColor = new Color(240, 240, 240);
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
		isOut = 0;

		// 0: display size
		// 1: sensitivity
		// 2: delta t
		// 3: randomness (movement scale)
		// 4: center square ratio
		// 5: target radius ratio
		// 6: grid line width
		// 7: target line width
		// 8, 9, 10: left input component(index, lower, upper)
		// 11, 12, 13: right input component(index, lower, upper)
		// 14, 15, 16: up input component(index, lower, upper)
		// 17, 18, 19: down input component(index, lower, upper)

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
		    displaySize = Integer.parseInt(array[0]);
		    sensitivity = Float.parseFloat(array[1]);
		    delta = Integer.parseInt(array[2]);
		    movementScale = Integer.parseInt(array[3]);
		    gridSize = (int)(displaySize * Float.parseFloat(array[4]));
		    targetRadius = (int)(displaySize * Float.parseFloat(array[5]));
		    gridLineWidth = Float.parseFloat(array[6]);
		    targetLineWidth = Float.parseFloat(array[7]);
		    
		    if (isControllerUsed) {
                targetComponentLeftIdentifier = targetController.getComponents()[Integer.parseInt(array[8])].getIdentifier();
                leftLowerBound = Float.parseFloat(array[9]);
                leftUpperBound = Float.parseFloat(array[10]);
                targetComponentRightIdentifier = targetController.getComponents()[Integer.parseInt(array[11])].getIdentifier();
                rightLowerBound = Float.parseFloat(array[12]);
                rightUpperBound = Float.parseFloat(array[13]);
                targetComponentUpIdentifier = targetController.getComponents()[Integer.parseInt(array[14])].getIdentifier();
                upLowerBound = Float.parseFloat(array[15]);
                upUpperBound = Float.parseFloat(array[16]);
                targetComponentDownIdentifier = targetController.getComponents()[Integer.parseInt(array[17])].getIdentifier();
                downLowerBound = Float.parseFloat(array[18]);
                downUpperBound = Float.parseFloat(array[19]);
            }
		    
		    gridCenterX = screenWidth/2;
            gridCenterY = screenHeight/2;
            targetCenterX = gridCenterX;
            targetCenterY = gridCenterY;
		  
			// 결과 column Header 저장
			bufferedWriter.newLine();
			bufferedWriter.write("t" + ",");
			bufferedWriter.write("x(t)" + ",");
			bufferedWriter.write("y(t)" + ",");
			bufferedWriter.write("device input(x)" + ",");
			bufferedWriter.write("device input(y)" + ",");
			bufferedWriter.write("RMSD" + ",");
			bufferedWriter.write("boundary contact");
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
		experimentStartTime = System.currentTimeMillis();
		
		xInput = 0;
		yInput = 0;
        count = 0;
        meanSquaredDeviation = 0;
        pausedTime = 0;
        
        if (isControllerUsed) {
            controllerListenerThread = new ControllerListenerThread();
            controllerListenerThread.setStop(false);
            controllerListenerThread.start();
        }
	}
	
	class SettingDialog extends JDialog {
		private static final long serialVersionUID = 1L;
		
		private int width = 600;
        private int height = 230;
		
        private JPanel firstRowPanel = new JPanel();
        private JLabel participantNameLabel = new JLabel("Participant Name: ", JLabel.LEFT);
        private JTextField participantNameTextField = new JTextField();
        private JLabel experimentTimeLabel = new JLabel("Experiment Time: ", JLabel.LEFT);
        private JTextField experimentTimeTextField = new JTextField();
        
        private JPanel secondRowPanel = new JPanel();
        private JLabel controllerInputLabel = new JLabel("Controller Input", JLabel.LEFT);
        private JCheckBox controllerCheckBox = new JCheckBox("", false);
        private JLabel emptyLabel = new JLabel("", JLabel.LEFT);
        
        private JPanel thirdRowPanel = new JPanel();
        private JComboBox<String> controllerCombo;
        private Controller[] controllers = {};
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
			super(frame, "MATB Setting Dialog", true);
			setLayout(new FlowLayout());
			setSize(width, height);
			setLocation((MATB.super.getWidth()-width)/2, (MATB.super.getHeight()-height)/2);
			this.setFocusable(true);
			this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			
			firstRowPanel.setLayout(new FlowLayout());
            firstRowPanel.setPreferredSize(new Dimension(600, 30));
            participantNameLabel.setPreferredSize(new Dimension(120, 20));
            firstRowPanel.add(participantNameLabel);
            participantNameTextField.setPreferredSize(new Dimension(160, 20));
            firstRowPanel.add(participantNameTextField);
            experimentTimeLabel.setPreferredSize(new Dimension(120, 20));
            firstRowPanel.add(experimentTimeLabel);
            experimentTimeTextField.setPreferredSize(new Dimension(160, 20));
            firstRowPanel.add(experimentTimeTextField);
            
            secondRowPanel.setLayout(new FlowLayout());
            secondRowPanel.setPreferredSize(new Dimension(600, 30));
            controllerInputLabel.setPreferredSize(new Dimension(120, 20));
            secondRowPanel.add(controllerInputLabel);
            controllerCheckBox.setPreferredSize(new Dimension(20, 20));
            secondRowPanel.add(controllerCheckBox);
            emptyLabel.setPreferredSize(new Dimension(425, 20));
            secondRowPanel.add(emptyLabel);
            
            String[] controllerName = {};
            controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
            controllerName = new String[controllers.length];        
            for (int i = 0; i<controllerName.length; i++) {
                controllerName[i] = controllers[i].getName() + " / " + controllers[i].getType();
            }
            controllerCombo = new JComboBox<String>(controllerName);
            controllerCombo.setEnabled(false);

            thirdRowPanel.setLayout(new FlowLayout());
            thirdRowPanel.setPreferredSize(new Dimension(600, 30));
            controllerCombo.setPreferredSize(new Dimension(510, 20));
            thirdRowPanel.add(controllerCombo);
            okButton.setPreferredSize(new Dimension(60, 20));
            thirdRowPanel.add(okButton);
            
            controllerCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == 1)
                        controllerCombo.setEnabled(true);
                    else
                        controllerCombo.setEnabled(false);
                }
            });
            
            add(firstRowPanel, "North");
            add(secondRowPanel, "North");
            add(thirdRowPanel, "North");
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
            
            if (experimentTimeTextField.getText().equals(""))
                experimentTime = Long.MAX_VALUE;
            else
                experimentTime = Long.parseLong(experimentTimeTextField.getText());
            
            if (controllerCheckBox.isSelected()) {
                isControllerUsed = true;
                targetController = controllers[controllerCombo.getSelectedIndex()];     
            }
            this.setVisible(false);
		}
	}
	
	class PauseDialog extends JDialog {
	    private static final long serialVersionUID = 1L;
        
        private int width = 250;
        private int height = 70;
        
        private JButton continueButton = new JButton("Continue");
        private JButton quitButton = new JButton("Quit");
        
        public PauseDialog(JFrame frame) {
            super(frame, "", true);
            setLayout(new FlowLayout());
            setSize(width, height);
            setLocation((MATB.super.getWidth()-width)/2, (MATB.super.getHeight()-height)/2);
            this.setFocusable(true);
            this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            
            continueButton.setPreferredSize(new Dimension(100, 20));
            quitButton.setPreferredSize(new Dimension(100, 20));
            add(continueButton);
            add(quitButton);
            
            continueButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    pausedTime = System.currentTimeMillis() - pauseStartTime;
                    System.out.println(pausedTime);
                    timerThread = new TimerThread();
                    timerThread.setStop(false);
                    timerStartTime = System.currentTimeMillis();
                    timerThread.start();
                    dispose();
                }
            });
            
            quitButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Quit();
                }
            });
            
            KeyListener escListener = new KeyListener() {
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == 27) {
                        pausedTime += System.currentTimeMillis() - pauseStartTime;
                        System.out.println(pausedTime);
                        timerThread = new TimerThread();
                        timerThread.setStop(false);
                        timerStartTime = System.currentTimeMillis();
                        timerThread.start();
                        setVisible(false);
                    }
                }
                
                @Override
                public void keyTyped(KeyEvent e) {}

                @Override
                public void keyReleased(KeyEvent e) {}
            };
            
            this.addKeyListener(escListener);
        }
	}
	
	class QuitDialog extends JDialog {
	    private static final long serialVersionUID = 1L;
        
        private int width = 250;
        private int height = 70;
        
        private JButton newTrialButton = new JButton("New Trial");
        private JButton quitButton = new JButton("Quit");
        
        public QuitDialog(JFrame frame) {
            super(frame, "", true);
            setLayout(new FlowLayout());
            setSize(width, height);
            setLocation((MATB.super.getWidth()-width)/2, (MATB.super.getHeight()-height)/2);
            this.setFocusable(true);
            this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            
            newTrialButton.setPreferredSize(new Dimension(100, 20));
            quitButton.setPreferredSize(new Dimension(100, 20));
            add(newTrialButton);
            add(quitButton);
            
            newTrialButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dispose();
                    @SuppressWarnings("unused")
                    MATB matb = new MATB("MATBsetting.csv");
                }
            });
            
            quitButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Quit();
                }
            });
        }
	}
	
	public void OpenPauseDialog() {
        PauseDialog pauseDialog = new PauseDialog(this);
        pauseDialog.setVisible(true);
    }
    
    public void OpenQuitDialog() {
        QuitDialog quitDialog = new QuitDialog(this);
        quitDialog.setVisible(true);
    }
	
	// TODO: draw에서 width, size 받아온 값으로 변경, 회색 주변부가 원을 덮을 수 있게 순서 수정
	public void paint(Graphics g) {
		img = createImage(super.getWidth(), super.getHeight());
		img_g = img.getGraphics();
		Graphics2D g2 = (Graphics2D)img_g;
		
		// Draw Display Region
		g2.setColor(MATBBackgroundColor);
		g2.fillRect((screenWidth-displaySize)/2, (screenHeight-displaySize)/2, displaySize, displaySize);
		
		g2.setColor(MATBBlueColor);
		// Draw Grid Axis
		g2.setStroke(new BasicStroke(gridLineWidth));
		g2.drawLine(gridCenterX-displaySize/2, gridCenterY, gridCenterX+displaySize/2, gridCenterY); // x-axis
		g2.drawLine(gridCenterX, gridCenterY-displaySize/2, gridCenterX, gridCenterY+displaySize/2); // y-axis
		
		// Draw Scale Line along the X-axis
		g2.drawLine(gridCenterX-displaySize/2, gridCenterY-(int)(0.05*displaySize), gridCenterX-displaySize/2, gridCenterY+(int)(0.05*displaySize));
		g2.drawLine(gridCenterX-(int)(0.75*displaySize/2), gridCenterY-(int)(0.025*displaySize), gridCenterX-(int)(0.75*displaySize/2), gridCenterY+(int)(0.025*displaySize));
		g2.drawLine(gridCenterX-(int)(0.5*displaySize/2), gridCenterY-(int)(0.05*displaySize), gridCenterX-(int)(0.5*displaySize/2), gridCenterY+(int)(0.05*displaySize));
		g2.drawLine(gridCenterX-(int)(0.25*displaySize/2), gridCenterY-(int)(0.025*displaySize), gridCenterX-(int)(0.25*displaySize/2), gridCenterY+(int)(0.025*displaySize));
		g2.drawLine(gridCenterX+(int)(0.25*displaySize/2), gridCenterY-(int)(0.025*displaySize), gridCenterX+(int)(0.25*displaySize/2), gridCenterY+(int)(0.025*displaySize));
		g2.drawLine(gridCenterX+(int)(0.5*displaySize/2), gridCenterY-(int)(0.05*displaySize), gridCenterX+(int)(0.5*displaySize/2), gridCenterY+(int)(0.05*displaySize));
		g2.drawLine(gridCenterX+(int)(0.75*displaySize/2), gridCenterY-(int)(0.025*displaySize), gridCenterX+(int)(0.75*displaySize/2), gridCenterY+(int)(0.025*displaySize));
		g2.drawLine(gridCenterX+displaySize/2, gridCenterY-(int)(0.05*displaySize), gridCenterX+displaySize/2, gridCenterY+(int)(0.05*displaySize));
		
		// Draw Scale Line along the Y-axis
		g2.drawLine(gridCenterX-(int)(0.05*displaySize), gridCenterY-displaySize/2, gridCenterX+(int)(0.05*displaySize), gridCenterY-displaySize/2);
        g2.drawLine(gridCenterX-(int)(0.025*displaySize), gridCenterY-(int)(0.75*displaySize/2), gridCenterX+(int)(0.025*displaySize), gridCenterY-(int)(0.75*displaySize/2));
        g2.drawLine(gridCenterX-(int)(0.05*displaySize), gridCenterY-(int)(0.5*displaySize/2), gridCenterX+(int)(0.05*displaySize), gridCenterY-(int)(0.5*displaySize/2));
        g2.drawLine(gridCenterX-(int)(0.025*displaySize), gridCenterY-(int)(0.25*displaySize/2), gridCenterX+(int)(0.025*displaySize), gridCenterY-(int)(0.25*displaySize/2));
        g2.drawLine(gridCenterX-(int)(0.025*displaySize), gridCenterY+(int)(0.25*displaySize/2), gridCenterX+(int)(0.025*displaySize), gridCenterY+(int)(0.25*displaySize/2));
        g2.drawLine(gridCenterX-(int)(0.05*displaySize), gridCenterY+(int)(0.5*displaySize/2), gridCenterX+(int)(0.05*displaySize), gridCenterY+(int)(0.5*displaySize/2));
        g2.drawLine(gridCenterX-(int)(0.025*displaySize), gridCenterY+(int)(0.75*displaySize/2), gridCenterX+(int)(0.025*displaySize), gridCenterY+(int)(0.75*displaySize/2));
        g2.drawLine(gridCenterX-(int)(0.05*displaySize), gridCenterY+displaySize/2, gridCenterX+(int)(0.05*displaySize), gridCenterY+displaySize/2);
		
		// Draw Corner Edges
        g2.drawLine(gridCenterX-displaySize/2, gridCenterY-displaySize/2, gridCenterX-displaySize/2, gridCenterY-displaySize/2+(int)(0.05*displaySize));
        g2.drawLine(gridCenterX-displaySize/2, gridCenterY-displaySize/2, gridCenterX-displaySize/2+(int)(0.05*displaySize), gridCenterY-displaySize/2);
        g2.drawLine(gridCenterX+displaySize/2, gridCenterY-displaySize/2, gridCenterX+displaySize/2, gridCenterY-displaySize/2+(int)(0.05*displaySize));
        g2.drawLine(gridCenterX+displaySize/2, gridCenterY-displaySize/2, gridCenterX+displaySize/2-(int)(0.05*displaySize), gridCenterY-displaySize/2);
        g2.drawLine(gridCenterX-displaySize/2, gridCenterY+displaySize/2, gridCenterX-displaySize/2, gridCenterY+displaySize/2-(int)(0.05*displaySize));
        g2.drawLine(gridCenterX-displaySize/2, gridCenterY+displaySize/2, gridCenterX-displaySize/2+(int)(0.05*displaySize), gridCenterY+displaySize/2);
        g2.drawLine(gridCenterX+displaySize/2, gridCenterY+displaySize/2, gridCenterX+displaySize/2, gridCenterY+displaySize/2-(int)(0.05*displaySize));
        g2.drawLine(gridCenterX+displaySize/2, gridCenterY+displaySize/2, gridCenterX+displaySize/2-(int)(0.05*displaySize), gridCenterY+displaySize/2);
        
		// Draw Grid Rect
		g2.setStroke(new BasicStroke(gridLineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, new float[]{5, 5}, 0));
		g2.drawRect(gridCenterX-gridSize/2, gridCenterY-gridSize/2, gridSize, gridSize);
		
		// Draw Target
		g2.setStroke(new BasicStroke(targetLineWidth));
		// target 내부의 작은 원 그리기
		g2.fillOval(targetCenterX-targetRadius/5, targetCenterY-targetRadius/5, 2*targetRadius/5, 2*targetRadius/5);
		if (isOut == 1)
			g2.setColor(MATBOrangeColor);
		g2.drawOval(targetCenterX-targetRadius, targetCenterY-targetRadius, 2*targetRadius, 2*targetRadius);
		g2.drawLine(targetCenterX, targetCenterY-targetRadius, targetCenterX, targetCenterY-targetRadius/2);
		g2.drawLine(targetCenterX-targetRadius, targetCenterY, targetCenterX-targetRadius/2, targetCenterY);
		g2.drawLine(targetCenterX, targetCenterY+targetRadius, targetCenterX, targetCenterY+targetRadius/2);
		g2.drawLine(targetCenterX+targetRadius, targetCenterY, targetCenterX+targetRadius/2, targetCenterY);
		
		// Draw Background
		g2.setColor(MATBBackgroundColor);
        g2.fillRect(0, 0, screenWidth, gridCenterY-displaySize/2);
        g2.fillRect(0, gridCenterY+displaySize/2, screenWidth, screenHeight);
        g2.fillRect(0, gridCenterY-displaySize/2, gridCenterX-displaySize/2, gridCenterY+displaySize/2);
        g2.fillRect(gridCenterX+displaySize/2, gridCenterY-displaySize/2, screenWidth, gridCenterY+displaySize/2);
		
		g.drawImage(img, 0, 0, null);
		
		repaint();
	}
	// TODO: device input 반영
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
                pauseStartTime = System.currentTimeMillis();
                OpenPauseDialog();
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
	
	public void Quit() {
        // Close BufferedWriter
        try {
            bufferedWriter.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        // Stop controllerListener
        if (isControllerUsed)
            controllerListenerThread.setStop(true);
        
        System.exit(0);
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
				    count++;
					int targetXNextStatus = random.nextInt(3) - 1;
					int targetYNextStatus = random.nextInt(3) - 1;
					targetCenterX = targetCenterX + targetXNextStatus * movementScale + (int)(sensitivity * xInput);
					targetCenterY = targetCenterY + targetYNextStatus * movementScale + (int)(sensitivity * yInput);
					
					isOut = 0;
					// TODO: 여기 계산 다시 할 것
					if (targetCenterX-gridCenterX >= displaySize/2) {
					    isOut = 1;
						targetCenterX = screenWidth/2+displaySize/2;
					}
					else if (gridCenterX-targetCenterX >= displaySize/2) {
					    isOut = 1;
						targetCenterX = screenWidth/2-displaySize/2;
					}
					if (targetCenterY-gridCenterY >= displaySize/2) {
					    isOut = 1;
					    targetCenterY = screenHeight/2+displaySize/2;
					}
					else if (gridCenterY-targetCenterY >= displaySize/2) {
					    isOut = 1;
					    targetCenterY = screenHeight/2-displaySize/2;
					}

					double error = Math.pow(targetCenterX - gridCenterX, 2) + Math.pow(targetCenterY - gridCenterY, 2);
                    meanSquaredDeviation = (meanSquaredDeviation*(count-1)+error)/count;
                    String dateString = format.format(timerTime);

                    try {
                        bufferedWriter.write(dateString + ", " + (targetCenterX - gridCenterX) + ", " + (targetCenterY - gridCenterY) + ", "
                                            + xInput + ", " + yInput + ", " + Math.sqrt(meanSquaredDeviation) + ", " + isOut);
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
					timerStartTime = System.currentTimeMillis();
				}
				if (timerTime - experimentStartTime - pausedTime >= experimentTime) {
                    this.setStop(true);
                    OpenQuitDialog();
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
			    try {
                    Thread.sleep(20);
                } catch (Exception e) {}
                targetController.poll();
                
                xInput = 0;
                yInput = 0;
                
                float leftPolledData = targetController.getComponent(targetComponentLeftIdentifier).getPollData();
                float rightPolledData = targetController.getComponent(targetComponentRightIdentifier).getPollData();
                float upPolledData = targetController.getComponent(targetComponentUpIdentifier).getPollData();
                float downPolledData = targetController.getComponent(targetComponentDownIdentifier).getPollData();
                
                if (leftPolledData >= leftLowerBound && leftPolledData <= leftUpperBound)
                    xInput--;
                if (rightPolledData >= rightLowerBound && rightPolledData <= rightUpperBound)
                    xInput++;
                if (upPolledData >= upLowerBound && upPolledData <= upUpperBound)
                    yInput--;
                if (downPolledData >= downLowerBound && downPolledData <= downUpperBound)
                    yInput++;
			}
		}
	}
	
	public static void main(String[] args) {
		@SuppressWarnings("unused")
		MATB matb= new MATB("MATBsetting.csv");
	}
}

package CTT;

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

import net.java.games.input.Component.Identifier;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

public class CTT extends JFrame {
	private static final long serialVersionUID = 1L;

	// mode definition 1: horizontal (original) version / 0: vertical (rotated) version
	private int mode;
	private int screenSize;
	private int[] screenOffset = new int[] {0,0};
	private float centerLineWidth;
	private float targetLineWidth;
	private float referenceLineWidth;
	private int[] centerLinePosition = new int[] {0,0};
	private int[] centerLineLength = new int[] {0,0};
	private int[] targetLineLength = new int[] {0,0};
	private float[] targetLineVelocity = new float[] {0f, 0f};
	private float[] targetLinePosition = new float[] {0f, 0f};
	private int[] referenceLineLength = new int[] {0,0};
	private int[] upperReferenceLinePosition = new int[] {0,0};
	private int[] lowerReferenceLinePosition = new int[] {0,0};
	private float[] dash = new float[] {50, 50};
	
	private float instability;
	private float sensitivity;
	private int delta;
	private int deviceInput;
	
	private String participantName;
	
	private Image img;
	private Graphics img_g;
	
	private boolean isControllerUsed = false;
	private Controller targetController;
	private Identifier targetComponentIdentifier;
	private float upOrLeftLowerBound;
	private float upOrLeftUpperBound;
	private float downOrRightLowerBound;
	private float downOrRightUpperBound;
	private ControllerListenerThread controllerListenerThread;
	
	private TimerThread timerThread;
	private long timerStartTime;
	private BufferedWriter bufferedWriter;
	
	private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	private int count;
	private int outOfBoundCount;
	private double meanSquaredDeviation;
	
	private long experimentTime;
	private long experimentStartTime;
	private long pauseStartTime;
	private long pausedTime;
	
	public CTT(String inputFileName) {
		super("Life Enhancing Technology Lab. - Critical Tracking Task");
		
		this.setUndecorated(true);
		this.setExtendedState(JFrame.MAXIMIZED_BOTH);
		this.setVisible(true);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.addKeyListener(new MyKeyListener());
		this.setFocusable(true);
		
		SettingDialog dialog = new SettingDialog(this);
		dialog.setVisible(true);
		
		int screenWidth = super.getWidth();
		int screenHeight = super.getHeight();
		if (screenWidth >= screenHeight) {
			screenSize = screenHeight;
			screenOffset[0] = (screenWidth-screenSize)/2;
		}
		else {
			screenSize = screenWidth;
			screenOffset[1] = (screenHeight-screenSize)/2;
		}
		centerLinePosition[0] = screenOffset[0];
		centerLinePosition[1] = screenOffset[1];
		upperReferenceLinePosition[0] = screenOffset[0];
		upperReferenceLinePosition[1] = screenOffset[1];
		lowerReferenceLinePosition[0] = screenOffset[0];
		lowerReferenceLinePosition[1] = screenOffset[1];
		
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
			mode = Integer.parseInt(array[0]);
			instability = Float.parseFloat(array[1]);
			sensitivity = Float.parseFloat(array[2]);
			delta = Integer.parseInt(array[3]);
			centerLineWidth = Float.parseFloat(array[4]);
			targetLineWidth = Float.parseFloat(array[5]);
			referenceLineWidth = Float.parseFloat(array[6]);
			centerLinePosition[mode] += screenSize/2;
			centerLineLength[(mode+1)%2] = screenSize;
			targetLineLength[(mode+1)%2] = (int)(screenSize/2*Float.parseFloat(array[7]));
			referenceLineLength[(mode+1)%2] = (int)(screenSize/2*Float.parseFloat(array[8]));
			upperReferenceLinePosition[0] += (int)(screenSize/2*Float.parseFloat(array[9]));
			upperReferenceLinePosition[1] += (int)(screenSize/2*Float.parseFloat(array[10]));
			dash[0] = Float.parseFloat(array[11]);
			dash[1] = Float.parseFloat(array[12]);
			if (mode == 1) {
				lowerReferenceLinePosition[0] += (int)(screenSize/2*Float.parseFloat(array[9]));
				lowerReferenceLinePosition[1] += (screenSize - (int)(screenSize/2*Float.parseFloat(array[10])));
			}
			else {
				lowerReferenceLinePosition[0] += (screenSize - (int)(screenSize/2*Float.parseFloat(array[9])));
				lowerReferenceLinePosition[1] += (int)(screenSize/2*Float.parseFloat(array[10]));
			}
			if (isControllerUsed) {
				targetComponentIdentifier = targetController.getComponents()[Integer.parseInt(array[13])].getIdentifier();
				upOrLeftLowerBound = Float.parseFloat(array[14]);
				upOrLeftUpperBound = Float.parseFloat(array[15]);
				downOrRightLowerBound = Float.parseFloat(array[16]);
				downOrRightUpperBound = Float.parseFloat(array[17]);
			}
			
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

		Random random = new Random();
		int plusminus = (random.nextInt(2) == 0 ? -1 : 1);
		targetLinePosition[mode] = screenOffset[mode] + screenSize/2 + plusminus*screenSize/100;
		targetLinePosition[(mode+1)%2] = screenOffset[(mode+1)%2] + (screenSize - targetLineLength[(mode+1)%2])/2;
		targetLineVelocity[mode] = instability * (targetLinePosition[mode] - centerLinePosition[mode]);
		deviceInput = 0;
		count = 0;
		outOfBoundCount = 0;
		meanSquaredDeviation = 0;
		pausedTime = 0;
		
		timerThread = new TimerThread();
		timerThread.setStop(false);
		timerStartTime = System.currentTimeMillis();
		timerThread.start();
		experimentStartTime = System.currentTimeMillis();
		
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
			super(frame, "CTT Setting Dialog", true);
			setLayout(new FlowLayout());
			setSize(width, height);
			setLocation((CTT.super.getWidth()-width)/2, (CTT.super.getHeight()-height)/2);
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
				controllerName[i] = controllers[i].getName();
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
			setLocation((CTT.super.getWidth()-width)/2, (CTT.super.getHeight()-height)/2);
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
			setLocation((CTT.super.getWidth()-width)/2, (CTT.super.getHeight()-height)/2);
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
					CTT ctt = new CTT("CTTsetting.csv");
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
	
	public void paint(Graphics g) {
		img = createImage(super.getWidth(), super.getHeight());
		img_g = img.getGraphics();
		Graphics2D g2 = (Graphics2D)img_g;
		
		// Draw BackGround
		g2.setColor(Color.BLACK);
		g2.fillRect(0, 0, super.getWidth(), super.getHeight());
		
		// Draw Activated Region
		g2.setColor(Color.WHITE);
		g2.fillRect(screenOffset[0], screenOffset[1], screenSize, screenSize);
		
		// Draw Center Line
		g2.setColor(Color.RED);
		g2.setStroke(new BasicStroke(centerLineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, dash, 0));
		g2.drawLine(centerLinePosition[0], centerLinePosition[1], centerLinePosition[0]+centerLineLength[0], centerLinePosition[1]+centerLineLength[1]);
		
		// Draw Reference Line
		g2.setColor(Color.BLUE);
		g2.setStroke(new BasicStroke(referenceLineWidth));
		g2.drawLine(upperReferenceLinePosition[0], upperReferenceLinePosition[1], upperReferenceLinePosition[0] + referenceLineLength[0], upperReferenceLinePosition[1] + referenceLineLength[1]);
		g2.drawLine(lowerReferenceLinePosition[0], lowerReferenceLinePosition[1], lowerReferenceLinePosition[0] + referenceLineLength[0], lowerReferenceLinePosition[1] + referenceLineLength[1]);
		
		// Draw Target Line
		if (targetLinePosition[mode] > lowerReferenceLinePosition[mode] || targetLinePosition[mode] < upperReferenceLinePosition[mode])
			g2.setColor(Color.RED);
		else
			g2.setColor(Color.BLACK);
		g2.setStroke(new BasicStroke(targetLineWidth));
		g2.drawLine((int)targetLinePosition[0], (int)targetLinePosition[1], (int)targetLinePosition[0] + targetLineLength[0], (int)targetLinePosition[1] + targetLineLength[1]);
		
		g.drawImage(img, 0, 0, null);
		
		repaint();
	}
	
	class MyKeyListener implements KeyListener {
		@Override
		public void keyPressed(KeyEvent e) {
			// up
			if (e.getKeyCode() == 38 && mode == 1) {
				deviceInput = -1;
			}
			// down
			else if (e.getKeyCode() == 40 && mode == 1) {
				deviceInput = 1;
			}
			// right
			else if (e.getKeyCode() == 39 && mode == 0) {
				deviceInput = 1;
			}
			//left
			else if (e.getKeyCode() == 37 && mode == 0) {
				deviceInput = -1;
			}
			else if (e.getKeyCode() == 27) {
				timerThread.setStop(true);
				pauseStartTime = System.currentTimeMillis();
				OpenPauseDialog();
			}
		}
		
		@Override
		public void keyTyped(KeyEvent e) {

		}

		@Override
		public void keyReleased(KeyEvent e) {
			// up & down
			if (mode == 1 && (e.getKeyCode() == 38 || e.getKeyCode() == 40)) {
				deviceInput = 0;
			}
			// right & left
			else if (mode == 0 && (e.getKeyCode() == 39 || e.getKeyCode() == 37)) {
				deviceInput = 0;
			}
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
		
		public void setStop(boolean stop) {
			this.stop = stop;
		}
		
		public void run() {
			while (!stop) {				
				long timerTime = System.currentTimeMillis();
				if (timerTime - timerStartTime >= delta) {
					count++;
					targetLinePosition[mode] += targetLineVelocity[mode] * delta + sensitivity * deviceInput;
					if ((int)targetLinePosition[mode] == 0)
						targetLinePosition[mode] = 0.1f;
					else if (targetLinePosition[mode] <= screenOffset[mode]) {
						targetLinePosition[mode] = screenOffset[mode];
						outOfBoundCount++;
					}
					else if (targetLinePosition[mode] >= screenOffset[mode] + screenSize) {
						targetLinePosition[mode] = screenOffset[mode] + screenSize;
						outOfBoundCount++;
					}
					targetLineVelocity[mode] = instability * (targetLinePosition[mode] - screenSize/2 - screenOffset[mode]);
					
					double error = Math.pow((targetLinePosition[mode] - screenSize/2 - screenOffset[mode]), 2);
					meanSquaredDeviation = (meanSquaredDeviation*(count-1)+error)/count;
					String dateString = format.format(timerTime);
					try {
						bufferedWriter.write(dateString + ", " + (targetLinePosition[mode] - screenSize/2 - screenOffset[mode]) + ", " + deviceInput + ", " + Math.sqrt(meanSquaredDeviation) + ", " + ((float)outOfBoundCount/(float)count));
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
				
				float polledData = targetController.getComponent(targetComponentIdentifier).getPollData();
				if (polledData >= upOrLeftLowerBound && polledData <= upOrLeftUpperBound)
					deviceInput = -1;
				else if (polledData >= downOrRightLowerBound && polledData <= downOrRightUpperBound)
					deviceInput = 1;
				else
					deviceInput = 0;
			}
		}
	}
	
	public static void main(String[] args) {
		@SuppressWarnings("unused")
		CTT ctt = new CTT("CTTsetting.csv");
	}
}
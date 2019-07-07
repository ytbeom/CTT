package CTT;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Random;

import javax.swing.JFrame;

//import net.java.games.input.Component;
//import net.java.games.input.Controller;
//import net.java.games.input.ControllerEnvironment;


public class CTT extends JFrame {
	private static final long serialVersionUID = 1L;

	// mode definition 1: horizontal (original) version / 0: vertical (rotated) version
	private int mode;
	private int screenSize;
	private int[] screenOffset = new int[] {0,0};
	private float lineWidth;
	private int[] centerLinePosition = new int[] {0,0};
	private int[] centerLineLength = new int[] {0,0};
	private int[] targetLineLength = new int[] {0,0};
	private float[] targetLineVelocity = new float[] {0f, 0f};
	private float[] targetLinePosition = new float[] {0f, 0f};
	private int[] referenceLineLength = new int[] {0,0};
	private int[] upperReferenceLinePosition = new int[] {0,0};
	private int[] lowerReferenceLinePosition = new int[] {0,0};
	private float[] dash = new float[] {50};
	
	private float instability;
	private float sensitivity;
	private int delta;
	private int deviceInput;
	
	private Image img;
	private Graphics img_g;
	
	//private Controller targetController;
	//private Component[] components;
	//private ControllerListenerThread controllerListenerThread;
	
	private TimerThread timerThread;
	private long timerStartTime;
	
	private File saveFile;
	private BufferedWriter bufferedWriter;
	
	private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	private int count;
	private int outOfBoundCount;
	private double meanSquaredDeviation;
	
	
	public CTT(String filename) {
		super("Life Enhancing Technology Lab. - Critical Tracking Task");
		
		this.setUndecorated(true);
		this.setExtendedState(JFrame.MAXIMIZED_BOTH);
		this.setVisible(true);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.addKeyListener(new MyKeyListener());
		this.setFocusable(true);
		
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
			File file = new File(filename);
			BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
			String line = "";
			
			if((line = bufferedReader.readLine()) != null) {
				line = bufferedReader.readLine();
				String array[] = line.split(",");
				mode = Integer.parseInt(array[0]);
				instability = Float.parseFloat(array[1]);
				sensitivity = Float.parseFloat(array[2]);
				delta = Integer.parseInt(array[3]);
				lineWidth = Float.parseFloat(array[4]);
				centerLinePosition[mode] += screenSize/2;
				centerLineLength[(mode+1)%2] = screenSize;
				targetLineLength[(mode+1)%2] = (int)(screenSize/2*Float.parseFloat(array[5]));
				referenceLineLength[(mode+1)%2] = (int)(screenSize/2*Float.parseFloat(array[6]));
				upperReferenceLinePosition[0] += (int)(screenSize/2*Float.parseFloat(array[7]));
				upperReferenceLinePosition[1] += (int)(screenSize/2*Float.parseFloat(array[8]));
				if (mode == 1) {
					lowerReferenceLinePosition[0] += (int)(screenSize/2*Float.parseFloat(array[7]));
					lowerReferenceLinePosition[1] += (screenSize - (int)(screenSize/2*Float.parseFloat(array[8])));
				}
				else {
					lowerReferenceLinePosition[0] += (screenSize - (int)(screenSize/2*Float.parseFloat(array[7])));
					lowerReferenceLinePosition[1] += (int)(screenSize/2*Float.parseFloat(array[8]));
				
				}
			}
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
		
		saveFile = new File("SaveFile.csv");
		try {
			if(saveFile.exists() == false) 
				saveFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			bufferedWriter = new BufferedWriter(new FileWriter(saveFile, true));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		timerThread = new TimerThread();
		timerThread.setStop(false);
		timerStartTime = System.currentTimeMillis();
		timerThread.start();

		/*
		Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
		
		for(Controller controller : controllers) {
			if (controller.getType() == Controller.Type.GAMEPAD)
				targetController = controller;
		}
		components = targetController.getComponents();
		
		controllerListenerThread = new ControllerListenerThread();
		*/
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
		g2.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, dash, 0));
		g2.drawLine(centerLinePosition[0], centerLinePosition[1], centerLinePosition[0]+centerLineLength[0], centerLinePosition[1]+centerLineLength[1]);
		
		// Draw Reference Line
		g2.setColor(Color.BLUE);
		g2.setStroke(new BasicStroke(lineWidth));
		g2.drawLine(upperReferenceLinePosition[0], upperReferenceLinePosition[1], upperReferenceLinePosition[0] + referenceLineLength[0], upperReferenceLinePosition[1] + referenceLineLength[1]);
		g2.drawLine(lowerReferenceLinePosition[0], lowerReferenceLinePosition[1], lowerReferenceLinePosition[0] + referenceLineLength[0], lowerReferenceLinePosition[1] + referenceLineLength[1]);
		
		// Draw Target Line
		g2.setColor(Color.BLACK);
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
		public void keyTyped(KeyEvent e) {

		}

		@Override
		public void keyReleased(KeyEvent e) {
			deviceInput = 0;
		}
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
					if (targetLinePosition[mode] <= screenOffset[mode]) {
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
			}
		}
	}
	
	/*
	class ControllerListenerThread extends Thread {
		public void run() {
			while (true) {
				try {
					Thread.sleep(40);
				} catch (Exception e) {}
				targetController.poll();
				for (Component component : components) {
					System.out.print(component.getPollData()+", ");
				}
				System.out.println();
			}
		}
	}
	*/
	
	public static void main(String[] args) {
		@SuppressWarnings("unused")
		CTT ctt = new CTT("CTTsetting.csv");
	}
}
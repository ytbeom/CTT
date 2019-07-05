package CTT;

import java.awt.BasicStroke;
import java.awt.Color;
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
import java.text.SimpleDateFormat;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.Timer;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;


public class CTT extends JFrame {
	private static final long serialVersionUID = 1L;

	private int screenSize;
	private int screenOffset;
	private float lineWidth;
	private int targetLineLength;
	private int referenceLineLength;
	private int referenceLinePosition;
	private float[] dash = new float[] {50};
	
	private float instability;
	private float sensitivity;
	private int delta;
	private float targetLineVelocity;
	private float targetLinePosition;
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
		screenSize = (screenWidth > screenHeight ? screenHeight : screenWidth);
		screenOffset = (screenWidth-screenSize)/2;
		
		try {
			File file = new File(filename);
			BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
			String line = "";

			
			if((line = bufferedReader.readLine()) != null) {
				line = bufferedReader.readLine();
				String array[] = line.split(",");
				instability = Float.parseFloat(array[0]);
				sensitivity = Float.parseFloat(array[1]);
				delta = Integer.parseInt(array[2]);
				lineWidth = Float.parseFloat(array[3]);
				targetLineLength = (int)(screenSize/2*Float.parseFloat(array[4]));
				referenceLineLength = (int)(screenSize/2*Float.parseFloat(array[5]));
				referenceLinePosition = (int)(screenSize/2*Float.parseFloat(array[6]));
			}
			bufferedReader.close();
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		Random random = new Random();
		int plusminus = (random.nextInt(2) == 0 ? -1 : 1);
		targetLinePosition = screenSize/2 + plusminus*screenSize/10;
		targetLineVelocity = instability * (targetLinePosition - screenSize/2);
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
		g2.setColor(Color.BLACK);
		g2.fillRect(0, 0, super.getWidth(), super.getHeight());
		
		g2.setColor(Color.WHITE);
		g2.fillRect(screenOffset, 0, screenSize, screenSize);
		
		g2.setColor(Color.RED);
		g2.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, dash, 0));
		g2.drawLine(screenOffset, screenSize/2, screenOffset + screenSize, screenSize/2);
		
		g2.setColor(Color.BLUE);
		g2.setStroke(new BasicStroke(lineWidth));
		g2.drawLine(screenOffset + referenceLinePosition, screenSize/2 - referenceLinePosition, screenOffset + referenceLinePosition+referenceLineLength, screenSize/2 - referenceLinePosition);
		g2.drawLine(screenOffset + referenceLinePosition, screenSize/2 + referenceLinePosition, screenOffset + referenceLinePosition+referenceLineLength, screenSize/2 + referenceLinePosition);
		
		g2.setColor(Color.BLACK);
		g2.drawLine(screenOffset + (screenSize - targetLineLength)/2, (int)targetLinePosition, screenOffset + (screenSize - targetLineLength)/2 + targetLineLength, (int)targetLinePosition);
		
		g.drawImage(img, 0, 0, null);
		
		repaint();
	}
	
	class MyKeyListener implements KeyListener {
		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == 38) {
				deviceInput = -1;
			}
			else if (e.getKeyCode() == 40) {
				deviceInput = 1;
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
					targetLinePosition = targetLinePosition + targetLineVelocity * delta + sensitivity * deviceInput;
					if (targetLinePosition <= 0) {
						targetLinePosition = 0;
						outOfBoundCount++;
					}
					else if (targetLinePosition >= screenSize) {
						targetLinePosition = screenSize;
						outOfBoundCount++;
					}
					double error = Math.pow((targetLinePosition - screenSize/2), 2);
					meanSquaredDeviation = (meanSquaredDeviation*(count-1)+error)/count;
					targetLineVelocity = instability * (targetLinePosition - screenSize/2);

					String dateString = format.format(timerTime);
					try {
						bufferedWriter.write(dateString + ", " + targetLinePosition + ", " + deviceInput + ", " + Math.sqrt(meanSquaredDeviation) + ", " + ((float)outOfBoundCount/(float)count));
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
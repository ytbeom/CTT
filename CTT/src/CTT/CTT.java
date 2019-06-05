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
import javax.swing.JFrame;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;


public class CTT extends JFrame {
	private static final long serialVersionUID = 1L;
	
	private int screenWidth;
	private int screenHeight;
	private float targetLineWidth;
	private int targetLineLength;
	private float centreLineWidth;
	private float referenceLineWidth;
	//private float lambda;
	private float targetLinePosition = 200;
	private float[] dash = new float[] {50};
	private float updown;
	private boolean isStarted;
	
	private Image img;
	private Graphics img_g;
	
	private Controller targetController;
	private Component[] components;
	private ControllerListenerThread controllerListenerThread;
	
	private TimerThread timerThread;
	private long timerStartTime;
	
	private File saveFile;
	private BufferedWriter bufferedWriter;
	
	public CTT(String filename) {
		super("Life Enhancing Technology Lab. - Critical Tracking Task");
		try {
			File file = new File(filename);
			BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
			String line = "";
			
			if((line = bufferedReader.readLine()) != null) {
				String array[] = line.split(",");
				targetLineWidth = Float.parseFloat(array[0]);
				targetLineLength = Integer.parseInt(array[1]);
				centreLineWidth = Float.parseFloat(array[2]);
				referenceLineWidth = Float.parseFloat(array[3]);
				//lambda = Float.parseFloat(array[4]);
			}
			bufferedReader.close();
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		this.setUndecorated(false);
		this.setExtendedState(JFrame.MAXIMIZED_BOTH);
		this.setVisible(true);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.addKeyListener(new MyKeyListener());
		this.setFocusable(true);
		
		screenWidth = super.getWidth();
		screenHeight = super.getHeight();
		isStarted = false;
		updown = 0;
		timerThread = new TimerThread();
		
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
		
		Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
		
		for(Controller controller : controllers) {
			if (controller.getType() == Controller.Type.GAMEPAD)
				targetController = controller;
		}
		components = targetController.getComponents();
		
		controllerListenerThread = new ControllerListenerThread();
	}
	
	public void paint(Graphics g) {
		calculateTargetLinePosition();
		
		img = createImage(super.getWidth(), super.getHeight());
		img_g = img.getGraphics();
		Graphics2D g2 = (Graphics2D)img_g;
		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, super.getWidth(), super.getHeight());
		
		g2.setColor(Color.RED);
		g2.setStroke(new BasicStroke(centreLineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, dash, 0));
		g2.drawLine(0, screenHeight/2, screenWidth, screenHeight/2);
		
		g2.setColor(Color.BLUE);
		g2.setStroke(new BasicStroke(referenceLineWidth));
		g2.drawLine(screenWidth/5, screenHeight/4, screenWidth/5-50, screenHeight/4);
		g2.drawLine(screenWidth/5, screenHeight/4*3, screenWidth/5-50, screenHeight/4*3);
		
		g2.setColor(Color.BLACK);
		g2.setStroke(new BasicStroke(targetLineWidth));
		g2.drawLine((screenWidth-targetLineLength)/2, (int)targetLinePosition, (screenWidth-targetLineLength)/2+targetLineLength, (int)targetLinePosition);

		g.drawImage(img, 0, 0, null);
	
		repaint();
	}
	
	public void calculateTargetLinePosition() {
		targetLinePosition = targetLinePosition + updown;
	}
	
	class MyKeyListener implements KeyListener {
		@Override
		public void keyPressed(KeyEvent e) {
			if (!isStarted) {
				timerStartTime = System.currentTimeMillis();
				timerThread.start();
				//controllerListenerThread.start();
				isStarted = true;
			}
			if (e.getKeyCode() == 38) {
				updown = (float)-1.5;
			}
			else if (e.getKeyCode() == 40) {
				updown = (float)1.5;
			}
			else if (e.getKeyCode() == 27) {
				timerThread.interrupt();
				System.exit(0);
			}
			repaint();
		}
		
		@Override
		public void keyTyped(KeyEvent e) {

		}

		@Override
		public void keyReleased(KeyEvent e) {
			//updown = 0;
			//repaint();
		}
	}
	
	class TimerThread extends Thread {
		public void run() {
			while (true) {
				if(Thread.interrupted()) {
					try {
						bufferedWriter.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					break;
				}
				long timerTime = System.currentTimeMillis();
				if (timerTime - timerStartTime >= 50) {
					try {
						bufferedWriter.write(timerTime - timerStartTime + ", " + targetLinePosition);
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
	
	public static void main(String[] args) {
		@SuppressWarnings("unused")
		CTT ctt = new CTT("CTTsetting.csv");
	}
}

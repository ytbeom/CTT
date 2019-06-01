package CTT;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import javax.swing.JFrame;


public class CTT extends JFrame {
	private static final long serialVersionUID = 1L;
	
	private int screenWidth;
	private int screenHeight;
	private float targetLineWidth;
	private int targetLineLength;
	private float centreLineWidth;
	private float referenceLineWidth;
	//private float lambda;
	private int targetLinePosition = 200;
	private float[] dash = new float[] {50};
	private int updown;
	
	private Image img;
	private Graphics img_g;
	
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
		updown = 1;
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
		g2.drawLine((screenWidth-targetLineLength)/2, targetLinePosition, (screenWidth-targetLineLength)/2+targetLineLength, targetLinePosition);

		g.drawImage(img, 0, 0, null);
	
		repaint();
	}
	
	public void calculateTargetLinePosition() {
		targetLinePosition =  targetLinePosition + updown;
	}
	
	class MyKeyListener implements KeyListener {
		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == 38) {
				updown = -1;
			}
			else if (e.getKeyCode() == 40) {
				updown = +1;
			}
			else if (e.getKeyCode() == 0) {
				
			}
			repaint();
		}
		
		@Override
		public void keyTyped(KeyEvent e) {

		}

		@Override
		public void keyReleased(KeyEvent e) {

		}
	}
	
	public static void main(String[] args) {
		@SuppressWarnings("unused")
		CTT ctt = new CTT("CTTsetting.csv");
	}
}

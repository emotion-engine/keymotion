import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;

import javax.swing.*;

public class Render extends JPanel implements Datas, Runnable, KeyListener
{
	public boolean isInCollision, explosion, isRunning;
	public Bubble[] bubbles;
	public Bubble bigBubble;
	BufferStrategy buffer;
	KeyMotion key;
	DynamicalStart dyn;
	boolean isExploded, isAttracted, isDetected;
	
	public Render(BufferStrategy b)
	{
		this.buffer = b;

		isAttracted = false;
		bigBubble = new Bubble(this, 200);
		new Thread(this).start(); 
		addKeyListener(this);
	}
	
	public void addBubbles(Bubble[] bubs)
	{
		this.bubbles = bubs;
		dyn = new DynamicalStart(bubbles);
	}
	
	public void run() 
	{
		while (__RUN)
		{
			Graphics g = null;
            try 
            {
                g = buffer.getDrawGraphics();
                draw(g);
            } 
            finally { g.dispose(); }

            buffer.show(); // Shows everything except the scrollbars..
            Toolkit.getDefaultToolkit().sync(); 

            try { Thread.sleep( 1000/60 ) ; } 
            catch(InterruptedException ie) {}
		}
	}
	
	private void draw(Graphics g )
    {
        Graphics2D g2d=(Graphics2D)g;
        g2d.setColor(g2d.getBackground()); // clear background
        g2d.fillRect(0, 0, __WIDTH, __HEIGHT);
        
        if (isDetected)
        	g2d.setColor(Color.GREEN);
        else 
        	g2d.setColor(Color.RED);
        
        g2d.fillOval(900, 50, 20, 20);
        if (!isExploded)
        {
        	g2d.setColor(bigBubble.getColor());
            g2d.fillOval((int) bigBubble.getX(), (int) bigBubble.getY(), bigBubble.getDiametre(), bigBubble.getDiametre());
        }
        else 
        {
        	collision(100);
        	for (int i = 0; i < MAX_BUBBLES; i++)
    		{
        		if (isAttracted)
        			bubbles[i].attraction(bigBubble.getX(), bigBubble.getY());
        		
        		g2d.setColor(bubbles[i].getColor());
                g2d.fillOval((int) bubbles[i].getX(), (int) bubbles[i].getY(), bubbles[i].getDiametre(), bubbles[i].getDiametre());
    		}
        }
    }
	
	

	public void collision(double rbd) 
	{
		  
		for (int i = 0; i <MAX_BUBBLES; i++) 
		  {
		    for (int j=0;j<MAX_BUBBLES;j++) 
		    {   
		      double dx = bubbles[i].x - bubbles[j].x;
		      double dy =bubbles[i].y - bubbles[j].y;
		      double distance = Math.sqrt(dx*dx + dy*dy); // on utilise pythagore pour gerer la distance entre les elements
		      double minDist = bubbles[i].diametre/2 + bubbles[j].diametre/2; // la distance minimale est la moitié de chaque diametre des bulles
		      if (distance < minDist) 
		      { 
		        double angle = Math.atan2(dy, dx);
		        double targetX = bubbles[j].x + Math.cos(angle) * minDist;// ici on gere la position d'ou a été taper la balle pour la renvoyer dans le bon sens
		        double targetY = bubbles[j].y + Math.sin(angle) * minDist;
		        double ax = (targetX - bubbles[i].x);
		        double ay = (targetY - bubbles[i].y);
		        bubbles[j].vx -= ax/rbd;
		        bubbles[j].vy -= ay/rbd;
		        bubbles[i].vx += ax/rbd;
		        bubbles[i].vy += ay/rbd;
		      }
		    }
		  }
	}
	
	public void stopAcceleration()
	{
		
	}
	
	public void keyPressed(KeyEvent k) 
	{
		if (k.getKeyCode() == 39) // LEFT
			acceleration(false);
		else if (k.getKeyCode() == 37) // RIGHT
			acceleration(true);
		else if (k.getKeyCode() == 38) // UP
			bigBubble.setExplosion(true);
		else if (k.getKeyCode() == 40) // DOWN
			isAttracted = !isAttracted;
	}
	
	public void acceleration(boolean b)
	{
		for (int i = 0; i < MAX_BUBBLES; i++)
		{
			if (b)
				bubbles[i].setVitesse(0.99);
			else 
				bubbles[i].setVitesse(0.88);
		}
	}

	public void keyReleased(KeyEvent k) {}

	public void keyTyped(KeyEvent k) {}
	
	public void startSynchronization()
	{
		System.out.println("Render : synchronisé");
		isAttracted = true;
	}
	
	public void stopSynchronization()
	{
		System.out.println("Render : désynchronisé");
		isAttracted = false;
	}
	
	public boolean isSynchronized()
	{
		return isAttracted;
	}
	
	public void createExplosion()
	{
		System.out.println("3");
		isDetected = true;
		bigBubble.setExplosion(true);
	}
	
	public void launchEvent(String event)
	{
		if (event == "EXPLOSION_DONE")
		{
			isExploded = true;
			bigBubble.stopBubble();
		}
		else if (event == "PREPARE_BUBBLES")
		{
			dyn.addBubbles();
		}
	}

}

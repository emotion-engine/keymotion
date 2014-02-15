import java.awt.Color;
import java.awt.Point;
import java.util.Random;
import java.util.Vector;


public class Bubble implements Runnable, Datas
{
	double x, y, vx, vy, alpha, vitesse, diametre;
	Color couleur;
	boolean explosion, attraction, needBubble;
	Render render;
	int compteur;
	static int id =0;
	Vector<Point> location, velocity;
	
	public Bubble(Render v)
	{
		needBubble = true;
		render= v;
		diametre = 20;
		x = (Math.random()*(__WIDTH-diametre)+diametre);
		y =(Math.random()*(__HEIGHT-diametre)+diametre);
		vx = 0.01 * (Math.random() - 0.5);
        vy = 0.01 * (Math.random() - 0.5);
		couleur = new Color((int) Math.random()*(200-20) + 20, (int) Math.random()*(200-20) + 20, (int) Math.random()*(200-20) + 20);
		vitesse =  0.99;
	}
	
	public Bubble(Render v, double d)
	{
			needBubble = true;	
			render = v;	
		 	x = __WIDTH/4;
		    y = __HEIGHT/3;
		    vx = 0.01 * (Math.random() - 0.5);
	        vy = 0.01 * (Math.random() - 0.5);
		    couleur = new Color(0,0,0);
		    alpha=50;
		    vitesse=  0.60;
		    diametre = d;
		    new Thread(this).start(); 
	}

	public void run() 
	{
		while (needBubble)
		{
			if (explosion)
			{
				explose();
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if (attraction)
			{
				this.updatePosition();
				this.updateVitesse();
		        try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else
			{
				this.updatePosition();
				this.updateVitesse();
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	public void startBubble()
	{
		new Thread(this).start(); 
	}
	
	public void stopBubble()
	{
		needBubble = false;
	}
	
	public void updatePosition() 
	{		
        this.x += vx * this.vitesse;
        this.y += vy * this.vitesse;
        
        if (this.x >= __WIDTH-this.diametre) 
        	this.vx = -vx;
        
	    if (this.x < 0) 
	    	this.vx = -vx;
	    
	    if (this.y > __HEIGHT-this.diametre) 
	    	this.vy = -vy;
	    
	    if (this.y < 0) 
	    	this.vy = -vy;
	}
	
	  public void updateVitesse() 
	  {
		  this.vx += 0.01 * (Math.random() - 0.5);
		  this.vx *= vitesse;
		  this.vy += 0.01 * (Math.random() - 0.5);
		  this.vy *= vitesse;
	  }
	
	  public void explose()
		{
		  
		 	this.diametre--;
			this.x+=0.5;
			this.y+=0.5;
			if (this.diametre < 150 && compteur == 0) 
			{
				compteur++;
				render.launchEvent("PREPARE_BUBBLES");
			}
			if (this.diametre < 0)
			{
				render.launchEvent("EXPLOSION_DONE");
				explosion = false;
			}
				
		}
	  
	  public void attraction(double px, double py) 
	  {
		 
		  	double easing=0.5;
		  	double dx= px - this.x;
		  	double dy = py - this.y;
		  	
		  	
		  	
		  	 if (Math.abs(dx) > 10) 
		  	 {
		  		vx += dx * easing/1000;
		  	 }
		  	 if (Math.abs(dy) > 10) 
		  	 {
		  		vy += dy * easing/1000;
		     }
		  }  
	  
	  
	  
	  
	  
	  
	public void setExplosion(boolean b)
	{
		System.out.println("4");
		this.explosion = b;
	}
	 
	public void setAttraction(boolean b)
	{
		this.attraction = b;
	}
	
	
	public void setX(double px)
	{
		this.x = px;
	}
	
	public void setY(double py)
	{
		this.y = py;
	}
	
	public void setVX(double px)
	{
		this.vx = px;
	}
	
	public void setVY(double py)
	{
		this.vy = py;
	}
	
	public void setVitesse(double v)
	{
		this.vitesse = v;
	}

	public void setDiametre(double d)
	{
		this.diametre = d;
	}
	
	public double getX() 
	{
		return this.x;
	}

	public double getY() 
	{
		return this.y;
	}
	
	public double getVX() 
	{
		return this.vx;
	}

	public double getVY() 
	{
		return this.vy;
	}
	
	public int getDiametre() 
	{
		return (int) this.diametre;
	}
	
	public Color getColor()
	{
		return this.couleur;
	}


}

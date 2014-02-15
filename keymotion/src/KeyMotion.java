import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;
import javax.swing.*;


public class KeyMotion extends JFrame implements Datas
{
	
	private JButton skeleton, keymotion;
	private Render render;
	private Tracker tracker;
	private BufferStrategy bufferStrategy;
	private Bubble[] bubbles;
	private Bubble bigBubble;
	
	public KeyMotion() 
	{
		bubbles = new Bubble[MAX_BUBBLES];
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setLayout(null);
		this.setUndecorated(true);
	    this.setSize(this.getToolkit().getScreenSize());
	    this.setLocationRelativeTo(null);
	    this.setState(JFrame.MAXIMIZED_BOTH);
	    this.setExtendedState(JFrame.MAXIMIZED_BOTH);
	    this.setVisible(true);
	    this.createBufferStrategy(3);
	    this.bufferStrategy = this.getBufferStrategy();
	    
	    render = new Render(bufferStrategy);
	    tracker = new Tracker(render);

	    for (int i = 0; i < MAX_BUBBLES; i++)
		{
			bubbles[i] = new Bubble(render);
		}
	    
		render.addBubbles(bubbles);
	    setContentPane(render);
		render.requestFocus();
	    
	}
	
	public static void main(String[] args)
	{
		KeyMotion keymotion = new KeyMotion();
	}

}

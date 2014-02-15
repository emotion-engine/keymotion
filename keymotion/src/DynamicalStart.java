
public class DynamicalStart implements Runnable, Datas
{
	boolean needDynamical, addBubbles;
	Bubble[] bubbles;
	
	public DynamicalStart(Bubble[] b)
	{
		bubbles = b;
		needDynamical = true;
		addBubbles = false;
		
		new Thread(this).start(); 
	}

	public void run() 
	{
		while (needDynamical)
		{
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (addBubbles)
			{
				for (int i = 0; i < MAX_BUBBLES; i++)
				{
					bubbles[i].startBubble();
				}
				stopDynamical();
			}
		}
	}
	
	public void stopDynamical()
	{
		this.needDynamical = false;
	}
	
	public void addBubbles()
	{
		addBubbles = true;
	}
}

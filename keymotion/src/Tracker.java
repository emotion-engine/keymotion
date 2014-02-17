// Tracker.java
// Emotion Engine, Keymotion

/* Based on the Java OpenNI UserTracker sample and the work of Andrew Davison : ad@fivedots.psu.ac.th

   Comments in french
*/

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.nio.ShortBuffer;
import java.text.DecimalFormat;
import javax.swing.*;
import org.openni.*;


public class Tracker implements Runnable, Datas
{
	private volatile boolean isRunning;
	private byte[] imageInBytes;
	private int imageWidth, imageHeight, compteurSynchro, compteurDesynchro;
	private float depthValues[]; // Tableau avec les valeurs de profondeur
	private int maxDepth = 0; // La plus grande valeur de profondeur
	// Données en millisecondes
	private int imageCount = 0; // Nombre d'images
	private long totalTime = 0; // Temps total
	private DecimalFormat df; 
	private Font msgFont;
	private Render render;
	// OpenNI
	private Context context; // Contexte de la scène
	private DepthMetaData depthMD; // Les données relatives à la profondeur
	private SceneMetaData sceneMD; // Les données relatives aux données de l'image capturée
									// Chaque pixel possède un identifiant, 0 = fond, 1 = user 1, 2 = user 2, etc...
	private Skeletons skels;   // the users' skeletons
	private boolean isSynchro;
	
	public Tracker(Render r)
	{
		render = r;
		isSynchro = false;

		// Options pour le texte qui s'affiche
		df = new DecimalFormat("0.#");  // 1 dp
	    msgFont = new Font("SansSerif", Font.BOLD, 18);
	    compteurSynchro = 0;
	    compteurDesynchro = 0;
	    configurationOpenNI();
	    
	    depthValues = new float[MAX_DEPTH_SIZE];

	    imageWidth = depthMD.getFullXRes();
	    imageHeight = depthMD.getFullYRes();
	    System.out.println("Image dimensions (" + imageWidth + ", " +
	                                              imageHeight + ")");
	    // Crée un tableau d'octets en fonction du nombre de pixels et du codage en bits de chacun d'entre eux
	    imageInBytes = new byte[imageWidth * imageHeight * 3];
	    
	    // Lance le processus qui met à jour les données à chaque variation dans l'image
	    new Thread(this).start(); 
	}
	
	/* Création du contexte, gestion de la profondeur, des données de profondeur, 
	   gestion d'utilisateurs, données de la scène, squelettes */
	public void configurationOpenNI()
	{
		try {
			context = new Context();
			
			// Ajout de la licence OpenNI, si elle n'a pas été rajoutée dans les fichiers système
		    License license = new License("PrimeSense", "0KOIk2JeIBYClPWVnMoRKn5cdY4=");
		    context.addLicense(license); 
		    // Gestion de la profondeur à partir du contexte de la scène
		    DepthGenerator depthGenerator = DepthGenerator.create(context);
		    // Sortie du mapping de la scène
		    MapOutputMode mapMode = new MapOutputMode(640, 480, 30);   // xRes, yRes, FPS
		    // Configure la sortie du mapping pour la gestion des données de profondeur
		    depthGenerator.setMapOutputMode(mapMode); 
		    
		    context.setGlobalMirror(true);
		    // Utilise les métadonnées de la gestion de profondeur pour accéder aux informations de profondeur
		    depthMD = depthGenerator.getMetaData();
		    
		    // Instancie la détection d'utilisateur par rapport à la scène
		    UserGenerator userGen = UserGenerator.create(context);
		    // Sert à générer un mapping de la scène avec un id utilisateur pour chaque donnée de profondeur
		    sceneMD = userGen.getUserPixels(0);

		    skels = new Skeletons(userGen, depthGenerator, this);

		    context.startGeneratingAll(); 
		    System.out.println("Génère la scène..."); 
			
		} catch (GeneralException e) 
				{
					System.out.println("Erreur lors de la configuration avec les samples OpenNI");
				}
	}

	// Thread principal
	public void run() 
	{
		while (__RUN)
		{	    	
		      try 
		      {
		        context.waitAnyUpdateAll();
		      
		      }
		      catch(StatusException e)
		      {  
		    	  System.out.println("Problème lors de la mise à jour"); 
		      }
		      
		      // Récupère le temps où la mise à jour des données commence
			  long startTime = System.currentTimeMillis();
		      updateUserDepths();
		      skels.update();
		      imageCount++;
		      totalTime += (System.currentTimeMillis() - startTime);
	      
	    }
	}

	
	// Construit un tableau d'octets dans lequel les utilisateurs détectés sont représentés par du blanc
	public void updateUserDepths()
    {
			ShortBuffer depthBuf = depthMD.getData().createShortBuffer();
			calculateDepthValues(depthBuf);
			depthBuf.rewind(); // Remet les valeurs du tableau de données de profondeur
			
			// Crée un tableau par rapport au mapping de la scène dans lequel chaque pixel a un identifiant utilisateur ou 0 pour le fond
			ShortBuffer usersBuf = sceneMD.getData().createShortBuffer();
	
			while (depthBuf.remaining() > 0) 
			{
					int pos = depthBuf.position();
				    short depthVal = depthBuf.get();
				    short userID = usersBuf.get();
				
				    imageInBytes[3*pos] = 0;     // default colour is black when there's no depth data
				    imageInBytes[3*pos + 1] = 0;
				    imageInBytes[3*pos + 2] = 0;
				
				    if (depthVal != 0) 
				    {  
					       int couleur = 255;
					
					       if (userID == 0)    // not a user; actually the background
					    	   couleur = 0;   
					       // use last index: the position of white in USER_COLORS[]
					       // convert histogram value (0.0-1.0f) to a RGB color
					       float histValue = depthValues[depthVal];
					       imageInBytes[3*pos] = (byte) (histValue * couleur);
					       imageInBytes[3*pos + 1] = (byte) (histValue * couleur);
					       imageInBytes[3*pos + 2] = (byte) (histValue * couleur);
				    }
			}
	}

	private void calculateDepthValues(ShortBuffer depthBuf) 
	{
		// Remet le tableau à 0
	    for (int i = 0; i <= maxDepth; i++)
	    	depthValues[i] = 0;

	    // Enregiste le nombre de valeurs différentes existantes dans le tableau d'octets
	    int numPoints = 0;
	    maxDepth = 0;
	    
	    // Tant que le tableau n'a pas été parcouru
	    while (depthBuf.remaining() > 0) 
	    {
	    	short depthVal = depthBuf.get();
		    if (depthVal > maxDepth)
		        maxDepth = depthVal;
		    if ((depthVal != 0)  && (depthVal < MAX_DEPTH_SIZE))
		    { 
		    	depthValues[depthVal]++;
		        numPoints++;
		    }
	    }

	    // convert into a cummulative depth count (skipping histogram[0])
	    for (int i = 1; i <= maxDepth; i++)
	    	depthValues[i] += depthValues[i-1];

	    /* convert cummulative depth into the range 0.0 - 1.0f
	       which will later be used to modify a color from USER_COLORS[] */
	    if (numPoints > 0) {
	      for (int i = 1; i <= maxDepth; i++)    // skipping histogram[0]
	    	  depthValues[i] = 1.0f - (depthValues[i] / (float) numPoints);
	    }
	}
	

	public boolean isSynchronized()
	{
		return this.isSynchro;
	}
	
	public int getCompteur()
	{
		return compteurSynchro;
	}
	
	public void synchronize()
	{
		
		compteurSynchro++;
		System.out.println("Compteur synchro : "+compteurSynchro);
		render.setCurrentState(true);
		if (compteurSynchro >= 50)
		{
			
			isSynchro = true;
			render.startSynchronization();
		}
		
		if (compteurSynchro >= 100)
		{
			compteurSynchro = 100;
		}
	}
	
	public void disynchronize()
	{
		compteurSynchro--;
		System.out.println("Compteur synchro : "+compteurSynchro);
		render.setCurrentState(false);
		if (compteurSynchro < 50)
		{
			
			isSynchro = false;
			render.stopSynchronization();
		}
		if (compteurSynchro <= 0)
		{
			compteurSynchro = 0;
		}
	}
	
	public void explosion()
	{
		System.out.println("2");
		render.createExplosion();
	}

}
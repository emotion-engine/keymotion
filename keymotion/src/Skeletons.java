
// Skeletons.java
// Andrew Davison, September 2011, ad@fivedots.psu.ac.th

/* Skeletons sets up four 'observers' (listeners) so that 
   when a new user is detected in the scene, a standard pose for that 
   user is detected, the user skeleton is calibrated in the pose, and then the
   skeleton is tracked. The start of tracking adds a skeleton entry to userSkels.

   Each call to update() updates the joint positions for each user's
   skeleton.
  
   Each call to draw() draws each user's skeleton, with a rotated HEAD_FNM
   image for their head, and status text at the body's center-of-mass.
*/

import java.awt.*;
import javax.swing.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.awt.color.*;
import java.io.*;
import javax.imageio.*;
import java.util.*;
import org.openni.*;
import java.nio.ShortBuffer;

public class Skeletons implements Datas
{
  // OpenNI
  private UserGenerator userGen;
  private DepthGenerator depthGen;

  // OpenNI capabilities used by UserGenerator
  private SkeletonCapability skelDatas;
                // to output skeletal data, including the location of the joints
  private PoseDetectionCapability poseDetectionCap;
               // to recognize when the user is in a specific position
  private static int compteur = 0;

  private String calibPoseName = null;
  private Tracker tracker;
  private HashMap<Integer, HashMap<SkeletonJoint, SkeletonJointPosition>> userSkels;
                                           // was SkeletonJointTransformation
    /* userSkels maps user IDs --> a joints map (i.e. a skeleton)
       skeleton maps joints --> positions (was positions + orientations)
    */

  public Skeletons(UserGenerator userGen, DepthGenerator depthGen, Tracker t)
  {
	this.tracker = t;
    this.userGen = userGen;
    this.depthGen = depthGen;
    
    configure();
    userSkels = new HashMap<Integer, HashMap<SkeletonJoint, SkeletonJointPosition>>();
  } // end of Skeletons()

  
  
  public int userRatio(int userID1, int userID2)
  {
	 int dist1, dist2, head1, head2, lfoot1, lfoot2, rfoot1, rfoot2;
	 int ratio = 0;
	 try 
	 {
		
		 head1 = (int)this.skelDatas.getSkeletonJointPosition(userID1, SkeletonJoint.HEAD).getPosition().getY();
		 head2 = (int)this.skelDatas.getSkeletonJointPosition(userID2, SkeletonJoint.HEAD).getPosition().getY();
		 lfoot1 = (int)this.skelDatas.getSkeletonJointPosition(userID1, SkeletonJoint.LEFT_FOOT).getPosition().getY();
		 lfoot2 = (int)this.skelDatas.getSkeletonJointPosition(userID2, SkeletonJoint.LEFT_FOOT).getPosition().getY();
		 rfoot1 = (int)this.skelDatas.getSkeletonJointPosition(userID1, SkeletonJoint.RIGHT_FOOT).getPosition().getY();
		 rfoot2 = (int)this.skelDatas.getSkeletonJointPosition(userID2, SkeletonJoint.RIGHT_FOOT).getPosition().getY();
		 
		 dist1 = ((head1 - lfoot1) + (head1 - rfoot1)) / 2;
		 dist2 = ((head2 - lfoot2) + (head2 - rfoot2)) / 2;
		 
		 ratio = dist1 < dist2 ? dist1/dist2 : dist2/dist1;
		 
	} catch (StatusException e) 
	{
		System.out.println("Erreur lors de la récupération du ratio");
	}
	  	
	 return ratio;
	  
  }
  
  
  private void configure()
  /* create pose and skeleton detection capabilities for the user generator, 
     and set up observers (listeners)   */
  {
    try {
      // setup UserGenerator pose and skeleton detection capabilities;
      // should really check these using ProductionNode.isCapabilitySupported()
      poseDetectionCap = userGen.getPoseDetectionCapability();

      skelDatas = userGen.getSkeletonCapability();
      calibPoseName = skelDatas.getSkeletonCalibrationPose();  // the 'psi' pose
      skelDatas.setSkeletonProfile(SkeletonProfile.ALL);
             // other possible values: UPPER_BODY, LOWER_BODY, HEAD_HANDS

      // set up four observers
      userGen.getNewUserEvent().addObserver(new NewUserObserver());   // new user found
      userGen.getLostUserEvent().addObserver(new LostUserObserver()); // lost a user

      poseDetectionCap.getPoseDetectedEvent().addObserver(
                                             new PoseDetectedObserver());  
          // for when a pose is detected

      skelDatas.getCalibrationCompleteEvent().addObserver(
                                             new CalibrationCompleteObserver());
         // for when skeleton calibration is completed, and tracking starts
    } 
    catch (Exception e) {
      System.out.println(e);
      System.exit(1);
    }
  }  // end of configure()


  // --------------- updating ----------------------------

  public void update()
  // update skeleton of each user
  {
    try {   
      int[] userIDs = userGen.getUsers();   // there may be many users in the scene
      for (int i = 0; i < userIDs.length; ++i) {
        int userID = userIDs[i];
        if (skelDatas.isSkeletonCalibrating(userID))
          continue;    // test to avoid occasional crashes with isSkeletonTracking()
        if (skelDatas.isSkeletonTracking(userID))
          updateJoints(userID);
      }
    }
    catch (StatusException e) 
    {  System.out.println(e); }
    
    if (compteur == 2)
    {
    	if (isSynchronizing())
    		tracker.synchronize();    		
    	else
    		tracker.disynchronize();
    }
  }  // end of update()




  public boolean isSynchronizing() 
  {
	  if (check3Points(SkeletonJoint.LEFT_SHOULDER, SkeletonJoint.LEFT_ELBOW, SkeletonJoint.LEFT_HAND) && 
			  check3Points(SkeletonJoint.LEFT_HAND, SkeletonJoint.LEFT_SHOULDER, SkeletonJoint.LEFT_HIP))
		  return true;
	  if (check3Points(SkeletonJoint.RIGHT_SHOULDER, SkeletonJoint.RIGHT_ELBOW, SkeletonJoint.RIGHT_HAND) && 
			  check3Points(SkeletonJoint.RIGHT_HAND, SkeletonJoint.RIGHT_SHOULDER, SkeletonJoint.RIGHT_HIP))
		  return true;
//	  if (check3Points(SkeletonJoint.RIGHT_KNEE, SkeletonJoint.WAIST, SkeletonJoint.LEFT_KNEE))
//		  return true;
	  
	  return false;
  }

  public boolean check3Points(SkeletonJoint A, SkeletonJoint B, SkeletonJoint C)
  {
	  double A_X1, A_Y1, B_X1, B_Y1, C_X1, C_Y1, A_X2, A_Y2, B_X2, B_Y2, C_X2, C_Y2;

	  try 
	  {
		  A_X1 = this.skelDatas.getSkeletonJointPosition(0, A).getPosition().getX();
		  A_Y1 = this.skelDatas.getSkeletonJointPosition(0, A).getPosition().getY();
		  B_X1 = this.skelDatas.getSkeletonJointPosition(0, B).getPosition().getX();
		  B_Y1 = this.skelDatas.getSkeletonJointPosition(0, B).getPosition().getY();
		  C_X1 = this.skelDatas.getSkeletonJointPosition(0, C).getPosition().getX();
		  C_Y1 = this.skelDatas.getSkeletonJointPosition(0, C).getPosition().getY();
		  A_X2 = this.skelDatas.getSkeletonJointPosition(1, A).getPosition().getX();
		  A_Y2 = this.skelDatas.getSkeletonJointPosition(1, A).getPosition().getY();
		  B_X2 = this.skelDatas.getSkeletonJointPosition(1, B).getPosition().getX();
		  B_Y2 = this.skelDatas.getSkeletonJointPosition(1, B).getPosition().getY();
		  C_X2 = this.skelDatas.getSkeletonJointPosition(1, C).getPosition().getX();
		  C_Y2 = this.skelDatas.getSkeletonJointPosition(1, C).getPosition().getY();

		  AB_1[0] = A_X1-B_X1;
		  AB_1[1] = A_Y1-B_Y1;
		  BC_1[0] = B_X1-C_X1;
		  BC_1[1] = B_Y1-C_Y1;
		  AC_1[0] = A_X1-C_X1;
		  AC_1[1] = A_Y1-C_Y1;
		  
		  AB_2[0] = A_X2-B_X2;
		  AB_2[1] = A_Y2-B_Y2;
		  BC_2[0] = B_X2-C_X2;
		  BC_2[1] = B_Y2-C_Y2;
		  AC_2[0] = A_X2-C_X2;
		  AC_2[1] = A_Y2-C_Y2;
		  
		  double distAB_1 = Math.sqrt(Math.pow(AB_1[0], 2)+ Math.pow(AB_1[1], 2)); 
		  double distBC_1 = Math.sqrt(Math.pow(BC_1[0], 2)+ Math.pow(BC_1[1], 2)); 
		  double distAC_1 = Math.sqrt(Math.pow(AC_1[0], 2)+ Math.pow(AC_1[1], 2)); 
		  double distAB_2 = Math.sqrt(Math.pow(AB_2[0], 2)+ Math.pow(AB_2[1], 2)); 
		  double distBC_2 = Math.sqrt(Math.pow(BC_2[0], 2)+ Math.pow(BC_2[1], 2)); 
		  double distAC_2 = Math.sqrt(Math.pow(AC_2[0], 2)+ Math.pow(AC_2[1], 2));
		  
		  double angleABC_1 =Math.acos((Math.pow(distAB_1, 2)+Math.pow(distAB_1,2)-Math.pow(distAB_1,2))/(2*distAB_1*distBC_1));
		  double angleABC_2 =Math.acos((Math.pow(distAB_2, 2)+Math.pow(distAB_2,2)-Math.pow(distAB_2,2))/(2*distAB_2*distBC_2));
		  
		  if ((int) angleABC_1 == (int) angleABC_2)
			  return true;
				  
	  } catch (StatusException e) 
	  {
		  e.printStackTrace();
	  }
	  
	  return false;
  }

  private void updateJoints(int userID) throws StatusException
  {
	    HashMap<SkeletonJoint, SkeletonJointPosition> skel = userSkels.get(userID);
	    updateJoint(skel, userID, SkeletonJoint.HEAD);
	    updateJoint(skel, userID, SkeletonJoint.NECK);
	    updateJoint(skel, userID, SkeletonJoint.LEFT_SHOULDER);
	    updateJoint(skel, userID, SkeletonJoint.LEFT_ELBOW);
	    updateJoint(skel, userID, SkeletonJoint.LEFT_HAND);
	    updateJoint(skel, userID, SkeletonJoint.RIGHT_SHOULDER);
	    updateJoint(skel, userID, SkeletonJoint.RIGHT_ELBOW);
	    updateJoint(skel, userID, SkeletonJoint.RIGHT_HAND);
	    updateJoint(skel, userID, SkeletonJoint.TORSO);
	    updateJoint(skel, userID, SkeletonJoint.LEFT_HIP);
	    updateJoint(skel, userID, SkeletonJoint.LEFT_KNEE);
	    updateJoint(skel, userID, SkeletonJoint.LEFT_FOOT);
	    updateJoint(skel, userID, SkeletonJoint.RIGHT_HIP);
	    updateJoint(skel, userID, SkeletonJoint.RIGHT_KNEE);
	    updateJoint(skel, userID, SkeletonJoint.RIGHT_FOOT);

  }

  private void updateJoint(HashMap<SkeletonJoint, SkeletonJointPosition> skel, int userID, SkeletonJoint joint)
  {
	    try 
	    {
		      // report unavailable joints (should not happen)
		      if (!skelDatas.isJointAvailable(joint) || !skelDatas.isJointActive(joint)) {
		        System.out.println(joint + " not available for updates");
		        return;
		      }
		
		      SkeletonJointPosition pos = skelDatas.getSkeletonJointPosition(userID, joint);
		      if (pos == null) {
		        System.out.println("No update for " + joint);
		        return;
		      }
		
		      SkeletonJointPosition jPos = null;
		      if (pos.getPosition().getZ() != 0)   // has a depth position
		        jPos = new SkeletonJointPosition( 
		                           depthGen.convertRealWorldToProjective(pos.getPosition()),
		                                            pos.getConfidence());
		      else  // no info found for that user's joint
		        jPos = new SkeletonJointPosition(new Point3D(), 0);
		      skel.put(joint, jPos);
	    }
	    catch (StatusException e) 
	    {  System.out.println(e); }
  } 



  // --------------------- 4 observers -----------------------
  /*   user detection --> pose detection --> skeleton calibration -->
       skeleton tracking (and creation of userSkels entry)
       + may also lose a user (and so delete its userSkels entry)
  */


  class NewUserObserver implements IObserver<UserEventArgs>
  {
    public void update(IObservable<UserEventArgs> observable, UserEventArgs args)
    {
      System.out.println("Detected new user " + args.getId());
      try {
        // try to detect a pose for the new user
        poseDetectionCap.StartPoseDetection(calibPoseName, args.getId());   // big-S ?
      }
      catch (StatusException e)
      { e.printStackTrace(); }
    }
  }  // end of NewUserObserver inner class



  class LostUserObserver implements IObserver<UserEventArgs>
  {
    public void update(IObservable<UserEventArgs> observable, UserEventArgs args)
    { System.out.println("Lost track of user " + args.getId());
      userSkels.remove(args.getId());    // remove user from userSkels
      compteur--;
    }
  } // end of LostUserObserver inner class



  class PoseDetectedObserver implements IObserver<PoseDetectionEventArgs>
  {
    public void update(IObservable<PoseDetectionEventArgs> observable,
                                                     PoseDetectionEventArgs args)
    {
      int userID = args.getUser();
      System.out.println(args.getPose() + " pose detected for user " + userID);
      try {
        // finished pose detection; switch to skeleton calibration
        poseDetectionCap.StopPoseDetection(userID);    // big-S ?
        skelDatas.requestSkeletonCalibration(userID, true);
      }
      catch (StatusException e)
      {  e.printStackTrace(); }
    }
  }  // end of PoseDetectedObserver inner class



  class CalibrationCompleteObserver implements IObserver<CalibrationProgressEventArgs>
  {
    public void update(IObservable<CalibrationProgressEventArgs> observable,
                                                    CalibrationProgressEventArgs args)
    {
      int userID = args.getUser();
      System.out.println("Calibration status: " + args.getStatus() + " for user " + userID);
      try {
        if (args.getStatus() == CalibrationProgressStatus.OK) {
          // calibration succeeded; move to skeleton tracking
          System.out.println("Starting tracking user " + userID);
          skelDatas.startTracking(userID);
          userSkels.put(new Integer(userID),
                     new HashMap<SkeletonJoint, SkeletonJointPosition>());  
              // create new skeleton map for the user in userSkels
          compteur++;
        }
        else    // calibration failed; return to pose detection
          poseDetectionCap.StartPoseDetection(calibPoseName, userID);    // big-S ?
        
        if (compteur == 2)
        {
        	System.out.println("1");
        	tracker.explosion();
//        	userRatio(1, 2);
        }
      }
      catch (StatusException e)
      {  e.printStackTrace(); }
    }
  }  // end of CalibrationCompleteObserver inner class


} // end of Skeletons class

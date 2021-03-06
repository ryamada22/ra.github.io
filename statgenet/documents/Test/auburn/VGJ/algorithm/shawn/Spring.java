/*
 *  File:  Spring.java  (Implementation of Kamada and Kawai's spring algorithm 
 *                      with modifications).  
 *
 *  12/12/96    Shawn Lorae Stutzman
 */

package auburn.VGJ.algorithm.shawn;

import auburn.VGJ.algorithm.*;
import auburn.VGJ.graph.*;
import auburn.VGJ.util.*;
import java.lang.*;

/**
 *  Class to implement Kamada and Kawai's spring algorithm 
 *                      with modifications).
 * </p>Here is the <a href="../algorithm/shawn/Spring.java">source</a>.
 */




public class Spring implements GraphAlgorithm
  {
   private static DRect rect;
   private static double xmax,xmin,ymax,ymin;
   private static int orderedList[];	// this is used to enable a quick lookup
					// of nodes' array values (which might 
					// differ from their index values)
   private static int N;  // nodes in graph; will be set in compute function
   private static int EDGES;  // edges in graph; set in compute function
   private static long D[][]; //the graphical distances between all pairs
   private static double K[][]; // spring strengths
   private static double Ko;    // spring constant
   private static double L[][]; // ideal spring lengths
   private static double epsilon;
   private static double delta[];
   private static int MAX_TIMES_REPOSITIONED = 10;// constant to limit number 
						  // of times thru "inner" loop
   private static int numTimesRepositioned = 0;  // counts number of times 
						 // thru "inner" loop
   private static int NUM_TIMES_MOVED[];	// counts number of times each 
						// vertex has been selected to 
						// move

   private static int HY_SIZE = 10;	//  these variables control the E 
   private static int HY_PERCENTAGE = 5;//   stopping condition and how it 
   private static double E,E_HY[];	//   keeps track of its information
   private static int COUNTER = 0;

   private static Node mv = null;  // the node chosen to move at each iteration
   private static double partial_x,partial_y,partial_xx, 
		partial_xy,partial_yx,partial_yy;
   private static boolean connected;



   public String compute(Graph G, GraphUpdate update)
     {
      double tempValue,maxValue,maxDel; int maxTimes = 0;
      int mvi,i;
      Node v,u;

      rect = update.windowRect();
      Initialize(G);

      // if graph not entirely contained in drawing area, change drawing area?
      // change graph?? (want to make sure that graph is inside the drawing 
      // area before starting....)  For now (with the "non-bouncing 
      // moving-function" used), this doesn't need to be enforced)

      // Also check to make sure that graph is connected, or else we need to 
      // exit here
         for(i=0;i<G.numberOfNodes();i++)
            if (connected == false)
              {
               //System.out.println("Error:  This algorithm should not be run on a non-connected graph!");
               return "Error:  This algorithm should not be run on a non-connected graph!";
              }

      //  and undirected (maybe a prompt dialog box to ask if it should be made 
      // undirected or if we should cancel the algorithm) but for now just exit 
      // here.
         if (G.isDirected())
           {
            //System.out.println("Error:  This algorithm should not be run on an undirected graph.");
            return "Error:  This algorithm should not be run on an undirected graph.";
           }


      // Done with primary checking start main iterative loop:

      while((Boolean.TRUE).booleanValue())
        {
         // if any pair of vertices share the same position, move them away
         CheckPositions(G);

         // determine the next vertex to move according to TempFunction

         maxDel = 0;
         for (v = G.firstNode(); v !=null; v = G.nextNode(v))
           {
           delta[enum(G.getIndexFromNode(v))]=Math.sqrt(findDelta2(G,v));
           if (delta[enum(G.getIndexFromNode(v))] > maxDel)
              maxDel = delta[enum(G.getIndexFromNode(v))];
           if (NUM_TIMES_MOVED[enum(G.getIndexFromNode(v))] > maxTimes)
              maxTimes = NUM_TIMES_MOVED[enum(G.getIndexFromNode(v))];
           }

         mv = G.firstNode(); mvi = enum(G.getIndexFromNode(mv));
         maxValue = 0;
         for (v = mv; v !=null; v = G.nextNode(v))
           {
            if (maxTimes != 0)
               tempValue = TempFunction 
	         (delta[enum(G.getIndexFromNode(v))]/maxDel,
	         1 - NUM_TIMES_MOVED[enum(G.getIndexFromNode(v))]/maxTimes);
            else
               // the first time, base the decision only upon the delta
               tempValue = delta[enum(G.getIndexFromNode(v))]/maxDel;
            if (tempValue > maxValue)
              {
               mv = v; mvi = enum(G.getIndexFromNode(mv));
               maxValue = tempValue;
              }
           }


         // see if any stopping conditions are met

         if (delta[mvi] < epsilon)
           {
            //System.out.println("Quitting because of epsilon");
            break;
           }
         if (COUNTER > HY_SIZE)
            if (E_HY[COUNTER%HY_SIZE] * HY_PERCENTAGE/100.0  > (E = findE(G)))
              {
               //System.out.println("Quitting because of history");
               break;
              }

         // can't quit yet, so put the E in the history array and get ready to 
	 // find a new position for vertex 'mv'

         E_HY[COUNTER%HY_SIZE] = E;
         
         numTimesRepositioned = 0;

         while ( (delta[mvi] > epsilon)
			 && (numTimesRepositioned < MAX_TIMES_REPOSITIONED) )
           {
            //MoveToNewPosition(G);  // this function simulates elastic collision 
				   //  instead of hitting & sticking
            //MoveToNewPosition1(G); // this function "hits and sticks"
            MoveToNewPosition2(G); // this function doesn't care
            delta[mvi] = Math.sqrt(findDelta2(G,mv));
            numTimesRepositioned++;
           }//end "inner" while

         NUM_TIMES_MOVED[mvi]++;

         //if redraw is on ???
              update.update(false);

         // if the cool stop button (that isn't implemented) has been pressed
              // redraw
              // break;

        }//end "outer" while
      return null;

     }//end main compute function





   private static void Initialize(Graph G)
     {
      Node v,u;
      int i;
      N = G.numberOfNodes();
      makeOrderedList(G);
      NUM_TIMES_MOVED = new int[N];
      for (i=0;i<N;i++)
         NUM_TIMES_MOVED[i] = 0;
      EDGES=0;
      for (v = G.firstNode(); v !=null; v = G.nextNode(v))
        {
         for (u = G.nextNode(v); u !=null; u = G.nextNode(u))
            if (v.hasChild(u)) EDGES++;
        }
      epsilon = 0.5*(N+EDGES);
      findDistances(G);
      find_l_and_k(G);
      delta = new double[N];
      E_HY = new double[HY_SIZE];
      xmin = rect.x;  xmax = xmin + rect.width;
      ymin = rect.y;  ymax = ymin + rect.height;
     }






   private static void findDistances(Graph G)
     {
      Node s,u,v,p;
      int i,j,vi,pi,si,ui;
      boolean done[];
      Queue queue;

      queue = new Queue();
      done = new boolean[G.numberOfNodes()];
      D = new long[G.numberOfNodes()][G.numberOfNodes()];


      /* Initialize the distance matrix to 0s */
      for (i=0;i<G.numberOfNodes();i++)
         for (j=i;j<G.numberOfNodes();j++)
            { D[i][j] = 0; D[j][i] = 0; }


      /* For each node, do Dijstra... */
      for (s = G.firstNode(); s !=null; s = G.nextNode(s))
        {
         /* Initialize the done array to false */
         for (i=0;i<G.numberOfNodes();i++)
            done[i] = false;

         si = enum(G.getIndexFromNode(s));
         done[si] = true;
         for (v = G.firstNode(); v !=null; v = G.nextNode(v))
         if (s.hasChild(v))    /* fix this using Node's nextChild() */
           {
            queue.push(G.getIndexFromNode(v));
            queue.push(G.getIndexFromNode(s));
            vi = enum(G.getIndexFromNode(v));
            done[vi] = true;
           }
         while (!(queue.isEmpty()))
           {
            v = G.getNodeFromIndex(queue.pop());
            p = G.getNodeFromIndex(queue.pop());
            pi = enum(G.getIndexFromNode(p));
            vi = enum(G.getIndexFromNode(v));
            D[si][vi] = D[pi][si] + 1;
            D[vi][si] = D[si][vi];
            for (u = G.firstNode(); u !=null; u = G.nextNode(u))
            if (v.hasChild(u))    /* fix this using Node's nextChild() */
              {
               ui = enum(G.getIndexFromNode(u));
               if (!(done[ui]))
                 {
                  queue.push(G.getIndexFromNode(u));
                  queue.push(G.getIndexFromNode(v));
                  done[ui] = true;
                 }
              }
           }
        }/* for s*/

   /* Change the appropriate remaining 0's to infinities and set value of the 
      boolean variable connected */
   connected = true;
   for (i=0;i<G.numberOfNodes();i++)
      for (j=i+1;j<G.numberOfNodes();j++)
         if (D[i][j]==0)
           { 
            connected = false;
            D[i][j] = Long.MAX_VALUE;
            D[j][i] = Long.MAX_VALUE;
           }
     }//end findDistances function






   private static void makeOrderedList(Graph G)
     {
      int highestIndex,i;
      Node v;

      orderedList = new int[G.numberOfNodes()];
      for (i=0,v = G.firstNode(); v !=null; i++,v = G.nextNode(v))
         orderedList[i] = G.getIndexFromNode(v);

     }//end makeOrderedList function






   private static int enum(int Index)
     {
      int i;

      for (i=0;  ;i++)
         if (orderedList[i] == Index)
            return i;
     }//end  enum function






   private static void find_l_and_k(Graph G)
     {
      int i,j;
      long diam;
      double Lo;

      Ko = 0;
      L = new double[G.numberOfNodes()][G.numberOfNodes()];
      K = new double[G.numberOfNodes()][G.numberOfNodes()];
      /* Find the diameter of the graph */
      diam = D[0][0];
      for (i=0;i<G.numberOfNodes();i++)
         for (j=i+1;j<G.numberOfNodes();j++)
           {
            if ((diam < D[i][j]) && (D[i][j] < Long.MAX_VALUE))
               diam = D[i][j];
            Ko+= D[i][j];
           }
      Ko = Ko/G.numberOfNodes();

      for (i=0;i<G.numberOfNodes();i++)
         for (j=i+1;j<G.numberOfNodes();j++)
           {
            /* Lo is desired edge length */
        /*    Lo = avgEdgeLength(G);  */         /* this Lo makes it grow */
        /*  Lo = Math.sqrt(findArea(G))/diam;*/  /* this Lo makes it shrink */
            Lo = Math.sqrt(rect.width*rect.height/4)/diam;
            L[i][j] = Lo*D[i][j];  
            L[j][i] = L[i][j];
            if (D[i][j] < Long.MAX_VALUE)
             /*  K[i][j] = Ko/(D[i][j]*D[i][j]);*/
           K[i][j] = Ko*Ko/(D[i][j]*D[i][j]);
            else
               K[i][j] = 0;
            K[j][i] = K[i][j];
           }
     }//end find_l_and_k function





   private static double findDelta2(Graph G,Node v)
     {
      findPartials(G,v);
      return(partial_x*partial_x+partial_y*partial_y);
     }





   private static void findPartials(Graph G,Node v)
     {
      Node u;
      int vi,ui;
      double dx,dy,dd;
      

      vi = enum(G.getIndexFromNode(v));
      partial_x=0;
      partial_y=0;
      partial_xx=0;
      partial_xy=0;
      partial_yx=0;
      partial_yy=0;
      for (u = G.firstNode(); u !=null; u = G.nextNode(u))
         if (v!=u)
           {
            ui = enum(G.getIndexFromNode(u));
            dx = v.getPosition().x - u.getPosition().x;
            dy = v.getPosition().y - u.getPosition().y;
            dd = Math.sqrt(dx*dx+dy*dy);
            partial_x+= K[vi][ui]*(dx-L[vi][ui]*dx/dd);
            partial_y+= K[vi][ui]*(dy-L[vi][ui]*dy/dd);
            partial_xx+= K[vi][ui]*(1-L[vi][ui]*dy*dy/(dd*dd*dd));
            partial_xy+= K[vi][ui]*(L[vi][ui]*dx*dy/(dd*dd*dd));
            partial_yx+= K[vi][ui]*(L[vi][ui]*dy*dx/(dd*dd*dd));
            partial_yy+= K[vi][ui]*(1-L[vi][ui]*dx*dx/(dd*dd*dd));
           }
     }





   private static double TempFunction(double del, double times)
     {
      return (0.5 * del  +  0.5 * times);
     }





   private static double findE(Graph G)
     {
      Node u,v;
      int vi,ui;
      double dx,dy,dd,dif;
      double total=0;

      for (v = G.firstNode(); v !=null; v = G.nextNode(v))
        {
         vi = enum(G.getIndexFromNode(v));
         for (u = G.nextNode(v); u !=null; u = G.nextNode(u))
           {
            ui = enum(G.getIndexFromNode(u));
            dx = v.getPosition().x - u.getPosition().x;
            dy = v.getPosition().y - u.getPosition().y;
            dd = Math.sqrt(dx*dx+dy*dy);
            dif = dd - L[vi][ui];
            total+= K[vi][ui]*dif*dif;
           }
        }
      return total/2;
     }






   private static void MoveToNewPosition(Graph G)
     {
      double A,B,C,D,E,F,dx,dy;
      double xpos,ypos,xfrac,yfrac;

      xpos = mv.getPosition().x;
      ypos = mv.getPosition().y;



      int mvi = enum(G.getIndexFromNode(mv));
      findPartials(G,mv);
            A = partial_xx;
            B = partial_xy;
            C = -(partial_x);
            D = partial_yx;
            E = partial_yy;
            F = -(partial_y);
      dy = (A*F-C*D)/(A*E-B*D);
      dx = (C*E-B*F)/(A*E-B*D);

      while ( (dy != 0) && (dx != 0) )
        {
         if (xpos + dx < xmin)
            xfrac = (xpos - xmin)/dx;
         else if (xpos + dx > xmax)
            xfrac = (xmax - xpos)/dx;
         else
            xfrac = 1;


         if (ypos + dy < ymin)
            yfrac = (ypos - ymin)/dy;
         else if (ypos + dy > ymax)
            yfrac = (ymax - ypos)/dy;
         else
            yfrac = 1;
double x = xpos+dx;
double y = ypos+dy;

         if (xfrac < yfrac)
           {
            xpos += dx * xfrac;
            dx *= (xfrac - 1);
            ypos += dy * xfrac;
            dy *= (1 - xfrac);
           }
         else if (yfrac < xfrac)
           {
            ypos += dy * yfrac;
            dy *= (yfrac - 1);
            xpos += dx * yfrac;
            dx *= (1 - yfrac);
           }
         else
           {
            xpos += dx;
            dx = 0;
            ypos += dy;
            dy = 0;
           }
        }//end while

      mv.setPosition(xpos,ypos);
     }




   private static void MoveToNewPosition1(Graph G)
     {
      double A,B,C,D,E,F,dx,dy;
      double xpos,ypos,xfrac,yfrac;

      xpos = mv.getPosition().x;
      ypos = mv.getPosition().y;



      int mvi = enum(G.getIndexFromNode(mv));
      findPartials(G,mv);
            A = partial_xx;
            B = partial_xy;
            C = -(partial_x);
            D = partial_yx;
            E = partial_yy;
            F = -(partial_y);
      dy = (A*F-C*D)/(A*E-B*D);
      dx = (C*E-B*F)/(A*E-B*D);

         if (xpos + dx < xmin)
            xfrac = (xpos - xmin)/dx;
         else if (xpos + dx > xmax)
            xfrac = (xmax - xpos)/dx;
         else
            xfrac = 1;


         if (ypos + dy < ymin)
            yfrac = (ypos - ymin)/dy;
         else if (ypos + dy > ymax)
            yfrac = (ymax - ypos)/dy;
         else
            yfrac = 1;

         if (xfrac < yfrac)
           {
            yfrac = xfrac;
           }
         else if (yfrac < xfrac)
           {
            xfrac = yfrac;
           }
double x = xpos+dx;
double y = ypos+dy;

            xpos += dx*xfrac;
            dx = 0;
            ypos += dy*yfrac;
            dy = 0;


      mv.setPosition(xpos,ypos);
     }



   private static void MoveToNewPosition2(Graph G)
     {
      double A,B,C,D,E,F,dx,dy;
      double xpos,ypos,xfrac,yfrac;

      xpos = mv.getPosition().x;
      ypos = mv.getPosition().y;



      int mvi = enum(G.getIndexFromNode(mv));
      findPartials(G,mv);
            A = partial_xx;
            B = partial_xy;
            C = -(partial_x);
            D = partial_yx;
            E = partial_yy;
            F = -(partial_y);
      dy = (A*F-C*D)/(A*E-B*D);
      dx = (C*E-B*F)/(A*E-B*D);



      mv.setPosition(xpos+dx,ypos+dy);
     }



   private static void CheckPositions(Graph G)
     {
      Node v,u;
      int vi,ui;
      double rand;
      boolean OK = false;

      while(!OK)
        {
         OK = true;
         for (v = G.firstNode(); v != null; v = G.nextNode(v))
            for (u = G.nextNode(v); u != null; u = G.nextNode(u))
               if ((v.getPosition().x == u.getPosition().x)
                  && (v.getPosition().y == u.getPosition().y))
                 {
                  vi = enum(G.getIndexFromNode(v));
                  ui = enum(G.getIndexFromNode(u));
                  rand = (1.4*Math.random() - 1) * L[vi][ui];
                  u.setPosition(u.getPosition().x + rand , u.getPosition().y);
                  rand = (1.4*Math.random() - 1) * L[vi][ui];
                  u.setPosition(u.getPosition().x , u.getPosition().y + rand);
                  OK = false;
                 }
        }
     }


  }//end class KK

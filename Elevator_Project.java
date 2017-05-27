import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.util.*;
import java.io.*;

public class Elevator_Project extends JApplet
{
	Image Buffer;
	Graphics canvas;
	int frame=0;
	
	int stories=15;
	int shafts=8;

	int width=800;
	int height=700;

	int storyheight=40;
	int floorwidth=30;

	int elewidth=20;
	int eleheight=30;

	DrawPanel drawpanel=new DrawPanel();

	Runnable[] runarr=new ShaftRun[shafts];
	Thread[] threadarr=new Thread[shafts];
	Elevator[] earr=new Elevator[shafts];

	ArrayList<LinkedList<Passenger>> floorarr=new ArrayList<LinkedList<Passenger>>(stories);

	boolean uprequest[]=new boolean [stories];
	boolean downrequest[]=new boolean [stories];
	
	JTextField startjtf = new JTextField(3);
	JTextField destjtf = new JTextField(3);
	JTextField rjtf = new JTextField(3);
	JTextField gjtf = new JTextField(3);
	JTextField bjtf = new JTextField(3);

	public void init()
	{
		Buffer=createImage(width,height);
		canvas=Buffer.getGraphics();
		new Elevator_Project();

		for(int k=0;k<stories;k++)
		{
			uprequest[k]=false;
			downrequest[k]=false;
		}

		floorarr.ensureCapacity(stories);

		for(int k=0;k<stories;k++)
		{
			floorarr.add(new LinkedList<Passenger>());
		}

		//create the passenger spawner runnable object, put that into a thread, then start it
		PassengerSpawner passrun=new PassengerSpawner();
		Thread passthread=new Thread(passrun);
		passthread.start();

		//create an elevator object for each shaft
		for(int k=0;k<shafts;k++)
		{
			earr[k]=new Elevator(0,k);
		}
		//put the elevator object into a runnable object
		for(int k=0;k<shafts;k++)
		{
			runarr[k]=new ShaftRun(earr[k]);
		}
		//put the runnable object into a thread
		for(int k=0;k<shafts;k++)
		{
			threadarr[k]=new Thread(runarr[k]);
		}
		//start all the threads with elevators in them
		for(int k=0;k<shafts;k++)
		{
			threadarr[k].start();
		}

		//create the masterrun runnable object, put that into a thread, then start it
		MasterRun master=new MasterRun();
		Thread masterthread=new Thread(master);
		masterthread.start();

	}

	public Elevator_Project()
	{
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		p.add(new JLabel("Manually Create a Passenger\t"));
		
		p.add(new JLabel("Starting Story[0, stories): "));
		p.add(startjtf);
		startjtf.addActionListener(new ManualSpawnerListener());
		
		p.add(new JLabel("Destination Story[0, stories): "));
		p.add(destjtf);
		destjtf.addActionListener(new ManualSpawnerListener());
		
		p.add(new JLabel("Red[0,255]: "));
		p.add(rjtf);
		rjtf.addActionListener(new ManualSpawnerListener());
		
		p.add(new JLabel("Green[0,255]: "));
		p.add(gjtf);
		gjtf.addActionListener(new ManualSpawnerListener());
		
		p.add(new JLabel("Blue[0,255]: "));
		p.add(bjtf);
		bjtf.addActionListener(new ManualSpawnerListener());
		
		p.add(new JLabel(" ) Type in \"-1\" in any field to randomize."));
		
		p.add(new JLabel("start != destination"));
		
		this.setLayout(new BorderLayout());
		this.add(drawpanel,BorderLayout.CENTER);
		this.add(p, BorderLayout.SOUTH);
		this.setFocusable(true);
	}


	class PassengerSpawner implements Runnable
	{
		public void run()
		{
			System.out.println("runnin PassengerSpawner ");

			while(true)
			{
				int start=0;
				int destination=0;
				do
				{
					start=(int)(Math.random()*stories);
					destination=(int)(Math.random()*stories);
				}
				while(start==destination);

				int R = (int)(Math.random()*256);
				int G = (int)(Math.random()*256);
				int B= (int)(Math.random()*256);
				Color color = new Color(R, G, B);

				System.out.println("passenger spawner thread created a passenger, start="+start+" destination="+destination);

				floorarr.get(start).add(new Passenger(start,destination, color));

				try
				{
					Thread.sleep(2000);
				}
				catch(InterruptedException ex)
				{
					System.out.println("Error with sleep in PassengerSpawner");
				}
			}
		}
	}

	class ShaftRun implements Runnable
	{
		private Elevator el;

		public ShaftRun(Elevator e)
		{
			el=e;
		}

		public void run()
		{
			System.out.println("runnin ShaftRun "+el.shaftnum);

			while(true)
			{
				//while idle
				while(el.getElevatorState()==0)
				{
					for(int k=0;k<stories;k++)
					{
						if(uprequest[k]||downrequest[k])
						{
							if(k>el.getCurrentFloor())
							{
								el.setElevatorState(1);
								break;
							}
							if(k<el.getCurrentFloor())
							{
								el.setElevatorState(-1);
								break;
							}
							if(k==el.getCurrentFloor())
							{
								if(uprequest[k])
								{
									el.setElevatorState(1);
								}
								if(downrequest[k])
								{
									el.setElevatorState(-1);
								}
								break;
							}
						}
					}
				}

				//while going up
				while(el.getElevatorState()==1)
				{
					//all the passengers on the elevator that have arrived at their destination floor get off
					for(int k=0;k<el.getPassengersOnEl().size();k++)
					{
						if(el.getPassengersOnEl().get(k).getDestinationStory()==el.getCurrentFloor())
						{
							el.getPassengersOnEl().get(k).exitElevator(el);
							el.getPassengersOnEl().remove(k);
							k--;
						}
					}

					//see if any more passengers need to get on

					if(uprequest[el.getCurrentFloor()])
					{
						synchronized(uprequest)
						{
							uprequest[el.getCurrentFloor()]=false;
						}

						synchronized(floorarr.get(el.getCurrentFloor()))
						{
							//all passengers on this floor that want to go up will enter Elevator.  the ones that want to go down are ignored
							for(int k=0;k<floorarr.get(el.getCurrentFloor()).size();k++)
							{
								if(floorarr.get(el.getCurrentFloor()).get(k).getUpOrDown()==1)
								{
									//System.out.println("Size of linked list before "+floorarr.get(el.getCurrentFloor()).size());
									el.getPassengersOnEl().add(floorarr.get(el.getCurrentFloor()).get(k));
									floorarr.get(el.getCurrentFloor()).get(k).enterElevator(el,k);
									floorarr.get(el.getCurrentFloor()).remove(k);
									k--;
									//System.out.println("Size of linked list after "+floorarr.get(el.getCurrentFloor()).size());
								}
							}
						}
						
						synchronized(uprequest)
						{
							uprequest[el.getCurrentFloor()]=false;
						}
					}

					boolean requestsabove=false;
					for(int k=el.getCurrentFloor();k<stories;k++)
					{
						if(uprequest[k]==true||downrequest[k]==true)
						{
							requestsabove=true;
						}
					}


					//if not on the top floor AND there are passengers on the elevator (who want to go up), then go up
					//else if there are more requests above, keep going up anyways.
					//else go idle
					if(el.getCurrentFloor()<stories-1)
					{
						if(el.getPassengersOnEl().size()>0)
						{
							el.goUp();
						}
						else if(requestsabove)
						{
							el.goUp();
						}
						else
						{
							el.setElevatorState(0);
							System.out.println("Elevator "+el.getShaftnum()+" set idle when going up 1");
						}
					}
					else
					{
						el.setElevatorState(0);
						System.out.println("Elevator "+el.getShaftnum()+" set idle when going up 2");
					}
				}

				//while going down
				while(el.getElevatorState()==-1)
				{
					//all the passengers on the elevator that have arrived at their destination floor get off
					for(int k=0;k<el.getPassengersOnEl().size();k++)
					{
						if(el.getPassengersOnEl().get(k).getDestinationStory()==el.getCurrentFloor())
						{
							el.getPassengersOnEl().get(k).exitElevator(el);
						}
					}

					//see if any more passengers need to get on
					if(downrequest[el.getCurrentFloor()])
					{
						synchronized(downrequest)
						{
							downrequest[el.getCurrentFloor()]=false;
						}

						synchronized(floorarr.get(el.getCurrentFloor()))
						{
							//all passengers on this floor that want to go down will enter Elevator.  the ones that want to go up are ignored
							for(int k=0;k<floorarr.get(el.getCurrentFloor()).size();k++)
							{
								if(floorarr.get(el.getCurrentFloor()).get(k).getUpOrDown()==-1)
								{
									el.getPassengersOnEl().add(floorarr.get(el.getCurrentFloor()).get(k));
									floorarr.get(el.getCurrentFloor()).get(k).enterElevator(el,k);
									floorarr.get(el.getCurrentFloor()).remove(k);
									k--;
								}
							}
						}
					}

					boolean requestsbelow=false;
					for(int k=el.getCurrentFloor();k>=0;k--)
					{
						if(uprequest[k]==true||downrequest[k]==true)
						{
							requestsbelow=true;
						}
					}

					//if not on the bottom floor AND there are passengers on the elevator (who want to go down), then go down
					//else if there are more requests below, keep going down anyways.
					//else go idle
					if(el.getCurrentFloor()>0)
					{
						if(el.getPassengersOnEl().size()>0)
						{
							el.goDown();
						}
						else if(requestsbelow)
						{
							el.goDown();
						}
						else
						{
							el.setElevatorState(0);
							System.out.println("Elevator "+el.getShaftnum()+" set idle when going down 1");
						}

					}
					else
					{
						el.setElevatorState(0);
						System.out.println("Elevator "+el.getShaftnum()+" set idle when going down 2");
					}
				}
			}
		}
	}

	class MasterRun implements Runnable
	{
		public void run()
		{
			while(true)
			{
				drawpanel.clear();
				drawpanel.drawBuilding();

				//draw elevators in all shafts
				for(int k=0;k<shafts;k++)
				{
					drawpanel.drawElevator(earr[k]);
				}

				//draw all the passengers on all floors that are still waiting for elevator or getting on
				for(int k=0;k<stories;k++)
				{
					//draw all the ones that are waiting first, then draw the ones going into the elevator
					for(int j=0;j<floorarr.get(k).size();j++)
					{
						if(floorarr.get(k).get(j).getStatus()==1)
						{
							drawpanel.drawPassenger(floorarr.get(k).get(j), j*20);
						}
					}
					//draw the ones that are going into elevator
					for(int j=0;j<floorarr.get(k).size();j++)
					{
						if(floorarr.get(k).get(j).getStatus()==0)
						{
							drawpanel.drawPassenger(floorarr.get(k).get(j), j*20);
						}
					}
				}

				//draw all passengers that are exiting elevator
				for(int k=0;k<shafts;k++)
				{
					for(int j=0;j<earr[k].getPassengersOnEl().size();j++)
					{
						//if(earr[k].getPassengersOnEl().get(j).getStatus()==1)
						if(earr[k].getPassengersOnEl().get(j).getDestinationStory()==earr[k].getCurrentFloor())
						{
							drawpanel.drawPassenger(earr[k].getPassengersOnEl().get(j),0);
						}
					}
				}

				//draw all the floorstate arrows
				for(int k=0;k<stories;k++)
				{
					if(uprequest[k])
					{
						drawpanel.drawUpRequest(k);
					}
					if(downrequest[k])
					{
						drawpanel.drawDownRequest(k);
					}
				}

				repaint();

				try
				{
					Thread.sleep(20);
				}
				catch(InterruptedException ex)
				{
					System.out.println("Error with sleep in MasterRun");
				}
			}
		}
	}

	class DrawPanel extends JPanel
	{
		public void paint(Graphics g)
		{
			g.drawImage(Buffer,0,0, this);
			frame++;
			System.out.println("repaint"+frame);
		}

		public void clear()
		{
			canvas.setColor(Color.WHITE);
			canvas.fillRect(0,0,width,height);
		}

		public void drawElevator(Elevator el)
		{
			canvas.setColor(Color.BLACK);

			canvas.fillRect(
				10+5+floorwidth*el.getShaftnum(),
				10+10+storyheight*(stories-1)-storyheight*el.getCurrentFloor()-el.getAnimationOffset(),
				elewidth,
				eleheight);

		}

		public void drawPassenger(Passenger p, int clearance)
		{
			canvas.setColor(p.getColor());
			if(p.getStatus()==1)
			{
				canvas.fillOval(
					10+floorwidth*shafts+30+clearance-p.getPassengerAnimationOffset(),
					10+10+storyheight*(stories-1)-storyheight*p.getStartStory(),
					20,
					20);
			}
			if(p.getStatus()==0)
			{
				canvas.fillOval(
					10+floorwidth*shafts+30+clearance-p.getPassengerAnimationOffset(),
					10+10+storyheight*(stories-1)-storyheight*p.getStartStory(),
					20,
					20);
			}
			if(p.getStatus()==-1)
			{
				canvas.fillOval(
					p.getPassengerAnimationOffset(),
					10+10+storyheight*(stories-1)-storyheight*p.getDestinationStory(),
					20,
					20);
			}
		}

		public void drawUpRequest(int floor)
		{
			canvas.setColor(Color.RED);

			int xcoords[]={10+30*shafts+1,10+30*shafts+1+14,10+30*shafts+1+14+14};
			int ycoords[]={10+40*(stories-floor-1)+18,10+40*(stories-floor-1)+5,10+40*(stories-floor-1)+18};

			canvas.fillPolygon(xcoords,ycoords,3);
		}

		public void drawDownRequest(int floor)
		{
			canvas.setColor(Color.RED);

			int xcoords[]={10+30*shafts+1,10+30*shafts+1+14,10+30*shafts+1+14+14};
			int ycoords[]={10+40*(stories-floor-1)+22,10+40*(stories-floor-1)+35,10+40*(stories-floor-1)+22};

			canvas.fillPolygon(xcoords,ycoords,3);
		}

		public void drawBuilding()
		{
			canvas.setColor(Color.BLACK);

			for(int k=0;k<shafts;k++)
			{
				canvas.drawRect(10+30*k,10,30,40*stories);
			}

			for(int k=0;k<stories;k++)
			{
				canvas.drawLine(10,10+40*k,10+30*shafts,10+40*k);
			}
		}
	}

	class Elevator
	{
		private int currentfloor;
		private int shaftnum;
		private int anioff;

		//1 means the elevator is going up
		//-1 means the elevator is going down
		//0 means the elevator is idle
		//2 means the elevator is responding to an up request, but currently has no passengers
		//-2 means the elevator is responding to a down request, but currently has no passengers
		private int elstate;

		private LinkedList<Passenger> passengersonel=new LinkedList<Passenger>();

		public Elevator(int c, int s)
		{
			currentfloor=c;
			shaftnum=s;
			anioff=0;
			elstate=0;
		}

		public int getCurrentFloor()
		{
			return currentfloor;
		}

		public int getShaftnum()
		{
			return shaftnum;
		}

		public int getAnimationOffset()
		{
			return anioff;
		}

		public int getElevatorState()
		{
			return elstate;
		}

		public void setElevatorState(int state)
		{
			elstate=state;
		}

		public LinkedList<Passenger> getPassengersOnEl()
		{
			return passengersonel;
		}

		public void goUp()
		{
			for(int k=0;k<storyheight;k++)
			{
				anioff=k;

				try
				{
					Thread.sleep(20);
				}
				catch(InterruptedException ex)
				{
					System.out.println();
				}

			}
			anioff=0;

			currentfloor++;

		}

		public void goDown()
		{
			for(int k=0;k<=storyheight;k++)
			{
				//drawpanel.drawElevator(this.shaftnum, -k);
				anioff=-k;

				try
				{
					Thread.sleep(20);

				}
				catch(InterruptedException ex)
				{
					System.out.println();
				}

			}
			anioff=0;

			currentfloor--;

		}
	}

	class Passenger
	{
		private int startstory;
		private int destinationstory;
		private Color co;

		//1 if go up, -1 if go down
		private int upordown;

		//1 if waiting, 0 if entering elevator, -1 if exiting elevator
		private int status;

		private int passanioff;

		public Passenger(int s, int d, Color c)
		{
			startstory=s;
			destinationstory=d;
			co=c;

			if(destinationstory>startstory)
			{
				upordown=1;
				uprequest[startstory]=true;
			}
			if(destinationstory<startstory)
			{
				upordown=-1;
				downrequest[startstory]=true;
			}

			status=1;
			passanioff=0;

		}

		public void enterElevator(Elevator ele, int index)
		{
			status=0;

			//(10+floorwidth*shafts+30+index*20)-(10+floorwidth*el.getShaftnum())
			//floorarr.get(el.getCurrentFloor()).get(index)
			for(int k=0;k<(10+floorwidth*shafts+30+index*20)-(10+floorwidth*ele.getShaftnum());k=k+2)
			{
				passanioff=k;

				try
				{
					Thread.sleep(20);
				}
				catch(InterruptedException ex)
				{
					System.out.println();
				}
			}
			passanioff=0;
		}

		public void exitElevator(Elevator ele)
		{
			status=-1;
			
			for(int k=10+floorwidth*ele.getShaftnum()+5;k>-20;k=k-2)
			{
				passanioff=k;
				
				try
				{
					Thread.sleep(20);
				}
				catch(InterruptedException ex)
				{
					System.out.println();
				}
			}
		}

		public int getStartStory()
		{
			return startstory;
		}

		public int getDestinationStory()
		{
			return destinationstory;
		}

		public int getUpOrDown()
		{
			return upordown;
		}

		public int getStatus()
		{
			return status;
		}

		public Color getColor()
		{
			return co;
		}

		public int getPassengerAnimationOffset()
		{
			return passanioff;
		}
	}
	
	class ManualSpawnerListener implements ActionListener
	{
		
		public void actionPerformed(ActionEvent e)
		{
			int start=-1;
			int dest=-1;
			int r=-1;
			int g=-1;
			int b=-1;
			
			start=Integer.parseInt(startjtf.getText().trim());
			
			dest=Integer.parseInt(destjtf.getText().trim());
			
			r=Integer.parseInt(rjtf.getText().trim());
			g=Integer.parseInt(gjtf.getText().trim());
			b=Integer.parseInt(bjtf.getText().trim());
			
			
			if(start==-1)
			{
				do
				{
					start=(int)(Math.random()*stories);
					dest=(int)(Math.random()*stories);
				}
				while(start==dest);
			}
			
			if(dest==-1)
			{
				do
				{
					start=(int)(Math.random()*stories);
					dest=(int)(Math.random()*stories);
				}
				while(start==dest);
			}
			
			if(r==-1)
			{
				r = (int)(Math.random()*256);
			}
			if(g==-1)
			{
				g = (int)(Math.random()*256);
			}
			if(b==-1)
			{
				b = (int)(Math.random()*256);
			}
			
			Color color = new Color(r,g,b);
			
			if(start!=dest)
			{
				floorarr.get(start).add(new Passenger(start,dest, color));
			}
		}
	}
}
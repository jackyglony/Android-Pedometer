package ca.uwaterloo.lab4_202_19;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.text.Editable;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import ca.uwaterloo.mapper.*;
import android.view.View.OnClickListener;

public class MainActivity extends Activity implements PositionListener
{
	//Creates MaphView object for map
	MapView mv;
	
	//Creates empty Finite State Machine object
	FSM machine=new FSM();
	
	//Creates global float array for inter-sensor communication
	float VP[]=new float[3];
	
	boolean enable = false;
	
	//False = stop
	//True  = reset
	boolean Bstate = false;
	
	//Calibration
	float user_input;
	
	//Origin and Destination points
	PointF dest;
	PointF orig;
	
	//Current X Y coordinate
	float userX;
	float userY;
	PointF currentPoint;
	PointF futurePoint;
	
	float Fy;
	float Fx;
	
	//WayPoints
	PointF A = new PointF(3f, 9f);
	PointF B = new PointF(7.4f, 9f);
	PointF C = new PointF(11.5f, 9f);
	PointF D = new PointF(15.5f, 9f);
	
	//Variables for Path Finding
	boolean Oset=false;
	boolean Dset=false;
	List<InterceptPoint> pathWallClipper = new ArrayList<InterceptPoint>();
	
	
	//Prevent wall clipping
	LineSegment bearing;
	List<InterceptPoint> wallClipper = new ArrayList<InterceptPoint>();
	
	//Trace Path walked
	List<PointF> userPath = new ArrayList<PointF>();
	
	
	 NavigationalMap map;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//Locks orientation in Portrait
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		//Creates TextView Header
		TextView title = (TextView) findViewById(R.id.header);
	    title.setText("Pedometer");
	    
	    //Creates the parent LinearLayout object (Stacked Vertically)
	    LinearLayout l = (LinearLayout) findViewById(R.id.layout);
	    l.setOrientation(LinearLayout.VERTICAL);
	    
	    //Text Fields
	    TextView accText = new TextView(getApplicationContext());
	    l.addView(accText);
	    
	    
	    TextView stepsText = new TextView(getApplicationContext());
	    l.addView(stepsText);
	    stepsText.setText("Total Steps: 0");
	    
	    TextView dirText = new TextView(getApplicationContext());
	    l.addView(dirText);
	    dirText.setText("\nSteps North: 0.00\nSteps East: 0.00");

	    //LinearLayout object for adding special features (non-text)
	    LinearLayout layout = ((LinearLayout)findViewById(R.id.layout));

	    
	    //Adds map to layout
	    mv = new MapView(getApplicationContext(), 1000, 750, 55, 55);
	    registerForContextMenu(mv);
	    map = MapLoader.loadMap(getExternalFilesDir(null), "Lab-room-peninsula.svg");
	    mv.setMap(map);
	    layout.addView(mv);
	    mv.setVisibility(View.VISIBLE);
	    
	    
	    
	    mv.addListener(this); 

	   
		//Calls the sensor system for sensor management
	    SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
	    
	    //Creates Accelerometer Sensor
	    Sensor accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	    SensorEventListener accListen = new ASensorEventListener(accText, stepsText, dirText); 
	    sensorManager.registerListener(accListen, accSensor,SensorManager.SENSOR_DELAY_FASTEST);
	        
	    //Creates Rotation Sensor
	    Sensor rotSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
	    SensorEventListener rotListen = new RSensorEventListener(accText, stepsText, dirText); 
	    sensorManager.registerListener(rotListen, rotSensor,SensorManager.SENSOR_DELAY_FASTEST);
	    
	    //Start Button
	    Button startB = new Button(getApplicationContext());
	    //Stop/Reset Button
	    final Button newB = new Button(getApplicationContext());
	    
	    
	    //Start Button Details
	    layout.addView(startB);
		startB.setText("Start");
		startB.setOnClickListener(new OnClickListener() {
			 
			@Override
			public void onClick(View arg0) {
				//Gives access to sensor listeners
				enable=true;
				
				//Changes Reset Button to Stop 
				newB.setText("Stop");
				
				//Makes Bstate behaive as stop
				Bstate=false;
				machine.findPath(userX, userY);
			}
 
		});
	    
	    
	    //Stop/Reset Button Details
	    layout.addView(newB);
		newB.setText("Stop");
		newB.setOnClickListener(new OnClickListener(){
			public void onClick(View v) 
			{
				if(Bstate)
				{
					//Changes button text to stop
					newB.setText("Stop");
					
					//Allows access to sensor listeners
					enable=true;
					
					//Resets Step fields 
					machine.steps=0;
					machine.stepN=0d;
					machine.stepE=0d;
					machine.s=String.format(Locale.getDefault(),"Steps North: 0.00%nSteps East: 0.00" );
					
					//Resets all points
					dest.x=0;
					dest.y=0;
					orig.x=0;
					orig.y=0;
					userX=0;
					userY=0;
					Fy= 0;
					Fx= 0;
					
					//Clears array of path travelled
					int l = userPath.size();
					for(int i=0; i<l; i++)
					{
						userPath.remove(0);
					}
					//erases previous path
					mv.setUserPath(userPath);
					
					
					l = wallClipper.size();
					for(int i=0; i<l; i++)
					{
						wallClipper.remove(0);
					}
		    		
					
					//Stops access to sensor listeners
					enable=false;
					
					//Flips Bstate
					Bstate=false;
				}
				else
				{
					//Flips Bstate 
					Bstate=true;
					
					//Changes button text to Reset
					newB.setText("Reset");
					
					//Stops access to sensor listeners
					enable=false;
				}
				
			}
		}); 
		
		
		// final TextView y = new TextView(getApplicationContext());
		  //  l.addView(y);
		
		/*
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Welcome");
		alert.setMessage("Please enter stride length");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		alert.setView(input);
		//REFERENCE CODE!
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int whichButton) 
			{
				Editable value = input.getText();
				// Do something with value!
				String z =""+value;
		
				user_input=Float.parseFloat(z);
				y.setText(""+user_input);
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() 
		{
		  public void onClick(DialogInterface dialog, int whichButton) 
		  {
		    // Canceled.
		  }
		});

		alert.show();*/
		    user_input=1;
    
	}
	
	
	
	
	
	
	
	
	
	/*
	 * Position Listener implemented methods 
	 */
	@Override
	public void originChanged(MapView source, PointF loc) {
		// TODO Auto-generated method stub
		source.setUserPoint(loc);
		
		Oset=true;
		
		orig=loc;
		userX=orig.x;
		userY=orig.y;
		currentPoint=new PointF(userX, userY);
		userPath.add(currentPoint);
		
	}

	@Override
	public void destinationChanged(MapView source, PointF destination) {
		// TODO Auto-generated method stub
		dest=destination;
		Dset=true;
	}
	
	
	
	
	
	
	
	
	
	
	//Methods for the map
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) 
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		mv.onCreateContextMenu(menu, v, menuInfo); 
	}
	@Override
	public boolean onContextItemSelected(MenuItem item) 
	{
		return super.onContextItemSelected(item) || mv.onContextItemSelected(item); 
	}
	
	
	//APHA
	float updateFreq = 30; // update speed
    float cutOffFreq = 2.5f;
    float RC = 1.0f / cutOffFreq;//0.4
    float dt = 1.0f / updateFreq;//0.0333
    
    //Lowpass filter
	float lowpass (float current, float prevH)
	{
		// dt/(dt+RC)
		float alpha = dt/(dt+RC);//0.0769
		float output = prevH + alpha * (current-prevH);
		return output;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	
	//Creates SensorEventListener
	class RSensorEventListener implements SensorEventListener
	{
		//Instantiate text fields
	    TextView output;
	    TextView steps;
	    TextView dir;
	    
	    
	    //Variable for Rotation Matrix
	    float[] R = new float[16];
	    
	    //Variables for GetOrientatino
	    float[] values = new float [3];
	        
	    //Object Constructor
	    public RSensorEventListener(TextView outputView, TextView stepsText, TextView dirText)
	    {
	    	output=outputView;
	    	steps=stepsText;
	    	dir=dirText;
	    }

	    //A method required for this class
	    public void onAccuracyChanged(Sensor s, int i) {}
	     
		public void onSensorChanged(SensorEvent se) 
	    {
			if (se.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) 
			{
				//Set up Rotation Matrix and get Orientation
				SensorManager.getRotationMatrixFromVector(R, se.values);
				SensorManager.getOrientation(R, values);

				
				//Populate global array VP with the rotation values
				VP[0] = values[0];
				VP[1] = values[1];
				VP[2] = values[2];
				
				float z =VP[0];
				String s="";
				double degrees=z*180/3.14159265;
				
				if(z>=0 && z<=1.571)
				{
					//NE
					s=String.format(Locale.getDefault(),"Current Direction: [N %.1f E]", degrees); 
				}
				else if(z>1.571 && z<=3.14)
				{
					//SE
					s=String.format(Locale.getDefault(),"Current Direction: [S %.1f E]" , Math.abs(degrees-180)); 
				}
				else if(z<0 && z>=-1.571)
				{
					//NW
					s=String.format(Locale.getDefault(),"Current Direction: [N %.1f W]" , Math.abs(degrees)); 
				}
				else if(z<-1.571 && z>-3.14)
				{
					//SW
					s=String.format(Locale.getDefault(),"Current Direction: [S %.1f W]" , Math.abs(degrees+180)); 
				}
				output.setText(s);
				
				
			}	
	    } 
	}
	class ASensorEventListener implements SensorEventListener 
	{
		//Instantiate text fields
	    TextView output;
	    TextView steps;
	    TextView dir;
	    
	    
	    //Instantiate fields for filtered values
	    float lowPassZ=0;
	    float lowPassX=0;
	    float lowPassY=0;
	     
	    //Object Constructor
	    public ASensorEventListener(TextView outputView, TextView stepsText, TextView dirText)
	    {
	    	output=outputView;
	    	steps=stepsText;
	    	dir=dirText;
	    }

	    //A method required for this class
	    public void onAccuracyChanged(Sensor s, int i) {}
	     
	    
		public void onSensorChanged(SensorEvent se) 
	    {
		
			if ((se.sensor.getType() == Sensor.TYPE_ACCELEROMETER) && enable) 
	    	{
				//Send Accelerometer values through lowpass filter
	    		lowPassX = lowpass(se.values[0], lowPassX);
	    		lowPassY = lowpass(se.values[1], lowPassY);
	    		lowPassZ = lowpass(se.values[2], lowPassZ);
	    		
	    		//Creates the prediction line
	    		Fy= (float) (userY - (user_input)*Math.cos(VP[0]));
				Fx= (float) (userX + (user_input)*Math.sin(VP[0]));
	    		futurePoint = new PointF(Fx, Fy);

	    	}
			
			
			
			//Current Location
    		mv.setUserPoint(userX,userY);
    		
			//Output text for steps
    		String a ="Total Steps: "+ machine.stepCount(lowPassX, lowPassY, lowPassZ, VP[0], dir)+"\n";
			steps.setText(a);
			dir.setText(machine.s);	
			
	    } 
	}

	
	//Finite State Machine
	class FSM 
	{
		public int steps;
		public int state;
		public int check;
		
		public double stepN;
		public double stepE;
		public double degrees;
		
		public String s=String.format(Locale.getDefault(),"Steps North: 0.00%nSteps East: 0.00" );
		
		/*
		--- State 1 : Rising 
		--- State 2 : Peak 9.7
		--- State 3 : Dip
		*/
		public FSM()
		{
			steps=0;
			state=0;
			check = 0;
			stepN=0;
			stepE=0;
		}
		public int stepCount(float dataX, float dataY, float dataZ, float rad, TextView t)
		{
			if(!(((dataX>2) || (dataX<-2)) || ((dataY>3) || (dataY<-2))))
			{
				if((dataZ>8.9) && (dataZ<9.8))
				{
					check = 0;
					if((dataZ>=11) || (dataZ<=7))
					{
						state=0;
						check=1;
					}
					
					if( (dataZ<=9) && ((state==0) || (state==3)) && (check == 0))
					{
						state=1;
					}
					else if( (dataZ>=9.7) && (state==1) )
					{
						state=2;
					}
					else if ( (dataZ<=9) && (state==2) )
					{
						
						state=3;
						checkDir(rad, t);
						//Clears array of path travelled
						int l = userPath.size();
						for(int i=0; i<l; i++)
						{
							userPath.remove(0);
						}
						findPath(userX, userY);
						steps++;	
					}
				}
			}
			return steps;
		}
		public void checkDir(float z, TextView t)
		{
			stepN= stepN + Math.cos(z);
			stepE= stepE + Math.sin(z);
			
			wallClipper= map.calculateIntersections(currentPoint, futurePoint);

    		if(wallClipper.size()!=0)
    		{
    			float xLength = wallClipper.get(0).getPoint().x - userX;
    			float yLength = wallClipper.get(0).getPoint().y  - userY;

    			if((yLength>=(userY - (user_input)*Math.cos(z))))
    			{
    				userY= (float) (userY - (user_input)*Math.cos(z));
    			}
    			else
    			{
    				userY=(wallClipper.get(0).getPoint().y+userY)/2;
    			}
    			if((xLength>=(userX + (user_input)*Math.sin(z))))
    			{
    				userX= (float) (userX + (user_input)*Math.sin(z));
    			}
    			else
    			{
    				userX=(wallClipper.get(0).getPoint().x+userX)/2;
    				
    			}
    		}
    		else
    		{
    			userY= (float) (userY - (user_input)*Math.cos(z));
    			userX= (float) (userX + (user_input)*Math.sin(z));		
    		}
    		
    		
			if(z>=0 && z<=1.571)
			{
				//NE
				s=String.format(Locale.getDefault(),"Steps North: %.2f%nSteps East: %.2f" , stepN, stepE); 
			}
			else if(z>1.571 && z<=3.14)
			{
				//SE	
				s=String.format(Locale.getDefault(),"Steps North: %.2f%nSteps East: %.2f" , stepN, stepE); 
			}
			else if(z<0 && z>=-1.571)
			{
				//NW
				s=String.format(Locale.getDefault(),"Steps North: %.2f%nSteps East: %.2f" , stepN, stepE); 
			}
			else if(z<-1.571 && z>-3.14)
			{
				//SW
				s=String.format(Locale.getDefault(),"Steps North: %.2f%nSteps East: %.2f" , stepN, stepE); 
			}
		}
		
		
		public void findPath(float tempX, float tempY)
		{
			String userQuad=testQuad(userX);
			String destQuad=testQuad(dest.x);
			
			currentPoint=new PointF(userX, userY);
			userPath.add(currentPoint);
			
			pathWallClipper= map.calculateIntersections(currentPoint, dest);
			
			
			if(userQuad==destQuad && pathWallClipper.size()==0)
			{
				
			}
			else if(userQuad=="A")
			{
				userPath.add(A);
			}
			else if (userQuad=="B")
			{
				userPath.add(B);
			}
			else if(userQuad=="C")
			{
				userPath.add(C);
			}
			else if(userQuad=="D")
			{
				userPath.add(D);
			}
			
			if(userQuad==destQuad && pathWallClipper.size()==0)
			{
				
			}
			else if(destQuad=="A")
			{
				userPath.add(A);
			}
			else if (destQuad=="B")
			{
				userPath.add(B);
			}
			else if(destQuad=="C")
			{
				userPath.add(C);
			}
			else if(destQuad=="D")
			{
				userPath.add(D);
			}
			
			userPath.add(dest);
			mv.setUserPath(userPath);	
		}
		public String testQuad(float x)
		{
			if(x<=5.4)
			{
				return "A";
			}
			else if(x>5.4 && x<=9.4)
			{
				return "B";
			}
			else if (x>9.4 && x<=13.6)
			{
				return "C";
			}
			else 
			{
				return "D";
			}
		}
		
		
		
		/*public void finished()
		{
			if((userX<=dest.x+0.5) && (userX>=dest.x-0.5) && (userY<=dest.y-0.5) && (userY>=dest.y+0.5))
			{
				//Resets Step fields 
				machine.steps=0;
				machine.stepN=0d;
				machine.stepE=0d;
				machine.s=String.format(Locale.getDefault(),"Steps North: 0.00%nSteps East: 0.00" );
				
				//Resets all points
				dest.x=0;
				dest.y=0;
				orig.x=0;
				orig.y=0;
				userX=0;
				userY=0;
				Fy= 0;
				Fx= 0;
				
				//Clears array of path travelled
				int l = userPath.size();
				for(int i=0; i<l; i++)
				{
					userPath.remove(0);
				}
				//erases previous path
				mv.setUserPath(userPath);
			}
			
		}*/
	}
	
}

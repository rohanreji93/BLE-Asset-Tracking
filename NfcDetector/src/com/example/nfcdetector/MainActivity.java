package com.example.nfcdetector;



//uncomment in three places in mainactivity.java and one place in androidmanifest.xml

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



import redis.clients.jedis.Jedis;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


@SuppressLint("NewApi") public class MainActivity extends Activity {
	protected NfcAdapter nfcAdapter;
	protected PendingIntent nfcPendingIntent;
	String tagid;
	Intent in;
	TextView t;
	String nam;
	int back_flag=0;
	int first_use;
	boolean active;
	JSONArray contacts = null,contacts1=null;
	//stroes both id and name for projects
	ArrayList<HashMap<String, String>> contactList,contactList1;
	//to load first spinner
	ArrayList<String> Loclist;
	ArrayList<String> Sitelist;
	//The id corresponding to location selected from first spinner
	int Locposition,Siteposition;
	private static final String TAG = MainActivity.class.getName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        back_flag=0;
        contactList = new ArrayList<HashMap<String, String>>();
        contactList1 = new ArrayList<HashMap<String, String>>();
        Loclist=new ArrayList<String>();
        Sitelist=new ArrayList<String>();
        Locposition=0;
        Siteposition=0;
        first_use=PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getInt("FIRST_USE",0);
        if(first_use==0)
        {
        	firstusecall();
        }
        Typeface font = Typeface.createFromAsset(getAssets(), "demo.otf");
        ActionBar bar = getActionBar();
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#34495e")));
        bar.setTitle(Html.fromHtml("<font color='#ecf0f1'>NFC Detector</font>"));
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		nfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		t=(TextView)findViewById(R.id.textView1);
		t.setTypeface(font);
		TextView t2=(TextView)findViewById(R.id.textView2);
		t2.setTypeface(font);
		/*
		 * perform on background
		 */
        if(NfcAdapter.ACTION_TECH_DISCOVERED.equals(this.getIntent().getAction()))
		{
        	back_flag=0;
        	Tag tagFromIntent = this.getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
        	Log.d(TAG, "UID: " + bin2hex(tagFromIntent.getId()));
         	tagid=bin2hex(tagFromIntent.getId());
         	t.setText("Tag: "+tagid);
         	t.setTextColor(Color.parseColor("#2c3e50"));
         	UploadASyncTask upload = new UploadASyncTask();
            upload.execute();
        			
		}
	
	}
	//commented for testing
    @SuppressLint("NewApi") public void enableForegroundMode() {
    	Log.d(TAG, "enableForegroundMode");

    	IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED); // filter for all
    	IntentFilter[] writeTagFilters = new IntentFilter[] {tagDetected};
    	nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, writeTagFilters, null);
    }

    @SuppressLint("NewApi") public void disableForegroundMode() {
    	Log.d(TAG, "disableForegroundMode");

    	nfcAdapter.disableForegroundDispatch(this);
    }


    @Override
    public void onStart() {
    	super.onStart();
    	active = true;
    } 

    @Override
    public void onStop() {
    	super.onStop();
    	active = false;
    }


/*
 * called when a tag myfare classic tag has been detected on activity start.
 * (non-Javadoc)
 * @see android.app.Activity#onNewIntent(android.content.Intent)
 */


    @Override
    public void onNewIntent(Intent intent) {
    	Log.d(TAG, "onNewIntent");
    	back_flag=0;

    	if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {		
			in=intent;
						 
			Tag tagFromIntent = in.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			Log.d(TAG, "UID: " + bin2hex(tagFromIntent.getId()));
   
		
			tagid=bin2hex(tagFromIntent.getId());
       		t.setText("Tag: "+tagid);
       		t.setTextColor(Color.parseColor("#2c3e50"));
       		
       		UploadASyncTask upload = new UploadASyncTask();
       		upload.execute();
			
    	} else {
    		Toast.makeText(getApplicationContext(),"No Tag Discovered", Toast.LENGTH_SHORT).show();
		}
    }

   /*
    * called on button click - fro testing
    */
    public void clickme(View v)
    {
    	tagid="C74AA7D6";
    	t.setText("Tag: "+tagid);
   		t.setTextColor(Color.parseColor("#2c3e50"));
   		
   		UploadASyncTask upload = new UploadASyncTask();
   		upload.execute();
    }
    static String bin2hex(byte[] data) {
    	return String.format("%0" + (data.length * 2) + "X", new BigInteger(1,data));
    }
    /*
     * called on first use
     */
    public void firstusecall()
    {
    	new GetLocation().execute();
    	
    }
    /*
     * called on firstuse - second dialog
     */
    public void firstusecall2()
    {
    	
    	
    	new GetWorkSite().execute();
    	
    }

    /*
     * called when options is clicked
     */
    public void ddemo()
    {
    	 final Dialog dialog = new Dialog(MainActivity.this);
         // Include dialog.xml file
         dialog.setContentView(R.layout.settings_view);
         // Set dialog title
         dialog.setTitle("Options");

         // set values for custom dialog components - text, image and button
        dialog.show();
       
         Button declineButton = (Button) dialog.findViewById(R.id.button1);
         // if decline button is clicked, close the custom dialog
         declineButton.setOnClickListener(new OnClickListener() {
             @Override
             public void onClick(View v) {
                 // Close dialog
                 dialog.dismiss();
                 final EditText e=new EditText(MainActivity.this);
             	
                 
             	AlertDialog.Builder alert=new AlertDialog.Builder(MainActivity.this);
             	alert.setTitle("Set Server");
             	alert.setView(e);
             	e.setText(PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString("MYIP","http://ec2-54-148-0-61.us-west-2.compute.amazonaws.com:1337/"));
             	alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
             		public void onClick(DialogInterface dialog, int whichButton) {
             			PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putString("MYIP",e.getText().toString()).commit();
             			dialog.cancel();
             			firstusecall();
             		}
             	});
             	alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
             		public void onClick(DialogInterface dialog, int whichButton) {
             			//Canceled.
             		}
             	});
             	alert.show();
                 
             }
         });

         Button declineButton1 = (Button) dialog.findViewById(R.id.button2);
         declineButton1.setOnClickListener(new OnClickListener() {
             @Override
             public void onClick(View v) {
            	  dialog.dismiss();
            	  firstusecall();
             }
         });
         Button declineButton2 = (Button) dialog.findViewById(R.id.button3);
         declineButton2.setOnClickListener(new OnClickListener() {
             @Override
             public void onClick(View v) {
            	  dialog.dismiss();
            	  firstusecall2();
             }
         });
    }
   


    @Override
    protected void onResume() {
    	Log.d(TAG, "onResume");

    	super.onResume();
    	//commented for testing
    	enableForegroundMode();
    }

    @Override	
    protected void onPause() {
    	Log.d(TAG, "onPause");

    	super.onPause();
    	//commented for testing
    	disableForegroundMode();
    }

    


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
        	ddemo();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
  
/*
 * upload scanned tag to server by http post 
 * default: http://ec2-54-148-0-61.us-west-2.compute.amazonaws.com:1337/attendance/pushtodb
       
 */
 
    String responseBody ;
    ProgressDialog pDialog;
    private class UploadASyncTask extends AsyncTask<Void,Void, String>{

    	@Override
    	protected void onPostExecute(String result) {
    		// TODO Auto-generated method stub
    		super.onPostExecute(result);
    		if(result.equals("sorry"))
    		{
    			Toast.makeText(getApplicationContext(), "Cannot send check connection",Toast.LENGTH_SHORT).show();
    		}
    		else
    		{
    			Toast.makeText(getApplicationContext(), "Delivered",Toast.LENGTH_SHORT).show();
    		}
    		
//    		if (pDialog.isShowing())
//    			pDialog.dismiss();
    		
    	}

    	@Override
    	protected void onPreExecute() {
    		// TODO Auto-generated method stub
    		super.onPreExecute();
//    		pDialog = new ProgressDialog(MainActivity.this);
//    		pDialog.setMessage("Please wait...");
//    		pDialog.setCancelable(false);
//         	pDialog.show();
    	}

    	@Override
    	protected String doInBackground(Void... params) {
            try{
            
               TelephonyManager mngr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE); 
               JSONObject location = new JSONObject();
             
               location.put("uid", tagid);
               location.put("capturedAt", System.currentTimeMillis());
               location.put("deviceid", mngr.getDeviceId());
               String siteid=PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString("SITE_NAME","HQPP0093");
               location.put("projcode", siteid);
              
                HttpClient httpclient = new DefaultHttpClient();
                String JEDIS_SERVER1 = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString("MYIP","http://ec2-54-148-0-61.us-west-2.compute.amazonaws.com:1337/");
                JEDIS_SERVER1=JEDIS_SERVER1+"attendance/pushtodb";
                URL url = new URL(JEDIS_SERVER1);
                HttpPost httpPost = new HttpPost(JEDIS_SERVER1);
                String json = "";
                json = location.toString();
                StringEntity se = new StringEntity(json);
                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                httpPost.setEntity(se);

                httpPost.setHeader("User-Agent", "NFC-DETECTOR/1.0");
                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Content-type", "application/json");

                HttpResponse response = httpclient.execute(httpPost);
                HttpEntity entity = response.getEntity();

                String jsonString = EntityUtils.toString(entity);
                Log.d("rear", jsonString);
                if(jsonString.equals("Not Found"))
                {
                	 return "sorry";
                }
                else
                	return "true";
            }catch(Exception e){
                Log.e("ERROR IN SEVER UPLOAD", e.getMessage());
                return "sorry";
           }
            
        }
    }    
    
    
    
    
    
    
    
    
    
    /*
     * the call to obtain json object of location
     */
    
    private class GetLocation extends AsyncTask<Void, Void, Void> {
    	 
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();
 
        }
 
        @Override
        protected Void doInBackground(Void... arg0) {
            // Creating service handler class instance
            ServiceHandler sh = new ServiceHandler();
            contactList = new ArrayList<HashMap<String, String>>();
            Loclist=new ArrayList<String>(); 
            //String url="http://127.prayer.php";
            String url = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString("MYIP","http://ec2-54-148-0-61.us-west-2.compute.amazonaws.com:1337/");
            url=url+"locations";
            // Making a request to url and getting response
            String jsonStr = sh.makeServiceCall(url, ServiceHandler.GET);
 
            Log.d("Response: ", "> " + jsonStr);
 
            if (jsonStr != null) {
                try {
                   // JSONObject jsonObj = new JSONObject(jsonStr);
                     
                    // Getting JSON Array node
                    contacts = new JSONArray(jsonStr);
 
                    // looping through All Contacts
                    for (int i = 0; i < contacts.length(); i++) {
                        JSONObject c = contacts.getJSONObject(i);
                         
                        String id = c.getString("id");
                        String loc = c.getString("name");
                      System.out.println("name:  "+loc);
 
                        // tmp hashmap for single contact
                        HashMap<String, String> contact = new HashMap<String, String>();
 
                        // adding each child node to HashMap key => value
                        contact.put("id", id);
                        contact.put("loc", loc);
                       
 
                        // adding contact to contact list
                        contactList.add(contact);
                        Loclist.add(loc);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e("ServiceHandler", "Couldn't get any data from the url");
            }
 
            return null;
        }
 
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();
            /**
             * Updating parsed JSON data into ListView
             * */
            
            final Spinner s=new Spinner(MainActivity.this);
        	s.setAdapter(new ArrayAdapter<String>(MainActivity.this,
        	android.R.layout.simple_spinner_dropdown_item,
        	Loclist));
        	
        	s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        	 
        	@Override
        	public void onItemSelected(AdapterView<?> arg0,
        	View arg1, int position, long arg3) {
        	// TODO Auto-generated method stub
        	// Locate the textviews in activity_main.xml
        		Locposition=Integer.parseInt(contactList.get(position).get("id"));
        		System.out.println("id is:"+Locposition);
        	}
        	 
        	@Override
        	public void onNothingSelected(AdapterView<?> arg0) {
        	// TODO Auto-generated method stub
        	}
        	});
        	
        	
        	AlertDialog.Builder alert=new AlertDialog.Builder(MainActivity.this);
        	alert.setTitle("Select Location");
        	alert.setView(s);
        	//e.setText(PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString("MYIP","http://ec2-54-148-0-61.us-west-2.compute.amazonaws.com:1337/"));
        	alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dialog, int whichButton) {
        			PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putInt("FIRST_USE",1).commit();
        			PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putInt("PROJECT_CODE",Locposition).commit();
        			
        			dialog.cancel();
        			firstusecall2();
        		}
        	});
        	alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dialog, int whichButton) {
        			//Canceled.
        		}
        	});
        	alert.show();
          
        }
 
    }
    
    
    
    
    
    
    
    
    
    
    
    
    /*
     * the call to obtain json object of worksite for a location
     */
    
    private class GetWorkSite extends AsyncTask<Void, Void, Void> {
    	 
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();
 
        }
 
        @Override
        protected Void doInBackground(Void... arg0) {
            // Creating service handler class instance
            ServiceHandler sh = new ServiceHandler();
            String url = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString("MYIP","http://ec2-54-148-0-61.us-west-2.compute.amazonaws.com:1337/");
            url=url+"locations/getAllProjects/"+PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getInt("PROJECT_CODE",0);
            // Making a request to url and getting response
            String jsonStr = sh.makeServiceCall(url, ServiceHandler.GET);
 
            Log.d("Response: ", "> " + jsonStr);
 
            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);
                     
                    // Getting JSON Array node
                    contacts1 = jsonObj.getJSONArray("data");
 
                    // looping through All Contacts
                    for (int i = 0; i < contacts1.length(); i++) {
                        JSONObject c = contacts1.getJSONObject(i);
                         
                        String id = c.getString("location");
                        String loc = c.getString("code");
                      System.out.println("name:  "+loc);
 
                        // tmp hashmap for single contact
                        HashMap<String, String> contact = new HashMap<String, String>();
 
                        // adding each child node to HashMap key => value
                        contact.put("location", id);
                        contact.put("code", loc);
                       
 
                        // adding contact to contact list
                        contactList1.add(contact);
                        Sitelist.add(loc);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e("ServiceHandler", "Couldn't get any data from the url");
            }
 
            return null;
        }
 
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();
          
            
            final Spinner s=new Spinner(MainActivity.this);
        	s.setAdapter(new ArrayAdapter<String>(MainActivity.this,
        	android.R.layout.simple_spinner_dropdown_item,
        	Sitelist));
        	  
        	s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        	
        	@Override
        	public void onItemSelected(AdapterView<?> arg0,
        	View arg1, int position, long arg3) {
        	// TODO Auto-generated method stub
        	// Locate the textviews in activity_main.xml
        		Siteposition=Integer.parseInt(contactList1.get(position).get("location"));
        		nam=contactList1.get(position).get("code");
        		System.out.println("site code in shared: "+PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getInt("SITE_CODE",0));
        	}
        	 
        	@Override
        	public void onNothingSelected(AdapterView<?> arg0) {
        	// TODO Auto-generated method stub
        	}
        	});
        	
        	AlertDialog.Builder alert=new AlertDialog.Builder(MainActivity.this);
        	alert.setTitle("Select Worksite");
        	alert.setView(s);
        	//e.setText(PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString("MYIP","http://ec2-54-148-0-61.us-west-2.compute.amazonaws.com:1337/"));
        	alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dialog, int whichButton) {
        			PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putInt("FIRST_USE",1).commit();
        			PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putString("SITE_NAME",nam).commit();
        			PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putInt("SITE_CODE",Siteposition).commit();
        			
        			dialog.cancel();
        			
        		}
        	});
        	alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dialog, int whichButton) {
        			//Canceled.
        		}
        	});
        	alert.show();
          
        }
 
    }
    
    
    
    
    
    
    
    
    
    
    
 }



















	

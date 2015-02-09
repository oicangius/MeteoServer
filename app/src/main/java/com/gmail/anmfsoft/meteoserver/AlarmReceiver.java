package com.gmail.anmfsoft.meteoserver;

import android.content.Intent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

public class AlarmReceiver extends BroadcastReceiver{



    String nombreDispositivo;
	String device = "";
	//String password ="1234";
	Random randomGenerator = new Random();
	float h,t;
    private static final int REQUEST_CODE_CAPTURE_IMAGE = 1;
    private static final int REQUEST_CODE_CREATOR = 2;
    private static final int REQUEST_CODE_RESOLUTION = 3;
    private static final String TAG = "alarmaReceiver";

    //Context cc;

    public class LongOperation extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            android.text.format.Time now = new android.text.format.Time();
            now.setToNow();
            Log.d(TAG, "doInBackground");
            String URL="http://oican.esy.es/insert.php?dia="+now.format("%Y-%m-%d")+"&hora="+now.format("%H:%M:%S")+"&t="+t+"&h="+h+"&dispositivo="+nombreDispositivo;
            Log.d(TAG,URL);

            //Toast.makeText(cc, URL, Toast.LENGTH_SHORT).show();

            HttpClient httpClient = new DefaultHttpClient();
            //HttpPost httpPost = new HttpPost("http://oican.esy.es/index.php");
            HttpPost httpPost = new HttpPost(URL);
            // Url Encoding the POST parameters
            Log.d(TAG, "doInBackground ANTES DEL TRY");
            try {
                HttpResponse response = httpClient.execute(httpPost);
                Log.d("Http Response:", response.toString());

            } catch (ClientProtocolException e) {
                //e.printStackTrace();
            } catch (IOException e) {
                //e.printStackTrace();
            }
            Log.d(TAG, "fin doInBackground");
            return null;
        }
    }


    private void insertarEnDB(){
        Log.d(TAG,"insertarEnDB");
        android.text.format.Time now = new android.text.format.Time();
        now.setToNow();
        Log.d(TAG,now.format("%d-%m-%Y %H.%M.%S"));
        Log.d(TAG,now.format("%d-%m-%Y"));
        Log.d(TAG,now.format("%H:%M:%S"));
        //String URL="http://oican.esy.es/insert.php?dia="+now.format("%Y-%m-%d")+"&hora="+now.format("%H:%M:%S")+"&t="+t+"&h="+h+"&dispositivo="+nombreDispositivo;

        LongOperation longOperation = new LongOperation();
        longOperation.execute();

        Log.d(TAG,"fin insertarEnDB");
    }

    public String bundle2string(Bundle bundle) {
        String string = "Bundle{";
        for (String key : bundle.keySet()) {
            string += " " + key + " => " + bundle.get(key) + ";";
        }
        string += " }Bundle";
        return string;
    }

    @Override
    public void onReceive(Context arg0, Intent arg1) {
        Log.d(TAG,"onReceive");
        //Bundle extrasBundle = arg1.getExtras();
        if (arg1.getExtras()!=null && this.device==""){
            this.device = arg1.getExtras().getString("device");
            Log.d(TAG,"onReceive, if, device="+device);
            Log.d(TAG,this.bundle2string(arg1.getExtras()));
        }else {
            this.device = "MeteoBlue";
            Log.d(TAG,"onReceive, else, device="+device);
        }


        if(this.findBT()){
        	try {
				this.openBT();
				this.data="";
				int randomInt = randomGenerator.nextInt(10);
				this.sendData(""+randomInt);
				this.beginListenForData();
        		try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
                    Toast.makeText(arg0, "Error esperando BT", Toast.LENGTH_SHORT).show();
				}
				this.closeBT();
				
				if(data!=""){
					String[] arrayAux=data.split(" ");
					if(arrayAux.length==6){
						h=Float.parseFloat(arrayAux[2]);
						t=Float.parseFloat(arrayAux[4]);
						//Log.d("MeteoServer",h+"..."+t);
						Toast.makeText(arg0, h+"..."+t, Toast.LENGTH_SHORT).show();
                        //cc=arg0;
                        insertarEnDB();
					}else 
						Toast.makeText(arg0, "data not good", Toast.LENGTH_SHORT).show();
				}else
					Toast.makeText(arg0, "NO data", Toast.LENGTH_SHORT).show();
			} catch (IOException e) {
                Toast.makeText(arg0, "Error abriendo BT", Toast.LENGTH_SHORT).show();
			}
        }else{
        	Toast.makeText(arg0, "BT no encontrado", Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG,"fin onReceive");
    }
    
    
	//////////////////
	//////////////////
	
	BluetoothAdapter mBluetoothAdapter;
	BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    boolean conectado=false;
    
    volatile boolean stopWorker;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    int[] sensorsV;

    String data;
	
	boolean findBT()
	{
        Log.d(TAG,"findBT");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();




        if(mBluetoothAdapter == null)
        {
        	//Log.d("MeteoServer","No bluetooth adapter available");
        }
        
        if(!mBluetoothAdapter.isEnabled())
        {
            /*Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            final Activity act=new Activity();
            act.startActivityForResult(enableBluetooth, 0);*/
        	//Log.d("MeteoServer","BlueTooth NO Activado");
        }
        
        //Log.d("MeteoServer","Bluetooth activated");

        this.nombreDispositivo=mBluetoothAdapter.getName();


        android.text.format.Time now = new android.text.format.Time();
        now.setToNow();
        String URL="http://oican.esy.es/insert.php?dia="+now.format("%Y-%m-%d")+"&hora="+now.format("%H:%M:%S")+"&t="+t+"&h="+h+"&dispositivo="+nombreDispositivo;
        Log.d(TAG,URL);


        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
            	//Log.d("MeteoServer","Dispositivo:"+device.getName());
                if(device.getName().equals(this.device))
                {
                    mmDevice = device;
                    //Log.d("MeteoServer","MeteoBlue encontrado");
                    return true;
                }
            }
        }
        //Log.d("MeteoServer","MeteoBlue NO encontrado");
        Log.d(TAG,"fin findBT");
        return false;
	}
	
	
	
	void openBT() throws IOException
    {
        Log.d(TAG,"openBT");
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);        
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();
        //Log.d("MeteoServer","2 openBT");
        if(mmInputStream!=null) {
            //Log.d("MeteoServer","mmInputStream is not null");
        }else {
            //Log.d("MeteoServer","mmInputStream is null");
        }
        beginListenForData();
        
        //Log.d("MeteoServer","Bluetooth Opened");
        
        this.conectado=true;
        Log.d(TAG,"fin openBT");
    }

	
	
	void beginListenForData()
    {
        Log.d(TAG,"beginListenForData");
        final Handler handler = new Handler(); 
        final byte delimiter = 13; //This is the ASCII code for a newline character
        
        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {                
            	int bytesAvailable;
            	while(!Thread.currentThread().isInterrupted() && !stopWorker)
            	{
                    try 
                    {
                        bytesAvailable= mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                            	if(b==10){
                            		continue;
                            	}
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    data = new String(encodedBytes);
                                    readBufferPosition = 0;
                                   	//Log.d("MeteoServer","Recivida linea por BT:"+data);
                                }
                                else
                                {
                                	readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } 
                    catch (IOException ex) 
                    {
                        stopWorker = true;
                    }
               }
            }
        });

        workerThread.start();
        Log.d(TAG,"fin beginListenForData");
    }

	void sendData(String msg) throws IOException
    {
        Log.d(TAG,"sendData");
        //String msg = "hola";
        msg += "\n";
        mmOutputStream.write(msg.getBytes());
        mmOutputStream.flush();
        Log.d(TAG,"fin sendData");    }
	
	void closeBT() throws IOException
    {
        Log.d(TAG,"closeBT");
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        //Log.d("MeteoServer","Bluetooth Closed");
        this.conectado=false;
        Log.d(TAG,"fin closeBT");
    }


}

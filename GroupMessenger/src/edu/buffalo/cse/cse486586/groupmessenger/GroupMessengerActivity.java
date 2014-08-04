package edu.buffalo.cse.cse486586.groupmessenger;



import java.io.IOException;
import java.io.ObjectOutputStream;

import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

	static final String TAG = GroupMessengerActivity.class.getSimpleName();
	static final int SERVER_PORT = 10000;
	//
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_group_messenger);

		 TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
	        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
	        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

		
		Log.e(TAG,"\nAvd is ");
		Log.e(TAG,myPort);
		/*
		 * TODO: Use the TextView to display your messages. Though there is no grading component
		 * on how you display the messages, if you implement it, it'll make your debugging easier.
		 */
		TextView tv = (TextView) findViewById(R.id.textView1);
		tv.setMovementMethod(new ScrollingMovementMethod());

		TextView tv_1 = (TextView) findViewById(R.id.editText1);

		/*
		 * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
		 * OnPTestClickListener demonstrates how to access a ContentProvider.
		 */
		findViewById(R.id.button1).setOnClickListener(
				new OnPTestClickListener(tv, getContentResolver()));

		findViewById(R.id.button4).setOnClickListener(
				new SendClickListener(tv_1, getContentResolver(),myPort));
		try {
			ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
			Ports ports=new Ports();
			ports.myPort=myPort;
			ports.serversocket=serverSocket;
			new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ports);
		} catch (IOException e) {
			/*
			 * Log is a good way to debug your code. LogCat prints out all the messages that
			 * Log class writes.
			 * 
			 * Please read http://developer.android.com/tools/debugging/debugging-projects.html
			 * and http://developer.android.com/tools/debugging/debugging-log.html
			 * for more information on debugging.
			 */
			Log.e(TAG, "Can't create a ServerSocket");
			return;
		}
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
		return true;
	}
	
	private class Ports{
		ServerSocket serversocket;
		String myPort;
		
	}

	public static int seq_buf_idx=0;
	private class ServerTask extends AsyncTask<Ports, Message, Void> {

		Queue <Message>buffer =new LinkedList<Message>();
		ArrayList <Message>sequence_buffer =new ArrayList<Message>();
	
		int sequencer = -1;
		@Override
		protected Void doInBackground(Ports... ports) {
			
			ServerSocket serverSocket = ports[0].serversocket;
			Helper helper = new Helper();
			boolean msg=true;
			while(true){
				try {	
					Log.e("my port","server is listening");
					Log.e("my port",ports[0].myPort);
					Socket client=serverSocket.accept();
					String port="My port is ";
					port.concat(ports[0].myPort);
					Log.e("my port",ports[0].myPort);
					ObjectInputStream instream = new ObjectInputStream(client.getInputStream());
					Message message = (Message)instream.readObject();

					instream.close();	
					if(!message.is_Ordered){
						Log.e(TAG,"an unorder message recved");
						Log.e(TAG,message.Message);
						int num=helper.what_is_my_num(ports[0].myPort);
						helper.Increment_my_Vec(num);		
						if(ports[0].myPort.equals("11112".toString())){
							//I am a sequencer
							Log.e(TAG,"I am the sequencer");
							
							if(!message.is_Ordered){
								sequencer++;
								message.Sequence=String.valueOf(sequencer);
								message.is_Ordered=true;
								Log.e(TAG,"sending out sequenced message");
								helper.Send_all_message(helper.what_is_my_num(ports[0].myPort),message);
								publishProgress(message);
								
							}
						}
					}
					else{
						Log.e(TAG,"an ordered message recved");
						Log.e(TAG,message.Sequence);

						if(Math.abs(Integer.parseInt(message.Sequence)-sequencer) == 1){
							
							sequencer=Integer.parseInt(message.Sequence);
							publishProgress(message);
						}
						else{
							int i=0;
							while(i<seq_buf_idx){
								if(Integer.parseInt(message.Sequence) - Integer.parseInt(sequence_buffer.get(i).Sequence)==-1){
									if(msg){
										sequencer=Integer.parseInt(message.Sequence);
										msg=false;
										publishProgress(message);
									}
									sequencer=Integer.parseInt(sequence_buffer.get(i).Sequence);
									publishProgress(sequence_buffer.get(i));
									helper.remove_from_arr(sequence_buffer,i);
								
								}
								else if (Integer.parseInt(message.Sequence) < Integer.parseInt(sequence_buffer.get(i).Sequence)){
									helper.add_into_arr(sequence_buffer,i);	
								}	
								else if(Integer.parseInt(message.Sequence) > Integer.parseInt(sequence_buffer.get(i).Sequence)){
									i++;
								}
							}
						}	

					}
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					Log.e(TAG,"Not able to accpet client");
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		private Uri buildUri(String scheme, String authority) {
			Uri.Builder uriBuilder = new Uri.Builder();
			uriBuilder.authority(authority);
			uriBuilder.scheme(scheme);
			return uriBuilder.build();
		}

		protected void onProgressUpdate(Message...messages) {
			/*
			 * The following code displays what is received in doInBackground().
			 */
			Log.e("on progress","key" + messages[0].Sequence+" "+ messages[0].Message);
			Uri mURI = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger.provider");
			ContentValues values= new ContentValues();
			values.put("key", messages[0].Sequence);
			values.put("value", messages[0].Message);
			
			ContentResolver mContentResolver=getContentResolver();
			mContentResolver.insert(mURI, values);

			String strReceived = messages[0].Message;
			
			TextView TextView = (TextView) findViewById(R.id.textView1);
			TextView.append(strReceived + "\t\n");
			//Log.e(TAG,strReceived);

			/*
			 * The following code creates a file in the AVD's internal storage and stores a file.
			 * 
			 * For more information on file I/O on Android, please take a look at
			 * http://developer.android.com/training/basics/data-storage/files.html
			 */

			return;
		}
	}

	/*
	 * TODO: You need to register and implement an OnClickListener for the "Send" button.
	 * In your implementation you need to get the message from the input box (EditText)
	 * and send it to other AVDs in a total-causal order.
	 */
}

class Helper{
	String remotePort []= {"11108","11112","11116","11120","11124"};

	public void remove_from_arr(ArrayList<Message> arr, int idx){
		int i=idx;

		while(i<GroupMessengerActivity.seq_buf_idx){
			arr.set(i, arr.get(++i));
		}		
		GroupMessengerActivity.seq_buf_idx--;
	}

	public void add_into_arr(ArrayList<Message> arr, int idx){
		int i=idx;

		while(i<GroupMessengerActivity.seq_buf_idx){
			arr.set(i+1, arr.get(i));
		}		
		GroupMessengerActivity.seq_buf_idx++;
	}


	public void send_to_socket(int i, Message message){
		try {
			Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
					Integer.parseInt(remotePort[i]));


			//PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			//	out.write(msgToSend);

			ObjectOutputStream out= new ObjectOutputStream(socket.getOutputStream()); 
			out.writeObject(message);
			out.close();    
			socket.close();
		} 
		catch (UnknownHostException e) {
			Log.e("Send to socket from server", "ClientTask UnknownHostException");
		} catch (IOException e) {
			Log.e("Send to socket from server", "ClientTask socket IOException");
		}
	}

	void Increment_my_Vec(int num){

		SendClickListener.Vec_Clock[num]++;
	}

	public boolean did_1_happened_before2(Message recv, Message in_buff){

		for(int i=0;i<5;i++){
			if(recv.Vec_Clock[i]<in_buff.Vec_Clock[i]){
				return true;	
			}
		}
		return false;
	}


	public int what_is_my_num(String myPort){

		for (int i=0;i<5;i++){
			if(String.valueOf(remotePort[i]).equals(myPort)){
				return i;
			}
		}
		return -1;
	}

	public void Send_all_message(int num, Message message){

		for (int i=0; i< remotePort.length; i++){

			if(i==num){
				continue;
			}
			else {
				send_to_socket(i,message);
			}

		}
	}


	public boolean put_in_Buffer(int my_vec_clock[], int recv_vec_clock[], int num){

		for (int i=0; i< my_vec_clock.length; i++){

			if(i==num){
				continue;
			}

			if (my_vec_clock[i]!=recv_vec_clock[i] ){
				return true;
			}
		}
		return false;		
	}

}


package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.content.ContentResolver;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.os.AsyncTask;
import android.util.Log;


public class SendClickListener implements OnClickListener {
	public static final int local_text_display=0x7f050000;
	public static final int remote_text_display=0x7f050001;
	final String TAG = SendClickListener.class.getSimpleName();
	private final TextView mTextView;
	private final ContentResolver mContentResolver;

	static int Vec_Clock [] = {0,0,0,0,0};
	static final int SERVER_PORT = 10000;
	String myPort;
	private static final String remotePort []= {"11108","11112","11116","11120","11124"};



	public SendClickListener(TextView _tv, ContentResolver _cr, String myPort) {
		mTextView = _tv;
		mContentResolver = _cr;
		this.myPort=myPort;
	}

	@Override
	public void onClick(View arg0) {

		// TODO Auto-generated method stub
		String msg = mTextView.getText().toString();
		mTextView.setText(""); // This is one way to reset the input box.
		//	mTextView.append(msg);
		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
	}

	private class ClientTask extends AsyncTask<String, Void, Void>  {

		/**
		 * 
		 */

		@Override
		protected Void doInBackground(String... msgs) {

			Log.e(TAG, "client!!");
			int count=0;

			for (int i=0;i<5;i++){
				while(count<6){
					try {
						Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
								Integer.parseInt(remotePort[i]));

						String msgToSend = msgs[0];

						Log.e(TAG, msgToSend);

						//PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
						//	out.write(msgToSend);
						Message message=new Message(msgToSend,myPort, SendClickListener.Vec_Clock,"",false);

						ObjectOutputStream out= new ObjectOutputStream(socket.getOutputStream()); 
						out.writeObject(message);
						Log.e(TAG, "sent the message");
						Log.e(TAG, remotePort[i]);
						count=0;
						out.close();    
						socket.close();
						break;
					} 
					catch (UnknownHostException e) {
						Log.e(TAG, "ClientTask UnknownHostException");
					} catch (IOException e) {
						count++;
						Log.e("ClientTask socket IOException",remotePort[i]);
					}
				}
				count=0;
			}
			return null;
		}	
		
	}

}


class Message implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String Message;
	String Myport;
	boolean is_Ordered;
	int Vec_Clock[] = new int[5];
	String Sequence;

	public Message(String Message, String Myport, int Vec_clock[], String Sequence, boolean is_Ordered){
		this.Message=Message;
		this.Myport=Myport;
		this.is_Ordered=is_Ordered;
		this.Vec_Clock=Vec_clock;
		this.Sequence=Sequence;
	}
}

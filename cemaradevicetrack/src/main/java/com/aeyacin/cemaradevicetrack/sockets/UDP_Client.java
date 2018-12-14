package com.aeyacin.cemaradevicetrack.sockets;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDP_Client
{

    private InetAddress IPAddress = null;
    private String message = "Hello Android!" ;
    private AsyncTask<Void, Void, Void> async_cient;
    public String Message;


    @SuppressLint("NewApi")
    public void NachrichtSenden()
    {
        async_cient = new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(Void... params)
            {
                DatagramSocket ds = null;

                try
                {
                    byte[] ipAddr = new byte[]{ (byte) 178, (byte) 18, (byte)200, (byte) 114};
                    InetAddress addr = InetAddress.getByAddress(ipAddr);
                    ds = new DatagramSocket(8086);
                    DatagramPacket dp;
                    dp = new DatagramPacket(Message.getBytes(), Message.getBytes().length, addr, 8086);
                    ds.setBroadcast(true);
                    ds.send(dp);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                finally
                {
                    if (ds != null)
                    {
                        ds.close();
                    }
                }
                return null;
            }

            protected void onPostExecute(Void result)
            {
                super.onPostExecute(result);
            }
        };

        if (Build.VERSION.SDK_INT >= 11) async_cient.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else async_cient.execute();
    }
}
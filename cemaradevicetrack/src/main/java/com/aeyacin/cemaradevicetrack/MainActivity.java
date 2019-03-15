package com.aeyacin.cemaradevicetrack;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.aeyacin.cemaradevicetrack.db.model.DataModel;
import com.aeyacin.cemaradevicetrack.sockets.TcpClient;
import com.aeyacin.cemaradevicetrack.utils.DateTime;
import com.aeyacin.cemaradevicetrack.utils.converters.NV21toYuv420;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.Geofence;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import fr.quentinklein.slt.LocationTracker;
import ime.extsdk.ImePreviewDataManager;
import ime.extsdk.ImePreviewDataManager.PreviewSize;
import ime.extsdk.ImeTTS;
import ime.extsdk.ipc.CameraProxy;
import ime.extsdk.ipc.CameraProxy.CameraDataCallBack;
import io.nlopez.smartlocation.OnReverseGeocodingListener;
import io.nlopez.smartlocation.SmartLocation;
import io.realm.Realm;
import io.realm.RealmResults;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferListener;


public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener
    /*    OnLocationUpdatedListener, OnActivityUpdatedListener,
        OnGeofencingTransitionListener, LocationBasedOnActivityProvider.LocationBasedOnActivityListener */ {

    //FTP Connection
    public static final String FTP_HOST = "178.18.200.115";
    public static final String FTP_USER = "administrator";
    public static final String FTP_PASS = "Uj~Hp9dXgbX?";
    private FTPClient client;

    //Files
    private File mVideoFile,rota_file,video,ses,mp4,last_mp4,ftp_dosya,audio_file,photo,ses_deneme,dest_file,dest_file2,dest_file3;
    private static final String TAG = "ImeSdk";
    FileOutputStream fos = null;

    String h264_path,h264_new,path2,pathdeneme,outputFile,output_new,last_output;

    //imei init
    private ImePreviewDataManager mImePreviewDataManager;
    private static final int RESTART_PREVIEW_SUCCESS = 0x01;
    private static final int RESTOP_PREVIEW_SUCCESS = 0x02;
    ImePreviewDataManager.PreviewSize realPreviewSize;

    MediaRecorder recorder;
    boolean isAudioRecording;

    //Media Codec
    MediaCodec mMediaCodec,audioCodec;
    private BufferedOutputStream outputStream;
    private String audioType = "audio/mp4a-latm";
    int i = 0;

    //FFmepeg init
    FFmpeg ffmpeg;
    String[] cmd;
    String[] complexCommand;
    String[] cmd3;

    private SensorManager sensorManager;
    private Vibrator vibrator;
    private float last_x, last_y, last_z = 0;
    private float son_x, son_y, son_z, son_bear,son_speed= 0;
    float bear=0;
    private double Hesap0 = 0;
    private double Hesap0yz = 0;
    private double Hesapxyz = 0;
    private double Hesapyz = 0;
    private float first_x, first_y, first_z = 0;
    double d = 1.85;
    float f = (float) d;

    private float Alarm=0;
    //UDP Client erstellen
    //UDP_Client Client;
    TcpClient mTcpClient;
    Realm realm;

    static final float ALPHA = 0.25f;
    //   private LocationGooglePlayServicesProvider provider;
    private static final int LOCATION_PERMISSION_ID = 1001;
    LocationTracker tracker;
    LocationManager locationManager;
    private TextView locationText;
    private TextView activityText;
    private TextView geofenceText;
    private TextView sensorText;
    private TextView GsensorText;

    final static String fileName = "data.txt";
    final static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
    List<float[]> OrtamalaListe = new ArrayList<>();
    List<float[]> IHLAL = new ArrayList<>();
    Location GlobalLocation;

    boolean isRecordViolate = false;

    byte[] send_data = new byte[1024];

    DataModel dataModel;

    String msg = "";

    String get_mesaj= "";

    private Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {

            switch (msg.what) {

                case RESTART_PREVIEW_SUCCESS:
/*                    try {
                        startPreview();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }*/
                    break;
                case RESTOP_PREVIEW_SUCCESS:
//                    stopPreview();
                    break;

                default:
                    break;
            }
        }

    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        client = new FTPClient();

        initFiles();
        mImePreviewDataManager = ImePreviewDataManager.getImePreviewDataManager(this);
        initVideoCodec();

        //Client = new UDP_Client();
        realm = Realm.getDefaultInstance();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Button xyzLocation = findViewById(R.id.xyz);
        xyzLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                stopPreview();
            }
        });


        // Bind event clicks
        Button startLocation = findViewById(R.id.start_location);
        startLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Location permission not granted
//                if (GetPermission()) {
                    //startLocation();

                    try {
                        startPreview();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
//            }
        });

        Button stopLocation = findViewById(R.id.stop_location);
        stopLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //stopLocation();

                // Join Video and audio
                String[] cmd2 = {"-i",path2,"-ss","2","-to","10","-c","copy",pathdeneme};
                cmd = new String[]{"-i", h264_path, "-i", path2, "-c", "copy", "-map", "0:v:0", "-map", "1:a:0", outputFile};
                cmd3 = new String[]{"-i", h264_path, "-filter:v", "setpts=1.25*PTS", h264_new};
                //String[]cmd = {"-i", h264_path, "-i", path2, "-preset", "ultrafast", "-filter_complex", "[1:v]scale="+1280*0.21+":"+720*0.35+" [ovrl],[0:v][ovrl] overlay=x=(main_w-overlay_w):y=(main_h-overlay_h)", outputFile};
                complexCommand = new String[]{"-i", outputFile, "-filter_complex", "[0:v]setpts=1.10*PTS[v];[0:a]atempo=1.00[a]", "-map", "[v]", "-map", "[a]", "-b:v", "1097k", "-r", "10", "-vcodec", "mpeg4", output_new};
                String[] convert_mp4 = {"-i",h264_path,"-c","copy",output_new};

                try {
                    //FFMPEG initialize
                    ffmeg();

                } catch (FFmpegNotSupportedException e) {
                    e.printStackTrace();
                }

                //Do something after 100ms

                try {
                    //FFMPEG execute command
                    executeCommand(cmd);

                } catch (FFmpegCommandAlreadyRunningException e) {
                    e.printStackTrace();
                }



            }
        });


        locationText = findViewById(R.id.location_text);
        activityText = findViewById(R.id.activity_text);
        geofenceText = findViewById(R.id.geofence_text);
        sensorText = findViewById(R.id.sensor_text);
        GsensorText = findViewById(R.id.sensorG_text);


        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        startLocation();

    }

    public void ffmeg() throws FFmpegNotSupportedException {

        ffmpeg = FFmpeg.getInstance(getApplicationContext());
        ffmpeg.loadBinary(new FFmpegLoadBinaryResponseHandler() {
            @Override
            public void onFailure() {
                Log.i("Failed","Failed FFmeg");
            }

            @Override
            public void onSuccess() {
                Log.i("Success","Success FFmeg");
            }

            @Override
            public void onStart() {

            }

            @Override
            public void onFinish() {

            }
        });
    }

    public void executeCommand(final String [] command) throws FFmpegCommandAlreadyRunningException {
        try {
            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {

                @Override
                public void onFailure(String message) {
                    super.onFailure(message);
                    Log.i("Failure", "" + message);
                }

                @Override
                public void onFinish() {
                    super.onFinish();

                    try {
                        executeCommand_final(complexCommand);
                    } catch (FFmpegCommandAlreadyRunningException e) {
                        e.printStackTrace();
                    }

                    //mux(output_new, path2, last_output);
                }

                @Override
                public void onProgress(String message) {
                    super.onProgress(message);
                }

                @Override
                public void onStart() {
                    super.onStart();
                }

                @Override
                public void onSuccess(String message) {
                    super.onSuccess(message);
                    Log.i("Success", "" + message);
                }
            });
        }catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
            e.printStackTrace();
        }

    }

    public void executeCommand_final(final String [] command) throws FFmpegCommandAlreadyRunningException {
        try {
            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {

                @Override
                public void onFailure(String message) {
                    super.onFailure(message);
                    Log.i("Failure", "" + message);
                }

                @Override
                public void onFinish() {
                    super.onFinish();

                    //Uploading Video to FTP
                    new FtpTask().execute();



                }

                @Override
                public void onProgress(String message) {
                    super.onProgress(message);
                }

                @Override
                public void onStart() {
                    super.onStart();
                }

                @Override
                public void onSuccess(String message) {
                    super.onSuccess(message);
                    Log.i("Success", "" + message);
                }
            });
        }catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
            e.printStackTrace();
        }

    }

    private class FtpTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... args) {



            try {
                client.connect(FTP_HOST,21);
                client.login(FTP_USER,FTP_PASS);
                client.setType(FTPClient.TYPE_BINARY);
                client.changeDirectory("/upload/");
                client.upload(dest_file2,new MyTransferListener());

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    client.disconnect(true);
                }catch (Exception e2){
                    e2.printStackTrace();
                }
            }

            return null;
        }

        protected void onPostExecute(String result) {

            //Where ftpClient is a instance variable in the main activity
        }
    }

    /*******  Used to file upload and show progress  **********/

    public class MyTransferListener implements FTPDataTransferListener {

        public void started() {
            // Transfer started

            //Toast.makeText(getBaseContext(), " Upload Started ...", Toast.LENGTH_SHORT).show();
            //System.out.println(" Upload Started ...");
        }

        public void transferred(int length) {
            // Yet other length bytes has been transferred since the last time this
            // method was called
            //Toast.makeText(getBaseContext(), " transferred ..." + length, Toast.LENGTH_SHORT).show();
            //System.out.println(" transferred ..." + length);
        }

        public void completed() {
            // Transfer completed
            //Toast.makeText(getBaseContext(), " completed ...", Toast.LENGTH_SHORT).show();
            //System.out.println(" completed ..." );

            finish();
            startActivity(getIntent());
        }

        public void aborted() {
            // Transfer aborted
            //Toast.makeText(getBaseContext()," transfer aborted , please try again...", Toast.LENGTH_SHORT).show();
            //System.out.println(" aborted ..." );
        }

        public void failed() {
            // Transfer failed
            System.out.println(" failed ..." );
        }

    }


    private void initVideoCodec() {

        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc",
                1280,
                720);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 6000000);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 180); //video second

        /*        mDestData = new byte[1280 * 720
         * ImageFormat.getBitsPerPixel(ImageFormat.YV12) / 8];*/

        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);//5
        try {
            mMediaCodec = MediaCodec.createEncoderByType("video/avc");
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaCodec.configure(mediaFormat,
                null,
                null,
                MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();



        //Audio Encoder
        audio_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/", "audio_encoded.mp4");
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(audio_file));
            Log.e("AudioEncoder", "outputStream initialized");
        } catch (Exception e){
            e.printStackTrace();
        }

        try {
            audioCodec = MediaCodec.createEncoderByType(audioType);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final int kSampleRates[] = { 8000, 11025, 22050, 44100, 48000 };
        final int kBitRates[] = { 64000, 128000 };
        MediaFormat audioFormat  = MediaFormat.createAudioFormat(audioType,kSampleRates[3],2);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, kBitRates[1]);
        audioCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        audioCodec.start();
    }







    private void initFiles() {

        rota_file = new File("/mnt/sdcard2/rota");
        video = new File("/mnt/sdcard2/rota/video");
        ses = new File("/mnt/sdcard2/rota/ses");
        mp4 = new File("/mnt/sdcard2/rota/mp4");
        last_mp4 = new File("/mnt/sdcard2/rota/last_mp4");
        ftp_dosya = new File("/mnt/sdcard2/rota/ses/07032019_134058.aac");

        if(!rota_file.exists()) {
            rota_file.mkdir();
        }
        if(!video.exists()) {
            video.mkdir();
        }
        if(!ses.exists()) {
            ses.mkdir();
        }
        if(!mp4.exists()) {
            mp4.mkdir();
        }
        if(!last_mp4.exists()) {
            last_mp4.mkdir();
        }
    }

    private CameraProxy.CameraDataCallBack callBack = new CameraProxy.CameraDataCallBack() {

        @Override
        public void onDataBack(byte[] data, long length) {
            // TODO Auto-generated method stub
            Log.i(TAG, "length . " + length);

            photo = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/", "photo" + i + ".jpg");
            byte[] outpt = new byte[data.length];
            //output = new byte[data.length];


            byte[] datam = NV21toYuv420.NV21toYUV420Planar(data, outpt, 1280, 720);

            encode(datam);
        }
    };

    //Video format H264
    private synchronized void encode(byte[] data) {

        ByteBuffer[]  inputBuffers = mMediaCodec.getInputBuffers();
        ByteBuffer[]  outputBuffers = mMediaCodec.getOutputBuffers();

        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.capacity();
            inputBuffer.clear();
            inputBuffer.put(data);
            mMediaCodec.queueInputBuffer(inputBufferIndex, 0, data.length, 1000000, 0);

        } else {
            return;
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        Log.i(TAG, "outputBufferIndex-->" + outputBufferIndex);
        do {
            if (outputBufferIndex >= 0) {
                ByteBuffer outBuffer = outputBuffers[outputBufferIndex];
                System.out.println("buffer info-->" + bufferInfo.offset + "--"
                        + bufferInfo.size + "--" + bufferInfo.flags + "--"
                        + bufferInfo.presentationTimeUs);
                byte[] outData = new byte[bufferInfo.size];
                outBuffer.get(outData);
                //new ClientAsycTask().execute(outData);
                try {
                    if (bufferInfo.offset != 0) {
                        fos.write(outData, bufferInfo.offset, outData.length
                                - bufferInfo.offset);


                    } else {
                        fos.write(outData, 0, outData.length);
                        //new ClientAsycTask().execute(data);
                    }
                    fos.flush();
                    Log.i(TAG, "out data -- > " + outData.length);
                    mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo,
                            0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = mMediaCodec.getOutputBuffers();
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat format = mMediaCodec.getOutputFormat();
            }
        } while (outputBufferIndex >= 0);
    }

    private void files() {

        //getting Current Date and Time
        String currentDateandTime = DateTime.getDate();

        mVideoFile = new File("/mnt/sdcard2/rota/video", currentDateandTime + ".264");
        h264_path = "/mnt/sdcard2/rota/video/" + currentDateandTime + ".264";
        h264_new = "/storage/sdcard0/newvideo.264";

        try {
            fos = null;
            fos = new FileOutputStream(mVideoFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        //ses = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/", "ses.aac"); for saving internal storage
        //path2 = "/storage/sdcard0/ses.aac"; for saving internal storage
        ses = new File("/mnt/sdcard2/rota/ses", currentDateandTime + ".aac"); //for saving external storage(sd card)
        path2 = "/mnt/sdcard2/rota/ses/" + currentDateandTime + ".aac"; //for saving external storage(sd card)

        ses_deneme = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/", "ses_deneme.aac");
        pathdeneme = "/storage/sdcard0/sesdeneme.aac";

        dest_file = new File("/mnt/sdcard2/rota/mp4", currentDateandTime + ".mp4");
        dest_file2 = new File("/mnt/sdcard2/rota/last_mp4", currentDateandTime + ".mp4");
        dest_file3 = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/", "fener2.mp4");
        outputFile = "/mnt/sdcard2/rota/mp4/" + currentDateandTime + ".mp4";
        output_new = "/mnt/sdcard2/rota/last_mp4/" + currentDateandTime + ".mp4";
        last_output = "/storage/sdcard0/fener2.mp4";
    }

    private void startPreview() throws IOException {


        files();

        mHandler.removeMessages(RESTART_PREVIEW_SUCCESS);

        //判断预览数据管理器是否初始化化完成
        if(mImePreviewDataManager.checkPreviewDataManagerInitSuccess()) {

            //设置摄像头ID
            mImePreviewDataManager.initCamId(ImePreviewDataManager.MAIN_CAM_ID);

            //设置预览尺寸
            mImePreviewDataManager.setPreviewSize(new ImePreviewDataManager.PreviewSize(1280, 720));

            //获取实际预览尺寸，预览尺寸有可能设置失败
            realPreviewSize = mImePreviewDataManager.getPreviewSize();


            Log.i(TAG, "realPreviewSize : " + realPreviewSize.getWidth() + ", " + realPreviewSize.getHeight());

            //设置数据回调接口
            mImePreviewDataManager.setPreviewDataCallback(callBack);

            //开始预览，在预览之后就会有相应的数据通过callBack回调出来
            mImePreviewDataManager.startPreview();

            Log.i(TAG, "startPreview success");

            //stateTv.setText("startPreview success");

            try {
                audio_recorder_init();
            } catch (IOException e) {
                e.printStackTrace();
            }




        } else {
            mHandler.sendEmptyMessageDelayed(RESTART_PREVIEW_SUCCESS, 2 * 1000);
            Log.i(TAG, "wait startPreview success...");
            //stateTv.setText("wait startPreview success...");
        }
        //Do something after 100ms

/*
                if(recorder != null) {

                    recorder.stop();
                    recorder.reset();
                    recorder.release();
                    recorder = null;
                    isAudioRecording = false;
                }

                  if(socket.isConnected()) {
                      try {
                          socket.close();
                      } catch (IOException e) {
                          e.printStackTrace();
                      }
                  }
*/




    }

    private void stopPreview() {

        mHandler.removeMessages(RESTOP_PREVIEW_SUCCESS);

        //判断预览数据管理器是否初始化化完成
        if(mImePreviewDataManager.checkPreviewDataManagerInitSuccess()) {

            //停止预览
            mImePreviewDataManager.stopPreview();

            Log.i(TAG, "stopPreview success");
            //stateTv.setText("stopPreview success");

            if(recorder != null) {

                recorder.stop();
                recorder.reset();
                recorder.release();
                recorder = null;
                isAudioRecording = false;
            }


        } else {
            mHandler.sendEmptyMessageDelayed(RESTOP_PREVIEW_SUCCESS, 2 * 1000);
            Log.i(TAG, "wait stopPreview success...");
            //stateTv.setText("wait stopPreview success...");
        }
    }

    private void audio_recorder_init() throws IOException {

        //Audio Recorder initialize
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); //for mp3= TREE_GPP last_edit = MPEG_4
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);    //for mp3= AMR_NB   last_edit = AAC
        //recorder.setAudioEncodingBitRate(48000);//48000
        //recorder.setAudioSamplingRate(16000);//16000

        recorder.setOutputFile(path2);
        //recorder.setOutputFile(parcelWrite.getFileDescriptor());

        try {
            recorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        recorder.start();


    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onEvent(MessageEvent event) {
        //Toast.makeText(this, "Hey, my message" + event.getMessage(), Toast.LENGTH_SHORT).show();
        get_mesaj = event.getMessage();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float[] values = sensorEvent.values;
            float x = values[0];
            float y = values[1];
            float z = values[2];


            if (z * last_z < 0) {
                //    vibrator.vibrate(500);
            }


            Log.d("TYPE_ACCELEROMETER", new Date() + String.format("x : %f y : %f z : %f", x, y, z));

            // X YANAL İSTİKAMET
            // y BOYSAL İSTİKAMET
            // Z DİKEY İSTİKAMET

            //last_x = x;
            //last_y = y;
            //last_z = z;

            Hesapxyz=Math.sqrt(x* x + y * y + z * z);
            Hesapyz=Math.sqrt( y * y + z * z);
            float[] ort = AddOrtalama(x, y, z);

            if (isstart) { // buton ile son veri veya bir kaç verinin kayıt edilmesi

                if (counter <= 3) {
                    counter++;
                } else {
                    isstart = false;
                    last_x = ort[0];
                    last_y = ort[1];
                    last_z = ort[2];


                    counter = 0;
                }
            }
            Hesap0 = Math.sqrt((x-last_x) * (x-last_x) + (y-last_y) * (y-last_y) + (z-last_z) * (z-last_z));
            Hesap0yz = Math.sqrt((y-last_y) * (y-last_y) + (z-last_z) * (z-last_z));
            sensorText.setText(String.format("x : %.4f y : %.4f z : %.4f yz0 : %.4f xyz0 : %.4f yz : %.4f xyz : %.4f  ", x-last_x, y-last_y, z-last_z, Hesap0yz,Hesap0,Hesapyz,Hesapxyz));

            String get_x = String.valueOf(x-last_x);
            String get_y = String.valueOf(y-last_y);
            String get_z = String.valueOf(z-last_z);

            //new ConnectTask().execute("");
/*
            try {
                new ConnectTask().execute("").get(1000,TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();

            }
*/
            //Realm transaction
            if(get_mesaj.equals("Hata")) {
                try {
                    realm.executeTransactionAsync(new Realm.Transaction() {
                        @Override
                        public void execute(Realm bgRealm) {

                            //creating auto increment of primary key
                            Number maxId = bgRealm.where(DataModel.class).max("id");
                            int nextId = (maxId == null) ? 1 : maxId.intValue() + 1;
                            DataModel model = bgRealm.createObject(DataModel.class, nextId);

                            //inserting data to realm DB
                            model.setX(get_x);
                            model.setY(get_y);
                            model.setZ(get_z);

                            //commit transaction
                            bgRealm.copyToRealm(model);

                            //getting all data from realm DB
                            RealmResults<DataModel> coordinate = bgRealm.where(DataModel.class).findAll();
                            Log.i("Coordinate:", "ds" + coordinate);

                            for (int i = 0; i < coordinate.size(); i++) {

                                coordinate.get(i).getID();
                                coordinate.get(i).getX();
                                coordinate.get(i).getY();
                                coordinate.get(i).getZ();
                            }
                        }

                    }, new Realm.Transaction.OnSuccess() {
                        @Override
                        public void onSuccess() {
                            // Toast.makeText(MainActivity.this, "Kayıt başarılı bir şekilde eklendi.", Toast.LENGTH_SHORT).show();

                        }
                    }, new Realm.Transaction.OnError() {
                        @Override
                        public void onError(Throwable error) {
                            Toast.makeText(MainActivity.this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {

                }
            }



            //sends the message to the server
            else if (mTcpClient != null) {

                try {
                    //mTcpClient.sendMessage(get_x + "," + get_y + "," + get_z);
                    mTcpClient.sendMessage(get_x);
                } catch (Exception e) {

                    Log.e("error_TCP_connection", "" + e);

                }
            }


       /*
            try {
                new ConnectTask().execute("").get(1000,TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();

            }

            try {
                TcpClient str_result = new ConnectTask().execute("").get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
*/



/*
            dataModel.setX(get_x);
            dataModel.setY(get_y);
            dataModel.setZ(get_z);
            //commit transaction
            realm.commitTransaction();
*/

/*
            //Sending Server on UDB
            Client.Message = get_x+get_y+get_z;
            Client.NachrichtSenden();
*/

            Alarm=0;
            if (GlobalLocation != null) {
                int BB1=BOLGE(GlobalLocation.getBearing());
                int BB0=BOLGE(son_bear);

                if (BB1==1 && BB0==4){
                    bear=(GlobalLocation.getBearing()-son_bear+360)/180*(float)Math.PI;
                }else if(BB1==4 && BB0==1){
                    bear=(son_bear-GlobalLocation.getBearing()+360)/180*(float)Math.PI;
                }else {
                    bear=(Math.abs(GlobalLocation.getBearing()-son_bear))/180*(float)Math.PI;
                }

                float ort_speed=(GlobalLocation.getSpeed()+son_speed)/2;



            if (last_z != 0) {
                if (Hesap0yz > 4) {
                    Alarm = 1;
                    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                    toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                    activityText.setText(String.format("x : %.4f y : %.4f z : %.4f yz0 : %.4f xyz0 : %.4f yz : %.4f xyz : %.4f ", x - last_x, y - last_y, z - last_z, Hesap0yz, Hesap0,Hesapyz,Hesapxyz));
                    saveToFile(new Date() + String.format("x : %.4f y : %.4f z : %.4f x0 : %.4f y0 : %.4f z0 : %.4f  yz0 : %.4f xyz0 : %.4f  yz : %.4f xyz : %.4f Latitude: %.6f Longitude: %.6f Bearing: %.1f Speed: %.1f LAT_ACC: %.4f LIN_ACC: %.4f Alarm : %.1f", x  , y  , z  ,x - last_x, y - last_y, z - last_z, Hesap0yz, Hesap0,Hesapyz,Hesapxyz, GlobalLocation.getLatitude(), GlobalLocation.getLongitude(), GlobalLocation.getBearing(), GlobalLocation.getSpeed() * 3.6, bear * ort_speed, GlobalLocation.getSpeed() - son_speed, Alarm));

                }else if((Math.abs(x-last_x)>4)){
                    Alarm = 2;
                    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_RING, 100);
                    toneG.startTone(ToneGenerator.TONE_CDMA_HIGH_PBX_SLS, 200);
                    activityText.setText(String.format("x : %.4f y : %.4f z : %.4f yz0 : %.4f xyz0 : %.4f yz : %.4f xyz : %.4f ", x - last_x, y - last_y, z - last_z, Hesap0yz, Hesap0,Hesapyz,Hesapxyz));
                    saveToFile(new Date() + String.format("x : %.4f y : %.4f z : %.4f x0 : %.4f y0 : %.4f z0 : %.4f  yz0 : %.4f xyz0 : %.4f  yz : %.4f xyz : %.4f Latitude: %.6f Longitude: %.6f Bearing: %.1f Speed: %.1f LAT_ACC: %.4f LIN_ACC: %.4f Alarm : %.1f", x  , y  , z  ,x - last_x, y - last_y, z - last_z, Hesap0yz, Hesap0,Hesapyz,Hesapxyz, GlobalLocation.getLatitude(), GlobalLocation.getLongitude(), GlobalLocation.getBearing(), GlobalLocation.getSpeed() * 3.6, bear * ort_speed, GlobalLocation.getSpeed() - son_speed, Alarm));

                }else{
                    saveToFile(new Date() + String.format("x : %.4f y : %.4f z : %.4f x0 : %.4f y0 : %.4f z0 : %.4f  yz0 : %.4f xyz0 : %.4f  yz : %.4f xyz : %.4f Latitude: %.6f Longitude: %.6f Bearing: %.1f Speed: %.1f LAT_ACC: %.4f LIN_ACC: %.4f Alarm : %.1f", x  , y  , z  ,x - last_x, y - last_y, z - last_z, Hesap0yz, Hesap0,Hesapyz,Hesapxyz, GlobalLocation.getLatitude(), GlobalLocation.getLongitude(), GlobalLocation.getBearing(), GlobalLocation.getSpeed() * 3.6, bear * ort_speed, GlobalLocation.getSpeed() - son_speed, Alarm));
                }

               // TONE_CDMA_EMERGENCY_RINGBACK
            }
                son_x=x;
                son_y=y;
                son_z=z;
                son_bear=GlobalLocation.getBearing();
                son_speed=GlobalLocation.getSpeed();



/*
            if (Hesap0 != 0) {
                if (((ort[4] - Hesap0yz) > 4)|| ((ort[4] - Hesap0yz) < -5)){
                    isRecordViolate = true;
                } else {
                    isRecordViolate = false;
                }


                if (Math.abs(ort[0] - last_x) > 4) {
                    isRecordViolate = true;
                } else {
                    isRecordViolate = false;
                }

                if (isRecordViolate) {

                    float IHLALL[] = new float[3];
                    IHLALL[0] = ort[3];
                    IHLALL[1] = ort[4];
                    IHLALL[2] = x;
                    IHLAL.add(IHLALL);
                    //activityText.setText(String.format("x : %f y : %f z : %f xyz : %f IHLAL : %d  ", ort[0], ort[1], ort[2], ort[3] - Hesap0,IS));
                    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                    toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                } else {
                    float Mak = 0;
                    float Avg = 0;
                    float Toplam=0;
                    float Makx = 0;
                    float Avgx = 0;
                    float Toplamx=0;

                    for (int i = 0; i < IHLAL.size(); i++) {
                        if (IHLAL.get(i)[1] > Mak) {
                            Mak = IHLAL.get(i)[1];
                            Toplam=(Toplam+IHLAL.get(i)[1]);
                        }
                        if (IHLAL.get(i)[2] > Mak) {
                            Makx = IHLAL.get(i)[2];
                            Toplamx=(Toplamx+IHLAL.get(i)[2]);
                        }

                    }



                    Avg=Toplam/IHLAL.size();
                    Avgx=Toplamx/IHLAL.size();

                    if (IHLAL.size() > 0) {
                        activityText.setText("IS:" + IHLAL.size() + " MAXyz:" + Mak + " AVGyz:"+ Avg + " MakX:"+ Makx +" AVGx:"+ Avgx) ;
                       // GlobalLocation.getAltitude();
                        saveToFile(("--------------------------------------------------------"));
                        saveToFile(new Date() + ("IS:" + IHLAL.size() + " MAXyz:" + Mak + " AVGyz:"+ Avg + " MakX:"+ Makx +" AVGx:"+ Avgx));
                        saveToFile(("--------------------------------------------------------"));
                        IHLAL = new ArrayList<>();
                        Mak = 0;
                        Avg = 0;
                    }
                }

            }
*/

               // saveToFile(new Date() + String.format("x : %f y : %f z : %f Latitude: %.6f Longitude: %.6f Bearing: %.1f Speed: %.1f LAT_ACC: %.1f LIN_ACC: %.1f", x, y, z, GlobalLocation.getLatitude(), GlobalLocation.getLongitude(), GlobalLocation.getBearing(), GlobalLocation.getSpeed(), bear*ort_speed,GlobalLocation.getSpeed()-son_speed));
                //GsensorText.setText(String.format("x : %f y : %f z : %f Latitude: %.6f Longitude: %.6f Bearing: %.1f Speed: %.1f LAT_ACC: %.1f LIN_ACC: %.1f", x, y, z, GlobalLocation.getLatitude(), GlobalLocation.getLongitude(), GlobalLocation.getBearing(), GlobalLocation.getSpeed(), bear*ort_speed,GlobalLocation.getSpeed()-son_speed));
            }
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float[] values = sensorEvent.values;
            float x = values[0];
            float y = values[1];
            float z = values[2];

            if (z * last_z < 0) {
            }
            Log.d("TYPE_GYROSCOPE", String.format("x : %f y : %f z : %f", x, y, z));
            last_x = x;
            last_y = y;
            last_z = z;
            GsensorText.setText(String.format("x : %f y : %f z : %f", x, y, z));
        }

    }




    public int BOLGE(Float ACI) {
        int B=0;
        if (GlobalLocation.getBearing()>269){
            B=4;
        }else if(GlobalLocation.getBearing()>179){
            B=3;
        }else if(GlobalLocation.getBearing()>89){
            B=2;
        }else if(GlobalLocation.getBearing()<90){
            B=1;
        }
        return B;
    }
    public void CalculateViolate(float val) {

    }

    protected float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    public static boolean saveToFile(String data) {
        try {
            new File(path).mkdir();
            File file = new File(path + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file, true);
            fileOutputStream.write((data + System.getProperty("line.separator")).getBytes());

            return true;
        } catch (FileNotFoundException ex) {
            Log.d("HATA", ex.getMessage());
        } catch (IOException ex) {
            Log.d("HATA", ex.getMessage());
        }
        return false;


    }


    boolean isstart = false;
    int counter = 0;


    public void XYZ0(View AAA) {

        OrtamalaListe = new ArrayList<>();
        last_x = 0;
        last_y = 0;
        last_z = 0;
        isstart = true;
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        first_x = last_x;
        first_y = last_y;
        first_z = last_z;
        //activityText.setText(String.format("x : %f y : %f z : %f", first_x, first_y, first_z));
    }

    // public float[] AddIHLAL(float x, float y, float z) {


//    }

    public float[] AddOrtalama(float x, float y, float z) {

        float[] ortalama = new float[5];
        ortalama[0] = 0;
        ortalama[1] = 0;
        ortalama[2] = 0;
        ortalama[3] = 0;
        ortalama[4] = 0;

        if (OrtamalaListe.size() < 3) { //ortalama boyutu
            float[] val = new float[5];
            val[0] = x;
            val[1] = y;
            val[2] = z;
            val[3] = (float) Math.sqrt((x) * (x) + (y) * (y) + (z) * (z));
            val[4] = (float) Math.sqrt((y) * (y) + (z - f) * (z - f));

            OrtamalaListe.add(val);
            ortalama[0] = ortalama[0] + val[0];
            ortalama[1] = ortalama[1] + val[1];
            ortalama[2] = ortalama[2] + val[2];
            ortalama[3] = ortalama[3] + val[3];
            ortalama[4] = ortalama[4] + val[4];

        } else {
            for (int i = 0; i < OrtamalaListe.size() - 1; i++) {
                if (i > 0) {
                    OrtamalaListe.set(i - 1, OrtamalaListe.get(i));
                }
                ortalama[0] = ortalama[0] + OrtamalaListe.get(i)[0];
                ortalama[1] = ortalama[1] + OrtamalaListe.get(i)[1];
                ortalama[2] = ortalama[2] + OrtamalaListe.get(i)[2];
                ortalama[3] = ortalama[3] + OrtamalaListe.get(i)[3];
                ortalama[4] = ortalama[4] + OrtamalaListe.get(i)[4];
            }
            float[] val = new float[5];
            val[0] = x;
            val[1] = y;
            val[2] = z;
            val[3] = (float) Math.sqrt((x) * (x) + (y) * (y) + (z - f) * (z - f));
            val[4] = (float) Math.sqrt((y) * (y) + (z - f) * (z - f));
            OrtamalaListe.set(OrtamalaListe.size() - 1, val);
            ortalama[0] = ortalama[0] + val[0];
            ortalama[1] = ortalama[1] + val[1];
            ortalama[2] = ortalama[2] + val[2];
            ortalama[3] = ortalama[3] + val[3];
            ortalama[4] = ortalama[4] + val[4];
        }
        ortalama[0] = ortalama[0] / OrtamalaListe.size();
        ortalama[1] = ortalama[1] / OrtamalaListe.size();
        ortalama[2] = ortalama[2] / OrtamalaListe.size();
        ortalama[3] = ortalama[3] / OrtamalaListe.size();
        ortalama[4] = ortalama[4] / OrtamalaListe.size();

        return ortalama;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {


    }

    /*

        @Override
        public void onActivityUpdated(DetectedActivity detectedActivity) {
            showActivity(detectedActivity);

        }

        @Override
        public void onGeofenceTransition(TransitionGeofence transitionGeofence) {
            showGeofence(transitionGeofence.getGeofenceModel().toGeofence(), transitionGeofence.getTransitionType());

        }

        @Override
        public void onLocationUpdated(Location location) {
            showLocation2(location);

        }

    */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_ID && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocation();
        }
    }

    private void startLocation() {

      /*  provider = new LocationGooglePlayServicesProvider();
        provider.setCheckLocationSettings(true);
*/
        //    SmartLocation smartLocation = new SmartLocation.Builder(this).logging(true).build();

        //     smartLocation.location(new LocationBasedOnActivityProvider(this)).config(LocationParams.NAVIGATION).start(this);


        //  smartLocation.location(provider).config(LocationParams.NAVIGATION).start(this);
        //     smartLocation.activity().start(this);


        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // You need to ask the user to enable the permissions
        } else {

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 0, this);

         
            /*
            tracker = new LocationTracker(getApplicationContext(), new TrackerSettings()
                    .setUseGPS(true)
                    .setUseNetwork(false)
                    .setUsePassive(false)
                    .setTimeBetweenUpdates( 10 * 1000)
                  //  .setMetersBetweenUpdates(100)
            ) {


                @Override
                public void onLocationFound(Location location) {
                    // Do some stuff when a new GPS Location has been found
                    showLocation2(location);
                }

                @Override
                public void onTimeout() {

                }
            };

            tracker.startListening();
            */
        }




        /*
        // Create some geofences
        GeofenceModel mestalla = new GeofenceModel.Builder("1").setTransition(Geofence.GEOFENCE_TRANSITION_ENTER).setLatitude(39.47453120000001).setLongitude(-0.358065799999963).setRadius(500).build();
        smartLocation.geofencing().add(mestalla).start(this);
        */
    }

    private void stopLocation() {
        SmartLocation.with(this).location().stop();
        locationText.setText("Location Durdu!");

        SmartLocation.with(this).activity().stop();
        //     activityText.setText("Activity Recognition stopped!");

        SmartLocation.with(this).geofencing().stop();
        geofenceText.setText("Geofencing Durdu!");
    }


    private void showLocation(Location location) {
        if (location != null) {
            final String text = String.format("Latitude %.6f, Longitude %.6f",
                    location.getLatitude(),
                    location.getLongitude());
            locationText.setText(text);

            // We are going to get the address for the current position
            SmartLocation.with(this).geocoding().reverse(location, new OnReverseGeocodingListener() {
                @Override
                public void onAddressResolved(Location original, List<Address> results) {
                    if (results.size() > 0) {
                        Address result = results.get(0);
                        StringBuilder builder = new StringBuilder(text);
                        builder.append("\n[Reverse Geocoding] ");
                        List<String> addressElements = new ArrayList<>();
                        for (int i = 0; i <= result.getMaxAddressLineIndex(); i++) {
                            addressElements.add(result.getAddressLine(i));
                        }
                        builder.append(TextUtils.join(", ", addressElements));
                        locationText.setText(builder.toString());
                    }
                }
            });
        } else {
            locationText.setText("Null location");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showLocation2(Location location) {
        if (location != null) {
            GlobalLocation = location;
       String text = String.format("Latitude %.6f, Longitude %.6f , Bearing %.1f, Accu %.1f, Speed %.1f",
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getBearing(),
                    location.getAccuracy(),
                    //location.getVerticalAccuracyMeters(),

                    //location.getExtras(),
                    //location.getBearingAccuracyDegrees(),
                    location.getSpeed()*3.6




                    //location.getTime()
                    //location.getSpeedAccuracyMetersPerSecond()
            );
            locationText.setText(text);







        } else {
            locationText.setText("Null location");
        }
    }

    private void showActivity(DetectedActivity detectedActivity) {
        if (detectedActivity != null) {
       /*     activityText.setText(
                    String.format("Activity %s with %d%% confidence",
                            getNameFromType(detectedActivity),
                            detectedActivity.getConfidence())
            );*/
        } else {
            //   activityText.setText("Null activity");
        }
    }

    private void showGeofence(Geofence geofence, int transitionType) {
        if (geofence != null) {
            geofenceText.setText("Transition " + getTransitionNameFromType(transitionType) + " for Geofence with id = " + geofence.getRequestId());
        } else {
            geofenceText.setText("Null geofence");
        }
    }

    private String getNameFromType(DetectedActivity activityType) {
        switch (activityType.getType()) {
            case DetectedActivity.IN_VEHICLE:
                return "in_vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "on_bicycle";
            case DetectedActivity.ON_FOOT:
                return "on_foot";
            case DetectedActivity.STILL:
                return "still";
            case DetectedActivity.TILTING:
                return "tilting";
            default:
                return "unknown";
        }
    }

    private String getTransitionNameFromType(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return "enter";
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return "exit";
            default:
                return "dwell";
        }
    }

    private boolean GetPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_ID);
            return false;
        }
        return true;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
        sensorManager.unregisterListener(this);
        stopLocation();
        if (tracker != null) {
            tracker.stopListening();
        }

        if (mTcpClient != null) {
            mTcpClient.stopClient();
        }
    }

    @Override
    protected void onPause() {
        if (tracker != null) {
            tracker.stopListening();
        }
        super.onPause();
    }


    @Override
    public void onLocationChanged(Location location) {

        showLocation2(location);


    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

   //for TCP AsyncTask, for connecting to your server and receiving responses on the UI thread
    public class ConnectTask extends AsyncTask<String, String, TcpClient> {

       @Override
       protected TcpClient doInBackground(String... message) {

           //we create a TCPClient object
           mTcpClient = new TcpClient(new TcpClient.OnMessageReceived() {
               @Override
               //here the messageReceived method is implemented
               public void messageReceived(String message) {
                   //this method calls the onProgressUpdate
                   publishProgress(message);
               }
           });
           mTcpClient.run();
           //msg = mTcpClient.msg1();

           return null;
       }

       @Override
       protected void onProgressUpdate(String... values) {
           super.onProgressUpdate(values);
           //response received from server
           Log.d("test", "response " + values[0]);
           //process server response here....

       }



   }
}

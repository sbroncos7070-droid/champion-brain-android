package com.championbrain.overlay;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.*;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.nio.ByteBuffer;

public class OverlayService extends Service {
    private static final String CHANNEL="champion_brain_capture";
    private WindowManager wm; private View bubble,panel; private MediaProjection projection; private ImageReader reader; private VirtualDisplay display;
    private TextView play,reason,confidence,status;

    @Override public void onCreate(){super.onCreate();createChannel();startForeground(7,new Notification.Builder(this,CHANNEL).setContentTitle("Champion Brain active").setContentText("Tap the floating bubble to analyze a battle").setSmallIcon(android.R.drawable.ic_menu_view).build());wm=(WindowManager)getSystemService(WINDOW_SERVICE);showBubble();}
    @Override public int onStartCommand(Intent intent,int flags,int startId){if(projection==null&&intent!=null){int code=intent.getIntExtra("resultCode",Activity.RESULT_CANCELED);Intent data=intent.getParcelableExtra("resultData");MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);projection=m.getMediaProjection(code,data);projection.registerCallback(new MediaProjection.Callback(){@Override public void onStop(){stopSelf();}},new Handler(Looper.getMainLooper()));prepareCapture();}return START_NOT_STICKY;}
    private WindowManager.LayoutParams params(int w,int h){return new WindowManager.LayoutParams(w,h,android.os.Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);}
    private void showBubble(){TextView b=new TextView(this);b.setText("CB");b.setTextColor(Color.rgb(12,17,29));b.setTextSize(17);b.setGravity(Gravity.CENTER);GradientDrawable bg=new GradientDrawable();bg.setColor(0xFFF7C948);bg.setShape(GradientDrawable.OVAL);b.setBackground(bg);WindowManager.LayoutParams p=params(150,150);p.gravity=Gravity.TOP|Gravity.END;p.x=20;p.y=280;final int[] start={0,0};b.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_DOWN){start[0]=p.x+(int)e.getRawX();start[1]=p.y-(int)e.getRawY();return true;}if(e.getAction()==MotionEvent.ACTION_MOVE){p.x=start[0]-(int)e.getRawX();p.y=start[1]+(int)e.getRawY();wm.updateViewLayout(v,p);return true;}if(e.getAction()==MotionEvent.ACTION_UP){if(Math.abs(e.getRawX()-(start[0]-p.x))<18)togglePanel();return true;}return false;});bubble=b;wm.addView(b,p);}
    private void togglePanel(){if(panel!=null){wm.removeView(panel);panel=null;return;}LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(32,26,32,26);GradientDrawable bg=new GradientDrawable();bg.setColor(0xF211192B);bg.setCornerRadius(34);bg.setStroke(2,0xFF3A4C70);box.setBackground(bg);TextView head=tv("CHAMPION BRAIN",13,0xFFF7C948);box.addView(head);play=tv("Ready to analyze",24,Color.WHITE);play.setPadding(0,18,0,8);box.addView(play);reason=tv("Open the move-selection screen, then tap Analyze Screen.",16,0xFFC6D0E7);box.addView(reason);confidence=tv("",14,0xFF56DC9A);confidence.setPadding(0,10,0,14);box.addView(confidence);status=tv("Screenshots are processed and discarded.",12,0xFF8796B7);box.addView(status);Button analyze=new Button(this);analyze.setText("Analyze Screen");analyze.setAllCaps(false);analyze.setOnClickListener(v->capture());box.addView(analyze,new LinearLayout.LayoutParams(-1,120));Button close=new Button(this);close.setText("Close assistant");close.setAllCaps(false);close.setOnClickListener(v->stopSelf());box.addView(close,new LinearLayout.LayoutParams(-1,105));WindowManager.LayoutParams p=params((int)(getResources().getDisplayMetrics().widthPixels*.88f),WindowManager.LayoutParams.WRAP_CONTENT);p.gravity=Gravity.CENTER;panel=box;wm.addView(box,p);}
    private TextView tv(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    private void prepareCapture(){DisplayMetrics dm=getResources().getDisplayMetrics();reader=ImageReader.newInstance(dm.widthPixels,dm.heightPixels,PixelFormat.RGBA_8888,2);display=projection.createVirtualDisplay("ChampionBrain",dm.widthPixels,dm.heightPixels,dm.densityDpi,0,reader.getSurface(),null,null);}
    private void capture(){if(reader==null){status.setText("Capture session is not ready.");return;}status.setText("Reading battle screen…");new Handler(Looper.getMainLooper()).postDelayed(()->{Image image=reader.acquireLatestImage();if(image==null){status.setText("No frame yet. Tap Analyze Screen again.");return;}Bitmap bitmap=toBitmap(image);image.close();TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(InputImage.fromBitmap(bitmap,0)).addOnSuccessListener(text->{BattleAnalyzer.Result r=BattleAnalyzer.analyze(text.getText());play.setText(r.play);reason.setText(r.reason);confidence.setText(r.confidence+"% confidence");status.setText("Screen analyzed locally · image discarded");bitmap.recycle();}).addOnFailureListener(e->{status.setText("I couldn't read this screen. Show the move menu and try again.");bitmap.recycle();});},180);}
    private Bitmap toBitmap(Image image){Image.Plane p=image.getPlanes()[0];ByteBuffer b=p.getBuffer();int pixelStride=p.getPixelStride(),rowStride=p.getRowStride(),padding=rowStride-pixelStride*image.getWidth();Bitmap full=Bitmap.createBitmap(image.getWidth()+padding/pixelStride,image.getHeight(),Bitmap.Config.ARGB_8888);full.copyPixelsFromBuffer(b);return Bitmap.createBitmap(full,0,0,image.getWidth(),image.getHeight());}
    private void createChannel(){if(android.os.Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(CHANNEL,"Champion Brain capture",NotificationManager.IMPORTANCE_LOW);getSystemService(NotificationManager.class).createNotificationChannel(c);}}
    @Override public void onDestroy(){if(panel!=null)wm.removeView(panel);if(bubble!=null)wm.removeView(bubble);if(display!=null)display.release();if(reader!=null)reader.close();if(projection!=null)projection.stop();super.onDestroy();}
    @Override public android.os.IBinder onBind(Intent intent){return null;}
}

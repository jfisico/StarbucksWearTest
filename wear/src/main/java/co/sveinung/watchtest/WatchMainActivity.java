package co.sveinung.watchtest;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.wearable.activity.InsetActivity;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;
import android.widget.ImageView;
import android.graphics.Typeface;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.zxing.WriterException;
import com.google.zxing.datamatrix.DataMatrixWriter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;

public class WatchMainActivity extends InsetActivity {

    private static final String TAG = WatchMainActivity.class.getSimpleName();
    private TextView mTextView;
    private GoogleApiClient apiClient;
    private final Handler handler = new Handler();
    public static final String PREFS_NAME = "MyPrefsFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiClient = new GoogleApiClient.Builder(this, onConnectedListener, onConnectionListener).addApi(Wearable.API).build();
        apiClient.connect();
    }

    @Override
    public void onReadyForContent() {
        setContentView(R.layout.activity_main);
       // mTextView = (TextView) findViewById(R.id.text);
        //mTextView.setText("Ready!");
        //BitMatrix bitMatrix = new DataMatrixWriter().encode("124958761310", BarcodeFormat.DATA_MATRIX, 1080, 1080, null);
        //int height = bitMatrix.getHeight(); //height is always 14, it doesn't matter what value I pass to the encoder

        // ImageView to display the QR code in.  This should be defined in
        // your Activity's XML layout file
        ImageView imageView = (ImageView) findViewById(R.id.qrCode);
        //SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences( this );
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String qrData = settings.getString( "barcode", "" );
        int qrCodeDimention = 500;

        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(qrData, null,
                Contents.Type.TEXT, BarcodeFormat.PDF_417.toString(), qrCodeDimention);

        try {
            Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
            imageView.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }

        mTextView = (TextView)findViewById(R.id.text);

        /*// set barcode font for TextView.
        // ttf file must be placed is assets/fonts
        Typeface font = Typeface.createFromAsset(this.getAssets(), "fonts/EanP72Tt.ttf");
        mTextView.setTypeface(font);

        // generate barcode string
        EAN13CodeBuilder bb = new EAN13CodeBuilder("124958761310");
        mTextView.setText(bb.getCode());*/
        Log.d(TAG, "TextView: " + mTextView.getText() + " view=" + mTextView);
        Log.d(TAG, "Watch ready for content!");
    }

    private final GoogleApiClient.ConnectionCallbacks onConnectedListener = new GoogleApiClient.ConnectionCallbacks() {

        @Override
        public void onConnected(Bundle bundle) {
            Log.d(TAG, "Connected, start listening for data!");
            Wearable.DataApi.addListener(apiClient, onDataChangedListener);
        }

        @Override
        public void onConnectionSuspended(int i) {
        }
    };

    private final GoogleApiClient.OnConnectionFailedListener onConnectionListener = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.d(TAG, "Connection failed: " + connectionResult);
        }
    };

    public DataApi.DataListener onDataChangedListener = new DataApi.DataListener() {
        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {
            Log.d(TAG, "Data changed: " + dataEvents);
            for (DataEvent event : dataEvents) {
                if (event.getType() == DataEvent.TYPE_DELETED) {
                    Log.d(TAG, "DataItem deleted: " + event.getDataItem().getUri());
                    handler.post(onNewCount(null));
                } else if (event.getType() == DataEvent.TYPE_CHANGED) {

                    Log.d(TAG, "DataItem changed: " + event.getDataItem().getUri());
                    if (event.getDataItem().getUri().getPath().endsWith("/barcode")) {
                        DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                        String barcode = dataMapItem.getDataMap().getString("barcode");
                        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("barcode", barcode);
                        editor.commit();
                        handler.post(onNewCount(barcode));
                    }
                }
            }
        }
    };

    private Runnable onNewCount(final String barcode) {
        return new Runnable() {
            @Override
            public void run() {
                if (mTextView != null) {
                    if (barcode == null) {
                        mTextView.setText("Stopped!");
                    }
                    else {
                        //mTextView.setText("Count is: " + barcode);
                        ImageView imageView = (ImageView) findViewById(R.id.qrCode);
                        //SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences( this );
                        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                        String qrData = settings.getString( "barcode", "" );
                        int qrCodeDimention = 500;

                        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(qrData, null,
                                Contents.Type.TEXT, BarcodeFormat.PDF_417.toString(), qrCodeDimention);

                        try {
                            Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
                            imageView.setImageBitmap(bitmap);
                        } catch (WriterException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
    }
}

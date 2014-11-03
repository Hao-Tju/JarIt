
package net.jarit;

import org.apache.http.HttpResponse;

import com.github.johnpersano.supertoasts.SuperToast;
import com.github.johnpersano.supertoasts.util.Style;

import net.jarit.JarItService.HttpResponseCallback;
import net.jarit.JarItService.JarItServiceBinder;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.sax.TextElementListener;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

public class SetNewJarActivity extends FragmentActivity {
    private static final String TAG = SetNewJarActivity.class.getSimpleName();

    private Location location;

    private JarItServiceBinder binder;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (JarItService.JarItServiceBinder) service;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent receivedIntent = getIntent();
        location = (Location) receivedIntent.getExtras().get(Constant.LOCATION);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        ReadyDialog dialog = new ReadyDialog(this);
        dialog.show(this.getSupportFragmentManager(), null);

        Intent intent = new Intent(this, JarItService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @SuppressLint("ValidFragment")
    private class ReadyDialog extends DialogFragment {
        private Context mContext;

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            setCancelable(false);
            this.setStyle(R.style.DialogStyle, R.style.TransparentTheme);
            return (new DialogContent(mContext));
        }

        public ReadyDialog(Context context) {
            mContext = context;
        }

        class DialogContent extends AlertDialog implements View.OnClickListener {
            private Button trashButton;
            private Button addButton;
            private EditText editText;

            public DialogContent(Context context) {
                super(context);
            }

            @Override
            protected void onCreate(Bundle savedInstanceState) {
                requestWindowFeature(Window.FEATURE_NO_TITLE);
                setContentView(R.layout.activity_set_new_jar);
                trashButton = (Button) findViewById(R.id.trashButton);
                addButton = (Button) findViewById(R.id.setButton);
                editText = (EditText) findViewById(R.id.new_jar_text);

                trashButton.setOnClickListener(this);
                addButton.setOnClickListener(this);

                getWindow().setGravity(Gravity.CENTER);
            }

            @Override
            public void onDetachedFromWindow() {
                SetNewJarActivity.this.finish();
                unbindService(serviceConnection);
            }

            @Override
            public void onClick(View v) {
                if (v == trashButton) {
                    cancel();
                } else if (v == addButton) {
                    String text = editText.getText().toString();
                    binder.submitNewTreasure(text, location, new HttpResponseCallback() {
                        @Override
                        public void onResponse(HttpResponse resp) {
                            // TODO check return value
                            SuperToast toast = SuperToast.create(DialogContent.this.getContext(),
                                    "New Treasure Ready!.", SuperToast.Duration.MEDIUM,
                                    Style.getStyle(Style.ORANGE, SuperToast.Animations.FLYIN));
                            toast.setIcon(SuperToast.Icon.Light.SAVE, SuperToast.IconPosition.LEFT);
                            toast.show();
                            DialogContent.this.cancel();
                        }
                    });
                }

            }
        }
    }
}

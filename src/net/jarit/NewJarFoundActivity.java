
package net.jarit;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

public class NewJarFoundActivity extends FragmentActivity {
    private static final String TAG = NewJarFoundActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        ReadyDialog dialog = new ReadyDialog(this);
        dialog.show(this.getSupportFragmentManager(), null);
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "destroy");
        super.onDestroy();
    }

    private void onBindSuccess() {
        Log.i(TAG, "show dialog");
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

        class DialogContent extends AlertDialog {
            
            public DialogContent(Context context) {
                super(context);
            }

            @Override
            protected void onCreate(Bundle savedInstanceState) {
                requestWindowFeature(Window.FEATURE_NO_TITLE);
                setContentView(R.layout.activity_new_jar_found);
                getWindow().setGravity(Gravity.CENTER);
            }

            @Override
            public void onDetachedFromWindow() {
                NewJarFoundActivity.this.finish();
            }
        }
    }
}

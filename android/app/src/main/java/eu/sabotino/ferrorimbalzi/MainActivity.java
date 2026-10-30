package eu.sabotino.ferrorimbalzi;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Toast;

public class MainActivity extends Activity {
    private GameView gameView;
    private long lastBack = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        applyImmersive();

        gameView = new GameView(this);
        setContentView(gameView);
    }

    private void applyImmersive() {
        Window window = getWindow();

        // 1. Estendi la finestra a tutto schermo sotto al notch / punch-hole della fotocamera (Android P+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(lp);
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // 2. Nascondi permanentemente barra di stato e barra di navigazione
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersive();
    }

    @Override
    protected void onPause() {
        if (gameView != null) {
            gameView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyImmersive();
        if (gameView != null) {
            gameView.resume();
        }
    }

    @Override
    public void onBackPressed() {
        if (gameView != null && !gameView.paused && "wave".equals(gameView.game.phase)) {
            gameView.togglePause();
            Toast.makeText(this, "Battaglia sospesa", Toast.LENGTH_SHORT).show();
            return;
        }

        long now = SystemClock.elapsedRealtime();
        if (lastBack > 0 && now - lastBack < 2000) {
            finish();
            return;
        }
        lastBack = now;
        Toast.makeText(this, "Premi ancora Indietro per abbandonare la rocca", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        if (gameView != null) {
            gameView.pause();
        }
        super.onDestroy();
    }
}

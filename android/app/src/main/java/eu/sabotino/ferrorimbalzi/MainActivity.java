package eu.sabotino.ferrorimbalzi;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

public class MainActivity extends Activity {
    private GameView gameView;
    private long lastBack = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        applyImmersive();

        gameView = new GameView(this);
        setContentView(gameView);
    }

    private void applyImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
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
        if (gameView != null) {
            gameView.resume();
        }
    }

    @Override
    public void onBackPressed() {
        if (gameView != null && !gameView.paused && "wave".equals(gameView.game.phase)) {
            gameView.togglePause();
            Toast.makeText(this, "Gioco in pausa", Toast.LENGTH_SHORT).show();
            return;
        }

        long now = SystemClock.elapsedRealtime();
        if (lastBack > 0 && now - lastBack < 2000) {
            finish();
            return;
        }
        lastBack = now;
        Toast.makeText(this, "Premi ancora Indietro per uscire", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        if (gameView != null) {
            gameView.pause();
        }
        super.onDestroy();
    }
}


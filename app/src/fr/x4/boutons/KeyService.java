package fr.x4.boutons;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Capte les 2 boutons programmables du Core-X4 (F11 = bouton 1, F12 = bouton 2 via gpio-keys.kl)
 * et distingue appui court / double / long / triple.
 */
public class KeyService extends AccessibilityService {
    static final int LONG_MS = 600;   // durée d'un appui long
    static final int MULTI_MS = 400;  // délai max entre 2 appuis d'un double/triple

    private final Handler h = new Handler(Looper.getMainLooper());
    private final Tracker[] trackers = {new Tracker(0), new Tracker(1)};

    private class Tracker {
        final String button;
        int count;
        boolean longFired;
        boolean downSeen;  // un UP sans DOWN = l'appui qui a réveillé l'écran (le DOWN n'est pas transmis)
        final Runnable longPress = new Runnable() {
            @Override public void run() { longFired = true; count = 0; fire("long"); }
        };
        final Runnable finish = new Runnable() {
            @Override public void run() { int n = count; count = 0; fire(n >= 2 ? "double" : "short"); }
        };

        Tracker(int i) { button = Actions.BUTTONS[i]; }

        void fire(String gesture) { Actions.run(KeyService.this, Actions.get(KeyService.this, button, gesture)); }

        boolean configured(String gesture) { return !"NONE".equals(Actions.get(KeyService.this, button, gesture)); }

        void down() {
            h.removeCallbacks(finish);
            longFired = false;
            downSeen = true;
            h.postDelayed(longPress, LONG_MS);
        }

        void up() {
            h.removeCallbacks(longPress);
            if (!downSeen) return;
            downSeen = false;
            if (longFired) { longFired = false; return; }
            count++;
            boolean wantDouble = configured("double"), wantTriple = configured("triple");
            if (count >= 3) { count = 0; fire("triple"); return; }
            if (count == 2 && !wantTriple) { count = 0; fire("double"); return; }
            if (count == 1 && !wantDouble && !wantTriple) { count = 0; fire("short"); return; }
            h.postDelayed(finish, MULTI_MS);
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        int idx;
        if (e.getKeyCode() == KeyEvent.KEYCODE_F11) idx = 0;
        else if (e.getKeyCode() == KeyEvent.KEYCODE_F12) idx = 1;
        else return false;  // toutes les autres touches passent normalement
        if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0) trackers[idx].down();
        else if (e.getAction() == KeyEvent.ACTION_UP) trackers[idx].up();
        return true;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { }
}

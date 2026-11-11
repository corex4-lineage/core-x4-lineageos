package fr.x4.boutons;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.widget.Toast;

/** Actions disponibles et leur exécution. Une action est stockée sous forme de texte : "TORCH", "APP:com.exemple". */
final class Actions {
    static final String PREFS = "x4boutons";

    static final String[] IDS = {"NONE", "TORCH", "CAMERA", "APP", "SCREENSHOT", "NOTIFICATIONS", "QUICK_SETTINGS", "ASSISTANT", "RINGER"};
    static final String[] LABELS = {"Rien", "Lampe torche (marche/arrêt)", "Appareil photo", "Lancer une application…",
            "Capture d'écran", "Volet des notifications", "Réglages rapides", "Assistant vocal", "Sonnerie / vibreur"};

    static final String[] BUTTONS = {"b1", "b2"};
    static final String[] BUTTON_LABELS = {"Bouton 1 (haut)", "Bouton 2 (bas)"};
    static final String[] GESTURES = {"short", "double", "long", "triple"};
    static final String[] GESTURE_LABELS = {"Appui court", "Double appui", "Appui long", "Triple appui"};

    private static Boolean torchOn = false;
    private static String torchId;
    private static boolean torchCallbackSet;

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static String get(Context c, String button, String gesture) {
        String def = "NONE";
        if (button.equals("b1") && gesture.equals("long")) def = "TORCH";
        if (button.equals("b2") && gesture.equals("long")) def = "CAMERA";
        return prefs(c).getString(button + "_" + gesture, def);
    }

    static void set(Context c, String button, String gesture, String action) {
        prefs(c).edit().putString(button + "_" + gesture, action).apply();
    }

    static String label(Context c, String action) {
        if (action.startsWith("APP:")) {
            String pkg = action.substring(4);
            try {
                PackageManager pm = c.getPackageManager();
                return "Appli : " + pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0));
            } catch (PackageManager.NameNotFoundException e) {
                return "Appli : " + pkg + " (absente)";
            }
        }
        for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(action)) return LABELS[i];
        return action;
    }

    /** Exécute l'action. Retourne false si rien n'est configuré. */
    static boolean run(AccessibilityService s, String action) {
        if (action == null || action.equals("NONE")) return false;
        try {
            if (action.equals("TORCH")) toggleTorch(s);
            else if (action.equals("CAMERA")) openCamera(s);
            else if (action.startsWith("APP:")) openApp(s, action.substring(4));
            else if (action.equals("SCREENSHOT")) s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT);
            else if (action.equals("NOTIFICATIONS")) s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
            else if (action.equals("QUICK_SETTINGS")) s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS);
            else if (action.equals("ASSISTANT")) start(s, new Intent(Intent.ACTION_VOICE_COMMAND));
            else if (action.equals("RINGER")) toggleRinger(s);
            vibrate(s);
            return true;
        } catch (Exception e) {
            toast(s, "X4 Boutons : " + e.getMessage());
            return false;
        }
    }

    private static void start(Context c, Intent i) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(i);
    }

    private static void openCamera(Context c) {
        KeyguardManager km = (KeyguardManager) c.getSystemService(Context.KEYGUARD_SERVICE);
        boolean locked = km != null && km.isKeyguardLocked();
        start(c, new Intent(locked ? MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE
                : MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA));
    }

    private static void openApp(Context c, String pkg) {
        Intent i = c.getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) { toast(c, "Application introuvable : " + pkg); return; }
        start(c, i);
    }

    private static void toggleRinger(Context c) {
        AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        if (am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
            toast(c, "Vibreur");
        } else {
            am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            toast(c, "Sonnerie");
        }
    }

    private static void toggleTorch(Context c) throws Exception {
        CameraManager cm = (CameraManager) c.getSystemService(Context.CAMERA_SERVICE);
        if (torchId == null) {
            for (String id : cm.getCameraIdList()) {
                Boolean flash = cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer facing = cm.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING);
                if (Boolean.TRUE.equals(flash) && facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) { torchId = id; break; }
            }
            if (torchId == null) throw new Exception("pas de flash trouvé");
        }
        if (!torchCallbackSet) {
            cm.registerTorchCallback(new CameraManager.TorchCallback() {
                @Override public void onTorchModeChanged(String id, boolean enabled) {
                    if (id.equals(torchId)) torchOn = enabled;
                }
            }, new Handler(Looper.getMainLooper()));
            torchCallbackSet = true;
        }
        cm.setTorchMode(torchId, !torchOn);
    }

    private static void vibrate(Context c) {
        Vibrator v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) v.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    static void toast(Context c, String msg) {
        Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
    }
}

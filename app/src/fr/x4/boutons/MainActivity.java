package fr.x4.boutons;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Écran de réglage façon Paramètres LineageOS (Material You, couleurs dynamiques). */
public class MainActivity extends Activity {
    private LinearLayout root;
    private TextView statusTitle, statusSummary;
    private LinearLayout statusCard;
    private int accent, textPrimary, textSecondary;
    private boolean night;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        TypedArray a = obtainStyledAttributes(new int[]{android.R.attr.colorAccent,
                android.R.attr.textColorPrimary, android.R.attr.textColorSecondary});
        accent = a.getColor(0, 0xFF167C80);
        textPrimary = a.getColor(1, 0xFF000000);
        textSecondary = a.getColor(2, 0xFF666666);
        a.recycle();
        if (getActionBar() != null) getActionBar().hide();

        ScrollView sv = new ScrollView(this);
        sv.setFitsSystemWindows(true);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, dp(16), 0, dp(24));
        sv.addView(root);
        setContentView(sv);
        build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    private int sysColor(int lightRes, int darkRes) { return getColor(night ? darkRes : lightRes); }

    private void build() {
        // Grand titre, comme la barre repliable des Paramètres
        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(34);
        title.setTextColor(textPrimary);
        title.setPadding(dp(24), dp(32), dp(24), dp(20));
        root.addView(title);

        // Encart d'état (cliquable)
        statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setPadding(dp(20), dp(18), dp(20), dp(18));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(28));
        bg.setColor(sysColor(android.R.color.system_accent1_100, android.R.color.system_accent2_800));
        statusCard.setBackground(bg);
        statusCard.setClickable(true);
        statusCard.setForeground(getDrawable(ripple()));
        statusCard.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
        });
        statusTitle = new TextView(this);
        statusTitle.setTextSize(18);
        statusTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        statusTitle.setTextColor(sysColor(android.R.color.system_accent1_900, android.R.color.system_accent1_100));
        statusSummary = new TextView(this);
        statusSummary.setTextSize(14);
        statusSummary.setTextColor(sysColor(android.R.color.system_accent1_800, android.R.color.system_accent1_200));
        statusSummary.setPadding(0, dp(4), 0, 0);
        statusCard.addView(statusTitle);
        statusCard.addView(statusSummary);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(16), 0, dp(16), dp(8));
        root.addView(statusCard, lp);

        for (int bi = 0; bi < Actions.BUTTONS.length; bi++) {
            category(Actions.BUTTON_LABELS[bi]);
            for (int gi = 0; gi < Actions.GESTURES.length; gi++) addRow(Actions.BUTTONS[bi], Actions.GESTURES[gi], Actions.GESTURE_LABELS[gi]);
        }

        category("À savoir");
        info("Écran éteint : le premier appui rallume l'écran, l'appui suivant déclenche l'action (y compris sur l'écran de verrouillage).");
        info("Appui long : " + KeyService.LONG_MS + " ms. Double / triple : appuis espacés de moins de " + KeyService.MULTI_MS
                + " ms. Si seul l'appui court est réglé, l'action est immédiate.");
    }

    private int ripple() {
        TypedArray a = obtainStyledAttributes(new int[]{android.R.attr.selectableItemBackground});
        int r = a.getResourceId(0, 0);
        a.recycle();
        return r;
    }

    private void category(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(14);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setTextColor(accent);
        t.setPadding(dp(24), dp(28), dp(24), dp(8));
        root.addView(t);
    }

    private void info(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(14);
        t.setTextColor(textSecondary);
        t.setPadding(dp(24), dp(6), dp(24), dp(6));
        root.addView(t);
    }

    private void addRow(final String button, final String gesture, String gestureLabel) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(72));
        row.setPadding(dp(24), dp(14), dp(24), dp(14));
        row.setBackgroundResource(ripple());
        row.setClickable(true);
        TextView t = new TextView(this);
        t.setText(gestureLabel);
        t.setTextSize(17);
        t.setTextColor(textPrimary);
        final TextView s = new TextView(this);
        s.setTextSize(14);
        s.setTextColor(textSecondary);
        s.setPadding(0, dp(2), 0, 0);
        s.setText(Actions.label(this, Actions.get(this, button, gesture)));
        row.addView(t);
        row.addView(s);
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickAction(button, gesture, s); }
        });
        root.addView(row);
    }

    private void pickAction(final String button, final String gesture, final TextView target) {
        String cur = Actions.get(this, button, gesture);
        int checked = -1;
        for (int i = 0; i < Actions.IDS.length; i++) if (Actions.IDS[i].equals(cur.startsWith("APP:") ? "APP" : cur)) checked = i;
        new AlertDialog.Builder(this)
                .setTitle(target.getContext() == null ? "Action" : "Action")
                .setSingleChoiceItems(Actions.LABELS, checked, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        d.dismiss();
                        String id = Actions.IDS[which];
                        if (id.equals("APP")) { pickApp(button, gesture, target); return; }
                        Actions.set(MainActivity.this, button, gesture, id);
                        target.setText(Actions.label(MainActivity.this, id));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void pickApp(final String button, final String gesture, final TextView target) {
        final PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> apps = new ArrayList<>(pm.queryIntentActivities(main, 0));
        Collections.sort(apps, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo x, ResolveInfo y) {
                return x.loadLabel(pm).toString().compareToIgnoreCase(y.loadLabel(pm).toString());
            }
        });
        String[] names = new String[apps.size()];
        for (int i = 0; i < names.length; i++) names[i] = apps.get(i).loadLabel(pm).toString();
        new AlertDialog.Builder(this)
                .setTitle("Application")
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        String act = "APP:" + apps.get(which).activityInfo.packageName;
                        Actions.set(MainActivity.this, button, gesture, act);
                        target.setText(Actions.label(MainActivity.this, act));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void updateStatus() {
        String me = new ComponentName(this, KeyService.class).flattenToString();
        String enabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        boolean on = false;
        if (enabled != null) {
            TextUtils.SimpleStringSplitter sp = new TextUtils.SimpleStringSplitter(':');
            sp.setString(enabled);
            for (String x : sp) {
                ComponentName cn = ComponentName.unflattenFromString(x);
                if (cn != null && cn.flattenToString().equals(me)) on = true;
            }
        }
        statusTitle.setText(on ? "Boutons actifs" : "Service désactivé");
        statusSummary.setText(on ? "Les 2 boutons programmables sont pris en charge."
                : "Toucher ici, puis activer « X4 Boutons » dans Accessibilité.");
    }
}

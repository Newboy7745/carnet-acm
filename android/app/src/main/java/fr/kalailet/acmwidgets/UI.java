package fr.kalailet.acmwidgets;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Petits constructeurs d'interface (pas de bibliothèque externe). */
final class UI {
    static final int NAVY = 0xFF16224A;
    static final int ACCENT = 0xFFC4461A;
    static final int MUTED = 0xFF4F5668;
    static final int BG = 0xFFF1F2EE;

    private UI() { }

    static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 20);
        l.setPadding(p, dp(c, 28), p, dp(c, 32));
        l.setBackgroundColor(BG);
        return l;
    }

    static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.WHITE);
        g.setCornerRadius(dp(c, 22));
        l.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 14);
        l.setLayoutParams(lp);
        return l;
    }

    static TextView title(Context c, String s) {
        TextView t = text(c, s, 26);
        t.setTypeface(Typeface.SERIF, Typeface.BOLD);
        t.setTextColor(0xFF141A2E);
        return t;
    }

    static TextView text(Context c, String s, float sp) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(0xFF141A2E);
        t.setLineSpacing(0, 1.15f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 6);
        t.setLayoutParams(lp);
        return t;
    }

    static TextView muted(Context c, String s) {
        TextView t = text(c, s, 14);
        t.setTextColor(MUTED);
        return t;
    }

    static Button button(Context c, String s, int color) {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, 16));
        b.setBackground(g);
        b.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 52));
        lp.topMargin = dp(c, 10);
        b.setLayoutParams(lp);
        return b;
    }

    static EditText input(Context c, String hint) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        return e;
    }
}

package fr.kalailet.acmwidgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;

/** Comportement commun : affichage immédiat depuis le dernier résumé, puis lecture de ntfy en arrière-plan s'il date de plus de 10 min. */
public abstract class BaseWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) Widgets.render(context, manager, id, getClass());
        if (Store.topic(context) == null || !Store.stale(context)) return;
        final Context app = context.getApplicationContext();
        final PendingResult pending = goAsync();
        new Thread(() -> {
            try {
                Store.fetch(app);
                Widgets.updateAll(app);
            } finally {
                pending.finish();
            }
        }).start();
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int id, Bundle options) {
        Widgets.render(context, manager, id, getClass());
    }

    @Override
    public void onDeleted(Context context, int[] ids) {
        for (int id : ids) Store.remove(context, id);
    }
}

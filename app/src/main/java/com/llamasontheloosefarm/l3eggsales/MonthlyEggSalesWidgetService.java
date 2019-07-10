package com.llamasontheloosefarm.l3eggsales;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.llamasontheloosefarm.l3eggsales.data.EggSale;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.Month;
import java.time.ZonedDateTime;
import java.util.ArrayList;

public class MonthlyEggSalesWidgetService extends IntentService {

    public static final String ACTION_UPDATE_MONTHLY_EGG_SALES_WIDGET = "com.llamasontheloosefarm.l3eggsales.action.update_sales";

    FirebaseFirestore db;
    ArrayList<EggSale> eggSaleArrayList;

    public MonthlyEggSalesWidgetService() {
        super("MonthlyEggSalesWidgetService");
    }

    public static void startActionUpdateMonthlySales(Context context) {
        Intent intent = new Intent(context, MonthlyEggSalesWidgetService.class);
        intent.setAction(ACTION_UPDATE_MONTHLY_EGG_SALES_WIDGET);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UPDATE_MONTHLY_EGG_SALES_WIDGET.equals(action)) {
                Log.d("The Widget Service", "Widget Stuff");

                final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
                final Context serviceContext = this;
                final int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, MonthlySalesWidgetProvider.class));

                db = FirebaseFirestore.getInstance();

                db.collection("sales")
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    eggSaleArrayList = new ArrayList<>();
                                    Double totalSalesForMonth = 0.00;


                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        Log.d("GETDATA: ", document.getId() + " => " + document.getData());

                                        EggSale sale = document.toObject(EggSale.class);
                                        eggSaleArrayList.add(sale);
                                    }

                                    if (eggSaleArrayList.size() > 0) {
                                        Instant dbInstant;
                                        Instant today = ZonedDateTime.now().plusDays(1).toInstant();
                                        Instant thrityDaysAgo = ZonedDateTime.now().minusDays(30).toInstant();

                                        for (int i = 0; i < eggSaleArrayList.size(); i++) {
                                            dbInstant = eggSaleArrayList.get(i).getSaleDate().toDate().toInstant();
                                            Boolean withinLast30Days = dbInstant.isAfter(thrityDaysAgo);
                                            Boolean isBeforeToday = dbInstant.isBefore(today);
                                            if(withinLast30Days && isBeforeToday) {
                                               totalSalesForMonth = totalSalesForMonth + eggSaleArrayList.get(i).getTotalPrice();
                                            }

                                        }

                                    }

                                    // Update all the widgets
                                    String totalSalesSring = NumberFormat.getCurrencyInstance().format(totalSalesForMonth);
                                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.appwidget_monthly_sales_number);
                                    MonthlySalesWidgetProvider.updateMonthlySalesWidget(serviceContext, appWidgetManager, totalSalesSring, appWidgetIds);



                                } else {
                                    Log.d("GETDATA: ", "Error getting data for the WIDGET");
                                }
                            }
                        });

            }

        }

    }
}

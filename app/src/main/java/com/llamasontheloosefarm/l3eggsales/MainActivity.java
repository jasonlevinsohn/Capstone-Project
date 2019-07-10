package com.llamasontheloosefarm.l3eggsales;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.llamasontheloosefarm.l3eggsales.data.EggSale;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private static String TAG = MainActivity.class.getSimpleName();

    Spinner timePeriodSpinner;
    TextView timePeriodText;
//    TableLayout tlRecentTransactionsTable;
    FirebaseFirestore db;
    ArrayList<EggSale> eggSalesList;
    Context mainActivityContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainActivityContext = this;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        // Setup Timber
        Timber.plant(new Timber.DebugTree());


        timePeriodSpinner = (Spinner) findViewById(R.id.spinner_time_period_selector);
        timePeriodText = (TextView) findViewById(R.id.tv_time_period_text);
//        tlRecentTransactionsTable = (TableLayout) findViewById(R.id.tl_recent_transactions_table);

        // Setup the array adapter to populate the timePeriodSelector
        ArrayAdapter<CharSequence> timePeriodAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.time_period_selections,
                android.R.layout.simple_spinner_item
                );
        timePeriodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timePeriodSpinner.setAdapter(timePeriodAdapter);

        timePeriodSpinner.setOnItemSelectedListener(this);


        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = MainActivity.this;
                Class dest = RecordSalesActivity.class;

                Intent addSalesRecordIntent = new Intent(context, dest);
                startActivity(addSalesRecordIntent);

            }
        });

        db = FirebaseFirestore.getInstance();
        Timber.d("Let's get the datas");


    }

    @Override
    protected void onResume() {
        super.onResume();

        Timber.d("********************");
        Timber.d("WE ARE ON RESUMING");
        Timber.d("********************");

        db.collection("sales")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {

                            MonthlyEggSalesWidgetService.startActionUpdateMonthlySales(mainActivityContext);

                            eggSalesList = new ArrayList<>();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d("GETDATA: ", document.getId() + " => " + document.getData());

                                EggSale sale = document.toObject(EggSale.class);
                                eggSalesList.add(sale);


                            }

                            if (eggSalesList.size() > 0) {
                                Collections.sort(eggSalesList);
                                populateRecentSalesTable();
                                setTotalSalesForTimePeriod();
                            }
                        } else {
                            Log.d("GETDATA: ", "Error getting data: ", task.getException());
                        }
                    }
                });

        Log.d("GETDATA: ", "DATAS GOTTEN");



    }

    private void setTotalSalesForTimePeriod() {
        if (eggSalesList != null && eggSalesList.size() > 0) {

            String timePeriodString = String.valueOf(timePeriodSpinner.getSelectedItem());
            Instant timePeriodInstant;
            Instant dbInstant;
            Double totalSalesForPeriod = 0.00;
            Instant today = ZonedDateTime.now().plusDays(1).toInstant();

            switch (timePeriodString) {
                case "Week":
                    timePeriodInstant = ZonedDateTime.now().minusDays(7).toInstant();
                    break;
                case "Month":
                    timePeriodInstant = ZonedDateTime.now().minusDays(30).toInstant();
                    break;
                case "Year to Date":
                    timePeriodInstant = ZonedDateTime.now().minusDays(365).toInstant();
                    break;
                default:
                    timePeriodInstant = ZonedDateTime.now().minusDays(7).toInstant();

            }

            for (int i = 0; i < eggSalesList.size(); i++) {
                dbInstant = eggSalesList.get(i).getSaleDate().toDate().toInstant();
                Boolean withinLast7Days = dbInstant.isAfter(timePeriodInstant);
                Boolean isBeforeToday = dbInstant.isBefore(today);
                if (withinLast7Days && isBeforeToday) {
                    totalSalesForPeriod = totalSalesForPeriod + eggSalesList.get(i).getTotalPrice();
                }
            }

            TextView tvSalesDollarValue = findViewById(R.id.tv_sales_dollar_value);
            tvSalesDollarValue.setText(NumberFormat.getCurrencyInstance().format(totalSalesForPeriod));
        }
    }

    private void populateRecentSalesTable() {

        // Create Table

        // Get Table Layout
        TableLayout recentTransTable = (TableLayout) findViewById(R.id.tl_recent_transactions_table);

        TextView dateColumn;
        TextView sellerColumn;
        TextView numberOfDozenColumn;
        TextView totalSaleColumn;

        if (eggSalesList.size() > 0) {
//            TableRow header = new TableRow(this);
//            TableRow.LayoutParams headerLp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
//            header.setLayoutParams(headerLp);

            // Refresh all the rows before adding them again from database.
//            int numberOfRows = recentTransTable.getChildCount();
//            for (int i = 1; i < numberOfRows; i++) {
//                TableRow rowToDelete = (TableRow) recentTransTable.getChildAt(i);
            View isThereAView = recentTransTable.getChildAt(1);
            do {
                if (isThereAView != null) {
                    recentTransTable.removeViewAt(1);
                }
                isThereAView = recentTransTable.getChildAt(1);
            } while (isThereAView != null);
//            }

            for (int i = 0; i < eggSalesList.size(); i++) {
                EggSale sale = eggSalesList.get(i);

                TableRow row = new TableRow(this);
                TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
                row.setLayoutParams(lp);
                dateColumn = new TextView(this);
                sellerColumn = new TextView(this);
                numberOfDozenColumn = new TextView(this);
                totalSaleColumn = new TextView(this);

                // Set Padding
                dateColumn.setPadding(10, 10, 10, 10);
                sellerColumn.setPadding(5, 10, 5, 10);
                numberOfDozenColumn.setPadding(5, 10, 5, 10);
                totalSaleColumn.setPadding(5, 10, 5, 10);

                // Set Gravity
                dateColumn.setGravity(Gravity.CENTER);
                sellerColumn.setGravity(Gravity.CENTER);
                numberOfDozenColumn.setGravity(Gravity.CENTER);
                totalSaleColumn.setGravity(Gravity.CENTER);

                // Set Background
                dateColumn.setBackgroundResource(R.drawable.cell_border);
                sellerColumn.setBackgroundResource(R.drawable.cell_border);
                numberOfDozenColumn.setBackgroundResource(R.drawable.cell_border);
                totalSaleColumn.setBackgroundResource(R.drawable.cell_border);


                Timestamp saleTimeStamp = sale.getSaleDate();

                if (saleTimeStamp != null) {
                    Date saleDate = saleTimeStamp.toDate();
                    SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
                    String dateString = format.format(saleDate);
                    dateColumn.setText(dateString);
                }

                sellerColumn.setText(sale.getCustomer());
                numberOfDozenColumn.setText(Integer.toString(sale.getNumberOfDozen()));


                // Price of Eggs Formatter
                NumberPicker.Formatter priceFormatter = new NumberPicker.Formatter() {
                    @Override
                    public String format(int value) {
                        return NumberFormat.getCurrencyInstance(Locale.US).format((long)value);
//            return NumberFormat.getCurrencyInstance(Locale.US).format((long)value).toString();
                    }
                };
//                totalSaleColumn.setText(Double.toString(sale.getTotalPrice()));
                totalSaleColumn.setText(NumberFormat.getCurrencyInstance().format(sale.getTotalPrice()));
//                tvEggTotal.setText(NumberFormat.getCurrencyInstance().format(eggTotal));
                row.addView(dateColumn);
                row.addView(sellerColumn);
                row.addView(numberOfDozenColumn);
                row.addView(totalSaleColumn);
                recentTransTable.addView(row, i + 1);


            }
        }
    }

    // ***************  TimePeriod Selector Section - BEGIN *****************
    // Setup Time Period Selector Selected Function
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {

        int item = adapterView.getSelectedItemPosition();

        Timber.d("Spinner: The item position is: %d", pos);
        Timber.d("Spinner: The item is: %d", item);

        switch(pos) {
            case 0:
                Timber.d("We have selected the Week");
//                Toast.makeText(this, "Week", Toast.LENGTH_LONG).show();
                timePeriodText.setText(String.valueOf(timePeriodSpinner.getSelectedItem()));
                setTotalSalesForTimePeriod();
                break;
            case 1:
                Timber.d("We have selected the Month");
//                Toast.makeText(this, "Month", Toast.LENGTH_LONG).show();
                timePeriodText.setText(String.valueOf(timePeriodSpinner.getSelectedItem()));
                setTotalSalesForTimePeriod();
                break;
            case 2:
                Timber.d("We have selected the Year");
//                Toast.makeText(this, "Year", Toast.LENGTH_LONG).show();
                timePeriodText.setText(String.valueOf(timePeriodSpinner.getSelectedItem()));
                setTotalSalesForTimePeriod();
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        Timber.d("Spinner: Nothing has been selected");
    }

    // ***************  TimePeriod Selector Section - END *****************

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

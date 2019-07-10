package com.llamasontheloosefarm.l3eggsales;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.llamasontheloosefarm.l3eggsales.data.EggSale;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import timber.log.Timber;


public class RecordSalesActivity extends FragmentActivity implements
        AdapterView.OnItemSelectedListener,
        DatePickerDialog.OnDateSetListener,
        View.OnClickListener {

    Spinner customerSpinner;
    String selectedCustomer;
    TextView tvDatePicker;
    Timestamp selectedDate;
    NumberPicker npNumberOfDozenPicker;
    NumberPicker npPricePicker;
    double eggTotal;
    TextView tvEggTotal;
    Button btnCancel;
    Button btnRecord;

    FirebaseFirestore db;

    // Firebase Analytics
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_sales);

        // Customer Spinner
        customerSpinner = findViewById(R.id.spinner_customer_selector);
        tvDatePicker = findViewById(R.id.tv_date_picker);

        ArrayAdapter<CharSequence> customerAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.customer_selections,
                android.R.layout.simple_spinner_item
                );

        customerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        customerSpinner.setAdapter(customerAdapter);
        customerSpinner.setOnItemSelectedListener(this);

        //  Number of Dozen Number Picker - BEGIN
        npNumberOfDozenPicker = findViewById(R.id.np_number_of_dozen_selection);

        npNumberOfDozenPicker.setMinValue(1);
        npNumberOfDozenPicker.setMaxValue(20);
        npNumberOfDozenPicker.setValue(1);

        npNumberOfDozenPicker.setOnValueChangedListener(onDozenOfEggsValueChangeListener);

        npNumberOfDozenPicker.setWrapSelectorWheel(false);

        //  Number of Dozen Number Picker - END

        // Price Number Picker - BEGIN
        npPricePicker = findViewById(R.id.np_price_per_dozen);

        // TODO: Currency Formatter is causing the selected price to be blank on initialization
//        npPricePicker.setFormatter(priceFormatter);
        npPricePicker.setMinValue(1);
        npPricePicker.setMaxValue(10);
        npPricePicker.setValue(5);

        npPricePicker.setOnValueChangedListener(onPriceValueChangeListener);


        npPricePicker.setWrapSelectorWheel(false);

        // Price Number Picker - END

        tvEggTotal = findViewById(R.id.tv_total_number);

        setTotal();

        // Cancel and Record Buttons
        btnCancel = findViewById(R.id.btn_cancel);
        btnRecord = findViewById(R.id.btn_record);
        btnCancel.setOnClickListener(this);
        btnRecord.setOnClickListener(this);

        // Cloud Firestore Initialization
        db = FirebaseFirestore.getInstance();

        // Get Firebase Analytics Instance
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Timber.d("The View:");
        int viewMe = view.getId();
        int parentView = parent.getId();

        switch(parent.getId()) {
            case R.id.spinner_customer_selector:
                Timber.d("Lets do some customer stuff");
                selectedCustomer = String.valueOf(customerSpinner.getSelectedItem());
                Timber.d("Selected Customer is: %s", selectedCustomer);
        }

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Timber.d("Spinner Nothing was selected");
    }

    public void showDatePickerDialog(View v) {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        Timber.d("THE DATE HAS BEEN SET");
        Timber.d("Year: %d", year);
        Timber.d("Month: %d", month);
        Timber.d("Day: %d", day);

        int convertMonth = month + 1;
        String stringDate = convertMonth + "/" + day + "/" + year;
//        selectedDate = stringDate;

        Calendar calendar = new GregorianCalendar(year, month, day);

        long epoch = calendar.getTimeInMillis();

        selectedDate = new Timestamp(new Date(epoch));

        tvDatePicker.setText(stringDate);

    }

    // Number of Dozen of Eggs Picker
    NumberPicker.OnValueChangeListener onDozenOfEggsValueChangeListener = new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            Toast.makeText(RecordSalesActivity.this, npNumberOfDozenPicker.getValue() + " Dozen Eggs Selected", Toast.LENGTH_LONG).show();
            setTotal();
        }
    };

    // Price of Eggs Picker
    NumberPicker.OnValueChangeListener onPriceValueChangeListener = new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            Toast.makeText(RecordSalesActivity.this, npPricePicker.getValue() + " Price Selected", Toast.LENGTH_LONG).show();
            setTotal();
        }
    };

    // Price of Eggs Formatter
    NumberPicker.Formatter priceFormatter = new NumberPicker.Formatter() {
        @Override
        public String format(int value) {
            return NumberFormat.getCurrencyInstance(Locale.US).format((long)value);
//            return NumberFormat.getCurrencyInstance(Locale.US).format((long)value).toString();
        }
    };

    private void setTotal() {
        double localEggTotal;
        int eggPrice = npPricePicker.getValue();
        int numberOfDozen = npNumberOfDozenPicker.getValue();

        localEggTotal = eggPrice * numberOfDozen;

        eggTotal = localEggTotal;

//        tvEggTotal.setText(eggTotal.toString());

        tvEggTotal.setText(NumberFormat.getCurrencyInstance().format(eggTotal));
    }

    @Override
    public void onClick(View v) {
        Timber.d("The view has been clicked");
        if (v != null) {
            int id = v.getId();

            if (id == R.id.btn_cancel) {
                finish();
            } else if (id == R.id.btn_record){

                // Validate the data before recording the sale.
                if (selectedCustomer == null) {
                    Toast.makeText(this, "Please select a customer", Toast.LENGTH_LONG).show();
                } else if (selectedDate == null) {
                    Toast.makeText(this, "Please select a date of sale", Toast.LENGTH_LONG).show();
                } else {
                    recordSale();
                }
            }

        }
    }

    private void recordSale() {


        // Create a new sale
        EggSale sale = new EggSale(selectedCustomer, selectedDate, npNumberOfDozenPicker.getValue(), npPricePicker.getValue(), eggTotal);

        // Create a new sale
//        Map<String, Object> sale = new HashMap<>();
//        sale.put("customer", selectedCustomer);
//        sale.put("date", selectedDate);
//        sale.put("numberOfDozen", npNumberOfDozenPicker.getValue());
//        sale.put("pricePerDozen", npPricePicker.getValue());
//        sale.put("totalPrice", NumberFormat.getCurrencyInstance().format(eggTotal));

        // Log Customer Selected in Firebase Analytics
        Bundle customerBundle = new Bundle();
        customerBundle.putString("sale_customer_selected", String.valueOf(customerSpinner.getSelectedItemId()));
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, customerBundle);


        Bundle dateSelectedBundle = new Bundle();
        dateSelectedBundle.putString(FirebaseAnalytics.Param.ITEM_ID, "1");
        dateSelectedBundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "dateSelected");
        dateSelectedBundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "datepicker");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, dateSelectedBundle);

        Bundle numberOfDozenBundle = new Bundle();
        numberOfDozenBundle.putString(FirebaseAnalytics.Param.ITEM_ID, "2");
        numberOfDozenBundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "numberOfDozenSelected");
        numberOfDozenBundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "1");
        numberOfDozenBundle.putString(FirebaseAnalytics.Param.CONTENT, String.valueOf(npNumberOfDozenPicker.getValue()));
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, numberOfDozenBundle);

        Bundle priceOfEggsBundle = new Bundle();
        priceOfEggsBundle.putString(FirebaseAnalytics.Param.ITEM_ID, "1");
        priceOfEggsBundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "eggPriceSelected");
        priceOfEggsBundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "5");
        priceOfEggsBundle.putString(FirebaseAnalytics.Param.CONTENT, String.valueOf(npPricePicker.getValue()));
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, priceOfEggsBundle);

        // Add a new document with a generated ID
        db.collection("sales")
                .add(sale)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Timber.d("Sale document added with ID: %s", documentReference.getId());
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Timber.d("Failed to add document to sales database - message: %s", e.getMessage());
                    }
                });
    }
}



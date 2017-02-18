package fr.ralala.worktime.activities;

import android.content.Context;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

import fr.ralala.worktime.MainApplication;
import fr.ralala.worktime.models.DayEntry;
import fr.ralala.worktime.models.DayType;
import fr.ralala.worktime.models.WorkTimeDay;
import fr.ralala.worktime.utils.AndroidHelper;


import fr.ralala.worktime.R;

/**
 *******************************************************************************
 * <p><b>Project WorkTime</b><br/>
 * Manage the activity containing the day entries used for the profiles and the classic insertions
 * </p>
 * @author Keidan
 *
 *******************************************************************************
 */
public class DayActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener{
  public static final String DAY_ACTIVITY_EXTRA_DATE = "fr.ralala.worktime.activities.DAY_ACTIVITY_EXTRA_DATE_date";
  public static final String DAY_ACTIVITY_EXTRA_PROFILE = "fr.ralala.worktime.activities.DAY_ACTIVITY_EXTRA_DATE_profile";
  private Spinner spProfile = null;
  private Spinner spType = null;
  private TextView tvStartMorning = null;
  private TextView tvEndMorning = null;
  private TextView tvStartAfternoon = null;
  private TextView tvEndAfternoon = null;
  private EditText etAmount = null;
  private EditText etName = null;
  private DayEntry de = null;
  private boolean displayProfile = false;
  private ArrayAdapter<String> spProfilesAdapter = null;
  private FloatingActionButton fab = null;

  private WorkTimeDay wtdStartMorning = null;
  private WorkTimeDay wtdEndMorning = null;
  private WorkTimeDay wtdStartAfternoon = null;
  private WorkTimeDay wtdEndAfternoon = null;
  private MainApplication app = null;


  public static void startActivity(final Context ctx, final String date, final boolean profile) {
    Intent intent = new Intent(ctx, DayActivity.class);
    intent.putExtra(DAY_ACTIVITY_EXTRA_DATE, date);
    intent.putExtra(DAY_ACTIVITY_EXTRA_PROFILE, profile);
    ctx.startActivity(intent);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    app = MainApplication.getApp(this);
    setContentView(R.layout.activity_day);
    android.support.v7.app.ActionBar actionBar = getDelegate().getSupportActionBar();
    actionBar.setDisplayShowHomeEnabled(true);
    actionBar.setDisplayHomeAsUpEnabled(true);
    String date =  null;
    if(getIntent().getExtras() != null) {
      Bundle extras = getIntent().getExtras();
      date = extras.getString(DAY_ACTIVITY_EXTRA_DATE);
      displayProfile = extras.getBoolean(DAY_ACTIVITY_EXTRA_PROFILE);
      if(date.equals("null")) date = "";
    }
    List<DayEntry> days = displayProfile ? app.getDaysFactory().list() : app.getProfilesFactory().list();
    for(DayEntry de : days) {
      if((displayProfile && de.getDay().dateString().equals(date)) || (!displayProfile && de.getName().equals(date))) {
        Log.e(getClass().getSimpleName(), "found de:"+de.getDay().dateString());
        this.de = de;
        break;
      }
    }
    if(de == null) {
      de = new DayEntry(WorkTimeDay.now(), DayType.ERROR);
      if(!date.isEmpty() && date.indexOf("/") != -1)
        de.setDay(date);
    }
    fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(this);
    refreshStartEndPause(de);

    if(displayProfile)
      setTitle(date);
    TextView tvProfile = (TextView)findViewById(R.id.tvProfile);
    spProfile = (Spinner)findViewById(R.id.spProfile);
    spType = (Spinner)findViewById(R.id.spType);
    tvStartMorning = (TextView)findViewById(R.id.tvStartMorning);
    tvEndMorning = (TextView)findViewById(R.id.tvEndMorning);
    tvStartAfternoon = (TextView)findViewById(R.id.tvStartAfternoon);
    tvEndAfternoon = (TextView)findViewById(R.id.tvEndAfternoon);
    etAmount = (EditText)findViewById(R.id.etAmount);
    TextView tvName = (TextView)findViewById(R.id.tvName);
    etName = (EditText)findViewById(R.id.etName);

    boolean hw = app.isHideWage();
    etAmount.setVisibility(hw ? View.INVISIBLE : View.VISIBLE);
    findViewById(R.id.tvLblAmount).setVisibility(hw ? View.INVISIBLE : View.VISIBLE);
    /* add click listener for the time picker */
    tvStartMorning.setOnClickListener(this);
    tvEndMorning.setOnClickListener(this);
    tvStartAfternoon.setOnClickListener(this);
    tvEndAfternoon.setOnClickListener(this);
    /* manage view for the call from the profile view */
    if(displayProfile) {
      int v = View.VISIBLE;
      spProfile.setVisibility(v);
      tvProfile.setVisibility(v);
      v = View.GONE;
      tvName.setVisibility(v);
      etName.setVisibility(v);
      if(app.getProfilesFactory().list().isEmpty()) {
        spProfile.setVisibility(View.GONE);
        tvProfile.setVisibility(View.GONE);
      }
    } else {
      int v = View.GONE;
      spProfile.setVisibility(v);
      tvProfile.setVisibility(v);
      v = View.VISIBLE;
      tvName.setVisibility(v);
      etName.setVisibility(v);
      etName.setText(de.getName());
    }
    /* build type spinner */
    final ArrayAdapter<String> spTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
    spTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spType.setAdapter(spTypeAdapter);
    spTypeAdapter.add(DayType.getText(this, DayType.AT_WORK));
    spTypeAdapter.add(DayType.getText(this, DayType.HOLIDAY));
    spTypeAdapter.add(DayType.getText(this, DayType.PUBLIC_HOLIDAY));
    spTypeAdapter.add(DayType.getText(this, DayType.SICKNESS));
    spTypeAdapter.add(DayType.getText(this, DayType.UNPAID));
    spType.setSelection(de.getType() == DayType.ERROR ? 0 : de.getType().value());

    if(displayProfile) {
    /* build profiles spinner */
      spProfilesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
      spProfilesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      spProfile.setAdapter(spProfilesAdapter);
      spProfilesAdapter.add("");
      for (DayEntry profile : app.getProfilesFactory().list())
        spProfilesAdapter.add(profile.getName());
      spProfile.setOnItemSelectedListener(this);
    }

    AndroidHelper.initTimeTextView(de.getStartMorning(), tvStartMorning);
    AndroidHelper.initTimeTextView(de.getEndMorning(), tvEndMorning);
    AndroidHelper.initTimeTextView(de.getStartAfternoon(), tvStartAfternoon);
    AndroidHelper.initTimeTextView(de.getEndAfternoon(), tvEndAfternoon);

    if(!app.isHideWage()) {
      etAmount.setTypeface(tvStartAfternoon.getTypeface());
      etAmount.setTextSize(14);
      etAmount.setTextColor(tvStartAfternoon.getTextColors());
      etAmount.setText(String.format(Locale.US, "%.02f",
        de.getAmountByHour() != 0 ? de.getAmountByHour() : app.getAmountByHour()).replaceAll(",", "."));
    }
  }

  @Override
  public boolean onCreateOptionsMenu(final Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.activity_day, menu);

    MenuItem action_cancel = menu.findItem(R.id.action_cancel);
    action_cancel.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    action_cancel.setTitle(!displayProfile ? R.string.cancel : R.string.clear);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_cancel:
        if(displayProfile) {
          AndroidHelper.initTimeTextView(wtdStartMorning = new WorkTimeDay(), tvStartMorning);
          AndroidHelper.initTimeTextView(wtdEndMorning = new WorkTimeDay(), tvEndMorning);
          AndroidHelper.initTimeTextView(wtdStartAfternoon = new WorkTimeDay(), tvStartAfternoon);
          AndroidHelper.initTimeTextView(wtdEndAfternoon = new WorkTimeDay(), tvEndAfternoon);
          etAmount.setText(getString(R.string.zero));
          spType.setSelection(DayType.AT_WORK.value());
          etName.setText("");
          setTitle(de.getDay().dateString());
          spProfile.setSelection(0);
        } else
          finish();
        return true;
    }
    return false;
  }

  public void onClick(final View v) {
    if(v.equals(fab)) {
      DayEntry newEntry = new DayEntry(de.getDay().toCalendar(), DayType.compute(this, spType.getSelectedItem().toString()));
      String s = etAmount.getText().toString().trim();
      if (s.equals(getString(R.string.zero))) s = "";
      newEntry.setAmountByHour(s.isEmpty() ? app.getAmountByHour() : Double.parseDouble(s));
      newEntry.setName(etName.getText().toString());
      newEntry.setEndMorning(tvEndMorning.getText().toString());
      newEntry.setStartMorning(tvStartMorning.getText().toString());
      newEntry.setEndAfternoon(tvEndAfternoon.getText().toString());
      newEntry.setStartAfternoon(tvStartAfternoon.getText().toString());
      if (!displayProfile && etName.getText().toString().isEmpty()) {
        AndroidHelper.showAlertDialog(this, R.string.error, R.string.error_no_name);
        return;
      } else if (!displayProfile && newEntry.getStartMorning().getHours() == 0) {
        AndroidHelper.showAlertDialog(this, R.string.error, R.string.error_invalid_start);
        return;
      } else if (!displayProfile && newEntry.getEndMorning().getHours() == 0) {
        AndroidHelper.showAlertDialog(this, R.string.error, R.string.error_invalid_end);
        return;
      } else if (!displayProfile && newEntry.getStartAfternoon().getHours() == 0) {
        AndroidHelper.showAlertDialog(this, R.string.error, R.string.error_invalid_start);
        return;
      } else if (!displayProfile && newEntry.getEndAfternoon().getHours() == 0) {
        AndroidHelper.showAlertDialog(this, R.string.error, R.string.error_invalid_end);
        return;
      } else if (!displayProfile && tvStartMorning.getText().toString().equals(tvEndMorning.getText().toString())) {
        AndroidHelper.showAlertDialog(this, R.string.error, R.string.error_invalid_start_end_morning);
        return;
      } else if (!displayProfile && tvStartAfternoon.getText().toString().equals(tvEndAfternoon.getText().toString())) {
        AndroidHelper.showAlertDialog(this, R.string.error, R.string.error_invalid_start_end_afternoon);
        return;
      }
      if(displayProfile) {
        Log.e(getClass().getSimpleName(), "de.match(newEntry):"+de.match(newEntry));
        if (!de.match(newEntry))
          app.getDaysFactory().remove(de);
        Log.e(getClass().getSimpleName(), "newEntry.getStartMorning().isValidTime():"+newEntry.getStartMorning().isValidTime());
        Log.e(getClass().getSimpleName(), "newEntry.getEndAfternoon().isValidTime():"+newEntry.getEndAfternoon().isValidTime());
        if (newEntry.getStartMorning().isValidTime() && newEntry.getEndAfternoon().isValidTime())
          app.getDaysFactory().add(newEntry);
      } else {
        if(de.getName().isEmpty() || !de.match(newEntry))
          app.getProfilesFactory().remove(de);
        if(newEntry.getStartMorning().isValidTime() && newEntry.getEndAfternoon().isValidTime()) {
          app.getProfilesFactory().add(newEntry);
        }
      }
      finish();
    } else if(v.equals(tvStartMorning))
      AndroidHelper.openTimePicker(this, wtdStartMorning, tvStartMorning);
    else if(v.equals(tvEndMorning))
      AndroidHelper.openTimePicker(this, wtdEndMorning, tvEndMorning);
    else if(v.equals(tvStartAfternoon))
      AndroidHelper.openTimePicker(this, wtdStartAfternoon, tvStartAfternoon);
    else if(v.equals(tvEndAfternoon))
      AndroidHelper.openTimePicker(this, wtdEndAfternoon, tvEndAfternoon);
  }

  @Override
  public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
    if(spProfilesAdapter != null) {
      String name = spProfilesAdapter.getItem(i);
      if(name != null && !name.isEmpty()) {
        DayEntry de = app.getProfilesFactory().getByName(name);
        if (de != null) {
          AndroidHelper.initTimeTextView(de.getStartMorning(), tvStartMorning);
          AndroidHelper.initTimeTextView(de.getEndMorning(), tvEndMorning);
          AndroidHelper.initTimeTextView(de.getStartAfternoon(), tvStartAfternoon);
          AndroidHelper.initTimeTextView(de.getEndAfternoon(), tvEndAfternoon);
          etAmount.setText(String.format(Locale.US, "%.02f", de.getAmountByHour()).replaceAll(",", "."));
          spType.setSelection(de.getType() == DayType.ERROR ? 0 : de.getType().value());
          etName.setText(de.getName());
          refreshStartEndPause(de);
        }
      }
    }
  }


  private void refreshStartEndPause(DayEntry de) {
    wtdStartMorning = de.getStartMorning().clone();
    wtdEndMorning = de.getEndMorning().clone();
    wtdStartAfternoon = de.getStartAfternoon().clone();
    wtdEndAfternoon = de.getEndAfternoon().clone();
  }

  @Override
  public void onNothingSelected(AdapterView<?> adapterView) {

  }
}

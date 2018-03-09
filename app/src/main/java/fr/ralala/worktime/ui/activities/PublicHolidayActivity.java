package fr.ralala.worktime.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;

import java.util.List;

import fr.ralala.worktime.MainApplication;
import fr.ralala.worktime.R;
import fr.ralala.worktime.models.DayEntry;
import fr.ralala.worktime.models.DayType;
import fr.ralala.worktime.models.WorkTimeDay;
import fr.ralala.worktime.ui.utils.UIHelper;

/**
 *******************************************************************************
 * <p><b>Project WorkTime</b><br/>
 * Manage the activity containing the public holiday entries used for the profiles and the classic insertions
 * </p>
 * @author Keidan
 *
 *******************************************************************************
 */
public class PublicHolidayActivity extends AppCompatActivity {
  public static final int REQUEST_START_ACTIVITY = 100;
  public static final String PUBLIC_HOLIDAY_ACTIVITY_EXTRA_NAME = "PUBLIC_HOLIDAY_ACTIVITY_EXTRA_NAME_name";
  private MainApplication mApp = null;
  private DayEntry mDe = null;
  private EditText mTname = null;
  private DatePicker mTdate = null;
  private CheckBox mCkRecurrence = null;

  /**
   * Starts an activity.
   * @param fragment The Android fragment.
   * @param name The extra name.
   */
  public static void startActivity(final Fragment fragment, final String name) {
    Intent intent = new Intent(fragment.getContext(), PublicHolidayActivity.class);
    intent.putExtra(PUBLIC_HOLIDAY_ACTIVITY_EXTRA_NAME, name);
    fragment.startActivityForResult(intent, REQUEST_START_ACTIVITY);
  }

  /**
   * Called to handle the click on the back button.
   */
  @Override
  public void onBackPressed() {
    setResult(RESULT_CANCELED);
    super.onBackPressed();
    UIHelper.closeAnimation(this);
  }

  /**
   * Called when the activity is created.
   * @param savedInstanceState The saved instance state.
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    UIHelper.openAnimation(this);
    super.onCreate(savedInstanceState);
    mApp = MainApplication.getApp(this);
    setContentView(R.layout.activity_public_holiday);
    android.support.v7.app.ActionBar actionBar = getDelegate().getSupportActionBar();
    if(actionBar != null) {
      actionBar.setDisplayShowHomeEnabled(true);
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
    String extra =  "";
    if(getIntent().getExtras() != null) {
      Bundle extras = getIntent().getExtras();
      extra = extras.getString(PUBLIC_HOLIDAY_ACTIVITY_EXTRA_NAME);
      if(extra == null || extra.equals("null")) extra = "";
    }
    if(!extra.isEmpty()) {
      int idx = extra.lastIndexOf('|');
      String name = extra.substring(0, idx);
      String date = extra.substring(idx + 1);
      List<DayEntry> days = mApp.getPublicHolidaysFactory().list();
      for (DayEntry de : days) {
        if (de.getName().equals(name) && de.getDay().dateString().equals(date)) {
          this.mDe = de;
          break;
        }
      }
    }
    if(mDe == null) {
      mDe = new DayEntry(this, WorkTimeDay.now(), DayType.ERROR, DayType.ERROR);
      mDe.setName("");
    }
    mCkRecurrence = findViewById(R.id.ckRecurrence);
    mTname = findViewById(R.id.etName);
    mTdate = findViewById(R.id.dpDate);
    if(mDe != null) {
      mTname.setText(mDe.getName());
      // set current date into datepicker
      mTdate.init(mDe.getDay().getYear(), mDe.getDay().getMonth() - 1, mDe.getDay().getDay(), null);
      mCkRecurrence.setChecked(mDe.isRecurrence());
    } else {
      mTname.setText("");
      WorkTimeDay now = WorkTimeDay.now();
      // set current date into datepicker
      mTdate.init(now.getYear(), now.getMonth() - 1, now.getDay(), null);
      mCkRecurrence.setChecked(false);
    }
  }

  /**
   * Called when the options menu is clicked.
   * @param menu The selected menu.
   * @return boolean
   */
  @Override
  public boolean onCreateOptionsMenu(final Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.activity_day, menu);
    return true;
  }

  /**
   * Called when the options item is clicked (home and cancel).
   * @param item The selected menu.
   * @return boolean
   */
  @Override
  public boolean onOptionsItemSelected(final MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        return true;
      case R.id.action_done: {
        final String name = mTname.getText().toString().trim();
        if(name.isEmpty()) {
          UIHelper.shakeError(mTname, getString(R.string.error_no_name));
          return true;
        }
        WorkTimeDay wtd = new WorkTimeDay();
        wtd.setDay(mTdate.getDayOfMonth());
        wtd.setMonth(mTdate.getMonth() + 1);
        wtd.setYear(mTdate.getYear());
        if(mDe != null) mApp.getPublicHolidaysFactory().remove(mDe); /* remove old entry */
        DayEntry de = new DayEntry(this, wtd, DayType.PUBLIC_HOLIDAY, DayType.PUBLIC_HOLIDAY);
        if(mApp.getPublicHolidaysFactory().testValidity(de)) {
          de.setName(name);
          de.setRecurrence(mCkRecurrence.isChecked());
          mApp.getPublicHolidaysFactory().add(de);
          setResult(RESULT_OK);
          finish();
          UIHelper.closeAnimation(this);
        } else {
          UIHelper.shakeError(mTname, getString(R.string.error_duplicate_public_holiday));
        }
        return true;
      }
      case R.id.action_cancel:
        onBackPressed();
        return true;
    }
    return false;
  }

}

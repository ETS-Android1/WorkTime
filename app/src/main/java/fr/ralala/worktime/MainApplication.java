package fr.ralala.worktime;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import fr.ralala.worktime.sql.SqlHelper;
import fr.ralala.worktime.ui.activities.DayActivity;
import fr.ralala.worktime.ui.activities.MainActivity;
import fr.ralala.worktime.dropbox.DropboxImportExport;
import fr.ralala.worktime.factories.DaysFactory;
import fr.ralala.worktime.factories.ProfilesFactory;
import fr.ralala.worktime.factories.PublicHolidaysFactory;
import fr.ralala.worktime.models.DayEntry;
import fr.ralala.worktime.models.WorkTimeDay;
import fr.ralala.worktime.ui.activities.settings.SettingsActivity;
import fr.ralala.worktime.ui.activities.settings.SettingsDatabaseActivity;
import fr.ralala.worktime.ui.activities.settings.SettingsDisplayActivity;
import fr.ralala.worktime.ui.activities.settings.SettingsExcelExportActivity;
import fr.ralala.worktime.ui.activities.settings.SettingsLearningActivity;
import fr.ralala.worktime.ui.quickaccess.QuickAccessNotification;
import fr.ralala.worktime.sql.SqlFactory;
import fr.ralala.worktime.utils.MyActivityLifecycleCallbacks;
import fr.ralala.worktime.ui.utils.UIHelper;

/**
 *******************************************************************************
 * <p><b>Project WorkTime</b><br/>
 * Application context
 * </p>
 * @author Keidan
 *
 *******************************************************************************
 */
public class MainApplication extends Application  {
  private static final int NFY_QUICK_ACCESS = 1;
  private PublicHolidaysFactory mPublicHolidaysFactory;
  private ProfilesFactory mProfilesFactory;
  private DaysFactory mDaysFactory;
  private SqlFactory mSql = null;
  private Calendar mCurrentDate = null;
  private boolean mQuickAccessPause = true;
  private QuickAccessNotification mQuickAccessNotification = null;
  private int mLastFirstVisibleItem = 0;
  private DropboxImportExport mDropboxImportExport = null;
  private WorkTimeDay mLastQuickAccessBreak = null;
  private long mLastWidgetOpen = 0L;
  private MyActivityLifecycleCallbacks mLifeCycle;
  private static int mResumedCounter = 0;
  private String mDbMD5 = null;
  public static MainApplication mInstace;

  public MainApplication() {
  }

  /**
   * Called by Android to create the application context.
   */
  @Override
  public void onCreate() {
    super.onCreate();
    mInstace = this;
    mPublicHolidaysFactory = new PublicHolidaysFactory();
    mProfilesFactory = new ProfilesFactory();
    mDaysFactory = new DaysFactory();
    mQuickAccessNotification = new QuickAccessNotification(this, NFY_QUICK_ACCESS);
    mDropboxImportExport = new DropboxImportExport();
    // Register for activity state changes notifications
    Class<?>[] classes = new Class<?>[] { MainActivity.class, DayActivity.class };
    registerActivityLifecycleCallbacks(mLifeCycle = new MyActivityLifecycleCallbacks(Arrays.asList(classes)));
    reloadDatabaseMD5();
  }

  /**
   * Returns the onResumed method call counter.
   * @return int
   */
  public int getResumedCounter() {
    return mResumedCounter;
  }

  /**
   * Increments the onResumed method call counter.
   */
  public void incResumedCounter() {
    mResumedCounter++;
  }

  /**
   * Returns the application ActivityLifecycleCallbacks
   * @return MyActivityLifecycleCallbacks
   */
  public MyActivityLifecycleCallbacks getLifeCycle() {
    return mLifeCycle;
  }


  /**
   * Returns the current application instance.
   * @return MainApplication
   */
  public static MainApplication getInstance() {
    return mInstace;
  }

  /**
   * Returns the current date time.
   * @return Calendar
   */
  public Calendar getCurrentDate() {
    if(mCurrentDate == null) {
      mCurrentDate = Calendar.getInstance();
      mCurrentDate.setTimeZone(TimeZone.getTimeZone("GMT"));
      mCurrentDate.setTime(new Date());
    }
    return mCurrentDate;
  }

  /**
   * Returns the last quick access break time.
   * @return WorkTimeDay
   */
  public WorkTimeDay getLastQuickAccessBreak() {
    return mLastQuickAccessBreak;
  }

  /**
   * Sets the last quick access break time.
   * @param lastQuickAccessBreak The new break time.
   */
  public void setLastQuickAccessBreak(WorkTimeDay lastQuickAccessBreak) {
    mLastQuickAccessBreak = lastQuickAccessBreak;
  }

  /**
   * Returns the instance of quick access notification.
   * @return QuickAccessNotification
   */
  public QuickAccessNotification getQuickAccessNotification() {
    return mQuickAccessNotification;
  }

  /**
   * Returns the instance of the profiles factory.
   * @return ProfilesFactory
   */
  public ProfilesFactory getProfilesFactory() {
    return mProfilesFactory;
  }

  /**
   * Returns the instance of the days factory.
   * @return DaysFactory
   */
  public DaysFactory getDaysFactory() {
    return mDaysFactory;
  }

  /**
   * Returns the instance of the public holidays factory.
   * @return PublicHolidaysFactory
   */
  public PublicHolidaysFactory getPublicHolidaysFactory() {
    return mPublicHolidaysFactory;
  }

  /**
   * Returns the instance of the SQLite object.
   * @return SqlFactory
   */
  public SqlFactory getSql() {
    return mSql;
  }

  /**
   * Returns the estimated hours in accordance to the input list.
   * @param wDays The input list.
   * @return WorkTimeDay
   */
  public WorkTimeDay getEstimatedHours(List<DayEntry> wDays) {
    WorkTimeDay w = new WorkTimeDay();
    for(int i = 0; i < wDays.size(); ++i)
      w.addTime(wDays.get(i).getLegalWorktime());
    return w;
  }

  /**
   * Sets if the quick access is paused.
   * @param quickAccessPause The new value.
   */
  public void setQuickAccessPause(boolean quickAccessPause) {
    mQuickAccessPause = quickAccessPause;
  }

  /**
   * Tests if the quick access is paused.
   * @return boolean
   */
  public boolean isQuickAccessPause() {
    return mQuickAccessPause;
  }

  /**
   * Sets the last visible day (first of the display).
   * @param lastFirstVisibleItem The new index.
   */
  public void setLastFirstVisibleItem(int lastFirstVisibleItem) {
    mLastFirstVisibleItem = lastFirstVisibleItem;
  }

  /**
   * Returns the index of the last visible day (first of the display).
   * @return int
   */
  public int getLastFirstVisibleItem() {
    return mLastFirstVisibleItem;
  }

  /**
   * Returns the DropboxImportExport object.
   * @return DropboxImportExport
   */
  public DropboxImportExport getDropboxImportExport() {
    return mDropboxImportExport;
  }

  /* ----------------------------------
   * Global configuration
   * ----------------------------------
   */

  /**
   * Returns the periodicity with which the application must backup the databases (Only with dropbox and whether automatic export is enabled).
   * @return int
   */
  public int getExportAutoSavePeriodicity() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    return Integer.parseInt(prefs.getString(SettingsDatabaseActivity.PREFS_KEY_IMPORT_EXPORT_AUTO_SAVE_PERIODICITY, SettingsDatabaseActivity.PREFS_DEFVAL_IMPORT_EXPORT_AUTO_SAVE_PERIODICITY));
  }

  /**
   * Tests whether the application should enable automatic saving.
   * @return boolean
   */
  public boolean isExportAutoSave() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    return prefs.getBoolean(SettingsDatabaseActivity.PREFS_KEY_IMPORT_EXPORT_AUTO_SAVE, SettingsDatabaseActivity.PREFS_DEFVAL_IMPORT_EXPORT_AUTO_SAVE.equals("true"));
  }

  /**
   * Returns the default home that the application should display.
   * @return int
   */
  public int getDefaultHome() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    return Integer.parseInt(prefs.getString(SettingsDisplayActivity.PREFS_KEY_DEFAULT_HOME, SettingsDisplayActivity.PREFS_DEFVAL_DEFAULT_HOME));
  }

  /**
   * Returns the depth of weight used by learning profiles.
   * @return int
   */
  public int getProfilesWeightDepth() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    return Integer.parseInt(prefs.getString(SettingsLearningActivity.PREFS_KEY_PROFILES_WEIGHT_DEPTH, SettingsLearningActivity.PREFS_DEFVAL_PROFILES_WEIGHT_DEPTH));
  }

  /**
   * Returns the row height of the days view (WorkTimeFragment).
   * @return int
   */
  public int getDayRowsHeight() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    return Integer.parseInt(prefs.getString(SettingsDisplayActivity.PREFS_KEY_DAY_ROWS_HEIGHT, SettingsDisplayActivity.PREFS_DEFVAL_DAY_ROWS_HEIGHT));
  }

  /**
   * Returns the legal work time value by day.
   * @return WorkTimeDay
   */
  public WorkTimeDay getLegalWorkTimeByDay() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    String [] split = prefs.getString(SettingsActivity.PREFS_KEY_WORKTIME_BY_DAY, SettingsActivity.PREFS_DEFVAL_WORKTIME_BY_DAY).split(":");
    return new WorkTimeDay(0, 0, 0, Integer.parseInt(split[0]), Integer.parseInt(split[1]));
  }

  /**
   * Returns the default amount by hour.
   * @return double
   */
  public double getAmountByHour() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    return Double.parseDouble(prefs.getString(SettingsActivity.PREFS_KEY_AMOUNT_BY_HOUR, SettingsActivity.PREFS_DEFVAL_AMOUNT_BY_HOUR).replaceAll(",", "."));
  }

  /**
   * Returns the currency that must be used by the application.
   * @return String
   */
  public String getCurrency() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    return prefs.getString(SettingsActivity.PREFS_KEY_CURRENCY, SettingsActivity.PREFS_DEFVAL_CURRENCY);
  }

  /**
   * Returns the email address that the application must use for the export function.
   * @return String
   */
  public String getEMail() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    return prefs.getString(SettingsExcelExportActivity.PREFS_KEY_EMAIL, SettingsExcelExportActivity.PREFS_DEFVAL_EMAIL);
  }

  /**
   * Tests whether the exported file should be sent by e-mail.
   * @return boolean
   */
  public boolean isExportMailEnabled() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    return prefs.getBoolean(SettingsExcelExportActivity.PREFS_KEY_EMAIL_ENABLE, SettingsExcelExportActivity.PREFS_DEFVAL_EMAIL_ENABLE.equals("true"));
  }

  /**
   * Tests if the application (DayActivity) should display the wage part.
   * @return boolean
   */
  public boolean isHideWage() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    return prefs.getBoolean(SettingsDisplayActivity.PREFS_KEY_HIDE_WAGE, SettingsDisplayActivity.PREFS_DEFVAL_HIDE_WAGE.equals("true"));
  }

  /**
   * Tests whether the wage section should be displayed when exporting work time.
   * @return boolean
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean isExportHideWage() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    return prefs.getBoolean(SettingsExcelExportActivity.PREFS_KEY_EXPORT_HIDE_WAGE, SettingsExcelExportActivity.PREFS_DEFVAL_EXPORT_HIDE_WAGE.equals("true"));
  }

  /**
   * Tests whether the application (WorkTimeFragment) should scroll to the current day.
   * @return boolean
   */
  public boolean isScrollToCurrentDay() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    return prefs.getBoolean(SettingsDisplayActivity.PREFS_KEY_SCROLL_TO_CURRENT_DAY, SettingsDisplayActivity.PREFS_DEFVAL_SCROLL_TO_CURRENT_DAY.equals("true"));
  }

  /**
   * Tests whether the exit button must be displayed or hidden.
   * @return boolean
   */
  public boolean isHideExitButton() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    return prefs.getBoolean(SettingsDisplayActivity.PREFS_KEY_HIDE_EXIT_BUTTON, SettingsDisplayActivity.PREFS_DEFVAL_HIDE_EXIT_BUTTON.equals("true"));
  }

  /* ----------------------------------
   * Database management
   * ----------------------------------
   */

  /**
   * Detects a change in the SQLite tables.
   * @return boolean
   */
  public boolean isTablesChanges() {
    String md5 = SqlHelper.getDatabaseMD5(this);
    Log.i(getClass().getSimpleName(), "isTablesChanges -> Database MD5: " + md5);
    return md5 != null && mDbMD5 != null && !mDbMD5.equals(md5);
  }

  /**
   * Reloads the database MD5.
   */
  public void reloadDatabaseMD5() {
    if(isExportAutoSave()) {
      mDbMD5 = SqlHelper.getDatabaseMD5(this);
      Log.i(getClass().getSimpleName(), "reloadDatabaseMD5 -> Database MD5: " + mDbMD5);
    }
  }

  /**
   * Opens the SQLite connection and loads the application databases.
   * @param c The Android context.
   * @return false on error.
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean openSql(final Context c) {
    try {
      mSql = new SqlFactory(c);
      mSql.open();
      mPublicHolidaysFactory.setSqlFactory(mSql);
      mDaysFactory.setSqlFactory(mSql);
      mProfilesFactory.setSqlFactory(mSql);
      return true;
    } catch (final Exception e) {
      Log.e(getClass().getSimpleName(), "Error: " + e.getMessage(), e);
      UIHelper.showAlertDialog(this, R.string.error, getString(R.string.error) + ": " + e.getMessage());
    }
    return false;
  }
  /* ----------------------------------
   * Widget management 
   * ----------------------------------
   */

  /**
   * Returns the time, in milliseconds, at which the widget opened the day activity.
   * @return long.
   */
  public long getLastWidgetOpen() {
    return mLastWidgetOpen;
  }

  /**
   * Sets the time, in milliseconds, at which the widget opened the day activity.
   * @param lastWidgetOpen The time in milliseconds.
   */
  public void setLastWidgetOpen(long lastWidgetOpen) {
    mLastWidgetOpen = lastWidgetOpen;
  }

}

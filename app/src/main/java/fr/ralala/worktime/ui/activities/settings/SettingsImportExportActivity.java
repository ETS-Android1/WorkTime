package fr.ralala.worktime.ui.activities.settings;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.MenuItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ralala.worktime.MainApplication;
import fr.ralala.worktime.R;
import fr.ralala.worktime.dropbox.DropboxImportExport;
import fr.ralala.worktime.services.DropboxAutoExportService;
import fr.ralala.worktime.sql.SqlHelper;
import fr.ralala.worktime.ui.activities.FileChooserActivity;
import fr.ralala.worktime.ui.utils.UIHelper;


/**
 *******************************************************************************
 * <p><b>Project WorkTime</b><br/>
 * Management of the db import/export
 * </p>
 * @author Keidan
 *
 *******************************************************************************
 */
public class SettingsImportExportActivity extends PreferenceActivity implements Preference.OnPreferenceClickListener {
  public static final String PREFS_KEY_EXPORT_TO_DEVICE = "prefExportToDevice";
  public static final String PREFS_KEY_IMPORT_FROM_DEVICE = "prefImportFromDevice";
  public static final String PREFS_KEY_EXPORT_TO_DROPBOX = "prefExportToDropbox";
  public static final String PREFS_KEY_IMPORT_FROM_DROPBOX = "prefImportFromDropbox";

  private MyPreferenceFragment mPrefFrag = null;
  private MainApplication mApp = null;

  /**
   * Called when the activity is created.
   * @param savedInstanceState The saved instance state.
   */
  @Override
  public void onCreate(final Bundle savedInstanceState) {
    UIHelper.openAnimation(this);
    super.onCreate(savedInstanceState);
    mApp = MainApplication.getApp(this);
    mPrefFrag = new MyPreferenceFragment();
    getFragmentManager().beginTransaction()
      .replace(android.R.id.content, mPrefFrag).commit();
    getFragmentManager().executePendingTransactions();
    android.support.v7.app.ActionBar actionBar = AppCompatDelegate.create(this, null).getSupportActionBar();
    if(actionBar != null) {
      actionBar.setDisplayShowHomeEnabled(true);
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
    mPrefFrag.findPreference(PREFS_KEY_EXPORT_TO_DEVICE).setOnPreferenceClickListener(this);
    mPrefFrag.findPreference(PREFS_KEY_IMPORT_FROM_DEVICE).setOnPreferenceClickListener(this);
    mPrefFrag.findPreference(PREFS_KEY_EXPORT_TO_DROPBOX).setOnPreferenceClickListener(this);
    mPrefFrag.findPreference(PREFS_KEY_IMPORT_FROM_DROPBOX).setOnPreferenceClickListener(this);
  }

  /**
   * Called to handle the click on the back button.
   */
  @Override
  public void onBackPressed() {
    super.onBackPressed();
    UIHelper.closeAnimation(this);
  }

  /**
   * Called when the options item is clicked (home).
   * @param item The selected menu.
   * @return boolean
   */
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId())
    {
      case android.R.id.home:
        NavUtils.navigateUpFromSameTask(this);
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  /**
   * Called when a preference is clicked.
   * @param preference The preference.
   * @return boolean
   */
  @Override
  public boolean onPreferenceClick(final Preference preference) {
    if (preference.equals(mPrefFrag.findPreference(PREFS_KEY_EXPORT_TO_DROPBOX))) {
      DropboxAutoExportService.setNeedUpdate(mApp, false);
      mApp.reloadDatabaseMD5();
      mApp.getDropboxImportExport().exportDatabase(this, true, null);
    } else if (preference.equals(mPrefFrag.findPreference(PREFS_KEY_IMPORT_FROM_DROPBOX))) {
      mApp.getDropboxImportExport().importDatabase(this);
    } else if (preference.equals(mPrefFrag.findPreference(PREFS_KEY_EXPORT_TO_DEVICE))) {
      Map<String, String> extra = new HashMap<>();
      extra.put(FileChooserActivity.FILECHOOSER_TYPE_KEY, "" + FileChooserActivity.FILECHOOSER_TYPE_DIRECTORY_ONLY);
      extra.put(FileChooserActivity.FILECHOOSER_TITLE_KEY, getString(R.string.pref_title_export));
      extra.put(FileChooserActivity.FILECHOOSER_MESSAGE_KEY, getString(R.string.use_folder) + ":? ");
      extra.put(FileChooserActivity.FILECHOOSER_DEFAULT_DIR, Environment
        .getExternalStorageDirectory().getAbsolutePath());
      extra.put(FileChooserActivity.FILECHOOSER_SHOW_KEY, "" + FileChooserActivity.FILECHOOSER_SHOW_DIRECTORY_ONLY);
      myStartActivity(extra, FileChooserActivity.class, FileChooserActivity.FILECHOOSER_SELECTION_TYPE_DIRECTORY);
    } else if (preference.equals(mPrefFrag.findPreference(PREFS_KEY_IMPORT_FROM_DEVICE))) {
      Map<String, String> extra = new HashMap<>();
      extra.put(FileChooserActivity.FILECHOOSER_TYPE_KEY, "" + FileChooserActivity.FILECHOOSER_TYPE_FILE_AND_DIRECTORY);
      extra.put(FileChooserActivity.FILECHOOSER_TITLE_KEY, getString(R.string.pref_title_import));
      extra.put(FileChooserActivity.FILECHOOSER_MESSAGE_KEY, getString(R.string.use_file) + ":? ");
      extra.put(FileChooserActivity.FILECHOOSER_DEFAULT_DIR, Environment
        .getExternalStorageDirectory().getAbsolutePath());
      extra.put(FileChooserActivity.FILECHOOSER_SHOW_KEY, "" + FileChooserActivity.FILECHOOSER_SHOW_FILE_AND_DIRECTORY);
      myStartActivity(extra, FileChooserActivity.class, FileChooserActivity.FILECHOOSER_SELECTION_TYPE_FILE);
    }
    return true;
  }

  /**
   * Called when the file chooser is disposed with a result.
   * @param requestCode The request code.
   * @param resultCode The result code.
   * @param data The Intent data.
   */
  @Override
  protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    // Check which request we're responding to
    if (requestCode == FileChooserActivity.FILECHOOSER_SELECTION_TYPE_DIRECTORY) {
      if (resultCode == RESULT_OK) {
        String dir = data.getStringExtra(FileChooserActivity.FILECHOOSER_SELECTION_KEY);
        try {
          SqlHelper.copyDatabase(this, SqlHelper.DB_NAME, dir);
          UIHelper.toast(this, getString(R.string.export_success));
        } catch(Exception e) {
          UIHelper.toastLong(this, getString(R.string.error) + ": " + e.getMessage());
          Log.e(getClass().getSimpleName(), "Error: " + e.getMessage(), e);
        }
      }
    } else if (requestCode == FileChooserActivity.FILECHOOSER_SELECTION_TYPE_FILE) {
      if (resultCode == RESULT_OK) {
        String file = data.getStringExtra(FileChooserActivity.FILECHOOSER_SELECTION_KEY);
        Log.d(getClass().getSimpleName(), "Selected file: '" + file + "'");
        List<File> files = new ArrayList<>();
        for(File f : new File(file).listFiles()) {
          if(f.getName().endsWith(".sqlite3"))
            files.add(f);
        }
        files.sort(Comparator.comparing(File::lastModified));
        DropboxImportExport.computeAndLoad(this, files, new DropboxImportExport.AlertDialogListListener<String>() {
          @Override
          public void onClick(String s) {
            try {
              DropboxImportExport.loadDb(SettingsImportExportActivity.this, new File(s));
            } catch (Exception e) {
              UIHelper.toastLong(SettingsImportExportActivity.this, getString(R.string.error) + ": " + e.getMessage());
              Log.e(getClass().getSimpleName(), "Error: " + e.getMessage(), e);
            }
          }
        });
      }
    }
  }

  /**
   * Starts an activity.
   * @param extra The extra data.
   * @param c The Android context.
   * @param code The request code.
   */
  private void myStartActivity(Map<String, String> extra, Class<?> c, int code) {
    final Intent i = new Intent(getApplicationContext(), c);
    if(extra != null) {
      Set<String> keysSet = extra.keySet();
      for (String key : keysSet) {
        i.putExtra(key, extra.get(key));
      }
    }
    startActivityForResult(i, code);
  }

  public static class MyPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setRetainInstance(true);
      addPreferencesFromResource(R.xml.preferences_import_export);
    }
  }
}

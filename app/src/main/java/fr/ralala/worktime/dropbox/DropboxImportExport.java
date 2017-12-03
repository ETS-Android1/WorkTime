package fr.ralala.worktime.dropbox;


import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import fr.ralala.worktime.MainApplication;
import fr.ralala.worktime.R;
import fr.ralala.worktime.sql.SqlHelper;
import fr.ralala.worktime.utils.AndroidHelper;
import fr.ralala.worktime.ui.utils.UIHelper;

/**
 *******************************************************************************
 * <p><b>Project WorkTime</b><br/>
 * Manage exportation and importation using dropbox
 * </p>
 * @author Keidan
 *
 *******************************************************************************
 */
public class DropboxImportExport implements DropboxListener{
  private static final String PATH = "";
  private DropboxHelper mHelper = null;
  private AlertDialog mDialog = null;
  private File mFile = null;
  private Context mContext = null;
  private DropboxUploaded mDropboxUploaded = null;
  private DropboxDownloaded mDropboxDownloaded = null;


  public DropboxImportExport() {
    mHelper = DropboxHelper.helper();
  }

  public interface DropboxUploaded {
    void dropboxUploaded(final boolean error);
  }
  public interface DropboxDownloaded {
    void dropboxDownloaded(final boolean error);
  }

  public void importDatabase(final Context c) {
    mContext = c;
    mDropboxDownloaded = null;
    if(mHelper.connect(c, c.getString(R.string.app_key))) {
      if(mDialog == null)
        mDialog = UIHelper.showProgressDialog(c, R.string.data_transfer);
      mDialog.show();
      new ListFolderTask(mHelper.getClient(), this).execute(PATH);
    }
  }

  public boolean exportDatabase(final Context c, boolean displayDialog, final DropboxUploaded dropboxUploaded) {
    mContext = c;
    mDropboxUploaded = dropboxUploaded;
    if(mDialog == null && displayDialog)
      mDialog = UIHelper.showProgressDialog(c, R.string.data_transfer);
    if(mHelper.connect(c, c.getString(R.string.app_key))) {
      if(mDialog != null)
        mDialog.show();
      try {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        mFile = new File(SqlHelper.copyDatabase(c, SqlHelper.DB_NAME, path.getAbsolutePath()));
        new UploadFileTask(c, mHelper.getClient(), this).execute(Uri.fromFile(mFile).toString(), PATH);
        return true;
      } catch(Exception e) {
        safeRemove();
        if(mDialog != null)
          mDialog.dismiss();
        UIHelper.toast_long(c, c.getString(R.string.error) + ": " + e.getMessage());
        Log.e(getClass().getSimpleName(), "Error: " + e.getMessage(), e);
      }
    }
    return false;
  }

  private void safeRemove() {
    if(mFile != null) {
      //noinspection ResultOfMethodCallIgnored
      mFile.delete();
      mFile = null;
    }
  }

  @Override
  public void onDropboxUploadComplete(FileMetadata result) {
    if(mDialog != null)
      mDialog.dismiss();
    UIHelper.toast(mContext, mContext.getString(R.string.export_success));
    safeRemove();
    if(mDropboxUploaded != null)
      mDropboxUploaded.dropboxUploaded(false);
  }

  @Override
  public void onDropboxUploadError(Exception e) {
    if(mDialog != null)
      mDialog.dismiss();
    Log.e(getClass().getSimpleName(), "Failed to upload file.", e);
    UIHelper.toast(mContext, mContext.getString(R.string.error_dropbox_upload));
    safeRemove();
    if(mDropboxUploaded != null)
      mDropboxUploaded.dropboxUploaded(true);
  }

  @Override
  public void onDroptboxDownloadComplete(File result) {
    if(mDialog != null)
      mDialog.dismiss();
    try {
      loadDb(mContext, result);
    } catch(Exception e) {
      UIHelper.toast_long(mContext, mContext.getString(R.string.error) + ": " + e.getMessage());
      Log.e(getClass().getSimpleName(), "Error: " + e.getMessage(), e);
    }
    safeRemove();
    if(mDropboxDownloaded != null)
      mDropboxDownloaded.dropboxDownloaded(false);
  }

  @Override
  public void onDroptboxDownloadError(Exception e) {
    if(mDialog != null)
      mDialog.dismiss();
    Log.e(getClass().getSimpleName(), "Failed to download file.", e);
    UIHelper.toast(mContext, mContext.getString(R.string.error_dropbox_download));
    safeRemove();
    if(mDropboxDownloaded != null)
      mDropboxDownloaded.dropboxDownloaded(true);
  }


  @Override
  public void onDroptboxListFoderDataLoaded(ListFolderResult result) {
    if(mDialog != null)
      mDialog.dismiss();
    List<Metadata> list = result.getEntries();
    list.sort(Comparator.comparing(Metadata::getName));
    if (list.isEmpty())
      UIHelper.toast_long(mContext, mContext.getString(R.string.error_no_files));
    else {
      computeAndLoad(mContext, list, new AlertDialogListListener<Metadata>() {
        @Override
        public void onClick(final Metadata m) {
          try {
            if(mDialog != null)
              mDialog.show();
            new DownloadFileTask(mContext, mHelper.getClient(), DropboxImportExport.this).execute((FileMetadata)m);
          } catch (Exception e) {
            UIHelper.toast_long(mContext, mContext.getString(R.string.error) + ": " + e.getMessage());
            Log.e(getClass().getSimpleName(), "Error: " + e.getMessage(), e);
          }
        }
      });
    }
  }

  @Override
  public void onDroptboxListFoderError(Exception e) {
    if(mDialog != null)
      mDialog.dismiss();
    Log.e(getClass().getSimpleName(), "Failed to get the file list.", e);
    UIHelper.toast(mContext, mContext.getString(R.string.error_dropbox_list_directory));
    if(mDropboxDownloaded != null)
      mDropboxDownloaded.dropboxDownloaded(true);
  }

  public static void loadDb(final Context c, File file) throws Exception{
    SqlHelper.loadDatabase(c, SqlHelper.DB_NAME, file);
    MainApplication app = MainApplication.getApp(c);
    app.getDaysFactory().reload(app.getSql());
    app.getProfilesFactory().reload(app.getSql());
    app.getPublicHolidaysFactory().reload(app.getSql());
    UIHelper.toast(c, c.getString(R.string.import_success));
    AndroidHelper.restartApplication(c, -1);
  }

  public static <T, V> void computeAndLoad(final Context c, final List<T> list, AlertDialogListListener<V> yes) {
    List<String> files = compute(list);
    if(files.isEmpty())
      UIHelper.toast_long(c, c.getString(R.string.error_no_files));
    else {
      showAlertDialog(c, R.string.box_select_db_file, files, yes);
    }
  }

  @SuppressWarnings("unchecked")
  private static <T, V> List<V> compute(final List<T> list) {
    List<V> files = new ArrayList<>();
    for(T t : list) {
      if(File.class.isInstance(t)) {
        File f = (File)t;
        if (f.getName().endsWith(".sqlite3"))
          files.add((V)f);
      } else if(FileMetadata.class.isInstance(t)) {
        FileMetadata f = (FileMetadata)t;
        if (f.getName().endsWith(".sqlite3"))
          files.add((V)f);
      }
    }
    return files;
  }

  private static class ListItem<T> {
    public String name;
    T value;

    ListItem(final String name, final T value) {
      this.name = name;
      this.value = value;
    }

    public String toString() {
      return name;
    }
  }

  public interface AlertDialogListListener<T> {
    void onClick(T t);
  }

  @SuppressWarnings("unchecked")
  private static <T> void showAlertDialog(final Context c, final int title, List<T> list, final AlertDialogListListener yes) {
    AlertDialog.Builder builder = new AlertDialog.Builder(c);
    builder.setTitle(c.getResources().getString(title));
    builder.setIcon(android.R.drawable.ic_dialog_alert);
    List<ListItem> items = new ArrayList<>();
    for(T s : list) {
      String ss = new File(s.toString()).getName();
      if(ss.endsWith("\"}")) ss = ss.substring(0, ss.length() - 2);
      items.add(new ListItem<>(ss, s));
    }
    final ArrayAdapter<ListItem> arrayAdapter = new ArrayAdapter<>(c, android.R.layout.select_dialog_item, items);
    builder.setNegativeButton(c.getString(R.string.cancel), (dialog, which) -> dialog.dismiss());

    builder.setAdapter(arrayAdapter, (dialog, which) -> {
      dialog.dismiss();
      ListItem li = arrayAdapter.getItem(which);
      if(yes != null && li != null) yes.onClick(li.value);
    });
    builder.show();
  }
}

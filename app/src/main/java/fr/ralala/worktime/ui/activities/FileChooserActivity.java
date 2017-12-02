package fr.ralala.worktime.ui.activities;

import java.io.File;

import fr.ralala.worktime.R;
import fr.ralala.worktime.models.FileChooserOption;
import fr.ralala.worktime.ui.utils.UIHelper;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

/**
 *******************************************************************************
 * <p><b>Project WorkTime</b><br/>
 * File chooser activity
 * </p>
 * @author Keidan
 *
 *******************************************************************************
 */
public class FileChooserActivity extends AbstractFileChooserActivity {
  public static final String FILECHOOSER_SELECTION_KEY = "selection";
  public static final int FILECHOOSER_SELECTION_TYPE_FILE = 1;
  public static final int FILECHOOSER_SELECTION_TYPE_DIRECTORY = 2;
  private static final int MSG_ERR    = 0;
  private static final int MSG_OK     = 1;
  private static final int MSG_CANCEL = 2;
  private FileChooserOption opt = null;
  private Handler handler = null;
  private AlertDialog progress = null;
  public enum ErrorStatus {
    NO_ERROR, CANCEL, ERROR_NOT_MOUNTED, ERROR_CANT_READ
  }

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    handler = new IncomingHandler(this);
  }
  

  @Override
  public boolean onOptionsItemSelected(final MenuItem item) {
    if (item.getItemId() == R.id.action_cancel) {
        cancel();
    }
    return false;
  }
  

  @Override
  public boolean onCreateOptionsMenu(final Menu menu) {
      // Inflate the menu; this adds items to the action bar if it is present.
      getMenuInflater().inflate(R.menu.filechooser_menu, menu);
      return true;
  }

  @Override
  protected void onFileSelected(final FileChooserOption opt) {
    progress = UIHelper.showProgressDialog(this, R.string.loading);
    progress.show();
    // useful code, variables declarations...
    new Thread((new Runnable() {
      @Override
      public void run() {
        // starts the first long operation
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            Message msg;
            ErrorStatus status = doComputeHandler(opt);
            if (status == ErrorStatus.CANCEL) {
              msg = handler.obtainMessage(MSG_CANCEL, this);
              // sends the message to our handler
              handler.sendMessage(msg);
            } else if (status != ErrorStatus.NO_ERROR) {
              // error management, creates an error message
              msg = handler.obtainMessage(MSG_ERR, this);
              // sends the message to our handler
              handler.sendMessage(msg);
            } else {
              msg = handler.obtainMessage(MSG_OK, this);
              // sends the message to our handler
              handler.sendMessage(msg);
            }
          }
        });

      }
    })).start();

  }

  @Override
  public void onBackPressed() {
    File parent = currentDir.getParentFile();
    if (parent == null || parent.equals(defaultDir.getParentFile())) {
      super.onBackPressed();
      cancel();
    } else {
      currentDir = parent;
      fill(currentDir);
    }
  }
  
  private void cancel() {
    finish();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    cancel();
  }

  public ErrorStatus doComputeHandler(final FileChooserOption userObject) {
    opt = userObject;
    if (opt == null)
      return ErrorStatus.CANCEL; /* cancel action */
    if (!isMountedSdcard())
      return ErrorStatus.ERROR_NOT_MOUNTED;

    final File file = new File(new File(opt.getPath()).getParent(),
        opt.getName());
    if (!file.canRead())
      return ErrorStatus.ERROR_CANT_READ;
    return ErrorStatus.NO_ERROR;
  }

  public void onSuccessHandler() {
    final Intent returnIntent = new Intent();
    int result = RESULT_CANCELED;
    if (opt != null) {
      final File file = new File(new File(opt.getPath()).getParent(),
          opt.getName());
      returnIntent.putExtra(FILECHOOSER_SELECTION_KEY, file.getAbsolutePath());
      if(getUserMessage() != null)
        returnIntent.putExtra(FILECHOOSER_USER_MESSAGE, getUserMessage());
      result = RESULT_OK;
    }
    setResult(result, returnIntent);
    opt = null;
    cancel();
  }

  public void onCancelHandler() {

  }

  public void onErrorHandler() {
    opt = null;
    final Intent returnIntent = new Intent();
    setResult(RESULT_CANCELED, returnIntent);
    onBackPressed();
  }

  private boolean isMountedSdcard() {
    final String state = Environment.getExternalStorageState();
    return (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
  }


  private static class IncomingHandler extends Handler {

    private FileChooserActivity adaptee = null;

    private IncomingHandler(FileChooserActivity adaptee) {
      this.adaptee = adaptee;
    }

    @Override
    public void handleMessage(final Message msg) {
      switch (msg.what) {
        case MSG_ERR:
          final String err = "Activity compute failed !";
          Log.e(getClass().getSimpleName(), err);
          if (adaptee.progress.isShowing())
            adaptee.progress.dismiss();
          Toast.makeText(adaptee, err, Toast.LENGTH_SHORT).show();
          adaptee.onErrorHandler();
          break;
        case MSG_OK:
          if (adaptee.progress.isShowing())
            adaptee.progress.dismiss();
          adaptee.onSuccessHandler();
          break;
        case MSG_CANCEL:
          if (adaptee.progress.isShowing()) adaptee.progress.dismiss();
          adaptee.onCancelHandler();
          break;
        default: // should never happen
          break;
      }
    }
  }
}

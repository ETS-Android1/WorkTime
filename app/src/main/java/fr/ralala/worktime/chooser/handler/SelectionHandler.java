package fr.ralala.worktime.chooser.handler;

import android.app.Activity;

import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

/**
 *******************************************************************************
 * <p><b>Project WorkTime</b><br/>
 * File selection handler
 * </p>
 * @author Keidan
 *
 *******************************************************************************
 */
public class SelectionHandler {
  private Handler          handler    = null;
  private ProgressDialog   progress   = null;
  private Activity         ctx        = null;
  private ErrorStatus      status     = null;

  private static final int MSG_ERR    = 0;
  private static final int MSG_OK     = 1;
  private static final int MSG_CANCEL = 2;

  public SelectionHandler(final Activity ctx) {
    this.ctx = ctx;
    handler = new IncomingHandler(this);
  }

  static class IncomingHandler extends Handler {
    private SelectionHandler adaptee = null;

    public IncomingHandler(final SelectionHandler adaptee) {
      this.adaptee = adaptee;
    }

    @Override
    public void handleMessage(final Message msg) {
      adaptee.handleMessage(msg);
    }
  };

  public void compute(final ISelectionHandler action, final Object userObject) {
    progress = ProgressDialog.show(ctx, "", "Loading...", true);
    // useful code, variables declarations...
    new Thread((new Runnable() {
      @Override
      public void run() {
        // starts the first long operation
        ctx.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            Message msg = null;
            status = action.doCompute(userObject);
            if (status == ErrorStatus.CANCEL) {
              msg = handler.obtainMessage(MSG_CANCEL, action);
              // sends the message to our handler
              handler.sendMessage(msg);
            } else if (status != ErrorStatus.NO_ERROR) {
              // error management, creates an error message
              msg = handler.obtainMessage(MSG_ERR, action);
              // sends the message to our handler
              handler.sendMessage(msg);
            } else {
              msg = handler.obtainMessage(MSG_OK, action);
              // sends the message to our handler
              handler.sendMessage(msg);
            }
          }
        });

      }
    })).start();
  }

  public void handleMessage(final Message msg) {
    ISelectionHandler action = null;
    switch (msg.what) {
      case MSG_ERR:
        action = (ISelectionHandler) msg.obj;
        final String err = "Activity compute failed with error code: " + status;
        Log.e(getClass().getSimpleName(), err);
        if (progress.isShowing())
          progress.dismiss();
        Toast.makeText(ctx, err, Toast.LENGTH_SHORT).show();
        action.onError();
        break;
      case MSG_OK:
        action = (ISelectionHandler) msg.obj;
        if (progress.isShowing())
          progress.dismiss();
        action.onSuccess();
        break;
      case MSG_CANCEL:
        action = (ISelectionHandler) msg.obj;
        if (progress.isShowing()) progress.dismiss();
        action.onCancel();
        break;
      default: // should never happen
        break;
    }
  }
}

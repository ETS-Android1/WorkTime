package fr.ralala.worktime.ui.utils;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.CycleInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;

import fr.ralala.worktime.R;
import fr.ralala.worktime.models.WorkTimeDay;

/**
 *******************************************************************************
 * <p><b>Project WorkTime</b><br/>
 * UI Helper functions
 * </p>
 * @author Keidan
 *
 *******************************************************************************
 */
public class UIHelper {

  /**
   * Returns the view width.
   * @param view The view.
   * @return float
   */
  private static float getViewWidth(View view) {
    return Math.abs((float) view.getRight() - (float) view.getLeft());
  }

  /**
   * Returns percent of the view width.
   * @param view The view.
   * @param percent The percent value.
   * @return float
   */
  private static float percentWidthOf(View view, float percent) {
    return getViewWidth(view)*(percent/100.0f);
  }

  /**
   * Called by ItemTouchHelper on RecyclerView's onDraw callback.
   * @param activity An activity instance.
   * @param c The canvas which RecyclerView is drawing its children.
   * @param viewHolder The ViewHolder which is being interacted by the User or it was interacted and simply animating to its original position.
   * @param dX The amount of horizontal displacement caused by user's action.
   * @param actionState The type of interaction on the View. Is either ACTION_STATE_DRAG or ACTION_STATE_SWIPE.
   */
  public static void onRecyclerViewChildDrawWithEditAndDelete(Activity activity, Canvas c, RecyclerView.ViewHolder viewHolder, float dX, int actionState) {
    Bitmap icon;
    float percent = 11.f;
    final Paint paint = new Paint();
    if(actionState == ItemTouchHelper.ACTION_STATE_SWIPE){
      View itemView = viewHolder.itemView;
      float height = (float) itemView.getBottom() - (float) itemView.getTop();
      float width = height / 3;
      if(dX != 0) {
        if (dX > 0) {
          paint.setColor(ResourcesCompat.getColor(activity.getResources(), R.color.item_edit, activity.getTheme()));
          RectF background = new RectF((float) itemView.getLeft(), (float) itemView.getTop(), dX, (float) itemView.getBottom());
          c.drawRect(background, paint);
          if(background.right >= percentWidthOf(itemView, percent)) {
            icon = BitmapFactory.decodeResource(activity.getResources(), R.mipmap.ic_edit);
            RectF icon_dest = new RectF((float) itemView.getLeft() + width, (float) itemView.getTop() + width, (float) itemView.getLeft() + 2 * width, (float) itemView.getBottom() - width);
            c.drawBitmap(icon, null, icon_dest, paint);
          }
        } else {
          paint.setColor(ResourcesCompat.getColor(activity.getResources(), R.color.item_delete, activity.getTheme()));
          RectF background = new RectF((float) itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
          c.drawRect(background, paint);
          float max = getViewWidth(itemView);
          if(max - background.left >= percentWidthOf(itemView, percent)) {
            icon = BitmapFactory.decodeResource(activity.getResources(), R.mipmap.ic_delete);
            RectF icon_dest = new RectF((float) itemView.getRight() - 2 * width, (float) itemView.getTop() + width, (float) itemView.getRight() - width, (float) itemView.getBottom() - width);
            c.drawBitmap(icon, null, icon_dest, paint);
          }
        }
      }
    }
  }

  /**
   * Displays a progress dialog.
   * @param context The Android context.
   * @param message The progress message.
   * @return AlertDialog
   */
  public static AlertDialog showProgressDialog(Context context, int message) {
    LayoutInflater layoutInflater = LayoutInflater.from(context);
    final ViewGroup nullParent = null;
    View view = layoutInflater.inflate(R.layout.progress_dialog, nullParent);
    AlertDialog progress = new AlertDialog.Builder(context).create();
    TextView tv = view.findViewById(R.id.text);
    tv.setText(message);
    progress.setCancelable(false);
    progress.setView(view);
    return progress;
  }

  /**
   * Shake a view on error.
   * @param owner The owner view.
   * @param errText The error text.
   */
  public static void shakeError(TextView owner, String errText) {
    TranslateAnimation shake = new TranslateAnimation(0, 10, 0, 0);
    shake.setDuration(500);
    shake.setInterpolator(new CycleInterpolator(5));
    if(owner != null) {
      if(errText != null)
        owner.setError(errText);
      owner.clearAnimation();
      owner.startAnimation(shake);
    }
  }

  /**
   * Changes the view background with a gradient effect.
   * @param view The output view.
   * @param colors The gradient colors.
   */
  public static void applyLinearGradient(final View view, final int ...colors) {
    Drawable[] layers = new Drawable[1];

    ShapeDrawable.ShaderFactory sf = new ShapeDrawable.ShaderFactory() {
      @Override
      public Shader resize(int width, int height) {
        return new LinearGradient(
            view.getWidth(),
            0,
            0,
            0,
            colors,
            new float[] { 0, 1 },
            Shader.TileMode.CLAMP);
      }
    };
    PaintDrawable p = new PaintDrawable();
    p.setShape(new RectShape());
    p.setShaderFactory(sf);
    layers[0] = p;

    LayerDrawable composite = new LayerDrawable(layers);
    view.setBackground(composite);
  }

  /**
   * Opens a snack.
   * @param activity The associated activity.
   * @param msg The snack message.
   */
  public static void snack(final Activity activity, String msg) {
    snack(activity, msg, null, null);
  }

  /**
   * Opens a snack.
   * @param activity The associated activity.
   * @param msg The snack message.
   * @param actionLabel null for default label (Hide), the label.
   * @param clickListener Click listener (view = null ; snackbar.dismiss() is called after the event).
   */
  public static void snack(final Activity activity, String msg, String actionLabel, View.OnClickListener clickListener) {
    final View cl = activity.findViewById(R.id.coordinatorLayout);
    final Snackbar snackbar = Snackbar
        .make(cl, msg, Snackbar.LENGTH_LONG);
    snackbar.setAction(
        actionLabel == null ? activity.getString(R.string.snack_hide) : actionLabel, (view) -> {
          if(clickListener != null)
            clickListener.onClick(null);
          snackbar.dismiss();
        });
    snackbar.show();
  }

  /**
   * Displays a confirm dialog.
   * @param c The Android context.
   * @param cancelable Cancelable state.
   * @param message The dialog message.
   * @param yes Listener used when the 'yes' button is clicked.
   * @param no Listener used when the 'no' button is clicked.
   */
  public static void showConfirmDialog(final Context c, boolean cancelable,
                                       String message, final android.view.View.OnClickListener yes, final android.view.View.OnClickListener no) {
    new AlertDialog.Builder(c)
        .setCancelable(cancelable)
        .setMessage(message)
        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
          if(yes != null) yes.onClick(null);
        })
        .setNegativeButton(android.R.string.no, (dialog, whichButton) -> {
          if(no != null) no.onClick(null);
        }).show();
  }

  /**
   * Displays a confirm dialog.
   * @param c The Android context.
   * @param title The dialog title.
   * @param message The dialog message.
   * @param yes Listener used when the 'yes' button is clicked.
   * @param no Listener used when the 'no' button is clicked.
   */
  public static void showConfirmDialog(final Context c, final String title,
                                       String message, final android.view.View.OnClickListener yes,
                                       final android.view.View.OnClickListener no) {
    new AlertDialog.Builder(c)
        .setTitle(title)
        .setMessage(message)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
          if(yes != null) yes.onClick(null);
        })
        .setNegativeButton(android.R.string.no, (dialog, whichButton) -> {
          if(no != null) no.onClick(null);
        }).show();
  }

  /**
   * Opens a date picker dialog.
   * @param c The Android context.
   * @param current The current date.
   * @param li The listener used when the date is selected.
   */
  public static void openDatePicker(final Context c, final Calendar current, DatePickerDialog.OnDateSetListener li) {
    DatePickerDialog dpd = new DatePickerDialog(c, li, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH));
    dpd.show();
  }

  /**
   * Opens a time picker dialog.
   * @param c The Android context.
   * @param current The current time.
   * @param tv The output text view.
   */
  public static void openTimePicker(final Context c, final WorkTimeDay current, final TextView tv) {
    tv.setText(current.timeString());
    AlertDialog.Builder builder = new AlertDialog.Builder(c);
    builder.setView(R.layout.timepicker);
    // Set up the buttons
    builder.setPositiveButton(c.getString(R.string.ok), null);
    builder.setNegativeButton(c.getString(R.string.cancel), null);
    final AlertDialog mAlertDialog = builder.create();

    mAlertDialog.setOnShowListener((dialog) -> {
      Button b = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
      b.setOnClickListener((view) -> {
        final TimePicker timePicker = mAlertDialog.findViewById(R.id.timepicker);
        if(timePicker != null) {
          tv.setText(String.format(Locale.US, "%02d:%02d", timePicker.getHour(), timePicker.getMinute()));
          current.setHours(timePicker.getHour());
          current.setMinutes(timePicker.getMinute());
        }
        mAlertDialog.dismiss();
      });
    });
    mAlertDialog.show();
    final TimePicker timePicker = mAlertDialog.findViewById(R.id.timepicker);
    if(timePicker != null) {
      timePicker.setIs24HourView(true);
      timePicker.setHour(current.getHours());
      timePicker.setMinute(current.getMinutes());
    }
  }

  /**
   * Displays an alert dialog.
   * @param c The Android context.
   * @param title The alert dialog title.
   * @param message The alert dialog message.
   */
  public static void showAlertDialog(final Context c, final int title, final String message) {
    AlertDialog alertDialog = new AlertDialog.Builder(c).create();
    alertDialog.setTitle(c.getResources().getString(title));
    alertDialog.setMessage(message);
    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, c.getResources().getString(R.string.ok), (dialog, which) -> dialog.dismiss());
    alertDialog.show();
  }

  /**
   * Displays a toast.
   * @param c The Android context.
   * @param message The toast message.
   * @param timer The toast duration.
   */
  public static void toast(final Context c, final String message, final int timer) {
    /* Create a toast with the launcher icon */
    Toast toast = Toast.makeText(c, message, timer);
    TextView tv = toast.getView().findViewById(android.R.id.message);
    if (null!=tv) {
      Drawable drawable = ContextCompat.getDrawable(c, R.mipmap.ic_launcher);
      if(drawable != null) {
        final Bitmap b = ((BitmapDrawable) drawable).getBitmap();
        final Bitmap bitmapResized = Bitmap.createScaledBitmap(b, 32, 32, false);
        tv.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(c.getResources(), bitmapResized), null, null, null);
        tv.setCompoundDrawablePadding(5);
      }
    }
    toast.show();
  }

  /**
   * Displays a long toast.
   * @param c The Android context.
   * @param message The toast message.
   */
  public static void toastLong(final Context c, final int message) {
    toast(c, c.getResources().getString(message), Toast.LENGTH_LONG);
  }

  /**
   * Displays a long toast.
   * @param c The Android context.
   * @param message The toast message.
   */
  public static void toastLong(final Context c, final String message) {
    toast(c, message, Toast.LENGTH_LONG);
  }

  /**
   * Displays a short toast.
   * @param c The Android context.
   * @param message The toast message.
   */
  public static void toast(final Context c, final String message) {
    toast(c, message, Toast.LENGTH_SHORT);
  }

  /**
   * Displays a short toast.
   * @param c The Android context.
   * @param message The toast message.
   */
  public static void toast(final Context c, final int message) {
    toast(c, c.getResources().getString(message));
  }

  /**
   * Starts a transition effect (slide) when the activity is opened.
   * @param a The activity to animate.
   */
  public static void openAnimation(final Activity a) {
    a.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
  }

  /**
   * Starts a transition effect (slide) when the activity is closed.
   * @param a The activity to animate.
   */
  public static void closeAnimation(final Activity a) {
    a.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
  }
}

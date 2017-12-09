package fr.ralala.worktime.ui.fragments;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.Locale;

import fr.ralala.worktime.MainApplication;
import fr.ralala.worktime.R;
import fr.ralala.worktime.models.DayEntry;
import fr.ralala.worktime.models.WorkTimeDay;
import fr.ralala.worktime.services.QuickAccessService;
import fr.ralala.worktime.utils.AndroidHelper;

/**
 *******************************************************************************
 * <p><b>Project WorkTime</b><br/>
 * Management of the quick access fragment view
 * </p>
 * @author Keidan
 *
 *******************************************************************************
 */
public class QuickAccessFragment extends Fragment implements OnClickListener {
  private MainApplication app = null;
  private Button btFinalize = null;
  private ToggleButton btStart = null;
  private TextView tvTime = null;
  private Blink blink = null;

  /**
   * Called when the fragment is created.
   * @param inflater The fragment inflater.
   * @param container The fragment container.
   * @param savedInstanceState The saved instance state.
   * @return The created view.
   */
  @Override
  public View onCreateView(@NonNull final LayoutInflater inflater,
                           final ViewGroup container, final Bundle savedInstanceState) {
    final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_quick_access, container, false);

    btFinalize = rootView.findViewById(R.id.btFinalize);
    btStart = rootView.findViewById(R.id.btStart);
    tvTime = rootView.findViewById(R.id.tvTime);

    btFinalize.setOnClickListener(this);
    btStart.setOnClickListener(this);
    btFinalize.setVisibility(View.GONE);

    tvTime.setText(getString(R.string.default_time));
    app = MainApplication.getApp(getContext());
    return rootView;
  }

  /**
   * Called when the fragment is resumed.
   */
  @Override
  public void onResume() {
    super.onResume();
    if(blink == null) {
      blink = new Blink();
      blink.start();
    }
    if(isStarted() || app.getQuickAccessNotification().isVisible()) {
      btStart.setChecked(true);
      btFinalize.setVisibility(View.VISIBLE);
    } else {
      btStart.setChecked(false);
      btFinalize.setVisibility(View.GONE);
    }
    updateText(app.getDaysFactory().getCurrentDay(), false, false);
  }

  /**
   * Called when the fragment is destroyed.
   */
  @Override
  public void onDestroy() {
    if(blink != null) {
      blink.kill();
      blink = null;
    }
    super.onDestroy();
  }

  /**
   * Called when the user click on a button (start finalize).
   * @param v The clicked view.
   */
  @Override
  public void onClick(View v) {
    Activity activity = getActivity();
    if(activity == null) return;
    if(v.equals(btStart)) {
      String text = getString(R.string.work_time) + ": ";
      text += tvTime.getText() + ":00";
      if(btStart.isChecked()) {
        app.setQuickAccessPause(false);
        btFinalize.setVisibility(View.VISIBLE);
        app.getQuickAccessNotification().set(text);
      } else {
        app.setQuickAccessPause(true);
        app.getQuickAccessNotification().update(null, app.isQuickAccessPause());
      }
      if(!app.isQuickAccessPause()) {
        if(!isStarted()) {
          activity.startService(new Intent(activity, QuickAccessService.class));
        }
      } else if(isStarted())  {
        activity.stopService(new Intent(activity, QuickAccessService.class));
      }
    } else if(v.equals(btFinalize)) {
      app.setQuickAccessPause(true);
      btStart.setChecked(false);
      btFinalize.setVisibility(View.GONE);
      DayEntry de = app.getDaysFactory().getCurrentDay();
      app.getDaysFactory().remove(de);
      app.getDaysFactory().add(de);
      activity.stopService(new Intent(activity, QuickAccessService.class));
      app.getQuickAccessNotification().remove(activity);
    }
  }

  /**
   * Class used to blink(:) and update the time label.
   */
  class Blink extends Thread {
    private boolean end = false;

    private void kill() {
      end = true;
      interrupt();
    }

    public void run() {
      long delay = 1000;
      while(!end) {
        try {
          sleep(delay);
        } catch (InterruptedException e) {
          return;
        }
        boolean restore = true;
        /* updated from the notification */
        if(isStarted() && !btStart.isChecked() && !app.isQuickAccessPause()) {
          btStart.post(() -> btStart.setChecked(true));
        } else if(!isStarted() && btStart.isChecked()) {
          btStart.post(() -> btStart.setChecked(false));
        }
        while(app.isQuickAccessPause()) try {
          if(restore) {
            updateText(app.getDaysFactory().getCurrentDay(), true, true);
            restore = false;
          }
          sleep(delay);
        } catch (InterruptedException e) {
          return;
        }
        updateText(app.getDaysFactory().getCurrentDay(), false, true);
      }
    }
  }

  /**
   * Tests if the service is started.
   * @return boolean
   */
  private boolean isStarted() {
    return AndroidHelper.isServiceRunning(getContext(), QuickAccessService.class);
  }

  /**
   * Updates the time text.
   * @param d The current time entry
   * @param reset True to reset the text.
   * @param post True to post the treatment in the UI thread.
   */
  private void updateText(DayEntry d, final boolean reset, final boolean post) {
    if(!post) {
      WorkTimeDay w = d.getWorkTime();
      tvTime.setText(String.format(Locale.US, "%02d:%02d", w.getHours(), w.getMinutes()));
    } else {
      tvTime.post(() -> {
        String text = tvTime.getText().toString();
        if (reset) {
          tvTime.setText(text.replaceAll(" ", ":"));
        } else {
          boolean space = text.contains(" ");
          DayEntry dd = app.getDaysFactory().getCurrentDay();
          WorkTimeDay w = dd.getWorkTime();
          if(w.getHours() < 0 || w.getMinutes() < 0)
            text = String.format(Locale.US, "-%02d%s%02d", Math.abs(w.getHours()), space ? ":" : " ", Math.abs(w.getMinutes()));
          else
            text = String.format(Locale.US, "%02d%s%02d", w.getHours(), space ? ":" : " ", w.getMinutes());
          tvTime.setText(text);
        }
      });
    }
  }
}

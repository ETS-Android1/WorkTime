package fr.ralala.worktime.ui.fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import fr.ralala.worktime.ui.activities.DayActivity;

import fr.ralala.worktime.MainApplication;
import fr.ralala.worktime.R;
import fr.ralala.worktime.ui.adapters.ProfilesEntriesArrayAdapter;
import fr.ralala.worktime.ui.adapters.SimpleEntriesArrayAdapterMenuListener;
import fr.ralala.worktime.models.DayEntry;


/**
 *******************************************************************************
 * <p><b>Project WorkTime</b><br/>
 * Management of the profiles fragment view
 * </p>
 * @author Keidan
 *
 *******************************************************************************
 */
public class ProfileFragment  extends Fragment implements SimpleEntriesArrayAdapterMenuListener<DayEntry>, View.OnClickListener {

  private ProfilesEntriesArrayAdapter mAdapter = null;
  private MainApplication mApp = null;

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
    final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_profile, container, false);
    ListView lv = rootView.findViewById(R.id.profiles);
    FloatingActionButton fab = rootView.findViewById(R.id.fab);
    fab.setOnClickListener(this);
    mApp = MainApplication.getApp(getContext());
    mAdapter = new ProfilesEntriesArrayAdapter(
      getContext(), R.layout.listview_item, mApp.getProfilesFactory().list(), this);
    lv.setAdapter(mAdapter);
    return rootView;
  }

  /**
   * Called when the fragment is resumed.
   */
  @Override
  public void onResume() {
    super.onResume();
    if(mApp.isResumeAfterActivity()) {
      mAdapter.notifyDataSetChanged();
      mApp.setResumeAfterActivity(false);
    }
  }

  /**
   * Called when the user click on a button (fab).
   * @param view The clicked view.
   */
  @Override
  public void onClick(View view) {
    mApp.setResumeAfterActivity(true);
    DayActivity.startActivity(getActivity(), "null", false);
  }

  /**
   * Called when the edit button is clicked.
   * @param de The clicked entry object.
   */
  @Override
  public void onMenuEdit(DayEntry de) {
    mApp.setResumeAfterActivity(true);
    DayActivity.startActivity(getActivity(), de.getName(), false);
  }

  /**
   * Called when the delete button is clicked.
   * @param de The clicked entry object.
   */
  @Override
  public void onMenuDelete(DayEntry de) {
    mApp.getProfilesFactory().remove(de);
    mAdapter.remove(de);
  }
}

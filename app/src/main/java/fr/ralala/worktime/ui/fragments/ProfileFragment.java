package fr.ralala.worktime.ui.fragments;


import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import fr.ralala.worktime.ui.activities.DayActivity;

import fr.ralala.worktime.MainApplication;
import fr.ralala.worktime.R;
import fr.ralala.worktime.ui.adapters.ProfilesEntriesArrayAdapter;
import fr.ralala.worktime.models.DayEntry;
import fr.ralala.worktime.ui.utils.UIHelper;


/**
 *******************************************************************************
 * <p><b>Project WorkTime</b><br/>
 * Management of the profiles fragment view
 * </p>
 * @author Keidan
 *
 *******************************************************************************
 */
public class ProfileFragment  extends Fragment implements View.OnClickListener {

  private ProfilesEntriesArrayAdapter mAdapter = null;
  private MainApplication mApp = null;
  private RecyclerView mRecyclerView;
  private Activity mActivity;
  private Paint mPaint = new Paint();

  /**
   * Called when the fragment is created.
   * @param savedInstanceState The saved instance state.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

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
    FloatingActionButton fab = rootView.findViewById(R.id.fab);
    fab.setOnClickListener(this);
    mApp = MainApplication.getApp(getContext());
    mActivity = getActivity();
    if(mActivity == null)
      return rootView;

    mRecyclerView = rootView.findViewById(R.id.profiles);
    mRecyclerView.setHasFixedSize(true);
    RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
    mRecyclerView.setLayoutManager(layoutManager);
    mAdapter = new ProfilesEntriesArrayAdapter(
        getContext(), R.layout.listview_item, mApp.getProfilesFactory().list());
    mRecyclerView.setAdapter(mAdapter);
    mAdapter.notifyDataSetChanged();
    initSwipe();

    return rootView;
  }

  /**
   * Called when the options menu is created.
   * @param menu The menu to inflate.
   * @param inflater The menu inflater.
   */
  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.profile_fragment, menu);
    super.onCreateOptionsMenu(menu, inflater);
  }

  /**
   * Called when a menu is selected.
   * @param item The selected item.
   * @return boolean
   */
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_help:
        Activity a = getActivity();
        if(a == null)
          break;
        UIHelper.snack(a, getString(R.string.help_profile));
        break;
    }
    return true;
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

  private void initSwipe(){
    ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

      /**
       * Called when ItemTouchHelper wants to move the dragged item from its old position to the new position.
       * @param recyclerView The RecyclerView to which ItemTouchHelper is attached to.
       * @param viewHolder The ViewHolder which is being dragged by the user.
       * @param target The ViewHolder over which the currently active item is being dragged.
       * @return True if the viewHolder has been moved to the adapter position of target.
       */
      @Override
      public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
      }

      /**
       * Called when a ViewHolder is swiped by the user.
       * @param viewHolder The ViewHolder which has been swiped by the user.
       * @param direction The direction to which the ViewHolder is swiped. It is one of UP, DOWN, LEFT or RIGHT.
       */
      @Override
      public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        if (direction == ItemTouchHelper.LEFT){
          DayEntry de = mAdapter.getItem(position);
          if(de == null) return;
          UIHelper.showConfirmDialog(getActivity(),
              (getString(R.string.delete_public_holiday) + " '" + de.getName() + "'" + getString(R.string.help)),
              (v) -> {
                mAdapter.removeItem(de);
                mApp.getProfilesFactory().remove(de);
                UIHelper.snack(mActivity, getString(R.string.profile_removed),
                    getString(R.string.undo), (nullview) -> {
                      mAdapter.addItem(de);
                      mApp.getProfilesFactory().add(de);
                    });
          }, (v) -> mRecyclerView.getAdapter().notifyItemChanged(viewHolder.getAdapterPosition()));
        } else {
          DayEntry de = mAdapter.getItem(position);
          if(de == null) return;
          mApp.setResumeAfterActivity(true);
          DayActivity.startActivity(getActivity(), de.getName(), false);
        }
      }

      /**
       * Called by ItemTouchHelper on RecyclerView's onDraw callback.
       * @param c The canvas which RecyclerView is drawing its children
       * @param recyclerView The recycler view.
       * @param viewHolder The ViewHolder which is being interacted by the User or it was interacted and simply animating to its original position
       * @param dX The amount of horizontal displacement caused by user's action
       * @param dY The amount of vertical displacement caused by user's action
       * @param actionState The type of interaction on the View. Is either ACTION_STATE_DRAG or ACTION_STATE_SWIPE.
       * @param isCurrentlyActive True if this view is currently being controlled by the user or false it is simply animating back to its original state.
       */
      @Override
      public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        UIHelper.onRecyclerViewChildDrawWithEditAndDelete(mActivity, mPaint, c, viewHolder, dX, actionState);
        //super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
      }
    };
    ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
    itemTouchHelper.attachToRecyclerView(mRecyclerView);
  }
}

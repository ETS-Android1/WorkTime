package fr.ralala.worktime.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import fr.ralala.worktime.sql.SqlFactory;

/**
 *******************************************************************************
 * <p><b>Project WorkTime</b><br/>
 * Profiles factory functions
 * </p>
 * @author Keidan
 *
 *******************************************************************************
 */
public class ProfilesFactory {
  private final List<DayEntry> profiles;
  private SqlFactory sql = null;

  public ProfilesFactory() {
    profiles = new ArrayList<>();
  }

  public void reload(final SqlFactory sql) {
    this.sql = sql;
    profiles.clear();
    profiles.addAll(sql.getProfiles());
  }

  public List<DayEntry> list() {
    return profiles;
  }

  public boolean hasProfile(final String name) {
    for(DayEntry de : profiles) {
      if(de.getName().equals(name))
        return true;
    }
    return false;
  }

  public void remove(final DayEntry de) {
    profiles.remove(de);
    sql.removeProfile(de);
  }

  public void add(final DayEntry de) {
    profiles.add(de);
    sql.insertProfile(de);
    Collections.sort(profiles, new Comparator<DayEntry>() {
      @Override
      public int compare(DayEntry a, DayEntry b) {
        return a.getName().compareTo(b.getName());
      }
    });
  }
  public DayEntry getByName(final String name) {
    for(DayEntry de : profiles) {
      if(de.getName().equals(name))
        return de;
    }
    return null;
  }
}

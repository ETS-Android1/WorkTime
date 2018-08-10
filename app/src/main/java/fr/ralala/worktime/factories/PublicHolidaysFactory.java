package fr.ralala.worktime.factories;


import java.util.Collections;
import java.util.List;

import fr.ralala.worktime.models.PublicHolidayEntry;
import fr.ralala.worktime.models.DayType;
import fr.ralala.worktime.models.WorkTimeDay;
import fr.ralala.worktime.sql.SqlConstants;
import fr.ralala.worktime.sql.SqlFactory;

/**
 *******************************************************************************
 * <p><b>Project WorkTime</b><br/>
 * Public holidays factory functions
 * </p>
 * @author Keidan
 *
 *******************************************************************************
 */
public class PublicHolidaysFactory {
  private SqlFactory mSql = null;


  /**
   * Sets the reference to the SqlFactory.
   * @param sql The SQLite factory.
   */
  public void setSqlFactory(final SqlFactory sql) {
    mSql = sql;
  }

  /**
   * Returns the list of public holidays.
   * @return List<PublicHolidayEntry>
   */
  public List<PublicHolidayEntry> list() {
    if(mSql == null)
      return Collections.emptyList();
    return mSql.getPublicHolidays(-1, -1, -1);
  }

  /**
   * Returns the public holiday by name and date.
   * @param name The public holiday name.
   * @param date The public holiday date.
   * @return PublicHolidayEntry
   */
  public PublicHolidayEntry getByNameAndDate(String name, String date) {
    return mSql.getPublicHoliday(name, date);
  }

  /**
   * Tests id the current date is a public holiday.
   * @param refList The reference List.
   * @param currentDate The current date.
   * @return boolean
   */
  public boolean isPublicHolidays(List<PublicHolidayEntry> refList, WorkTimeDay currentDate) {
    for(PublicHolidayEntry de : refList) {
      if((de.getTypeMorning() == DayType.PUBLIC_HOLIDAY && de.getTypeAfternoon() == DayType.PUBLIC_HOLIDAY) && de.matchSimpleDate(currentDate))
        return true;
    }
    return false;
  }

  /**
   * Tests the validity of the input entry.
   * @param phe The netry to test.
   * @return boolean
   */
  public boolean testValidity(final PublicHolidayEntry phe) {
    WorkTimeDay de_day = phe.getDay();
    List<PublicHolidayEntry> publicHolidays = mSql.getPublicHolidays(-1, de_day.getMonth(), de_day.getDay());
    for(PublicHolidayEntry d : publicHolidays) {
      WorkTimeDay d_day = d.getDay();
      if(d.isRecurrence() || phe.isRecurrence()) {
        return false;
      } else if(d_day.getYear() == de_day.getYear())
        return false;
    }
    return true;
  }

  /**
   * Removes an entry.
   * @param phe The entry to delete.
   */
  public void remove(final PublicHolidayEntry phe) {
    mSql.removePublicHoliday(phe);
  }

  /**
   * Adds a new entry.
   * @param phe The entry to add.
   */
  public void add(final PublicHolidayEntry phe) {
    mSql.insertOrUpdatePublicHoliday(phe, phe.getID() != SqlConstants.INVALID_ID);
  }
}

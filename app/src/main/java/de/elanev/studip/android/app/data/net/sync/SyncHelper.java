/*
 * Copyright (c) 2016 ELAN e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package de.elanev.studip.android.app.data.net.sync;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.RemoteException;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import dagger.Lazy;
import de.elanev.studip.android.app.StudIPConstants;
import de.elanev.studip.android.app.data.datamodel.Course;
import de.elanev.studip.android.app.data.datamodel.Courses;
import de.elanev.studip.android.app.planner.data.entity.EventEntity;
import de.elanev.studip.android.app.planner.data.entity.Events;
import de.elanev.studip.android.app.data.datamodel.Institutes;
import de.elanev.studip.android.app.data.datamodel.InstitutesContainer;
import de.elanev.studip.android.app.data.datamodel.Recording;
import de.elanev.studip.android.app.data.datamodel.Routes;
import de.elanev.studip.android.app.data.datamodel.Semester;
import de.elanev.studip.android.app.data.datamodel.Semesters;
import de.elanev.studip.android.app.data.datamodel.Settings;
import de.elanev.studip.android.app.data.datamodel.UnizensusItem;
import de.elanev.studip.android.app.data.datamodel.User;
import de.elanev.studip.android.app.data.db.AbstractContract;
import de.elanev.studip.android.app.data.db.CoursesContract;
import de.elanev.studip.android.app.data.db.EventsContract;
import de.elanev.studip.android.app.data.db.InstitutesContract;
import de.elanev.studip.android.app.data.db.RecordingsContract;
import de.elanev.studip.android.app.data.db.SemestersContract;
import de.elanev.studip.android.app.data.db.UnizensusContract;
import de.elanev.studip.android.app.data.db.UsersContract;
import de.elanev.studip.android.app.data.net.services.CustomJsonConverterApiService;
import de.elanev.studip.android.app.data.net.services.StudIpLegacyApiService;
import de.elanev.studip.android.app.util.Prefs;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * A convenience class for interacting with the rest.IP endpoints.
 *
 * @author joern
 */
public class SyncHelper {
  public static final String TAG = SyncHelper.class.getSimpleName();
  private static ArrayList<ContentProviderOperation> mUserDbOp = new ArrayList<>();
  private Lazy<StudIpLegacyApiService> mApiService;
  private CompositeSubscription mCompositeSubscription = new CompositeSubscription();
  static Context mContext;
  Lazy<CustomJsonConverterApiService> mCustomJsonConverterApiService;

  public SyncHelper(Context context, Lazy<StudIpLegacyApiService> apiService,
      Lazy<CustomJsonConverterApiService> customJsonConverterApiService) {
    mContext = context;
    this.mApiService = apiService;
    this.mCustomJsonConverterApiService = customJsonConverterApiService;
  }

  /**
   * Resets the internal SyncHelper state
   */
  public void resetSyncHelper() {
    mUserDbOp.clear();
  }

  public void requestInstitutesForUserID(String userId, final SyncHelperCallbacks callbacks) {
    mCompositeSubscription.add(mApiService.get().getInstitutes(userId)
        .subscribeOn(Schedulers.newThread())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<InstitutesContainer>() {
          @Override public void onCompleted() {
            if (callbacks != null) {
              callbacks.onSyncFinished(SyncHelperCallbacks.FINISHED_INSTITUTES_SYNC);
            }
          }

          @Override public void onError(Throwable e) {
            if (e != null && e.getLocalizedMessage() != null) {
              Timber.e(e.getLocalizedMessage());

              if (callbacks != null) {
                callbacks.onSyncError(SyncHelperCallbacks.ERROR_INSTITUTES_SYNC,
                    e.getLocalizedMessage(), 0);
              }
            }
          }

          @Override public void onNext(InstitutesContainer institutesContainer) {
            try {
              mContext.getContentResolver()
                  .applyBatch(AbstractContract.CONTENT_AUTHORITY,
                      parseInstitutes(institutesContainer.getInstitutes()));
            } catch (RemoteException | OperationApplicationException e) {
              e.printStackTrace();
            }
          }
        }));


  }

  private static ArrayList<ContentProviderOperation> parseInstitutes(Institutes institutes) {
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
    for (Institutes.Institute i : institutes.getStudy()) {
      ContentProviderOperation.Builder instituteBuilder = ContentProviderOperation.newInsert(
          InstitutesContract.CONTENT_URI)
          .withValue(InstitutesContract.Columns.INSTITUTE_ID, i.getInstituteId())
          .withValue(InstitutesContract.Columns.INSTITUTE_NAME, i.getName())
          .withValue(InstitutesContract.Columns.INSTITUTE_PERMS, i.getPerms())
          .withValue(InstitutesContract.Columns.INSTITUTE_CONSULTATION, i.getConsultation())
          .withValue(InstitutesContract.Columns.INSTITUTE_ROOM, i.getRoom())
          .withValue(InstitutesContract.Columns.INSTITUTE_PHONE, i.getPhone())
          .withValue(InstitutesContract.Columns.INSTITUTE_FAX, i.getFax())
          .withValue(InstitutesContract.Columns.INSTITUTE_STREET, i.getStreet())
          .withValue(InstitutesContract.Columns.INSTITUTE_CITY, i.getCity())
          .withValue(InstitutesContract.Columns.INSTITUTE_FACULTY_NAME, i.getFacultyName())
          .withValue(InstitutesContract.Columns.INSTITUTE_FACULTY_STREET, i.getFacultyStreet())
          .withValue(InstitutesContract.Columns.INSTITUTE_FACULTY_CITY, i.getFacultyCity());
      ops.add(instituteBuilder.build());
    }

    for (Institutes.Institute i : institutes.getWork()) {
      ContentProviderOperation.Builder instituteBuilder = ContentProviderOperation.newInsert(
          InstitutesContract.CONTENT_URI)
          .withValue(InstitutesContract.Columns.INSTITUTE_ID, i.getInstituteId())
          .withValue(InstitutesContract.Columns.INSTITUTE_NAME, i.getName())
          .withValue(InstitutesContract.Columns.INSTITUTE_PERMS, i.getPerms())
          .withValue(InstitutesContract.Columns.INSTITUTE_CONSULTATION, i.getConsultation())
          .withValue(InstitutesContract.Columns.INSTITUTE_ROOM, i.getRoom())
          .withValue(InstitutesContract.Columns.INSTITUTE_PHONE, i.getPhone())
          .withValue(InstitutesContract.Columns.INSTITUTE_FAX, i.getFax())
          .withValue(InstitutesContract.Columns.INSTITUTE_STREET, i.getStreet())
          .withValue(InstitutesContract.Columns.INSTITUTE_CITY, i.getCity())
          .withValue(InstitutesContract.Columns.INSTITUTE_FACULTY_NAME, i.getFacultyName())
          .withValue(InstitutesContract.Columns.INSTITUTE_FACULTY_STREET, i.getFacultyStreet())
          .withValue(InstitutesContract.Columns.INSTITUTE_FACULTY_CITY, i.getFacultyCity());
      ops.add(instituteBuilder.build());
    }

    return ops;
  }

  /**
   * Requests new contacts and contact groups data from the API and refreshes the DB values
   *
   * @param callbacks SyncHelperCallbacks for calling back, can be null
   */
  public void performContactsSync(final SyncHelperCallbacks callbacks) {
    if (callbacks != null) {
      callbacks.onSyncFinished(SyncHelperCallbacks.FINISHED_CONTACTS_SYNC);
    }
  }

  /**
   * Requests new courses groups data from the API and refreshes the DB values
   *
   * @param callbacks SyncHelperCallbacks for calling back, can be null
   */
  public void performCoursesSync(final SyncHelperCallbacks callbacks) {
    //TODO Rxify the user insertion tasks

    Timber.i("SYNCING COURSES");
    mCompositeSubscription.add(mApiService.get().getCourses()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<Courses>() {
          @Override public void onCompleted() {
            Timber.i("FINISHED SYNCING COURSES");
            if (callbacks != null) {
              callbacks.onSyncFinished(SyncHelperCallbacks.FINISHED_COURSES_SYNC);
            }
          }

          @Override public void onError(Throwable e) {
            if (e != null && e.getLocalizedMessage() != null) {
              Timber.e(e.getLocalizedMessage());

              if (callbacks != null) {
                callbacks.onSyncError(SyncHelperCallbacks.ERROR_COURSES_SYNC,
                    e.getLocalizedMessage(), 0);
              }
            }
          }

          @Override public void onNext(Courses courses) {
            try {
              mContext.getContentResolver() //
                  .applyBatch(AbstractContract.CONTENT_AUTHORITY, parseCourses(courses));
            } catch (RemoteException | OperationApplicationException e) {
              e.printStackTrace();
            }

            int teacherRole = CoursesContract.USER_ROLE_TEACHER;
            int tutorRole = CoursesContract.USER_ROLE_TUTOR;
            int studentRole = CoursesContract.USER_ROLE_STUDENT;

            for (Course c : courses.courses) {
              new CourseUsersInsertTask(c.teachers).execute(c.courseId, teacherRole);
              new CourseUsersInsertTask(c.tutors).execute(c.courseId, tutorRole);
              new CourseUsersInsertTask(c.students).execute(c.courseId, studentRole);
              new UsersRequestTask().execute(c.teachers.toArray(new String[c.teachers.size()]));
            }
          }
        }));
  }

  private static ArrayList<ContentProviderOperation> parseCourses(Courses courses) {
    ArrayList<ContentProviderOperation> operations = new ArrayList<>();
    // First delete any existing courses
    operations.add(ContentProviderOperation.newDelete(CoursesContract.CONTENT_URI)
        .build());

    // Then add new course information
    for (Course c : courses.courses) {
      operations.addAll(parseCourse(c));
    }

    return operations;

  }


  private static ArrayList<ContentProviderOperation> parseCourse(Course course) {
    // Static recorings contract references
    Uri recordingsContentUrl = RecordingsContract.CONTENT_URI;
    String recordingId = RecordingsContract.Columns.Recordings.RECORDING_ID;
    String recordingAudioUrl = RecordingsContract.Columns.Recordings.RECORDING_AUDIO_DOWNLOAD;
    String recordingAuthor = RecordingsContract.Columns.Recordings.RECORDING_AUTHOR;
    String recordingCourseId = RecordingsContract.Columns.Recordings.RECORDING_COURSE_ID;
    String recordingDescription = RecordingsContract.Columns.Recordings.RECORDING_DESCRIPTION;
    String recordingDuration = RecordingsContract.Columns.Recordings.RECORDING_DURATION;
    String recordingPlayerUrl = RecordingsContract.Columns.Recordings.RECORDING_EXTERNAL_PLAYER_URL;
    String recordingPresentationUrl = RecordingsContract.Columns.Recordings.RECORDING_PRESENTATION_DOWNLOAD;
    String recordingPresenterDownload = RecordingsContract.Columns.Recordings.RECORDING_PRESENTER_DOWNLOAD;
    String recordingPreview = RecordingsContract.Columns.Recordings.RECORDING_PREVIEW;
    String recordingStart = RecordingsContract.Columns.Recordings.RECORDING_START;
    String recordingTitle = RecordingsContract.Columns.Recordings.RECORDING_TITLE;

    // Static unizensus contract references
    Uri unizensusContentUri = UnizensusContract.CONTENT_URI;
    String unizensusType = UnizensusContract.Columns.Unizensus.ZENSUS_TYPE;
    String unizensusUrl = UnizensusContract.Columns.Unizensus.ZENSUS_URL;
    String unizensusCourseId = UnizensusContract.Columns.Unizensus.ZENSUS_COURSE_ID;


    // DB Operations Array
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();

    // Parse the course data
    ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
        CoursesContract.CONTENT_URI)
        .withValue(CoursesContract.Columns.Courses.COURSE_ID, course.courseId)
        .withValue(CoursesContract.Columns.Courses.COURSE_TITLE, course.title)
        .withValue(CoursesContract.Columns.Courses.COURSE_DESCIPTION, course.description)
        .withValue(CoursesContract.Columns.Courses.COURSE_SUBTITLE, course.subtitle)
        .withValue(CoursesContract.Columns.Courses.COURSE_LOCATION, course.location)
        .withValue(CoursesContract.Columns.Courses.COURSE_DURATION_TIME, course.durationTime)
        .withValue(CoursesContract.Columns.Courses.COURSE_COLOR, course.color)
        .withValue(CoursesContract.Columns.Courses.COURSE_TYPE, course.type)
        .withValue(CoursesContract.Columns.Courses.COURSE_MODULES, course.modules.getAsJson())
        .withValue(CoursesContract.Columns.Courses.COURSE_START_TIME, course.startTime);

    if (course.durationTime == -1L) {
      builder.withValue(CoursesContract.Columns.Courses.COURSE_SEMESERT_ID,
          SemestersContract.UNLIMITED_COURSES_SEMESTER_ID);
    } else if (course.durationTime > 0L) {
      //TODO: Add these courses to the correct semester (c.start + duration between s.start, end)
      builder.withValue(CoursesContract.Columns.Courses.COURSE_SEMESERT_ID,
          SemestersContract.UNLIMITED_COURSES_SEMESTER_ID);
    } else {
      builder.withValue(CoursesContract.Columns.Courses.COURSE_SEMESERT_ID, course.semesterId);
    }
    ops.add(builder.build());

    if (course.getAdditionalData() != null) {


      // Parse the course recordings, if existing
      if (course.modules.recordings && course.getAdditionalData()
          .getRecordings() != null) {
        List<Recording> recordings = course.getAdditionalData()
            .getRecordings();
        for (Recording r : recordings) {
          builder = ContentProviderOperation.newInsert(recordingsContentUrl)
              .withValue(recordingId, r.getId())
              .withValue(recordingAudioUrl, r.getAudioDownload())
              .withValue(recordingAuthor, r.getAuthor())
              .withValue(recordingCourseId, course.courseId)
              .withValue(recordingDescription, r.getDescription())
              .withValue(recordingDuration, r.getDuration())
              .withValue(recordingPlayerUrl, r.getExternalPlayerUrl())
              .withValue(recordingPresentationUrl, r.getPresentationDownload())
              .withValue(recordingPresenterDownload, r.getPresenterDownload())
              .withValue(recordingPreview, r.getPreview())
              .withValue(recordingStart, r.getStart())
              .withValue(recordingTitle, r.getTitle());

          ops.add(builder.build());
        }
      }

      // Parse the course unizensus items, if existing
      if (course.modules.unizensus && course.getAdditionalData()
          .getUnizensusItem() != null) {
        UnizensusItem unizensusItem = course.getAdditionalData()
            .getUnizensusItem();

        builder = ContentProviderOperation.newInsert(unizensusContentUri)
            .withValue(unizensusType, unizensusItem.type)
            .withValue(unizensusUrl, unizensusItem.url)
            .withValue(unizensusCourseId, course.courseId);

        ops.add(builder.build());
      }
    }

    return ops;
  }

  /**
   * Requests news data for all courses from the API and refreshes the DB values
   *
   * @param callbacks SyncHelperCallbacks for calling back, can be null
   */
  public void performNewsSync(final SyncHelperCallbacks callbacks) {
    if (callbacks != null) {
      callbacks.onSyncFinished(SyncHelperCallbacks.FINISHED_NEWS_SYNC);
    }
  }

  /**
   * Requests all users from a specific course
   *
   * @param courseId  the ID of the course to request the users for
   * @param callbacks SyncHelperCallbacks for calling back, can be null
   */
  public void loadUsersForCourse(String courseId, SyncHelperCallbacks callbacks) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
        new UserLoadTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, courseId, callbacks);
      } else {
        new UserLoadTask().execute(courseId, callbacks);
      }
    } catch (RejectedExecutionException e) {
      // All thread are used, try again next time
    }

  }

  /**
   * Requests the users Semesters from the API and updates the DB values
   *
   * @param callbacks SyncHelperCallbacks for calling back, can be null
   */
  public void performSemestersSync(final SyncHelperCallbacks callbacks) {
    Timber.i("SYNCING SEMESTERS");
    mCompositeSubscription.add(mApiService.get().getSemesters()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<Semesters>() {
          @Override public void onCompleted() {
            if (callbacks != null)
              callbacks.onSyncFinished(SyncHelperCallbacks.FINISHED_SEMESTER_SYNC);
            Timber.i("FINISHED SYNCING SEMESTERS");
          }

          @Override public void onError(Throwable e) {
            if (e != null && e.getLocalizedMessage() != null) {
              Timber.e(e.getLocalizedMessage());

              if (callbacks != null) {
                callbacks.onSyncError(SyncHelperCallbacks.ERROR_SEMESTER_SYNC,
                    e.getLocalizedMessage(), 0);
              }
            }
          }

          @Override public void onNext(Semesters semesters) {
            try {
              mContext.getContentResolver()
                  .applyBatch(AbstractContract.CONTENT_AUTHORITY, parseSemesters(semesters));
            } catch (RemoteException | OperationApplicationException e) {
              e.printStackTrace();
            }
          }
        }));
  }

  private static ArrayList<ContentProviderOperation> parseSemesters(Semesters semesterList) {
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
    long currentTime = System.currentTimeMillis();

    for (Semester semester : semesterList.semesters) {
      long semesterBegin = semester.begin * 1000L;
      long semesterEnd = semester.end * 1000L;
      if (currentTime > semesterBegin && currentTime < semesterEnd) {
        Prefs.getInstance(mContext)
            .setCurrentSemesterId(semester.semester_id);
      }
      ContentProviderOperation.Builder semesterBuilder = ContentProviderOperation.newInsert(
          SemestersContract.CONTENT_URI)
          .withValue(SemestersContract.Columns.SEMESTER_ID, semester.semester_id)
          .withValue(SemestersContract.Columns.SEMESTER_TITLE, semester.title)
          .withValue(SemestersContract.Columns.SEMESTER_DESCRIPTION, semester.description)
          .withValue(SemestersContract.Columns.SEMESTER_BEGIN, semester.begin)
          .withValue(SemestersContract.Columns.SEMESTER_END, semester.end)
          .withValue(SemestersContract.Columns.SEMESTER_SEMINARS_BEGIN, semester.seminars_begin)
          .withValue(SemestersContract.Columns.SEMESTER_SEMINARS_END, semester.seminars_end);
      ops.add(semesterBuilder.build());
    }

    return ops;
  }

  /**
   * Requests a specific user from the API if no in DB
   *
   * @param userId    the ID of the user to request from the API
   * @param callbacks SyncHelperCallbacks for calling back, can be null
   */
  public void requestUser(String userId, final SyncHelperCallbacks callbacks) {

    if (!TextUtils.equals("", userId) && !TextUtils.equals(StudIPConstants.STUDIP_SYSTEM_USER_ID,
        userId)) {
      if (callbacks != null) callbacks.onSyncStarted();
      mCompositeSubscription.add(mApiService.get().getUser(userId)
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(new Subscriber<User>() {
            @Override public void onCompleted() {
              if (callbacks != null)
                callbacks.onSyncFinished(SyncHelperCallbacks.FINISHED_USER_SYNC);
            }

            @Override public void onError(Throwable e) {
              if (e != null && e.getLocalizedMessage() != null) {
                Timber.e(e.getLocalizedMessage());

                if (callbacks != null) {
                  callbacks.onSyncError(SyncHelperCallbacks.ERROR_USER_SYNC, e.getLocalizedMessage(),
                      0);
                }
              }
            }

            @Override public void onNext(User user) {
              if (user != null && !TextUtils.equals("____%system%____", user.userId)) {
                mUserDbOp.add(parseUser(user));
                try {
                  mContext.getContentResolver()
                      .applyBatch(AbstractContract.CONTENT_AUTHORITY, mUserDbOp);
                } catch (RemoteException | OperationApplicationException e) {
                  e.printStackTrace();
                }
                mUserDbOp.clear();
              }
            }
          }));
    }
  }


  private static ContentProviderOperation parseUser(User user) {
    ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
        UsersContract.CONTENT_URI);
    builder.withValue(UsersContract.Columns.USER_ID, user.userId);
    builder.withValue(UsersContract.Columns.USER_USERNAME, user.username);
    builder.withValue(UsersContract.Columns.USER_PERMS, user.perms);
    builder.withValue(UsersContract.Columns.USER_TITLE_PRE, user.titlePre);
    builder.withValue(UsersContract.Columns.USER_FORENAME, user.forename);
    builder.withValue(UsersContract.Columns.USER_LASTNAME, user.lastname);
    builder.withValue(UsersContract.Columns.USER_TITLE_POST, user.titlePost);
    builder.withValue(UsersContract.Columns.USER_EMAIL, user.email);
    builder.withValue(UsersContract.Columns.USER_AVATAR_SMALL, user.avatarSmall);
    builder.withValue(UsersContract.Columns.USER_AVATAR_MEDIUM, user.avatarMedium);
    builder.withValue(UsersContract.Columns.USER_AVATAR_NORMAL, user.avatarNormal);
    builder.withValue(UsersContract.Columns.USER_PHONE, user.phone);
    builder.withValue(UsersContract.Columns.USER_HOMEPAGE, user.homepage);
    builder.withValue(UsersContract.Columns.USER_PRIVADR, user.privadr);
    builder.withValue(UsersContract.Columns.USER_SKYPE_NAME, user.skype);

    return builder.build();
  }

  public void requestApiRoutes(final SyncHelperCallbacks callbacks) {
    mCompositeSubscription.add(mCustomJsonConverterApiService.get().discoverApi()
        .subscribeOn(Schedulers.newThread())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<Routes>() {
          @Override public void onCompleted() {
            if (callbacks != null) {
              callbacks.onSyncFinished(SyncHelperCallbacks.FINISHED_ROUTES_SYNC);
            }
          }

          @Override public void onError(Throwable e) {
            if (e != null && e.getLocalizedMessage() != null) {
              Timber.e(e, e.getLocalizedMessage());

              if (callbacks != null) {
                callbacks.onSyncError(SyncHelperCallbacks.ERROR_ROUTES_SYNC, e.getLocalizedMessage(),
                    0);
              }
            }
          }

          @Override public void onNext(Routes routes) {
            Prefs.getInstance(mContext)
                .setForumIsActivated(routes.isForumActivated);
          }
        }));
  }

  public void getSettings() {

    mCompositeSubscription.add(mApiService.get().getSettings()
        .subscribeOn(Schedulers.newThread())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<Settings>() {
          @Override public void onCompleted() {
          }

          @Override public void onError(Throwable e) {
            if (e != null && e.getLocalizedMessage() != null) {
              Timber.e(e, e.getLocalizedMessage());
            }
          }

          @Override public void onNext(Settings settings) {
            String serialized = settings.toJson();
            Timber.d("Storing following settings in the Prefs:\n" + serialized);
            Prefs.getInstance(mContext)
                .setApiSettings(serialized);
          }
        }));
  }

  public void requestCurrentUserInfo(final SyncHelperCallbacks callbacks) {

    mCompositeSubscription.add(mApiService.get().getCurrentUserInfo()
        .subscribeOn(Schedulers.newThread())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<User>() {
          @Override public void onCompleted() {
            if (callbacks != null) {
              callbacks.onSyncFinished(SyncHelperCallbacks.FINISHED_USER_SYNC);
            }
          }

          @Override public void onError(Throwable e) {
            if (e != null && e.getLocalizedMessage() != null) {
              String errMsg = e.getLocalizedMessage();
              Timber.e(e, errMsg);

              if (callbacks != null) {
                callbacks.onSyncError(SyncHelperCallbacks.ERROR_USER_SYNC, errMsg, 0);
              }
            }
          }

          @Override public void onNext(User user) {
            Prefs.getInstance(mContext)
                .setUserInfo(User.toJson(user));
          }
        }));
  }

  /**
   * Callback interface for clients to interact with the SyncHelper
   */
  public interface SyncHelperCallbacks {
    int STARTED_COURSES_SYNC = 101;
    int STARTED_NEWS_SYNC = 102;
    int STARTED_SEMESTER_SYNC = 103;
    int STARTED_CONTACTS_SYNC = 104;
    int STARTED_MESSAGES_SYNC = 105;
    int STARTED_USER_SYNC = 106;
    int STARTED_INSTITUTES_SYNC = 107;
    int FINISHED_COURSES_SYNC = 201;
    int FINISHED_NEWS_SYNC = 202;
    int FINISHED_SEMESTER_SYNC = 203;
    int FINISHED_CONTACTS_SYNC = 204;
    int FINISHED_USER_SYNC = 206;
    int FINISHED_INSTITUTES_SYNC = 207;
    int FINISHED_ROUTES_SYNC = 209;
    int ERROR_COURSES_SYNC = 301;
    int ERROR_NEWS_SYNC = 302;
    int ERROR_SEMESTER_SYNC = 303;
    int ERROR_CONTACTS_SYNC = 304;
    int ERROR_MESSAGES_SYNC = 305;
    int ERROR_USER_SYNC = 306;
    int ERROR_INSTITUTES_SYNC = 307;
    int ERROR_ROUTES_SYNC = 309;

    void onSyncStarted();

    void onSyncStateChange(int status);

    void onSyncFinished(int status);

    void onSyncError(int status, String errorMsg, int errorCode);

  }

  private class UserLoadTask extends AsyncTask<Object, Void, Void> {

    @Override protected Void doInBackground(Object... params) {

      String courseId = (String) params[0];
      SyncHelperCallbacks callbacks = (SyncHelperCallbacks) params[1];
      Uri courseUserUri = CoursesContract.CONTENT_URI.buildUpon()
          .appendPath("userids")
          .appendPath(courseId)
          .build();

      Cursor c = mContext.getContentResolver()
          .query(courseUserUri,
              new String[]{CoursesContract.Columns.CourseUsers.COURSE_USER_USER_ID}, null, null,
              null);
      if (c != null) {
        try {
          c.moveToFirst();
          while (!c.isAfterLast()) {
            String userId = c.getString(c.getColumnIndex(CoursesContract.
                Columns.
                CourseUsers.
                COURSE_USER_USER_ID));

            if (c.isLast() || c.isFirst()) {
              requestUser(userId, callbacks);
            } else {
              requestUser(userId, null);
            }
            c.moveToNext();
          }
        } finally {
          c.close();
        }
      }
      return null;
    }
  }

  private class UsersRequestTask extends AsyncTask<String, Void, Void> {

    @Override protected Void doInBackground(String... params) {
      for (String userId : params) {
        requestUser(userId, null);
      }
      return null;
    }
  }

  private class CourseUsersInsertTask extends AsyncTask<Object, Void, Void> {
    ArrayList<String> mUserIds;
    String mCourseIdCol;
    String mUserIdCol;
    String mUserRoleCol;

    public CourseUsersInsertTask(ArrayList<String> ids) {
      this.mUserIds = ids;
      mCourseIdCol = CoursesContract.Columns.CourseUsers.COURSE_USER_COURSE_ID;
      mUserIdCol = CoursesContract.Columns.CourseUsers.COURSE_USER_USER_ID;
      mUserRoleCol = CoursesContract.Columns.CourseUsers.COURSE_USER_USER_ROLE;
    }

    @Override protected Void doInBackground(Object... params) {

      String courseId = (String) params[0];
      int role = (Integer) params[1];
      int userIdListLen = mUserIds.size();
      ContentValues[] values = new ContentValues[userIdListLen];
      String[] userIdsArray = mUserIds.toArray(new String[userIdListLen]);

      String courseIdCol = mCourseIdCol;
      String userIdCol = mUserIdCol;
      String userRoleCol = mUserRoleCol;

      for (int i = 0; i < userIdListLen; ++i) {
        ContentValues cv = new ContentValues();
        cv.put(courseIdCol, courseId);
        cv.put(userIdCol, userIdsArray[i]);
        cv.put(userRoleCol, role);
        values[i] = cv;
      }

      mContext.getContentResolver() //
          .bulkInsert(CoursesContract.COURSES_USERS_CONTENT_URI, values);

      return null;
    }
  }
}
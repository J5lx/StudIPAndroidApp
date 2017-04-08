/*
 * Copyright (c) 2016 ELAN e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package de.elanev.studip.android.app.courses.domain;

import javax.inject.Inject;

import de.elanev.studip.android.app.base.UseCase;
import de.elanev.studip.android.app.base.domain.executor.PostExecutionThread;
import de.elanev.studip.android.app.base.domain.executor.ThreadExecutor;
import de.elanev.studip.android.app.news.domain.GetNewsList;
import de.elanev.studip.android.app.news.domain.NewsItem;
import de.elanev.studip.android.app.planner.domain.Event;
import de.elanev.studip.android.app.user.domain.UserRepository;
import rx.Observable;

/**
 * @author joern
 */
public class GetCourseOverview extends UseCase {
  private final String id;
  private final CoursesRepository repository;
  private final UserRepository userRepository;
  private final GetNewsList getNewsList;

  @Inject public GetCourseOverview(String id, GetNewsList getNewsList,
      CoursesRepository coursesRepository, ThreadExecutor threadExecutor,
      PostExecutionThread postExecutionThread, UserRepository userRepository) {
    super(threadExecutor, postExecutionThread);

    this.id = id;
    this.repository = coursesRepository;
    this.getNewsList = getNewsList;
    this.userRepository = userRepository;
  }

  @Override protected Observable buildUseCaseObservable(boolean forceUpdate) {
    Observable<DomainCourse> courseObs = repository.course(id, forceUpdate)
        .flatMap(course -> userRepository.getUsers(course.getTeachers())
            .flatMap(users -> {
              course.setTeacherEntities(users);
              return Observable.defer(() -> Observable.just(course));
            }));

    Observable<NewsItem> newsObs = getNewsList.get(forceUpdate)
        .flatMap(newsItems -> Observable.from(newsItems)
            .filter(newsItem -> newsItem.getRange()
                .equals(id)))
        .toSortedList((newsItem, newsItem2) -> newsItem2.getDate()
            .compareTo(newsItem.getDate()))
        .flatMap(Observable::from)
        .firstOrDefault(null);

    Observable<Event> eventsObs = repository.courseEvents(id, forceUpdate)
        .flatMap(Observable::from)
        .filter(event -> event.getStart() * 1000L >= System.currentTimeMillis())
        .firstOrDefault(null);

    return Observable.zip(courseObs, eventsObs, newsObs, (course, events, news) -> {
      CourseOverview courseOverview = new CourseOverview();
      courseOverview.setCourse(course);
      courseOverview.setEvent(events);
      courseOverview.setNewsItem(news);

      return courseOverview;
    });
  }
}

/*
 * Copyright (c) 2016 ELAN e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package de.elanev.studip.android.app.news.presentation.mapper;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import de.elanev.studip.android.app.base.internal.di.PerActivity;
import de.elanev.studip.android.app.news.domain.NewsItem;
import de.elanev.studip.android.app.news.presentation.model.NewsModel;
import de.elanev.studip.android.app.user.presentation.mapper.UserModelDataMapper;

/**
 * @author joern
 */
@PerActivity
public class NewsModelDataMapper {


  private final UserModelDataMapper userModelDataMapper;

  @Inject NewsModelDataMapper(UserModelDataMapper userModelDataMapper) {
    this.userModelDataMapper = userModelDataMapper;
  }

  public List<NewsModel> transformNewsList(List<NewsItem> news) {
    if (news == null) {
      throw new IllegalArgumentException("NewsItem List must not be null");
    }

    ArrayList<NewsModel> items = new ArrayList<>();

    for (NewsItem item : news) {
      items.add(transformNewsItem(item));
    }

    return items;
  }

  public NewsModel transformNewsItem(NewsItem newsItem) {
    if (newsItem == null) {
      throw new IllegalArgumentException("NewsItem must not be null");
    }

    NewsModel newsModel = new NewsModel();
    newsModel.author = userModelDataMapper.transform(newsItem.getAuthor());
    newsModel.title = newsItem.getTitle();
    newsModel.body = newsItem.getBody();
    newsModel.date = newsItem.getDate();
    newsModel.id = newsItem.getNewsId();
    newsModel.range = newsItem.getRange();
    newsModel.course = newsItem.getCourse();

    return newsModel;
  }

}

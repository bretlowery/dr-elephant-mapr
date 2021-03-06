/*
 * Copyright 2016 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package controllers;

import com.avaje.ebean.Query;
import models.AppResult;
import com.bretlowery.drelephant.aggregates.UserSeverityAggregate;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import play.api.mvc.Content;
import play.test.FakeApplication;
import play.test.Helpers;
import views.html.page.homePage;
import views.html.results.searchResults;
import views.html.results.userSeverityAggregateResults;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ApplicationTest {
  private static final long DAY = 24 * 60 * 60 * 1000;

  @Test
  public void testRenderHomePage() {
    String rightNow = new SimpleDateFormat("yyyy.MM.dd HH.mm.ss").format(new Date());
    Long now = System.currentTimeMillis();
    Long sevenDaysAgo = now - (7 * DAY);
    List<UserSeverityAggregate> usaResults = Application.getUserSeverityAggregate(sevenDaysAgo, now, 3, 5, "!TEST!", 5, null, null);
    String goodTitle = "Last 15 Days, OK/Low/Moderate Status (Max 50 Jobs)";
    String badTitle = "Last 15 Days, Severe/Critical Status (Max 50 Jobs)";
    String usaTitle = "Top Unique Offenders in Last 72Hr";
    Content html = homePage.render(5, 2, 3, 0, rightNow, 0,0,0,0,0,0,0,0,0,0,0,0,
            searchResults.render(goodTitle, null),
            searchResults.render(badTitle, null),
            userSeverityAggregateResults.render(usaTitle, usaResults)
    );
    assertEquals("text/html", html.contentType());
    assertTrue(html.body().contains("<b>5</b> jobs ran on this cluster in the last 72h"));
    assertTrue(html.body().contains("<b>2</b> of them could use some tuning"));
    assertTrue(html.body().contains("<b>3</b> of them threw critical errors and need attention"));
    assertTrue(html.body().contains("No exceptions occurred."));
  }


  public static FakeApplication app;

  @BeforeClass
  public static void startApp() {
    app = Helpers.fakeApplication(Helpers.inMemoryDatabase());
    Helpers.start(app);
  }

  @AfterClass
  public static void stopApp() {
    Helpers.stop(app);
  }

  @Test
  public void testGenerateSearchQuery() {

    Map<String, String> searchParams = new HashMap<String, String>();

    // Null searchParams Check
    Query<AppResult> query1 = Application.generateSearchQuery("*", null);
    assertNotNull(query1.findList());
    String sql1 = query1.getGeneratedSql();
    assertTrue(sql1.contains("select t0.id c0"));
    assertTrue(sql1.contains("from yarn_app_result t0 order by t0.finish_time desc"));

    // No searchParams Check
    Query<AppResult> query2 = Application.generateSearchQuery("*", searchParams);
    assertNotNull(query2.findList());
    String sql2 = query2.getGeneratedSql();
    assertTrue(sql2.contains("select t0.id c0"));
    assertTrue(sql2.contains("from yarn_app_result t0 order by t0.finish_time desc"));

    // Query by username
    searchParams.put(Application.USERNAME, "username");
    query2 = Application.generateSearchQuery("*", searchParams);
    assertNotNull(query2.findList());
    sql2 = query2.getGeneratedSql();
    assertTrue(sql2.contains("select t0.id c0"));
    assertTrue(sql2.contains("from yarn_app_result t0 where"));
    assertTrue(sql2.contains("t0.username = ?  order by t0.finish_time desc"));

    // Query by queuename
    searchParams.put(Application.QUEUE_NAME, "queueName");
    query2 = Application.generateSearchQuery("*", searchParams);
    assertNotNull(query2.findList());
    sql2 = query2.getGeneratedSql();
    assertTrue(sql2.contains("select t0.id c0"));
    assertTrue(sql2.contains("from yarn_app_result t0 where"));
    assertTrue(sql2.contains("t0.queue_name = ?  order by t0.finish_time desc"));


      // Query by jobtype
    searchParams.put(Application.JOB_TYPE, "Pig");
    query2 = Application.generateSearchQuery("*", searchParams);
    assertNotNull(query2.findList());
    sql2 = query2.getGeneratedSql();
    assertTrue(sql2.contains("select t0.id c0"));
    assertTrue(sql2.contains("from yarn_app_result t0 where"));
    assertTrue(sql2.contains("t0.username = ?"));
    assertTrue(sql2.contains("t0.job_type = ?"));
    assertTrue(sql2.contains("order by t0.finish_time desc"));

    // Query by username, jobtype and start time
    searchParams.put(Application.STARTED_TIME_BEGIN, "1459713751000");
    searchParams.put(Application.STARTED_TIME_END, "1459713751000");
    Query<AppResult> query3 = Application.generateSearchQuery("*", searchParams);
    assertNotNull(query3.findList());
    String sql3 = query3.getGeneratedSql();
    assertTrue(sql3.contains("select t0.id c0"));
    assertTrue(sql3.contains("from yarn_app_result t0 where"));
    assertTrue(sql3.contains("t0.username = ?"));
    assertTrue(sql3.contains("t0.start_time >= ?"));
    assertTrue(sql3.contains("t0.start_time <= ?"));
    assertTrue(sql3.contains("order by t0.start_time desc"));

    // Query by finish time
    searchParams = new HashMap<String, String>();
    searchParams.put(Application.FINISHED_TIME_BEGIN, "1459713751000");
    searchParams.put(Application.FINISHED_TIME_END, "1459713751000");
    Query<AppResult> query4 = Application.generateSearchQuery("*", searchParams);
    assertNotNull(query4.findList());
    String sql4 = query4.getGeneratedSql();
    assertTrue(sql4.contains("select t0.id c0"));
    assertTrue(sql4.contains("from yarn_app_result t0 where"));
    assertTrue(sql4.contains("t0.finish_time >= ?"));
    assertTrue(sql4.contains("t0.finish_time <= ?"));
    assertTrue(sql4.contains("order by t0.finish_time desc"));
  }
}

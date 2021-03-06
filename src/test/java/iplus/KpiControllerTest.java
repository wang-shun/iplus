/*
 * $Id$
 *
 * Copyright (c) 2015 Sogou.com. All Rights Reserved.
 */
package iplus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.sogou.iplus.api.KpiController;
import com.sogou.iplus.api.KpiController.AVERAGE;
import com.sogou.iplus.api.KpiController.HOST;
import com.sogou.iplus.config.DaoConfig;
import com.sogou.iplus.config.RootConfig;
import com.sogou.iplus.entity.Company;
import com.sogou.iplus.entity.Project;
import com.sogou.iplus.manager.PushManager;
import com.sogou.iplus.model.ApiResult;

import commons.spring.RedisRememberMeService.User;
import commons.spring.RedisRememberMeService.UserPerm;

//--------------------- Change Logs----------------------
//@author wangwenlong Initial Created at 2016年10月14日;
//-------------------------------------------------------
@ContextConfiguration(classes = { RootConfig.class, DaoConfig.class })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class KpiControllerTest {

  @Mock
  private HttpServletRequest request;

  private final int testId = 260, debugId = 0;

  private final Project testProject = Project.getProjectWithXmId(testId),
      debugProject = Project.getProjectWithXmId(debugId);

  private final String testKey = testProject.getXmKey(), debugKey = debugProject.getXmKey();

  private final List<Integer> kpiIds = testProject.getKpis().stream().map(kpi -> kpi.getKpiId())
      .collect(Collectors.toList());

  private LocalDate today = LocalDate.now(), yesterday = today.minusDays(1), tomorrow = today.plusDays(1);

  private User testUser = new User(210679, "xiaop_liteng", "liteng", -1, Arrays.asList(new UserPerm("10055", 123)));

  @Autowired
  private KpiController controller;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    testProject.getKpis().forEach(
        kpi -> Mockito.when(request.getParameter(kpi.getKpiId().toString())).thenReturn(kpi.getKpiId().toString()));
  }

  @Test
  public void test() {
    Assert.assertNotNull(controller);
    getCompany();
    add();
    update();
    selectNull();
    selectKpisWithDateAndXmId();
    selectKpisWithDateRangeAndKpiId();
    push();
    getAverage();
    System.out.println(getRandomString("0123456789abcdefghijklmnopqrstuvwxyz".toCharArray(), 16));
    System.out.println(getDailyActiveUserKpiIds());
    System.out.println(getNewUserKpiIds());
    System.out.println(getRetentionRateKpiIds());
  }

  private void getAverage() {
    System.out.println(controller
        .getAverage(testId, testKey, Optional.of(Arrays.asList("21+22", "23+24", "25")), today, AVERAGE.day).getData());
  }

  public void getCompany() {
    ApiResult<?> result = controller.getCompany(testUser);
    Assert.assertTrue(ApiResult.isOk(result));
    Company sogou = (Company) result.getData();
    Assert.assertNotNull(sogou);
    System.out.println(sogou);
    Assert.assertFalse(CollectionUtils.isEmpty(sogou.getBusinessUnits()));
  }

  public void add() {
    Assert.assertTrue(ApiResult.isOk(controller.add(Optional.empty())));
  }

  public void update() {
    Assert.assertTrue(ApiResult.isOk(controller.update(request, testId, testKey, yesterday)));
  }

  public void selectNull() {
    ApiResult<?> result = controller.selectProjectsDoNotSubmitKpiOnNamedDate(Optional.empty());
    Assert.assertTrue(ApiResult.isOk(result));
    List<?> list = (List<?>) result.getData();
    Assert.assertTrue(list.stream().map(o -> (Project) o).noneMatch(p -> Objects.equals(testId, p.getXmId())));
  }

  public void selectKpisWithDateAndXmId() {
    validateResultOfSelectKpisWithDateAndXmId(selectKpisWithDateAndXmId(testId, testUser));
    validateResultOfSelectKpisWithDateAndXmId(selectKpisWithDateAndXmId(testId, testKey));
    validateResultOfSelectKpisWithDateAndXmId(selectKpisWithDateAndXmId(debugId, debugKey));
  }

  void validateResultOfSelectKpisWithDateAndXmId(ApiResult<?> result) {
    Assert.assertTrue(ApiResult.isOk(result));
    Map<?, ?> map = (Map<?, ?>) result.getData();
    Assert.assertFalse(MapUtils.isEmpty(map));
    System.out.println(map);
    Assert.assertTrue(map.entrySet().stream().filter(e -> kpiIds.contains(e.getKey()))
        .allMatch(e -> Objects.equals((Integer) e.getKey(), ((BigDecimal) e.getValue()).intValue())));
  }

  private ApiResult<?> selectKpisWithDateAndXmId(int xmId, String xmKey) {
    return controller.selectKpisWithDateAndXmId(HOST.privateWeb.getValue(), null, Optional.of(xmId), Optional.of(xmKey),
        today);
  }

  private ApiResult<?> selectKpisWithDateAndXmId(int xmId, User user) {
    return controller.selectKpisWithDateAndXmId(HOST.publicWeb.getValue(), user, Optional.of(xmId), Optional.empty(),
        today);
  }

  public void selectKpisWithDateRangeAndKpiId() {
    validateResultOfSelectKpisWithDateRangeAndKpiId(
        selectKpisWithDateRangeAndKpiId(HOST.privateWeb, null, testId, testKey));
    validateResultOfSelectKpisWithDateRangeAndKpiId(
        selectKpisWithDateRangeAndKpiId(HOST.publicWeb, testUser, null, null));
  }

  private ApiResult<?> selectKpisWithDateRangeAndKpiId(HOST host, User user, Integer xmId, String xmKey) {
    return controller.selectKpisWithDateRangeAndKpiId(host.getValue(), user, Optional.ofNullable(xmId),
        Optional.ofNullable(xmKey), kpiIds, today, tomorrow);
  }

  private void validateResultOfSelectKpisWithDateRangeAndKpiId(ApiResult<?> result) {
    Assert.assertTrue(ApiResult.isOk(result));
    Map<?, ?> map = (Map<?, ?>) result.getData();
    Assert.assertTrue(MapUtils.isNotEmpty(map) && map.entrySet().stream().allMatch(e -> kpiIds.contains(e.getKey())));
    System.out.println(map);
  }

  @Autowired
  PushManager pushManager;

  public void push() {
    controller.pushPandoraMessage(Optional.empty());
  }

  public String getRandomString(char[] chars, int len) {
    Random random = new Random();
    char[] result = new char[len];
    for (int i = 0; i < len; result[i++] = chars[random.nextInt(chars.length)]);
    return new String(result);
  }

  public List<Integer> getDailyActiveUserKpiIds() {
    return Project.getProjects().stream().flatMap(p -> p.getKpis().stream())
        .filter(kpi -> Arrays.asList("日活", "活跃").stream().anyMatch(regex -> -1 != kpi.getKpiName().indexOf(regex)))
        .map(kpi -> kpi.getKpiId()).collect(Collectors.toList());
  }

  public List<Integer> getNewUserKpiIds() {
    return Project.getProjects().stream().flatMap(p -> p.getKpis().stream())
        .filter(kpi -> -1 != kpi.getKpiName().indexOf("激活")).map(kpi -> kpi.getKpiId()).collect(Collectors.toList());
  }

  public Map<Integer, List<Integer>> getRetentionRateKpiIds() {
    Map<Integer, List<Integer>> result = new HashMap<>();

    result.put(1,
        Project.getProjects().stream().flatMap(p -> p.getKpis().stream())
            .filter(kpi -> Arrays.asList("留存", "次").stream().allMatch(regex -> -1 != kpi.getKpiName().indexOf(regex)))
            .map(kpi -> kpi.getKpiId()).collect(Collectors.toList()));
    result.put(7,
        Project.getProjects().stream().flatMap(p -> p.getKpis().stream())
            .filter(kpi -> Arrays.asList("留存", "7").stream().allMatch(regex -> -1 != kpi.getKpiName().indexOf(regex)))
            .map(kpi -> kpi.getKpiId()).collect(Collectors.toList()));
    result.put(30,
        Project.getProjects().stream().flatMap(p -> p.getKpis().stream())
            .filter(kpi -> Arrays.asList("留存", "30").stream().allMatch(regex -> -1 != kpi.getKpiName().indexOf(regex)))
            .map(kpi -> kpi.getKpiId()).collect(Collectors.toList()));

    return result;
  }
}

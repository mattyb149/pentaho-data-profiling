/*!
 * PENTAHO CORPORATION PROPRIETARY AND CONFIDENTIAL
 *
 * Copyright 2002 - 2015 Pentaho Corporation (Pentaho). All rights reserved.
 *
 * NOTICE: All information including source code contained herein is, and
 * remains the sole property of Pentaho and its licensors. The intellectual
 * and technical concepts contained herein are proprietary and confidential
 * to, and are trade secrets of Pentaho and may be covered by U.S. and foreign
 * patents, or patents in process, and are protected by trade secret and
 * copyright laws. The receipt or possession of this source code and/or related
 * information does not convey or imply any rights to reproduce, disclose or
 * distribute its contents, or to manufacture, use, or sell anything that it
 * may describe, in whole or in part. Any reproduction, modification, distribution,
 * or public display of this information without the express written authorization
 * from Pentaho is strictly prohibited and in violation of applicable laws and
 * international treaties. Access to the source code contained herein is strictly
 * prohibited to anyone except those individuals and entities who have executed
 * confidentiality and non-disclosure agreements or other agreements with Pentaho,
 * explicitly covering such access.
 */

package com.pentaho.profiling.services;

import com.pentaho.model.metrics.contributor.metricManager.impl.CardinalityMetricContributor;
import com.pentaho.profiling.api.metrics.MetricContributor;
import com.pentaho.profiling.api.metrics.MetricContributorService;
import com.pentaho.profiling.api.metrics.MetricContributors;
import com.pentaho.profiling.api.metrics.MetricManagerContributor;
import com.pentaho.profiling.api.metrics.mapper.MetricContributorsObjectMapperFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by bryan on 3/18/15.
 */
public class MetricContributorServiceImplTest {
  private MetricContributorService delegate;
  private MetricContributorsObjectMapperFactory metricContributorsObjectMapperFactory;
  private MetricContributorServiceImpl metricContributorService;

  @Before
  public void setup() {
    delegate = mock( MetricContributorService.class );
    metricContributorsObjectMapperFactory = mock( MetricContributorsObjectMapperFactory.class );
    metricContributorService = new MetricContributorServiceImpl( delegate, metricContributorsObjectMapperFactory );
  }

  @Test
  public void testGetDefault() throws IOException {
    HttpServletResponse httpServletResponse = mock( HttpServletResponse.class );
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    when( httpServletResponse.getOutputStream() ).thenReturn( new ServletOutputStream() {
      @Override public void write( int b ) throws IOException {
        byteArrayOutputStream.write( b );
      }

      @Override public void flush() throws IOException {
        super.flush();
        byteArrayOutputStream.flush();
      }

      @Override public void close() throws IOException {
        super.close();
        byteArrayOutputStream.close();
      }
    } );
    CardinalityMetricContributor cardinalityMetricContributor = new CardinalityMetricContributor();
    cardinalityMetricContributor.setNormalPrecision( 150 );
    cardinalityMetricContributor.setSparsePrecision( 250 );
    MetricContributors metricContributors = new MetricContributors( new ArrayList<MetricContributor>(), new ArrayList
      <MetricManagerContributor>( Arrays.asList( cardinalityMetricContributor ) ) );
    when( delegate.getDefaultMetricContributors() ).thenReturn( metricContributors );
    metricContributorService.getDefaultMetricContributorsWs( httpServletResponse );
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enableDefaultTyping( ObjectMapper.DefaultTyping.NON_FINAL );
    assertEquals( metricContributors,
      objectMapper.readValue( byteArrayOutputStream.toByteArray(), MetricContributors.class ) );
  }

  @Test
  public void testSetDefault() throws IOException {
    MetricContributors metricContributors = mock( MetricContributors.class );
    HttpServletRequest httpServletRequest = mock( HttpServletRequest.class );
    ServletInputStream inputStream = mock( ServletInputStream.class );
    when( httpServletRequest.getInputStream() ).thenReturn( inputStream );
    ObjectMapper objectMapper = mock( ObjectMapper.class );
    when( metricContributorsObjectMapperFactory.createObjectMapper() ).thenReturn( objectMapper );
    when( objectMapper.readValue( inputStream, MetricContributors.class ) ).thenReturn( metricContributors );
    metricContributorService.setDefaultMetricContributors( httpServletRequest );
    verify( delegate ).setDefaultMetricContributors( metricContributors );
  }
}
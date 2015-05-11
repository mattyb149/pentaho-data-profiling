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

import com.pentaho.profiling.api.Profile;
import com.pentaho.profiling.api.ProfileCreationException;
import com.pentaho.profiling.api.ProfileFactory;
import com.pentaho.profiling.api.ProfileStatus;
import com.pentaho.profiling.api.ProfileStatusManager;
import com.pentaho.profiling.api.ProfileStatusReadOperation;
import com.pentaho.profiling.api.ProfileStatusReader;
import com.pentaho.profiling.api.ProfilingService;
import com.pentaho.profiling.api.configuration.DataSourceMetadata;
import com.pentaho.profiling.api.configuration.ProfileConfiguration;
import com.pentaho.profiling.api.doc.rest.ErrorCode;
import com.pentaho.profiling.api.doc.rest.Example;
import com.pentaho.profiling.api.doc.rest.SuccessResponseCode;
import com.pentaho.profiling.api.dto.ProfileStatusDTO;
import com.pentaho.profiling.api.metrics.MetricContributorService;
import com.pentaho.profiling.api.sample.SampleProviderManager;

import javax.jws.WebService;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by bryan on 7/31/14.
 */
@Produces( { MediaType.APPLICATION_JSON } )
@Consumes( { MediaType.APPLICATION_JSON } )
@WebService
public class ProfilingServiceWebserviceImpl implements ProfilingService {
  public static final String PROFILE_ID = "profileId";
  private final SampleProviderManager sampleProviderManager;
  private final ProfilingService delegate;
  private final MetricContributorService metricContributorService;

  public ProfilingServiceWebserviceImpl( SampleProviderManager sampleProviderManager, ProfilingService delegate,
                                         MetricContributorService metricContributorService ) {
    this.sampleProviderManager = sampleProviderManager;
    this.delegate = delegate;
    this.metricContributorService = metricContributorService;
  }

  @Override public ProfileFactory getProfileFactory( DataSourceMetadata dataSourceMetadata ) {
    return delegate.getProfileFactory( dataSourceMetadata );
  }

  /**
   * Returns a boolean indicating whether a given DataSourceMetadata's provider can be used to create a profile.
   * (Returns true iff there is a registered ProfileFactory that accepts the DataSourceMetadata.)
   *
   * @param dataSourceMetadata The DataSourceMetadata.
   * @return A boolean indicating whether a given DataSourceMetadata's provider can be used to create a profile.
   */
  @POST
  @Path( "/accepts" )
  @SuccessResponseCode( 200 )
  @Override public boolean accepts( DataSourceMetadata dataSourceMetadata ) {
    return delegate.accepts( dataSourceMetadata );
  }

  /**
   * Creates examples for accepts call
   *
   * @return examples for accepts call
   */
  public List<Example> acceptsExample() {
    List<Example> examples = new ArrayList<Example>();
    for ( DataSourceMetadata dataSourceReference : sampleProviderManager.provide( DataSourceMetadata.class ) ) {
      examples.add( new Example( null, null, dataSourceReference, delegate.accepts( dataSourceReference ) ) );
    }
    return examples;
  }

  /**
   * Creates a profile from the given ProfileConfiguration. If no metric contributors are specified, the profile will be
   * created with the current defaults.
   *
   * @param profileConfiguration The profile configuration.
   * @return The initial profile status.
   * @throws ProfileCreationException
   */
  @POST
  @Path( "/" )
  @SuccessResponseCode( 200 )
  @ErrorCode( code = 500, reason = "Unable to create profile." )
  public ProfileStatusDTO createWebservice( ProfileConfiguration profileConfiguration ) {
    ProfileStatusManager profileStatus;
    try {
      profileStatus = delegate.create( profileConfiguration );
    } catch ( ProfileCreationException e ) {
      throw new WebApplicationException( 500 );
    }
    return new ProfileStatusDTO( profileStatus );
  }

  @Override
  public ProfileStatusManager create( ProfileConfiguration profileConfiguration ) throws ProfileCreationException {
    return delegate.create( profileConfiguration );
  }

  /**
   * Example generation code for create call
   *
   * @return the list of examples
   */
  public List<Example> createWebserviceExample() {
    List<Example> result = new ArrayList<Example>();
    for ( ProfileStatusManager profileStatusManager : sampleProviderManager.provide( ProfileStatusManager.class ) ) {
      result.add( new Example( null, null, profileStatusManager.getProfileConfiguration(),
        new ProfileStatusDTO( profileStatusManager ) ) );
    }
    return result;
  }

  /**
   * Returns current status for all active profiles.
   *
   * @return Current status for all active profiles.
   */
  @GET
  @Path( "/" )
  @SuccessResponseCode( 200 )
  public List<ProfileStatus> getActiveProfilesWebservice() {
    List<ProfileStatusReader> profileStatusReaders = getActiveProfiles();
    List<ProfileStatus> result = new ArrayList<ProfileStatus>( profileStatusReaders.size() );
    for ( ProfileStatusReader profileStatusManager : profileStatusReaders ) {
      result.add( profileStatusManager.read( new ProfileStatusReadOperation<ProfileStatus>() {
        @Override public ProfileStatus read( ProfileStatus profileStatus ) {
          return new ProfileStatusDTO( profileStatus );
        }
      } ) );
    }
    return result;
  }

  /**
   * Example generation for getActiveProfilesWebservice
   *
   * @return the list of examples
   */
  public Example getActiveProfilesWebserviceExample() {
    List<ProfileStatus> provide = sampleProviderManager.provide( ProfileStatus.class );
    ArrayList<ProfileStatus> response = new ArrayList<ProfileStatus>( provide.size() );
    for ( ProfileStatus profileStatus : provide ) {
      response.add( new ProfileStatusDTO( profileStatus ) );
    }
    return new Example( null, null, null, response );
  }

  @Override
  public List<ProfileStatusReader> getActiveProfiles() {
    return delegate.getActiveProfiles();
  }

  @Override public Profile getProfile( String profileId ) {
    return delegate.getProfile( profileId );
  }

  /**
   * Gets the current status of the profile with the given id.
   *
   * @param profileId The profile id.
   * @return The current status of the corresponding profile.
   */
  @GET
  @Path( "/{profileId}" )
  @SuccessResponseCode( 200 )
  public ProfileStatus getProfileUpdateWebservice( @PathParam( "profileId" ) String profileId ) {
    return getProfileUpdate( profileId ).read( new ProfileStatusReadOperation<ProfileStatus>() {
      @Override public ProfileStatus read( ProfileStatus profileStatus ) {
        return new ProfileStatusDTO( profileStatus );
      }
    } );
  }

  public List<Example> getProfileUpdateWebserviceExample() {
    List<Example> result = new ArrayList<Example>();
    for ( ProfileStatus profileStatus : sampleProviderManager.provide( ProfileStatus.class ) ) {
      Example example = new Example();
      example.getPathParameters().put( PROFILE_ID, profileStatus.getId() );
      example.setResponse( new ProfileStatusDTO( profileStatus ) );
      result.add( example );
    }
    return result;
  }

  @Override
  public ProfileStatusReader getProfileUpdate( String profileId ) {
    return delegate.getProfileUpdate( profileId );
  }

  /**
   * Stops the profile with the given id.
   *
   * @param profileId The profile id.
   */
  @PUT
  @Path( "/stop/{profileId}" )
  @Override public void stop( @PathParam( "profileId" ) String profileId ) {
    delegate.stop( profileId );
  }

  public Example stopExample() {
    Example example = new Example();
    example.getPathParameters().put( "profileId", UUID.randomUUID().toString() );
    return example;
  }

  /**
   * Checks to see if the given profile is currently running.
   *
   * @param profileId The profile id to check.
   * @return A boolean indicating whether the profile is running.
   */
  @GET
  @Path( "/isRunning/{profileId}" )
  @SuccessResponseCode( 200 )
  @Override public boolean isRunning( @PathParam( PROFILE_ID ) String profileId ) {
    return delegate.isRunning( profileId );
  }

  public List<Example> isRunningExample() {
    List<Example> result = new ArrayList<Example>();
    Example example = new Example();
    example.getPathParameters().put( PROFILE_ID, UUID.randomUUID().toString() );
    example.setResponse( true );
    result.add( example );
    example = new Example();
    example.getPathParameters().put( PROFILE_ID, UUID.randomUUID().toString() );
    example.setResponse( false );
    result.add( example );
    return result;
  }

  /**
   * Discards the profile, removing it from memory.
   *
   * @param profileId The profile id to discard.
   */
  @PUT
  @Path( "/discard/{profileId}" )
  @SuccessResponseCode( 204 )
  @Override public void discardProfile( @PathParam( "profileId" ) String profileId ) {
    delegate.discardProfile( profileId );
  }

  public Example discardProfileExample() {
    Example example = new Example();
    example.getPathParameters().put( "profileId", UUID.randomUUID().toString() );
    return example;
  }
}

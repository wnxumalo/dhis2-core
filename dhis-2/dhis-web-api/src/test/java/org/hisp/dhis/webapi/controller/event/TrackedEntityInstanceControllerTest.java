/*
 * Copyright (c) 2004-2021, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.webapi.controller.event;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.Collections;

import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.schema.descriptors.TrackedEntityInstanceSchemaDescriptor;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.exception.BadRequestException;
import org.hisp.dhis.webapi.controller.exception.NotFoundException;
import org.hisp.dhis.webapi.controller.exception.OperationNotAllowedException;
import org.hisp.dhis.webapi.strategy.old.tracker.imports.impl.TrackedEntityInstanceAsyncStrategyImpl;
import org.hisp.dhis.webapi.strategy.old.tracker.imports.impl.TrackedEntityInstanceStrategyImpl;
import org.hisp.dhis.webapi.strategy.old.tracker.imports.impl.TrackedEntityInstanceSyncStrategyImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @author Luca Cambi <luca@dhis2.org>
 */
public class TrackedEntityInstanceControllerTest
{

    private MockMvc mockMvc;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private TrackedEntityInstanceAsyncStrategyImpl trackedEntityInstanceAsyncStrategy;

    @Mock
    private TrackedEntityInstanceSyncStrategyImpl trackedEntityInstanceSyncStrategy;

    @Mock
    private User user;

    @Mock
    private org.hisp.dhis.trackedentity.TrackedEntityInstanceService instanceService;

    @Mock
    private TrackerAccessManager trackerAccessManager;

    @Mock
    private TrackedEntityInstance trackedEntityInstance;

    private final static String ENDPOINT = TrackedEntityInstanceSchemaDescriptor.API_ENDPOINT;

    @Before
    public void setUp()
        throws BadRequestException,
        IOException
    {
        final TrackedEntityInstanceController controller = new TrackedEntityInstanceController(
            mock( TrackedEntityInstanceService.class ), instanceService, null, null, null,
            currentUserService, null, trackerAccessManager, null, null,
            new TrackedEntityInstanceStrategyImpl(
                trackedEntityInstanceSyncStrategy, trackedEntityInstanceAsyncStrategy ) );

        mockMvc = MockMvcBuilders.standaloneSetup( controller ).build();
        when( currentUserService.getCurrentUser() ).thenReturn( user );
        when( user.getUid() ).thenReturn( "userId" );
    }

    @Test
    public void shouldCallSyncStrategy()
        throws Exception
    {

        when( trackedEntityInstanceSyncStrategy.mergeOrDeleteTrackedEntityInstances( any() ) )
            .thenReturn( new ImportSummaries() );

        mockMvc.perform( post( ENDPOINT )
            .contentType( MediaType.APPLICATION_JSON )
            .content( "{}" ) )
            .andExpect( status().isOk() )
            .andReturn();

        verify( trackedEntityInstanceSyncStrategy, times( 1 ) ).mergeOrDeleteTrackedEntityInstances( any() );
        verify( trackedEntityInstanceAsyncStrategy, times( 0 ) ).mergeOrDeleteTrackedEntityInstances( any() );
    }

    @Test
    public void shouldCallAsyncStrategy()
        throws Exception
    {
        mockMvc.perform( post( ENDPOINT )
            .contentType( MediaType.APPLICATION_JSON ).param( "async", "true" )
            .content( "{}" ) )
            .andExpect( status().isOk() )
            .andReturn();

        verify( trackedEntityInstanceSyncStrategy, times( 0 ) ).mergeOrDeleteTrackedEntityInstances( any() );
        verify( trackedEntityInstanceAsyncStrategy, times( 1 ) ).mergeOrDeleteTrackedEntityInstances( any() );
    }

    @Test
    public void shouldFlagPotentialDuplicate()
        throws Exception
    {
        String uid = "uid";

        ArgumentCaptor<TrackedEntityInstance> trackedEntityInstanceArgumentCaptor = ArgumentCaptor
            .forClass( TrackedEntityInstance.class );

        when( instanceService.getTrackedEntityInstance( uid ) ).thenReturn( new TrackedEntityInstance() );

        mockMvc.perform( put( ENDPOINT + "/" + uid + "/potentialduplicate" )
            .contentType( MediaType.APPLICATION_JSON ).param( "flag", "true" )
            .content( "{}" ) )
            .andExpect( status().isOk() );

        verify( instanceService ).updateTrackedEntityInstance( trackedEntityInstanceArgumentCaptor.capture() );
        assertTrue( trackedEntityInstanceArgumentCaptor.getValue().isPotentialDuplicate() );

        reset( instanceService );

        when( instanceService.getTrackedEntityInstance( uid ) ).thenReturn( new TrackedEntityInstance() );

        mockMvc.perform( put( ENDPOINT + "/" + uid + "/potentialduplicate" )
            .contentType( MediaType.APPLICATION_JSON ).param( "flag", "false" )
            .content( "{}" ) )
            .andExpect( status().isOk() );

        verify( instanceService ).updateTrackedEntityInstance( trackedEntityInstanceArgumentCaptor.capture() );
        assertFalse( trackedEntityInstanceArgumentCaptor.getValue().isPotentialDuplicate() );
    }

    @Test
    public void shouldThrowFlagPotentialDuplicateMissingTeiAccess()
        throws Exception
    {
        String uid = "uid";

        when( instanceService.getTrackedEntityInstance( uid ) ).thenReturn( trackedEntityInstance );

        when( trackerAccessManager.canWrite( user, trackedEntityInstance ) )
            .thenReturn( Collections.singletonList( "Read error" ) );

        mockMvc.perform( put( ENDPOINT + "/" + uid + "/potentialduplicate" )
            .contentType( MediaType.APPLICATION_JSON ).param( "flag", "true" )
            .content( "{}" ) )
            .andExpect( status().isForbidden() )
            .andExpect( result -> assertTrue( result.getResolvedException() instanceof OperationNotAllowedException ) );

        verify( instanceService, times( 0 ) ).updateTrackedEntityInstance( trackedEntityInstance );
    }

    @Test
    public void shouldThrowFlagPotentialDuplicateInvalidTei()
        throws Exception
    {
        String uid = "uid";

        when( instanceService.getTrackedEntityInstance( uid ) ).thenReturn( null );

        mockMvc.perform( put( ENDPOINT + "/" + uid + "/potentialduplicate" )
            .contentType( MediaType.APPLICATION_JSON ).param( "flag", "true" )
            .content( "{}" ) )
            .andExpect( status().isNotFound() )
            .andExpect( result -> assertTrue( result.getResolvedException() instanceof NotFoundException ) );

        verify( instanceService, times( 0 ) ).updateTrackedEntityInstance( trackedEntityInstance );
    }
}

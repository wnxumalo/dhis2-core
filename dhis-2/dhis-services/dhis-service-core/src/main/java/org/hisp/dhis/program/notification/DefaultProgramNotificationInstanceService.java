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
package org.hisp.dhis.program.notification;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Zubair Asghar
 */

@Slf4j
@Service( "org.hisp.dhis.program.notification.ProgramNotificationInstanceService" )
public class DefaultProgramNotificationInstanceService
    implements ProgramNotificationInstanceService
{
    private final ProgramNotificationInstanceStore notificationInstanceStore;

    private final ProgramInstanceService programInstanceService;

    private final ProgramStageInstanceService programStageInstanceService;

    public DefaultProgramNotificationInstanceService( ProgramNotificationInstanceStore notificationInstanceStore,
        ProgramInstanceService programInstanceService, ProgramStageInstanceService programStageInstanceService )
    {

        checkNotNull( notificationInstanceStore );
        checkNotNull( programInstanceService );
        checkNotNull( programStageInstanceService );
        this.notificationInstanceStore = notificationInstanceStore;
        this.programInstanceService = programInstanceService;
        this.programStageInstanceService = programStageInstanceService;
    }

    @Override
    @Transactional( readOnly = true )
    public List<ProgramNotificationInstance> getProgramNotificationInstances( ProgramInstance programInstance )
    {
        return notificationInstanceStore.getProgramNotificationInstances( programInstance );
    }

    @Override
    @Transactional( readOnly = true )
    public List<ProgramNotificationInstance> getProgramNotificationInstances(
        ProgramStageInstance programStageInstance )
    {
        return notificationInstanceStore.getProgramNotificationInstances( programStageInstance );
    }

    @Override
    @Transactional( readOnly = true )
    public List<ProgramNotificationInstance> getProgramNotificationInstances(
        ProgramNotificationInstanceParam programNotificationInstanceParam )
    {
        return notificationInstanceStore.getProgramNotificationInstances( programNotificationInstanceParam );
    }

    @Override
    @Transactional( readOnly = true )
    public void validateQueryParameters( ProgramNotificationInstanceParam params )
    {
        String violation = null;

        if ( !params.hasProgramInstance() && !params.hasProgramStageInstance() )
        {
            violation = "Program instance or program stage instance must be provided";
        }

        if ( !programInstanceService.programInstanceExists( params.getProgramInstance() ) )
        {
            violation = String.format( "Program instance %s does not exist", params.getProgramInstance() );
        }

        if ( !programStageInstanceService.programStageInstanceExists( params.getProgramStageInstance() ) )
        {
            violation = String.format( "Program stage instance %s does not exist", params.getProgramStageInstance() );
        }

        if ( violation != null )
        {
            log.warn( "Parameter validation failed: " + violation );

            throw new IllegalQueryException( violation );
        }
    }

    @Override
    @Transactional( readOnly = true )
    public ProgramNotificationInstance get( long programNotificationInstance )
    {
        return notificationInstanceStore.get( programNotificationInstance );
    }

    @Override
    @Transactional
    public void save( ProgramNotificationInstance programNotificationInstance )
    {
        notificationInstanceStore.save( programNotificationInstance );
    }

    @Override
    @Transactional
    public void update( ProgramNotificationInstance programNotificationInstance )
    {
        notificationInstanceStore.update( programNotificationInstance );
    }

    @Override
    @Transactional
    public void delete( ProgramNotificationInstance programNotificationInstance )
    {
        notificationInstanceStore.delete( programNotificationInstance );
    }
}

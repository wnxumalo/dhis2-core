/*
 * Copyright (c) 2004-2004-2021, University of Oslo
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
package org.hisp.dhis.dashboard.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.NonNull;

import org.apache.commons.collections4.MapUtils;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryDimension;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryOptionGroupSetDimension;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementGroupSetDimension;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.sharing.AccessObject;
import org.hisp.dhis.sharing.CascadeSharingParameters;
import org.hisp.dhis.sharing.CascadeSharingReport;
import org.hisp.dhis.sharing.CascadeSharingService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeDimension;
import org.hisp.dhis.trackedentity.TrackedEntityDataElementDimension;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.hisp.dhis.visualization.Visualization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultCascadeSharingService
    implements CascadeSharingService
{
    private final IdentifiableObjectManager manager;

    public DefaultCascadeSharingService( @NonNull IdentifiableObjectManager manager )
    {
        this.manager = manager;
    }

    @Override
    @Transactional
    public CascadeSharingReport cascadeSharing( Dashboard dashboard, CascadeSharingParameters parameters )
    {
        if ( CollectionUtils.isEmpty( dashboard.getItems() ) )
        {
            return parameters.getReport();
        }

        Set<IdentifiableObject> updateObjects = new HashSet<>();

        dashboard.getItems().forEach( dashboardItem -> {

            Set<IdentifiableObject> dashboardItemUpdateObjects = new HashSet<>();

            switch ( dashboardItem.getType() )
            {
            case MAP:
                handleMapObject( dashboard, dashboardItem.getMap(), dashboardItemUpdateObjects, parameters );
                break;
            case VISUALIZATION:
                handleVisualization( dashboard, dashboardItem.getVisualization(), dashboardItemUpdateObjects,
                    parameters );
                break;
            case EVENT_REPORT:
                handleEventReport( dashboard, dashboardItem.getEventReport(), dashboardItemUpdateObjects, parameters );
                break;
            case EVENT_CHART:
                handleEventChart( dashboard, dashboardItem.getEventChart(), dashboardItemUpdateObjects, parameters );
                break;
            default:
                break;
            }

            if ( !CollectionUtils.isEmpty( dashboardItemUpdateObjects ) )
            {
                updateObjects.addAll( dashboardItemUpdateObjects );
                parameters.getReport().incUpdatedDashboardItem();
            }
        } );

        if ( !canUpdate( parameters ) || CollectionUtils.isEmpty( updateObjects ) )
        {
            return parameters.getReport();
        }

        manager.update( new ArrayList<>( updateObjects ) );

        return parameters.getReport();
    }

    private void handleMapObject( Dashboard dashboard, Map map, Set<IdentifiableObject> updateObjects,
        CascadeSharingParameters parameters )
    {
        if ( mergeSharing( dashboard.getSharing(), map, parameters ) )
        {
            updateObjects.add( dashboard );
        }
    }

    private void handleVisualization( Dashboard dashboard, Visualization visualization,
        Set<IdentifiableObject> updateObjects,
        CascadeSharingParameters parameters )
    {
        if ( handleIdentifiableObject( dashboard.getSharing(), visualization, updateObjects, parameters ) )
        {
            updateObjects.add( visualization );
        }

        handleBaseAnalyticObject( dashboard.getSharing(), visualization, updateObjects, parameters );
    }

    private void handleEventReport( Dashboard dashboard, EventReport eventReport, Set<IdentifiableObject> updateObjects,
        CascadeSharingParameters parameters )
    {
        if ( handleIdentifiableObject( dashboard.getSharing(), eventReport, updateObjects, parameters ) )
        {
            updateObjects.add( eventReport );
        }

        handleBaseAnalyticObject( dashboard.getSharing(), eventReport, updateObjects, parameters );
    }

    private void handleEventChart( Dashboard dashboard, EventChart eventChart, Set<IdentifiableObject> updateObjects,
        CascadeSharingParameters parameters )
    {

        if ( handleIdentifiableObject( dashboard.getSharing(), eventChart, updateObjects, parameters ) )
        {
            updateObjects.add( eventChart );
        }

        handleIdentifiableObject( dashboard.getSharing(), eventChart.getAttributeValueDimension(), updateObjects,
            parameters );

        handleIdentifiableObject( dashboard.getSharing(), eventChart.getDataElementValueDimension(), updateObjects,
            parameters );

        handleBaseAnalyticObject( dashboard.getSharing(), eventChart, updateObjects, parameters );
    }

    private void handleBaseAnalyticObject( Sharing source, BaseAnalyticalObject analyticalObject,
        Set<IdentifiableObject> listUpdateObjects,
        CascadeSharingParameters parameters )
    {
        handleIdentifiableObjects( source, analyticalObject.getDataElements(), listUpdateObjects, parameters );

        handleIdentifiableObjects( source, analyticalObject.getIndicators(), listUpdateObjects, parameters );

        handleCategoryDimension( source, analyticalObject, listUpdateObjects, parameters );

        handleDataElementDimensions( source, analyticalObject, listUpdateObjects, parameters );

        handleDataElementGroupSetDimensions( source, analyticalObject, listUpdateObjects, parameters );

        handleCategoryOptionGroupSetDimensions( source, analyticalObject, listUpdateObjects, parameters );

        handleTrackedEntityAttributeDimension( source, analyticalObject, listUpdateObjects, parameters );
    }

    private void handleTrackedEntityAttributeDimension( Sharing source, BaseAnalyticalObject analyticalObject,
        Set<IdentifiableObject> listUpdateObjects, CascadeSharingParameters parameters )
    {
        List<TrackedEntityAttributeDimension> attributeDimensions = analyticalObject
            .getAttributeDimensions();

        if ( CollectionUtils.isEmpty( attributeDimensions ) )
        {
            return;
        }

        attributeDimensions.forEach( attributeDimension -> {
            handleIdentifiableObject( source, attributeDimension.getAttribute(), listUpdateObjects, parameters );
            handleIdentifiableObject( source, attributeDimension.getLegendSet(), listUpdateObjects, parameters );
        } );
    }

    private void handleCategoryOptionGroupSetDimensions( Sharing source, BaseAnalyticalObject analyticalObject,
        Set<IdentifiableObject> listUpdateObjects, CascadeSharingParameters parameters )
    {
        List<CategoryOptionGroupSetDimension> catOptionGroupSetDimensions = analyticalObject
            .getCategoryOptionGroupSetDimensions();

        if ( CollectionUtils.isEmpty( catOptionGroupSetDimensions ) )
        {
            return;
        }

        catOptionGroupSetDimensions.forEach( categoryOptionGroupSetDimension -> {
            CategoryOptionGroupSet catOptionGroupSet = categoryOptionGroupSetDimension
                .getDimension();

            handleIdentifiableObject( source, catOptionGroupSet, listUpdateObjects, parameters );

            List<CategoryOptionGroup> catOptionGroups = catOptionGroupSet.getMembers();

            if ( CollectionUtils.isEmpty( catOptionGroups ) )
            {
                return;
            }

            catOptionGroups.forEach( catOptionGroup -> {
                handleIdentifiableObject( source, catOptionGroup, listUpdateObjects, parameters );
                handleIdentifiableObjects( source, catOptionGroup.getMembers(), listUpdateObjects, parameters );
            } );

        } );
    }

    private void handleDataElementDimensions( Sharing source, BaseAnalyticalObject analyticalObject,
        Set<IdentifiableObject> listUpdateObjects,
        CascadeSharingParameters parameters )
    {
        List<TrackedEntityDataElementDimension> deDimensions = analyticalObject
            .getDataElementDimensions();

        if ( CollectionUtils.isEmpty( deDimensions ) )
        {
            return;
        }

        deDimensions.forEach( deDimension -> {
            handleIdentifiableObject( source, deDimension.getDataElement(), listUpdateObjects, parameters );
            handleIdentifiableObject( source, deDimension.getLegendSet(), listUpdateObjects, parameters );
            handleIdentifiableObject( source, deDimension.getProgramStage(), listUpdateObjects, parameters );
        } );
    }

    private void handleCategoryDimension( Sharing source, BaseAnalyticalObject analyticalObject,
        Set<IdentifiableObject> listUpdateObjects,
        CascadeSharingParameters parameters )
    {
        List<CategoryDimension> catDimensions = analyticalObject.getCategoryDimensions();

        if ( CollectionUtils.isEmpty( catDimensions ) )
        {
            return;
        }

        catDimensions.forEach( catDimension -> {
            Category category = catDimension.getDimension();

            handleIdentifiableObject( source, category, listUpdateObjects, parameters );

            List<CategoryOption> catOptions = catDimension.getItems();

            if ( CollectionUtils.isEmpty( catOptions ) )
            {
                return;
            }

            catOptions.forEach( catOption -> handleIdentifiableObject( source, catOption,
                listUpdateObjects, parameters ) );
        } );
    }

    private void handleDataElementGroupSetDimensions( Sharing source, BaseAnalyticalObject analyticalObject,
        Set<IdentifiableObject> listUpdateObjects,
        CascadeSharingParameters parameters )
    {
        List<DataElementGroupSetDimension> deGroupSetDimensions = analyticalObject
            .getDataElementGroupSetDimensions();

        if ( CollectionUtils.isEmpty( deGroupSetDimensions ) )
        {
            return;
        }

        deGroupSetDimensions.forEach( deGroupSetDimension -> {
            DataElementGroupSet deGroupSet = deGroupSetDimension.getDimension();

            handleIdentifiableObject( source, deGroupSet, listUpdateObjects, parameters );

            List<DataElementGroup> deGroups = deGroupSetDimension.getItems();

            if ( CollectionUtils.isEmpty( deGroups ) )
            {
                return;
            }

            deGroups
                .forEach( deGroup -> handleIdentifiableObject( source, deGroup, listUpdateObjects, parameters ) );
        } );
    }

    private void handleIdentifiableObjects( final Sharing source,
        final Collection<? extends IdentifiableObject> targetObjects, Collection<IdentifiableObject> listUpdateObjects,
        CascadeSharingParameters parameters )
    {
        if ( CollectionUtils.isEmpty( targetObjects ) )
        {
            return;
        }

        targetObjects.forEach( object -> handleIdentifiableObject( source, object, listUpdateObjects, parameters ) );
    }

    private boolean handleIdentifiableObject( final Sharing source,
        IdentifiableObject target, Collection<IdentifiableObject> listUpdateObjects,
        CascadeSharingParameters parameters )
    {
        if ( target == null )
        {
            return false;
        }

        if ( mergeSharing( source, target, parameters ) )
        {
            listUpdateObjects.add( target );
            return true;
        }

        return false;
    }

    private <S extends IdentifiableObject, T extends IdentifiableObject> boolean mergeSharing( Sharing source, T target,
        CascadeSharingParameters parameters )
    {
        if ( AccessStringHelper.canRead( target.getSharing().getPublicAccess() ) )
        {
            return false;
        }

        java.util.Map<String, UserAccess> userAccesses = new HashMap<>( target.getSharing().getUsers() );
        java.util.Map<String, UserGroupAccess> userGroupAccesses = new HashMap<>( target.getSharing()
            .getUserGroups() );

        if ( mergeAccessObject( source.getUsers(), userAccesses )
            || mergeAccessObject( source.getUserGroups(), userGroupAccesses ) )
        {
            if ( !parameters.isDryRun() )
            {
                target.getSharing().setUsers( userAccesses );
                target.getSharing().setUserGroups( userGroupAccesses );
            }

            parameters.getReport().addUpdatedObject( target );
            return true;
        }

        return false;
    }

    /**
     * Merge {@link org.hisp.dhis.sharing.AccessObject} from source to target
     * {@code Map<String,AccessObject>}
     */
    private <T extends AccessObject> boolean mergeAccessObject(
        java.util.Map<String, T> source, java.util.Map<String, T> target )
    {
        if ( MapUtils.isEmpty( source ) )
        {
            return false;
        }

        boolean shouldUpdate = false;

        for ( T sourceAccess : source.values() )
        {
            if ( !AccessStringHelper.canRead( sourceAccess.getAccess() ) )
            {
                continue;
            }

            T targetAccess = target.get( sourceAccess.getId() );

            if ( targetAccess != null && AccessStringHelper.canRead( targetAccess.getAccess() ) )
            {
                continue;
            }

            if ( targetAccess == null )
            {
                targetAccess = sourceAccess;
            }

            targetAccess.setAccess( AccessStringHelper.READ );
            target.put( targetAccess.getId(), targetAccess );

            shouldUpdate = true;
        }

        return shouldUpdate;
    }

    private boolean canUpdate( CascadeSharingParameters parameters )
    {
        return !parameters.isDryRun() && parameters.getReport().getErrorReports().isEmpty();
    }
}

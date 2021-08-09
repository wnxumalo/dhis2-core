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
package org.hisp.dhis.sharing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.collections.SetUtils;
import org.hisp.dhis.chart.ChartType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeDimension;
import org.hisp.dhis.trackedentity.TrackedEntityDataElementDimension;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.hisp.dhis.visualization.Visualization;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

public class DashboardCascadeSharingTest
    extends CascadeSharingTest
{
    @Autowired
    private UserService _userService;

    @Autowired
    private AclService aclService;

    @Autowired
    private CascadeSharingService cascadeSharingService;

    @Autowired
    private IdentifiableObjectManager objectManager;

    private UserGroup userGroupA;

    private User userA;

    private User userB;

    private Sharing sharingReadForUserA;

    private Sharing sharingReadForUserAB;

    private Sharing sharingReadWriteForUserB;

    private Sharing sharingUserGroupA;

    private Program programA;

    @Override
    public void setUpTest()
    {
        userService = _userService;
        userA = createUser( 'A' );
        userB = createUser( 'B' );

        userGroupA = createUserGroup( 'A', SetUtils.EMPTY_SET );

        sharingReadForUserA = new Sharing();
        sharingReadForUserA.setPublicAccess( AccessStringHelper.DEFAULT );
        sharingReadForUserA.addUserAccess( new UserAccess( userA, AccessStringHelper.READ ) );

        sharingReadWriteForUserB = new Sharing();
        sharingReadWriteForUserB.setPublicAccess( AccessStringHelper.DEFAULT );
        sharingReadWriteForUserB.addUserAccess( new UserAccess( userB, AccessStringHelper.READ_WRITE ) );

        sharingReadForUserAB = new Sharing();
        sharingReadForUserAB.setPublicAccess( AccessStringHelper.DEFAULT );
        sharingReadForUserAB.addUserAccess( new UserAccess( userA, AccessStringHelper.READ ) );
        sharingReadForUserAB.addUserAccess( new UserAccess( userB, AccessStringHelper.READ ) );

        sharingUserGroupA = new Sharing();
        sharingUserGroupA.setPublicAccess( AccessStringHelper.DEFAULT );
        sharingUserGroupA.addUserGroupAccess( new UserGroupAccess( userGroupA, AccessStringHelper.READ ) );

        programA = createProgram( 'A' );
        programA.setSharing( defaultSharing() );
        objectManager.save( programA, false );

        createAndInjectAdminUser();
    }

    /**
     * Dashboard has sharingUserA and visualizationA
     * <p>
     * visualizationA has dataElementA
     * <p>
     * Expected: visualizationA and dataElementA should be shared to userA
     */
    @Test
    public void testCascadeShareVisualization()
    {
        DataElement dataElementA = createDEWithDefaultSharing( 'A' );
        objectManager.save( dataElementA );
        DataElement dataElementB = createDEWithDefaultSharing( 'B' );
        objectManager.save( dataElementB );

        Visualization visualizationA = createVisualization( 'A' );
        visualizationA.addDataDimensionItem( dataElementA );
        visualizationA.addDataDimensionItem( dataElementB );
        visualizationA.setSharing( Sharing.builder().publicAccess( AccessStringHelper.DEFAULT ).build() );
        objectManager.save( visualizationA, false );

        Dashboard dashboard = createDashboardWithItem( "A", sharingReadForUserAB );
        dashboard.getItems().get( 0 ).setVisualization( visualizationA );
        objectManager.save( dashboard, false );

        CascadeSharingReport report = cascadeSharingService.cascadeSharing( dashboard,
            new CascadeSharingParameters() );
        assertEquals( 0, report.getErrorReports().size() );

        DataElement updatedDataElementA = objectManager.get( DataElement.class, dataElementA.getUid() );
        DataElement updatedDataElementB = objectManager.get( DataElement.class, dataElementB.getUid() );

        assertTrue( aclService.canRead( userA, visualizationA ) );
        assertTrue( aclService.canRead( userB, visualizationA ) );
        assertTrue( aclService.canRead( userA, updatedDataElementA ) );
        assertTrue( aclService.canRead( userB, updatedDataElementB ) );

    }

    @Test
    public void testCascadeShareVisualizationError()
    {
        DataElement dataElementA = createDataElement( 'A' );
        dataElementA.setSharing( Sharing.builder().publicAccess( AccessStringHelper.DEFAULT ).build() );
        objectManager.save( dataElementA, false );

        Visualization vzA = createVisualization( 'A' );
        vzA.setSharing( Sharing.builder().publicAccess( AccessStringHelper.DEFAULT ).build() );
        vzA.addDataDimensionItem( dataElementA );
        objectManager.save( vzA, false );

        Sharing sharing = new Sharing();
        sharing.setPublicAccess( AccessStringHelper.DEFAULT );
        sharing.addUserAccess( new UserAccess( userB, AccessStringHelper.DEFAULT ) );
        Dashboard dashboard = createDashboardWithItem( "A", sharing );
        dashboard.getItems().get( 0 ).setVisualization( vzA );
        objectManager.save( dashboard, false );

        CascadeSharingReport report = cascadeSharingService.cascadeSharing( dashboard,
            new CascadeSharingParameters() );
        assertEquals( 0, report.getUpdatedObjects().size() );

        assertFalse( aclService.canRead( userB, vzA ) );
        assertFalse( aclService.canRead( userB, dataElementA ) );
    }

    /**
     * Dashboard is shared to userA
     * <p>
     * Dashboard has a MapA
     * <p>
     * Expected: MapA will be shared to userA
     */
    @Test
    public void testCascadeShareMap()
    {
        MapView mapView = createMapView( "Test" );
        Map map = new Map();
        map.setName( "mapA" );
        map.setSharing( Sharing.builder().publicAccess( AccessStringHelper.DEFAULT ).build() );
        map.setMapViews( Lists.newArrayList( mapView ) );
        objectManager.save( map, false );

        Dashboard dashboard = createDashboardWithItem( "A", sharingReadForUserA );
        dashboard.getItems().get( 0 ).setMap( map );
        objectManager.save( dashboard, false );

        CascadeSharingReport report = cascadeSharingService.cascadeSharing( dashboard,
            new CascadeSharingParameters() );
        assertEquals( 0, report.getErrorReports().size() );
        assertTrue( aclService.canRead( userA, dashboard.getItems().get( 0 ).getMap() ) );
        assertEquals( AccessStringHelper.READ,
            dashboard.getItems().get( 0 ).getMap().getSharing().getUsers().get( userA.getUid() ).getAccess() );
        assertFalse( aclService.canRead( userB, dashboard.getItems().get( 0 ).getMap() ) );
    }

    /**
     * Dashboard has publicAccess READ and not shared to any User or UserGroup.
     * <p>
     * Expected cascade sharing for PublicAccess is not supported, so user can't
     * access dashboardItem's objects.
     */
    @Test
    public void testCascadeSharePublicAccess()
    {
        MapView mapView = createMapView( "Test" );
        Map map = new Map();
        map.setName( "mapA" );
        map.setSharing( Sharing.builder().publicAccess( AccessStringHelper.DEFAULT ).build() );
        map.setMapViews( Lists.newArrayList( mapView ) );
        objectManager.save( map, false );

        Dashboard dashboard = createDashboardWithItem( "dashboardA",
            Sharing.builder().publicAccess( AccessStringHelper.READ ).build() );
        dashboard.getItems().get( 0 ).setMap( map );
        objectManager.save( dashboard, false );

        CascadeSharingReport report = cascadeSharingService.cascadeSharing( dashboard,
            new CascadeSharingParameters() );

        assertEquals( 0, report.getErrorReports().size() );
        assertFalse( aclService.canRead( userA, dashboard.getItems().get( 0 ).getMap() ) );
        assertFalse( aclService.canRead( userB, dashboard.getItems().get( 0 ).getMap() ) );
    }

    /**
     * Dashboard is shared to userB.
     * <p>
     * But userB's access is DEFAULT('--------') Expected: no objects being
     * updated.
     */
    @Test
    public void testCascadeShareMapError()
    {
        DataElement dataElementB = createDataElement( 'B' );
        dataElementB.setSharing( Sharing.builder().publicAccess( AccessStringHelper.DEFAULT ).build() );
        objectManager.save( dataElementB, false );

        MapView mapView = createMapView( "Test" );
        Map map = new Map();
        map.setName( "mapA" );
        map.setSharing( Sharing.builder().publicAccess( AccessStringHelper.DEFAULT ).build() );
        map.setMapViews( Lists.newArrayList( mapView ) );
        objectManager.save( map, false );
        objectManager.flush();

        Sharing sharing = new Sharing();
        sharing.setPublicAccess( AccessStringHelper.DEFAULT );
        sharing.addUserAccess( new UserAccess( userB, AccessStringHelper.DEFAULT ) );
        Dashboard dashboard = createDashboardWithItem( "dashboardA", sharing );
        dashboard.getItems().get( 0 ).setMap( map );
        objectManager.save( dashboard, false );

        CascadeSharingReport report = cascadeSharingService.cascadeSharing( dashboard,
            new CascadeSharingParameters() );
        assertEquals( 0, report.getUpdatedObjects().size() );

        assertFalse( aclService.canRead( userB, dashboard.getItems().get( 0 ).getMap() ) );
    }

    @Test
    public void testCascadeShareEventReport()
    {
        DataElement deA = createDataElement( 'A' );
        deA.setSharing( defaultSharing() );
        objectManager.save( deA, false );

        LegendSet lsA = createLegendSet( 'A' );
        lsA.setSharing( defaultSharing() );
        objectManager.save( lsA, false );

        ProgramStage psA = createProgramStage( 'A', 1 );
        psA.setSharing( defaultSharing() );
        objectManager.save( psA, false );

        TrackedEntityDataElementDimension teDeDim = new TrackedEntityDataElementDimension( deA, lsA, psA, "EQ:1" );

        EventReport eventReport = new EventReport();
        eventReport.setName( "eventReportA" );
        eventReport.setAutoFields();
        eventReport.setProgram( programA );
        eventReport.addTrackedEntityDataElementDimension( teDeDim );
        eventReport.setSharing( defaultSharing() );

        objectManager.save( eventReport, false );

        // Add eventReport to dashboard
        Dashboard dashboard = createDashboardWithItem( "dashboardA", sharingReadForUserA );
        dashboard.getItems().get( 0 ).setEventReport( eventReport );
        objectManager.save( dashboard, false );

        CascadeSharingReport report = cascadeSharingService
            .cascadeSharing( dashboard, CascadeSharingParameters.builder().build() );

        assertEquals( 0, report.getErrorReports().size() );
        assertEquals( 4, report.getUpdatedObjects().size() );
        assertEquals( 1, report.getUpdatedObjects().get( DataElement.class ).size() );
        assertEquals( 1, report.getUpdatedObjects().get( LegendSet.class ).size() );
        assertEquals( 1, report.getUpdatedObjects().get( ProgramStage.class ).size() );
        assertEquals( 1, report.getUpdatedObjects().get( EventReport.class ).size() );

        assertTrue( aclService.canRead( userA, eventReport ) );
        assertTrue( aclService.canRead( userA, deA ) );
        assertTrue( aclService.canRead( userA, lsA ) );
        assertTrue( aclService.canRead( userA, psA ) );

        assertFalse( aclService.canRead( userB, eventReport ) );
        assertFalse( aclService.canRead( userB, deA ) );
        assertFalse( aclService.canRead( userB, lsA ) );
        assertFalse( aclService.canRead( userB, psA ) );

    }

    @Test
    public void cascadeShareEventChart()
    {

        LegendSet legendSet = createLegendSet( 'A' );
        legendSet.setSharing( defaultSharing() );
        objectManager.save( legendSet, false );
        TrackedEntityAttribute trackedEntityAttribute = createTrackedEntityAttribute( 'A' );
        trackedEntityAttribute.setSharing( defaultSharing() );
        objectManager.save( trackedEntityAttribute, false );

        assertFalse( aclService.canRead( userA, legendSet ) );
        assertFalse( aclService.canRead( userA, trackedEntityAttribute ) );

        TrackedEntityAttributeDimension attributeDimension = new TrackedEntityAttributeDimension();
        attributeDimension.setLegendSet( legendSet );
        attributeDimension.setAttribute( trackedEntityAttribute );
        EventChart eventChart = new EventChart();
        eventChart.setName( "eventChartA" );
        eventChart.setProgram( programA );
        eventChart.setType( ChartType.COLUMN );
        eventChart.setAttributeValueDimension( trackedEntityAttribute );
        eventChart.getAttributeDimensions().add( attributeDimension );
        eventChart.setSharing( defaultSharing() );
        objectManager.save( eventChart, false );

        Dashboard dashboard = createDashboardWithItem( "dashboardA", sharingReadForUserA );
        dashboard.getItems().get( 0 ).setEventChart( eventChart );
        objectManager.save( dashboard, false );

        CascadeSharingReport report = cascadeSharingService
            .cascadeSharing( dashboard, CascadeSharingParameters.builder().build() );

        assertEquals( 0, report.getErrorReports().size() );
        assertEquals( 3, report.getUpdatedObjects().size() );
        assertEquals( 1, report.getUpdatedObjects().get( LegendSet.class ).size() );
        assertEquals( 1, report.getUpdatedObjects().get( TrackedEntityAttribute.class ).size() );
        assertEquals( 1, report.getUpdatedObjects().get( EventChart.class ).size() );

        assertTrue( aclService.canRead( userA, eventChart ) );
        assertTrue( aclService.canRead( userA, legendSet ) );
        assertTrue( aclService.canRead( userA, trackedEntityAttribute ) );

        assertFalse( aclService.canRead( userB, eventChart ) );
        assertFalse( aclService.canRead( userB, legendSet ) );
        assertFalse( aclService.canRead( userB, trackedEntityAttribute ) );
    }

    @Test
    public void testCascadeIndicatorAndDataElement()
    {
        IndicatorType indicatorType = createIndicatorType( 'A' );
        objectManager.save( indicatorType );
        Indicator indicatorA = createIndicator( 'A', indicatorType );
        indicatorA.setSharing( Sharing.builder().publicAccess( AccessStringHelper.DEFAULT ).build() );
        objectManager.save( indicatorA, false );

        DataElement dataElementA = createDEWithDefaultSharing( 'A' );
        objectManager.save( dataElementA, false );

        Visualization visualizationA = createVisualization( 'A' );
        visualizationA.addDataDimensionItem( dataElementA );
        visualizationA.addDataDimensionItem( indicatorA );
        visualizationA.setSharing( defaultSharing() );
        objectManager.save( visualizationA, false );

        Dashboard dashboard = createDashboardWithItem( "a", sharingReadForUserA );
        dashboard.getItems().get( 0 ).setVisualization( visualizationA );
        objectManager.save( dashboard, false );

        CascadeSharingReport report = cascadeSharingService.cascadeSharing( dashboard,
            new CascadeSharingParameters() );
        assertEquals( 0, report.getErrorReports().size() );
        assertEquals( 3, report.getUpdatedObjects().size() );
        assertEquals( 1, report.getUpdatedObjects().get( DataElement.class ).size() );
        assertEquals( 1, report.getUpdatedObjects().get( Indicator.class ).size() );
        assertEquals( 1, report.getUpdatedObjects().get( Visualization.class ).size() );

        DataElement updatedDataElementA = objectManager.get( DataElement.class, dataElementA.getUid() );
        Indicator updatedIndicatorA = objectManager.get( Indicator.class, indicatorA.getUid() );

        assertTrue( aclService.canRead( userA, visualizationA ) );
        assertTrue( aclService.canRead( userA, updatedDataElementA ) );
        assertTrue( aclService.canRead( userA, updatedIndicatorA ) );
        assertFalse( aclService.canRead( userB, visualizationA ) );
        assertFalse( aclService.canRead( userB, updatedDataElementA ) );
        assertFalse( aclService.canRead( userB, updatedIndicatorA ) );
    }
}

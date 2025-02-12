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
package org.hisp.dhis.metadata;

import static org.hamcrest.Matchers.hasSize;

import org.hamcrest.Matchers;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.metadata.DataElementActions;
import org.hisp.dhis.actions.metadata.SharingActions;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class MetadataPatchTests
    extends ApiTest
{
    private LoginActions loginActions;

    private DataElementActions dataElementActions;

    private RestApiActions dataElementGroupActions;

    private SharingActions sharingActions;

    private String dataElementId;

    private String dataElementGroupId;

    @BeforeAll
    public void before()
    {
        dataElementActions = new DataElementActions();
        loginActions = new LoginActions();
        dataElementGroupActions = new RestApiActions( "/dataElementGroups" );
        sharingActions = new SharingActions();

        loginActions.loginAsAdmin();

        dataElementId = dataElementActions.create( dataElementActions.body(
            "SUM",
            "AGGREGATE",
            "TEXT" ) );

        dataElementGroupId = dataElementGroupActions
            .create( new JsonObjectBuilder().addProperty( "name", DataGenerator.randomString() ).build() );
    }

    @Disabled( "DHIS2-11483" )
    @Test
    public void shouldReplaceArray()
    {
        sharingActions.setupSharingForUsers( "dataElement", dataElementId, Constants.SUPER_USER_ID,
            Constants.ADMIN_ID );

        JsonObject userAccesses = JsonObjectBuilder.jsonObject()
            .addProperty( "access", "rw------" )
            .addProperty( "id", Constants.SUPER_USER_ID )
            .build();

        JsonArray array = new JsonArray();

        array.add(
            buildOperation( "replace", "/userAccesses", new JsonObjectBuilder( userAccesses ).wrapIntoArray() ) );

        dataElementActions.patch( dataElementId, array ).validate().statusCode( 200 );
        dataElementActions.get( dataElementId ).validate().body( "userAccesses", hasSize( 1 ) );
    }

    @Disabled( "DHIS2-11483" )
    @Test
    public void shouldRemoveArray()
    {
        sharingActions.setupSharingForUsers( "dataElement", dataElementId, Constants.SUPER_USER_ID,
            Constants.ADMIN_ID );

        JsonArray array = new JsonArray();

        array.add(
            JsonObjectBuilder
                .jsonObject( buildOperation( "remove", "/sharing[users]", new JsonObjectBuilder().build() ) )
                .addArray( "value", new JsonObjectBuilder().build() ).build() );

        dataElementActions.patch( dataElementId, array )
            .validate()
            .statusCode( 200 );
        dataElementActions.get( dataElementId )
            .validate()
            .body( "userAccesses", Matchers.emptyArray() );
    }

    @Test
    public void shouldAcceptAnArrayOfOperations()
    {
        String replacedProp = DataGenerator.randomString() + "-replaced";

        JsonArray array = new JsonArray();

        array.add( buildOperation( "replace", "/shortName", replacedProp ) );
        array.add( buildOperation( "add", "/code", replacedProp ) );

        dataElementActions.patch( dataElementId, array ).validate().statusCode( 200 );

        dataElementActions.get( dataElementId )
            .validate()
            .body( "shortName", Matchers.equalTo( replacedProp ) )
            .body( "code", Matchers.equalTo( replacedProp ) );
    }

    @Test
    public void shouldAddToArrays()
    {
        JsonArray array = new JsonArray();

        array.add( buildOperation( "add", "/dataElements/-", new JsonObjectBuilder().addProperty(
            "id", dataElementId ).build() ) );

        dataElementGroupActions.patch( dataElementGroupId, array ).validate()
            .statusCode( 200 );

        dataElementActions.get( dataElementId ).validate()
            .body( "dataElementGroups.id", Matchers.contains( dataElementGroupId ) );
    }

    private JsonObject buildOperation( String op, String path, JsonObject object )
    {
        return JsonObjectBuilder.jsonObject()
            .addProperty( "op", op )
            .addProperty( "path", path )
            .addObject( "value", object )
            .build();
    }

    private JsonObject buildOperation( String op, String path, JsonArray object )
    {
        return JsonObjectBuilder.jsonObject()
            .addProperty( "op", op )
            .addProperty( "path", path )
            .addArray( "value", object )
            .build();
    }

    private JsonObject buildOperation( String op, String path, String value )
    {
        return JsonObjectBuilder.jsonObject()
            .addProperty( "op", op )
            .addProperty( "path", path )
            .addProperty( "value", value )
            .build();
    }
}

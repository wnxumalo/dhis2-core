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
package org.hisp.dhis.node.types;

import java.util.Objects;

import org.hisp.dhis.node.Node;
import org.hisp.dhis.node.config.Config;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @deprecated No new usage of this class and its children should happen, we
 *             should instead directly use Jackson ObjectMappers or Jackson
 *             object factory if we need dynamically created objects.
 */
@Deprecated
public class RootNode extends ComplexNode
{
    private String defaultNamespace;

    private final Config config = new Config();

    public RootNode( String name )
    {
        super( name );
    }

    public RootNode( Node node )
    {
        super( node.getName() );
        setNamespace( node.getNamespace() );
        setComment( node.getComment() );
        addChildren( node.getChildren() );
    }

    public String getDefaultNamespace()
    {
        return defaultNamespace;
    }

    public void setDefaultNamespace( String defaultNamespace )
    {
        this.defaultNamespace = defaultNamespace;
    }

    public Config getConfig()
    {
        return config;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }
        if ( !super.equals( o ) )
        {
            return false;
        }
        RootNode rootNode = (RootNode) o;
        return Objects.equals( defaultNamespace, rootNode.defaultNamespace ) &&
            config.equals( rootNode.config );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( super.hashCode(), defaultNamespace, config );
    }
}

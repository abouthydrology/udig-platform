/*
 *    uDig - User Friendly Desktop Internet GIS client
 *    http://udig.refractions.net
 *    (C) 2012, Refractions Research Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package net.refractions.udig.core.filter;

import java.util.List;

import org.opengis.filter.And;
import org.opengis.filter.Filter;

/**
 * AdaptingFilter that implements And interface.
 * 
 * @author Jody
 * @since 1.1.0
 */
class AdaptingAnd extends AdaptingFilter<And> implements And {

    AdaptingAnd( And filter ) {
        super(filter);
    }

    public List<Filter> getChildren() {
        return wrapped.getChildren();
    }
    
}

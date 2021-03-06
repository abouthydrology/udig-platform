/* uDig - User Friendly Desktop Internet GIS client
 * http://udig.refractions.net
 * (C) 2004-2012, Refractions Research Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package net.refractions.udig.project.internal.command.navigation;

import java.text.MessageFormat;

import net.refractions.udig.project.command.MapCommand;
import net.refractions.udig.project.command.NavCommand;
import net.refractions.udig.project.internal.Messages;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Sets the height of the Viewport bounding box. Because the aspect ratio of the viewport bounding
 * box is tied to the bounding box of the viewport, the width is also set to maintain the correct
 * aspect ratio. To change the display size the PlatformUI class should be used to obtain the
 * eclipse shell or site that should be resized. The viewport model does not modify the size of the
 * display.
 * 
 * @author jeichar
 * @since 0.3
 */
public class SetViewportHeight extends AbstractNavCommand implements NavCommand {

    private double height;

    /**
     * Creates a new instance of SetViewportHeight
     * 
     * @param height The new viewport height
     */
    public SetViewportHeight( double height ) {
        this.height = height;
    }

    /**
     * @see net.refractions.udig.project.internal.command.navigation.AbstractNavCommand#runImpl()
     */
    protected void runImpl( IProgressMonitor monitor ) throws Exception {
        model.setHeight(height);
    }

    /**
     * @see net.refractions.udig.project.internal.command.MapCommand#copy()
     */
    public MapCommand copy() {
        return new SetViewportHeight(height);
    }

    /**
     * @see net.refractions.udig.project.command.MapCommand#getName()
     */
    public String getName() {
        return MessageFormat.format(
                Messages.SetViewportHeight_setViewHeight, new Object[]{height}); 
    }

}
/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
 
package org.aitools.programd.multiplexor;

import org.aitools.programd.util.Settings;

/**
 * 21/10/05: sis05kol
 */
public class OntologySettings extends Settings
{
	/**
     * Filename of ontology 
     */
    private String filename;
    
    /**
     * General Entry Point of a chat 
     */
    private String entrypoint;

    /**
     * Creates a <code>OntologySettings</code> using default property values.
     */
    public OntologySettings()
    {
        super();
    }
    
    /**
     * Creates a <code>OntologySettings</code> with the (XML-formatted) properties
     * located at the given path.
     *
     * @param propertiesPath the path to the configuration file
     */
    public OntologySettings(String propertiesPath)
    {
        super(propertiesPath);
    }

    /**
    * Initializes the Settings with values from properties, or defaults.
    */
    protected void initialize()
    {
    	setFileName(this.properties.getProperty("programd.ontology.filename",null));
        setEntryPoint(this.properties.getProperty("programd.ontology.entrypoint", ":THING"));
    }

    /**
     * @return the value of filename
     */
    public String getFileName()
    {
        return this.filename;
    }
    
    /**
     * @return the value of entrypoint
     */
    public String getEntryPoint()
    {
        return this.entrypoint;
    }

    /**
     * @param entrypointToSet   the value to which to set entrypoint
     */
    public void setFileName(String filenameToSet)
    {
        this.filename = filenameToSet;
    }
    
    /**
     * @param entrypointToSet   the value to which to set entrypoint
     */
    public void setEntryPoint(String entrypointToSet)
    {
        this.entrypoint = entrypointToSet;
    }
}
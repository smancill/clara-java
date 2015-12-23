/*
 * Copyright (C) 2015. Jefferson Lab, CLARA framework (JLAB). All Rights Reserved.
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for educational, research, and not-for-profit purposes,
 * without fee and without a signed licensing agreement.
 *
 * Contact Vardan Gyurjyan
 * Department of Experimental Nuclear Physics, Jefferson Lab.
 *
 * IN NO EVENT SHALL JLAB BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL,
 * INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF
 * THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JLAB HAS BEEN ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JLAB SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE. THE CLARA SOFTWARE AND ACCOMPANYING DOCUMENTATION, IF ANY, PROVIDED
 * HEREUNDER IS PROVIDED "AS IS". JLAB HAS NO OBLIGATION TO PROVIDE MAINTENANCE,
 * SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */

package org.jlab.clara.claraol.impl;


import java.util.Collection;

import org.jlab.clara.claraol.DataSource;
import org.jlab.clara.claraol.EngineConfig;
import org.jlab.clara.claraol.Vocabulary;
import org.protege.owl.codegeneration.impl.WrappedIndividualImpl;

import org.protege.owl.codegeneration.inference.CodeGenerationInference;

import org.semanticweb.owlapi.model.IRI;


/**
 * Generated by Protege (http://protege.stanford.edu).<br>
 * Source Class: DefaultEngineConfig <br>
 * @version generated on Tue Dec 22 14:51:01 EST 2015 by gurjyan
 */
public class DefaultEngineConfig extends WrappedIndividualImpl implements EngineConfig {

    public DefaultEngineConfig(CodeGenerationInference inference, IRI iri) {
        super(inference, iri);
    }





    /* ***************************************************
     * Object Property http://claraweb.jlab.org/ontology/2015/11/ClaraOL#hasData
     */
     
    public Collection<? extends DataSource> getHasData() {
        return getDelegate().getPropertyValues(getOwlIndividual(),
                                               Vocabulary.OBJECT_PROPERTY_HASDATA,
                                               DefaultDataSource.class);
    }

    public boolean hasHasData() {
	   return !getHasData().isEmpty();
    }

    public void addHasData(DataSource newHasData) {
        getDelegate().addPropertyValue(getOwlIndividual(),
                                       Vocabulary.OBJECT_PROPERTY_HASDATA,
                                       newHasData);
    }

    public void removeHasData(DataSource oldHasData) {
        getDelegate().removePropertyValue(getOwlIndividual(),
                                          Vocabulary.OBJECT_PROPERTY_HASDATA,
                                          oldHasData);
    }


    /* ***************************************************
     * Object Property http://claraweb.jlab.org/ontology/2015/11/ClaraOL#hasInputData
     */
     
    public Collection<? extends DataSource> getHasInputData() {
        return getDelegate().getPropertyValues(getOwlIndividual(),
                                               Vocabulary.OBJECT_PROPERTY_HASINPUTDATA,
                                               DefaultDataSource.class);
    }

    public boolean hasHasInputData() {
	   return !getHasInputData().isEmpty();
    }

    public void addHasInputData(DataSource newHasInputData) {
        getDelegate().addPropertyValue(getOwlIndividual(),
                                       Vocabulary.OBJECT_PROPERTY_HASINPUTDATA,
                                       newHasInputData);
    }

    public void removeHasInputData(DataSource oldHasInputData) {
        getDelegate().removePropertyValue(getOwlIndividual(),
                                          Vocabulary.OBJECT_PROPERTY_HASINPUTDATA,
                                          oldHasInputData);
    }


    /* ***************************************************
     * Object Property http://claraweb.jlab.org/ontology/2015/11/ClaraOL#hasOutputData
     */
     
    public Collection<? extends DataSource> getHasOutputData() {
        return getDelegate().getPropertyValues(getOwlIndividual(),
                                               Vocabulary.OBJECT_PROPERTY_HASOUTPUTDATA,
                                               DefaultDataSource.class);
    }

    public boolean hasHasOutputData() {
	   return !getHasOutputData().isEmpty();
    }

    public void addHasOutputData(DataSource newHasOutputData) {
        getDelegate().addPropertyValue(getOwlIndividual(),
                                       Vocabulary.OBJECT_PROPERTY_HASOUTPUTDATA,
                                       newHasOutputData);
    }

    public void removeHasOutputData(DataSource oldHasOutputData) {
        getDelegate().removePropertyValue(getOwlIndividual(),
                                          Vocabulary.OBJECT_PROPERTY_HASOUTPUTDATA,
                                          oldHasOutputData);
    }


}
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

import org.jlab.clara.claraol.State;
import org.jlab.clara.claraol.Vocabulary;
import org.protege.owl.codegeneration.impl.WrappedIndividualImpl;

import org.protege.owl.codegeneration.inference.CodeGenerationInference;

import org.semanticweb.owlapi.model.IRI;


/**
 * Generated by Protege (http://protege.stanford.edu).<br>
 * Source Class: DefaultState <br>
 * @version generated on Tue Dec 22 14:51:01 EST 2015 by gurjyan
 */
public class DefaultState extends WrappedIndividualImpl implements State {

    public DefaultState(CodeGenerationInference inference, IRI iri) {
        super(inference, iri);
    }





    /* ***************************************************
     * Data Property http://claraweb.jlab.org/ontology/2015/11/ClaraOL#hasSeverity
     */
     
    public Collection<? extends Object> getHasSeverity() {
		return getDelegate().getPropertyValues(getOwlIndividual(), Vocabulary.DATA_PROPERTY_HASSEVERITY, Object.class);
    }

    public boolean hasHasSeverity() {
		return !getHasSeverity().isEmpty();
    }

    public void addHasSeverity(Object newHasSeverity) {
	    getDelegate().addPropertyValue(getOwlIndividual(), Vocabulary.DATA_PROPERTY_HASSEVERITY, newHasSeverity);
    }

    public void removeHasSeverity(Object oldHasSeverity) {
		getDelegate().removePropertyValue(getOwlIndividual(), Vocabulary.DATA_PROPERTY_HASSEVERITY, oldHasSeverity);
    }


    /* ***************************************************
     * Data Property http://claraweb.jlab.org/ontology/2015/11/ClaraOL#hasSeverityID
     */
     
    public Collection<? extends Object> getHasSeverityID() {
		return getDelegate().getPropertyValues(getOwlIndividual(), Vocabulary.DATA_PROPERTY_HASSEVERITYID, Object.class);
    }

    public boolean hasHasSeverityID() {
		return !getHasSeverityID().isEmpty();
    }

    public void addHasSeverityID(Object newHasSeverityID) {
	    getDelegate().addPropertyValue(getOwlIndividual(), Vocabulary.DATA_PROPERTY_HASSEVERITYID, newHasSeverityID);
    }

    public void removeHasSeverityID(Object oldHasSeverityID) {
		getDelegate().removePropertyValue(getOwlIndividual(), Vocabulary.DATA_PROPERTY_HASSEVERITYID, oldHasSeverityID);
    }


    /* ***************************************************
     * Data Property http://claraweb.jlab.org/ontology/2015/11/ClaraOL#hasStateName
     */
     
    public Collection<? extends String> getHasStateName() {
		return getDelegate().getPropertyValues(getOwlIndividual(), Vocabulary.DATA_PROPERTY_HASSTATENAME, String.class);
    }

    public boolean hasHasStateName() {
		return !getHasStateName().isEmpty();
    }

    public void addHasStateName(String newHasStateName) {
	    getDelegate().addPropertyValue(getOwlIndividual(), Vocabulary.DATA_PROPERTY_HASSTATENAME, newHasStateName);
    }

    public void removeHasStateName(String oldHasStateName) {
		getDelegate().removePropertyValue(getOwlIndividual(), Vocabulary.DATA_PROPERTY_HASSTATENAME, oldHasStateName);
    }


}
/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.couchdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import static org.eclipse.sw360.datahandler.common.SW360Constants.KEY_ID;
import static org.eclipse.sw360.datahandler.common.SW360Constants.KEY_REV;

/**
 * Mixin class for Jackson serialization into CouchDB. Allows to use objects generated by Thrift directly into CouchDB
 * via Ektorp.
 *
 * @author cedric.bodet@tngtech.com
 */
@JsonPropertyOrder({KEY_ID, KEY_REV})
// Always put _id and _rev upfront. Not required, but serialized objects then look nicer.
@JsonIgnoreProperties({"optionals", "_attachments"})
@SuppressWarnings("unused")
public class DatabaseMixIn {

    @JsonProperty("issetBitfield")
    private byte __isset_bitfield = 0;

    /*
     * Definitions of the standard CouchDB fields
     */

    @JsonProperty(KEY_ID)
    public String getId() {
        return null;
    }

    @JsonProperty(KEY_ID)
    public void setId(String id) {
        // No implementation necessary
    }

    @JsonProperty(KEY_REV)
    public String getRevision() {
        return null;
    }

    @JsonProperty(KEY_REV)
    public void setRevision(String revision) {
        // No implementation necessary
    }

}
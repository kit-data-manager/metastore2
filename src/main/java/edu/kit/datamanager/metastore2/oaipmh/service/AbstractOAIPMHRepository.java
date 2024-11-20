/*
 * Copyright 2016 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.kit.datamanager.metastore2.oaipmh.service;

import edu.kit.datamanager.metastore2.oaipmh.util.OAIPMHBuilder;
import org.openarchives.oai._2.DeletedRecordType;
import org.openarchives.oai._2.DescriptionType;
import org.openarchives.oai._2.GranularityType;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

/**
 * An abstract OAI-PMH repository implementation that can be used to implement
 * specific OAI-PMH repositories according to the OAI-PMH 2.0 protocol.
 *
 * @author jejkal
 */
public abstract class AbstractOAIPMHRepository {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AbstractOAIPMHRepository.class);
    
    private static final String VERSION = "2.0";

    private String name = null;
    private DateFormat df = null;

    /**
     * Default constructor.
     *
     * @param repositoryName The repository name.
     */
    protected AbstractOAIPMHRepository(String repositoryName) {
        name = repositoryName;
    }

    /**
     * Get one or more elements, e.g. a Re3Data XML document, describing the
     * repository. If no such elements are available, an empty list should be
     * returned.
     *
     * @return A list of DescriptionType elements.
     */
    public abstract List<DescriptionType> getRepositoryDescription();

    /**
     * Get information about deleted record support.
     *
     * @return The deleted record type enum value.
     */
    public abstract DeletedRecordType getDeletedRecordSupport();

    /**
     * Return one or more admin email addresses.
     *
     * @return A list of admin email addresses.
     */
    public abstract List<String> getAdminEmail();

    /**
     * Return the earlieste date stamp supported by the repository.
     *
     * @return String the earliest date stamp formatted according to the
     * repositories granularity using the format obtained via {@link #getDateFormat()
     * }.
     */
    public abstract String getEarliestDatestamp();

    /**
     * Get the time resolution granularity of the repository.
     *
     * @return The granularity a GranularityType enum.
     */
    public abstract GranularityType getGranularity();

    /**
     * The repository base url.
     *
     * @return The base url.
     */
    public abstract String getBaseUrl();

    /**
     * Check whether a metadata prefix is supported by the repository or not.
     *
     * @param prefix The prefix to check.
     *
     * @return TRUE if the prefix is supported, false otherwise.
     */
    public abstract boolean isPrefixSupported(String prefix);

    /**
     * Handle the OAI-PMH request defined by the provided builder object. The
     * builder object should already contain the requested verb and the prepared
     * request type information such that the repository only has to add the
     * response content to the builder.
     *
     * Internally, the different methods belonging to the six OAI-PMH verbs,
     * which have to be implemented by each repository, are called.
     *
     * @param builder The OAI-PMH builder containing the request information.
     */
    public void handleRequest(OAIPMHBuilder builder) {
        LOGGER.debug("Handling repository access for verb {}", builder.getVerb());
        switch (builder.getVerb()) {
            case IDENTIFY:
                identify(builder);
                break;
            case LIST_SETS:
                listSets(builder);
                break;
            case LIST_METADATA_FORMATS:
                listMetadataFormats(builder);
                break;
            case LIST_IDENTIFIERS:
                listIdentifiers(builder);
                break;
            case GET_RECORD:
                getRecord(builder);
                break;
            case LIST_RECORDS:
                listRecords(builder);
                break;
        }
    }

    /**
     * Identify this repository. Typically, all information needed to identify
     * the repository are already set by the provided OAIPMHBuilder. There are
     * no modifications to these values at this repository implementation.
     *
     * @param builder The OAIPMHBuilder.
     */
    public abstract void identify(OAIPMHBuilder builder);

    /**
     * List sets. Currently, all digital objects are in the set 'default' so
     * only the set 'default' is returned.
     *
     * @param builder The OAIPMHBuilder.
     */
    public abstract void listSets(OAIPMHBuilder builder);

    /**
     * List all metadata formats according the the parameters defined at the
     * provided OAIPMHBuilder. If the builder contains an identifier, only the
     * metadata formats available for the digital object with the according
     * identifier are returned.
     *
     * For this special repository implementation there is always returned at
     * least the DC_SCHEMA.
     *
     * @param builder The OAIPMHBuilder.
     */
    public abstract void listMetadataFormats(OAIPMHBuilder builder);

    /**
     * List all identifiers according the the parameters defined at the provided
     * OAIPMHBuilder. If no object matches, an according OAI-PMH error is set.
     *
     * @param builder The OAIPMHBuilder.
     */
    public abstract void listIdentifiers(OAIPMHBuilder builder);

    /**
     * Get a single record according the the parameters defined at the provided
     * OAIPMHBuilder. If no object matches, an according OAI-PMH error is set.
     *
     * @param builder The OAIPMHBuilder.
     */
    public abstract void getRecord(OAIPMHBuilder builder);

    /**
     * List all records according the the parameters defined at the provided
     * OAIPMHBuilder.
     *
     * @param builder The OAIPMHBuilder.
     */
    public abstract void listRecords(OAIPMHBuilder builder);

    /**
     * Get the name of the repository.
     *
     * @return The repository name.
     */
    public String getRepositoryName() {
        return name;
    }

    /**
     * Return the supported OAI-PMH protocol version. By default, this is 2.0
     *
     * @return The supported OAI-PMH protocol version.
     */
    public String getProtocolVersion() {
        return VERSION;
    }

    /**
     * Return the date format according to the supported granularity.
     *
     * @return The date format.
     */
    public DateFormat getDateFormat() {
        if (df == null) {
            switch (getGranularity()) {
                case YYYY_MM_DD:
                    df = new SimpleDateFormat("yyyy-MM-dd");
                    break;
                default:
                    df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            }
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        return df;
    }
}

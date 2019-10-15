/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.validation;

import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import java.io.File;
import java.io.InputStream;
import org.springframework.stereotype.Component;

/**
 *
 * @author Torridity
 */
@Component
public interface IValidator {

    boolean supportsSchemaType(MetadataSchemaRecord.SCHEMA_TYPE type);

    boolean isSchemaValid(InputStream schemaStream);

    boolean validateMetadataDocument(File schemaFile, InputStream metadataDocumentStream);
}

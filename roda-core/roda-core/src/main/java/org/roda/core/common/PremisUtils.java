/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.core.common;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.xmlbeans.XmlException;
import org.roda.core.RodaCoreFactory;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.ip.IndexedFile;
import org.roda.core.data.v2.ip.metadata.FileFormat;
import org.roda.core.data.v2.ip.metadata.Fixity;
import org.roda.core.data.v2.ip.metadata.PreservationMetadata.PreservationMetadataType;
import org.roda.core.metadata.MetadataException;
import org.roda.core.metadata.MetadataHelperUtility;
import org.roda.core.metadata.PremisMetadataException;
import org.roda.core.model.ModelService;
import org.roda.core.model.utils.ModelUtils;
import org.roda.core.plugins.Plugin;
import org.roda.core.storage.Binary;
import org.roda.core.storage.ContentPayload;
import org.roda.core.storage.StorageService;
import org.roda.core.storage.StringContentPayload;
import org.roda.core.storage.fs.FSUtils;
import org.roda.core.util.FileUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.util.DateParser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import lc.xmlns.premisV2.AgentComplexType;
import lc.xmlns.premisV2.AgentDocument;
import lc.xmlns.premisV2.AgentIdentifierComplexType;
import lc.xmlns.premisV2.ContentLocationComplexType;
import lc.xmlns.premisV2.CreatingApplicationComplexType;
import lc.xmlns.premisV2.EventComplexType;
import lc.xmlns.premisV2.EventDocument;
import lc.xmlns.premisV2.EventIdentifierComplexType;
import lc.xmlns.premisV2.EventOutcomeDetailComplexType;
import lc.xmlns.premisV2.EventOutcomeInformationComplexType;
import lc.xmlns.premisV2.FixityComplexType;
import lc.xmlns.premisV2.FormatComplexType;
import lc.xmlns.premisV2.FormatDesignationComplexType;
import lc.xmlns.premisV2.FormatRegistryComplexType;
import lc.xmlns.premisV2.LinkingObjectIdentifierComplexType;
import lc.xmlns.premisV2.ObjectCharacteristicsComplexType;
import lc.xmlns.premisV2.ObjectComplexType;
import lc.xmlns.premisV2.ObjectIdentifierComplexType;
import lc.xmlns.premisV2.Representation;
import lc.xmlns.premisV2.StorageComplexType;

public class PremisUtils {
  private final static Logger LOGGER = LoggerFactory.getLogger(PremisUtils.class);
  private static final String W3C_XML_SCHEMA_NS_URI = "http://www.w3.org/2001/XMLSchema";

  public static Fixity calculateFixity(Binary binary, String digestAlgorithm, String originator)
    throws IOException, NoSuchAlgorithmException {
    InputStream dsInputStream = binary.getContent().createInputStream();
    Fixity fixity = new Fixity(digestAlgorithm, FileUtility.calculateChecksumInHex(dsInputStream, digestAlgorithm),
      originator);
    dsInputStream.close();
    return fixity;
  }

  public static Binary addFormatToPremis(Binary preservationFile, FileFormat format)
    throws IOException, XmlException, MetadataException, RequestNotValidException, NotFoundException, GenericException {
    lc.xmlns.premisV2.File f = lc.xmlns.premisV2.File.Factory.parse(preservationFile.getContent().createInputStream());
    if (f.getObjectCharacteristicsList() != null && f.getObjectCharacteristicsList().size() > 0) {
      ObjectCharacteristicsComplexType occt = f.getObjectCharacteristicsList().get(0);
      if (occt.getFormatList() != null && occt.getFormatList().size() > 0) {
        FormatComplexType fct = occt.getFormatList().get(0);
        if (fct.getFormatDesignation() != null) {
          fct.getFormatDesignation().setFormatName(format.getMimeType());
        } else {
          fct.addNewFormatDesignation().setFormatName(format.getMimeType());
        }
      } else {
        FormatComplexType fct = occt.addNewFormat();
        fct.addNewFormatDesignation().setFormatName(format.getMimeType());
      }
    } else {
      ObjectCharacteristicsComplexType occt = f.addNewObjectCharacteristics();
      FormatComplexType fct = occt.addNewFormat();
      fct.addNewFormatDesignation().setFormatName(format.getMimeType());
    }

    Path temp = Files.createTempFile("file", ".premis.xml");

    org.roda.core.metadata.MetadataHelperUtility.saveToFile(f, temp.toFile());
    return (Binary) FSUtils.convertPathToResource(temp.getParent(), temp);
  }

  public static Binary getPremisFile(StorageService storage, String aipID, String representationID, String fileID)
    throws IOException, PremisMetadataException, GenericException, RequestNotValidException, NotFoundException,
    AuthorizationDeniedException {
    Binary binary = storage.getBinary(ModelUtils.getPreservationFilePath(aipID, representationID, fileID));
    return binary;
  }

  public static boolean isPremisV2(Binary binary, Path configBasePath) throws IOException, SAXException {
    boolean premisV2 = true;
    InputStream inputStream = binary.getContent().createInputStream();
    InputStream schemaStream = RodaCoreFactory.getConfigurationFileAsStream("schemas/premis-v2-0.xsd");
    Source xmlFile = new StreamSource(inputStream);
    SchemaFactory schemaFactory = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
    Schema schema = schemaFactory.newSchema(new StreamSource(schemaStream));
    Validator validator = schema.newValidator();
    RodaErrorHandler errorHandler = new RodaErrorHandler();
    validator.setErrorHandler(errorHandler);
    try {
      validator.validate(xmlFile);
      List<SAXParseException> errors = errorHandler.getErrors();
      if (errors.size() > 0) {
        premisV2 = false;
      }
    } catch (SAXException e) {
      premisV2 = false;
    }
    return premisV2;
  }

  public static Binary updatePremisToV3IfNeeded(Binary binary, Path configBasePath) throws IOException, SAXException,
    TransformerException, RequestNotValidException, NotFoundException, GenericException {
    if (isPremisV2(binary, configBasePath)) {
      LOGGER.debug("Binary " + binary.getStoragePath().asString() + " is Premis V2... Needs updated...");
      return updatePremisV2toV3(binary, configBasePath);
    } else {
      return binary;
    }

  }

  private static Binary updatePremisV2toV3(Binary binary, Path configBasePath)
    throws IOException, TransformerException, RequestNotValidException, NotFoundException, GenericException {
    InputStream transformerStream = null;
    InputStream bais = null;

    try {
      Map<String, Object> stylesheetOpt = new HashMap<String, Object>();
      Reader reader = new InputStreamReader(binary.getContent().createInputStream());
      transformerStream = RodaCoreFactory.getConfigurationFileAsStream("crosswalks/migration/v2Tov3.xslt");
      Reader xsltReader = new InputStreamReader(transformerStream);
      CharArrayWriter transformerResult = new CharArrayWriter();
      RodaUtils.applyStylesheet(xsltReader, reader, stylesheetOpt, transformerResult);
      Path p = Files.createTempFile("preservation", ".tmp");
      bais = new ByteArrayInputStream(transformerResult.toString().getBytes("UTF-8"));
      Files.copy(bais, p, StandardCopyOption.REPLACE_EXISTING);

      return (Binary) FSUtils.convertPathToResource(p.getParent(), p);
    } catch (IOException e) {
      throw e;
    } catch (TransformerException e) {
      throw e;
    } finally {
      if (transformerStream != null) {
        try {
          transformerStream.close();
        } catch (IOException e) {

        }
      }
      if (bais != null) {
        try {
          bais.close();
        } catch (IOException e) {

        }
      }
    }
  }

  private static class RodaErrorHandler extends DefaultHandler {
    List<SAXParseException> errors;

    public RodaErrorHandler() {
      errors = new ArrayList<SAXParseException>();
    }

    public void warning(SAXParseException e) throws SAXException {
      errors.add(e);
    }

    public void error(SAXParseException e) throws SAXException {
      errors.add(e);
    }

    public void fatalError(SAXParseException e) throws SAXException {
      errors.add(e);
    }

    public List<SAXParseException> getErrors() {
      return errors;
    }

    public void setErrors(List<SAXParseException> errors) {
      this.errors = errors;
    }

  }

  public static Binary updateFile(Binary preservationFile, IndexedFile file) throws XmlException, IOException {

    FileFormat fileFormat = file.getFileFormat();
    lc.xmlns.premisV2.File f = binaryToFile(preservationFile.getContent().createInputStream());
    if (fileFormat != null) {
      ObjectCharacteristicsComplexType occt = null;
      FormatComplexType fct = null;
      FormatDesignationComplexType fdct = null;
      FormatRegistryComplexType registry = null;
      CreatingApplicationComplexType cact = null;
      if (f.getObjectCharacteristicsList() != null && f.getObjectCharacteristicsList().size() > 0) {
        occt = f.getObjectCharacteristicsList().get(0);
      } else {
        occt = f.addNewObjectCharacteristics();
      }
      if (occt.getFormatList() != null && occt.getFormatList().size() > 0) {
        fct = occt.getFormatList().get(0);
      } else {
        fct = occt.addNewFormat();
      }
      if (fct.getFormatDesignation() != null) {
        fdct = fct.getFormatDesignation();
      } else {
        fdct = fct.addNewFormatDesignation();
      }
      if (!StringUtils.isBlank(fileFormat.getFormatDesignationName())) {
        fdct.setFormatName(fileFormat.getFormatDesignationName());
      }
      if (!StringUtils.isBlank(fileFormat.getFormatDesignationVersion())) {
        fdct.setFormatVersion(fileFormat.getFormatDesignationVersion());
      }
      if (!StringUtils.isBlank(fileFormat.getMimeType())) {
        fdct.setFormatName(fileFormat.getFormatDesignationName());
      }

      if (fct.getFormatRegistry() != null) {
        registry = fct.getFormatRegistry();
      } else {
        registry = fct.addNewFormatRegistry();
      }

      if (!StringUtils.isBlank(fileFormat.getPronom())) {
        registry.setFormatRegistryKey(RodaConstants.PRESERVATION_REGISTRY_PRONOM);
        registry.setFormatRegistryKey(fileFormat.getPronom());
      }
      if (occt.getCreatingApplicationList() != null && occt.getCreatingApplicationList().size() > 0) {
        cact = occt.getCreatingApplicationList().get(0);
      } else {
        cact = occt.addNewCreatingApplication();
      }
      if (!StringUtils.isBlank(file.getCreatingApplicationName())) {
        cact.setCreatingApplicationName(file.getCreatingApplicationName());
      }

      if (!StringUtils.isBlank(file.getCreatingApplicationVersion())) {
        cact.setCreatingApplicationVersion(file.getCreatingApplicationVersion());
      }

      if (!StringUtils.isBlank(file.getDateCreatedByApplication())) {
        cact.setDateCreatedByApplication(file.getDateCreatedByApplication());
      }
    }

    try {
      Path eventPath = Files.createTempFile("file", ".premis.xml");
      MetadataHelperUtility.saveToFile(f, eventPath.toFile());
      return (Binary) FSUtils.convertPathToResource(eventPath.getParent(), eventPath);
    } catch (IOException | MetadataException | GenericException | RequestNotValidException | NotFoundException e) {

    }
    return null;
  }

  public static ContentPayload createPremisEventBinary(String eventID, Date date, String type, String details,
    List<String> sources, List<String> targets, String outcome, String detailNote, String detailExtension,
    String agentID, String agentType) throws GenericException {
    EventDocument event = EventDocument.Factory.newInstance();
    EventComplexType ect = event.addNewEvent();
    EventIdentifierComplexType eict = ect.addNewEventIdentifier();
    eict.setEventIdentifierValue(eventID);
    eict.setEventIdentifierType("local");
    ect.setEventDateTime(DateParser.getIsoDate(date));
    ect.setEventType(type);
    ect.setEventDetail(details);
    if (sources != null) {
      for (String source : sources) {
        LinkingObjectIdentifierComplexType loict = ect.addNewLinkingObjectIdentifier();
        loict.setLinkingObjectIdentifierValue(source);
        loict.setLinkingObjectIdentifierType("source");
      }
    }

    if (targets != null) {
      for (String target : targets) {
        LinkingObjectIdentifierComplexType loict = ect.addNewLinkingObjectIdentifier();
        loict.setLinkingObjectIdentifierValue(target);
        loict.setLinkingObjectIdentifierType("target");
      }
    }
    EventOutcomeInformationComplexType outcomeInformation = ect.addNewEventOutcomeInformation();
    outcomeInformation.setEventOutcome(outcome);
    EventOutcomeDetailComplexType eodct = outcomeInformation.addNewEventOutcomeDetail();
    eodct.setEventOutcomeDetailNote(detailNote);

    // TODO handle...
    /*
     * if(detailExtension!=null){ ExtensionComplexType extension =
     * eodct.addNewEventOutcomeDetailExtension();
     * extension.set(XmlObject.Factory.newValue("<p>"+detailExtension+"</p>"));
     * }
     */
    try {
      return new StringContentPayload(MetadataHelperUtility.saveToString(event, true));
    } catch (MetadataException e) {
      throw new GenericException("Error creating Premis Event", e);
    }
  }

  public static ContentPayload createPremisAgentBinary(String id, String name, String type) throws GenericException {
    AgentDocument agent = AgentDocument.Factory.newInstance();
    
    AgentComplexType act = agent.addNewAgent();
    AgentIdentifierComplexType agentIdentifier = act.addNewAgentIdentifier();
    agentIdentifier.setAgentIdentifierType("local");
    agentIdentifier.setAgentIdentifierValue(id);
    act.addAgentName(name);
    act.setAgentType(type);
    try {
      return new StringContentPayload(MetadataHelperUtility.saveToString(agent, true));
    } catch (MetadataException e) {
      throw new GenericException("Error creating PREMIS agent binary", e);
    }
  }

  public static ContentPayload createBaseRepresentation(String representationId) throws GenericException {
    Representation representation = Representation.Factory.newInstance();
    ObjectIdentifierComplexType oict = representation.addNewObjectIdentifier();
    oict.setObjectIdentifierType("local");
    oict.setObjectIdentifierValue(representationId);
    representation.addNewPreservationLevel().setPreservationLevelValue("");

    try {
      return new StringContentPayload(MetadataHelperUtility.saveToString(representation, true));
    } catch (MetadataException e) {
      throw new GenericException("Error creating base representation", e);
    }
  }

  public static ContentPayload createBaseFile(Binary originalFile)
    throws NoSuchAlgorithmException, IOException, GenericException {
    lc.xmlns.premisV2.File file = lc.xmlns.premisV2.File.Factory.newInstance();
    file.addNewPreservationLevel().setPreservationLevelValue(RodaConstants.PRESERVATION_LEVEL_FULL);
    file.addNewObjectIdentifier().setObjectIdentifierValue(originalFile.getStoragePath().getName());
    ObjectCharacteristicsComplexType occt = file.addNewObjectCharacteristics();
    occt.setCompositionLevel(BigInteger.valueOf(0));
    FixityComplexType fixityMD5 = occt.addNewFixity();
    Fixity md5 = calculateFixity(originalFile, "MD5", "");
    fixityMD5.setMessageDigest(md5.getMessageDigest());
    fixityMD5.setMessageDigestAlgorithm(md5.getMessageDigestAlgorithm());
    fixityMD5.setMessageDigestOriginator(md5.getMessageDigestOriginator());
    occt.setSize(originalFile.getSizeInBytes());
    // occt.addNewObjectCharacteristicsExtension().set("");
    file.addNewOriginalName().setStringValue(originalFile.getStoragePath().getName());
    StorageComplexType sct = file.addNewStorage();
    ContentLocationComplexType clct = sct.addNewContentLocation();
    clct.setContentLocationType("");
    clct.setContentLocationValue("");
    try {
      return new StringContentPayload(MetadataHelperUtility.saveToString(file, true));
    } catch (MetadataException e) {
      throw new GenericException("Error creating base file", e);
    }
  }

  public static List<Fixity> extractFixities(Binary premisFile) {
    // TODO Auto-generated method stub
    return null;
  }

  public static lc.xmlns.premisV2.File binaryToFile(InputStream binaryInputStream) throws XmlException, IOException {
    return (lc.xmlns.premisV2.File) ObjectComplexType.Factory.parse(binaryInputStream);
  }

  public static EventComplexType binaryToEvent(InputStream binaryInputStream) throws XmlException, IOException {
    return EventComplexType.Factory.parse(binaryInputStream);
  }

  public static lc.xmlns.premisV2.Representation binaryToRepresentation(InputStream binaryInputStream)
    throws XmlException, IOException {
    return (Representation) ObjectComplexType.Factory.parse(binaryInputStream);
  }

  public static AgentComplexType binaryToAgent(InputStream binaryInputStream) throws XmlException, IOException {
    return AgentComplexType.Factory.parse(binaryInputStream);
  }

  public static SolrInputDocument updateSolrDocument(SolrInputDocument doc, Binary premisBinary) {
    try {
      lc.xmlns.premisV2.File premisFile = binaryToFile(premisBinary.getContent().createInputStream());
      if (premisFile.getOriginalName() != null) {
        doc.setField(RodaConstants.FILE_ORIGINALNAME, premisFile.getOriginalName());

        // TODO extension
      }
      if (premisFile.getObjectCharacteristicsList() != null && premisFile.getObjectCharacteristicsList().size() > 0) {
        ObjectCharacteristicsComplexType occt = premisFile.getObjectCharacteristicsList().get(0);
        doc.setField(RodaConstants.FILE_SIZE, occt.getSize());
        if (occt.getFixityList() != null && occt.getFixityList().size() > 0) {
          List<String> hashes = new ArrayList<>();
          for (FixityComplexType fct : occt.getFixityList()) {
            StringBuilder fixityPrint = new StringBuilder();
            fixityPrint.append(fct.getMessageDigest());
            fixityPrint.append(" (");
            fixityPrint.append(fct.getMessageDigestAlgorithm());
            if (StringUtils.isNotBlank(fct.getMessageDigestOriginator())) {
              fixityPrint.append(", "); //
              fixityPrint.append(fct.getMessageDigestOriginator());
            }
            fixityPrint.append(")");
            hashes.add(fixityPrint.toString());
          }
          doc.addField(RodaConstants.FILE_HASH, hashes);
        }
        if (occt.getFormatList() != null && occt.getFormatList().size() > 0) {
          FormatComplexType fct = occt.getFormatList().get(0);
          if (fct.getFormatDesignation() != null) {
            doc.addField(RodaConstants.FILE_FILEFORMAT, fct.getFormatDesignation().getFormatName());
            doc.addField(RodaConstants.FILE_FORMAT_VERSION, fct.getFormatDesignation().getFormatVersion());
            doc.addField(RodaConstants.FILE_FORMAT_MIMETYPE, fct.getFormatDesignation().getFormatName());
          }
          if (fct.getFormatRegistry() != null && fct.getFormatRegistry().getFormatRegistryName()
            .equalsIgnoreCase(RodaConstants.PRESERVATION_REGISTRY_PRONOM)) {
            doc.addField(RodaConstants.FILE_PRONOM, fct.getFormatRegistry().getFormatRegistryKey());
          }
          // TODO extension
        }
        if (occt.getCreatingApplicationList() != null && occt.getCreatingApplicationList().size() > 0) {
          CreatingApplicationComplexType cact = occt.getCreatingApplicationList().get(0);
          doc.addField(RodaConstants.FILE_CREATING_APPLICATION_NAME, cact.getCreatingApplicationName());
          doc.addField(RodaConstants.FILE_CREATING_APPLICATION_VERSION, cact.getCreatingApplicationVersion());
          doc.addField(RodaConstants.FILE_DATE_CREATED_BY_APPLICATION, cact.getDateCreatedByApplication());
        }
      }

    } catch (XmlException | IOException e) {

    }
    return doc;
  }

  public static void createPremisAgentBinary(Plugin<?> plugin, String preservationAgentTypeCharacterizationPlugin,
    ModelService model) throws GenericException, NotFoundException, RequestNotValidException, AuthorizationDeniedException {
    //TODO optimize agent creation
    String id = plugin.getClass().getName() + "@" + plugin.getVersion();
    ContentPayload agent;
    agent = PremisUtils.createPremisAgentBinary(id, plugin.getName(),
      RodaConstants.PRESERVATION_AGENT_TYPE_CHARACTERIZATION_PLUGIN);
    model.createPreservationMetadata(PreservationMetadataType.AGENT, null, null, id, agent);

  }
}

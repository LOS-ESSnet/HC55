package eu.europa.ec.eurostat.los.hc55;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.xml.sax.SAXException;

/**
 * The <code>ConceptConverter</code> class allows to convert a SDMX set of concepts into a SKOS concept scheme.
 * 
 * @author Franck
 */
public class ConceptConverter {

	private static Logger logger = LogManager.getLogger(ConceptConverter.class);

	private Document csDocument = null;

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, JDOMException {

		ConceptConverter reader = new ConceptConverter();
		reader.readDocument();
		Model ageModel = reader.convertConceptScheme("CENSUSHUB_CONCEPTS");
		RDFDataMgr.write(new FileOutputStream("src/main/resources/data/cs-census-hub.ttl"), ageModel, Lang.TURTLE);
	}

	/**
	 * Reads the SDMX file containing the concepts into a JDOM document.
	 * 
	 * @throws JDOMException In case of error while parsing the file content.
	 * @throws IOException In case of problem reading the SDMX file.
	 */
	private void readDocument() throws JDOMException, IOException {

		SAXBuilder jdomBuilder = new SAXBuilder();
		csDocument = jdomBuilder.build(new File(Configuration.CONCEPTS));
	}

	/**
	 * Translates a SDMX concepts scheme into a SKOS concept scheme and returns it as a Jena model.
	 * 
	 * @param conceptSchemeId The identifier of the concept scheme in the input SDMX file.
	 * @return The SKOS concept scheme as a Jena model.
	 */
	public Model convertConceptScheme(String conceptSchemeId) {

		// Define and execute the XQuery expression that will select the code lists with the requested identifier
		String query = "//*[(local-name(.) = 'ConceptScheme') and (@id= '" + conceptSchemeId + "')]";
		XPathExpression<Element> expression = XPathFactory.instance().compile(query, Filters.element());
		List<Element> selectedSchemes = expression.evaluate(csDocument);
		if (selectedSchemes.size() == 0) {
			logger.warn("No concept scheme found with identifier " + conceptSchemeId + ", returning null model");
			return null;
		}
		if (selectedSchemes.size() > 1) logger.warn("Several concept schemes have identifier "+ conceptSchemeId + ", returning model for first scheme found");

		Model csModel = ModelFactory.createDefaultModel();
		csModel.setNsPrefix("rdf", RDF.getURI());
		csModel.setNsPrefix("skos", SKOS.getURI());

		Element selectedScheme = selectedSchemes.get(0);
		String csIdentifier = selectedScheme.getAttributeValue("id").trim();
		Element csNameElement = selectedScheme.getChild("Name", Configuration.sdmxStructureNS); // Can there be several elements for different languages?

		// Creation of the SKOS concept scheme
		String schemeURI = Configuration.conceptSchemeURI(csIdentifier, csNameElement.getText().trim());
		logger.info("Creating SKOS concept scheme for SDMX concept scheme " + csNameElement.getText() + " with URI " + schemeURI);
		Resource csResource = csModel.createResource(schemeURI, SKOS.ConceptScheme);
		csResource.addProperty(SKOS.notation, csIdentifier);
		csResource.addProperty(SKOS.prefLabel, Configuration.getLiteral(csNameElement));

		// Filter out the Name child and loop through concepts
		List<Element> listOfConcepts = selectedScheme.getContent(Filters.element("Concept", Configuration.sdmxStructureNS));
		for (Element concept : listOfConcepts) {
			String conceptId = concept.getAttributeValue("id").trim(); // Ignoring coreRepresentation attribute for now
			Element conceptNameElement = concept.getChild("Name", Configuration.sdmxStructureNS);
			logger.info("Creating SKOS concept for concept " + conceptId + " (" + conceptNameElement.getText() + ")");
			String conceptURI = Configuration.conceptURI(csIdentifier, conceptId);
			Resource conceptResource = csModel.createResource(conceptURI, SKOS.Concept);
			conceptResource.addProperty(SKOS.notation, conceptId);
			conceptResource.addProperty(SKOS.prefLabel, Configuration.getLiteral(conceptNameElement));
			conceptResource.addProperty(SKOS.inScheme, csResource);
		}
		logger.info("Concept scheme conversion to SKOS finished, returning resulting model");
		return csModel;
	}
}

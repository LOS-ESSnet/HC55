package eu.europa.ec.eurostat.los.hc55;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
import org.jdom2.util.IteratorIterable;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.xml.sax.SAXException;

/**
 * The <code>CodeListConverter</code> class allows to convert a SDMX code list into a SKOS concept scheme.
 * 
 * @author Franck
 */
public class CodeListConverter {

	private static Logger logger = LogManager.getLogger(CodeListConverter.class);

	private Document clDocument = null;

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, JDOMException {

		CodeListConverter reader = new CodeListConverter();
		reader.readDocument();
		Model ageModel = reader.convertCodeList("CL_AGE");
		RDFDataMgr.write(new FileOutputStream("src/main/resources/data/cl-age.ttl"), ageModel, Lang.TURTLE);
	}

	/**
	 * Reads the SDMX file containing the code lists into a JDOM document.
	 * 
	 * @throws JDOMException In case of error while parsing the file content.
	 * @throws IOException In case of problem reading the SDMX file.
	 */
	private void readDocument() throws JDOMException, IOException {

		SAXBuilder jdomBuilder = new SAXBuilder();
		clDocument = jdomBuilder.build(new File(Configuration.NON_GEO_CODELISTS));
	}

	/**
	 * Translates a SDMX code list into a SKOS concept scheme and returns it as a Jena model.
	 * 
	 * @param codeListId The identifier of the code list in the input SDMX file.
	 * @return The SKOS concept scheme as a Jena model.
	 */
	public Model convertCodeList(String codeListId) {

		// TODO Add concept corresponding to class

		// Define and execute the XQuery expression that will select the code lists with the requested identifier
		String query = "//*[(local-name(.) = 'CodeList') and (@id= '" + codeListId + "')]";
		XPathExpression<Element> expression = XPathFactory.instance().compile(query, Filters.element());
		List<Element> selectedLists = expression.evaluate(clDocument);
		if (selectedLists.size() == 0) {
			logger.warn("No code list found with identifier " + codeListId + ", returning null model");
			return null;
		}
		if (selectedLists.size() > 1) logger.warn("Several code lists have identifier "+ codeListId + ", returning model for first list found");

		Model clModel = ModelFactory.createDefaultModel();
		clModel.setNsPrefix("rdf", RDF.getURI());
		clModel.setNsPrefix("skos", SKOS.getURI());

		Element selectedList = selectedLists.get(0);
		String clIdentifier = selectedList.getAttributeValue("id").trim();
		Element clNameElement = selectedList.getChild("Name", Configuration.sdmxStructureNS); // Can there be several elements for different languages?

		// Creation of the SKOS concept scheme
		String schemeURI = Configuration.codeListURI(clIdentifier, clNameElement.getText().trim());
		logger.info("Creating SKOS concept scheme for code list " + clNameElement.getText() + " with URI " + schemeURI);
		Resource clResource = clModel.createResource(schemeURI, SKOS.ConceptScheme);
		clResource.addProperty(SKOS.notation, clIdentifier);
		clResource.addProperty(SKOS.prefLabel, Configuration.getLiteral(clNameElement));

		// Filter out the Name child and loop through codes
		List<Element> listOfCodes = selectedList.getContent(Filters.element("Code", Configuration.sdmxStructureNS));
		for (Element code : listOfCodes) {
			String codeValue = code.getAttributeValue("value").trim();
			String parentCodeValue = code.getAttributeValue("parentCode").trim();
			Element codeDescriptionElement = code.getChild("Description", Configuration.sdmxStructureNS);
			logger.info("Creating SKOS concept for code " + codeValue + " (" + codeDescriptionElement.getText() + ")");
			String codeURI = Configuration.codeURI(clIdentifier, codeValue);
			Resource codeResource = clModel.createResource(codeURI, SKOS.Concept);
			codeResource.addProperty(SKOS.notation, codeValue);
			codeResource.addProperty(SKOS.prefLabel, Configuration.getLiteral(codeDescriptionElement));
			codeResource.addProperty(SKOS.inScheme, clResource);
			if (parentCodeValue.length() == 0) {
				codeResource.addProperty(SKOS.topConceptOf, clResource);
				clResource.addProperty(SKOS.hasTopConcept, codeResource);
			} else {
				Resource parentCodeResource = clModel.createResource(Configuration.codeURI(clIdentifier, parentCodeValue));
				codeResource.addProperty(SKOS.broader, parentCodeResource);
				parentCodeResource.addProperty(SKOS.narrower, codeResource);
			}
		}
		logger.info("Code list conversion to SKOS finished, returning resulting model");
		return clModel;
	}

	/**
	 * Returns the list of the code lists defined in the current file.
	 * 
	 * @return The list of the code list identifiers as a <code>List</code> of strings.
	 */
	public List<String> listCodeLists() {

		List<String> listOfLists = new ArrayList<String>();
		Element root = clDocument.getRootElement();
		IteratorIterable<Element> cl = root.getDescendants(Filters.element("CodeList", Configuration.sdmxStructureNS));
		cl.forEach(new Consumer<Element>() {
			@Override
			public void accept(Element element) {
				listOfLists.add(element.getAttributeValue("id"));
			}
		});
		return listOfLists;
	}
}

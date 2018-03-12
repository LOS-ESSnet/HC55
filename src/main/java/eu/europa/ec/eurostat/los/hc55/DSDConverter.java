package eu.europa.ec.eurostat.los.hc55;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.XSD;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import eu.europa.ec.eurostat.los.utils.DataCube;

/**
 * The <code>DSDConverter</code> class allows to convert a SDMX Key Family (name of the DSD in SDMX 2.0) into a Data Cube DSD.
 * 
 * @author Franck
 */
public class DSDConverter {

	private static Logger logger = LogManager.getLogger(DSDConverter.class);

	/** Correspondence between the SDMX component types and the Data Cube property classes */
	private static final Map<String, Resource> componentClassMappings = new HashMap<String, Resource>();
	static {
		componentClassMappings.put("Dimension", DataCube.DimensionProperty);
		componentClassMappings.put("TimeDimension", DataCube.DimensionProperty);
		componentClassMappings.put("PrimaryMeasure", DataCube.MeasureProperty);
		componentClassMappings.put("Attribute", DataCube.AttributeProperty);
	}
	/** Correspondence between the SDMX component types and the Data Cube properties */
	private static final Map<String, Property> componentPropertyMappings = new HashMap<String, Property>();
	static {
		componentPropertyMappings.put("Dimension", DataCube.dimension);
		componentPropertyMappings.put("TimeDimension", DataCube.dimension);
		componentPropertyMappings.put("PrimaryMeasure", DataCube.measure);
		componentPropertyMappings.put("Attribute", DataCube.attribute);
	}

	public static void main(String[] args) throws Exception {

		DSDConverter dsdConverter = new DSDConverter();
		Model hc55DSDModel = dsdConverter.convertDSD("HC55", new ArrayList<String>());
		RDFDataMgr.write(new FileOutputStream("src/main/resources/data/dsd-hc55.ttl"), hc55DSDModel, Lang.TURTLE);
	}

	/**
	 * Converts a SDMX key family into a Data Cube data structure definition.
	 * 
	 * @param dsdId The identifier of the key family in the input SDMX file.
	 * @param excludedComponents List of identifiers of components that will not be included in the DSD.
	 * @return The Data Cube data structure definition as a Jena model.
	 * @throws IOException In case of problem reading the SDMX file.
	 * @throws JDOMException In case of error while parsing the file content.
	 */
	public Model convertDSD(String dsdId, List<String> excludedComponents) throws JDOMException, IOException {

		SAXBuilder jdomBuilder = new SAXBuilder();
		Document dsdDocument = jdomBuilder.build(new File(Configuration.KEY_FAMILIES));

		// We will need the Census Hub concepts in order to link components to concepts and give them better names
		ConceptConverter conceptConverter = new ConceptConverter();
		Model conceptModel = conceptConverter.convertConceptScheme(Configuration.CH_CONCEPT_SCHEME_ID);
		// And we will also need the list of concepts which are coded
		Map<String, String> codedConcepts = conceptConverter.getCodedConcepts();

		// Define and execute the XQuery expression that will select the code lists with the requested identifier
		String query = "//*[(local-name(.) = 'KeyFamily') and (@id= '" + dsdId + "')]";
		XPathExpression<Element> expression = XPathFactory.instance().compile(query, Filters.element());

		List<Element> selectedDSDs = expression.evaluate(dsdDocument);
		if (selectedDSDs.size() == 0) {
			logger.warn("No key family found with identifier " + dsdId + ", returning null model");
			//return null;
		}
		if (selectedDSDs.size() > 1) logger.warn("Several key families have identifier " + dsdId + ", returning model for first key family found");

		Model dsdModel = ModelFactory.createDefaultModel();
		dsdModel.setNsPrefix("rdf", RDF.getURI());
		dsdModel.setNsPrefix("rdfs", RDFS.getURI());
		dsdModel.setNsPrefix("xs", XSD.getURI());
		dsdModel.setNsPrefix("qb", DataCube.getURI());

		Element selectedDSD = selectedDSDs.get(0);
		String dsdIdentifier = selectedDSD.getAttributeValue("id").trim();
		Element dsdNameElement = selectedDSD.getChild("Name", Configuration.sdmxStructureNS);

		// Creation of the DSD
		String dsdURI = Configuration.dsdURI(dsdIdentifier);
		logger.info("Creating DSD " + dsdNameElement.getText() + " with URI " + dsdURI);
		Resource dsdResource = dsdModel.createResource(dsdURI, DataCube.DataStructureDefinition);
		dsdResource.addProperty(RDFS.label, Configuration.getLiteral(dsdNameElement));

		// List SDMX components, create corresponding DC components and attach them to the DSD
		List<Element> components = selectedDSD.getChild("Components", Configuration.sdmxStructureNS).getChildren();
		int dimensionOrder = 1;
		for (Element component : components) {
			String componentType = component.getName();
			String conceptIdentifier = component.getAttributeValue("conceptRef");
			logger.debug("Found SDMX component of type " + componentType + " referring to concept " + conceptIdentifier);
			if (excludedComponents.contains(conceptIdentifier)) {
				logger.debug("This component is excluded from the Data Cube DSD");
				continue;
			}
			// Create the property corresponding to the SDMX component
			String propertyURI = Configuration.componentURI(conceptIdentifier, componentType);
			Resource componentPropertyClass = dsdModel.createResource(propertyURI, componentClassMappings.get(componentType));
			componentPropertyClass.addProperty(RDF.type, RDF.Property);
			if ((conceptIdentifier != null) && (conceptIdentifier.length() > 0)) {
				String conceptURI = Configuration.conceptURI(conceptIdentifier);
				Resource conceptResource = conceptModel.createResource(conceptURI);
				componentPropertyClass.addProperty(DataCube.concept, conceptResource);
				StmtIterator conceptLabels = conceptModel.listStatements(conceptResource, SKOS.prefLabel, null, "en");
				if (conceptLabels.hasNext()) {
					String conceptLabel = conceptLabels.next().getString();
					componentPropertyClass.addProperty(RDFS.label, dsdModel.createLiteral(conceptLabel, "en"));
				}
				conceptLabels.close(); // TODO Deal with the case where there are labels in different languages
				// If the concept has a coded core representation, the component property is also a coded property
				if (codedConcepts.containsKey(conceptIdentifier)) {
					componentPropertyClass.addProperty(RDF.type, DataCube.CodedProperty);
					componentPropertyClass.addProperty(DataCube.codeList, dsdModel.createResource(Configuration.codeListURI(codedConcepts.get(conceptIdentifier), null)));
				}
			}

			// Attach the property to the DSD via an anonymous ComponentSpecification node
			Resource blankCS = dsdModel.createResource(DataCube.ComponentSpecification).addProperty(componentPropertyMappings.get(componentType), componentPropertyClass);
			// For dimensions, add the order attribute
			if (componentClassMappings.get(componentType).equals(DataCube.DimensionProperty)) {
				blankCS.addProperty(DataCube.order, dsdModel.createTypedLiteral(dimensionOrder));
				dimensionOrder++;
			}
			dsdResource.addProperty(DataCube.component, blankCS);
		}

		conceptModel.close();
		return dsdModel;
	}
}

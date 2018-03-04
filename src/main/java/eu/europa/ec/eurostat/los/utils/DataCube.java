package eu.europa.ec.eurostat.los.utils;

import org.apache.jena.rdf.model.Model ;
import org.apache.jena.rdf.model.ModelFactory ;
import org.apache.jena.rdf.model.Property ;
import org.apache.jena.rdf.model.Resource ;

/**
 * Vocabulary definition for the 
 * <a href="https://www.w3.org/TR/vocab-data-cube/">W3C Data Cube Recommendation</a>.
 */
public class DataCube {
	/**
	 * The RDF model that holds the Data Cube entities
	 */
	private static final Model m = ModelFactory.createDefaultModel();
	/**
	 * The namespace of the Data Cube vocabulary as a string
	 */
	public static final String uri = "http://purl.org/linked-data/cube#";
	/**
	 * Returns the namespace of the Data Cube schema as a string
	 * @return the namespace of the Data Cube schema
	 */
	public static String getURI() {
		return uri;
	}
	/**
	 * The namespace of the Data Cube vocabulary
	 */
	public static final Resource NAMESPACE = m.createResource(uri);
	/* ##########################################################
	 * Defines Data Cube Classes
	   ########################################################## */
	public static final Resource Attachable = m.createResource(uri + "Attachable");
	public static final Resource AttributeProperty = m.createResource(uri + "AttributeProperty");
	public static final Resource CodedProperty = m.createResource(uri + "CodedProperty");
	public static final Resource ComponentProperty = m.createResource(uri + "ComponentProperty");
	public static final Resource ComponentSet = m.createResource(uri + "ComponentSet");
	public static final Resource ComponentSpecification = m.createResource(uri + "ComponentSpecification");
	public static final Resource DataSet = m.createResource(uri + "DataSet");
	public static final Resource DataStructureDefinition = m.createResource(uri + "DataStructureDefinition");
	public static final Resource DimensionProperty = m.createResource(uri + "DimensionProperty");
	public static final Resource HierarchicalCodeList = m.createResource(uri + "HierarchicalCodeList");
	public static final Resource MeasureProperty = m.createResource(uri + "MeasureProperty");
	public static final Resource Observation = m.createResource(uri + "Observation");
	public static final Resource Slice = m.createResource(uri + "Slice");
	public static final Resource ObservationGroup = m.createResource(uri + "ObservationGroup");
	public static final Resource SliceKey = m.createResource(uri + "SliceKey");
	/* ##########################################################
	 * Defines Data Cube Properties
	   ########################################################## */

	// Data properties
	public static final Property componentRequired = m.createProperty(uri + "componentRequired");
	public static final Property order = m.createProperty(uri + "order");
	// Object properties
	public static final Property attribute = m.createProperty(uri + "attribute");
	public static final Property codeList = m.createProperty(uri + "codeList");
	public static final Property component = m.createProperty(uri + "component");
	public static final Property componentAttachment = m.createProperty(uri + "componentAttachment");
	public static final Property componentProperty = m.createProperty(uri + "componentProperty");
	public static final Property concept = m.createProperty(uri + "concept");
	public static final Property dataSet = m.createProperty(uri + "dataSet");
	public static final Property dimension = m.createProperty(uri + "dimension");
	public static final Property hierarchyRoot = m.createProperty(uri + "hierarchyRoot");
	public static final Property measure = m.createProperty(uri + "measure");
	public static final Property measureDimension = m.createProperty(uri + "measureDimension");
	public static final Property measureType = m.createProperty(uri + "measureType");
	public static final Property observation = m.createProperty(uri + "observation");
	public static final Property observationGroup = m.createProperty(uri + "observationGroup");
	public static final Property parentChildProperty = m.createProperty(uri + "parentChildProperty");
	public static final Property slice = m.createProperty(uri + "slice");
	public static final Property sliceKey = m.createProperty(uri + "sliceKey");
	public static final Property sliceStructure = m.createProperty(uri + "sliceStructure");
	public static final Property structure = m.createProperty(uri + "structure");
}

package eu.europa.ec.eurostat.los.hc55;

import java.net.URI;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ResourceFactory;
import org.jdom2.Element;
import org.jdom2.Namespace;

import com.sun.xml.internal.ws.util.StringUtils;

public class Configuration {

	/** URI of the SDMX file containing the code lists (except geographical) */
	public final static URI NON_GEO_CODELISTS_URI = URI.create("https://circabc.europa.eu/sd/a/98c7429f-d350-4bdf-91c5-3fc3b42a286e/NonGeoCodeLists%252bESTAT%252b1.0(0).xml");
	public final static String NON_GEO_CODELISTS = "src/main/resources/data/NonGeoCodeLists+ESTAT+1.0(0).xml";

	/** Name of the file containing the Census Hub DSDs (or key families, since they are SDMX 2.0) */
	public final static String KEY_FAMILIES = "src/main/resources/data/CENSUSHUB+ESTAT+KEYFAMILIES+1.0.xml";

	/** Name of the file containing the Census Hub concepts */
	public final static String CONCEPTS = "src/main/resources/data/CENSUSHUB_CONCEPTS+ESTAT+1.0.xml";

	/** Identifier of the generic concept scheme for the Census Hub */
	public final static String CH_CONCEPT_SCHEME_ID = "CENSUSHUB_CONCEPTS";

	public static final String BASE_URI = "http://linked-open-statistics.org/metadata/";
	public static Namespace sdmxStructureNS = Namespace.getNamespace("structure", "http://www.SDMX.org/resources/SDMXML/schemas/v2_0/structure");

	/** Naming methods */
	public static String conceptSchemeURI(String conceptSchemeId, String conceptSchemeName) {
		return BASE_URI + "concepts/" + componentName(conceptSchemeId.toLowerCase()) + "/scheme";	
	}

	public static String conceptURI(String conceptSchemeId, String conceptId) {
		return BASE_URI + "concepts/" + componentName(conceptSchemeId.toLowerCase()) + "/" + componentName(conceptId.toLowerCase());
	}

	public static String conceptURI(String conceptId) {
		return conceptURI(CH_CONCEPT_SCHEME_ID, conceptId);
	}

	public static String codeListURI(String codeListId, String codeListName) {
		return BASE_URI + "codes/" + componentName(codeListId.toLowerCase()) + "/list";	
	}

	public static String codeURI(String codeListId, String codeId) {
		return BASE_URI + "codes/" + componentName(codeListId.toLowerCase()) + "/" + componentName(codeId.toLowerCase());
	}

	public static String dsdURI(String dsdId) {
		return BASE_URI + "structure/dsd/" + componentName(dsdId);
	}

	public static String componentURI(String componentId, String componentType) {
		return BASE_URI + "structure/" + componentName(componentType) + "/" + componentName(componentId);
	}

	/**
	 * Computes the name of a DSD component property from the name of the associated SDMX concept.
	 * For example: OBS_STATUS -> obsStatus
	 * 
	 * @param sdmxConceptId The name of the SDMX concept associated to the component.
	 * @return The name of the DSD component property.
	 */
	public static String componentName(String sdmxConceptId) {

		String[] terms = sdmxConceptId.split("_");
		String name = "";
		for (String term : terms) {
			name += StringUtils.capitalize(term.toLowerCase());
		}
		return StringUtils.decapitalize(name);
	}

	/**
	 * Creates a language-tagged RDF literal from an JDOM element.
	 * The text value of the element will be used as content for the literal, and the value of the xml:lang attribute as the language tag.
	 * 
	 * @param element The JDOM element to use to create the literal.
	 * @return The RDF literal as a Jena <code>Literal</code>.
	 */
	protected static Literal getLiteral(Element element) {
		return ResourceFactory.createLangLiteral(element.getText(), Configuration.getLanguage(element));
	}

	/**
	 * Gets the language tag of a JDOM element.
	 * 
	 * @param element The JDOM element.
	 * @return The value of the xml:lang attribute.
	 */
	protected static String getLanguage(Element element) {
		return element.getAttributeValue("lang", Namespace.getNamespace("xml", "http://www.w3.org/XML/1998/namespace"));
	}
}

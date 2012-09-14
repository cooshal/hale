//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-558 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.09.14 at 10:27:20 AM CEST 
//


package eu.esdihumboldt.hale.io.oml.internal.model.generated.oml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for RelationType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="RelationType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omwg.org/TR/d7/ontology/alignment}EntityType">
 *       &lt;sequence>
 *         &lt;element name="relationComposition" type="{http://www.omwg.org/TR/d7/ontology/alignment}RelationCompositionType" minOccurs="0"/>
 *         &lt;element ref="{http://www.omwg.org/TR/d7/ontology/alignment}domainRestriction"/>
 *         &lt;element ref="{http://www.omwg.org/TR/d7/ontology/alignment}rangeRestriction"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RelationType", propOrder = {
    "relationComposition",
    "domainRestriction",
    "rangeRestriction"
})
public class RelationType
    extends EntityType
{

    protected RelationCompositionType relationComposition;
    @XmlElement(required = true)
    protected DomainRestrictionType domainRestriction;
    @XmlElement(required = true)
    protected RangeRestrictionType rangeRestriction;

    /**
     * Gets the value of the relationComposition property.
     * 
     * @return
     *     possible object is
     *     {@link RelationCompositionType }
     *     
     */
    public RelationCompositionType getRelationComposition() {
        return relationComposition;
    }

    /**
     * Sets the value of the relationComposition property.
     * 
     * @param value
     *     allowed object is
     *     {@link RelationCompositionType }
     *     
     */
    public void setRelationComposition(RelationCompositionType value) {
        this.relationComposition = value;
    }

    /**
     * Gets the value of the domainRestriction property.
     * 
     * @return
     *     possible object is
     *     {@link DomainRestrictionType }
     *     
     */
    public DomainRestrictionType getDomainRestriction() {
        return domainRestriction;
    }

    /**
     * Sets the value of the domainRestriction property.
     * 
     * @param value
     *     allowed object is
     *     {@link DomainRestrictionType }
     *     
     */
    public void setDomainRestriction(DomainRestrictionType value) {
        this.domainRestriction = value;
    }

    /**
     * Gets the value of the rangeRestriction property.
     * 
     * @return
     *     possible object is
     *     {@link RangeRestrictionType }
     *     
     */
    public RangeRestrictionType getRangeRestriction() {
        return rangeRestriction;
    }

    /**
     * Sets the value of the rangeRestriction property.
     * 
     * @param value
     *     allowed object is
     *     {@link RangeRestrictionType }
     *     
     */
    public void setRangeRestriction(RangeRestrictionType value) {
        this.rangeRestriction = value;
    }

}

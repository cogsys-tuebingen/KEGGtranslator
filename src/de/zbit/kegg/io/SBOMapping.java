/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of KEGGtranslator, a program to convert KGML files
 * from the KEGG database into various other formats, e.g., SBML, GML,
 * GraphML, and many more. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/KEGGtranslator> to
 * obtain the latest version of KEGGtranslator.
 *
 * Copyright (C) 2010-2012 by the University of Tuebingen, Germany.
 *
 * KEGGtranslator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.kegg.io;

import java.util.HashMap;
import java.util.Map;

import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.EntryType;
import de.zbit.kegg.parser.pathway.Relation;
import de.zbit.kegg.parser.pathway.SubType;
import de.zbit.kegg.parser.pathway.ext.EntryExtended;
import de.zbit.kegg.parser.pathway.ext.GeneType;
import de.zbit.util.StringUtil;

/**
 * This static class defines how to map from certain
 * {@link EntryType}s or {@link GeneType}s to SBO
 * terms.
 * 
 * @author Clemens Wrzodek
 * @author Finja B&uuml;chel
 * @version $Rev$
 */
public class SBOMapping {
  
  /* **********************************
   * ENTRY TYPE MAPPINGS
   ***********************************/

  /**
   * SBO Term for EntryType "general modifier" (compound, map, other).
   */
  public static int ET_GeneralModifier2SBO = 13; // 13=catalyst // 460="enzymatic catalyst"
  /**
   * SBO Term for EntryType enzyme, gene, group, ortholog, genes.
   */
  public static int ET_EnzymaticModifier2SBO = 460;
  /**
   * SBO Term for EntryType Ortholog.
   */
  public static int ET_Ortholog2SBO = 354; // 354="informational molecule segment"
  /**
   * SBO Term for EntryType Enzyme.
   */
  public static int ET_Enzyme2SBO = 245; // 245="macromolecule",	// 252="polypeptide chain"
  /**
   * SBO Term for EntryType Gene.
   */
  public static int ET_Gene2SBO = 354; // 354="informational molecule segment"
  /**
   * SBO Term for EntryType Group.
   */
  public static int ET_Group2SBO = 253; // 253="non-covalent complex"
  /**
   * SBO Term for EntryType Compound.
   */
  public static int ET_Compound2SBO = 247; // 247="Simple Chemical"
  /**
   * SBO Term for EntryType Map.
   */
  public static int ET_Map2SBO = 552; // 552="reference annotation"
  /**
   * SBO Term for EntryType Other.
   */
  public static int ET_Other2SBO = 285; // 285="material entity of unspecified nature"
  
  
  
  
  /* **********************************
   * GENE TYPE MAPPINGS
   * NOTE: The missing ones are already covered by the
   * EntryType!
   ***********************************/
  
  /**
   * SBO Term for GeneType Protein.
   */
  public static int GT_Protein2SBO = 252; // 252="polypeptide chain"
  
  /**
   * SBO Term for GeneType DNA.
   */
  public static int GT_DNA2SBO = 251; // 251="deoxyribonucleic acid"
  
  /**
   * SBO Term for GeneType DNARegion.
   */
  public static int GT_DNARegion2SBO = 251; // 251="deoxyribonucleic acid"
  
  /**
   * SBO Term for GeneType RNA.
   */
  public static int GT_RNA2SBO = 250; // 252="ribonucleic acid"
  
  /**
   * SBO Term for GeneType RNARegion.
   */
  public static int GT_RNARegion2SBO = 250; // 252="ribonucleic acid"
  
  
  /**
   * A map to translate {@link SubType}s of {@link Relation}s to SBO terms
   */
  private static Map<String, Integer> subtype2SBO = new HashMap<String, Integer>();
  
  static {
    // Init subtype map
    subtype2SBO.put(SubType.ACTIVATION, 170); // = stimulation
    subtype2SBO.put(SubType.ASSOCIATION, 177); // = non-covalent binding
    subtype2SBO.put(SubType.BINDING, 177);
    subtype2SBO.put(SubType.BINDING_ASSOCIATION, 177);
    subtype2SBO.put(SubType.DEPHOSPHORYLATION, 330); // dephosphorylation
    subtype2SBO.put(SubType.DISSOCIATION, 177);
    subtype2SBO.put(SubType.EXPRESSION, 170); 
    subtype2SBO.put(SubType.GLYCOSYLATION, 217); // glycosylation
    subtype2SBO.put(SubType.INDIRECT_EFFECT, 344); // molecular interaction
    subtype2SBO.put(SubType.INHIBITION, 169);
    subtype2SBO.put(SubType.METHYLATION, 214); // methylation
    subtype2SBO.put(SubType.MISSING_INTERACTION, 396);  // uncertain process
    subtype2SBO.put(SubType.PHOSPHORYLATION, 216); // phosphorylation
    subtype2SBO.put(SubType.REPRESSION, 169);
    subtype2SBO.put(SubType.STATE_CHANGE, 168); // control
    subtype2SBO.put(SubType.UBIQUITINATION, 224); // ubiquitination
  }
  
  /**
   * Get the most appropriate SBO term for this
   * <code>entry</code>.
   * @param entry
   * @return
   */
  public static int getSBOTerm(Entry entry) {
    if (entry instanceof EntryExtended && 
        ((EntryExtended) entry).isSetGeneType()) {
      GeneType type = ((EntryExtended) entry).getGeneType();
      
      if (type.equals(GeneType.protein)) {
        return GT_Protein2SBO;
      } else if (type.equals(GeneType.dna)) {
        return GT_DNA2SBO;
      } else if (type.equals(GeneType.dna_region)) {
        return GT_DNARegion2SBO;
      } else if (type.equals(GeneType.rna)) {
        return GT_RNA2SBO;
      } else if (type.equals(GeneType.rna_region)) {
        return GT_RNARegion2SBO;
      } else {
        // GeneType is NOT mandatory and just additionally
        // to the entryType!
        return getSBOTerm(entry.getType());
      }
    } else {
      return getSBOTerm(entry.getType());
    }
  }
  
  
  /**
   * Returns the SBO Term for an EntryType.
   * @param type the KEGG EntryType, you want an SBMO Term for.
   * @return SBO Term (integer).
   */
  private static int getSBOTerm(EntryType type) {
    if (type.equals(EntryType.compound))
      return ET_Compound2SBO;
    if (type.equals(EntryType.enzyme))
      return ET_Enzyme2SBO;
    if (type.equals(EntryType.gene))
      return ET_Gene2SBO;
    if (type.equals(EntryType.group))
      return ET_Group2SBO;
    if (type.equals(EntryType.genes))
      return ET_Group2SBO;
    if (type.equals(EntryType.map))
      return ET_Map2SBO;
    if (type.equals(EntryType.ortholog))
      return ET_Ortholog2SBO;
    
    if (type.equals(EntryType.other))
      return ET_Other2SBO;
    
    return ET_Other2SBO;
  }


  /**
   * Get an SBO term for a {@link SubType}.
   * Typically, {@link SubType}s are used in {@link Relation}s.
   * @param subtype one of the constants, defined in the {@link SubType} class.
   * @return appropriate SBO term.
   */
  public static int getSBOTerm(String subtype) {
    // NOTE: It is intended to return -1 for "compound"!
    Integer ret = subtype2SBO.get(subtype);
    if (ret==null) ret = -1;
    return ret;
  }
  

  /**
   * Formats an SBO term. E.g. "177" to "SBO:0000177".
   * @param sbo
   * @return
   */
  public static String formatSBO(int sbo) {
    return formatSBO(sbo, "SBO:");
  }
  
  /**
   * Formats an SBO term. E.g. "177" to "SBO%3A0000177".
   * Uses HTML encoding %3A to encode the double point.
   * @param sbo
   * @return
   */
  public static String formatSBOforMIRIAM(int sbo) {
    return formatSBO(sbo, "SBO%3A");
  }
  
  /**
   * Formats an SBO term to contain 7 digits after an
   * arbitrary prefix. E.g., "177" and prefix "SBO%3A"
   * to "SBO%3A0000177".
   * @param sbo
   * @param prefix
   * @return
   */
  private static String formatSBO(int sbo, String prefix) { 
    StringBuilder b = new StringBuilder(prefix);
    String iString = Integer.toString(sbo);
    b.append(StringUtil.replicateCharacter('0', 7-iString.length()));
    b.append(iString);
    return b.toString();
  }
  
}

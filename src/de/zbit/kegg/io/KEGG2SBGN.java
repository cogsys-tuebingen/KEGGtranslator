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
 * Copyright (C) 2011 by the University of Tuebingen, Germany.
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.sbgn.SbgnUtil;
import org.sbgn.bindings.Arc;
import org.sbgn.bindings.Arc.End;
import org.sbgn.bindings.Arc.Start;
import org.sbgn.bindings.Bbox;
import org.sbgn.bindings.Glyph;
import org.sbgn.bindings.Glyph.Clone;
import org.sbgn.bindings.Glyph.Port;
import org.sbgn.bindings.Glyph.State;
import org.sbgn.bindings.Label;
import org.sbgn.bindings.ObjectFactory;
import org.sbgn.bindings.Sbgn;

import de.zbit.graph.io.def.SBGNProperties;
import de.zbit.graph.io.def.SBGNProperties.ArcType;
import de.zbit.graph.io.def.SBGNProperties.GlyphType;
import de.zbit.kegg.api.KeggInfos;
import de.zbit.kegg.api.cache.KeggInfoManagement;
import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.kegg.parser.pathway.Reaction;
import de.zbit.kegg.parser.pathway.ReactionComponent;
import de.zbit.kegg.parser.pathway.Relation;
import de.zbit.kegg.parser.pathway.SubType;

/**
 * A (not yet fully implemented) implementation of KEGG2SBGN.
 * 
 * <p>
 * Note:<br/>
 * Martijn and Manuel should be mentioned at least in 'Acknowledgments', in case
 * of a publication of this method.
 * </p>
 * 
 * @author Manuel Ruff
 * @author Clemens Wrzodek
 * @author Martijn van Iersel
 * @author Andreas Dr&auml;ger
 * @date 2011-04-22
 * @version $Rev$
 */
public class KEGG2SBGN extends AbstractKEGGtranslator<Sbgn> {

	private ObjectFactory objectFactory = new ObjectFactory();

	private Sbgn sbgn = objectFactory.createSbgn();
	private org.sbgn.bindings.Map map = objectFactory.createMap();
	private HashMap<Glyph, Integer> glyphNames = new HashMap<Glyph, Integer>();
	private int id = 0;

	/**
	 * Constructor
	 * 
	 * @param manager
	 */
	public KEGG2SBGN(KeggInfoManagement manager) {
		super(manager);
	}

	@Override
	protected Sbgn translateWithoutPreprocessing(Pathway p) {

		// set the map
		sbgn.setMap(map);

		// for every entry in the pathway
		handleAllEntries(p);

		// for every relation in the pathway
		if (this.considerRelations())
			handleAllRelations(p);

		// for every reaction in the pathway
		if (this.considerReactions())
			handleAllReactions(p);

		return sbgn;
	}

	/**
	 * Transform all the Entries from KEGG to SBGN {@link Glyph}s
	 * 
	 * @param p
	 *            Pathway
	 */
	private void handleAllEntries(Pathway p) {

		// map for the entries to determine if there occurs an entry twice or more to make them a clonemarker
		Map<String, Entry> handlesEntries = new HashMap<String, Entry>();

		// for every entry
		for (Entry e : p.getEntries()) {
			// create a glyph with the proper id
			Glyph g = createGlyphWithID();

			// check if the entry is already in use
			if (handlesEntries.containsKey(e.getName())) {
				// the entry is already in use, so create a clonemarker for the current glyph
				g.setClone(objectFactory.createGlyphClone());
				// get the already in use glyph
				Entry en = handlesEntries.get(e.getName());
				// check if the is legit
				if (en.getCustom() != null && ((Glyph) en.getCustom()).getClone() == null) {
					// if so make it a clone too
					((Glyph) en.getCustom()).setClone(objectFactory.createGlyphClone());
				}
			} else {
				// if the entry isnt already in use just put it into the map
				handlesEntries.put(e.getName(), e);
			}

			// determine the sbgn clazz for the glyph
			g.setClazz(SBGNProperties.getGlyphType(e).toString());

			// create a bbox and a label
			Bbox bb = objectFactory.createBbox();
			Label l = objectFactory.createLabel();

			List<KeggInfos> keggInfos = new LinkedList<KeggInfos>();

			// call KeggInfos for the correct name and additional informations
			for (String ko_id : e.getName().split(" ")) {
				if (ko_id.trim().equalsIgnoreCase("undefined") || e.hasComponents())
					continue;
				KeggInfos infos = KeggInfos.get(ko_id, manager);
				keggInfos.add(infos);
			}

			String name = getNameForEntry(e, keggInfos.toArray(new KeggInfos[0]));

			// define the bounding box
			bb.setX(e.getGraphics().getX());
			bb.setY(e.getGraphics().getY());
			bb.setW(e.getGraphics().getWidth());
			bb.setH(e.getGraphics().getHeight());

			// set the label name according to the KeggInfos fetched
			l.setText(name);

			// set the values for the glyph
			g.setBbox(bb);
			g.setLabel(l);

			// set the glyph as custom in the entry
			e.setCustom(g);
			
			// put the glyph into the map
			sbgn.getMap().getGlyph().add(g);
		}

	}

	/**
	 * Transform all the Relations from KEGG to SBGN arcs
	 * 
	 * @param p
	 *            Pathway
	 */
	private void handleAllRelations(Pathway p) {
		
		// for every relation
		for (Relation relation : p.getRelations()) {
			
			// get the relation partners
			Entry one = p.getEntryForId(relation.getEntry1());
			Entry two = p.getEntryForId(relation.getEntry2());

			// make sure all went right
			if (one == null || two == null) {
				// This happens, e.g. when removing pathways nodes
				// or in general when removing nodes... => below
				// info, because mostly this is wanted by user.
				log.fine("Relation with unknown entry!");
				continue;
			}

			// grab the source and the target of the relation as glyphs
			Glyph source = (Glyph) one.getCustom();
			Glyph target = (Glyph) two.getCustom();

			// for every subtype of the relation
			for (int i = 0; i < relation.getSubtypes().size(); i++) {
				// get the name of the relation subtype
				String currentRelation = relation.getSubtypes().get(i).getName();
				
				// create a glyphstate
				State state = objectFactory.createGlyphState();
				
				// check the possible kegg subtypes and translate them into arcs
				if(currentRelation.equalsIgnoreCase(SubType.GLYCOSYLATION)){
					// add "G" to the product
					state.setValue("G");
					target.setState(state);
					// after the change set the custom again
					two.setCustom(target);
					// create an edge between those two entries
					createLink(source, target);
				} else if(currentRelation.equalsIgnoreCase(SubType.METHYLATION)){
					// add "Me" to the product
					state.setValue("Me");
					target.setState(state);
					// after the change set the custom again
					two.setCustom(target);
					// create an edge between those two entries
					createLink(source, target);
				} else if(currentRelation.equalsIgnoreCase(SubType.PHOSPHORYLATION)){
					// add "P" to the product
					state.setValue("P");
					target.setState(state);
					// after the change set the custom again
					two.setCustom(target);
					// create an edge between those two entries
					createLink(source, target);
				} else if(currentRelation.equalsIgnoreCase(SubType.UBIQUITINATION)){
					// add "Ub" to the product
					state.setValue("Ub");
					target.setState(state);
					// after the change set the custom again
					two.setCustom(target);
					// create an edge between those two entries
					createLink(source, target);
				} else if(currentRelation.equalsIgnoreCase(SubType.DEPHOSPHORYLATION)){
					// add nothing to the product
					state.setValue("");
					target.setState(state);
					// after the change set the custom again
					two.setCustom(target);
					// create an edge between those two entries
					createLink(source, target);
				} else if(currentRelation.equalsIgnoreCase(SubType.DISSOCIATION)){
					ArrayList<Glyph> sources = new ArrayList<Glyph>();
					sources.add(source);
					ArrayList<Glyph> targets = new ArrayList<Glyph>();
					targets.add(target);
					ArrayList<Glyph> reactionModifiers = new ArrayList<Glyph>();
					// create an edge with a process glyph of the type dissociation
					createEdgeWithProcessGlyphAndPorts(sources, targets, GlyphType.dissociation, reactionModifiers);
				} else if(currentRelation.equalsIgnoreCase(SubType.ASSOCIATION)){
					ArrayList<Glyph> sources = new ArrayList<Glyph>();
					sources.add(source);
					ArrayList<Glyph> targets = new ArrayList<Glyph>();
					targets.add(target);
					ArrayList<Glyph> reactionModifiers = new ArrayList<Glyph>();
					// create an edge with a process glyph of the type association
					createEdgeWithProcessGlyphAndPorts(sources, targets, GlyphType.association, reactionModifiers);
				} else if(currentRelation.equalsIgnoreCase(SubType.COMPOUND)){
					/** TODO: create a triangle or something like this **/
				} else {
					createLink(source, target);
				}
			}
		}
	}

	/**
	 * Transform all the Reactions from KEGG to SBGN
	 * 
	 * @param p
	 */
	private void handleAllReactions(Pathway p) {
		for (Reaction reaction : p.getReactions()) {
			
			// create arraylists for the sources, targets and reactionModifiers
			ArrayList<Glyph> sources = new ArrayList<Glyph>();
			ArrayList<Glyph> targets = new ArrayList<Glyph>();
			ArrayList<Glyph> reactionModifiers = new ArrayList<Glyph>();

			// Substrates
			for (ReactionComponent rc : reaction.getSubstrates()) {
				// get the entry for the reactioncomponent
				Entry substrate = p.getEntryForReactionComponent(rc);

				// get the glyph for the entry
				Glyph substrateGlyph = (Glyph) substrate.getCustom();
				if(substrateGlyph != null)
					sources.add(substrateGlyph);
				else {
					String[] args = {substrate.getName(), String.valueOf(substrate.getId())};
					log.warning(String.format("Entry %s (id: %s) has no Custom Glyph set!", args));
				}
			}

			// Products
			for (ReactionComponent rc : reaction.getProducts()) {
				// get the entry for the reactioncomponent
				Entry product = p.getEntryForReactionComponent(rc);
				
				// get the glyph for the entry
				Glyph productGlyph = (Glyph) product.getCustom();
				if(productGlyph != null)
					targets.add(productGlyph);
				else {
					String[] args = {product.getName(), String.valueOf(product.getId())};
					log.warning(String.format("Entry %s (id: %s) has no Custom Glyph set!", args));
				}
			}

			// Enzymes
			Collection<Entry> enzymes = p.getReactionModifiers(reaction.getName());
			for (Entry ec : enzymes) {
				
				// get the glyph for the entry
				Glyph enzymeGlyph = (Glyph) ec.getCustom();
				if(enzymeGlyph != null)
					sources.add(enzymeGlyph);
				else {
					String[] args = {ec.getName(), String.valueOf(ec.getId())};
					log.warning(String.format("Entry %s (id: %s) has no Custom Glyph set!", args));
				}
			}
			
			// do the magic!
			createEdgeWithProcessGlyphAndPorts(sources, targets, GlyphType.process, reactionModifiers);

		}
	}

	/**
	 * Create a {@link Glyph} and name them ascendingly
	 *
	 * @return {@link Glyph}
	 */
	private Glyph createGlyphWithID() {
		// create a new glyph
		Glyph glyph = objectFactory.createGlyph();
		// name the glyph and add the id globally
		glyph.setId("glyph" + id++);
		// put the glyph in the hashmap with the number of the next subglyph
		glyphNames.put(glyph, 1);
		return glyph;
	}

	/**
	 * Create a {@link Port} for a {@link Glyph} with the correct name and
	 * number
	 * 
	 * @param glyph
	 * @return {@link Port}
	 */
	private Port createPortForGlyph(Glyph glyph) {
		// create a new port
		Port port = objectFactory.createGlyphPort();
		// get the number of the next subglyph from the hashmap
		int subId = glyphNames.get(glyph);
		// create the proper name for the port and set it
		port.setId(glyph.getId() + "." + subId);
		// increase the glyphs subglyphs
		glyphNames.put(glyph, subId++);
		return port;
	}

	/**
	 * Create a Connection between the source and the target {@link Glyph}s
	 * 
	 * @param source
	 * @param target
	 */
	private void createLink(Glyph source, Glyph target) {

		// create a connection
		Arc connection = objectFactory.createArc();

		// create start and end of the connection
		Start connectionStart = objectFactory.createArcStart();
		End connectionEnd = objectFactory.createArcEnd();

		// set start and end coordinations accordingly to the source and target
		// glyphs
		connectionStart.setX(source.getBbox().getX());
		connectionStart.setY(source.getBbox().getY());
		connectionEnd.setX(target.getBbox().getX());
		connectionEnd.setY(target.getBbox().getY());

		// set the startand end to the connection
		connection.setStart(connectionStart);
		connection.setEnd(connectionEnd);
		// clazz is needed otherwise there will be an error
		connection.setClazz(ArcType.consumption.toString());
		// set the glyphs as source and target within the connection
		connection.setSource(source);
		connection.setTarget(target);

		// add the connection to the arc list
		sbgn.getMap().getArc().add(connection);
	}

	/**
	 * Create a Connection with a process {@link Glyph} between the source and
	 * target {@link Glyph}s
	 * 
	 * @param source
	 * @param target
	 */
	private void createEdgeWithProcessGlyphAndPorts(ArrayList<Glyph> sources, ArrayList<Glyph> targets, GlyphType type, ArrayList<Glyph> reactionModifiers) {

		// create a process glyph and set the type
		Glyph process = createGlyphWithID();
		process.setClazz(type.toString());
		
		// Two port elements are required for process nodes
		/** TODO: the port elements need coordinates to connect with the arcs **/
		Port portIn = createPortForGlyph(process);
		Port portOut = createPortForGlyph(process);
		process.getPort().add(portIn);
		process.getPort().add(portOut);
		
		// make sure that the sources and targets contain at least 1 element
		if(sources.size() != 0 && targets.size() != 0) {
				
			// for all sources
			for(Glyph source : sources) {
				
				// create an connection arc / edge
				Arc connection = objectFactory.createArc();
				
				// set the type of the connection
				connection.setClazz(ArcType.consumption.toString());
				
				// create a start and end point of the arc
				Start connectionStart = objectFactory.createArcStart();
				End connectionEnd = objectFactory.createArcEnd();
				
				// set the coordinations for the start and end points
				connectionStart.setX(source.getBbox().getX());
				connectionStart.setY(source.getBbox().getY());
				connectionEnd.setX(portIn.getX());
				connectionEnd.setY(portIn.getY());
				
				// set the start and end points to the arc
				connection.setSource(source);
				connection.setTarget(portIn);
				
				connection.setStart(connectionStart);
				connection.setEnd(connectionEnd);
				
				// add the connection to the map
				map.getArc().add(connection);
			}
			
			
			// for all targets
			for(Glyph target : targets) {

				// create an connection arc / edge
				Arc connection = objectFactory.createArc();
				
				// set the type of the connection
				connection.setClazz(ArcType.production.toString());
				
				// create a start and end point of the arc
				Start connectionStart = objectFactory.createArcStart();
				End connectionEnd = objectFactory.createArcEnd();
				
				// set the coordinations for the start and end points
				connectionStart.setX(portOut.getX());
				connectionStart.setY(portOut.getY());
				connectionEnd.setX(target.getBbox().getX());
				connectionEnd.setY(target.getBbox().getY());
				
				// set the start and end points to the arc
        connection.setSource(portOut);
        connection.setTarget(target);
        
				connection.setStart(connectionStart);
				connection.setEnd(connectionEnd);
				
				// add the connection to the map
				map.getArc().add(connection);
			}
			
			
			// for all reactionModifiers
			for(Glyph rm : reactionModifiers) {
	
				// create an connection arc / edge
				Arc connection = objectFactory.createArc();
				
				// set the type of the connection
				/** TODO: set the class of the connection accordingly to the connection **/
				connection.setClazz(ArcType.catalysis.toString());
				
				// create a start and end point of the arc
				Start connectionStart = objectFactory.createArcStart();
				End connectionEnd = objectFactory.createArcEnd();
				
				// set the coordinations for the start and end points
				connectionStart.setX(rm.getBbox().getX());
				connectionStart.setY(rm.getBbox().getY());
				connectionEnd.setX(process.getBbox().getX());
				connectionEnd.setY(process.getBbox().getY());
				
				// set the start and end points to the arc
        connection.setSource(rm);
        //connection.setTarget(target);// // TODO: Create a port for the modifiers.
        
				connection.setStart(connectionStart);
				connection.setEnd(connectionEnd);
				
				// add the connection to the map
				map.getArc().add(connection);
				
			}
				
		}
	}

	public static void main(String[] args) {
	}

	@Override
	public boolean writeToFile(Sbgn doc, String outFile) {
		try {
			SbgnUtil.writeToFile(doc, new File(outFile));
			return true;
		} catch (JAXBException e) {
			return false;
		}
	}

	@Override
	protected boolean considerRelations() {
		return true;
	}

	@Override
	protected boolean considerReactions() {
		return true;
	}
}

/*******************************************************************************
 * Copyright (c) 2013 Systerel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Systerel - initial API and implementation
 *******************************************************************************/
package org.eventb.internal.core.ast.datatype;

import static org.eventb.core.ast.Formula.CONVERSE;
import static org.eventb.core.ast.Formula.CPROD;
import static org.eventb.core.ast.Formula.DPROD;
import static org.eventb.core.ast.Formula.EQUAL;
import static org.eventb.core.ast.Formula.FORALL;
import static org.eventb.core.ast.Formula.FUNIMAGE;
import static org.eventb.core.ast.Formula.IN;
import static org.eventb.core.ast.Formula.KPARTITION;
import static org.eventb.core.ast.Formula.KRAN;
import static org.eventb.core.ast.Formula.MAPSTO;
import static org.eventb.core.ast.Formula.RELIMAGE;
import static org.eventb.core.ast.Formula.STREL;
import static org.eventb.core.ast.Formula.TBIJ;
import static org.eventb.core.ast.Formula.TINJ;
import static org.eventb.core.ast.Formula.TSUR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eventb.core.ast.BoundIdentDecl;
import org.eventb.core.ast.Expression;
import org.eventb.core.ast.ExtendedExpression;
import org.eventb.core.ast.FormulaFactory;
import org.eventb.core.ast.FreeIdentifier;
import org.eventb.core.ast.GivenType;
import org.eventb.core.ast.IDatatypeTranslation;
import org.eventb.core.ast.ParametricType;
import org.eventb.core.ast.Predicate;
import org.eventb.core.ast.Type;
import org.eventb.core.ast.datatype.IConstructorArgument;
import org.eventb.core.ast.datatype.IConstructorExtension;
import org.eventb.core.ast.datatype.IDatatype;
import org.eventb.core.ast.datatype.ISetInstantiation;
import org.eventb.core.ast.datatype.ITypeInstantiation;
import org.eventb.core.ast.extension.IExpressionExtension;

/**
 * Common implementation of a translator for one datatype instance.
 * <p>
 * The translation scheme is described in {@link IDatatypeTranslation}.
 * </p>
 * <p>
 * <em>IMPORTANT NOTE:</em> As this class manipulates formulas in two different
 * versions of the mathematical language, it is very important not to mix them
 * inadvertently. To ease this, this file uses the convention that all
 * identifiers prefixed with <code>src</code> are in the source language (the
 * one containing the datatype to translate) and those prefixed with
 * <code>trg</code> are in the target language.
 * </p>
 * 
 * @author Thomas Muller
 */
public class DatatypeTranslator {

	private static final Predicate[] NO_PREDICATES = new Predicate[0];

	private static final String TYPE_SUFFIX = "_Type";

	private final DatatypeTranslation translation;
	private final FormulaFactory srcFactory;
	private final FormulaFactory trgFactory;

	// Types and extension of the source language
	private final ParametricType srcTypeInstance;
	private final ITypeInstantiation srcInstantiation;
	private final Type[] srcTypeParameters;
	private final IExpressionExtension srcTypeConstructor;
	private final IDatatype datatype;
	private final IConstructorExtension[] srcConstructors;
	private final boolean hasDestructors;
	private final boolean hasNoSetConstructor;
	private final boolean hasSingleConstructor;

	// Types and formulas of the target language
	private final Type[] trgTypeParameters;
	private final FreeIdentifier trgSetCons;
	private final GivenType trgDatatype;
	private final Expression trgDatatypeExpr;

	private final Map<Object, FreeIdentifier> replacements //
	= new HashMap<Object, FreeIdentifier>();

	public DatatypeTranslator(ParametricType typeInstance,
			DatatypeTranslation translation) {
		this.translation = translation;
		this.srcFactory = translation.getSourceFormulaFactory();
		this.trgFactory = translation.getTargetFormulaFactory();
		this.srcTypeInstance = typeInstance;
		this.srcTypeConstructor = typeInstance.getExprExtension();
		this.srcTypeParameters = typeInstance.getTypeParameters();
		this.datatype = (IDatatype) srcTypeConstructor.getOrigin();
		this.srcInstantiation = datatype.getTypeInstantiation(srcTypeInstance);
		this.srcConstructors = datatype.getConstructors();

		// A non-empty datatype must have at least one constructor
		assert srcConstructors.length != 0;

		this.hasDestructors = hasDestructors();
		this.hasNoSetConstructor = !hasDestructors
				|| srcTypeParameters.length == 0;
		this.hasSingleConstructor = srcConstructors.length == 1;

		// The first translation must be for the type parameters to ensure
		// consistent naming for tests
		this.trgTypeParameters = translateTypeParameters();

		final String srcSymbol = srcTypeConstructor.getSyntaxSymbol();
		this.trgDatatype = getTrgDatatype(srcSymbol);
		this.trgDatatypeExpr = toTrgExpr(trgDatatype);
		this.trgSetCons = getTrgSetConstructor(srcSymbol);
		computeReplacements();
	}

	private boolean hasDestructors() {
		for (final IConstructorExtension cons : datatype.getConstructors()) {
			if (cons.hasArguments())
				return true;
		}
		return false;
	}

	private Expression toTrgExpr(Type trgType) {
		return trgType.toExpression();
	}

	private Type[] translateTypeParameters() {
		final int length = srcTypeParameters.length;
		final Type[] trgResult = new Type[length];
		for (int i = 0; i < length; i++) {
			trgResult[i] = translateType(srcTypeParameters[i]);
		}
		return trgResult;
	}

	private GivenType getTrgDatatype(String srcSymbol) {
		final String symbol;
		if (hasNoSetConstructor) {
			symbol = srcSymbol;
		} else {
			symbol = srcSymbol + TYPE_SUFFIX;
		}
		return this.translation.solveGivenType(symbol);
	}

	private FreeIdentifier getTrgSetConstructor(String srcSymbol) {
		if (hasNoSetConstructor) {
			return null;
		}
		final Type trgType = makeTrgConsType(trgTypeParameters);
		return translation.solveIdentifier(srcSymbol, trgType);
	}

	/**
	 * Compute all fresh identifiers that may appear in the translation of this
	 * datatype instance.
	 */
	private void computeReplacements() {
		for (final IConstructorExtension srcCons : srcConstructors) {
			final Type[] trgArgTypes = computeDestructorReplacements(srcCons);
			addReplacement(srcCons, srcCons.getSyntaxSymbol(),
					makeTrgConsType(trgArgTypes));
		}
	}

	/*
	 * Compute replacements for the destructors of the given constructor.
	 * Returns an array of the result types in the target environment of every
	 * destructor added.
	 */
	private Type[] computeDestructorReplacements(IConstructorExtension cons) {
		final IConstructorArgument[] args = cons.getArguments();
		final int nbArgs = args.length;
		final Type[] trgResult = new Type[nbArgs];
		for (int i = 0; i < nbArgs; i++) {
			final IConstructorArgument arg = args[i];
			final Type trgAlpha = translateType(arg.getType(srcInstantiation));
			final String symbol;
			if (arg.isDestructor()) {
				symbol = arg.asDestructor().getSyntaxSymbol();
			} else {
				symbol = "d";  // Dummy name for unnamed arguments
			}
			addReplacement(arg, symbol, mTrgRelType(trgDatatype, trgAlpha));
			trgResult[i] = trgAlpha;
		}
		return trgResult;
	}

	private void addReplacement(Object ext, String symbol, Type trgType) {
		final FreeIdentifier ident = translation.solveIdentifier(symbol,
				trgType);
		replacements.put(ext, ident);
	}

	private Type makeTrgConsType(Type[] trgArgTypes) {
		if (trgArgTypes.length == 0) {
			return trgDatatype;
		}
		final Type trgProdType = makeTrgProdType(trgArgTypes);
		return mTrgRelType(trgProdType, trgDatatype);
	}

	private Type makeTrgProdType(Type[] trgTypes) {
		Type trgProdType = trgTypes[0];
		for (int i = 1; i < trgTypes.length; i++) {
			trgProdType = mTrgProdType(trgProdType, trgTypes[i]);
		}
		return trgProdType;
	}

	private Expression combineTrgExpr(int tag, Expression[] trgExprs) {
		final int length = trgExprs.length;
		assert length != 0;
		Expression trgResult = trgExprs[0];
		for (int i = 1; i < length; i++) {
			trgResult = mTrgBinExpr(tag, trgResult, trgExprs[i]);
		}
		return trgResult;
	}

	private Type translateType(Type srcType) {
		// This test prevents infinite loop during instance initialization
		if (srcTypeInstance.equals(srcType)) {
			return trgDatatype;
		}
		return translation.translate(srcType);
	}

	/**
	 * Returns the translation of the datatype instance handled by this
	 * translator.
	 */
	public Type getTranslatedType() {
		return trgDatatype;
	}

	/**
	 * Rewrites the given extended expression.
	 * 
	 * @param src
	 *            the extended expression to be translated
	 * @param trgChildExprs
	 *            the new children expressions
	 * @return a translation of the given extended expression
	 */
	public Expression rewrite(ExtendedExpression src, Expression[] trgChildExprs) {
		final IExpressionExtension ext = src.getExtension();
		if (ext.isATypeConstructor()) {
			if (hasNoSetConstructor || src.isATypeExpression()) {
				return trgDatatypeExpr;
			} else {
				return mTrgRelImage(trgSetCons, trgChildExprs);
			}
		}
		final Expression trgExpr = replacements.get(ext);
		if (trgChildExprs.length == 0) {
			return trgExpr;
		}
		final Expression trgMaplets = combineTrgExpr(MAPSTO, trgChildExprs);
		return mTrgBinExpr(FUNIMAGE, trgExpr, trgMaplets);
	}

	private Expression mTrgRelImage(Expression trgRel, Expression[] trgSets) {
		final Expression trgExpr = combineTrgExpr(CPROD, trgSets);
		return mTrgBinExpr(RELIMAGE, trgRel, trgExpr);
	}

	/**
	 * Returns the axioms that specify the properties of the fresh identifiers
	 * introduced by this translator.
	 */
	public List<Predicate> getAxioms() {
		final List<Predicate> axioms = new ArrayList<Predicate>();
		addSetConstructorDefinitionAxiom(axioms);
		for (final IConstructorExtension cons : srcConstructors) {
			addAxioms(axioms, cons);
		}
		addPartitionAxiom(axioms);
		addSetConstructorAxiom(axioms);
		return axioms;
	}

	/**
	 * Computes and adds the axiom (E)
	 */
	private void addSetConstructorDefinitionAxiom(List<Predicate> axioms) {
		if (hasNoSetConstructor)
			return;
		final Type trgSetConsType = trgSetCons.getType();
		final Expression trgProd = toTrgExpr(trgSetConsType.getSource());
		final Expression trgRange = toTrgExpr(trgSetConsType.getTarget());
		axioms.add(mTrgInRelationalSet(trgSetCons, STREL, trgProd, trgRange));
	}

	/**
	 * Computes and adds the axiom (F)
	 */
	private void addSetConstructorAxiom(List<Predicate> axioms) {
		if (hasNoSetConstructor)
			return;
		final List<Expression> trgParts = new ArrayList<Expression>();
		final Expression[] srcBoundIdents = makeSrcBoundIdentifiers();
		trgParts.add(mTrgRelImage(trgSetCons, translate(srcBoundIdents)));
		final ExtendedExpression srcSet = makeSrcSet(srcBoundIdents);
		final ISetInstantiation setInst = datatype.getSetInstantiation(srcSet);
		for (final IConstructorExtension cons : srcConstructors) {
			trgParts.add(makeTrgSetPartitionPart(cons, setInst));
		}
		final Predicate trgPartition = mTrgPartition(trgParts);
		final BoundIdentDecl[] trgDecls = makeTrgBoundIdentDecls();
		axioms.add(mTrgForall(trgDecls, trgPartition));
	}

	private Expression[] makeSrcBoundIdentifiers() {
		final int nbIdents = srcTypeParameters.length;
		final Expression[] idents = new Expression[nbIdents];
		// De Bruijn indexes are counted backwards
		int boundIndex = nbIdents - 1;
		for (int i = 0; i < nbIdents; i++) {
			final Type srcType = srcTypeParameters[i];
			final Type srcBoundType = mSrcPowerSetType(srcType);
			idents[i] = mSrcBoundIdent(boundIndex, srcBoundType);
			boundIndex--;
		}
		return idents;
	}

	private Expression[] translate(Expression[] srcExprs) {
		final int length = srcExprs.length;
		final Expression[] trgResult = new Expression[length];
		for (int i = 0; i < length; i++) {
			trgResult[i] = srcExprs[i].translateDatatype(translation);
		}
		return trgResult;
	}

	private Expression makeTrgSetPartitionPart(IConstructorExtension cons,
			ISetInstantiation setInst) {
		final Expression trgCons = replacements.get(cons);
		if (hasArguments(cons)) {
			final Expression[] trgArgSets = mTrgArgSets(cons, setInst);
			return mTrgRelImage(trgCons, trgArgSets);
		} else {
			return mTrgSingleton(trgCons);
		}
	}

	private Expression[] mTrgArgSets(IConstructorExtension cons,
			ISetInstantiation setInst) {
		final IConstructorArgument[] args = cons.getArguments();
		final Expression[] trgSets = new Expression[args.length];
		for (int i = 0; i < trgSets.length; i++) {
			final Expression srcSet = args[i].getSet(setInst);
			trgSets[i] = srcSet.translateDatatype(translation);
		}
		return trgSets;
	}

	private ExtendedExpression makeSrcSet(Expression[] srcExprs) {
		return srcFactory.makeExtendedExpression(srcTypeConstructor, srcExprs,
				NO_PREDICATES, null, null);
	}

	private BoundIdentDecl[] makeTrgBoundIdentDecls() {
		final int nbTypeParams = trgTypeParameters.length;
		final BoundIdentDecl[] trgResult = new BoundIdentDecl[nbTypeParams];
		final String[] typeParamsNames = datatype.getTypeConstructor()
				.getFormalNames();
		for (int i = 0; i < nbTypeParams; i++) {
			final Type trgType = mTrgPowerSetType(trgTypeParameters[i]);
			final String declName = typeParamsNames[i];
			trgResult[i] = mTrgBoundIdentDecl(declName, trgType);
		}
		return trgResult;
	}

	private void addAxioms(List<Predicate> axioms, IConstructorExtension cons) {
		if (!hasArguments(cons)) {
			return;
		}
		final Expression trgCons = replacements.get(cons);
		final Expression trgDomain = toTrgExpr(trgCons.getType().getSource());
		final Expression trgRange = trgDatatypeExpr;
		final int tag = hasSingleConstructor ? TBIJ : TINJ;
		axioms.add(mTrgInRelationalSet(trgCons, tag, trgDomain, trgRange));
		final Expression[] trgDest = getTrgDestructors(cons);
		addDestructorAxioms(axioms, cons, trgDest);
		addConstructorInverseAxiom(axioms, cons, trgDest);
	}

	// Returns the replacements of the destructors of the given constructor
	private Expression[] getTrgDestructors(IConstructorExtension cons) {
		final IConstructorArgument[] args = cons.getArguments();
		final int nbArgs = args.length;
		final Expression[] trgResult = new Expression[nbArgs];
		for (int i = 0; i < nbArgs; i++) {
			trgResult[i] = replacements.get(args[i]);
		}
		return trgResult;
	}

	private void addDestructorAxioms(List<Predicate> axioms,
			IConstructorExtension constructor, Expression[] trgDests) {
		final Expression trgPart = makeTrgPartitionPart(constructor);
		for (final Expression trgDest : trgDests) {
			final Type trgType = trgDest.getType().getTarget();
			axioms.add(mTrgInRelationalSet(trgDest, TSUR, trgPart,
					toTrgExpr(trgType)));
		}
	}

	private Expression makeTrgPartitionPart(IConstructorExtension cons) {
		final Expression trgGamma = replacements.get(cons);
		if (hasArguments(cons)) {
			return mTrgUnaryExpr(KRAN, trgGamma);
		} else {
			return mTrgSingleton(trgGamma);
		}
	}

	private void addConstructorInverseAxiom(List<Predicate> axioms,
			IExpressionExtension constructor, Expression[] trgDests) {
		final Expression trgDProd = combineTrgExpr(DPROD, trgDests);
		final Expression trgCons = replacements.get(constructor);
		final Expression trgConv = mTrgUnaryExpr(CONVERSE, trgCons);
		axioms.add(mTrgEquals(trgDProd, trgConv));
	}

	private void addPartitionAxiom(List<Predicate> axioms) {
		if (hasSingleConstructorWithArguments()) {
			// Partition predicate is useless
			return;
		}
		final List<Expression> trgParts = new ArrayList<Expression>();
		trgParts.add(trgDatatypeExpr);
		for (final IConstructorExtension cons : srcConstructors) {
			trgParts.add(makeTrgPartitionPart(cons));
		}
		axioms.add(mTrgPartition(trgParts));
	}

	private boolean hasSingleConstructorWithArguments() {
		return hasSingleConstructor && hasArguments(srcConstructors[0]);
	}

	private boolean hasArguments(IConstructorExtension constructor) {
		return constructor.getArguments().length != 0;
	}

	private Expression mSrcBoundIdent(int i, Type srcType) {
		return srcFactory.makeBoundIdentifier(i, null, srcType);
	}

	private Type mSrcPowerSetType(Type srcType) {
		return srcFactory.makePowerSetType(srcType);
	}

	private Type mTrgPowerSetType(Type trgType) {
		return trgFactory.makePowerSetType(trgType);
	}

	private BoundIdentDecl mTrgBoundIdentDecl(String name, Type trgType) {
		return trgFactory.makeBoundIdentDecl(name, null, trgType);
	}

	private Expression mTrgBinExpr(final int tag, final Expression e1,
			final Expression e2) {
		return trgFactory.makeBinaryExpression(tag, e1, e2, null);
	}

	private Predicate mTrgPartition(final List<Expression> parts) {
		return trgFactory.makeMultiplePredicate(KPARTITION, parts, null);
	}

	private Type mTrgProdType(Type t1, Type t2) {
		return trgFactory.makeProductType(t1, t2);
	}

	private Type mTrgRelType(Type t1, Type t2) {
		return trgFactory.makeRelationalType(t1, t2);
	}

	private Predicate mTrgForall(BoundIdentDecl[] trgDecls, Predicate trgPred) {
		return trgFactory.makeQuantifiedPredicate(FORALL, trgDecls, trgPred,
				null);
	}

	private Predicate mTrgEquals(Expression trgLeft, Expression trgRight) {
		return trgFactory.makeRelationalPredicate(EQUAL, trgLeft, trgRight,
				null);
	}

	private Predicate mTrgInRelationalSet(Expression trgRel, int tag,
			Expression trgDomain, Expression trgRange) {
		final Expression trgSet = mTrgBinExpr(tag, trgDomain, trgRange);
		return trgFactory.makeRelationalPredicate(IN, trgRel, trgSet, null);
	}

	private Expression mTrgSingleton(Expression expression) {
		return trgFactory.makeSetExtension(expression, null);
	}

	private Expression mTrgUnaryExpr(int tag, Expression constructor) {
		return trgFactory.makeUnaryExpression(tag, constructor, null);
	}

}
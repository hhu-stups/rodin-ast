/*******************************************************************************
 * Copyright (c) 2005, 2013 ETH Zurich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ETH Zurich - initial API and implementation
 *     Systerel - generalised getPositions() into inspect()
 *     Systerel - added child indexes
 *     Systerel - add given sets to free identifier cache
 *     Systerel - store factory used to build a formula
 *******************************************************************************/
package org.eventb.core.ast;

import static org.eventb.internal.core.ast.FormulaChecks.ensureMinLength;

import java.util.Arrays;

import org.eventb.internal.core.ast.FindingAccumulator;
import org.eventb.internal.core.ast.ITypeCheckingRewriter;
import org.eventb.internal.core.ast.IntStack;
import org.eventb.internal.core.typecheck.TypeUnifier;

/**
 * Common implementation for event-B assignments.
 * <p>
 * There are various kinds of assignments which are implemented in sub-classes
 * of this class. The commonality between these assignments is that they are
 * formed of two parts: a left-hand side and a right hand-side. The left-hand side,
 * that is a list of free identifiers, is implemented in this class, while the
 * right-hand side is implemented in subclasses.
 * </p>
 * <p>
 * This class is not intended to be subclassed by clients.
 * </p>
 * 
 * @author Laurent Voisin
 * @since 1.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class Assignment extends Formula<Assignment> {

	protected final FreeIdentifier[] assignedIdents;
	
	/**
	 * Creates a new assignment with the given arguments.
	 * 
	 * @param tag node tag of this expression
	 * @param ff the formula factory used to build this assignment
	 * @param location source location of this expression
	 * @param hashCode combined hash code for children
	 * @param assignedIdents array of free identifiers that constitute the left-hand side
	 * @since 3.0
	 */
	protected Assignment(int tag, FormulaFactory ff,
			SourceLocation location, int hashCode,
			FreeIdentifier[] assignedIdents) {
		super(tag, ff, location, combineHashCodes(
				combineHashCodes(assignedIdents), hashCode));
		ensureMinLength(assignedIdents, 1);
		this.assignedIdents = assignedIdents;
		ensureSameFactory(this.assignedIdents);
	}

	protected final void appendAssignedIdents(StringBuilder result) {
		boolean comma = false;
		for (FreeIdentifier ident : assignedIdents) {
			if (comma)
				result.append(',');
			comma = true;
			result.append(ident.getName());
		}
	}
	
	@Override
	protected final void solveType(TypeUnifier unifier) {
		if (isTypeChecked()) {
			return;
		}
		solveChildrenTypes(unifier);
		for (FreeIdentifier ident: assignedIdents) {
			ident.solveType(unifier);
		}
		synthesizeType();
	}

	/**
	 * @since 3.0
	 */
	// Calls recursively solveType on each child of this node.
	protected abstract void solveChildrenTypes(TypeUnifier unifier);

	/**
	 * Return the left-hand side of this assignment.
	 * 
	 * @return an array of the free identifiers that make up the left-hand side
	 *         of this assignment
	 */
	public final FreeIdentifier[] getAssignedIdentifiers() {
		return assignedIdents.clone();
	}
	
	protected final String getSyntaxTreeLHS(String[] boundNames, String tabs) {
		StringBuilder builder = new StringBuilder();
		for (FreeIdentifier ident: assignedIdents) {
			builder.append(ident.getSyntaxTree(boundNames, tabs));
		}
		return builder.toString();
	}
		
	@Override
	protected final Assignment getTypedThis() {
		return this;
	}

	protected final boolean hasSameAssignedIdentifiers(Assignment other) {
		return Arrays.equals(assignedIdents, other.assignedIdents);
	}

	@Override
	public final Assignment rewrite(IFormulaRewriter rewriter) {
		throw new UnsupportedOperationException("Assignments cannot be rewritten");
	}

	@Override
	protected final Assignment rewrite(ITypeCheckingRewriter rewriter) {
		throw new UnsupportedOperationException("Assignments cannot be rewritten");
	}
	
	/**
	 * Returns the (flattened) feasibility predicate of this assignment. An
	 * exception is thrown if this assignment was not type checked.
	 * 
	 * @return Returns the feasibility predicate
	 * @since 3.0
	 */
	public final Predicate getFISPredicate() {
		assert isTypeChecked();
		return getFISPredicateRaw().flatten();
	}
	
	/**
	 * @since 3.0
	 */
	protected abstract Predicate getFISPredicateRaw();
	
	/**
	 * Returns the (flattened) before-after predicate of this assignment. An
	 * exception is thrown if this assignment was not type checked.
	 * 
	 * @return Returns the before-after predicate of this assignment
	 * @since 3.0
	 */
	public final Predicate getBAPredicate() {
		assert isTypeChecked();
		return getBAPredicateRaw().flatten();
	}
	
	/**
	 * @since 3.0
	 */
	protected abstract Predicate getBAPredicateRaw();

	
	/**
	 * Returns an array of the free identifiers that occur on the right-hand
	 * side of this assignment. The free identifiers are extracted using
	 * {@link Formula#getFreeIdentifiers()} applied to all formulas that are
	 * part of the right-hand side of this assignment.
	 * 
	 * @return all free identifiers that occur in the right-hand side of this
	 *         assignment.
	 */
	public abstract FreeIdentifier[] getUsedIdentifiers();
	
	/**
	 * @since 3.0
	 */
	protected abstract void synthesizeType();

	@Override
	protected final <F> void inspect(FindingAccumulator<F> acc) {
		throw new UnsupportedOperationException(
				"Assignments cannot be rewritten");
	}

	@Override
	public final Formula<?> getChild(int index) {
		throw new UnsupportedOperationException(
				"Assignments cannot be rewritten");
	}

	@Override
	public final int getChildCount() {
		throw new UnsupportedOperationException(
				"Assignments cannot be rewritten");
	}

	@Override
	protected final IPosition getDescendantPos(SourceLocation sloc,
			IntStack indexes) {
		throw new UnsupportedOperationException(
				"Assignments cannot be rewritten");
	}

	@Override
	protected final Assignment rewriteChild(int index, SingleRewriter rewriter) {
		throw new UnsupportedOperationException(
				"Assignments cannot be rewritten");
	}

	@Override
	protected final Assignment getCheckedReplacement(SingleRewriter rewriter) {
		throw new UnsupportedOperationException(
				"Assignments cannot be rewritten");
	}

}

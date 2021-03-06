/*******************************************************************************
 * Copyright (c) 2005, 2013 ETH Zurich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ETH Zurich - initial API and implementation
 *     Systerel - add given sets to free identifier cache
 *     Systerel - store factory used to build a formula
 *******************************************************************************/
package org.eventb.core.ast;

import org.eventb.internal.core.typecheck.TypeUnifier;

/**
 * Represents predicates.
 * 
 * TODO: document Predicate.
 * 
 * @author Laurent Voisin
 * @since 1.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class Predicate extends Formula<Predicate> {

	/**
	 * Creates a new predicate with the specified tag and source location.
	 * 
	 * @param tag node tag of this predicate
	 * @param ff the formula factory used to build this predicate
	 * @param location source location of this predicate
	 * @param hashCode combined hash code for children
	 * @since 3.0
	 */
	protected Predicate(int tag, FormulaFactory ff,
			SourceLocation location, int hashCode) {
		super(tag, ff, location, hashCode);
	}

	@Override
	protected final Predicate getTypedThis() {
		return this;
	}

	/**
	 * @since 3.0
	 */
	protected abstract void synthesizeType();
	
	@Override
	protected final void solveType(TypeUnifier unifier) {
		if (isTypeChecked()) {
			return;
		}
		solveChildrenTypes(unifier);
		synthesizeType();
	}

	/**
	 * @since 3.0
	 */
	// Calls recursively solveType() on each child of this node.
	protected abstract void solveChildrenTypes(TypeUnifier unifier);
	
	@Override
	protected final Predicate getCheckedReplacement(SingleRewriter rewriter) {
		return rewriter.getPredicate(this);
	}

}

/*******************************************************************************
 * Copyright (c) 2010, 2018 Systerel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Systerel - initial API and implementation
 *******************************************************************************/
package org.eventb.core.ast;

import java.util.Collection;

/**
 * Common protocol for accumulating findings while inspecting sub-formulas of a
 * formula. This interface also allows to fine-tune the behavior of the
 * inspector, by skipping nodes in the input formula.
 * <p>
 * For instance, when one is visiting a formula tree looking like (each node is
 * numbered in the order in which it is visited by the inspector):
 * 
 * <pre>
 * 1
 * ├── 2
 * │   ├── 3
 * │   ├── 4
 * │   │   ├── 5
 * │   │   └── 6
 * │   └── 7
 * └── 8
 * </pre>
 * 
 * if a client calls none of the skip method, the inspector visits successively
 * nodes 1 2 3 4 5 6 7 8. If method {{@link #skipChildren()} is called while
 * visiting node 4, then the visited nodes are 1 2 3 4 7 8 (nodes 5 and 6 are
 * skipped). Finally, if method {@link #skipAll()} is called while visiting node
 * 4, then the visited nodes are 1 2 3 4 (all nodes after 4 are skipped).
 * </p>
 * 
 * @param <F>
 *            type of the findings to accumulate
 * 
 * @see IFormulaInspector
 * 
 * @author Laurent Voisin
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IAccumulator<F> {

	/**
	 * Returns the position of the sub-formula currently considered in a call to
	 * <code>inspect</code>. This method must only be called within the frame of
	 * a call to an <code>inspect</code> method.
	 * 
	 * @return the position of the current sub-formula
	 * @throws IllegalStateException
	 *             if this method is called out of the frame of an
	 *             <code>inspect</code> method
	 */
	IPosition getCurrentPosition();

	/**
	 * Adds the given finding to the accumulator.
	 * 
	 * @param finding
	 *            a finding to accumulate
	 */
	void add(F finding);

	/**
	 * Adds the given findings to the accumulator.
	 * 
	 * @param findings
	 *            findings to accumulate
	 */
	@SuppressWarnings("unchecked") // Actually safe in the single implementation
	void add(F... findings);

	/**
	 * Adds the given findings to the accumulator.
	 * 
	 * @param findings
	 *            findings to accumulate
	 */
	void add(Collection<F> findings);

	/**
	 * Tells the inspector to skip the children of the node being currently
	 * visited.
	 * 
	 * @since 2.2
	 */
	void skipChildren();

	/**
	 * Tells the inspector to skip all remaining nodes and return immediately.
	 * 
	 * @since 2.2
	 */
	void skipAll();

}

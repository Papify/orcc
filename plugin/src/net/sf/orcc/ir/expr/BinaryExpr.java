/*
 * Copyright (c) 2009, IETR/INSA of Rennes
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the IETR/INSA of Rennes nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package net.sf.orcc.ir.expr;

import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.Location;
import net.sf.orcc.ir.Type;

/**
 * This class defines a binary expression.
 * 
 * @author Matthieu Wipliez
 * 
 */
public class BinaryExpr extends AbstractExpression {

	/**
	 * Constant indicating left branch of a binary expression.
	 */
	public static final Object LEFT = new Object();

	/**
	 * Constant indicating right branch of a binary expression.
	 */
	public static final Object RIGHT = new Object();

	private Expression e1;

	private Expression e2;

	private BinaryOp op;

	private Type type;

	public BinaryExpr(Expression e1, BinaryOp op,
			Expression e2, Type type) {
		this(new Location(), e1, op, e2, type);
	}

	public BinaryExpr(Location location, Expression e1, BinaryOp op,
			Expression e2, Type type) {
		super(location);
		this.e1 = e1;
		this.e2 = e2;
		this.op = op;
		this.type = type;
	}

	@Override
	public Object accept(ExpressionInterpreter interpreter, Object... args) {
		return interpreter.interpret(this, args);
	}

	@Override
	public void accept(ExpressionVisitor visitor, Object... args) {
		visitor.visit(this, args);
	}

	/**
	 * Returns the first operand of this binary expression as an expression.
	 * 
	 * @return the first operand of this binary expression
	 */
	public Expression getE1() {
		return e1;
	}

	/**
	 * Returns the second operand of this binary expression as an expression.
	 * 
	 * @return the second operand of this binary expression
	 */
	public Expression getE2() {
		return e2;
	}

	/**
	 * Returns the operator of this binary expression.
	 * 
	 * @return the operator of this binary expression
	 */
	public BinaryOp getOp() {
		return op;
	}

	@Override
	public int getType() {
		return BINARY;
	}

	public Type getUnderlyingType() {
		return type;
	}

	public void setE1(Expression e1) {
		this.e1 = e1;
	}

	public void setE2(Expression e2) {
		this.e2 = e2;
	}

	public void setOp(BinaryOp op) {
		this.op = op;
	}

	public void setType(Type type) {
		this.type = type;
	}

}

/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.sf.orcc.backends.ir;

import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.Node;
import net.sf.orcc.ir.NodeBlock;
import net.sf.orcc.ir.NodeSpecific;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>For Node</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link net.sf.orcc.backends.ir.ForNode#getCondition <em>Condition</em>}</li>
 *   <li>{@link net.sf.orcc.backends.ir.ForNode#getJoinNode <em>Join Node</em>}</li>
 *   <li>{@link net.sf.orcc.backends.ir.ForNode#getLineNumber <em>Line Number</em>}</li>
 *   <li>{@link net.sf.orcc.backends.ir.ForNode#getNodes <em>Nodes</em>}</li>
 *   <li>{@link net.sf.orcc.backends.ir.ForNode#getLoopCounter <em>Loop Counter</em>}</li>
 *   <li>{@link net.sf.orcc.backends.ir.ForNode#getInit <em>Init</em>}</li>
 * </ul>
 * </p>
 *
 * @see net.sf.orcc.backends.ir.IrSpecificPackage#getForNode()
 * @model
 * @generated
 */
public interface ForNode extends NodeSpecific {
	/**
	 * Returns the value of the '<em><b>Condition</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Condition</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Condition</em>' containment reference.
	 * @see #setCondition(Expression)
	 * @see net.sf.orcc.backends.ir.IrSpecificPackage#getForNode_Condition()
	 * @model containment="true"
	 * @generated
	 */
	Expression getCondition();

	/**
	 * Sets the value of the '{@link net.sf.orcc.backends.ir.ForNode#getCondition <em>Condition</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Condition</em>' containment reference.
	 * @see #getCondition()
	 * @generated
	 */
	void setCondition(Expression value);

	/**
	 * Returns the value of the '<em><b>Join Node</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Join Node</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Join Node</em>' containment reference.
	 * @see #setJoinNode(NodeBlock)
	 * @see net.sf.orcc.backends.ir.IrSpecificPackage#getForNode_JoinNode()
	 * @model containment="true"
	 * @generated
	 */
	NodeBlock getJoinNode();

	/**
	 * Sets the value of the '{@link net.sf.orcc.backends.ir.ForNode#getJoinNode <em>Join Node</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Join Node</em>' containment reference.
	 * @see #getJoinNode()
	 * @generated
	 */
	void setJoinNode(NodeBlock value);

	/**
	 * Returns the value of the '<em><b>Line Number</b></em>' attribute.
	 * The default value is <code>"0"</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Line Number</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Line Number</em>' attribute.
	 * @see #setLineNumber(int)
	 * @see net.sf.orcc.backends.ir.IrSpecificPackage#getForNode_LineNumber()
	 * @model default="0"
	 * @generated
	 */
	int getLineNumber();

	/**
	 * Sets the value of the '{@link net.sf.orcc.backends.ir.ForNode#getLineNumber <em>Line Number</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Line Number</em>' attribute.
	 * @see #getLineNumber()
	 * @generated
	 */
	void setLineNumber(int value);

	/**
	 * Returns the value of the '<em><b>Nodes</b></em>' containment reference list.
	 * The list contents are of type {@link net.sf.orcc.ir.Node}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Nodes</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Nodes</em>' containment reference list.
	 * @see net.sf.orcc.backends.ir.IrSpecificPackage#getForNode_Nodes()
	 * @model containment="true"
	 * @generated
	 */
	EList<Node> getNodes();

	/**
	 * Returns the value of the '<em><b>Loop Counter</b></em>' containment reference list.
	 * The list contents are of type {@link net.sf.orcc.ir.Expression}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Loop Counter</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Loop Counter</em>' containment reference list.
	 * @see net.sf.orcc.backends.ir.IrSpecificPackage#getForNode_LoopCounter()
	 * @model containment="true"
	 * @generated
	 */
	EList<Expression> getLoopCounter();

	/**
	 * Returns the value of the '<em><b>Init</b></em>' containment reference list.
	 * The list contents are of type {@link net.sf.orcc.ir.Expression}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Init</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Init</em>' containment reference list.
	 * @see net.sf.orcc.backends.ir.IrSpecificPackage#getForNode_Init()
	 * @model containment="true"
	 * @generated
	 */
	EList<Expression> getInit();
	
	/**
	 * Return <code>true</code> if the node is a for node
	 * 
	 * @return <code>true</code> if the instruction is a for node
	 */
	public boolean isForNode();

} // ForNode
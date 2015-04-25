/*******************************************************************************
 * JetUML - A desktop application for fast UML diagramming.
 *
 * Copyright (C) 2015 Cay S. Horstmann and the contributors of the 
 * JetUML project.
 *
 * See: https://github.com/prmr/JetUML
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package ca.mcgill.cs.stg.jetuml.graph;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.Statement;
import java.util.ArrayList;
import java.util.List;

import ca.mcgill.cs.stg.jetuml.framework.Grid;


/**
 * A class that supplies convenience implementations for 
 * a number of methods in the Node interface.
 */
public abstract class AbstractNode implements Node
{
	public static final int SHADOW_GAP = 4;
	
	private static final Color SHADOW_COLOR = Color.LIGHT_GRAY;
	
	private ArrayList<Node> aChildren;
	private transient ArrayList<Edge> aOriginEdges;
	private transient ArrayList<Edge> aTerminalEdges;
	private Node aParent;
	
	/**
     * Constructs a node with no parents or children.
	 */
	public AbstractNode()
	{
		aChildren = new ArrayList<>();
		aOriginEdges = new ArrayList<>();
		aTerminalEdges = new ArrayList<>();
		aParent = null;
	}

	@Override
	public AbstractNode clone()
	{
		try
		{
			AbstractNode cloned = (AbstractNode) super.clone();
			cloned.aChildren = new ArrayList<Node>();
			cloned.aOriginEdges = new ArrayList<Edge>();
			cloned.aTerminalEdges = new ArrayList<Edge>();
			return cloned;
		}
		catch(CloneNotSupportedException exception)
		{
			return null;
		}
	}

	@Override
	public void translate(double pDeltaX, double pDeltaY)
	{
	}

	@Override
	public void addEndEdge(Edge pEdge)
	{
		aTerminalEdges.add(pEdge);
	}
	@Override
	public boolean addEdge(Edge pEdge, Point2D pPoint1, Point2D pPoint2)
	{
		aOriginEdges.add(pEdge);
		if(pEdge.getEnd() != null)
		{
			pEdge.getEnd().addEndEdge(pEdge);
		}
		return pEdge.getEnd() != null;
	}
	
	@Override
	public boolean removeEdge(Graph pGraph, Edge pEdge)
	{
		pEdge.getStart().removeOriginEdge(pGraph, pEdge);
		pEdge.getEnd().removeTerminalEdge(pGraph, pEdge);
		if(!aOriginEdges.contains(pEdge))
		{
			return true;
		}
		return false;
	}

	@Override
	public void removeOriginEdge(Graph pGraph, Edge pEdge)
	{
		aOriginEdges.remove(pEdge);
		pEdge.getEnd().removeTerminalEdge(pGraph, pEdge);
	}
	
	@Override
	public void removeTerminalEdge(Graph pGraph, Edge pEdge)
	{
		aTerminalEdges.remove(pEdge);
	}

	@Override
	public void removeNode(Graph pGraph, Node pNode)
	{
		if(pNode == aParent)
		{
			aParent = null;
		}	 
		if(pNode.getParent() == this)
		{
			aChildren.remove(pNode);
		}
	}
	
	@Override
	public void layout(Graph pGraph, Graphics2D pGraphics2D, Grid pGrid)
	{}

	@Override
	public boolean addNode(Node pNode, Point2D pPoint)
	{
		return false;
	}

	@Override
	public Node getParent() 
   	{ return aParent; }

	@Override
	public void setParent(Node pNode) 
	{ aParent = pNode; }

	@Override
	public List<Node> getChildren() 
	{ return aChildren; }
	
	@Override
	public List<Edge> getOriginEdges()
	{
		return aOriginEdges;
	}
	
	@Override
	public List<Edge> getTerminalEdges()
	{
		return aTerminalEdges;
	}


	@Override
	public void addChild(int pIndex, Node pNode) 
	{
		Node oldParent = pNode.getParent();
		if (oldParent != null)
		{
			oldParent.removeChild(pNode);
		}
		aChildren.add(pIndex, pNode);
		pNode.setParent(this);
	}

	/**
	 * Adds a node at the end of the list.
	 * @param pNode The node to add.
	 */
	public void addChild(Node pNode)
	{
		addChild(aChildren.size(), pNode);
	}

	@Override
	public void removeChild(Node pNode)
	{
		if (pNode.getParent() != this)
		{
			return;
		}
		aChildren.remove(pNode);
		pNode.setParent(null);
	}

	@Override
	public void draw(Graphics2D pGraphics2D)
	{
		Shape shape = getShape();
		if(shape == null)
		{
			return;
		}
		/*
      	Area shadow = new Area(shape);
      	shadow.transform(AffineTransform.getTranslateInstance(SHADOW_GAP, SHADOW_GAP));
      	shadow.subtract(new Area(shape));
		 */
		Color oldColor = pGraphics2D.getColor();
		pGraphics2D.translate(SHADOW_GAP, SHADOW_GAP);      
		pGraphics2D.setColor(SHADOW_COLOR);
		pGraphics2D.fill(shape);
		pGraphics2D.translate(-SHADOW_GAP, -SHADOW_GAP);
		pGraphics2D.setColor(pGraphics2D.getBackground());
		pGraphics2D.fill(shape);      
		pGraphics2D.setColor(oldColor);
	}
   
	/**
     *  @return the shape to be used for computing the drop shadow
    */
	public Shape getShape() 
	{ return null; }   
   
	/**
     *  Adds a persistence delegate to a given encoder that
     * encodes the child nodes of this node.
     * @param pEncoder the encoder to which to add the delegate
     */
	public static void setPersistenceDelegate(Encoder pEncoder)
	{
      pEncoder.setPersistenceDelegate(AbstractNode.class, new DefaultPersistenceDelegate()
         {
            protected void initialize(Class<?> pType, Object pOldInstance, Object pNewInstance, Encoder pOut) 
            {
            	super.initialize(pType, pOldInstance, pNewInstance, pOut);
            	for(Node node : ((Node) pOldInstance).getChildren())
            	{
            		pOut.writeStatement( new Statement(pOldInstance, "addChild", new Object[]{ node }) );            
               }
            }
         });
   }
}


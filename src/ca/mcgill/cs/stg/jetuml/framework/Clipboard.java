package ca.mcgill.cs.stg.jetuml.framework;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ca.mcgill.cs.stg.jetuml.diagrams.ClassDiagramGraph;
import ca.mcgill.cs.stg.jetuml.diagrams.ObjectDiagramGraph;
import ca.mcgill.cs.stg.jetuml.diagrams.SequenceDiagramGraph;
import ca.mcgill.cs.stg.jetuml.diagrams.StateDiagramGraph;
import ca.mcgill.cs.stg.jetuml.diagrams.UseCaseDiagramGraph;
import ca.mcgill.cs.stg.jetuml.graph.CallEdge;
import ca.mcgill.cs.stg.jetuml.graph.CallNode;
import ca.mcgill.cs.stg.jetuml.graph.ClassRelationshipEdge;
import ca.mcgill.cs.stg.jetuml.graph.Edge;
import ca.mcgill.cs.stg.jetuml.graph.Graph;
import ca.mcgill.cs.stg.jetuml.graph.GraphElement;
import ca.mcgill.cs.stg.jetuml.graph.ImplicitParameterNode;
import ca.mcgill.cs.stg.jetuml.graph.Node;
import ca.mcgill.cs.stg.jetuml.graph.ObjectReferenceEdge;
import ca.mcgill.cs.stg.jetuml.graph.ParentNode;
import ca.mcgill.cs.stg.jetuml.graph.ReturnEdge;
import ca.mcgill.cs.stg.jetuml.graph.StateTransitionEdge;

/**
 * @author JoelChev
 * 
 * A class that will be used to store the current graph for cutting/copying and then pasting.
 *
 */
public final class Clipboard 
{
	private List<Node> aNodes;
	private List<Edge> aEdges;

	/**
	 * Creates an empty Clipboard.
	 */
	public Clipboard() 
	{}

	/**
	 * @param pSelection The currently selected elements to add to the Clipboard.
	 */
	public void addSelection(SelectionList pSelection)
	{
		aNodes = new ArrayList<Node>();
		aEdges = new ArrayList<Edge>();
		Map<Node, Node> originalAndClonedNodes = new LinkedHashMap<Node, Node>();
		for(GraphElement element: pSelection)
		{
			if(element instanceof Node)
			{
				Node curNode = (Node) element;
				Node cloneNode = curNode.clone();
 				originalAndClonedNodes.put(curNode, cloneNode);
				aNodes.add(cloneNode);
				//Add children to the Selection if they are not in the current Selection.
				if(curNode instanceof ParentNode)
				{
					for(Node childNode:((ParentNode)curNode).getChildren())
					{
						if(!(pSelection.contains(childNode)))
						{
							Node clonedChildNode = childNode.clone();
							originalAndClonedNodes.put(childNode, clonedChildNode);
							aNodes.add(clonedChildNode);
						}
					}
				}
			}
		}
		for(GraphElement element: pSelection) //loop through and fix the parent/child relationships for all the clone children
		{
			fixParentChildRelationShips(element, originalAndClonedNodes);
		}	
		for(GraphElement element: pSelection)
		{
			if(element instanceof Edge)
			{
				Edge curEdge = (Edge) element;
				Node start = originalAndClonedNodes.get(curEdge.getStart());
				Node end = originalAndClonedNodes.get(curEdge.getEnd());  
				if (start != null && end != null)
				{
					Edge cloneEdge = (Edge) curEdge.clone();
					cloneEdge.connect(start, end);
					aEdges.add(cloneEdge);
				}
			}
		}
	}
	
	/**
	 * Fixes the parent child relationships of the cloned children nodes. 
	 * @param pElement the current GraphElement being considered.
	 * @param pOriginalAndClonedNodes the LinkedHasMap of original and cloned nodes.
	 */
	private void fixParentChildRelationShips(GraphElement pElement, Map<Node, Node> pOriginalAndClonedNodes)
	{
		if(pElement instanceof ParentNode)
		{
			ParentNode curNode = (ParentNode) pElement;
			if(!curNode.getChildren().isEmpty())
			{
				ParentNode cloneNode = (ParentNode)pOriginalAndClonedNodes.get(curNode);
				List<ParentNode> cloneChildren = cloneNode.getChildren();
				for(int i = 0; i < cloneChildren.size(); i++) //Repalce all children with their clones
				{
					ParentNode removed = cloneChildren.remove(i);
					ParentNode replacement = (ParentNode)pOriginalAndClonedNodes.get(removed);
					cloneChildren.add(i, replacement);
				}
			}
			if(curNode.getParent() != null)
			{
				ParentNode cloneNode = (ParentNode)pOriginalAndClonedNodes.get(curNode); //replace parent with its clone
				ParentNode cloneParent = (ParentNode)pOriginalAndClonedNodes.get(curNode.getParent());
				cloneNode.setParent(cloneParent);
			}
			if(curNode instanceof CallNode && ((CallNode)curNode).getImplicitParameter() != null)
			{
				Node cloneNode = pOriginalAndClonedNodes.get(curNode); //replace parent with its clone
				Node cloneParent = pOriginalAndClonedNodes.get(((CallNode)curNode).getImplicitParameter());
				((CallNode)cloneNode).setImplicitParameter((ImplicitParameterNode)cloneParent);
			}
		}
	}

	/**
	 * A wrapper method for the pasteInto method without the pOriginalPositions parameter.
	 * @param pGraphPanel The current GraphPanel to paste contents to.
	 * @return The elements to paste as a selectionList.
	 */
	public SelectionList pasteInto(GraphPanel pGraphPanel)
	{
		return pasteInto(pGraphPanel, false);
	}

	/**
	 * @param pGraphPanel The current GraphPanel to paste contents to.
	 * @param pOriginalPositions Whether to paste in the original position or not.
	 * @return The elements to paste as a selectionList.
	 */
	public SelectionList pasteInto(GraphPanel pGraphPanel, boolean pOriginalPositions)
	{
		Rectangle2D bounds = null;
		Graph aGraph = pGraphPanel.getGraph();
		Node[]currentProtoTypes = aGraph.getNodePrototypes();
		Edge[]currentEdgeTypes = aGraph.getEdgePrototypes();
		ArrayList<Node> copyNodes = new ArrayList<Node>();
		/*
		 * Clone all nodes and remember the original-cloned correspondence
		 */
		Map<Node, Node> originalAndClonedNodes = new LinkedHashMap<Node, Node>();
		/*
		 * First clone all of the nodes and link them with the previous nodes. All the nodes will be iterated over in
		 * the pastSelection SelectionList
		 */
		for(Node curNode: aNodes)
		{
			for(Node n: currentProtoTypes)
			{
				if(curNode.getClass() == n.getClass())
				{
					Node newNode = curNode.clone();
					originalAndClonedNodes.put(curNode, newNode);
					copyNodes.add(newNode);

					if(bounds ==null)
					{
						bounds = curNode.getBounds();
					}
					else
					{
						bounds.add(curNode.getBounds());
					}
				}
			}
		}
		for(GraphElement element: aNodes) //loop through and fix the parent/child relationships for all the clone children
		{
			fixParentChildRelationShips(element, originalAndClonedNodes);
		}
		/*
		 * Now the edges can be cloned as all the nodes have been cloned successfully at this point.
		 * The edges will be iterated over in the pastSelection SelectionList.
		 */
		ArrayList<Edge> copyEdges = new ArrayList<Edge>();
		for( Edge curEdge: aEdges)
		{
			for(Edge e: currentEdgeTypes)
			{
				/*
				 * Clone all edges that join copied nodes
				 */
				Node start = originalAndClonedNodes.get(curEdge.getStart());
				Node end = originalAndClonedNodes.get(curEdge.getEnd());  
				if(checkEdgeEquality(curEdge, e, aGraph) &&start != null && end != null)
				{
						Edge e2 = (Edge) e.clone();
						aGraph.connect(e2, start, end);
						copyEdges.add(e2);
				}
			}
		}	
		return constructNewSelectionList(pGraphPanel, copyNodes, copyEdges, pOriginalPositions, bounds);
	}

	/**
	 * A helper method to construct the new SelectionList in the graph.
	 * 
	 * @param pGraphPanel the current GraphPanel.
	 * @param pCopyNodes the list of copied Nodes in the paste operation.
	 * @param pCopyEdges the list of copied Edges int the paste operation.
	 * @param pOriginalPositions the parameter specifying if the new elements are translated or not.
	 * @param pBounds the bounds to translate the elements.
	 * @return a new SelectionList with the pasted elements.
	 */
	private SelectionList constructNewSelectionList(GraphPanel pGraphPanel, ArrayList<Node> pCopyNodes, ArrayList<Edge> pCopyEdges, 
			boolean pOriginalPositions, Rectangle2D pBounds)
	{
		/*
		 * updatedSelectionList is the selectionList to return.
		 */
		Graph aGraph = pGraphPanel.getGraph();
		SelectionList updatedSelectionList = new SelectionList();
		pGraphPanel.startCompoundListening();
		for(Node cloneNode: pCopyNodes)
		{
			double x = cloneNode.getBounds().getX();
			double y = cloneNode.getBounds().getY();
			if(!pOriginalPositions)
			{
				/*
				 * This translates all the new nodes and their respective edges over to the top left corner of the 
				 * GraphPanel.
				 */
				aGraph.add(cloneNode, new Point2D.Double(x-pBounds.getX(), y-pBounds.getY()));
			}
			else
			{
				aGraph.add(cloneNode, new Point2D.Double(x, y));
			}
			/*
			 * Don't add any Children to the SelectionList
			 */
			if(!(cloneNode instanceof ParentNode && ((ParentNode)cloneNode).getParent()!=null))
			{
				updatedSelectionList.add(cloneNode);
			}
		}
		for(Edge cloneEdge: pCopyEdges)
		{
			/*
			 * If the start and end nodes of a given edge are both CallNodes, then the Graph connection
			 * is skipped. Otherwise duplicate Call Nodes are produced. This is due to nodes now internally
			 * storing their origin and terminal edges.
			 */
			if(!(cloneEdge.getStart() instanceof CallNode && cloneEdge.getEnd() instanceof CallNode))
			{
				Point2D startCenter = new Point2D.Double(cloneEdge.getStart().getBounds().getCenterX(), 
						cloneEdge.getStart().getBounds().getCenterY());
				Point2D endCenter = new Point2D.Double(cloneEdge.getEnd().getBounds().getCenterX(), cloneEdge.getEnd().getBounds().getCenterY());
				aGraph.connect(cloneEdge, startCenter, endCenter);
			}	
			updatedSelectionList.add(cloneEdge);
		}
		pGraphPanel.endCompoundListening();
		return updatedSelectionList;
	}
	/**
	 * @param pEdge1 The copied or cut edge whose actual type needs to be determined.
	 * @param pEdge2 The edge from the list of edge types in the pGraph.
	 * @param pGraph The current graph in the GraphPanel.
	 * @return true if the two edges have the same type and false if not.
	 * NOTE: All Note edges are removed following this check.
	 */
	private boolean checkEdgeEquality(Edge pEdge1, Edge pEdge2, Graph pGraph)
	{
		boolean equal = false;
		if(pGraph instanceof ClassDiagramGraph || pGraph instanceof UseCaseDiagramGraph)
		{
			if(pEdge1 instanceof ClassRelationshipEdge && pEdge2 instanceof ClassRelationshipEdge)
			{
				equal = classDiagramEdgeEqual((ClassRelationshipEdge)pEdge1, (ClassRelationshipEdge)pEdge2, pGraph);

			}
		}
		else if(pGraph instanceof ObjectDiagramGraph)
		{
			if(pEdge1 instanceof ClassRelationshipEdge && pEdge2 instanceof ClassRelationshipEdge || 
					pEdge1 instanceof ObjectReferenceEdge && pEdge2 instanceof ObjectReferenceEdge)
			{
				equal = true;
			}
		}
		else if(pGraph instanceof SequenceDiagramGraph)
		{
			if(pEdge1 instanceof CallEdge && pEdge2 instanceof CallEdge || pEdge1 instanceof ReturnEdge && pEdge2 instanceof ReturnEdge)
			{
				equal = true;
			}
		}
		else if(pGraph instanceof StateDiagramGraph)
		{
			if(pEdge1 instanceof StateTransitionEdge && pEdge2 instanceof StateTransitionEdge)
			{
				equal = true;
			}
		}
		return equal;
	}

	/**
	 * @param pEdge1 The copied or cut Edge whose actual type needs to be determined.
	 * @param pEdge2 The edge from the list of ClassRelationshipEdges in the pGraph.
	 * @param pGraph The current ClassDiagramGraph in the GraphPanel
	 * @return true if the two edges have the same type, false otherwise.
	 */
	private boolean classDiagramEdgeEqual(ClassRelationshipEdge pEdge1, ClassRelationshipEdge pEdge2, Graph pGraph)
	{
		if(pEdge1.getLineStyle() == pEdge2.getLineStyle())
		{
			if(pEdge1.getStartArrowHead() == pEdge2.getStartArrowHead())
			{
				if(pEdge1.getEndArrowHead()== pEdge2.getEndArrowHead())
				{
					if(pEdge1.getBentStyle() == pEdge2.getBentStyle())
					{
						if(pEdge1.getMiddleLabel().equals(pEdge2.getMiddleLabel()))
						{
							return true;
						}
					}
				}
			}
		}
		return false;
	}

}






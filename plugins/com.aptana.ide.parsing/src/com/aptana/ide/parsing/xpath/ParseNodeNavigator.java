/**
 * This file Copyright (c) 2005-2008 Aptana, Inc. This program is
 * dual-licensed under both the Aptana Public License and the GNU General
 * Public license. You may elect to use one or the other of these licenses.
 * 
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT. Redistribution, except as permitted by whichever of
 * the GPL or APL you select, is prohibited.
 *
 * 1. For the GPL license (GPL), you can redistribute and/or modify this
 * program under the terms of the GNU General Public License,
 * Version 3, as published by the Free Software Foundation.  You should
 * have received a copy of the GNU General Public License, Version 3 along
 * with this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Aptana provides a special exception to allow redistribution of this file
 * with certain other free and open source software ("FOSS") code and certain additional terms
 * pursuant to Section 7 of the GPL. You may view the exception and these
 * terms on the web at http://www.aptana.com/legal/gpl/.
 * 
 * 2. For the Aptana Public License (APL), this program and the
 * accompanying materials are made available under the terms of the APL
 * v1.0 which accompanies this distribution, and is available at
 * http://www.aptana.com/legal/apl/.
 * 
 * You may view the GPL, Aptana's exception and additional terms, and the
 * APL in the file titled license.html at the root of the corresponding
 * plugin containing this source file.
 * 
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.ide.parsing.xpath;

import java.util.Iterator;

import org.jaxen.DefaultNavigator;
import org.jaxen.JaxenConstants;
import org.jaxen.UnsupportedAxisException;
import org.jaxen.XPath;
import org.jaxen.saxpath.SAXPathException;

import com.aptana.ide.parsing.nodes.IParseNode;
import com.aptana.ide.parsing.nodes.IParseNodeAttribute;

/**
 * @author Kevin Lindsey
 */
public class ParseNodeNavigator extends DefaultNavigator
{
	private static final long serialVersionUID = 841503047993117262L;
	private static ParseNodeNavigator _instance = new ParseNodeNavigator();

	/**
	 * getInstance
	 * 
	 * @return ParseNodeNavigator
	 */
	public static ParseNodeNavigator getInstance()
	{
		return _instance;
	}

	/**
	 * @see org.jaxen.DefaultNavigator#getAttributeAxisIterator(java.lang.Object)
	 */
	public Iterator getAttributeAxisIterator(Object contextNode) throws UnsupportedAxisException
	{
		if (isElement(contextNode))
		{
			IParseNode element = (IParseNode) contextNode;

			return new ParseNodeAttributeIterator(element.getAttributes());
		}
		else
		{
			return JaxenConstants.EMPTY_ITERATOR;
		}
	}

	/**
	 * @see org.jaxen.DefaultNavigator#getChildAxisIterator(java.lang.Object)
	 */
	public Iterator getChildAxisIterator(Object contextNode) throws UnsupportedAxisException
	{
		// System.out.println("Get child axis iterator for: " + ((IParseNode) contextNode).getSource());

		if (contextNode instanceof IParseNode)
		{
			return new ParseNodeIterator((IParseNode) contextNode)
			{
				protected IParseNode getFirstNode(IParseNode node)
				{
					return node.getChild(0);
				}

				protected IParseNode getNextNode(IParseNode node)
				{
					int index = node.getChildIndex();

					return node.getParent().getChild(index + 1);
				}
			};
		}
		else
		{
			return new Iterator()
			{
				public void remove()
				{
				}

				public boolean hasNext()
				{
					return false;
				}

				public Object next()
				{
					return null;
				}
			};
		}
	}

	/*
	 * Methods
	 */

	/**
	 * @see org.jaxen.DefaultNavigator#getDocumentNode(java.lang.Object)
	 */
	public Object getDocumentNode(Object contextNode)
	{
		IParseNode result = null;

		if (contextNode instanceof IParseNode)
		{
			result = (IParseNode) contextNode;

			while (result.getParent() != null)
			{
				result = result.getParent();
			}
		}

		return result;
	}

	/*
	 * Necessary overrides
	 */

	/**
	 * @see org.jaxen.Navigator#getElementNamespaceUri(java.lang.Object)
	 */
	public String getElementNamespaceUri(Object element)
	{
		// System.out.println("getElementNamespaceUri");

		return null;
	}

	/**
	 * @see org.jaxen.Navigator#getElementName(java.lang.Object)
	 */
	public String getElementName(Object element)
	{
		// System.out.println("getElementName: " + ((IParseNode) element).getName());

		return ((IParseNode) element).getName();
	}

	/**
	 * @see org.jaxen.Navigator#getElementQName(java.lang.Object)
	 */
	public String getElementQName(Object element)
	{
		// System.out.println("getElementQName");

		return null;
	}

	/**
	 * @see org.jaxen.Navigator#getAttributeNamespaceUri(java.lang.Object)
	 */
	public String getAttributeNamespaceUri(Object attr)
	{
		// System.out.println("getAttributeNamespaceUri");

		return null;
	}

	/**
	 * @see org.jaxen.Navigator#getAttributeName(java.lang.Object)
	 */
	public String getAttributeName(Object attr)
	{
		if (isAttribute(attr))
		{
			return ((IParseNodeAttribute) attr).getName();
		}
		else
		{
			return null;
		}
	}

	/**
	 * @see org.jaxen.Navigator#getAttributeQName(java.lang.Object)
	 */
	public String getAttributeQName(Object attr)
	{
		// System.out.println("getAttributeQName");

		return null;
	}

	/**
	 * @see org.jaxen.Navigator#isDocument(java.lang.Object)
	 */
	public boolean isDocument(Object object)
	{
		// System.out.println("isDocument");

		return false;
	}

	/**
	 * @see org.jaxen.Navigator#isElement(java.lang.Object)
	 */
	public boolean isElement(Object object)
	{
		return (object instanceof IParseNode);
	}

	/**
	 * @see org.jaxen.Navigator#isAttribute(java.lang.Object)
	 */
	public boolean isAttribute(Object object)
	{
		return (object instanceof IParseNodeAttribute);
	}

	/**
	 * @see org.jaxen.Navigator#isNamespace(java.lang.Object)
	 */
	public boolean isNamespace(Object object)
	{
		// System.out.println("isNamespace: " + object);

		return false;
	}

	/**
	 * @see org.jaxen.Navigator#isComment(java.lang.Object)
	 */
	public boolean isComment(Object object)
	{
		// System.out.println("isComment: " + object);

		return false;
	}

	/**
	 * @see org.jaxen.Navigator#isText(java.lang.Object)
	 */
	public boolean isText(Object object)
	{
		return (object instanceof String);
	}

	/**
	 * @see org.jaxen.Navigator#isProcessingInstruction(java.lang.Object)
	 */
	public boolean isProcessingInstruction(Object object)
	{
		// System.out.println("isProcessingInstruction: " + object);

		return false;
	}

	/**
	 * @see org.jaxen.Navigator#getCommentStringValue(java.lang.Object)
	 */
	public String getCommentStringValue(Object comment)
	{
		// System.out.println("getCommentStringValue");

		return null;
	}

	/**
	 * @see org.jaxen.Navigator#getElementStringValue(java.lang.Object)
	 */
	public String getElementStringValue(Object element)
	{
		// System.out.println("getElementStringValue");

		return null;
	}

	/**
	 * @see org.jaxen.Navigator#getAttributeStringValue(java.lang.Object)
	 */
	public String getAttributeStringValue(Object attr)
	{
		if (isAttribute(attr))
		{
			return ((IParseNodeAttribute) attr).getValue();
		}
		else
		{
			return null;
		}
	}

	/**
	 * @see org.jaxen.Navigator#getNamespaceStringValue(java.lang.Object)
	 */
	public String getNamespaceStringValue(Object ns)
	{
		// System.out.println("getNamespaceStringValue");

		return null;
	}

	/**
	 * @see org.jaxen.DefaultNavigator#getParentNode(java.lang.Object)
	 */
	public Object getParentNode(Object contextNode) throws UnsupportedAxisException
	{
		if (isAttribute(contextNode))
		{
			return ((IParseNodeAttribute) contextNode).getParent();
		}
		else
		{
			return ((IParseNode) contextNode).getParent();
		}
	}

	/**
	 * @see org.jaxen.Navigator#getTextStringValue(java.lang.Object)
	 */
	public String getTextStringValue(Object text)
	{
		if (isText(text))
		{
			return text.toString();
		}
		else
		{
			return null;
		}
	}

	/**
	 * @see org.jaxen.Navigator#getNamespacePrefix(java.lang.Object)
	 */
	public String getNamespacePrefix(Object ns)
	{
		// System.out.println("getNamespacePrefix");

		return null;
	}

	/**
	 * @see org.jaxen.Navigator#parseXPath(java.lang.String)
	 */
	public XPath parseXPath(String xpath) throws SAXPathException
	{
		// System.out.println("parseXPath");

		return null;
	}
}

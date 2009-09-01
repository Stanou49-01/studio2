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
package com.aptana.sax;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.aptana.ide.io.SourceWriter;

/**
 * @author Kevin Lindsey
 */
public class SchemaElement
{
	private static final Class<?>[] enterSignature = new Class[] { String.class, String.class, String.class, Attributes.class };
	private static final Class<?>[] exitSignature = new Class[] { String.class, String.class, String.class };

	private String _name;
	private Schema _owningSchema;
	private Map<String,SchemaElement> _transitions;
	private Map<String,Integer> _attributes;
	private List<String> _requiredAttributes;

	private String _instanceAttributes;

	private Method _onEnter;
	private Method _onExit;
	private boolean _hasText;

	/**
	 * Create a new instance of SchemaNode
	 * 
	 * @param owningSchema
	 *            The schema that owns this element
	 * @param name
	 *            The name of this node
	 */
	public SchemaElement(Schema owningSchema, String name)
	{
		// make sure we have a valid schema reference
		if (owningSchema == null)
		{
			throw new IllegalArgumentException(Messages.SchemaElement_Undefined_Owning_Schema);
		}

		// make sure we have a valid name
		if (name == null || name.length() == 0)
		{
			throw new IllegalArgumentException(Messages.SchemaElement_Undefined_Name);
		}

		this._owningSchema = owningSchema;
		this._name = name;
		this._transitions = new HashMap<String,SchemaElement>();
		this._attributes = new HashMap<String,Integer>();
		this._requiredAttributes = new ArrayList<String>();
	}
	
	/**
	 * Get the name associated with this Schema node
	 * 
	 * @return this node's name
	 */
	public String getName()
	{
		return this._name;
	}

	/**
	 * Return the Method to call when entering this element
	 * 
	 * @return The Method to invoke. This value can be null if there is no OnEnter event handler associated with this
	 *         element
	 */
	public Method getOnEnterMethod()
	{
		return this._onEnter;
	}

	/**
	 * Set a flag indicating whether this element expects text as a child node
	 * 
	 * @param value
	 */
	public void setHasText(boolean value)
	{
		this._hasText = value;
	}
	
	/**
	 * Set the method to call after entering this element
	 * 
	 * @param onEnterMethod
	 *            The name of the method to call on the schema's handler object when we enter this element
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	public void setOnEnter(String onEnterMethod) throws SecurityException, NoSuchMethodException
	{
		Class<?> handlerClass = this._owningSchema.getHandlerClass();

		if (handlerClass != null)
		{
			// this._onEnter = handlerClass.getDeclaredMethod(onEnterMethod,
			// enterSignature);
			this._onEnter = handlerClass.getMethod(onEnterMethod, enterSignature);
		}
		else
		{
			this._onEnter = null;
		}
	}

	/**
	 * Return the Method to call when exiting this element
	 * 
	 * @return The Method to invoke. This value can be null if there is no OnExit event handler associated with this
	 *         element
	 */
	public Method getOnExitMethod()
	{
		return this._onExit;
	}

	/**
	 * Set the method to call before exiting this element
	 * 
	 * @param onExitMethod
	 *            The name of the method to call on the schema's handler object when we exit this element
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	public void setOnExit(String onExitMethod) throws SecurityException, NoSuchMethodException
	{
		Class<?> handlerClass = this._owningSchema.getHandlerClass();

		if (handlerClass != null)
		{
			// this._onExit = handlerClass.getDeclaredMethod(onExitMethod,
			// exitSignature);
			this._onExit = handlerClass.getMethod(onExitMethod, exitSignature);
		}
		else
		{
			this._onExit = null;
		}
	}

	/**
	 * getTransitionElements
	 *
	 * @return Returns an array of schema elements to which this element can transition.
	 */
	public SchemaElement[] getTransitionElements()
	{
		Collection<SchemaElement> values = this._transitions.values();
		
		return values.toArray(new SchemaElement[values.size()]);
	}
	
	/**
	 * Determine if this element has a definition for the specified attribute name
	 * 
	 * @param name
	 *            The name of the attribute to test
	 * @return Returns true if this element has an entry for the specified attribute name
	 */
	public boolean hasAttribute(String name)
	{
		return (this._attributes.containsKey(name));
	}

	/**
	 * Determine if this element has an associated OnEnter handler
	 * 
	 * @return Returns true if this element has an OnEnter handler
	 */
	public boolean hasOnEnterMethod()
	{
		return (this._onEnter != null);
	}

	/**
	 * Determine if this element has an associated OnExit handler
	 * 
	 * @return Returns true if this element has an OnExit handler
	 */
	public boolean hasOnExitMethod()
	{
		return (this._onExit != null);
	}

	/**
	 * Determine if this element expects text as a child node or not
	 * 
	 * @return Returns true if this element expects to contain text
	 */
	public boolean hasText()
	{
		return this._hasText;
	}
	
	/**
	 * hasTransitions
	 *
	 * @return Returns true if this element has transitions
	 */
	public boolean hasTransitions()
	{
		return (this._transitions.size() > 0);
	}
	
	/**
	 * Determine if the specified attribute name is optional on this element
	 * 
	 * @param name
	 *            The name of the attribute to test
	 * @return Returns true if the specified attribute name does not have to exist on this element
	 */
	public boolean isDeprecatedAttribute(String name)
	{
		boolean result = false;

		if (this.isValidAttribute(name))
		{
			int flags = this._attributes.get(name).intValue();

			result = ((flags & AttributeUsage.DEPRECATED) == AttributeUsage.DEPRECATED);
		}

		return result;
	}
	
	/**
	 * Determine if the specified attribute name is optional on this element
	 * 
	 * @param name
	 *            The name of the attribute to test
	 * @return Returns true if the specified attribute name does not have to exist on this element
	 */
	public boolean isOptionalAttribute(String name)
	{
		boolean result = false;

		if (this.isValidAttribute(name))
		{
			int flags = this._attributes.get(name).intValue();

			result = ((flags & AttributeUsage.USAGE_MASK) == AttributeUsage.OPTIONAL);
		}

		return result;
	}

	/**
	 * Determine if the specified attribute name is required on this element
	 * 
	 * @param name
	 *            The name of the attribute to test
	 * @return Returns true if the specified attribute name must exist on this element
	 */
	public boolean isRequiredAttribute(String name)
	{
		boolean result = false;

		if (this.isValidAttribute(name))
		{
			int flags = this._attributes.get(name).intValue();

			result = ((flags & AttributeUsage.USAGE_MASK) == AttributeUsage.REQUIRED);
		}

		return result;
	}

	/**
	 * Determine if the specified attribute name is allowed on this element
	 * 
	 * @param name
	 *            The name of the attribute to test
	 * @return Returns true if the specified attribute name is allowed on this element
	 */
	public boolean isValidAttribute(String name)
	{
		return (this._attributes.containsKey(name));
	}

	/**
	 * Determine if this node can transition to another node using the given name
	 * 
	 * @param name
	 *            The name of the node to test as a possible transition target
	 * @return Returns true if this node can transition to the given node name
	 */
	public boolean isValidTransition(String name)
	{
		return this._transitions.containsKey(name);
	}

	/**
	 * Add an attribute to this element
	 * 
	 * @param name
	 *            The name of the attribute
	 * @param usage
	 *            The usage requirements for the attribute
	 */
	public void addAttribute(String name, String usage)
	{
		// make sure we have a valid name
		if (name == null || name.length() == 0)
		{
			throw new IllegalArgumentException(Messages.SchemaElement_Undefined_Name);
		}

		// make sure we haven't defined this attribute already
		if (this.hasAttribute(name))
		{
			String msg = MessageFormat.format(Messages.SchemaElement_Attribute_already_defined, name, this._name);
			throw new IllegalArgumentException(msg);
		}

		int usageValue;

		if (usage != null)
		{
			if (usage.equals("required")) //$NON-NLS-1$
			{
				usageValue = AttributeUsage.REQUIRED;
			}
			else if (usage.equals("optional")) //$NON-NLS-1$
			{
				usageValue = AttributeUsage.OPTIONAL;
			}
			else
			{
				String msg = MessageFormat.format(Messages.SchemaElement_Not_valid_usage_attribute, usage);
				throw new IllegalArgumentException(msg);
			}
		}
		else
		{
			usageValue = AttributeUsage.REQUIRED;
		}

		// store attribute and attribute usage
		this._attributes.put(name, new Integer(usageValue));

		// add required attributes to array list for easier testing
		if ((usageValue & AttributeUsage.USAGE_MASK) == AttributeUsage.REQUIRED)
		{
			this._requiredAttributes.add(name);
		}
	}

	/**
	 * Add a transition out of this node to another node
	 * 
	 * @param node
	 *            The node to which this node can transition
	 */
	public void addTransition(SchemaElement node)
	{
		// make sure we have a valid object
		if (node == null)
		{
			throw new IllegalArgumentException(Messages.SchemaElement_Undefined_Node);
		}

		// get the new node's name
		String nodeName = node.getName();

		// make sure we haven't added this name already
		if (this.isValidTransition(nodeName))
		{
			String msg = "A node name '" + nodeName + "' has already been added to " + this._name; //$NON-NLS-1$ //$NON-NLS-2$

			throw new IllegalArgumentException(msg);
		}

		// add a transition to the new node
		this._transitions.put(nodeName, node);
	}

	/**
	 * Get the named SchemaElement that transitions from this element
	 * 
	 * @param name
	 *            The name of the SchemaElement to transition to
	 * @return The new SchemaElement
	 */
	public SchemaElement moveTo(String name)
	{
		return this._transitions.get(name);
	}

	/**
	 * Validate the list of attributes against this element's definition. Required attributes must exist and no
	 * attributes can be in the list that have not been defined for this element.
	 * 
	 * @param attributes
	 *            The list of attributes to test
	 * @throws SAXException
	 */
	public void validateAttributes(Attributes attributes) throws SAXException
	{
		// save attributes for possible error messaging
		if (attributes.getLength() > 0)
		{
			this._instanceAttributes = ""; //$NON-NLS-1$

			for (int i = 0; i < attributes.getLength(); i++)
			{
				String key = attributes.getLocalName(i);
				String value = attributes.getValue(i);

				this._instanceAttributes += " " + key + "=\"" + value + "\""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}

		// make sure all required attributes are in the list
		for (int i = 0; i < this._requiredAttributes.size(); i++)
		{
			String name = this._requiredAttributes.get(i);
			String value = attributes.getValue(name);

			if (value == null)
			{
				SourceWriter writer = new SourceWriter();

				writer.print("<").print(this._name).print("> requires a '").print(name).println("' attribute"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				this._owningSchema.buildErrorMessage(writer, this._name, attributes);

				throw new SAXException(writer.toString());
			}
		}

		// make sure all attributes are allowed on this element
		for (int i = 0; i < attributes.getLength(); i++)
		{
			String name = attributes.getLocalName(i);

			if (this._attributes.containsKey(name) == false)
			{
				SourceWriter writer = new SourceWriter();
				writer.println(MessageFormat.format(Messages.SchemaElement_Invalid_attribute_on_tag, name, this._name));
				this._owningSchema.buildErrorMessage(writer, this._name, attributes);

				throw new SAXException(writer.toString());
			}
		}
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		String result = "<" + this._name; //$NON-NLS-1$

		if (this._instanceAttributes != null)
		{
			result += this._instanceAttributes;
		}

		if (this.hasTransitions())
		{
			result += ">"; //$NON-NLS-1$
		}
		else
		{
			result += "/>"; //$NON-NLS-1$
		}

		return result;
	}
}

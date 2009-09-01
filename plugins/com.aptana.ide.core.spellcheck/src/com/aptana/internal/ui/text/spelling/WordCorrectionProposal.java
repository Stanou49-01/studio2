/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.aptana.internal.ui.text.spelling;

import java.text.MessageFormat;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;

/**
 * Proposal to correct the incorrectly spelled word.
 * 
 * @since 3.0
 */
public class WordCorrectionProposal implements ICompletionProposal {

	/**
	 * Returns the html representation of the specified string.
	 * 
	 * @param string
	 *            The string to return the html representation for
	 * @return The html representation for the string
	 */
	public static String getHtmlRepresentation(final String string) {

		final int length = string.length();
		final StringBuffer buffer = new StringBuffer(string);

		for (int offset = length - 1; offset >= 0; offset--) {

			for (int index = 0; index < IHtmlTagConstants.HTML_ENTITY_CHARACTERS.length; index++) {

				if (string.charAt(offset) == IHtmlTagConstants.HTML_ENTITY_CHARACTERS[index]) {

					buffer
							.replace(
									offset,
									offset + 1,
									String
											.valueOf(IHtmlTagConstants.HTML_ENTITY_CODES[index]));
					break;
				}
			}
		}
		return buffer.toString();
	}

	/** The length in the document */
	private final int fLength;

	/** The line where to apply the correction */
	private final String fLine;

	/** The offset in the document */
	private final int fOffset;

	/** The relevance of this proposal */
	private final int fRelevance;

	/** The word to complete */
	private final String fWord;

	private final IInvocationContext context;

	/**
	 * Creates a new word correction proposal.
	 * 
	 * @param word
	 *            the corrected word
	 * @param arguments
	 *            the problem arguments associated with the spelling problem
	 * @param offset
	 *            the offset in the document where to apply the proposal
	 * @param length
	 *            the lenght in the document to apply the proposal
	 * @param context
	 * @param context
	 *            the invocation context for this proposal
	 * @param relevance
	 *            the relevance of this proposal
	 */
	public WordCorrectionProposal(final String word, final String[] arguments,
			final int offset, final int length, IInvocationContext context,
			final int relevance) {
		this.context = context;
		this.fWord = Character.isUpperCase(arguments[0].charAt(0)) ? Character
				.toUpperCase(word.charAt(0))
				+ word.substring(1) : word;

		this.fOffset = offset;
		this.fLength = length;
		this.fRelevance = relevance;

		final StringBuffer buffer = new StringBuffer(80);

		buffer.append("...<br>"); //$NON-NLS-1$
		buffer.append(getHtmlRepresentation(arguments[1]));
		buffer.append("<b>"); //$NON-NLS-1$
		buffer.append(getHtmlRepresentation(this.fWord));
		buffer.append("</b>"); //$NON-NLS-1$
		buffer.append(getHtmlRepresentation(arguments[2]));
		buffer.append("<br>..."); //$NON-NLS-1$

		this.fLine = buffer.toString();
	}

	/*
	 * @see
	 * org.eclipse.jface.text.contentassist.ICompletionProposal#apply(org.eclipse
	 * .jface.text.IDocument)
	 */
	public final void apply(final IDocument document) {
		try {
			document.replace(this.fOffset, this.fLength, this.fWord);
		} catch (final BadLocationException exception) {
			// Do nothing
		}
	}

	/*
	 * @seeorg.eclipse.jface.text.contentassist.ICompletionProposal#
	 * getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		return this.fLine;
	}

	/*
	 * @seeorg.eclipse.jface.text.contentassist.ICompletionProposal#
	 * getContextInformation()
	 */
	public final IContextInformation getContextInformation() {
		return null;
	}

	/*
	 * @see
	 * org.eclipse.jface.text.contentassist.ICompletionProposal#getDisplayString
	 * ()
	 */
	public String getDisplayString() {
		return MessageFormat.format("{0}", this.fWord); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_RENAME);
	}

	/*
	 * @see org.eclipse.jdt.ui.text.java.IJavaCompletionProposal#getRelevance()
	 */
	public final int getRelevance() {
		return this.fRelevance;
	}

	/*
	 * @see
	 * org.eclipse.jface.text.contentassist.ICompletionProposal#getSelection
	 * (org.eclipse.jface.text.IDocument)
	 */
	public final Point getSelection(final IDocument document) {

		int offset = this.context.getSelectionOffset();
		int length = this.context.getSelectionLength();

		final int delta = this.fWord.length() - this.fLength;
		if ((offset <= this.fOffset) && (offset + length >= this.fOffset)) {
			length += delta;
		} else if ((offset > this.fOffset)
				&& (offset + length > this.fOffset + this.fLength)) {
			offset += delta;
			length -= delta;
		} else {
			length += delta;
		}

		return new Point(offset, length);
	}
}

package com.fs.starfarer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;

/**
 * Exact-output specialization of TXW2's IndentingXMLStreamWriter for the
 * Starsector save format, which configures an empty indent step.
 *
 * The stock implementation still performs one writeCharacters("") call for
 * every nesting level on every start/end element. In CheerpJ those no-op
 * interface calls are expensive. This class preserves the stock newline/state
 * behavior byte-for-byte while eliminating only the empty indentation loop.
 */
public final class BrowserZeroIndentXMLStreamWriter extends IndentingXMLStreamWriter {
    private static final java.lang.String ENABLE_PROPERTY = "starsector.browserZeroIndentXmlWriter";
    private static final int SEEN_NOTHING = 0;
    private static final int SEEN_ELEMENT = 1;
    private static final int SEEN_DATA = 2;

    private final XMLStreamWriter delegate;
    private final boolean fastPath;
    private int state = SEEN_NOTHING;
    private int depth = 0;

    public BrowserZeroIndentXMLStreamWriter(final XMLStreamWriter delegate) {
        super(delegate);
        this.delegate = delegate;
        this.fastPath = Boolean.parseBoolean(System.getProperty(ENABLE_PROPERTY, "false"));
    }

    public boolean isFastPathEnabled() {
        return fastPath;
    }

    private void onStartElementFast() throws XMLStreamException {
        // IndentingXMLStreamWriter pushes SEEN_ELEMENT (not the prior state),
        // resets current state, emits one newline for nested starts, then calls
        // doIndent(). With indentStep="", doIndent() emits zero bytes.
        state = SEEN_NOTHING;
        if (depth > 0) delegate.writeCharacters("\n");
        depth++;
    }

    private void onEndElementFast() throws XMLStreamException {
        depth--;
        if (state == SEEN_ELEMENT) delegate.writeCharacters("\n");
        // The stock stack always restores SEEN_ELEMENT because onStartElement
        // always pushes that constant. No stack allocation is therefore needed.
        state = SEEN_ELEMENT;
    }

    private void onEmptyElementFast() throws XMLStreamException {
        state = SEEN_ELEMENT;
        if (depth > 0) delegate.writeCharacters("\n");
    }

    @Override
    public void writeStartDocument() throws XMLStreamException {
        if (!fastPath) { super.writeStartDocument(); return; }
        delegate.writeStartDocument();
        delegate.writeCharacters("\n");
    }

    @Override
    public void writeStartDocument(final java.lang.String version) throws XMLStreamException {
        if (!fastPath) { super.writeStartDocument(version); return; }
        delegate.writeStartDocument(version);
        delegate.writeCharacters("\n");
    }

    @Override
    public void writeStartDocument(final java.lang.String encoding, final java.lang.String version) throws XMLStreamException {
        if (!fastPath) { super.writeStartDocument(encoding, version); return; }
        delegate.writeStartDocument(encoding, version);
        delegate.writeCharacters("\n");
    }

    @Override
    public void writeStartElement(final java.lang.String localName) throws XMLStreamException {
        if (!fastPath) { super.writeStartElement(localName); return; }
        onStartElementFast();
        delegate.writeStartElement(localName);
    }

    @Override
    public void writeStartElement(final java.lang.String namespaceURI, final java.lang.String localName) throws XMLStreamException {
        if (!fastPath) { super.writeStartElement(namespaceURI, localName); return; }
        onStartElementFast();
        delegate.writeStartElement(namespaceURI, localName);
    }

    @Override
    public void writeStartElement(final java.lang.String prefix, final java.lang.String localName, final java.lang.String namespaceURI)
            throws XMLStreamException {
        if (!fastPath) { super.writeStartElement(prefix, localName, namespaceURI); return; }
        onStartElementFast();
        delegate.writeStartElement(prefix, localName, namespaceURI);
    }

    @Override
    public void writeEmptyElement(final java.lang.String namespaceURI, final java.lang.String localName) throws XMLStreamException {
        if (!fastPath) { super.writeEmptyElement(namespaceURI, localName); return; }
        onEmptyElementFast();
        delegate.writeEmptyElement(namespaceURI, localName);
    }

    @Override
    public void writeEmptyElement(final java.lang.String prefix, final java.lang.String localName, final java.lang.String namespaceURI)
            throws XMLStreamException {
        if (!fastPath) { super.writeEmptyElement(prefix, localName, namespaceURI); return; }
        onEmptyElementFast();
        delegate.writeEmptyElement(prefix, localName, namespaceURI);
    }

    @Override
    public void writeEmptyElement(final java.lang.String localName) throws XMLStreamException {
        if (!fastPath) { super.writeEmptyElement(localName); return; }
        onEmptyElementFast();
        delegate.writeEmptyElement(localName);
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        if (!fastPath) { super.writeEndElement(); return; }
        onEndElementFast();
        delegate.writeEndElement();
    }

    @Override
    public void writeCharacters(final java.lang.String text) throws XMLStreamException {
        if (!fastPath) { super.writeCharacters(text); return; }
        state = SEEN_DATA;
        delegate.writeCharacters(text);
    }

    @Override
    public void writeCharacters(final char[] text, final int start, final int len) throws XMLStreamException {
        if (!fastPath) { super.writeCharacters(text, start, len); return; }
        state = SEEN_DATA;
        delegate.writeCharacters(text, start, len);
    }

    @Override
    public void writeCData(final java.lang.String data) throws XMLStreamException {
        if (!fastPath) { super.writeCData(data); return; }
        state = SEEN_DATA;
        delegate.writeCData(data);
    }
}

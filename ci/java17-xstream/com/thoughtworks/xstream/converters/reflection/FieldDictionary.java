/*
 * Java 17 compatibility replacement for XStream 1.4.10's FieldDictionary.
 *
 * The original eagerly calls setAccessible(true) for every declared field,
 * including java.base internals. Java 17 throws InaccessibleObjectException
 * before XStream can even register aliases. Keep those fields in the
 * dictionary, but tolerate the module boundary; XStream's reflection provider
 * may still use its own allocation/Unsafe path when an actual value is read.
 */
package com.thoughtworks.xstream.converters.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import com.thoughtworks.xstream.core.Caching;
import com.thoughtworks.xstream.core.JVM;
import com.thoughtworks.xstream.core.util.OrderRetainingMap;

public class FieldDictionary implements Caching {

    private static final DictionaryEntry OBJECT_DICTIONARY_ENTRY =
            new DictionaryEntry(Collections.EMPTY_MAP, Collections.EMPTY_MAP);

    private transient Map dictionaryEntries;
    private final FieldKeySorter sorter;

    public FieldDictionary() {
        this(new ImmutableFieldKeySorter());
    }

    public FieldDictionary(final FieldKeySorter sorter) {
        this.sorter = sorter;
        init();
    }

    private void init() {
        dictionaryEntries = new HashMap();
    }

    public Iterator serializableFieldsFor(final Class cls) {
        return fieldsFor(cls);
    }

    public Iterator fieldsFor(final Class cls) {
        return buildMap(cls, true).values().iterator();
    }

    public Field field(final Class cls, final String name, final Class definedIn) {
        final Field field = fieldOrNull(cls, name, definedIn);
        if (field == null) {
            throw new MissingFieldException(cls.getName(), name);
        }
        return field;
    }

    public Field fieldOrNull(final Class cls, final String name, final Class definedIn) {
        final Map fields = buildMap(cls, definedIn != null);
        return (Field) fields.get(definedIn != null
                ? (Object) new FieldKey(name, definedIn, -1)
                : (Object) name);
    }

    private Map buildMap(final Class type, final boolean tupleKeyed) {
        Class cls = type;
        DictionaryEntry lastDictionaryEntry = null;
        final LinkedList superClasses = new LinkedList();
        while (lastDictionaryEntry == null) {
            if (Object.class.equals(cls) || cls == null) {
                lastDictionaryEntry = OBJECT_DICTIONARY_ENTRY;
            } else {
                lastDictionaryEntry = getDictionaryEntry(cls);
            }
            if (lastDictionaryEntry == null) {
                superClasses.addFirst(cls);
                cls = cls.getSuperclass();
            }
        }

        for (final Iterator iter = superClasses.iterator(); iter.hasNext();) {
            cls = (Class) iter.next();
            DictionaryEntry newDictionaryEntry =
                    buildDictionaryEntryForClass(cls, lastDictionaryEntry);
            synchronized (this) {
                final DictionaryEntry concurrentEntry = getDictionaryEntry(cls);
                if (concurrentEntry == null) {
                    dictionaryEntries.put(cls, newDictionaryEntry);
                } else {
                    newDictionaryEntry = concurrentEntry;
                }
            }
            lastDictionaryEntry = newDictionaryEntry;
        }

        return tupleKeyed
                ? lastDictionaryEntry.getKeyedByFieldKey()
                : lastDictionaryEntry.getKeyedByFieldName();
    }

    private DictionaryEntry buildDictionaryEntryForClass(
            final Class cls, final DictionaryEntry lastDictionaryEntry) {
        final Map keyedByFieldName =
                new HashMap(lastDictionaryEntry.getKeyedByFieldName());
        final Map keyedByFieldKey =
                new OrderRetainingMap(lastDictionaryEntry.getKeyedByFieldKey());
        final Field[] fields = cls.getDeclaredFields();
        if (JVM.reverseFieldDefinition()) {
            for (int i = fields.length >> 1; i-- > 0;) {
                final int idx = fields.length - i - 1;
                final Field field = fields[i];
                fields[i] = fields[idx];
                fields[idx] = field;
            }
        }
        for (int i = 0; i < fields.length; i++) {
            final Field field = fields[i];
            if (!field.isAccessible()) {
                try {
                    field.setAccessible(true);
                } catch (final RuntimeException inaccessibleOnJava9Plus) {
                    // Keep the metadata entry. Java 17 module encapsulation blocks
                    // eager accessibility for java.base fields, but alias lookup
                    // itself does not require reflective access to the value.
                } catch (final SecurityException inaccessibleByPolicy) {
                    // Same behavior for a SecurityManager-restricted field.
                }
            }
            final FieldKey fieldKey =
                    new FieldKey(field.getName(), field.getDeclaringClass(), i);
            final Field existent = (Field) keyedByFieldName.get(field.getName());
            if (existent == null
                    || (existent.getModifiers() & Modifier.STATIC) != 0
                    || (field.getModifiers() & Modifier.STATIC) == 0) {
                keyedByFieldName.put(field.getName(), field);
            }
            keyedByFieldKey.put(fieldKey, field);
        }
        final Map sortedFieldKeys = sorter.sort(cls, keyedByFieldKey);
        return new DictionaryEntry(keyedByFieldName, sortedFieldKeys);
    }

    private synchronized DictionaryEntry getDictionaryEntry(final Class cls) {
        return (DictionaryEntry) dictionaryEntries.get(cls);
    }

    public synchronized void flushCache() {
        dictionaryEntries.clear();
        if (sorter instanceof Caching) {
            ((Caching) sorter).flushCache();
        }
    }

    protected Object readResolve() {
        init();
        return this;
    }

    private static final class DictionaryEntry {
        private final Map keyedByFieldName;
        private final Map keyedByFieldKey;

        DictionaryEntry(final Map keyedByFieldName, final Map keyedByFieldKey) {
            this.keyedByFieldName = keyedByFieldName;
            this.keyedByFieldKey = keyedByFieldKey;
        }

        Map getKeyedByFieldName() {
            return keyedByFieldName;
        }

        Map getKeyedByFieldKey() {
            return keyedByFieldKey;
        }
    }
}

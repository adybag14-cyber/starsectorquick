package com.thoughtworks.xstream.converters.reflection;

import com.thoughtworks.xstream.core.Caching;
import com.thoughtworks.xstream.core.JVM;
import com.thoughtworks.xstream.core.util.OrderRetainingMap;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * Runtime-safe variant for modular JDKs.
 *
 * Upstream XStream 1.4.10 calls field.setAccessible(true) unconditionally.
 * On modular runtimes this may throw RuntimeException
 * (InaccessibleObjectException). We skip those fields so dictionary build can
 * continue.
 */
public class FieldDictionary implements Caching {
    private static final DictionaryEntry OBJECT_DICTIONARY_ENTRY =
            new DictionaryEntry(Collections.EMPTY_MAP, Collections.EMPTY_MAP);

    private transient Map dictionaryEntries;
    private final FieldKeySorter sorter;

    public FieldDictionary() {
        this(new ImmutableFieldKeySorter());
    }

    public FieldDictionary(FieldKeySorter sorter) {
        this.sorter = sorter;
        this.init();
    }

    private void init() {
        this.dictionaryEntries = new HashMap();
    }

    public Iterator serializableFieldsFor(Class cls) {
        return this.fieldsFor(cls);
    }

    public Iterator fieldsFor(Class cls) {
        return this.buildMap(cls, true).values().iterator();
    }

    public Field field(Class cls, String name, Class definedIn) {
        Field field = this.fieldOrNull(cls, name, definedIn);
        if (field == null) {
            throw new MissingFieldException(cls.getName(), name);
        }
        return field;
    }

    public Field fieldOrNull(Class cls, String name, Class definedIn) {
        Map fields = this.buildMap(cls, definedIn != null);
        return (Field) fields.get(definedIn != null ? new FieldKey(name, definedIn, -1) : name);
    }

    private Map buildMap(Class type, boolean tupleKeyed) {
        Class cls2 = type;
        DictionaryEntry lastDictionaryEntry = null;
        LinkedList superClasses = new LinkedList();
        while (lastDictionaryEntry == null) {
            lastDictionaryEntry = Object.class.equals(cls2) || cls2 == null
                    ? OBJECT_DICTIONARY_ENTRY
                    : this.getDictionaryEntry(cls2);
            if (lastDictionaryEntry == null) {
                superClasses.addFirst(cls2);
                cls2 = cls2.getSuperclass();
            }
        }

        for (Object c : superClasses) {
            Class superCls = (Class) c;
            DictionaryEntry newDictionaryEntry =
                    this.buildDictionaryEntryForClass(superCls, lastDictionaryEntry);
            synchronized (this) {
                DictionaryEntry concurrentEntry = this.getDictionaryEntry(superCls);
                if (concurrentEntry == null) {
                    this.dictionaryEntries.put(superCls, newDictionaryEntry);
                } else {
                    newDictionaryEntry = concurrentEntry;
                }
            }
            lastDictionaryEntry = newDictionaryEntry;
        }

        return tupleKeyed ? lastDictionaryEntry.getKeyedByFieldKey() : lastDictionaryEntry.getKeyedByFieldName();
    }

    private DictionaryEntry buildDictionaryEntryForClass(Class cls, DictionaryEntry lastDictionaryEntry) {
        int i;
        HashMap keyedByFieldName = new HashMap(lastDictionaryEntry.getKeyedByFieldName());
        OrderRetainingMap keyedByFieldKey = new OrderRetainingMap(lastDictionaryEntry.getKeyedByFieldKey());
        Field[] fields = cls.getDeclaredFields();

        if (JVM.reverseFieldDefinition()) {
            i = fields.length >> 1;
            while (i-- > 0) {
                int idx = fields.length - i - 1;
                Field field = fields[i];
                fields[i] = fields[idx];
                fields[idx] = field;
            }
        }

        for (i = 0; i < fields.length; ++i) {
            Field field = fields[i];
            if (!field.isAccessible()) {
                try {
                    field.setAccessible(true);
                } catch (RuntimeException ex) {
                    // Includes InaccessibleObjectException on modular JDKs.
                    // Keep field in dictionary even if it cannot be made accessible.
                }
            }

            FieldKey fieldKey = new FieldKey(field.getName(), field.getDeclaringClass(), i);
            Field existent = (Field) keyedByFieldName.get(field.getName());
            if (existent == null
                    || (existent.getModifiers() & 8) != 0
                    || ((field.getModifiers() & 8) == 0)) {
                keyedByFieldName.put(field.getName(), field);
            }
            keyedByFieldKey.put(fieldKey, field);
        }

        Map sortedFieldKeys = this.sorter.sort(cls, keyedByFieldKey);
        return new DictionaryEntry(keyedByFieldName, sortedFieldKeys);
    }

    private synchronized DictionaryEntry getDictionaryEntry(Class cls) {
        return (DictionaryEntry) this.dictionaryEntries.get(cls);
    }

    public synchronized void flushCache() {
        this.dictionaryEntries.clear();
        if (this.sorter instanceof Caching) {
            ((Caching) this.sorter).flushCache();
        }
    }

    protected Object readResolve() {
        this.init();
        return this;
    }

    private static final class DictionaryEntry {
        private final Map keyedByFieldName;
        private final Map keyedByFieldKey;

        public DictionaryEntry(Map keyedByFieldName, Map keyedByFieldKey) {
            this.keyedByFieldName = keyedByFieldName;
            this.keyedByFieldKey = keyedByFieldKey;
        }

        public Map getKeyedByFieldName() {
            return this.keyedByFieldName;
        }

        public Map getKeyedByFieldKey() {
            return this.keyedByFieldKey;
        }
    }
}

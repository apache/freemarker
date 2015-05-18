/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.template;

import java.io.Serializable;
import java.lang.reflect.Array;

import freemarker.ext.util.WrapperTemplateModel;

/**
 * Adapts an {@code array} of a non-primitive elements to the corresponding {@link TemplateModel} interface(s), most
 * importantly to {@link TemplateHashModelEx}. If you aren't wrapping an already existing {@code array}, but build a
 * sequence specifically to be used from a template, also consider using {@link SimpleSequence} (see comparison there).
 *
 * <p>
 * Thread safety: A {@link DefaultListAdapter} is as thread-safe as the array that it wraps is. Normally you only
 * have to consider read-only access, as the FreeMarker template language doesn't allow writing these sequences (though
 * of course, Java methods called from the template can violate this rule).
 * 
 * <p>
 * This adapter is used by {@link DefaultObjectWrapper} if its {@code useAdaptersForCollections} property is
 * {@code true}, which is the default when its {@code incompatibleImprovements} property is 2.3.22 or higher.
 * 
 * @see SimpleSequence
 * @see DefaultListAdapter
 * @see TemplateSequenceModel
 * 
 * @since 2.3.22
 */
public abstract class DefaultArrayAdapter extends WrappingTemplateModel implements TemplateSequenceModel,
        AdapterTemplateModel, WrapperTemplateModel, Serializable {

    /**
     * Factory method for creating new adapter instances.
     * 
     * @param array
     *            The array to adapt; can't be {@code null}. Must be an array. 
     * @param wrapper
     *            The {@link ObjectWrapper} used to wrap the items in the array. Has to be
     *            {@link ObjectWrapperAndUnwrapper} because of planned future features.
     */
    public static DefaultArrayAdapter adapt(Object array, ObjectWrapperAndUnwrapper wrapper) {
        final Class componentType = array.getClass().getComponentType();
        if (componentType == null) {
            throw new IllegalArgumentException("Not an array");
        }
        
        if (componentType.isPrimitive()) {
            if (componentType == int.class) {
                return new IntArrayAdapter((int[]) array, wrapper);
            }
            if (componentType == double.class) {
                return new DoubleArrayAdapter((double[]) array, wrapper);
            }
            if (componentType == long.class) {
                return new LongArrayAdapter((long[]) array, wrapper);
            }
            if (componentType == boolean.class) {
                return new BooleanArrayAdapter((boolean[]) array, wrapper);
            }
            if (componentType == float.class) {
                return new FloatArrayAdapter((float[]) array, wrapper);
            }
            if (componentType == char.class) {
                return new CharArrayAdapter((char[]) array, wrapper);
            }
            if (componentType == short.class) {
                return new ShortArrayAdapter((short[]) array, wrapper);
            }
            if (componentType == byte.class) {
                return new ByteArrayAdapter((byte[]) array, wrapper);
            }
            return new GenericPrimitiveArrayAdapter(array, wrapper);
        } else {
            return new ObjectArrayAdapter((Object[]) array, wrapper);
        }
    }

    private DefaultArrayAdapter(ObjectWrapper wrapper) {
        super(wrapper);
    }

    public final Object getAdaptedObject(Class hint) {
        return getWrappedObject();
    }

    private static class ObjectArrayAdapter extends DefaultArrayAdapter {

        private final Object[] array;

        private ObjectArrayAdapter(Object[] array, ObjectWrapper wrapper) {
            super(wrapper);
            this.array = array;
        }

        public TemplateModel get(int index) throws TemplateModelException {
            return index >= 0 && index < array.length ? wrap(array[index]) : null;
        }

        public int size() throws TemplateModelException {
            return array.length;
        }

        public Object getWrappedObject() {
            return array;
        }

    }

    private static class ByteArrayAdapter extends DefaultArrayAdapter {

        private final byte[] array;

        private ByteArrayAdapter(byte[] array, ObjectWrapper wrapper) {
            super(wrapper);
            this.array = array;
        }

        public TemplateModel get(int index) throws TemplateModelException {
            // Java 5: Use valueOf
            return index >= 0 && index < array.length ? wrap(new Byte(array[index])) : null;
        }

        public int size() throws TemplateModelException {
            return array.length;
        }

        public Object getWrappedObject() {
            return array;
        }

    }

    private static class ShortArrayAdapter extends DefaultArrayAdapter {

        private final short[] array;

        private ShortArrayAdapter(short[] array, ObjectWrapper wrapper) {
            super(wrapper);
            this.array = array;
        }

        public TemplateModel get(int index) throws TemplateModelException {
            // Java 5: Use valueOf
            return index >= 0 && index < array.length ? wrap(new Short(array[index])) : null;
        }

        public int size() throws TemplateModelException {
            return array.length;
        }

        public Object getWrappedObject() {
            return array;
        }

    }

    private static class IntArrayAdapter extends DefaultArrayAdapter {

        private final int[] array;

        private IntArrayAdapter(int[] array, ObjectWrapper wrapper) {
            super(wrapper);
            this.array = array;
        }

        public TemplateModel get(int index) throws TemplateModelException {
            // Java 5: Integer.valueOf
            return index >= 0 && index < array.length ? wrap(new Integer(array[index])) : null;
        }

        public int size() throws TemplateModelException {
            return array.length;
        }

        public Object getWrappedObject() {
            return array;
        }

    }

    private static class LongArrayAdapter extends DefaultArrayAdapter {

        private final long[] array;

        private LongArrayAdapter(long[] array, ObjectWrapper wrapper) {
            super(wrapper);
            this.array = array;
        }

        public TemplateModel get(int index) throws TemplateModelException {
            // Java 5: Use valueOf
            return index >= 0 && index < array.length ? wrap(new Long(array[index])) : null;
        }

        public int size() throws TemplateModelException {
            return array.length;
        }

        public Object getWrappedObject() {
            return array;
        }

    }

    private static class FloatArrayAdapter extends DefaultArrayAdapter {

        private final float[] array;

        private FloatArrayAdapter(float[] array, ObjectWrapper wrapper) {
            super(wrapper);
            this.array = array;
        }

        public TemplateModel get(int index) throws TemplateModelException {
            // Java 5: Use valueOf
            return index >= 0 && index < array.length ? wrap(new Float(array[index])) : null;
        }

        public int size() throws TemplateModelException {
            return array.length;
        }

        public Object getWrappedObject() {
            return array;
        }

    }

    private static class DoubleArrayAdapter extends DefaultArrayAdapter {

        private final double[] array;

        private DoubleArrayAdapter(double[] array, ObjectWrapper wrapper) {
            super(wrapper);
            this.array = array;
        }

        public TemplateModel get(int index) throws TemplateModelException {
            // Java 5: Use valueOf
            return index >= 0 && index < array.length ? wrap(new Double(array[index])) : null;
        }

        public int size() throws TemplateModelException {
            return array.length;
        }

        public Object getWrappedObject() {
            return array;
        }

    }

    private static class CharArrayAdapter extends DefaultArrayAdapter {

        private final char[] array;

        private CharArrayAdapter(char[] array, ObjectWrapper wrapper) {
            super(wrapper);
            this.array = array;
        }

        public TemplateModel get(int index) throws TemplateModelException {
            // Java 5: Use valueOf
            return index >= 0 && index < array.length ? wrap(new Character(array[index])) : null;
        }

        public int size() throws TemplateModelException {
            return array.length;
        }

        public Object getWrappedObject() {
            return array;
        }

    }

    private static class BooleanArrayAdapter extends DefaultArrayAdapter {

        private final boolean[] array;

        private BooleanArrayAdapter(boolean[] array, ObjectWrapper wrapper) {
            super(wrapper);
            this.array = array;
        }

        public TemplateModel get(int index) throws TemplateModelException {
            // Java 5: Use valueOf
            return index >= 0 && index < array.length ? wrap(new Boolean(array[index])) : null;
        }

        public int size() throws TemplateModelException {
            return array.length;
        }

        public Object getWrappedObject() {
            return array;
        }

    }

    /**
     * Much slower than the specialized versions; used only as the last resort.
     */
    private static class GenericPrimitiveArrayAdapter extends DefaultArrayAdapter {

        private final Object array;
        private final int length;

        private GenericPrimitiveArrayAdapter(Object array, ObjectWrapper wrapper) {
            super(wrapper);
            this.array = array;
            length = Array.getLength(array);
        }

        public TemplateModel get(int index) throws TemplateModelException {
            // Java 5: Integer.valueOf
            return index >= 0 && index < length ? wrap(Array.get(array, index)) : null;
        }

        public int size() throws TemplateModelException {
            return length;
        }

        public Object getWrappedObject() {
            return array;
        }

    }

}
